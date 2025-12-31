package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategyAlert;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for StrategyAlert entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class StrategyAlertBaseRepository extends BaseRepository<StrategyAlert> {

    public StrategyAlertBaseRepository(Firestore firestore) {
        super(firestore, StrategyAlert.class);
    }

    /**
     * Find strategy alerts by userId field
     */
    public java.util.List<StrategyAlert> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find strategy alerts by strategyId field
     */
    public java.util.List<StrategyAlert> findAllByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find strategy alerts by status
     */
    public java.util.List<StrategyAlert> findAllByStatus(String status) {
        return findByField("status", status);
    }

    /**
     * Find strategy alerts by status and subscription tier
     */
    public java.util.List<StrategyAlert> findAllByStatusAndTier(String status, String subscriptionTier) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("subscriptionTier", subscriptionTier)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    StrategyAlert entity = doc.toObject(StrategyAlert.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategyAlert");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyAlert");
        }
    }

    /**
     * Find strategy alerts by status and deployment type
     */
    public java.util.List<StrategyAlert> findAllByStatusAndDeploymentType(String status, String deploymentType) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("deploymentType", deploymentType)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    StrategyAlert entity = doc.toObject(StrategyAlert.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategyAlert");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyAlert");
        }
    }

    /**
     * Find active alerts by status, deployment type, and subscription tier
     */
    public java.util.List<StrategyAlert> findActiveAlertsByTierAndType(String subscriptionTier, String deploymentType) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("subscriptionTier", subscriptionTier)
                .whereEqualTo("deploymentType", deploymentType)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    StrategyAlert entity = doc.toObject(StrategyAlert.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategyAlert");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategyAlert");
        }
    }
}
