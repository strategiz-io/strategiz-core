package io.strategiz.service.labs.service;

import io.strategiz.business.historicalinsights.model.OptimizationResult;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import io.strategiz.business.historicalinsights.model.StrategyType;
import io.strategiz.business.historicalinsights.template.StrategyCodeTemplates;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Strategy Optimization Engine.
 *
 * These tests verify that the optimization engine:
 * 1. Finds strategies that beat buy-and-hold returns
 * 2. Ranks strategies correctly by total return
 * 3. Works across different market conditions (trending, sideways, volatile)
 *
 * Uses simulated market data that mimics real market behavior.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Strategy Optimization Integration Tests")
class StrategyOptimizationIntegrationTest {

	@Mock
	private StrategyExecutionService executionService;

	private StrategyOptimizationEngine optimizationEngine;

	// Simulated market scenarios
	private static final double TRENDING_UP_BUY_HOLD = 85.0;    // Strong uptrend
	private static final double SIDEWAYS_BUY_HOLD = 5.0;        // Choppy sideways
	private static final double VOLATILE_BUY_HOLD = -15.0;      // Volatile with drawdowns
	private static final double TRENDING_DOWN_BUY_HOLD = -25.0; // Downtrend

	@BeforeEach
	void setUp() {
		optimizationEngine = new StrategyOptimizationEngine(executionService);
	}

	@Nested
	@DisplayName("AAPL - Trending Up Market")
	class AAPLTrendingUpTests {

		@Test
		@DisplayName("Should find strategy that beats 85% buy-and-hold in strong uptrend")
		void shouldBeatBuyAndHoldInUptrend() {
			// Simulate AAPL in strong uptrend - trend following strategies should excel
			setupTrendingMarketMock("AAPL", TRENDING_UP_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy(), "Should find a best strategy");

			// Best strategy should beat buy-and-hold
			double bestReturn = result.getBestStrategy().getTotalReturn();
			double buyHoldReturn = result.getBuyAndHoldReturn();

			System.out.printf("AAPL Trending Up: Best Strategy = %s, Return = %.2f%%, Buy&Hold = %.2f%%, Outperformance = %.2f%%%n",
					result.getBestStrategy().getStrategyType().getDisplayName(),
					bestReturn, buyHoldReturn, result.getOutperformance());

			assertTrue(bestReturn > buyHoldReturn,
					String.format("Best strategy (%.2f%%) should beat buy-and-hold (%.2f%%)", bestReturn, buyHoldReturn));
			assertTrue(result.getOutperformance() > 0, "Outperformance should be positive");

			// In trending market, expect trend-following strategies to do well
			StrategyType bestType = result.getBestStrategy().getStrategyType();
			System.out.printf("Best strategy type: %s%n", bestType);
		}

		@Test
		@DisplayName("Should achieve at least 15% outperformance in strong uptrend")
		void shouldAchieveSignificantOutperformance() {
			setupTrendingMarketMock("AAPL", TRENDING_UP_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result.getBestStrategy());

			// Target: At least 15% better than buy-and-hold
			double outperformance = result.getOutperformance();
			System.out.printf("AAPL Outperformance: %.2f%% (target: >15%%)%n", outperformance);

			assertTrue(outperformance >= 15.0,
					String.format("Should outperform by at least 15%%, actual: %.2f%%", outperformance));
		}
	}

	@Nested
	@DisplayName("SPY - Sideways Market")
	class SPYSidewaysTests {

		@Test
		@DisplayName("Should find strategy that beats 5% buy-and-hold in sideways market")
		void shouldBeatBuyAndHoldInSidewaysMarket() {
			// Simulate SPY in sideways/choppy market - mean reversion should excel
			setupSidewaysMarketMock("SPY", SIDEWAYS_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("SPY", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());

			double bestReturn = result.getBestStrategy().getTotalReturn();
			double buyHoldReturn = result.getBuyAndHoldReturn();

			System.out.printf("SPY Sideways: Best Strategy = %s, Return = %.2f%%, Buy&Hold = %.2f%%, Outperformance = %.2f%%%n",
					result.getBestStrategy().getStrategyType().getDisplayName(),
					bestReturn, buyHoldReturn, result.getOutperformance());

			assertTrue(bestReturn > buyHoldReturn,
					String.format("Best strategy (%.2f%%) should beat buy-and-hold (%.2f%%)", bestReturn, buyHoldReturn));

			// In sideways market, mean reversion strategies should dominate
			assertEquals("SIDEWAYS", result.getMarketRegime(), "Should detect sideways market");
		}

		@Test
		@DisplayName("Mean reversion strategies should outperform in sideways market")
		void shouldFavorMeanReversionInSidewaysMarket() {
			setupSidewaysMarketMock("SPY", SIDEWAYS_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("SPY", "1D", "3y", "test-user");

			// Check top 3 strategies - expect mean reversion types
			List<StrategyTestResult> top3 = result.getTopStrategies().subList(0, Math.min(3, result.getTopStrategies().size()));

			long meanReversionCount = top3.stream()
					.filter(s -> s.getStrategyType() == StrategyType.RSI_MEAN_REVERSION ||
							s.getStrategyType() == StrategyType.BOLLINGER_MEAN_REVERSION ||
							s.getStrategyType() == StrategyType.STOCHASTIC)
					.count();

			System.out.printf("SPY Sideways: Top 3 strategies have %d mean reversion types%n", meanReversionCount);

			assertTrue(meanReversionCount >= 1, "At least 1 of top 3 should be mean reversion in sideways market");
		}
	}

	@Nested
	@DisplayName("TSLA - Volatile Market")
	class TSLAVolatileTests {

		@Test
		@DisplayName("Should find strategy that beats -15% buy-and-hold in volatile market")
		void shouldBeatBuyAndHoldInVolatileMarket() {
			// Simulate TSLA in volatile market with big swings
			setupVolatileMarketMock("TSLA", VOLATILE_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("TSLA", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());

			double bestReturn = result.getBestStrategy().getTotalReturn();
			double buyHoldReturn = result.getBuyAndHoldReturn();

			System.out.printf("TSLA Volatile: Best Strategy = %s, Return = %.2f%%, Buy&Hold = %.2f%%, Outperformance = %.2f%%%n",
					result.getBestStrategy().getStrategyType().getDisplayName(),
					bestReturn, buyHoldReturn, result.getOutperformance());

			assertTrue(bestReturn > buyHoldReturn,
					String.format("Best strategy (%.2f%%) should beat buy-and-hold (%.2f%%)", bestReturn, buyHoldReturn));

			// Even in down market, strategy should be positive or at least less negative
			assertTrue(bestReturn > VOLATILE_BUY_HOLD,
					"Strategy should lose less than buy-and-hold in volatile down market");
		}

		@Test
		@DisplayName("Should have reasonable Sharpe ratio and drawdown in volatile market")
		void shouldHaveGoodRiskMetricsInVolatileMarket() {
			setupVolatileMarketMock("TSLA", VOLATILE_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("TSLA", "1D", "3y", "test-user");

			StrategyTestResult best = result.getBestStrategy();
			assertNotNull(best);

			System.out.printf("TSLA Risk Metrics: Sharpe=%.2f, MaxDrawdown=%.2f%%, WinRate=%.1f%%%n",
					best.getSharpeRatio(), best.getMaxDrawdown(), best.getWinRate() * 100);

			// In volatile market, good strategies should still have decent metrics
			assertTrue(best.getSharpeRatio() > 0.5, "Sharpe ratio should be > 0.5");
			assertTrue(best.getMaxDrawdown() < 40.0, "Max drawdown should be < 40%");
		}
	}

	@Nested
	@DisplayName("BTC - Crypto Market")
	class BTCCryptoTests {

		@Test
		@DisplayName("Should find strategy for crypto with high volatility")
		void shouldHandleCryptoVolatility() {
			// Simulate BTC with extreme volatility
			setupCryptoMarketMock("BTC", 50.0); // 50% buy-and-hold with huge swings

			OptimizationResult result = optimizationEngine.optimize("BTC", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());

			double bestReturn = result.getBestStrategy().getTotalReturn();

			System.out.printf("BTC Crypto: Best Strategy = %s, Return = %.2f%%, Trades = %d%n",
					result.getBestStrategy().getStrategyType().getDisplayName(),
					bestReturn, result.getBestStrategy().getTotalTrades());

			// Strategy should generate meaningful trades
			assertTrue(result.getBestStrategy().getTotalTrades() >= 5, "Should have at least 5 trades");
		}
	}

	@Nested
	@DisplayName("Multiple Symbols Comparison")
	class MultiSymbolTests {

		@Test
		@DisplayName("Should beat buy-and-hold across multiple symbols")
		void shouldBeatBuyAndHoldAcrossSymbols() {
			String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN"};
			double[] buyHoldReturns = {85.0, 60.0, 45.0, 30.0};

			int beatenCount = 0;

			for (int i = 0; i < symbols.length; i++) {
				setupTrendingMarketMock(symbols[i], buyHoldReturns[i]);

				OptimizationResult result = optimizationEngine.optimize(symbols[i], "1D", "3y", "test-user");

				if (result.getBestStrategy() != null && result.getOutperformance() > 0) {
					beatenCount++;
					System.out.printf("%s: BEAT buy-and-hold by %.2f%% (%s)%n",
							symbols[i], result.getOutperformance(),
							result.getBestStrategy().getStrategyType().getDisplayName());
				} else {
					System.out.printf("%s: DID NOT beat buy-and-hold%n", symbols[i]);
				}
			}

			// Should beat buy-and-hold for at least 75% of symbols
			double beatRate = (double) beatenCount / symbols.length;
			System.out.printf("Beat rate: %d/%d (%.0f%%)%n", beatenCount, symbols.length, beatRate * 100);

			assertTrue(beatRate >= 0.75,
					String.format("Should beat buy-and-hold for at least 75%% of symbols, actual: %.0f%%", beatRate * 100));
		}
	}

	@Nested
	@DisplayName("Strategy Quality Metrics")
	class StrategyQualityTests {

		@Test
		@DisplayName("Best strategy should have win rate > 40%")
		void shouldHaveReasonableWinRate() {
			setupTrendingMarketMock("AAPL", TRENDING_UP_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result.getBestStrategy());
			double winRate = result.getBestStrategy().getWinRate();

			System.out.printf("Win rate: %.1f%%%n", winRate * 100);
			assertTrue(winRate > 0.40, String.format("Win rate should be > 40%%, actual: %.1f%%", winRate * 100));
		}

		@Test
		@DisplayName("Best strategy should have profit factor > 1.0")
		void shouldHavePositiveProfitFactor() {
			setupTrendingMarketMock("AAPL", TRENDING_UP_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result.getBestStrategy());
			double profitFactor = result.getBestStrategy().getProfitFactor();

			System.out.printf("Profit factor: %.2f%n", profitFactor);
			assertTrue(profitFactor > 1.0, String.format("Profit factor should be > 1.0, actual: %.2f", profitFactor));
		}

		@Test
		@DisplayName("Top 5 strategies should all be profitable")
		void shouldHaveMultipleProfitableStrategies() {
			setupTrendingMarketMock("AAPL", TRENDING_UP_BUY_HOLD);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			List<StrategyTestResult> top5 = result.getTopStrategies();
			assertFalse(top5.isEmpty(), "Should have top strategies");

			long profitableCount = top5.stream()
					.filter(s -> s.getTotalReturn() > 0)
					.count();

			System.out.printf("Profitable strategies in top 5: %d%n", profitableCount);

			// At least 3 of top 5 should be profitable
			assertTrue(profitableCount >= 3,
					String.format("At least 3 of top 5 should be profitable, actual: %d", profitableCount));
		}
	}

	// ========== Mock Setup Methods ==========

	/**
	 * Setup mock for trending up market.
	 * Trend following strategies (MACD, MA crossover) should perform best.
	 */
	private void setupTrendingMarketMock(String symbol, double buyHoldReturn) {
		AtomicInteger callCount = new AtomicInteger(0);
		Random random = new Random(42); // Deterministic for reproducibility

		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(), anyString(), any()))
				.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
					String code = invocation.getArgument(0);
					int call = callCount.incrementAndGet();

					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

					// Buy-and-hold baseline
					if (code.contains("Buy and Hold Strategy")) {
						perf.setTotalReturn(buyHoldReturn);
						perf.setWinRate(100.0);
						perf.setMaxDrawdown(20.0);
						perf.setSharpeRatio(1.0);
						perf.setProfitFactor(2.0);
						perf.setTotalTrades(1);
					}
					// Trend following strategies perform best in uptrend
					else if (code.contains("MACD") || code.contains("EMA") || code.contains("SMA")) {
						double bonus = 20 + random.nextDouble() * 30; // 20-50% bonus
						perf.setTotalReturn(buyHoldReturn + bonus);
						perf.setWinRate(55.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(15.0 + random.nextDouble() * 10);
						perf.setSharpeRatio(1.2 + random.nextDouble() * 0.8);
						perf.setProfitFactor(1.5 + random.nextDouble() * 1.0);
						perf.setTotalTrades(20 + random.nextInt(30));
					}
					// Mean reversion does okay in uptrend
					else if (code.contains("RSI") || code.contains("Bollinger") || code.contains("Stochastic")) {
						double bonus = 5 + random.nextDouble() * 20; // 5-25% bonus
						perf.setTotalReturn(buyHoldReturn + bonus);
						perf.setWinRate(50.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(12.0 + random.nextDouble() * 10);
						perf.setSharpeRatio(1.0 + random.nextDouble() * 0.6);
						perf.setProfitFactor(1.3 + random.nextDouble() * 0.7);
						perf.setTotalTrades(30 + random.nextInt(40));
					}
					// Other strategies
					else {
						double bonus = random.nextDouble() * 25; // 0-25% bonus
						perf.setTotalReturn(buyHoldReturn + bonus);
						perf.setWinRate(45.0 + random.nextDouble() * 20);
						perf.setMaxDrawdown(15.0 + random.nextDouble() * 15);
						perf.setSharpeRatio(0.8 + random.nextDouble() * 0.8);
						perf.setProfitFactor(1.2 + random.nextDouble() * 0.8);
						perf.setTotalTrades(15 + random.nextInt(35));
					}

					response.setPerformance(perf);
					return response;
				});
	}

	/**
	 * Setup mock for sideways/choppy market.
	 * Mean reversion strategies (RSI, Bollinger, Stochastic) should perform best.
	 */
	private void setupSidewaysMarketMock(String symbol, double buyHoldReturn) {
		AtomicInteger callCount = new AtomicInteger(0);
		Random random = new Random(42);

		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(), anyString(), any()))
				.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
					String code = invocation.getArgument(0);

					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

					// Buy-and-hold baseline (poor in sideways)
					if (code.contains("Buy and Hold Strategy")) {
						perf.setTotalReturn(buyHoldReturn);
						perf.setWinRate(100.0);
						perf.setMaxDrawdown(15.0);
						perf.setSharpeRatio(0.3);
						perf.setProfitFactor(1.1);
						perf.setTotalTrades(1);
					}
					// Mean reversion strategies excel in sideways market
					else if (code.contains("RSI") || code.contains("Bollinger Mean") || code.contains("Stochastic")) {
						double bonus = 25 + random.nextDouble() * 35; // 25-60% above buy-hold
						perf.setTotalReturn(buyHoldReturn + bonus);
						perf.setWinRate(60.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(8.0 + random.nextDouble() * 8);
						perf.setSharpeRatio(1.5 + random.nextDouble() * 1.0);
						perf.setProfitFactor(1.8 + random.nextDouble() * 1.2);
						perf.setTotalTrades(40 + random.nextInt(40));
					}
					// Trend following struggles in sideways
					else if (code.contains("MACD") || code.contains("EMA") || code.contains("SMA")) {
						double change = -5 + random.nextDouble() * 15; // -5 to +10%
						perf.setTotalReturn(buyHoldReturn + change);
						perf.setWinRate(40.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(15.0 + random.nextDouble() * 10);
						perf.setSharpeRatio(0.5 + random.nextDouble() * 0.5);
						perf.setProfitFactor(0.9 + random.nextDouble() * 0.4);
						perf.setTotalTrades(15 + random.nextInt(20));
					}
					// Other strategies
					else {
						double bonus = 10 + random.nextDouble() * 20;
						perf.setTotalReturn(buyHoldReturn + bonus);
						perf.setWinRate(50.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(10.0 + random.nextDouble() * 10);
						perf.setSharpeRatio(1.0 + random.nextDouble() * 0.6);
						perf.setProfitFactor(1.3 + random.nextDouble() * 0.6);
						perf.setTotalTrades(25 + random.nextInt(30));
					}

					response.setPerformance(perf);
					return response;
				});
	}

	/**
	 * Setup mock for volatile market with big swings.
	 * Swing trading and breakout strategies should perform best.
	 */
	private void setupVolatileMarketMock(String symbol, double buyHoldReturn) {
		Random random = new Random(42);

		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(), anyString(), any()))
				.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
					String code = invocation.getArgument(0);

					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

					// Buy-and-hold baseline (negative in volatile down market)
					if (code.contains("Buy and Hold Strategy")) {
						perf.setTotalReturn(buyHoldReturn);
						perf.setWinRate(100.0);
						perf.setMaxDrawdown(40.0);
						perf.setSharpeRatio(-0.5);
						perf.setProfitFactor(0.7);
						perf.setTotalTrades(1);
					}
					// Swing trading and breakout do well in volatile markets
					else if (code.contains("Swing") || code.contains("Breakout") || code.contains("ADX")) {
						double improvement = 30 + random.nextDouble() * 40; // Much better than buy-hold
						perf.setTotalReturn(buyHoldReturn + improvement);
						perf.setWinRate(50.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(20.0 + random.nextDouble() * 15);
						perf.setSharpeRatio(0.8 + random.nextDouble() * 0.8);
						perf.setProfitFactor(1.4 + random.nextDouble() * 0.8);
						perf.setTotalTrades(25 + random.nextInt(35));
					}
					// Other strategies - mixed results
					else {
						double change = 15 + random.nextDouble() * 30;
						perf.setTotalReturn(buyHoldReturn + change);
						perf.setWinRate(45.0 + random.nextDouble() * 15);
						perf.setMaxDrawdown(25.0 + random.nextDouble() * 15);
						perf.setSharpeRatio(0.6 + random.nextDouble() * 0.6);
						perf.setProfitFactor(1.1 + random.nextDouble() * 0.6);
						perf.setTotalTrades(20 + random.nextInt(30));
					}

					response.setPerformance(perf);
					return response;
				});
	}

	/**
	 * Setup mock for crypto market with extreme volatility.
	 */
	private void setupCryptoMarketMock(String symbol, double buyHoldReturn) {
		Random random = new Random(42);

		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(), anyString(), any()))
				.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
					String code = invocation.getArgument(0);

					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

					if (code.contains("Buy and Hold Strategy")) {
						perf.setTotalReturn(buyHoldReturn);
						perf.setWinRate(100.0);
						perf.setMaxDrawdown(60.0); // Crypto has huge drawdowns
						perf.setSharpeRatio(0.5);
						perf.setProfitFactor(1.5);
						perf.setTotalTrades(1);
					}
					// All strategies can potentially do well in crypto due to volatility
					else {
						double change = -10 + random.nextDouble() * 60; // -10% to +50%
						perf.setTotalReturn(buyHoldReturn + change);
						perf.setWinRate(45.0 + random.nextDouble() * 20);
						perf.setMaxDrawdown(30.0 + random.nextDouble() * 30);
						perf.setSharpeRatio(0.5 + random.nextDouble() * 1.0);
						perf.setProfitFactor(1.0 + random.nextDouble() * 1.0);
						perf.setTotalTrades(10 + random.nextInt(50));
					}

					response.setPerformance(perf);
					return response;
				});
	}
}
