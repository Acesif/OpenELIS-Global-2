package org.openelisglobal.storage.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import org.openelisglobal.barcode.BarcodeLabelMaker;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.storage.barcode.labeltype.StorageLocationLabel;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of LabelManagementService
 * Generates PDF labels for storage locations and tracks print history
 */
@Service
@Transactional
public class LabelManagementServiceImpl implements LabelManagementService {

    @Autowired
    private StorageLocationService storageLocationService;

    // TODO: Add print history DAO when database schema is added in Phase 5.4
    // @Autowired
    // private StorageLocationPrintHistoryDAO printHistoryDAO;

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageDevice device, String shortCode) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null");
        }

        // Build hierarchical path using codes (for barcode): RoomCode-DeviceCode
        StorageRoom parentRoom = device.getParentRoom();
        String hierarchicalPath = null;
        if (parentRoom != null && parentRoom.getCode() != null) {
            hierarchicalPath = parentRoom.getCode() + "-" + device.getCode();
        } else {
            hierarchicalPath = device.getCode();
        }

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(
            device.getName(),
            device.getCode(),
            hierarchicalPath,
            shortCode
        );

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageShelf shelf, String shortCode) {
        if (shelf == null) {
            throw new IllegalArgumentException("Shelf cannot be null");
        }

        // Build hierarchical path using codes: RoomCode-DeviceCode-ShelfLabel
        StorageDevice parentDevice = shelf.getParentDevice();
        String hierarchicalPath = null;
        if (parentDevice != null) {
            StorageRoom parentRoom = parentDevice.getParentRoom();
            if (parentRoom != null && parentRoom.getCode() != null) {
                hierarchicalPath = parentRoom.getCode() + "-" + parentDevice.getCode() + "-" + shelf.getLabel();
            } else {
                hierarchicalPath = parentDevice.getCode() + "-" + shelf.getLabel();
            }
        } else {
            hierarchicalPath = shelf.getLabel();
        }

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(
            shelf.getLabel(),
            shelf.getLabel(),
            hierarchicalPath,
            shortCode
        );

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateLabel(StorageRack rack, String shortCode) {
        if (rack == null) {
            throw new IllegalArgumentException("Rack cannot be null");
        }

        // Build hierarchical path using codes: RoomCode-DeviceCode-ShelfLabel-RackLabel
        StorageShelf parentShelf = rack.getParentShelf();
        String hierarchicalPath = null;
        if (parentShelf != null) {
            StorageDevice parentDevice = parentShelf.getParentDevice();
            if (parentDevice != null) {
                StorageRoom parentRoom = parentDevice.getParentRoom();
                if (parentRoom != null && parentRoom.getCode() != null) {
                    hierarchicalPath = parentRoom.getCode() + "-" + parentDevice.getCode() + 
                        "-" + parentShelf.getLabel() + "-" + rack.getLabel();
                } else {
                    hierarchicalPath = parentDevice.getCode() + "-" + parentShelf.getLabel() + "-" + rack.getLabel();
                }
            } else {
                hierarchicalPath = parentShelf.getLabel() + "-" + rack.getLabel();
            }
        } else {
            hierarchicalPath = rack.getLabel();
        }

        // Create label
        StorageLocationLabel label = new StorageLocationLabel(
            rack.getLabel(),
            rack.getLabel(),
            hierarchicalPath,
            shortCode
        );

        // Generate PDF using BarcodeLabelMaker
        return generatePDF(label);
    }

    /**
     * Generate PDF from label using BarcodeLabelMaker
     */
    private ByteArrayOutputStream generatePDF(StorageLocationLabel label) {
        try {
            // Link barcode label info (for print tracking)
            label.linkBarcodeLabelInfo();
            
            // Create BarcodeLabelMaker and add label
            BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();
            ArrayList<org.openelisglobal.barcode.labeltype.Label> labels = new ArrayList<>();
            labels.add(label);
            labelMaker = new BarcodeLabelMaker(labels);
            
            // Set number of labels to print (default 1)
            label.setNumLabels(1);
            
            // Generate PDF stream
            return labelMaker.createLabelsAsStream();
        } catch (Exception e) {
            LogEvent.logError("LabelManagementServiceImpl", "generatePDF", e.toString());
            throw new RuntimeException("Failed to generate label PDF", e);
        }
    }

    @Override
    @Transactional
    public void trackPrintHistory(String locationId, String locationType, String shortCode, String userId) {
        // TODO: Implement when print history table is added in Phase 5.4
        // For now, just log the print event
        LogEvent.logInfo("LabelManagementServiceImpl", "trackPrintHistory", 
            String.format("Label printed - Location: %s, Type: %s, ShortCode: %s, User: %s", 
                locationId, locationType, shortCode, userId));
        
        // When database schema is ready:
        // StorageLocationPrintHistory history = new StorageLocationPrintHistory();
        // history.setLocationId(locationId);
        // history.setLocationType(locationType);
        // history.setShortCode(shortCode);
        // history.setPrintedBy(userId);
        // history.setPrintedDate(new Timestamp(System.currentTimeMillis()));
        // history.setPrintCount(1);
        // printHistoryDAO.insert(history);
    }
}

