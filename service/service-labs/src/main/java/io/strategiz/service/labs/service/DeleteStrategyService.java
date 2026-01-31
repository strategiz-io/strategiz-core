package io.strategiz.service.labs.service;

import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.data.strategy.repository.DeleteStrategyRepository;
import io.strategiz.data.strategy.repository.ReadStrategyRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.labs.exception.ServiceStrategyErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for deleting strategies with proper access control
 */
@Service
public class DeleteStrategyService extends BaseService {

	private final DeleteStrategyRepository deleteStrategyRepository;

	private final ReadStrategyRepository readStrategyRepository;

	@Autowired
	public DeleteStrategyService(DeleteStrategyRepository deleteStrategyRepository,
			ReadStrategyRepository readStrategyRepository) {
		this.deleteStrategyRepository = deleteStrategyRepository;
		this.readStrategyRepository = readStrategyRepository;
	}

	@Override
	protected String getModuleName() {
		return "service-labs";
	}

	/**
	 * Delete a strategy with proper access control.
	 *
	 * Access rules: - Only the owner can delete their strategy - Cannot delete if
	 * strategy has active subscribers
	 * @param strategyId The strategy ID
	 * @param userId The user ID (must be owner)
	 * @return true if deleted successfully
	 * @throws StrategizException if strategy not found, user not owner, or has
	 * subscribers
	 */
	public boolean deleteStrategy(String strategyId, String userId) {
		log.info("Deleting strategy: {} for user: {}", strategyId, userId);

		// Check if strategy exists
		Strategy strategy = readStrategyRepository.findById(strategyId)
			.orElseThrow(() -> new StrategizException(ServiceStrategyErrorDetails.STRATEGY_NOT_FOUND, getModuleName(),
					"Strategy not found: " + strategyId));

		// Check if user is the owner (only owner can delete)
		if (!strategy.isOwner(userId)) {
			throwModuleException(ServiceStrategyErrorDetails.STRATEGY_MODIFICATION_DENIED,
					"Only the strategy owner can delete this strategy");
		}

		// Check if strategy has active subscribers (cannot delete if so)
		if (strategy.getSubscriberCount() != null && strategy.getSubscriberCount() > 0) {
			log.warn("Cannot delete strategy {} - has {} active subscribers", strategyId,
					strategy.getSubscriberCount());
			throwModuleException(ServiceStrategyErrorDetails.STRATEGY_HAS_SUBSCRIBERS,
					"Cannot delete strategy with active subscribers. Cancel all subscriptions first.");
		}

		// Perform deletion
		boolean deleted = deleteStrategyRepository.deleteByIdAndUserId(strategyId, userId);

		if (deleted) {
			log.info("Successfully deleted strategy: {} for user: {}", strategyId, userId);
		}
		else {
			log.error("Failed to delete strategy: {} for user: {}", strategyId, userId);
		}

		return deleted;
	}

	/**
	 * Delete all strategies for a user WARNING: This is a dangerous operation and should
	 * be used with caution
	 * @param userId The user ID
	 * @return The number of strategies deleted
	 */
	public int deleteAllUserStrategies(String userId) {
		log.warn("Deleting ALL strategies for user: {}", userId);

		int deletedCount = deleteStrategyRepository.deleteAllByUserId(userId);

		log.info("Deleted {} strategies for user: {}", deletedCount, userId);
		return deletedCount;
	}

}