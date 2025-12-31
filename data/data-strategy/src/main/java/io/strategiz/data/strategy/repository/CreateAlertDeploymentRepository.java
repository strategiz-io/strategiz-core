package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeployment;

/**
 * Repository interface for creating strategy alert entities
 * Following Single Responsibility Principle - focused only on create operations
 */
public interface CreateAlertDeploymentRepository {

    /**
     * Create a new strategy alert
     *
     * @param strategyAlert The strategy alert to create
     * @return The created strategy alert
     */
    AlertDeployment create(AlertDeployment strategyAlert);

    /**
     * Create a new strategy alert with a specific user ID
     *
     * @param strategyAlert The strategy alert to create
     * @param userId The user ID
     * @return The created strategy alert
     */
    AlertDeployment createWithUserId(AlertDeployment strategyAlert, String userId);
}
