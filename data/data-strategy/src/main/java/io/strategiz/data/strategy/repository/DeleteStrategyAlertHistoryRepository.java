package io.strategiz.data.strategy.repository;

/**
 * Repository interface for deleting strategy alert history entities
 * Following Single Responsibility Principle - focused only on delete operations
 */
public interface DeleteStrategyAlertHistoryRepository {

    /**
     * Soft delete an alert history record
     *
     * @param id The history ID
     * @param userId The user ID (for authorization)
     * @return True if deleted, false if not found or unauthorized
     */
    boolean delete(String id, String userId);

    /**
     * Delete all history records for an alert
     *
     * @param alertId The alert ID
     * @param userId The user ID (for authorization)
     * @return Number of records deleted
     */
    int deleteByAlertId(String alertId, String userId);

    /**
     * Restore a soft-deleted alert history record
     *
     * @param id The history ID
     * @param userId The user ID (for authorization)
     * @return True if restored, false if not found or unauthorized
     */
    boolean restore(String id, String userId);
}
