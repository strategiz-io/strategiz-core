package io.strategiz.data.auth.repository;

import io.strategiz.data.auth.entity.RecoveryRequestEntity;
import io.strategiz.data.auth.entity.RecoveryStatus;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firebase implementation of RecoveryRequestRepository.
 * Manages account recovery requests in the recovery_requests collection.
 */
@Repository
public class RecoveryRequestRepositoryImpl extends BaseRepository<RecoveryRequestEntity>
        implements RecoveryRequestRepository {

    private static final Logger log = LoggerFactory.getLogger(RecoveryRequestRepositoryImpl.class);

    @Autowired
    public RecoveryRequestRepositoryImpl(Firestore firestore) {
        super(firestore, RecoveryRequestEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-auth";
    }

    @Override
    public RecoveryRequestEntity save(RecoveryRequestEntity entity, String userId) {
        return super.save(entity, userId);
    }

    @Override
    public Optional<RecoveryRequestEntity> findById(String id) {
        return super.findById(id);
    }

    @Override
    public List<RecoveryRequestEntity> findActiveByUserId(String userId) {
        try {
            Instant now = Instant.now();
            Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

            Query query = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isActive", true)
                    .whereIn("status", List.of(
                            RecoveryStatus.PENDING_EMAIL.name(),
                            RecoveryStatus.PENDING_SMS.name()));

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                    .map(doc -> {
                        RecoveryRequestEntity entity = doc.toObject(RecoveryRequestEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .filter(entity -> !entity.isExpired())
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "RecoveryRequestEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "RecoveryRequestEntity", userId);
        }
    }

    @Override
    public List<RecoveryRequestEntity> findActiveByEmail(String email) {
        try {
            Query query = getCollection()
                    .whereEqualTo("email", email)
                    .whereEqualTo("isActive", true)
                    .whereIn("status", List.of(
                            RecoveryStatus.PENDING_EMAIL.name(),
                            RecoveryStatus.PENDING_SMS.name()));

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                    .map(doc -> {
                        RecoveryRequestEntity entity = doc.toObject(RecoveryRequestEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .filter(entity -> !entity.isExpired())
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "RecoveryRequestEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "RecoveryRequestEntity", email);
        }
    }

    @Override
    public List<RecoveryRequestEntity> findByStatus(RecoveryStatus status) {
        return findByField("status", status.name());
    }

    @Override
    public long countByEmailInLastHours(String email, int hours) {
        try {
            Instant cutoff = Instant.now().minusSeconds(hours * 3600L);
            Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

            Query query = getCollection()
                    .whereEqualTo("email", email)
                    .whereGreaterThan("createdDate", cutoffTimestamp);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            return docs.size();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "RecoveryRequestEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "RecoveryRequestEntity", email);
        }
    }

    @Override
    public long countByIpInLastHours(String ipAddress, int hours) {
        try {
            Instant cutoff = Instant.now().minusSeconds(hours * 3600L);
            Timestamp cutoffTimestamp = Timestamp.ofTimeSecondsAndNanos(cutoff.getEpochSecond(), cutoff.getNano());

            Query query = getCollection()
                    .whereEqualTo("ipAddress", ipAddress)
                    .whereGreaterThan("createdDate", cutoffTimestamp);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();
            return docs.size();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "RecoveryRequestEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "RecoveryRequestEntity", ipAddress);
        }
    }

    @Override
    public RecoveryRequestEntity update(RecoveryRequestEntity entity, String userId) {
        return super.save(entity, userId);
    }

    @Override
    public int deleteExpired() {
        try {
            Instant now = Instant.now();
            Timestamp nowTimestamp = Timestamp.ofTimeSecondsAndNanos(now.getEpochSecond(), now.getNano());

            Query query = getCollection()
                    .whereEqualTo("isActive", true)
                    .whereIn("status", List.of(
                            RecoveryStatus.PENDING_EMAIL.name(),
                            RecoveryStatus.PENDING_SMS.name()))
                    .whereLessThan("expiresAt", nowTimestamp);

            List<QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            int count = 0;
            for (QueryDocumentSnapshot doc : docs) {
                RecoveryRequestEntity entity = doc.toObject(RecoveryRequestEntity.class);
                entity.setId(doc.getId());
                entity.markExpired();
                super.save(entity, "system");
                count++;
            }

            if (count > 0) {
                log.info("Marked {} expired recovery requests", count);
            }

            return count;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "RecoveryRequestEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "RecoveryRequestEntity", "expired");
        }
    }

    @Override
    public int cancelAllActiveForUser(String userId, String systemUserId) {
        List<RecoveryRequestEntity> activeRequests = findActiveByUserId(userId);

        int count = 0;
        for (RecoveryRequestEntity entity : activeRequests) {
            entity.markCancelled();
            super.save(entity, systemUserId);
            count++;
        }

        if (count > 0) {
            log.info("Cancelled {} active recovery requests for user {}", count, userId);
        }

        return count;
    }
}
