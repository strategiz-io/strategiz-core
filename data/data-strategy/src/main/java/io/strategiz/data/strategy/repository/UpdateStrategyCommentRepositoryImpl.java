package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of UpdateStrategyCommentRepository.
 */
@Repository
public class UpdateStrategyCommentRepositoryImpl implements UpdateStrategyCommentRepository {

    private final StrategyCommentBaseRepository baseRepository;

    @Autowired
    public UpdateStrategyCommentRepositoryImpl(StrategyCommentBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public StrategyCommentEntity update(StrategyCommentEntity comment, String userId) {
        // Mark as edited
        comment.setEditedAt(Timestamp.now());
        return baseRepository.save(comment, userId);
    }

    @Override
    public void incrementLikes(String commentId, String userId) {
        baseRepository.findById(commentId).ifPresent(comment -> {
            comment.incrementLikes();
            baseRepository.save(comment, userId);
        });
    }

    @Override
    public void decrementLikes(String commentId, String userId) {
        baseRepository.findById(commentId).ifPresent(comment -> {
            comment.decrementLikes();
            baseRepository.save(comment, userId);
        });
    }

    @Override
    public void incrementReplies(String commentId, String userId) {
        baseRepository.findById(commentId).ifPresent(comment -> {
            comment.incrementReplies();
            baseRepository.save(comment, userId);
        });
    }

    @Override
    public void decrementReplies(String commentId, String userId) {
        baseRepository.findById(commentId).ifPresent(comment -> {
            comment.decrementReplies();
            baseRepository.save(comment, userId);
        });
    }
}
