package io.strategiz.data.quality.repository;

import java.util.Optional;

import io.strategiz.data.quality.entity.CachedQualityMetricsEntity;

/**
 * Repository for cached quality metrics from build-time analysis.
 *
 * Firestore collection: system/quality_cache
 */
public interface CachedQualityMetricsRepository {

	/**
	 * Save cached quality metrics.
	 * @param entity the metrics to cache
	 */
	void save(CachedQualityMetricsEntity entity);

	/**
	 * Get the latest cached quality metrics.
	 * @return latest metrics, or empty if none exist
	 */
	Optional<CachedQualityMetricsEntity> getLatest();

	/**
	 * Get cached quality metrics by analysis ID.
	 * @param analysisId the analysis ID
	 * @return metrics, or empty if not found
	 */
	Optional<CachedQualityMetricsEntity> findById(String analysisId);

	/**
	 * Delete all cached metrics (for cleanup).
	 */
	void deleteAll();

}
