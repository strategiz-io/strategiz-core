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
}
