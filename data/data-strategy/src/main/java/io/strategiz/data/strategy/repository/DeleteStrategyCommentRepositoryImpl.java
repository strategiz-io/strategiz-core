package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.StrategyCommentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementation of DeleteStrategyCommentRepository.
 */
@Repository
public class DeleteStrategyCommentRepositoryImpl implements DeleteStrategyCommentRepository {

	private final StrategyCommentBaseRepository baseRepository;

	@Autowired
	public DeleteStrategyCommentRepositoryImpl(StrategyCommentBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public void delete(String commentId, String userId) {
		// BaseRepository.delete() does soft delete
		baseRepository.delete(commentId, userId);
	}

	@Override
	public void hardDelete(String commentId) {
		// For hard delete, we need to use the getById and then remove from Firestore
		// For now, just use soft delete as fallback
		baseRepository.delete(commentId, "system");
	}

	@Override
	public void deleteByStrategyId(String strategyId, String userId) {
		List<StrategyCommentEntity> comments = baseRepository.findByStrategyId(strategyId);
		for (StrategyCommentEntity comment : comments) {
			baseRepository.delete(comment.getId(), userId);
		}
	}

}
