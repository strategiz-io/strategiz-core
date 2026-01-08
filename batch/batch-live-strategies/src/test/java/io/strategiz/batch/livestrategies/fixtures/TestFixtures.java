package io.strategiz.batch.livestrategies.fixtures;

import io.strategiz.batch.livestrategies.model.DeploymentBatchMessage;
import io.strategiz.business.livestrategies.model.SymbolSetGroup;
import io.strategiz.data.strategy.entity.AlertDeployment;
import io.strategiz.data.strategy.entity.BotDeployment;
import io.strategiz.data.strategy.entity.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test fixtures for live strategies batch tests.
 */
public final class TestFixtures {

	private TestFixtures() {
		// Utility class
	}

	// ===== Strategy Fixtures =====

	public static Strategy createStrategy(String id, String ownerId, String code) {
		Strategy strategy = new Strategy();
		strategy.setId(id);
		strategy.setOwnerId(ownerId);
		strategy.setName("Test Strategy " + id);
		strategy.setCode(code);
		// Note: createdDate/modifiedDate are set by repository layer in production
		return strategy;
	}

	public static Strategy createRsiStrategy(String id, String ownerId) {
		return createStrategy(id, ownerId, RSI_STRATEGY_CODE);
	}

	public static Strategy createMacdStrategy(String id, String ownerId) {
		return createStrategy(id, ownerId, MACD_STRATEGY_CODE);
	}

	// ===== Alert Deployment Fixtures =====

	public static AlertDeployment createAlertDeployment(String id, String userId, String strategyId,
			List<String> symbols, String tier) {
		AlertDeployment alert = new AlertDeployment();
		alert.setId(id);
		alert.setUserId(userId);
		alert.setStrategyId(strategyId);
		alert.setSymbols(symbols);
		alert.setSubscriptionTier(tier);
		alert.setStatus("ACTIVE");
		// Note: createdDate/modifiedDate are set by repository layer in production
		return alert;
	}

	public static AlertDeployment createAlertDeployment(String userId, String strategyId, String symbol, String tier) {
		return createAlertDeployment(UUID.randomUUID().toString(), userId, strategyId, List.of(symbol), tier);
	}

	// ===== Bot Deployment Fixtures =====

	public static BotDeployment createBotDeployment(String id, String userId, String strategyId, List<String> symbols,
			String tier, String environment) {
		BotDeployment bot = new BotDeployment();
		bot.setId(id);
		bot.setUserId(userId);
		bot.setStrategyId(strategyId);
		bot.setSymbols(symbols);
		bot.setSubscriptionTier(tier);
		bot.setEnvironment(environment);
		bot.setStatus("ACTIVE");
		// Note: createdDate/modifiedDate are set by repository layer in production
		return bot;
	}

	public static BotDeployment createPaperBot(String userId, String strategyId, String symbol, String tier) {
		return createBotDeployment(UUID.randomUUID().toString(), userId, strategyId, List.of(symbol), tier, "PAPER");
	}

	public static BotDeployment createLiveBot(String userId, String strategyId, String symbol, String tier) {
		return createBotDeployment(UUID.randomUUID().toString(), userId, strategyId, List.of(symbol), tier, "LIVE");
	}

	// ===== Symbol Set Group Fixtures =====

	public static SymbolSetGroup createSymbolSetGroup(List<String> symbols, List<String> alertIds,
			List<String> botIds) {
		SymbolSetGroup group = new SymbolSetGroup(symbols);
		for (String alertId : alertIds) {
			group.addAlert(alertId);
		}
		for (String botId : botIds) {
			group.addBot(botId);
		}
		return group;
	}

	public static SymbolSetGroup createSymbolSetGroup(String symbol, String alertId) {
		return createSymbolSetGroup(List.of(symbol), List.of(alertId), List.of());
	}

	public static SymbolSetGroup createSymbolSetGroup(String symbol, List<String> alertIds, List<String> botIds) {
		return createSymbolSetGroup(List.of(symbol), alertIds, botIds);
	}

	// ===== Batch Message Fixtures =====

	public static DeploymentBatchMessage createBatchMessage(String tier, List<SymbolSetGroup> symbolSets) {
		return DeploymentBatchMessage.builder().tier(tier).symbolSets(symbolSets).build();
	}

	public static DeploymentBatchMessage createSingleSymbolBatch(String tier, String symbol, String alertId) {
		List<SymbolSetGroup> symbolSets = new ArrayList<>();
		symbolSets.add(createSymbolSetGroup(symbol, alertId));
		return createBatchMessage(tier, symbolSets);
	}

	// ===== Sample Strategy Code =====

	public static final String RSI_STRATEGY_CODE = """
			import pandas as pd

			def strategy(data):
			    close = data['close']
			    rsi = calculate_rsi(close, 14)
			    if rsi.iloc[-1] < 30:
			        return 'BUY'
			    elif rsi.iloc[-1] > 70:
			        return 'SELL'
			    return 'HOLD'

			def calculate_rsi(prices, period):
			    delta = prices.diff()
			    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
			    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
			    rs = gain / loss
			    return 100 - (100 / (1 + rs))
			""";

	public static final String MACD_STRATEGY_CODE = """
			import pandas as pd

			def strategy(data):
			    close = data['close']
			    macd_line, signal_line = calculate_macd(close)
			    if macd_line.iloc[-1] > signal_line.iloc[-1]:
			        return 'BUY'
			    elif macd_line.iloc[-1] < signal_line.iloc[-1]:
			        return 'SELL'
			    return 'HOLD'

			def calculate_macd(prices):
			    ema12 = prices.ewm(span=12).mean()
			    ema26 = prices.ewm(span=26).mean()
			    macd_line = ema12 - ema26
			    signal_line = macd_line.ewm(span=9).mean()
			    return macd_line, signal_line
			""";

	public static final String SIMPLE_BUY_STRATEGY_CODE = """
			def strategy(data):
			    return 'BUY'
			""";

	public static final String SIMPLE_SELL_STRATEGY_CODE = """
			def strategy(data):
			    return 'SELL'
			""";

	public static final String SIMPLE_HOLD_STRATEGY_CODE = """
			def strategy(data):
			    return 'HOLD'
			""";

}
