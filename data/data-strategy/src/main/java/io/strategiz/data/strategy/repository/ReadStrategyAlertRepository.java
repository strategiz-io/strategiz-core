package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy alert entities
 * Following Single Responsibility Principle - focused only on read operations
 */
public interface ReadStrategyAlertRepository {

    /**
     * Find a strategy alert by ID
     */
    Optional<StrategyAlert> findById(String id);

    /**
     * Find all strategy alerts for a user
     */
    List<StrategyAlert> findByUserId(String userId);

    /**
     * Find strategy alerts by user ID and status
     */
    List<StrategyAlert> findByUserIdAndStatus(String userId, String status);

    /**
     * Find strategy alerts by strategy ID
     */
    List<StrategyAlert> findByStrategyId(String strategyId);

    /**
     * Find active strategy alerts for a user
     */
    List<StrategyAlert> findActiveByUserId(String userId);

    /**
     * Find strategy alerts by provider ID
     */
    List<StrategyAlert> findByProviderId(String userId, String providerId);

    /**
     * Find strategy alerts by subscription tier
     */
    List<StrategyAlert> findBySubscriptionTier(String userId, String subscriptionTier);

    /**
     * Check if a strategy alert exists by ID
     */
    boolean existsById(String id);

    /**
     * Count active alerts for a user (for subscription limits)
     */
    int countActiveByUserId(String userId);

    /**
     * Find all active strategy alerts across all users (for monitoring service)
     */
    List<StrategyAlert> findAllActive();

    /**
     * Find all active ALERT deployments by subscription tier (for tier-based scheduling)
     * Only returns alerts with deploymentType=ALERT (not BOT or PAPER)
     *
     * @param subscriptionTier The tier to filter by (FREE, STARTER, PRO)
     * @return List of active alerts for the specified tier
     */
    List<StrategyAlert> findActiveAlertsByTier(String subscriptionTier);

    /**
     * Find all active alerts that are due for evaluation based on their evaluation frequency.
     * This is a more efficient approach that lets the database filter by last checked time.
     *
     * @param maxMinutesSinceLastCheck Maximum minutes since last check
     * @return List of active alerts that haven't been checked within the specified time
     */
    List<StrategyAlert> findActiveAlertsDueForEvaluation(int maxMinutesSinceLastCheck);
}
