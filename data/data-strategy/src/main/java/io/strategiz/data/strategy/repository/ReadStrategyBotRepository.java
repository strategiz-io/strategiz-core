package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy bot entities.
 * Following Single Responsibility Principle - focused only on read operations.
 */
public interface ReadStrategyBotRepository {

    /**
     * Find a strategy bot by ID
     */
    Optional<StrategyBot> findById(String id);

    /**
     * Find all strategy bots for a user
     */
    List<StrategyBot> findByUserId(String userId);

    /**
     * Find strategy bots by user ID and status
     */
    List<StrategyBot> findByUserIdAndStatus(String userId, String status);

    /**
     * Find strategy bots by strategy ID
     */
    List<StrategyBot> findByStrategyId(String strategyId);

    /**
     * Find active strategy bots for a user
     */
    List<StrategyBot> findActiveByUserId(String userId);

    /**
     * Find strategy bots by provider ID
     */
    List<StrategyBot> findByProviderId(String userId, String providerId);

    /**
     * Find strategy bots by environment (PAPER or LIVE)
     */
    List<StrategyBot> findByEnvironment(String userId, String environment);

    /**
     * Check if a strategy bot exists by ID
     */
    boolean existsById(String id);

    /**
     * Count active bots for a user (for subscription limits)
     */
    int countActiveByUserId(String userId);

    /**
     * Find all active strategy bots across all users (for execution service)
     */
    List<StrategyBot> findAllActive();

    /**
     * Find all active bots by subscription tier
     *
     * @param subscriptionTier The tier to filter by (FREE, STARTER, PRO)
     * @return List of active bots for the specified tier
     */
    List<StrategyBot> findActiveBotsByTier(String subscriptionTier);

    /**
     * Find all active paper trading bots
     */
    List<StrategyBot> findActivePaperBots();

    /**
     * Find all active live trading bots
     */
    List<StrategyBot> findActiveLiveBots();
}
