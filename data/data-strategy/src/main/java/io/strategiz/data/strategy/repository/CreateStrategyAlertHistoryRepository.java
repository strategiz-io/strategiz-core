package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;

/**
 * Repository interface for creating strategy alert history entities
 * Following Single Responsibility Principle - focused only on create operations
 */
public interface CreateStrategyAlertHistoryRepository {

    /**
     * Create a new alert history record
     *
     * @param alertHistory The alert history to create
     * @return The created alert history
     */
    StrategyAlertHistory create(StrategyAlertHistory alertHistory);

    /**
     * Create a new alert history record with a specific user ID
     *
     * @param alertHistory The alert history to create
     * @param userId The user ID
     * @return The created alert history
     */
    StrategyAlertHistory createWithUserId(StrategyAlertHistory alertHistory, String userId);
}
