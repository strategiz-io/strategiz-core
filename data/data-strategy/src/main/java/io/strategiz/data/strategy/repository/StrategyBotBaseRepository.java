package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategyBot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for StrategyBot entities using Firestore.
 * Used internally by CRUD repository implementations.
 */
@Repository
public class StrategyBotBaseRepository extends BaseRepository<StrategyBot> {

    public StrategyBotBaseRepository(Firestore firestore) {
        super(firestore, StrategyBot.class);
    }

    /**
     * Find strategy bots by userId field
     */
    public java.util.List<StrategyBot> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find strategy bots by strategyId field
     */
    public java.util.List<StrategyBot> findAllByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find strategy bots by status
     */
    public java.util.List<StrategyBot> findAllByStatus(String status) {
        return findByField("status", status);
    }

    /**
     * Find strategy bots by environment (PAPER or LIVE)
     */
    public java.util.List<StrategyBot> findAllByEnvironment(String environment) {
        return findByField("environment", environment);
    }

    /**
     * Find strategy bots by status and subscription tier
     */
    public java.util.List<StrategyBot> findAllByStatusAndTier(String status, String subscriptionTier) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("subscriptionTier", subscriptionTier)
                .whereEqualTo("auditFields.isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    StrategyBot entity = doc.toObject(StrategyBot.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategyBot");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyBot");
        }
    }

    /**
     * Find active bots by status and environment
     */
    public java.util.List<StrategyBot> findAllByStatusAndEnvironment(String status, String environment) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("environment", environment)
                .whereEqualTo("auditFields.isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    StrategyBot entity = doc.toObject(StrategyBot.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategyBot");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyBot");
        }
    }
}
