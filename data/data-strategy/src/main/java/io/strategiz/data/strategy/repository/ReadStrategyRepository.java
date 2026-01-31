package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy entities Following Single Responsibility
 * Principle - focused only on read operations
 */
public interface ReadStrategyRepository {

	/**
	 * Find a strategy by ID
	 */
	Optional<Strategy> findById(String id);

	/**
	 * Find all strategies for a user
	 */
	List<Strategy> findByUserId(String userId);

	/**
	 * Find strategies by user ID and status
	 */
	List<Strategy> findByUserIdAndStatus(String userId, String status);

	/**
	 * Find strategies by user ID and language
	 */
	List<Strategy> findByUserIdAndLanguage(String userId, String language);

	/**
	 * Find all public strategies
	 */
	List<Strategy> findPublicStrategies();

	/**
	 * Find public strategies by language
	 */
	List<Strategy> findPublicStrategiesByLanguage(String language);

	/**
	 * Find public strategies by tags
	 */
	List<Strategy> findPublicStrategiesByTags(List<String> tags);

	/**
	 * Search strategies by name for a user
	 */
	List<Strategy> searchByName(String userId, String searchTerm);

	/**
	 * Check if a strategy exists by ID
	 */
	boolean existsById(String id);

	/**
	 * Find all versions of a strategy by parent strategy ID
	 */
	List<Strategy> findVersionsByParentId(String parentStrategyId);

	/**
	 * Find the latest version of a strategy
	 */
	Optional<Strategy> findLatestVersion(String parentStrategyId);

	/**
	 * Find strategies by owner and normalized name Used for per-user uniqueness
	 * validation
	 * @param ownerId Owner ID to search for
	 * @param normalizedName Normalized name to match
	 * @return List of strategies matching owner and normalized name
	 */
	List<Strategy> findByOwnerIdAndNormalizedName(String ownerId, String normalizedName);

	/**
	 * Find strategies by normalized name and publish status Used for global uniqueness
	 * validation (published strategies)
	 * @param normalizedName Normalized name to match
	 * @param publishStatus Publish status to filter by
	 * @return List of strategies matching normalized name and publish status
	 */
	List<Strategy> findByNormalizedNameAndPublishStatus(String normalizedName, String publishStatus);

}