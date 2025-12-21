package io.strategiz.data.strategy.repository;

/**
 * Repository interface for deleting strategy comments.
 */
public interface DeleteStrategyCommentRepository {

    /**
     * Soft delete a comment by ID.
     *
     * @param commentId the ID of the comment to delete
     * @param userId the ID of the user performing the deletion
     */
    void delete(String commentId, String userId);

    /**
     * Hard delete a comment by ID (permanent).
     *
     * @param commentId the ID of the comment to delete
     */
    void hardDelete(String commentId);

    /**
     * Delete all comments for a strategy (used when strategy is deleted).
     *
     * @param strategyId the strategy ID
     * @param userId the ID of the user performing the deletion
     */
    void deleteByStrategyId(String strategyId, String userId);
}
