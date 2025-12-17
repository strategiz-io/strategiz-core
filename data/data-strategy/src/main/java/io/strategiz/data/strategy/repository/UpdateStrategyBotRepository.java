package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyBot;

/**
 * Repository interface for updating strategy bot entities.
 * Following Single Responsibility Principle - focused only on update operations.
 */
public interface UpdateStrategyBotRepository {

    /**
     * Update an existing strategy bot
     */
    StrategyBot update(StrategyBot bot);

    /**
     * Update the status of a strategy bot
     * @return true if updated, false if not found or not owned by user
     */
    boolean updateStatus(String id, String userId, String status);

    /**
     * Record a successful trade execution
     */
    void recordTrade(String id, boolean isProfitable, double pnl);

    /**
     * Increment consecutive errors (for circuit breaker)
     */
    void incrementConsecutiveErrors(String id);

    /**
     * Reset consecutive errors to 0
     */
    void resetConsecutiveErrors(String id);

    /**
     * Increment daily trade count
     */
    void incrementDailyTradeCount(String id);

    /**
     * Reset daily trade count to 0
     */
    void resetDailyTradeCount(String id);
}
