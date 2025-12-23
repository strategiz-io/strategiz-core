package io.strategiz.data.featureflags.repository;

import io.strategiz.data.featureflags.entity.FeatureFlagEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for feature flags.
 * Flags are stored in system/config/feature_flags collection.
 */
public interface FeatureFlagRepository {

    /**
     * Find a feature flag by ID.
     */
    Optional<FeatureFlagEntity> findById(String flagId);

    /**
     * Find all feature flags.
     */
    List<FeatureFlagEntity> findAll();

    /**
     * Find all feature flags by category.
     */
    List<FeatureFlagEntity> findByCategory(String category);

    /**
     * Save or update a feature flag.
     */
    FeatureFlagEntity save(FeatureFlagEntity flag);

    /**
     * Delete a feature flag.
     */
    void delete(String flagId);

    /**
     * Check if a feature is enabled.
     */
    boolean isEnabled(String flagId);
}
