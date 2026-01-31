package io.strategiz.business.livestrategies.adapter;

import io.strategiz.business.livestrategies.model.Signal;
import io.strategiz.business.livestrategies.service.RiskValidator;
import io.strategiz.client.alpaca.client.AlpacaTradingClient;
import io.strategiz.client.alpaca.client.AlpacaTradingClient.OrderResult;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.repository.BotDeploymentBaseRepository;
import io.strategiz.framework.resilience.circuitbreaker.CircuitBreakerManager;
import io.strategiz.framework.secrets.controller.SecretManager;
import com.google.cloud.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Routes bot signals to trading execution via Alpaca. Validates risk parameters before
 * executing trades. Supports both paper and live trading modes.
 */
@Component
public class BotSignalAdapter implements SignalAdapter {

	private static final Logger log = LoggerFactory.getLogger(BotSignalAdapter.class);

	private final RiskValidator riskValidator;

	private final BotDeploymentBaseRepository botDeploymentRepository;

	private final AlpacaTradingClient alpacaTradingClient;

	private final SecretManager secretManager;

	private final CircuitBreakerManager circuitBreakerManager;

	public BotSignalAdapter(RiskValidator riskValidator, BotDeploymentBaseRepository botDeploymentRepository,
			AlpacaTradingClient alpacaTradingClient, @Qualifier("vaultSecretService") SecretManager secretManager,
			CircuitBreakerManager circuitBreakerManager) {
		this.riskValidator = riskValidator;
		this.botDeploymentRepository = botDeploymentRepository;
		this.alpacaTradingClient = alpacaTradingClient;
		this.secretManager = secretManager;
		this.circuitBreakerManager = circuitBreakerManager;
		log.info("BotSignalAdapter initialized with Alpaca trading client");
	}

	@Override
	public boolean canHandle(Signal signal) {
		return signal != null && signal.isBotSignal();
	}

	@Override
	public SignalResult process(Signal signal) {
		if (!canHandle(signal)) {
			return SignalResult.skipped(signal.getDeploymentId(), "Not a bot signal");
		}

		// Skip non-actionable signals (HOLD)
		if (!signal.isActionable()) {
			return SignalResult.skipped(signal.getDeploymentId(), "Signal is HOLD, no trade needed");
		}

		String botId = signal.getDeploymentId();
		log.info("Processing bot signal: {} {} @ {} for deployment {}", signal.getType(), signal.getSymbol(),
				signal.getPrice(), botId);

		try {
			// 1. Load bot deployment
			Optional<BotDeployment> optBot = botDeploymentRepository.findById(botId);
			if (optBot.isEmpty()) {
				log.warn("Bot deployment not found: {}", botId);
				return SignalResult.failure(botId, "UNKNOWN", "Bot deployment not found", null);
			}

			BotDeployment bot = optBot.get();

			// 2. Check if bot is active
			if (!"ACTIVE".equals(bot.getStatus())) {
				log.info("Bot {} is not active (status={}), skipping", botId, bot.getStatus());
				return SignalResult.skipped(botId, "Bot is not active: " + bot.getStatus());
			}

			// 3. Check circuit breaker state
			if (circuitBreakerManager.isOpen(bot)) {
				log.warn("Circuit breaker is OPEN for bot {}, skipping trade", botId);
				return SignalResult.skipped(botId, "Circuit breaker is OPEN - too many consecutive errors");
			}

			// 4. Check daily trade limit
			if (bot.isDailyLimitReached()) {
				log.info("Bot {} has reached daily trade limit, skipping", botId);
				return SignalResult.skipped(botId, "Daily trade limit reached");
			}

			// 5. Validate risk parameters
			RiskValidator.RiskCheckResult riskCheck = riskValidator.validate(signal);
			if (!riskCheck.passed()) {
				log.warn("Risk check failed for bot {}: {}", botId, riskCheck.reason());
				return SignalResult.skipped(botId, "Risk check failed: " + riskCheck.reason());
			}

			// 6. Check if bot is in simulated mode
			if (bot.isSimulated()) {
				return processSimulatedTrade(bot, signal);
			}

			// 7. Execute real trade via Alpaca
			return executeAlpacaTrade(bot, signal);

		}
		catch (Exception e) {
			log.error("Error processing bot signal for {}: {}", botId, e.getMessage(), e);
			// Record failure for circuit breaker
			recordFailure(botId, e.getMessage());
			return SignalResult.failure(botId, "TRADE", e.getMessage(), e);
		}
	}

	/**
	 * Process a simulated trade (no real order execution). Used for testing strategies
	 * before going live.
	 */
	private SignalResult processSimulatedTrade(BotDeployment bot, Signal signal) {
		String botId = bot.getId();
		String environment = bot.isPaperTrading() ? "PAPER" : "LIVE";

		log.info("[SIMULATED] {} {} {} @ {} for bot {} (env={})", signal.isBuy() ? "BUY" : "SELL",
				calculatePositionSize(bot, signal), signal.getSymbol(), signal.getPrice(), botId, environment);

		// Update bot state
		updateBotAfterTrade(bot, signal, true);

		return SignalResult.success(botId, "SIMULATED_" + environment,
				String.format("Simulated %s %s @ %.2f", signal.getType(), signal.getSymbol(), signal.getPrice()));
	}

	/**
	 * Execute a real trade via Alpaca Trading API.
	 */
	private SignalResult executeAlpacaTrade(BotDeployment bot, Signal signal) {
		String botId = bot.getId();
		String userId = bot.getUserId();
		boolean isPaper = bot.isPaperTrading();
		String environment = isPaper ? "paper" : "live";

		// Get OAuth access token from Vault
		String accessToken = getAlpacaAccessToken(userId, environment);
		if (accessToken == null) {
			log.error("No Alpaca access token found for user {} (env={})", userId, environment);
			recordFailure(botId, "No Alpaca access token found");
			return SignalResult.failure(botId, environment.toUpperCase(),
					"No Alpaca access token. Please reconnect your Alpaca account.", null);
		}

		// Determine order parameters
		String side = signal.isBuy() ? "buy" : "sell";
		double qty = calculatePositionSize(bot, signal);
		String symbol = signal.getSymbol();

		log.info("Executing Alpaca {} order: {} {} shares of {} (paper={})", side, qty, symbol, symbol, isPaper);

		// Execute market order
		OrderResult orderResult = alpacaTradingClient.placeMarketOrder(accessToken, symbol, side, qty, isPaper);

		if (orderResult.isSuccess()) {
			log.info("Alpaca order submitted: orderId={}, status={}, symbol={}, side={}", orderResult.getOrderId(),
					orderResult.getStatus(), symbol, side);

			// Record success for circuit breaker
			recordSuccess(bot);

			// Update bot state
			updateBotAfterTrade(bot, signal, true);

			String message = String.format("Executed %s %s @ %s (orderId=%s, status=%s)", signal.getType(), symbol,
					orderResult.getFilledAvgPrice() > 0 ? orderResult.getFilledAvgPrice() : "market",
					orderResult.getOrderId(), orderResult.getStatus());

			return SignalResult.success(botId, isPaper ? "PAPER" : "LIVE", message);
		}
		else {
			log.error("Alpaca order failed for bot {}: {}", botId, orderResult.getError());

			// Record failure for circuit breaker
			recordFailure(botId, orderResult.getError());

			// Update bot state with error
			updateBotAfterTrade(bot, signal, false);
			bot.setErrorMessage(orderResult.getError());
			botDeploymentRepository.save(bot, userId);

			return SignalResult.failure(botId, isPaper ? "PAPER" : "LIVE", orderResult.getError(), null);
		}
	}

	/**
	 * Get Alpaca OAuth access token from Vault. Path:
	 * secret/strategiz/users/{userId}/providers/alpaca-{environment}
	 */
	private String getAlpacaAccessToken(String userId, String environment) {
		try {
			String secretPath = "secret/strategiz/users/" + userId + "/providers/alpaca-" + environment;
			Map<String, Object> secretData = secretManager.readSecretAsMap(secretPath);

			if (secretData == null || secretData.isEmpty()) {
				// Fall back to generic alpaca path (no environment suffix)
				secretPath = "secret/strategiz/users/" + userId + "/providers/alpaca";
				secretData = secretManager.readSecretAsMap(secretPath);
			}

			if (secretData != null && secretData.containsKey("accessToken")) {
				return (String) secretData.get("accessToken");
			}

			return null;
		}
		catch (Exception e) {
			log.error("Failed to retrieve Alpaca access token for user {}: {}", userId, e.getMessage());
			return null;
		}
	}

	/**
	 * Calculate position size based on bot configuration.
	 */
	private double calculatePositionSize(BotDeployment bot, Signal signal) {
		Double maxSize = bot.getMaxPositionSize();
		if (maxSize != null && maxSize > 0) {
			// Calculate shares based on max position value and current price
			if (signal.getPrice() > 0) {
				return Math.floor(maxSize / signal.getPrice());
			}
		}

		// Default to 1 share if no position sizing configured
		return 1.0;
	}

	/**
	 * Update bot state after a trade attempt.
	 */
	private void updateBotAfterTrade(BotDeployment bot, Signal signal, boolean success) {
		try {
			Timestamp now = Timestamp.now();
			bot.setLastCheckedAt(now);

			if (success) {
				bot.setLastExecutedAt(now);
				bot.setTotalTrades(bot.getTotalTrades() != null ? bot.getTotalTrades() + 1 : 1);
				bot.setDailyTradeCount(bot.getDailyTradeCount() != null ? bot.getDailyTradeCount() + 1 : 1);
				bot.setErrorMessage(null);
			}

			botDeploymentRepository.save(bot, bot.getUserId());
		}
		catch (Exception e) {
			log.warn("Failed to update bot state for {}: {}", bot.getId(), e.getMessage());
		}
	}

	/**
	 * Record a successful operation for circuit breaker.
	 */
	private void recordSuccess(BotDeployment bot) {
		try {
			circuitBreakerManager.recordSuccess(bot);
			botDeploymentRepository.save(bot, bot.getUserId());
		}
		catch (Exception e) {
			log.warn("Failed to record success for circuit breaker: {}", e.getMessage());
		}
	}

	/**
	 * Record a failure for circuit breaker.
	 */
	private void recordFailure(String botId, String errorMessage) {
		try {
			Optional<BotDeployment> optBot = botDeploymentRepository.findById(botId);
			if (optBot.isPresent()) {
				BotDeployment bot = optBot.get();
				circuitBreakerManager.recordFailure(bot, errorMessage);

				// Check if circuit breaker should trip
				if (bot.shouldTripCircuitBreaker()) {
					log.warn("Circuit breaker tripped for bot {} after {} consecutive errors", botId,
							bot.getConsecutiveErrors());
					bot.setStatus("PAUSED");
					bot.setErrorMessage("Paused due to consecutive errors: " + errorMessage);
				}

				botDeploymentRepository.save(bot, bot.getUserId());
			}
		}
		catch (Exception e) {
			log.warn("Failed to record failure for circuit breaker: {}", e.getMessage());
		}
	}

}
