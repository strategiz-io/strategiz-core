package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.AlertDeploymentHistory;

import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for updating strategy alert history entities Following Single
 * Responsibility Principle - focused only on update operations
 */
public interface UpdateAlertDeploymentHistoryRepository {

	/**
	 * Update an alert history record
	 */
	AlertDeploymentHistory update(String id, String userId, AlertDeploymentHistory alertHistory);

	/**
	 * Mark notification as sent
	 */
	Optional<AlertDeploymentHistory> markNotificationSent(String id, String userId);

	/**
	 * Update metadata
	 */
	Optional<AlertDeploymentHistory> updateMetadata(String id, String userId, Map<String, Object> metadata);

	/**
	 * Add metadata field
	 */
	Optional<AlertDeploymentHistory> addMetadataField(String id, String userId, String key, Object value);

}
