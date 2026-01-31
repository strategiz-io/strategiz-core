package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.base.exception.DataRepositoryException;
import io.strategiz.data.base.exception.DataRepositoryErrorDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of DeleteBotDeploymentRepository.
 */
@Service
public class DeleteBotDeploymentRepositoryImpl implements DeleteBotDeploymentRepository {

	private final BotDeploymentBaseRepository baseRepository;

	public DeleteBotDeploymentRepositoryImpl(BotDeploymentBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public boolean delete(String id, String userId) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return false;
		}

		BotDeployment bot = existing.get();
		if (!userId.equals(bot.getUserId())) {
			return false;
		}

		// Soft delete using BaseRepository's delete method
		return baseRepository.delete(id, userId);
	}

	@Override
	public void hardDelete(String id) {
		// Note: Hard delete not implemented in BaseRepository
		// Use soft delete instead for now
		throw new DataRepositoryException(DataRepositoryErrorDetails.OPERATION_NOT_SUPPORTED, "StrategyBot",
				"Hard delete not supported - use soft delete");
	}

}
