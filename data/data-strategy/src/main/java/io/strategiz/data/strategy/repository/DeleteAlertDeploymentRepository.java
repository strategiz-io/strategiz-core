package io.strategiz.data.strategy.repository;

/**
 * Repository interface for deleting strategy alert entities
 * Following Single Responsibility Principle - focused only on delete operations
 */
public interface DeleteAlertDeploymentRepository {

    /**
     * Soft delete a strategy alert
     *
     * @param id The alert ID
     * @param userId The user ID (for authorization)
     * @return True if deleted, false if not found or unauthorized
     */
    boolean delete(String id, String userId);

    /**
     * Stop and delete an alert (sets status to STOPPED then soft deletes)
     *
     * @param id The alert ID
     * @param userId The user ID (for authorization)
     * @return True if deleted, false if not found or unauthorized
     */
    boolean stopAndDelete(String id, String userId);

    /**
     * Restore a soft-deleted strategy alert
     *
     * @param id The alert ID
     * @param userId The user ID (for authorization)
     * @return True if restored, false if not found or unauthorized
     */
    boolean restore(String id, String userId);
}
