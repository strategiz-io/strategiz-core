package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.BotDeployment;
import com.google.cloud.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of UpdateBotDeploymentRepository.
 */
@Service
public class UpdateBotDeploymentRepositoryImpl implements UpdateBotDeploymentRepository {

	private static final Logger logger = LoggerFactory.getLogger(UpdateBotDeploymentRepositoryImpl.class);

	private final BotDeploymentBaseRepository baseRepository;

	public UpdateBotDeploymentRepositoryImpl(BotDeploymentBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public BotDeployment update(BotDeployment bot) {
		return baseRepository.save(bot, bot.getUserId());
	}

	@Override
	public boolean updateStatus(String id, String userId, String status) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return false;
		}

		BotDeployment bot = existing.get();
		if (!userId.equals(bot.getUserId())) {
			return false;
		}

		bot.setStatus(status);
		baseRepository.save(bot, bot.getUserId());
		return true;
	}

	@Override
	public void recordTrade(String id, boolean isProfitable, double pnl) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			logger.warn("Bot not found for trade recording: {}", id);
			return;
		}

		BotDeployment bot = existing.get();

		// Update trade counts
		Integer totalTrades = bot.getTotalTrades() != null ? bot.getTotalTrades() : 0;
		bot.setTotalTrades(totalTrades + 1);

		if (isProfitable) {
			Integer profitableTrades = bot.getProfitableTrades() != null ? bot.getProfitableTrades() : 0;
			bot.setProfitableTrades(profitableTrades + 1);
		}

		// Update total PnL
		Double totalPnL = bot.getTotalPnL() != null ? bot.getTotalPnL() : 0.0;
		bot.setTotalPnL(totalPnL + pnl);

		// Update execution timestamp
		bot.setLastExecutedAt(Timestamp.now());

		// Reset consecutive errors on successful trade
		bot.setConsecutiveErrors(0);

		// Increment daily trade count
		Integer dailyCount = bot.getDailyTradeCount() != null ? bot.getDailyTradeCount() : 0;
		bot.setDailyTradeCount(dailyCount + 1);

		baseRepository.save(bot, bot.getUserId());
		logger.info("Recorded trade for bot {}: profitable={}, pnl={}", id, isProfitable, pnl);
	}

	@Override
	public void incrementConsecutiveErrors(String id) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return;
		}

		BotDeployment bot = existing.get();
		Integer errors = bot.getConsecutiveErrors() != null ? bot.getConsecutiveErrors() : 0;
		bot.setConsecutiveErrors(errors + 1);

		// Check if circuit breaker should trip
		if (bot.shouldTripCircuitBreaker()) {
			bot.setStatus("ERROR");
			bot.setErrorMessage("Circuit breaker tripped: too many consecutive errors");
			logger.warn("Circuit breaker tripped for bot {}", id);
		}

		baseRepository.save(bot, bot.getUserId());
	}

	@Override
	public void resetConsecutiveErrors(String id) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return;
		}

		BotDeployment bot = existing.get();
		bot.setConsecutiveErrors(0);
		bot.setErrorMessage(null);
		baseRepository.save(bot, bot.getUserId());
	}

	@Override
	public void incrementDailyTradeCount(String id) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return;
		}

		BotDeployment bot = existing.get();
		Integer count = bot.getDailyTradeCount() != null ? bot.getDailyTradeCount() : 0;
		bot.setDailyTradeCount(count + 1);
		baseRepository.save(bot, bot.getUserId());
	}

	@Override
	public void resetDailyTradeCount(String id) {
		Optional<BotDeployment> existing = baseRepository.findById(id);
		if (existing.isEmpty()) {
			return;
		}

		BotDeployment bot = existing.get();
		bot.setDailyTradeCount(0);
		bot.setLastDailyReset(Timestamp.now());
		baseRepository.save(bot, bot.getUserId());
	}

}
