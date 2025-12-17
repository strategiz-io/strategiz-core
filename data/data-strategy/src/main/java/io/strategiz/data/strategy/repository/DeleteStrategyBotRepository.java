package io.strategiz.data.strategy.repository;

/**
 * Repository interface for deleting strategy bot entities.
 * Following Single Responsibility Principle - focused only on delete operations.
 */
public interface DeleteStrategyBotRepository {

    /**
     * Delete a strategy bot by ID, verifying ownership
     * @return true if deleted, false if not found or not owned by user
     */
    boolean delete(String id, String userId);

    /**
     * Hard delete a strategy bot by ID (admin only)
     */
    void hardDelete(String id);
}
