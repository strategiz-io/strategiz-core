package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlert;

/**
 * Repository interface for creating strategy alert entities
 * Following Single Responsibility Principle - focused only on create operations
 */
public interface CreateStrategyAlertRepository {

    /**
     * Create a new strategy alert
     *
     * @param strategyAlert The strategy alert to create
     * @return The created strategy alert
     */
    StrategyAlert create(StrategyAlert strategyAlert);

    /**
     * Create a new strategy alert with a specific user ID
     *
     * @param strategyAlert The strategy alert to create
     * @param userId The user ID
     * @return The created strategy alert
     */
    StrategyAlert createWithUserId(StrategyAlert strategyAlert, String userId);
}
