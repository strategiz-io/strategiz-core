package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateStrategyCommentRepository.
 */
@Repository
public class CreateStrategyCommentRepositoryImpl implements CreateStrategyCommentRepository {

	private final StrategyCommentBaseRepository baseRepository;

	@Autowired
	public CreateStrategyCommentRepositoryImpl(StrategyCommentBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public StrategyCommentEntity create(StrategyCommentEntity comment, String userId) {
		// Generate ID if not provided
		if (comment.getId() == null || comment.getId().isEmpty()) {
			comment.setId(UUID.randomUUID().toString());
		}

		// Set user ID
		comment.setUserId(userId);

		// Set commented timestamp
		if (comment.getCommentedAt() == null) {
			comment.setCommentedAt(Timestamp.now());
		}

		// Initialize counts
		if (comment.getLikeCount() == null) {
			comment.setLikeCount(0);
		}
		if (comment.getReplyCount() == null) {
			comment.setReplyCount(0);
		}

		return baseRepository.save(comment, userId);
	}

	@Override
	public StrategyCommentEntity createReply(StrategyCommentEntity comment, String parentCommentId, String userId) {
		comment.setParentCommentId(parentCommentId);
		return create(comment, userId);
	}

}
