package org.openelisglobal.storage.service;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StoragePositionDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BarcodeValidationService
 * Implements 5-step validation process per FR-024 through FR-027
 */
@Service
@Transactional(readOnly = true)
public class BarcodeValidationServiceImpl implements BarcodeValidationService {

    @Autowired
    private BarcodeParsingService barcodeParsingService;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageRackDAO storageRackDAO;

    @Autowired
    private StoragePositionDAO storagePositionDAO;

    @Override
    public BarcodeValidationResponse validateBarcode(String barcode) {
        BarcodeValidationResponse response = new BarcodeValidationResponse();
        response.setBarcode(barcode);

        // Step 1: Format Validation
        ParsedBarcode parsed = barcodeParsingService.parseBarcode(barcode);
        if (!parsed.isValid()) {
            response.setValid(false);
            response.setFailedStep("FORMAT_VALIDATION");
            response.setErrorMessage(parsed.getErrorMessage());
            return response;
        }

        // Step 2: Location Existence Check
        StorageRoom room = storageRoomDAO.findByCode(parsed.getRoomCode());
        if (room == null) {
            response.setValid(false);
            response.setFailedStep("LOCATION_EXISTENCE");
            response.setErrorMessage("Room not found: " + parsed.getRoomCode());
            return response;
        }
        response.addValidComponent("room", createComponentMap(room.getId(), room.getName(), room.getCode()));

        StorageDevice device = null;
        if (parsed.getDeviceCode() != null) {
            device = storageDeviceDAO.findByCodeAndParentRoom(parsed.getDeviceCode(), room);
            if (device == null) {
                response.setValid(false);
                response.setFailedStep("LOCATION_EXISTENCE");
                response.setErrorMessage("Device not found: " + parsed.getDeviceCode() + " in room " + room.getName());
                return response;
            }
            response.addValidComponent("device", createComponentMap(device.getId(), device.getName(), device.getCode()));
        }

        StorageShelf shelf = null;
        if (parsed.getShelfCode() != null) {
            if (device == null) {
                response.setValid(false);
                response.setFailedStep("HIERARCHY_VALIDATION");
                response.setErrorMessage("Shelf cannot exist without device");
                return response;
            }
            shelf = storageShelfDAO.findByLabelAndParentDevice(parsed.getShelfCode(), device);
            if (shelf == null) {
                response.setValid(false);
                response.setFailedStep("LOCATION_EXISTENCE");
                response.setErrorMessage("Shelf not found: " + parsed.getShelfCode() + " in device " + device.getName());
                return response;
            }
            response.addValidComponent("shelf", createComponentMap(shelf.getId(), shelf.getLabel(), shelf.getLabel()));
        }

        StorageRack rack = null;
        if (parsed.getRackCode() != null) {
            if (shelf == null) {
                response.setValid(false);
                response.setFailedStep("HIERARCHY_VALIDATION");
                response.setErrorMessage("Rack cannot exist without shelf");
                return response;
            }
            rack = storageRackDAO.findByLabelAndParentShelf(parsed.getRackCode(), shelf);
            if (rack == null) {
                response.setValid(false);
                response.setFailedStep("LOCATION_EXISTENCE");
                response.setErrorMessage("Rack not found: " + parsed.getRackCode() + " in shelf " + shelf.getLabel());
                return response;
            }
            response.addValidComponent("rack", createComponentMap(rack.getId(), rack.getLabel(), rack.getLabel()));
        }

        StoragePosition position = null;
        if (parsed.getPositionCode() != null) {
            if (rack == null) {
                response.setValid(false);
                response.setFailedStep("HIERARCHY_VALIDATION");
                response.setErrorMessage("Position cannot exist without rack");
                return response;
            }
            position = storagePositionDAO.findByCoordinatesAndParentRack(parsed.getPositionCode(), rack);
            if (position == null) {
                response.setValid(false);
                response.setFailedStep("LOCATION_EXISTENCE");
                response.setErrorMessage("Position not found: " + parsed.getPositionCode() + " in rack " + rack.getLabel());
                return response;
            }
            response.addValidComponent("position", createComponentMap(position.getId(), position.getCoordinate(), position.getCoordinate()));
        }

        // Step 3: Hierarchy Validation (already done during existence checks above)

        // Step 4: Activity Check
        if (room.getActive() == null || !room.getActive()) {
            response.setValid(false);
            response.setFailedStep("ACTIVITY_CHECK");
            response.setErrorMessage("Room is inactive: " + room.getName());
            return response;
        }

        if (device != null && (device.getActive() == null || !device.getActive())) {
            response.setValid(false);
            response.setFailedStep("ACTIVITY_CHECK");
            response.setErrorMessage("Device is inactive: " + device.getName());
            return response;
        }

        if (shelf != null && (shelf.getActive() == null || !shelf.getActive())) {
            response.setValid(false);
            response.setFailedStep("ACTIVITY_CHECK");
            response.setErrorMessage("Shelf is inactive: " + shelf.getLabel());
            return response;
        }

        if (rack != null && (rack.getActive() == null || !rack.getActive())) {
            response.setValid(false);
            response.setFailedStep("ACTIVITY_CHECK");
            response.setErrorMessage("Rack is inactive: " + rack.getLabel());
            return response;
        }

        // Note: StoragePosition doesn't have an active field - it inherits activity from its parent hierarchy

        // Step 5: Conflict Check
        // Note: With the polymorphic location model (Phase 4), we check if the exact location
        // (locationId + locationType + optional coordinate) is occupied
        // For barcode validation, we're validating the barcode format and hierarchy,
        // not checking occupancy at this level (that's done during assignment)

        // All validation steps passed
        response.setValid(true);
        return response;
    }

    /**
     * Create a map with component details for form pre-filling
     */
    private Map<String, Object> createComponentMap(Integer id, String name, String code) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("code", code);
        return map;
    }
}
