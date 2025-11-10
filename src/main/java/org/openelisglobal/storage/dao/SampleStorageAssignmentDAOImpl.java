package org.openelisglobal.storage.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SampleStorageAssignmentDAOImpl extends BaseDAOImpl<SampleStorageAssignment, Integer>
        implements SampleStorageAssignmentDAO {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageAssignmentDAOImpl.class);

    public SampleStorageAssignmentDAOImpl() {
        super(SampleStorageAssignment.class);
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findBySampleId(String sampleId) {
        try {
            // Note: Sample.id is String in entity but stored as numeric in database
            // Pattern used throughout codebase: parse String sampleId to Integer for
            // database queries
            // This matches the approach in SampleItemDAOImpl, SampleAdditionalFieldDAOImpl,
            // etc.
            String hql = "SELECT ssa FROM SampleStorageAssignment ssa JOIN ssa.sample s WHERE s.id = :sampleId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SampleStorageAssignment.class);
            // Parse String to Integer to match database column type (numeric)
            query.setParameter("sampleId", Integer.parseInt(sampleId));
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (NumberFormatException e) {
            logger.error("Invalid sample ID format (not numeric): " + sampleId, e);
            throw new LIMSRuntimeException("Invalid sample ID format: " + sampleId, e);
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by sample ID: " + sampleId, e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by sample ID: " + sampleId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleStorageAssignment findByStoragePosition(org.openelisglobal.storage.valueholder.StoragePosition position) {
        try {
            if (position == null) {
                return null;
            }
            String hql = "FROM SampleStorageAssignment ssa WHERE ssa.storagePosition.id = :positionId";
            Query<SampleStorageAssignment> query = entityManager.unwrap(Session.class).createQuery(hql, SampleStorageAssignment.class);
            query.setParameter("positionId", position.getId());
            query.setMaxResults(1);
            List<SampleStorageAssignment> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            logger.error("Error finding SampleStorageAssignment by storage position", e);
            throw new LIMSRuntimeException("Error finding SampleStorageAssignment by storage position", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPositionOccupied(org.openelisglobal.storage.valueholder.StoragePosition position) {
        try {
            if (position == null) {
                return false;
            }

            // If position has a coordinate and parent rack, check for assignment with matching coordinate
            if (position.getCoordinate() != null && !position.getCoordinate().isEmpty() 
                    && position.getParentRack() != null) {
                // Check for assignment to this rack with this coordinate
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId "
                        + "AND ssa.positionCoordinate = :coordinate";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("rackId", position.getParentRack().getId());
                query.setParameter("coordinate", position.getCoordinate());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentRack() != null) {
                // Position without coordinate but with rack - check for any assignment to this rack
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'rack' AND ssa.locationId = :rackId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("rackId", position.getParentRack().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentShelf() != null) {
                // Position at shelf level - check for assignment to this shelf
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'shelf' AND ssa.locationId = :shelfId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("shelfId", position.getParentShelf().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            } else if (position.getParentDevice() != null) {
                // Position at device level - check for assignment to this device
                String hql = "SELECT COUNT(*) FROM SampleStorageAssignment ssa "
                        + "WHERE ssa.locationType = 'device' AND ssa.locationId = :deviceId";
                Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
                query.setParameter("deviceId", position.getParentDevice().getId());
                Long count = query.uniqueResult();
                return count != null && count > 0;
            }

            return false;
        } catch (Exception e) {
            logger.error("Error checking position occupancy: " + e.getMessage(), e);
            // On error, return false (position appears unoccupied)
            return false;
        }
    }

    // No override needed - BaseDAOImpl.getAll() uses entity fetch strategies
    // All relationships are EAGER at entity level, so they load automatically
}
