package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;

/**
 * Repository interface for updating strategy comments.
 */
public interface UpdateStrategyCommentRepository {

    /**
     * Update a comment.
     *
     * @param comment the comment to update
     * @param userId the ID of the user making the update
     * @return the updated comment
     */
    StrategyCommentEntity update(StrategyCommentEntity comment, String userId);

    /**
     * Increment the like count for a comment.
     */
    void incrementLikes(String commentId, String userId);

    /**
     * Decrement the like count for a comment.
     */
    void decrementLikes(String commentId, String userId);

    /**
     * Increment the reply count for a comment.
     */
    void incrementReplies(String commentId, String userId);

    /**
     * Decrement the reply count for a comment.
     */
    void decrementReplies(String commentId, String userId);
}
