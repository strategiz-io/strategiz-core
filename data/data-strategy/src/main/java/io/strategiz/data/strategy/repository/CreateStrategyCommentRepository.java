package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;

/**
 * Repository interface for creating strategy comments.
 */
public interface CreateStrategyCommentRepository {

    /**
     * Create a new comment.
     *
     * @param comment the comment to create
     * @param userId the ID of the user creating the comment
     * @return the created comment with generated ID
     */
    StrategyCommentEntity create(StrategyCommentEntity comment, String userId);

    /**
     * Create a reply to an existing comment.
     *
     * @param comment the reply comment
     * @param parentCommentId the ID of the parent comment
     * @param userId the ID of the user creating the reply
     * @return the created reply with generated ID
     */
    StrategyCommentEntity createReply(StrategyCommentEntity comment, String parentCommentId, String userId);
}
