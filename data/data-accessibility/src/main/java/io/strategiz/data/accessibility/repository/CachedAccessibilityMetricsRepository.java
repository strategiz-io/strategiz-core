package io.strategiz.data.accessibility.repository;

import java.util.List;
import java.util.Optional;

import io.strategiz.data.accessibility.entity.CachedAccessibilityMetricsEntity;

/**
 * Repository for cached accessibility metrics from CI/CD or on-demand scans.
 *
 * Firestore collection: system/accessibility_cache
 */
public interface CachedAccessibilityMetricsRepository {

	/**
	 * Save cached accessibility metrics.
	 * @param entity the metrics to cache
	 */
	void save(CachedAccessibilityMetricsEntity entity);

	/**
	 * Get the latest cached accessibility metrics for a specific app.
	 * @param appId the app identifier (web, auth, console)
	 * @return latest metrics for the app, or empty if none exist
	 */
	Optional<CachedAccessibilityMetricsEntity> getLatestByApp(String appId);

	/**
	 * Get the latest cached accessibility metrics across all apps.
	 * @return latest metrics, or empty if none exist
	 */
	Optional<CachedAccessibilityMetricsEntity> getLatest();

	/**
	 * Get cached accessibility metrics by scan ID.
	 * @param scanId the scan ID
	 * @return metrics, or empty if not found
	 */
	Optional<CachedAccessibilityMetricsEntity> findById(String scanId);

	/**
	 * Get historical scan results for a specific app.
	 * @param appId the app identifier
	 * @param limit maximum number of results to return
	 * @return list of historical scan results, ordered by scannedAt descending
	 */
	List<CachedAccessibilityMetricsEntity> getHistoryByApp(String appId, int limit);

	/**
	 * Get historical scan results across all apps.
	 * @param limit maximum number of results to return
	 * @return list of historical scan results, ordered by scannedAt descending
	 */
	List<CachedAccessibilityMetricsEntity> getHistory(int limit);

	/**
	 * Delete all cached metrics (for cleanup).
	 */
	void deleteAll();

}
