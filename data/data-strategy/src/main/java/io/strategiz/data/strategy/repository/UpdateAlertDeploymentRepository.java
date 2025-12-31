package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;
import com.google.cloud.Timestamp;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for updating strategy alert entities
 * Following Single Responsibility Principle - focused only on update operations
 */
public interface UpdateStrategyAlertRepository {

    /**
     * Update a strategy alert
     */
    StrategyAlert update(String id, String userId, StrategyAlert strategyAlert);

    /**
     * Update alert status
     */
    boolean updateStatus(String id, String userId, String status);

    /**
     * Update alert symbols
     */
    Optional<StrategyAlert> updateSymbols(String id, String userId, List<String> symbols);

    /**
     * Update notification channels
     */
    Optional<StrategyAlert> updateNotificationChannels(String id, String userId, List<String> notificationChannels);

    /**
     * Update last checked timestamp
     */
    Optional<StrategyAlert> updateLastCheckedAt(String id, String userId, Timestamp timestamp);

    /**
     * Update last triggered timestamp and increment trigger count
     */
    Optional<StrategyAlert> recordTrigger(String id, String userId);

    /**
     * Update error message
     */
    Optional<StrategyAlert> updateErrorMessage(String id, String userId, String errorMessage);

    /**
     * Clear error message
     */
    Optional<StrategyAlert> clearErrorMessage(String id, String userId);

    /**
     * Pause an alert (set status to PAUSED)
     */
    boolean pauseAlert(String id, String userId);

    /**
     * Resume an alert (set status to ACTIVE)
     */
    boolean resumeAlert(String id, String userId);

    // ========================
    // Cooldown & Deduplication
    // ========================

    /**
     * Record a signal for deduplication tracking.
     * Updates lastSignalType, lastSignalSymbol, and lastTriggeredAt.
     *
     * @param id Alert ID
     * @param userId User ID
     * @param signalType The signal type (BUY, SELL, HOLD)
     * @param symbol The symbol that triggered
     * @return Updated alert if successful
     */
    Optional<StrategyAlert> recordSignal(String id, String userId, String signalType, String symbol);

    // ========================
    // Circuit Breaker
    // ========================

    /**
     * Increment consecutive error count for circuit breaker.
     * If threshold is reached, status should be changed to ERROR.
     *
     * @param id Alert ID
     * @param userId User ID
     * @param errorMessage The error message
     * @return Updated alert with new error count
     */
    Optional<StrategyAlert> incrementConsecutiveErrors(String id, String userId, String errorMessage);

    /**
     * Reset consecutive error count (called on successful execution).
     *
     * @param id Alert ID
     * @param userId User ID
     * @return Updated alert with reset error count
     */
    Optional<StrategyAlert> resetConsecutiveErrors(String id, String userId);

    // ========================
    // Rate Limiting
    // ========================

    /**
     * Increment daily trigger count.
     * Also updates lastTriggeredAt and triggerCount.
     *
     * @param id Alert ID
     * @param userId User ID
     * @return Updated alert with incremented count
     */
    Optional<StrategyAlert> incrementDailyTriggerCount(String id, String userId);

    /**
     * Reset daily trigger count (called at midnight or when lastDailyReset is stale).
     *
     * @param id Alert ID
     * @param userId User ID
     * @return Updated alert with reset count
     */
    Optional<StrategyAlert> resetDailyTriggerCount(String id, String userId);
}
