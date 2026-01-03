package io.strategiz.data.strategy.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;
import io.strategiz.data.strategy.entity.SubscriptionStatus;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Base repository for StrategySubscription entities using Firestore.
 * Used internally by CRUD repository implementations.
 */
@Repository
public class StrategySubscriptionBaseRepository extends BaseRepository<StrategySubscriptionEntity> {

    public StrategySubscriptionBaseRepository(Firestore firestore) {
        super(firestore, StrategySubscriptionEntity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-strategy";
    }

    /**
     * Find all subscriptions for a user.
     */
    public List<StrategySubscriptionEntity> findByUserId(String userId) {
        return findByField("userId", userId);
    }

    /**
     * Find all subscriptions for a strategy.
     */
    public List<StrategySubscriptionEntity> findByStrategyId(String strategyId) {
        return findByField("strategyId", strategyId);
    }

    /**
     * Find all subscriptions created by a specific creator.
     */
    public List<StrategySubscriptionEntity> findByCreatorId(String creatorId) {
        return findByField("creatorId", creatorId);
    }

    /**
     * Find a user's subscription to a specific strategy.
     */
    public Optional<StrategySubscriptionEntity> findByUserAndStrategy(String userId, String strategyId) {
        List<StrategySubscriptionEntity> subscriptions = findByUserId(userId);
        return subscriptions.stream()
                .filter(s -> s.getStrategyId().equals(strategyId))
                .findFirst();
    }

    /**
     * Check if a user has an active subscription to a strategy.
     */
    public boolean hasActiveSubscription(String userId, String strategyId) {
        return findByUserAndStrategy(userId, strategyId)
                .map(StrategySubscriptionEntity::isValid)
                .orElse(false);
    }

    /**
     * Find active subscriptions for a user with pagination.
     */
    public List<StrategySubscriptionEntity> findActiveByUserId(String userId, int limit) {
        try {
            List<QueryDocumentSnapshot> docs = getCollection()
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isActive", true)
                    .whereIn("status", Arrays.asList(
                            SubscriptionStatus.ACTIVE.name(),
                            SubscriptionStatus.TRIAL.name(),
                            SubscriptionStatus.CANCELLED.name()
                    ))
                    .orderBy("startedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            return docs.stream()
                    .map(doc -> {
                        StrategySubscriptionEntity entity = doc.toObject(StrategySubscriptionEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategySubscriptionEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategySubscriptionEntity");
        }
    }

    /**
     * Find active subscribers for a strategy with pagination.
     */
    public List<StrategySubscriptionEntity> findActiveByStrategyId(String strategyId, int limit) {
        try {
            List<QueryDocumentSnapshot> docs = getCollection()
                    .whereEqualTo("strategyId", strategyId)
                    .whereEqualTo("isActive", true)
                    .whereIn("status", Arrays.asList(
                            SubscriptionStatus.ACTIVE.name(),
                            SubscriptionStatus.TRIAL.name()
                    ))
                    .orderBy("startedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            return docs.stream()
                    .map(doc -> {
                        StrategySubscriptionEntity entity = doc.toObject(StrategySubscriptionEntity.class);
                        entity.setId(doc.getId());
                        return entity;
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataRepositoryException(DataRepositoryErrorDetails.FIRESTORE_OPERATION_INTERRUPTED, e, "StrategySubscriptionEntity");
        } catch (ExecutionException e) {
            throw new DataRepositoryException(DataRepositoryErrorDetails.QUERY_EXECUTION_FAILED, e, "StrategySubscriptionEntity");
        }
    }

    /**
     * Count active subscribers for a strategy.
     */
    public int countActiveSubscribers(String strategyId) {
        return (int) findByStrategyId(strategyId).stream()
                .filter(StrategySubscriptionEntity::isValid)
                .count();
    }
}
