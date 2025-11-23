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
}
