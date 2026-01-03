package io.strategiz.data.marketdata.repository;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.marketdata.entity.MarketDataCoverageEntity;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore implementation of MarketDataCoverageRepository.
 *
 * Coverage snapshots are system-wide (not user-scoped), so we use "system" as the userId
 * for all audit operations. Snapshots are typically created by scheduled jobs or admin triggers.
 *
 * Collection: "marketdata_coverage_stats"
 * Document ID: Snapshot timestamp (e.g., "coverage_2025-12-24T10:00:00Z")
 */
@Repository
public class MarketDataCoverageRepositoryImpl
    extends BaseRepository<MarketDataCoverageEntity>
    implements MarketDataCoverageRepository {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCoverageRepositoryImpl.class);
    private static final String SYSTEM_USER = "system";
    private static final int BATCH_DELETE_SIZE = 500;

    @Autowired
    public MarketDataCoverageRepositoryImpl(Firestore firestore) {
        super(firestore, MarketDataCoverageEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-marketdata";
    }

    @Override
    public MarketDataCoverageEntity save(MarketDataCoverageEntity entity, String userId) {
        // Use provided userId for admin-triggered saves, or default to system
        String effectiveUserId = (userId != null && !userId.isEmpty()) ? userId : SYSTEM_USER;
        return super.save(entity, effectiveUserId);
    }

    @Override
    public Optional<MarketDataCoverageEntity> findLatest() {
        try {
            // Query for most recent snapshot by calculatedAt timestamp
            // Note: No isActive filter to avoid composite index requirement
            Query query = getCollection()
                .orderBy("calculatedAt", Query.Direction.DESCENDING)
                .limit(1);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (!docs.isEmpty()) {
                MarketDataCoverageEntity entity = docs.get(0).toObject(MarketDataCoverageEntity.class);
                entity.setId(docs.get(0).getId());
                log.debug("Found latest coverage snapshot: {}", entity.getSnapshotId());
                return Optional.of(entity);
            }

            log.debug("No coverage snapshots found");
            return Optional.empty();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while finding latest coverage snapshot", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity"
            );
        } catch (ExecutionException e) {
            log.error("Error finding latest coverage snapshot", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity",
                "findLatest"
            );
        }
    }

    @Override
    public List<MarketDataCoverageEntity> findByDateRange(Timestamp start, Timestamp end) {
        try {
            // Query for snapshots within date range
            // Note: No isActive filter to avoid composite index requirement
            Query query = getCollection()
                .whereGreaterThanOrEqualTo("calculatedAt", start)
                .whereLessThan("calculatedAt", end)
                .orderBy("calculatedAt", Query.Direction.DESCENDING);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            List<MarketDataCoverageEntity> entities = docs.stream()
                .map(doc -> {
                    MarketDataCoverageEntity entity = doc.toObject(MarketDataCoverageEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

            log.debug("Found {} coverage snapshots between {} and {}", entities.size(), start, end);
            return entities;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while finding coverage snapshots by date range", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity"
            );
        } catch (ExecutionException e) {
            log.error("Error finding coverage snapshots by date range: {} to {}", start, end, e);
            throw new StrategizException(
                DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity",
                "findByDateRange"
            );
        }
    }

    @Override
    public Optional<MarketDataCoverageEntity> findById(String snapshotId) {
        return super.findById(snapshotId);
    }

    @Override
    public List<MarketDataCoverageEntity> findRecent(int limit) {
        try {
            // Query for N most recent snapshots
            // Note: No isActive filter to avoid composite index requirement
            Query query = getCollection()
                .orderBy("calculatedAt", Query.Direction.DESCENDING)
                .limit(limit);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            List<MarketDataCoverageEntity> entities = docs.stream()
                .map(doc -> {
                    MarketDataCoverageEntity entity = doc.toObject(MarketDataCoverageEntity.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(Collectors.toList());

            log.debug("Found {} recent coverage snapshots", entities.size());
            return entities;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while finding recent coverage snapshots", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity"
            );
        } catch (ExecutionException e) {
            log.error("Error finding recent coverage snapshots with limit {}", limit, e);
            throw new StrategizException(
                DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity",
                "findRecent"
            );
        }
    }

    @Override
    public int deleteOlderThan(Timestamp cutoff) {
        try {
            // Find all snapshots older than cutoff
            // Note: No isActive filter to avoid composite index requirement
            Query query = getCollection()
                .whereLessThan("calculatedAt", cutoff);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            if (docs.isEmpty()) {
                log.debug("No snapshots older than {} to delete", cutoff);
                return 0;
            }

            int totalDeleted = 0;

            // Process in batches (Firestore limit is 500 per batch)
            for (int i = 0; i < docs.size(); i += BATCH_DELETE_SIZE) {
                int endIndex = Math.min(i + BATCH_DELETE_SIZE, docs.size());
                List<QueryDocumentSnapshot> batch = docs.subList(i, endIndex);

                WriteBatch writeBatch = firestore.batch();

                for (QueryDocumentSnapshot doc : batch) {
                    // Soft delete by setting isActive = false
                    MarketDataCoverageEntity entity = doc.toObject(MarketDataCoverageEntity.class);
                    entity.setId(doc.getId());
                    entity._softDelete(SYSTEM_USER);

                    DocumentReference docRef = getCollection().document(entity.getId());
                    writeBatch.set(docRef, entity);
                }

                writeBatch.commit().get();
                totalDeleted += batch.size();

                log.info("Soft deleted batch of {} coverage snapshots older than {}", batch.size(), cutoff);
            }

            log.info("Total soft deleted {} coverage snapshots older than {}", totalDeleted, cutoff);
            return totalDeleted;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while deleting old coverage snapshots", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity"
            );
        } catch (ExecutionException e) {
            log.error("Error deleting coverage snapshots older than {}", cutoff, e);
            throw new StrategizException(
                DataRepositoryErrorDetails.BULK_OPERATION_FAILED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity",
                "deleteOlderThan"
            );
        }
    }

    @Override
    public long count() {
        // Use inherited count from BaseRepository (counts active entities)
        return super.count();
    }

    @Override
    public int deleteAll() {
        try {
            // Find all active snapshots
            List<MarketDataCoverageEntity> allEntities = findAll();

            if (allEntities.isEmpty()) {
                log.debug("No coverage snapshots to delete");
                return 0;
            }

            int totalDeleted = 0;

            // Process in batches
            for (int i = 0; i < allEntities.size(); i += BATCH_DELETE_SIZE) {
                int endIndex = Math.min(i + BATCH_DELETE_SIZE, allEntities.size());
                List<MarketDataCoverageEntity> batch = allEntities.subList(i, endIndex);

                WriteBatch writeBatch = firestore.batch();

                for (MarketDataCoverageEntity entity : batch) {
                    entity._softDelete(SYSTEM_USER);
                    DocumentReference docRef = getCollection().document(entity.getId());
                    writeBatch.set(docRef, entity);
                }

                writeBatch.commit().get();
                totalDeleted += batch.size();

                log.info("Soft deleted batch of {} coverage snapshots", batch.size());
            }

            log.warn("Soft deleted ALL {} coverage snapshots - this should only be used for cleanup", totalDeleted);
            return totalDeleted;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while deleting all coverage snapshots", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity"
            );
        } catch (ExecutionException e) {
            log.error("Error deleting all coverage snapshots", e);
            throw new StrategizException(
                DataRepositoryErrorDetails.BULK_OPERATION_FAILED,
                "data-marketdata",
                e,
                "MarketDataCoverageEntity",
                "deleteAll"
            );
        }
    }
}
