package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for reading strategy comments.
 */
public interface ReadStrategyCommentRepository {

	/**
	 * Find a comment by ID.
	 */
	Optional<StrategyCommentEntity> findById(String id);

	/**
	 * Find all comments for a strategy.
	 */
	List<StrategyCommentEntity> findByStrategyId(String strategyId);

	/**
	 * Find top-level comments for a strategy (paginated).
	 */
	List<StrategyCommentEntity> findTopLevelByStrategyId(String strategyId, int limit);

	/**
	 * Find replies to a comment.
	 */
	List<StrategyCommentEntity> findReplies(String parentCommentId);

	/**
	 * Find all comments by a user.
	 */
	List<StrategyCommentEntity> findByUserId(String userId);

	/**
	 * Count total comments for a strategy.
	 */
	int countByStrategyId(String strategyId);

	/**
	 * Check if a comment exists.
	 */
	boolean existsById(String id);

}
