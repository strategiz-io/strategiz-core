package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyAlertHistory;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for updating strategy alert history entities
 * Following Single Responsibility Principle - focused only on update operations
 */
public interface UpdateStrategyAlertHistoryRepository {

    /**
     * Update an alert history record
     */
    StrategyAlertHistory update(String id, String userId, StrategyAlertHistory alertHistory);

    /**
     * Mark notification as sent
     */
    Optional<StrategyAlertHistory> markNotificationSent(String id, String userId);

    /**
     * Update metadata
     */
    Optional<StrategyAlertHistory> updateMetadata(String id, String userId, Map<String, Object> metadata);

    /**
     * Add metadata field
     */
    Optional<StrategyAlertHistory> addMetadataField(String id, String userId, String key, Object value);
}
