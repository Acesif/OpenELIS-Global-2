package org.openelisglobal.storage.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StoragePosition;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, Integer> {
    SampleStorageAssignment findBySampleId(String sampleId);

    /**
     * Find assignment by storage position (for barcode validation)
     *
     * @param position Storage position entity
     * @return SampleStorageAssignment or null if position is not occupied
     */
    SampleStorageAssignment findByStoragePosition(StoragePosition position);
}
