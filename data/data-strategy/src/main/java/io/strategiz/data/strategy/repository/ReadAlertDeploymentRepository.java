package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeployment;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy alert entities
 * Following Single Responsibility Principle - focused only on read operations
 */
public interface ReadAlertDeploymentRepository {

    /**
     * Find a strategy alert by ID
     */
    Optional<AlertDeployment> findById(String id);

    /**
     * Find all strategy alerts for a user
     */
    List<AlertDeployment> findByUserId(String userId);

    /**
     * Find strategy alerts by user ID and status
     */
    List<AlertDeployment> findByUserIdAndStatus(String userId, String status);

    /**
     * Find strategy alerts by strategy ID
     */
    List<AlertDeployment> findByStrategyId(String strategyId);

    /**
     * Find active strategy alerts for a user
     */
    List<AlertDeployment> findActiveByUserId(String userId);

    /**
     * Find strategy alerts by provider ID
     */
    List<AlertDeployment> findByProviderId(String userId, String providerId);

    /**
     * Find strategy alerts by subscription tier
     */
    List<AlertDeployment> findBySubscriptionTier(String userId, String subscriptionTier);

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
    List<AlertDeployment> findAllActive();

    /**
     * Find all active ALERT deployments by subscription tier (for tier-based scheduling)
     * Only returns alerts with deploymentType=ALERT (not BOT or PAPER)
     *
     * @param subscriptionTier The tier to filter by (FREE, STARTER, PRO)
     * @return List of active alerts for the specified tier
     */
    List<AlertDeployment> findActiveAlertsByTier(String subscriptionTier);

    /**
     * Find all active alerts that are due for evaluation based on their evaluation frequency.
     * This is a more efficient approach that lets the database filter by last checked time.
     *
     * @param maxMinutesSinceLastCheck Maximum minutes since last check
     * @return List of active alerts that haven't been checked within the specified time
     */
    List<AlertDeployment> findActiveAlertsDueForEvaluation(int maxMinutesSinceLastCheck);
}
