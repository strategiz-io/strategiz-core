package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategySubscriptionEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for strategy subscription operations.
 *
 * Supports queries:
 * - Subscribe to a strategy
 * - Cancel subscription
 * - Check subscription status
 * - Get user's subscriptions
 * - Get strategy's subscribers
 */
public interface StrategySubscriptionRepository {

    // === Create Operations ===

    /**
     * Create a new subscription.
     *
     * @param subscription The subscription entity to create
     * @param userId       Who is performing this action
     * @return The created subscription entity
     */
    StrategySubscriptionEntity create(StrategySubscriptionEntity subscription, String userId);

    // === Read Operations ===

    /**
     * Find a subscription by ID.
     */
    Optional<StrategySubscriptionEntity> findById(String subscriptionId);

    /**
     * Find a user's subscription to a specific strategy.
     */
    Optional<StrategySubscriptionEntity> findByUserAndStrategy(String userId, String strategyId);

    /**
     * Check if a user has an active subscription to a strategy.
     */
    boolean hasActiveSubscription(String userId, String strategyId);

    /**
     * Get a user's subscriptions (with pagination).
     *
     * @param userId The user ID
     * @param limit  Maximum number of results
     * @return List of subscription entities
     */
    List<StrategySubscriptionEntity> getUserSubscriptions(String userId, int limit);

    /**
     * Get subscribers for a strategy (with pagination).
     *
     * @param strategyId The strategy ID
     * @param limit      Maximum number of results
     * @return List of subscription entities
     */
    List<StrategySubscriptionEntity> getStrategySubscribers(String strategyId, int limit);

    /**
     * Count active subscribers for a strategy.
     */
    int countActiveSubscribers(String strategyId);

    // === Update Operations ===

    /**
     * Update a subscription.
     *
     * @param subscription The subscription to update
     * @param userId       Who is performing this action
     * @return The updated subscription entity
     */
    StrategySubscriptionEntity update(StrategySubscriptionEntity subscription, String userId);

    /**
     * Cancel a subscription (remains valid until expiry).
     *
     * @param subscriptionId The subscription ID
     * @param userId         Who is performing this action
     * @return The cancelled subscription entity
     */
    StrategySubscriptionEntity cancel(String subscriptionId, String userId);

    // === Delete Operations ===

    /**
     * Soft delete a subscription.
     *
     * @param subscriptionId The subscription ID
     * @param userId         Who is performing this action
     * @return True if deleted
     */
    boolean delete(String subscriptionId, String userId);
}
