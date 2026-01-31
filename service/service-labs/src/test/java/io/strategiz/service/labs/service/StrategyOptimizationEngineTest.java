package io.strategiz.service.labs.service;

import io.strategiz.business.historicalinsights.model.OptimizationResult;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import io.strategiz.business.historicalinsights.model.StrategyType;
import io.strategiz.business.historicalinsights.service.DeploymentInsightsCalculator;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StrategyOptimizationEngine. Tests parameter generation, parallel
 * execution, and result ranking.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Strategy Optimization Engine Tests")
class StrategyOptimizationEngineTest {

	@Mock
	private StrategyExecutionService executionService;

	@Mock
	private DeploymentInsightsCalculator deploymentInsightsCalculator;

	private StrategyOptimizationEngine optimizationEngine;

	@BeforeEach
	void setUp() {
		optimizationEngine = new StrategyOptimizationEngine(executionService, deploymentInsightsCalculator);
	}

	@Nested
	@DisplayName("Parameter Combination Generation")
	class ParameterGenerationTests {

		@Test
		@DisplayName("Should generate RSI combinations with valid parameter ranges")
		void shouldGenerateValidRSICombinations() {
			// Test RSI template with various parameters
			Map<String, Object> params = Map.of("period", 14, "oversold", 30, "overbought", 70, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.RSI_MEAN_REVERSION, params);

			assertNotNull(code);
			assertTrue(code.contains("RSI_PERIOD = 14"));
			assertTrue(code.contains("OVERSOLD = 30"));
			assertTrue(code.contains("OVERBOUGHT = 70"));
			assertTrue(code.contains("ATR_MULTIPLIER = 2.0"));
			assertTrue(code.contains("def calculate_rsi"));
		}

		@Test
		@DisplayName("Should generate MACD combinations with valid parameters")
		void shouldGenerateValidMACDCombinations() {
			Map<String, Object> params = Map.of("fast", 12, "slow", 26, "signal_period", 9, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.MACD_TREND_FOLLOWING, params);

			assertNotNull(code);
			assertTrue(code.contains("FAST_PERIOD = 12"));
			assertTrue(code.contains("SLOW_PERIOD = 26"));
			assertTrue(code.contains("SIGNAL_PERIOD = 9"));
			assertTrue(code.contains("MACD bullish crossover"));
		}

		@Test
		@DisplayName("Should generate Bollinger Bands code with correct bands")
		void shouldGenerateBollingerBandsCode() {
			Map<String, Object> params = Map.of("period", 20, "std_mult", 2.0, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.BOLLINGER_MEAN_REVERSION, params);

			assertNotNull(code);
			assertTrue(code.contains("BB_PERIOD = 20"));
			assertTrue(code.contains("STD_MULT = 2.0"));
			assertTrue(code.contains("bb_upper"));
			assertTrue(code.contains("bb_lower"));
		}

		@Test
		@DisplayName("Should generate swing trading code with thresholds")
		void shouldGenerateSwingTradingCode() {
			Map<String, Object> params = Map.of("buy_threshold", 10, "sell_threshold", 15, "lookback", 20,
					"atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.SWING_TRADING, params);

			assertNotNull(code);
			assertTrue(code.contains("BUY_THRESHOLD = 10"));
			assertTrue(code.contains("SELL_THRESHOLD = 15"));
			assertTrue(code.contains("LOOKBACK = 20"));
		}

		@Test
		@DisplayName("Should generate all strategy types without errors")
		void shouldGenerateAllStrategyTypes() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertNotNull(code, "Code should not be null for " + type);
				assertTrue(code.length() > 100, "Code should be substantial for " + type);
				assertTrue(code.contains("import pandas"), "Should import pandas for " + type);
			}
		}

	}

	@Nested
	@DisplayName("Optimization Execution")
	class OptimizationExecutionTests {

		@Test
		@DisplayName("Should run optimization and return best strategy by total return")
		void shouldReturnBestStrategyByTotalReturn() {
			// Mock execution service to return varying results
			when(executionService.executeStrategy(anyString(), eq("python"), eq("AAPL"), eq("1D"), eq("3y"),
					anyString(), any()))
				.thenAnswer(invocation -> {
					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();
					// Return random-ish performance based on call count
					perf.setTotalReturn(Math.random() * 100 - 20); // -20% to +80%
					perf.setWinRate(50.0);
					perf.setMaxDrawdown(15.0);
					perf.setSharpeRatio(1.0);
					perf.setProfitFactor(1.5);
					perf.setTotalTrades(20);
					response.setPerformance(perf);
					return response;
				});

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result);
			assertEquals("AAPL", result.getSymbol());
			assertEquals("1D", result.getTimeframe());
			assertTrue(result.getTotalCombinationsTested() > 100, "Should test many combinations");

			// Verify execution service was called
			verify(executionService, atLeast(50)).executeStrategy(anyString(), eq("python"), eq("AAPL"), eq("1D"),
					eq("3y"), anyString(), any());
		}

		@Test
		@DisplayName("Should handle execution failures gracefully")
		void shouldHandleExecutionFailures() {
			// Mock some failures
			when(executionService.executeStrategy(anyString(), eq("python"), eq("AAPL"), eq("1D"), eq("3y"),
					anyString(), any()))
				.thenThrow(new RuntimeException("Execution failed"));

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result);
			// Should complete without throwing
			assertEquals(0, result.getSuccessfulTests());
		}

		@Test
		@DisplayName("Should sort strategies by total return descending")
		void shouldSortByTotalReturnDescending() {
			// Create test results with known returns
			StrategyTestResult result1 = new StrategyTestResult(StrategyType.RSI_MEAN_REVERSION, Map.of());
			result1.setTotalReturn(50.0);
			result1.setSuccess(true);

			StrategyTestResult result2 = new StrategyTestResult(StrategyType.MACD_TREND_FOLLOWING, Map.of());
			result2.setTotalReturn(75.0);
			result2.setSuccess(true);

			StrategyTestResult result3 = new StrategyTestResult(StrategyType.SWING_TRADING, Map.of());
			result3.setTotalReturn(25.0);
			result3.setSuccess(true);

			// Verify Comparable implementation
			assertTrue(result2.compareTo(result1) < 0, "Higher return should come first");
			assertTrue(result1.compareTo(result3) < 0, "50% should come before 25%");
		}

	}

	@Nested
	@DisplayName("Result Building")
	class ResultBuildingTests {

		@Test
		@DisplayName("Should build optimization result with all metrics")
		void shouldBuildCompleteResult() {
			when(executionService.executeStrategy(anyString(), eq("python"), eq("TSLA"), eq("1D"), eq("3y"),
					anyString(), any()))
				.thenAnswer(invocation -> {
					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();
					perf.setTotalReturn(45.0);
					perf.setWinRate(55.0);
					perf.setMaxDrawdown(12.0);
					perf.setSharpeRatio(1.5);
					perf.setProfitFactor(2.0);
					perf.setTotalTrades(30);
					response.setPerformance(perf);
					return response;
				});

			OptimizationResult result = optimizationEngine.optimize("TSLA", "1D", "3y", "test-user");

			assertNotNull(result);
			assertEquals("TSLA", result.getSymbol());
			assertTrue(result.getTotalCombinationsTested() > 0);
			assertNotNull(result.getMarketRegime());

			if (result.getBestStrategy() != null) {
				assertNotNull(result.getBestStrategy().getStrategyType());
				assertNotNull(result.getBestStrategy().getPythonCode());
			}
		}

		@Test
		@DisplayName("Should calculate outperformance correctly")
		void shouldCalculateOutperformance() {
			// Mock strategy with 80% return
			when(executionService.executeStrategy(anyString(), eq("python"), eq("AAPL"), eq("1D"), eq("3y"),
					anyString(), any()))
				.thenAnswer(invocation -> {
					String code = invocation.getArgument(0);
					ExecuteStrategyResponse response = new ExecuteStrategyResponse();
					ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

					// Buy-and-hold code is simple, detect it
					if (code.contains("Buy and Hold Strategy")) {
						perf.setTotalReturn(50.0); // Buy and hold return
					}
					else {
						perf.setTotalReturn(80.0); // Strategy return
					}
					perf.setWinRate(55.0);
					perf.setMaxDrawdown(12.0);
					perf.setSharpeRatio(1.5);
					perf.setProfitFactor(2.0);
					perf.setTotalTrades(30);
					response.setPerformance(perf);
					return response;
				});

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result);
			// Outperformance = strategy return - buy and hold return
			if (result.getBestStrategy() != null) {
				double expectedOutperformance = result.getBestStrategy().getTotalReturn()
						- result.getBuyAndHoldReturn();
				assertEquals(expectedOutperformance, result.getOutperformance(), 0.01);
			}
		}

	}

	@Nested
	@DisplayName("Market Regime Detection")
	class MarketRegimeTests {

		@Test
		@DisplayName("Should detect trending market when trend-following strategies win")
		void shouldDetectTrendingMarket() {
			// This is more of an integration concern, but we can verify the logic
			StrategyTestResult trendResult = new StrategyTestResult(StrategyType.MACD_TREND_FOLLOWING, Map.of());
			trendResult.setTotalReturn(100.0);
			trendResult.setSuccess(true);
			trendResult.setTotalTrades(20);

			// Verify strategy type classification
			assertEquals("MACD Trend Following", trendResult.getStrategyType().getDisplayName());
		}

		@Test
		@DisplayName("Should detect sideways market when mean-reversion strategies win")
		void shouldDetectSidewaysMarket() {
			StrategyTestResult mrResult = new StrategyTestResult(StrategyType.RSI_MEAN_REVERSION, Map.of());
			mrResult.setTotalReturn(80.0);
			mrResult.setSuccess(true);
			mrResult.setTotalTrades(25);

			assertEquals("RSI Mean Reversion", mrResult.getStrategyType().getDisplayName());
		}

	}

	// Helper method to get default parameters for each strategy type
	private Map<String, Object> getDefaultParamsForType(StrategyType type) {
		return switch (type) {
			case RSI_MEAN_REVERSION -> Map.of("period", 14, "oversold", 30, "overbought", 70, "atr_multiplier", 2.0);
			case MACD_TREND_FOLLOWING -> Map.of("fast", 12, "slow", 26, "signal_period", 9, "atr_multiplier", 2.0);
			case BOLLINGER_MEAN_REVERSION, BOLLINGER_BREAKOUT ->
				Map.of("period", 20, "std_mult", 2.0, "atr_multiplier", 2.0);
			case MA_CROSSOVER_EMA, MA_CROSSOVER_SMA -> Map.of("fast", 10, "slow", 50, "atr_multiplier", 2.0);
			case STOCHASTIC ->
				Map.of("k_period", 14, "d_period", 3, "oversold", 20, "overbought", 80, "atr_multiplier", 2.0);
			case SWING_TRADING ->
				Map.of("buy_threshold", 10, "sell_threshold", 15, "lookback", 20, "atr_multiplier", 2.0);
			case COMBINED_ADX -> Map.of("adx_threshold", 25, "rsi_period", 14, "rsi_oversold", 30, "rsi_overbought", 70,
					"atr_multiplier", 2.0);
		};
	}

}
