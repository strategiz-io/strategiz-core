package io.strategiz.data.strategy.repository;

/**
 * Repository interface for deleting strategy entities Following Single Responsibility
 * Principle - focused only on delete operations
 */
public interface DeleteStrategyRepository {

	/**
	 * Delete a strategy by ID and user ID (soft delete)
	 */
	boolean deleteByIdAndUserId(String id, String userId);

	/**
	 * Delete all strategies for a user (soft delete) Warning: This is a dangerous
	 * operation and should be used carefully
	 */
	int deleteAllByUserId(String userId);

	/**
	 * Permanently delete a strategy (hard delete) Warning: This cannot be undone
	 */
	boolean hardDeleteByIdAndUserId(String id, String userId);

}