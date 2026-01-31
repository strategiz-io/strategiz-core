package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of DeleteStrategyRepository using BaseRepository
 */
@Repository
public class DeleteStrategyRepositoryImpl implements DeleteStrategyRepository {

	private final StrategyBaseRepository baseRepository;

	@Autowired
	public DeleteStrategyRepositoryImpl(StrategyBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public boolean deleteByIdAndUserId(String id, String userId) {
		// Note: Strategies are stored in user subcollections, so userId scoping is
		// enforced by the repository
		Optional<Strategy> existing = baseRepository.findById(id);
		if (existing.isEmpty() || !Boolean.TRUE.equals(existing.get().getIsActive())) {
			return false;
		}

		// Use BaseRepository's soft delete
		return baseRepository.delete(id, userId);
	}

	@Override
	public int deleteAllByUserId(String userId) {
		List<Strategy> strategies = baseRepository.findAllByUserId(userId);
		int deletedCount = 0;

		for (Strategy strategy : strategies) {
			if (Boolean.TRUE.equals(strategy.getIsActive())) {
				if (baseRepository.delete(strategy.getId(), userId)) {
					deletedCount++;
				}
			}
		}

		return deletedCount;
	}

	@Override
	public boolean hardDeleteByIdAndUserId(String id, String userId) {
		// TODO: Implement hard delete if needed
		// For now, just do soft delete
		return deleteByIdAndUserId(id, userId);
	}

}