package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.AlertDeployment;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

/**
 * Base repository for AlertDeployment entities using Firestore
 * Used internally by CRUD repository implementations
 */
@Repository
public class AlertDeploymentBaseRepository extends BaseRepository<AlertDeployment> {

    public AlertDeploymentBaseRepository(Firestore firestore) {
        super(firestore, AlertDeployment.class);
    }

    /**
     * Find strategy alerts by userId field
     */
    public java.util.List<AlertDeployment> findAllByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find strategy alerts by strategyId field
     */
    public java.util.List<AlertDeployment> findAllByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find strategy alerts by status
     */
    public java.util.List<AlertDeployment> findAllByStatus(String status) {
        return findByField("status", status);
    }

    /**
     * Find strategy alerts by status and subscription tier
     */
    public java.util.List<AlertDeployment> findAllByStatusAndTier(String status, String subscriptionTier) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("subscriptionTier", subscriptionTier)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    AlertDeployment entity = doc.toObject(AlertDeployment.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "AlertDeployment");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AlertDeployment");
        }
    }

    /**
     * Find strategy alerts by status and deployment type
     */
    public java.util.List<AlertDeployment> findAllByStatusAndDeploymentType(String status, String deploymentType) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", status)
                .whereEqualTo("deploymentType", deploymentType)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    AlertDeployment entity = doc.toObject(AlertDeployment.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "AlertDeployment");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AlertDeployment");
        }
    }

    /**
     * Find active alerts by status, deployment type, and subscription tier
     */
    public java.util.List<AlertDeployment> findActiveAlertsByTierAndType(String subscriptionTier, String deploymentType) {
        try {
            com.google.cloud.firestore.Query query = getCollection()
                .whereEqualTo("status", "ACTIVE")
                .whereEqualTo("subscriptionTier", subscriptionTier)
                .whereEqualTo("deploymentType", deploymentType)
                .whereEqualTo("isActive", true);

            java.util.List<com.google.cloud.firestore.QueryDocumentSnapshot> docs = query.get().get().getDocuments();

            return docs.stream()
                .map(doc -> {
                    AlertDeployment entity = doc.toObject(AlertDeployment.class);
                    entity.setId(doc.getId());
                    return entity;
                })
                .collect(java.util.stream.Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "AlertDeployment");
        } catch (java.util.concurrent.ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "AlertDeployment");
        }
    }
}
