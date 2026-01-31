package io.strategiz.data.preferences.repository;

import io.strategiz.data.preferences.entity.UserPreferenceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring Data repository for users/{userId}/preferences subcollection
 */
@Repository
public interface UserPreferenceRepository extends CrudRepository<UserPreferenceEntity, String> {

	// ===============================
	// Spring Data Query Methods
	// ===============================

	/**
	 * Find preferences by category
	 */
	List<UserPreferenceEntity> findByCategory(String category);

	/**
	 * Find preference by category (single result)
	 */
	Optional<UserPreferenceEntity> findFirstByCategory(String category);

	/**
	 * Check if category exists
	 */
	boolean existsByCategory(String category);

	/**
	 * Count preferences by category
	 */
	long countByCategory(String category);

	/**
	 * Delete by category
	 */
	void deleteByCategory(String category);

	// ===============================
	// Category-Specific Convenience Methods
	// ===============================

	/**
	 * Find theme preference
	 */
	default Optional<UserPreferenceEntity> findThemePreference() {
		return findFirstByCategory("theme");
	}

	/**
	 * Find notification preference
	 */
	default Optional<UserPreferenceEntity> findNotificationPreference() {
		return findFirstByCategory("notifications");
	}

	/**
	 * Find trading preference
	 */
	default Optional<UserPreferenceEntity> findTradingPreference() {
		return findFirstByCategory("trading");
	}

	/**
	 * Find display preference
	 */
	default Optional<UserPreferenceEntity> findDisplayPreference() {
		return findFirstByCategory("display");
	}

	/**
	 * Find privacy preference
	 */
	default Optional<UserPreferenceEntity> findPrivacyPreference() {
		return findFirstByCategory("privacy");
	}

	/**
	 * Find AI preference (model selection, etc.)
	 */
	default Optional<UserPreferenceEntity> findAIPreference() {
		return findFirstByCategory("ai");
	}

}