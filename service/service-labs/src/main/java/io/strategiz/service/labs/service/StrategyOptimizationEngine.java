package io.strategiz.service.labs.service;

import io.strategiz.business.historicalinsights.model.DeploymentInsights;
import io.strategiz.business.historicalinsights.model.OptimizationResult;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import io.strategiz.business.historicalinsights.model.StrategyType;
import io.strategiz.business.historicalinsights.service.DeploymentInsightsCalculator;
import io.strategiz.business.historicalinsights.template.StrategyCodeTemplates;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Strategy Optimization Engine that tests ~200 strategy combinations with various parameters,
 * ranks them by TOTAL RETURN (not win rate), and returns the best performer.
 *
 * This replaces the old deterministic strategy generation which only tested 4 indicators
 * with fixed parameters and ranked by win rate.
 */
@Service
public class StrategyOptimizationEngine {

	private static final Logger log = LoggerFactory.getLogger(StrategyOptimizationEngine.class);

	// Thread pool for parallel backtesting
	private static final int THREAD_POOL_SIZE = 8;

	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

	// Default optimization period
	private static final String DEFAULT_PERIOD = "3y";

	private final StrategyExecutionService executionService;

	private final DeploymentInsightsCalculator deploymentInsightsCalculator;

	public StrategyOptimizationEngine(StrategyExecutionService executionService,
			DeploymentInsightsCalculator deploymentInsightsCalculator) {
		this.executionService = executionService;
		this.deploymentInsightsCalculator = deploymentInsightsCalculator;
	}

	/**
	 * Main optimization method. Generates all parameter combinations, runs backtests in parallel,
	 * and returns the best strategy by total return.
	 * @param symbol The trading symbol (e.g., "AAPL", "BTC")
	 * @param timeframe The chart timeframe (e.g., "1D", "1h")
	 * @param period The backtest period (e.g., "3y", "5y")
	 * @param userId User ID for tracking
	 * @return OptimizationResult with best strategy and top alternatives
	 */
	public OptimizationResult optimize(String symbol, String timeframe, String period, String userId) {
		log.info("Starting strategy optimization for symbol={}, timeframe={}, period={}", symbol, timeframe, period);
		long startTime = System.currentTimeMillis();

		// Use default period if not specified
		String effectivePeriod = period != null ? period : DEFAULT_PERIOD;

		// Generate all parameter combinations
		List<StrategyTestResult> combinations = generateAllCombinations();
		log.info("Generated {} strategy combinations to test", combinations.size());

		// Run backtests in parallel
		List<StrategyTestResult> results = runParallelBacktests(combinations, symbol, timeframe, effectivePeriod, userId);

		// Filter successful results and sort by total return (descending)
		List<StrategyTestResult> successfulResults = results.stream()
			.filter(StrategyTestResult::isSuccess)
			.filter(r -> r.getTotalTrades() >= 3) // Require minimum trades
			.sorted() // Uses Comparable (by total return desc)
			.collect(Collectors.toList());

		log.info("Completed {} successful backtests out of {}", successfulResults.size(), results.size());

		// Build result
		OptimizationResult optimizationResult = new OptimizationResult(symbol, timeframe,
				calculateDaysFromPeriod(effectivePeriod));

		optimizationResult.setTotalCombinationsTested(combinations.size());
		optimizationResult.setSuccessfulTests(successfulResults.size());

		// Get buy-and-hold baseline from first successful result
		double buyAndHoldReturn = 0.0;
		if (!successfulResults.isEmpty()) {
			// Execute a simple buy-and-hold to get baseline
			buyAndHoldReturn = calculateBuyAndHoldReturn(symbol, timeframe, effectivePeriod, userId);
			optimizationResult.setBuyAndHoldReturn(buyAndHoldReturn);
		}

		// Set best strategy
		if (!successfulResults.isEmpty()) {
			StrategyTestResult best = successfulResults.get(0);
			optimizationResult.setBestStrategy(best);
			optimizationResult.setOutperformance(best.getTotalReturn() - buyAndHoldReturn);

			// Set top 5 strategies
			List<StrategyTestResult> top5 = successfulResults.stream().limit(5).collect(Collectors.toList());
			optimizationResult.setTopStrategies(top5);

			// Calculate deployment insights for the best strategy
			int daysAnalyzed = calculateDaysFromPeriod(effectivePeriod);
			DeploymentInsights insights = deploymentInsightsCalculator.calculate(best, daysAnalyzed);
			optimizationResult.setDeploymentInsights(insights);
		}

		// Determine market regime from results
		optimizationResult.setMarketRegime(determineMarketRegime(successfulResults));

		// Set execution stats
		long totalTime = System.currentTimeMillis() - startTime;
		optimizationResult.setTotalExecutionTimeMs(totalTime);
		optimizationResult.setAvgExecutionTimePerStrategy(
				combinations.isEmpty() ? 0 : (double) totalTime / combinations.size());

		log.info("Optimization complete in {}ms. Best strategy: {} with {:.2f}% return", totalTime,
				optimizationResult.getBestStrategy() != null
						? optimizationResult.getBestStrategy().getStrategyType().getDisplayName()
						: "NONE",
				optimizationResult.getBestStrategy() != null ? optimizationResult.getBestStrategy().getTotalReturn()
						: 0.0);

		return optimizationResult;
	}

	/**
	 * Generates all parameter combinations for all strategy types. Total: ~200 combinations.
	 */
	private List<StrategyTestResult> generateAllCombinations() {
		List<StrategyTestResult> combinations = new ArrayList<>();

		// RSI Mean Reversion: 4 x 4 x 4 = 64 combinations
		int[] rsiPeriods = { 7, 10, 14, 21 };
		int[] rsiOversold = { 20, 25, 30, 35 };
		int[] rsiOverbought = { 65, 70, 75, 80 };
		for (int period : rsiPeriods) {
			for (int oversold : rsiOversold) {
				for (int overbought : rsiOverbought) {
					if (overbought > oversold + 30) { // Ensure valid range
						combinations.add(new StrategyTestResult(StrategyType.RSI_MEAN_REVERSION,
								Map.of("period", period, "oversold", oversold, "overbought", overbought,
										"atr_multiplier", 2.0)));
					}
				}
			}
		}

		// MACD Trend Following: 3 x 3 x 3 = 27 combinations
		int[] macdFast = { 8, 10, 12 };
		int[] macdSlow = { 20, 26, 30 };
		int[] macdSignal = { 7, 9, 12 };
		for (int fast : macdFast) {
			for (int slow : macdSlow) {
				for (int signal : macdSignal) {
					if (slow > fast) {
						combinations.add(new StrategyTestResult(StrategyType.MACD_TREND_FOLLOWING,
								Map.of("fast", fast, "slow", slow, "signal_period", signal, "atr_multiplier", 2.0)));
					}
				}
			}
		}

		// Bollinger Mean Reversion: 4 x 3 = 12 combinations
		int[] bbPeriods = { 10, 15, 20, 25 };
		double[] bbStdMult = { 1.5, 2.0, 2.5 };
		for (int period : bbPeriods) {
			for (double stdMult : bbStdMult) {
				combinations.add(new StrategyTestResult(StrategyType.BOLLINGER_MEAN_REVERSION,
						Map.of("period", period, "std_mult", stdMult, "atr_multiplier", 2.0)));
			}
		}

		// Bollinger Breakout: 4 x 3 = 12 combinations
		for (int period : bbPeriods) {
			for (double stdMult : bbStdMult) {
				combinations.add(new StrategyTestResult(StrategyType.BOLLINGER_BREAKOUT,
						Map.of("period", period, "std_mult", stdMult, "atr_multiplier", 2.0)));
			}
		}

		// EMA Crossover: 3 x 4 = 12 combinations
		int[] maFast = { 5, 10, 20 };
		int[] maSlow = { 20, 50, 100, 200 };
		for (int fast : maFast) {
			for (int slow : maSlow) {
				if (slow > fast) {
					combinations.add(new StrategyTestResult(StrategyType.MA_CROSSOVER_EMA,
							Map.of("fast", fast, "slow", slow, "atr_multiplier", 2.0)));
				}
			}
		}

		// SMA Crossover: 3 x 4 = 12 combinations
		for (int fast : maFast) {
			for (int slow : maSlow) {
				if (slow > fast) {
					combinations.add(new StrategyTestResult(StrategyType.MA_CROSSOVER_SMA,
							Map.of("fast", fast, "slow", slow, "atr_multiplier", 2.0)));
				}
			}
		}

		// Stochastic: 3 x 2 x 3 = 18 combinations
		int[] stochK = { 10, 14, 20 };
		int[] stochD = { 3, 5 };
		int[][] stochLevels = { { 20, 80 }, { 25, 75 }, { 30, 70 } };
		for (int k : stochK) {
			for (int d : stochD) {
				for (int[] levels : stochLevels) {
					combinations.add(new StrategyTestResult(StrategyType.STOCHASTIC,
							Map.of("k_period", k, "d_period", d, "oversold", levels[0], "overbought", levels[1],
									"atr_multiplier", 2.0)));
				}
			}
		}

		// Swing Trading: 4 x 4 = 16 combinations
		int[] buyThresh = { 5, 8, 10, 12 };
		int[] sellThresh = { 8, 12, 15, 20 };
		for (int buy : buyThresh) {
			for (int sell : sellThresh) {
				if (sell > buy) {
					combinations.add(new StrategyTestResult(StrategyType.SWING_TRADING,
							Map.of("buy_threshold", buy, "sell_threshold", sell, "lookback", 20, "atr_multiplier",
									2.0)));
				}
			}
		}

		// Combined ADX: 3 x 3 x 3 = ~20 combinations (selected subset)
		int[] adxThresh = { 20, 25, 30 };
		int[] combinedRsiPeriod = { 10, 14, 21 };
		int[] combinedRsiOversold = { 25, 30, 35 };
		for (int adx : adxThresh) {
			for (int rsiP : combinedRsiPeriod) {
				for (int rsiOS : combinedRsiOversold) {
					combinations.add(new StrategyTestResult(StrategyType.COMBINED_ADX,
							Map.of("adx_threshold", adx, "rsi_period", rsiP, "rsi_oversold", rsiOS, "rsi_overbought",
									100 - rsiOS, "atr_multiplier", 2.0)));
				}
			}
		}

		log.info("Generated {} total strategy combinations", combinations.size());
		return combinations;
	}

	/**
	 * Runs backtests in parallel using the thread pool.
	 */
	private List<StrategyTestResult> runParallelBacktests(List<StrategyTestResult> combinations, String symbol,
			String timeframe, String period, String userId) {

		List<CompletableFuture<StrategyTestResult>> futures = combinations.stream().map(combo -> CompletableFuture
			.supplyAsync(() -> runSingleBacktest(combo, symbol, timeframe, period, userId), EXECUTOR)
			.exceptionally(ex -> {
				log.warn("Backtest failed for {}: {}", combo.getStrategyType(), ex.getMessage());
				return StrategyTestResult.failed(combo.getStrategyType(), combo.getParameters(), ex.getMessage());
			})).collect(Collectors.toList());

		// Wait for all to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		// Collect results
		return futures.stream().map(f -> {
			try {
				return f.get();
			}
			catch (Exception e) {
				return StrategyTestResult.failed(StrategyType.RSI_MEAN_REVERSION, Map.of(), e.getMessage());
			}
		}).collect(Collectors.toList());
	}

	/**
	 * Runs a single backtest for a strategy combination.
	 */
	private StrategyTestResult runSingleBacktest(StrategyTestResult combo, String symbol, String timeframe,
			String period, String userId) {
		long startTime = System.currentTimeMillis();

		try {
			// Generate Python code from template
			String pythonCode = StrategyCodeTemplates.generateCode(combo.getStrategyType(), combo.getParameters());
			combo.setPythonCode(pythonCode);

			// Execute strategy via gRPC
			ExecuteStrategyResponse response = executionService.executeStrategy(pythonCode, "python", symbol, timeframe,
					period, userId, null);

			// Extract performance metrics
			if (response != null && response.getPerformance() != null) {
				ExecuteStrategyResponse.Performance perf = response.getPerformance();
				combo.setTotalReturn(perf.getTotalReturn());
				combo.setWinRate(perf.getWinRate() / 100.0); // Convert from percentage
				combo.setMaxDrawdown(perf.getMaxDrawdown());
				combo.setSharpeRatio(perf.getSharpeRatio());
				combo.setProfitFactor(perf.getProfitFactor());
				combo.setTotalTrades(perf.getTotalTrades());
				combo.setSuccess(true);
			}
			else if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
				combo.setSuccess(false);
				combo.setErrorMessage(String.join(", ", response.getErrors()));
			}
			else {
				combo.setSuccess(false);
				combo.setErrorMessage("No performance data returned");
			}
		}
		catch (Exception e) {
			log.debug("Backtest error for {} with params {}: {}", combo.getStrategyType(), combo.getParameters(),
					e.getMessage());
			combo.setSuccess(false);
			combo.setErrorMessage(e.getMessage());
		}

		combo.setExecutionTimeMs(System.currentTimeMillis() - startTime);
		return combo;
	}

	/**
	 * Calculates buy-and-hold return for baseline comparison.
	 */
	private double calculateBuyAndHoldReturn(String symbol, String timeframe, String period, String userId) {
		try {
			// Simple buy-and-hold strategy - buy at start, sell at end
			String buyAndHoldCode = """
					# Buy and Hold Strategy (Baseline)
					import pandas as pd

					# Simply buy at the start
					first_row = data.iloc[0]
					signal('BUY', first_row['timestamp'], first_row['close'], 'Start of period', 'arrow_up')

					# Sell at the end
					last_row = data.iloc[-1]
					signal('SELL', last_row['timestamp'], last_row['close'], 'End of period', 'arrow_down')
					""";

			ExecuteStrategyResponse response = executionService.executeStrategy(buyAndHoldCode, "python", symbol,
					timeframe, period, userId, null);

			if (response != null && response.getPerformance() != null) {
				return response.getPerformance().getTotalReturn();
			}
		}
		catch (Exception e) {
			log.warn("Failed to calculate buy-and-hold return: {}", e.getMessage());
		}
		return 0.0;
	}

	/**
	 * Determines the market regime based on optimization results.
	 */
	private String determineMarketRegime(List<StrategyTestResult> results) {
		if (results.isEmpty()) {
			return "UNKNOWN";
		}

		// Count strategy types in top 10
		Map<StrategyType, Long> topTypeCount = results.stream()
			.limit(10)
			.collect(Collectors.groupingBy(StrategyTestResult::getStrategyType, Collectors.counting()));

		// Determine regime based on which strategies work best
		long trendFollowingCount = topTypeCount.getOrDefault(StrategyType.MACD_TREND_FOLLOWING, 0L)
				+ topTypeCount.getOrDefault(StrategyType.MA_CROSSOVER_EMA, 0L)
				+ topTypeCount.getOrDefault(StrategyType.MA_CROSSOVER_SMA, 0L);

		long meanReversionCount = topTypeCount.getOrDefault(StrategyType.RSI_MEAN_REVERSION, 0L)
				+ topTypeCount.getOrDefault(StrategyType.BOLLINGER_MEAN_REVERSION, 0L)
				+ topTypeCount.getOrDefault(StrategyType.STOCHASTIC, 0L);

		long breakoutCount = topTypeCount.getOrDefault(StrategyType.BOLLINGER_BREAKOUT, 0L)
				+ topTypeCount.getOrDefault(StrategyType.SWING_TRADING, 0L);

		if (trendFollowingCount > meanReversionCount && trendFollowingCount > breakoutCount) {
			return "TRENDING";
		}
		else if (meanReversionCount > trendFollowingCount && meanReversionCount > breakoutCount) {
			return "SIDEWAYS";
		}
		else if (breakoutCount > 3) {
			return "VOLATILE";
		}
		else {
			return "MIXED";
		}
	}

	/**
	 * Calculates approximate days from period string.
	 */
	private int calculateDaysFromPeriod(String period) {
		if (period == null)
			return 1095; // Default 3 years

		String lower = period.toLowerCase();
		if (lower.contains("y")) {
			try {
				int years = Integer.parseInt(lower.replace("y", ""));
				return years * 365;
			}
			catch (NumberFormatException e) {
				return 1095;
			}
		}
		else if (lower.contains("m")) {
			try {
				int months = Integer.parseInt(lower.replace("m", "").replace("o", ""));
				return months * 30;
			}
			catch (NumberFormatException e) {
				return 1095;
			}
		}
		return 1095;
	}

	/**
	 * Shuts down the executor service gracefully.
	 */
	public void shutdown() {
		EXECUTOR.shutdown();
		try {
			if (!EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
				EXECUTOR.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			EXECUTOR.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

}
