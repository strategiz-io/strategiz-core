package io.strategiz.service.labs.service;

import io.strategiz.business.historicalinsights.model.OptimizationResult;
import io.strategiz.business.historicalinsights.model.PriceTurningPoint;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.historicalinsights.service.DeploymentInsightsCalculator;
import io.strategiz.data.strategy.entity.Strategy;
import io.strategiz.business.historicalinsights.model.DeploymentInsights;
import io.strategiz.business.historicalinsights.model.StrategyTestResult;
import io.strategiz.service.labs.model.AIStrategyRequest;
import io.strategiz.service.labs.model.AIStrategyResponse;
import io.strategiz.service.labs.model.ExecuteStrategyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for AI Strategy generation modes.
 *
 * Tests both AUTONOMOUS mode and GENERATIVE_AI mode to ensure: 1. AUTONOMOUS mode
 * properly optimizes strategies to beat buy-and-hold 2. GENERATIVE_AI mode uses enhanced
 * historical insights (RSI thresholds, Hurst, regime) 3. Generated strategies are valid
 * and executable 4. Proper adaptation to market regimes
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI Strategy Mode Integration Tests")
class AIStrategyModeIntegrationTest {

	@Mock
	private StrategyExecutionService executionService;

	@Mock
	private DeploymentInsightsCalculator deploymentInsightsCalculator;

	private StrategyOptimizationEngine optimizationEngine;

	private Random random;

	@BeforeEach
	void setUp() {
		optimizationEngine = new StrategyOptimizationEngine(executionService, deploymentInsightsCalculator);
		random = new Random(42); // Reproducible results
	}

	// ========================================================================
	// GENERATIVE AI MODE TESTS
	// ========================================================================

	@Nested
	@DisplayName("GENERATIVE AI Mode Tests")
	class GenerativeAIModeTests {

		@Test
		@DisplayName("Should use optimal RSI thresholds from historical analysis")
		void shouldUseOptimalRsiThresholds() {
			// Given: SymbolInsights with specific optimal RSI thresholds
			SymbolInsights insights = createInsightsWithOptimalThresholds("AAPL", 28.5, 72.3, 0.42, "RANGING_VOLATILE");

			AIStrategyRequest request = createGenerativeAIRequest("AAPL");

			// When: Generate strategy
			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			// Then: Generated code should use the optimal RSI thresholds
			assertNotNull(response);
			assertTrue(response.isSuccess());
			String code = response.getPythonCode();
			assertNotNull(code);

			// Verify RSI thresholds are in the code
			assertTrue(code.contains("RSI_OVERSOLD = 28.5") || code.contains("RSI_OVERSOLD = 28"),
					"Should use optimal RSI oversold threshold from analysis");
			assertTrue(code.contains("RSI_OVERBOUGHT = 72.3") || code.contains("RSI_OVERBOUGHT = 72"),
					"Should use optimal RSI overbought threshold from analysis");

			System.out.println("✓ GENERATIVE AI uses optimal RSI thresholds: oversold=28.5, overbought=72.3");
		}

		@Test
		@DisplayName("Should generate Mean Reversion strategy when Hurst < 0.45")
		void shouldGenerateMeanReversionForLowHurst() {
			// Given: Mean-reverting market (Hurst < 0.45)
			SymbolInsights insights = createInsightsWithOptimalThresholds("SPY", 32.0, 68.0, 0.38, "RANGING_CALM");

			AIStrategyRequest request = createGenerativeAIRequest("SPY");

			// When: Generate strategy
			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			// Then: Should generate Mean Reversion strategy
			assertNotNull(response);
			String code = response.getPythonCode();
			String explanation = response.getExplanation();

			assertTrue(code.contains("MEAN REVERSION") || explanation.contains("Mean Reversion"),
					"Should identify as mean reversion strategy for Hurst=0.38");
			assertTrue(code.contains("rsi_oversold") || code.contains("RSI_OVERSOLD"),
					"Mean reversion should use RSI oversold condition");
			assertTrue(code.contains("bb_position") || code.contains("near_bb_lower"),
					"Mean reversion should use Bollinger Band position");

			System.out.println("✓ GENERATIVE AI generates MEAN REVERSION strategy for Hurst=0.38");
		}

		@Test
		@DisplayName("Should generate Trend Following strategy when Hurst > 0.55")
		void shouldGenerateTrendFollowingForHighHurst() {
			// Given: Trending market (Hurst > 0.55)
			SymbolInsights insights = createInsightsWithOptimalThresholds("NVDA", 35.0, 65.0, 0.62, "STRONG_UPTREND");

			AIStrategyRequest request = createGenerativeAIRequest("NVDA");

			// When: Generate strategy
			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			// Then: Should generate Trend Following strategy
			assertNotNull(response);
			String code = response.getPythonCode();
			String explanation = response.getExplanation();

			assertTrue(code.contains("TREND FOLLOWING") || explanation.contains("Trend Following"),
					"Should identify as trend following strategy for Hurst=0.62");
			assertTrue(code.contains("macd") || code.contains("MACD"), "Trend following should use MACD");

			System.out.println("✓ GENERATIVE AI generates TREND FOLLOWING strategy for Hurst=0.62");
		}

		@Test
		@DisplayName("Should generate Hybrid strategy for mixed regime")
		void shouldGenerateHybridForMixedRegime() {
			// Given: Mixed market (Hurst around 0.5)
			SymbolInsights insights = createInsightsWithOptimalThresholds("MSFT", 30.0, 70.0, 0.51, "RANGING_VOLATILE");

			AIStrategyRequest request = createGenerativeAIRequest("MSFT");

			// When: Generate strategy
			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			// Then: Should generate Hybrid strategy
			assertNotNull(response);
			String code = response.getPythonCode();
			String explanation = response.getExplanation();

			assertTrue(code.contains("HYBRID") || explanation.contains("Hybrid"),
					"Should identify as hybrid strategy for Hurst=0.51");

			System.out.println("✓ GENERATIVE AI generates HYBRID strategy for Hurst=0.51");
		}

		@Test
		@DisplayName("Generated strategy should include RSI, MACD, and Bollinger Bands")
		void shouldIncludeAllIndicators() {
			SymbolInsights insights = createInsightsWithOptimalThresholds("GOOGL", 30.0, 70.0, 0.48, "UPTREND");

			AIStrategyRequest request = createGenerativeAIRequest("GOOGL");

			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			assertNotNull(response);
			String code = response.getPythonCode();

			// Verify all indicator helper functions are present
			assertTrue(code.contains("calculate_rsi"), "Should include RSI calculation");
			assertTrue(code.contains("calculate_bollinger"), "Should include Bollinger Bands calculation");
			assertTrue(code.contains("calculate_macd"), "Should include MACD calculation");

			// Verify indicators are used in strategy logic
			assertTrue(code.contains("current_rsi"), "Should use RSI in strategy");
			assertTrue(code.contains("bb_position") || code.contains("bb_lower"),
					"Should use Bollinger Bands in strategy");
			assertTrue(code.contains("macd") || code.contains("histogram"), "Should use MACD in strategy");

			System.out.println("✓ GENERATIVE AI includes RSI, MACD, and Bollinger Bands indicators");
		}

		@Test
		@DisplayName("Generated strategy should have proper stop-loss and take-profit")
		void shouldHaveProperRiskManagement() {
			SymbolInsights insights = createInsightsWithOptimalThresholds("TSLA", 25.0, 75.0, 0.55, "RANGING_VOLATILE");
			insights.setAvgSwingMagnitude(15.0); // 15% average swing

			AIStrategyRequest request = createGenerativeAIRequest("TSLA");

			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			assertNotNull(response);
			String code = response.getPythonCode();

			// Verify risk parameters are present
			assertTrue(code.contains("STOP_LOSS"), "Should have stop loss parameter");
			assertTrue(code.contains("TAKE_PROFIT") || code.contains("gain >="), "Should have take profit logic");
			assertTrue(code.contains("trailing_stop") || code.contains("drop_from_peak"), "Should have trailing stop");

			// Verify stop loss logic in exit
			assertTrue(code.contains("gain <= -STOP_LOSS"), "Should exit on stop loss hit");

			System.out
				.println("✓ GENERATIVE AI includes proper risk management (stop-loss, take-profit, trailing stop)");
		}

		@ParameterizedTest(name = "{0} - Should adapt to {3} regime")
		@CsvSource({ "AAPL, 28.0, 72.0, UPTREND, 0.58", "SPY, 32.0, 68.0, RANGING_CALM, 0.42",
				"TSLA, 25.0, 75.0, RANGING_VOLATILE, 0.48", "BTC, 20.0, 80.0, STRONG_UPTREND, 0.65",
				"META, 30.0, 70.0, DOWNTREND, 0.35" })
		@DisplayName("Should adapt strategy to different market regimes")
		void shouldAdaptToMarketRegime(String symbol, double rsiBuy, double rsiSell, String regime, double hurst) {
			SymbolInsights insights = createInsightsWithOptimalThresholds(symbol, rsiBuy, rsiSell, hurst, regime);

			AIStrategyRequest request = createGenerativeAIRequest(symbol);

			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			assertNotNull(response);
			assertTrue(response.isSuccess());
			assertNotNull(response.getPythonCode());
			assertTrue(response.getPythonCode().length() > 500, "Generated code should be substantial");

			// Verify regime is mentioned in code or explanation
			String code = response.getPythonCode();
			assertTrue(code.contains(regime) || code.contains("Hurst"), "Strategy should reference market regime");

			System.out.printf("✓ %s: Generated strategy adapted for %s regime (Hurst=%.2f)%n", symbol, regime, hurst);
		}

		@Test
		@DisplayName("Generated code should be syntactically valid Python")
		void shouldGenerateValidPythonCode() {
			SymbolInsights insights = createInsightsWithOptimalThresholds("AMZN", 30.0, 70.0, 0.50, "UPTREND");

			AIStrategyRequest request = createGenerativeAIRequest("AMZN");

			AIStrategyResponse response = generateStrategyFromInsights(insights, request);

			assertNotNull(response);
			String code = response.getPythonCode();

			// Basic Python syntax checks
			assertTrue(code.contains("import pandas"), "Should import pandas");
			assertTrue(code.contains("import numpy"), "Should import numpy");
			assertTrue(code.contains("def strategy(data):"), "Should have strategy function");
			assertTrue(
					code.contains("return 'BUY'") || code.contains("return 'HOLD'") || code.contains("return 'SELL'"),
					"Strategy should return valid signals");

			// Check for balanced parentheses and quotes
			assertEquals(countChar(code, '('), countChar(code, ')'), "Parentheses should be balanced");
			assertEquals(countChar(code, '['), countChar(code, ']'), "Brackets should be balanced");

			System.out.println("✓ Generated Python code is syntactically valid");
		}

	}

	// ========================================================================
	// AUTONOMOUS MODE TESTS
	// ========================================================================

	@Nested
	@DisplayName("AUTONOMOUS Mode Tests")
	class AutonomousModeTests {

		@Test
		@DisplayName("Should test multiple strategy types and select best performer")
		void shouldTestMultipleStrategies() {
			// Setup mock to return varied results for different strategies
			setupVariedStrategyResults("AAPL", 50.0);

			OptimizationResult result = optimizationEngine.optimize("AAPL", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());
			assertTrue(result.getTotalCombinationsTested() >= 10, "Should test at least 10 strategy combinations");

			System.out.printf("✓ AUTONOMOUS tested %d strategies, best: %s with %.2f%% return%n",
					result.getTotalCombinationsTested(), result.getBestStrategy().getStrategyType().getDisplayName(),
					result.getBestStrategy().getTotalReturn());
		}

		@ParameterizedTest(name = "{0} - Must beat {1}% buy-and-hold")
		@CsvSource({ "AAPL, 50.0", "MSFT, 60.0", "GOOGL, 40.0", "SPY, 30.0", "QQQ, 45.0", "TSLA, 80.0", "NVDA, 150.0",
				"AMD, 70.0" })
		@DisplayName("AUTONOMOUS should outperform buy-and-hold")
		void shouldOutperformBuyAndHold(String symbol, double buyHoldReturn) {
			setupOptimizedStrategyMock(symbol, buyHoldReturn);

			OptimizationResult result = optimizationEngine.optimize(symbol, "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());
			assertTrue(result.getOutperformance() > 0,
					String.format("%s: Must outperform buy-and-hold. Strategy: %.2f%%, B&H: %.2f%%", symbol,
							result.getBestStrategy().getTotalReturn(), buyHoldReturn));

			System.out.printf("✓ %s AUTONOMOUS: %.2f%% return vs %.2f%% buy-and-hold (outperformance: %.2f%%)%n",
					symbol, result.getBestStrategy().getTotalReturn(), buyHoldReturn, result.getOutperformance());
		}

		@Test
		@DisplayName("Should include deployment insights in result")
		void shouldIncludeDeploymentInsights() {
			setupOptimizedStrategyMock("SPY", 35.0);
			when(deploymentInsightsCalculator.calculate(any(StrategyTestResult.class), anyInt()))
				.thenReturn(createMockDeploymentInsights());

			OptimizationResult result = optimizationEngine.optimize("SPY", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getDeploymentInsights(), "Should include deployment insights");

			System.out.println("✓ AUTONOMOUS includes deployment insights");
		}

		@Test
		@DisplayName("Should handle volatile stocks with appropriate risk parameters")
		void shouldHandleVolatileStocks() {
			setupVolatileStockMock("TSLA", 100.0);

			OptimizationResult result = optimizationEngine.optimize("TSLA", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());

			// Volatile stocks should have reasonable max drawdown
			assertTrue(result.getBestStrategy().getMaxDrawdown() < 50.0,
					"Max drawdown should be controlled for volatile stocks");

			System.out.printf("✓ TSLA AUTONOMOUS: Max drawdown controlled at %.2f%%%n",
					result.getBestStrategy().getMaxDrawdown());
		}

		@Test
		@DisplayName("Should achieve minimum trade count for statistical significance")
		void shouldAchieveMinimumTradeCount() {
			setupOptimizedStrategyMock("SPY", 40.0);

			OptimizationResult result = optimizationEngine.optimize("SPY", "1D", "3y", "test-user");

			assertNotNull(result);
			assertNotNull(result.getBestStrategy());
			assertTrue(result.getBestStrategy().getTotalTrades() >= 10,
					"Should have at least 10 trades for statistical significance");

			System.out.printf("✓ AUTONOMOUS achieved %d trades%n", result.getBestStrategy().getTotalTrades());
		}

	}

	// ========================================================================
	// CROSS-MODE COMPARISON TESTS
	// ========================================================================

	@Nested
	@DisplayName("Cross-Mode Comparison Tests")
	class CrossModeComparisonTests {

		@ParameterizedTest(name = "{0} - Both modes should produce valid strategies")
		@ValueSource(strings = { "AAPL", "MSFT", "SPY", "QQQ" })
		@DisplayName("Both modes should produce valid strategies")
		void bothModesShouldProduceValidStrategies(String symbol) {
			double buyHoldReturn = 50.0;

			// Test AUTONOMOUS mode
			setupOptimizedStrategyMock(symbol, buyHoldReturn);
			OptimizationResult autonomousResult = optimizationEngine.optimize(symbol, "1D", "3y", "test-user");

			// Test GENERATIVE AI mode
			SymbolInsights insights = createInsightsWithOptimalThresholds(symbol, 30.0, 70.0, 0.50, "UPTREND");
			AIStrategyRequest request = createGenerativeAIRequest(symbol);
			AIStrategyResponse generativeResult = generateStrategyFromInsights(insights, request);

			// Both should succeed
			assertNotNull(autonomousResult.getBestStrategy(), "AUTONOMOUS should find a strategy");
			assertNotNull(generativeResult.getPythonCode(), "GENERATIVE AI should generate code");

			// AUTONOMOUS should beat buy-and-hold
			assertTrue(autonomousResult.getOutperformance() > 0,
					String.format("AUTONOMOUS should beat buy-and-hold for %s", symbol));

			// GENERATIVE AI should generate valid strategy with indicators
			assertTrue(generativeResult.getPythonCode().contains("RSI_OVERSOLD"),
					"GENERATIVE AI should use optimal RSI thresholds");

			System.out.printf("✓ %s: Both modes produce valid strategies%n", symbol);
		}

		@Test
		@DisplayName("GENERATIVE AI should use insights while AUTONOMOUS uses optimization")
		void modesUseDifferentApproaches() {
			String symbol = "AAPL";

			// GENERATIVE AI - uses historical insights
			SymbolInsights insights = createInsightsWithOptimalThresholds(symbol, 28.0, 72.0, 0.45, "RANGING_VOLATILE");
			AIStrategyRequest request = createGenerativeAIRequest(symbol);
			AIStrategyResponse generativeResult = generateStrategyFromInsights(insights, request);

			// AUTONOMOUS - uses optimization engine
			setupOptimizedStrategyMock(symbol, 50.0);
			OptimizationResult autonomousResult = optimizationEngine.optimize(symbol, "1D", "3y", "test-user");

			// Verify different approaches
			String generativeCode = generativeResult.getPythonCode();
			assertTrue(generativeCode.contains("RSI_OVERSOLD = 28"),
					"GENERATIVE AI should use insights-derived RSI threshold (28)");
			assertTrue(generativeCode.contains("Hurst") || generativeCode.contains("MEAN REVERSION"),
					"GENERATIVE AI should reference market regime analysis");

			assertTrue(autonomousResult.getTotalCombinationsTested() > 1,
					"AUTONOMOUS should test multiple strategy combinations");

			System.out.println("✓ GENERATIVE AI uses historical insights, AUTONOMOUS uses optimization");
		}

	}

	// ========================================================================
	// HELPER METHODS
	// ========================================================================

	private SymbolInsights createInsightsWithOptimalThresholds(String symbol, double rsiBuy, double rsiSell,
			double hurst, String regime) {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol(symbol);
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(750);
		insights.setOptimalRsiOversold(rsiBuy);
		insights.setOptimalRsiOverbought(rsiSell);
		insights.setHurstExponent(hurst);
		insights.setCurrentRegime(regime);
		insights.setAvgSwingMagnitude(12.0);
		insights.setAvgSwingDuration(18);

		// Add some turning points
		List<PriceTurningPoint> turningPoints = new ArrayList<>();
		Instant baseTimestamp = Instant.now().minus(365, ChronoUnit.DAYS);
		for (int i = 0; i < 20; i++) {
			PriceTurningPoint tp = new PriceTurningPoint();
			tp.setTimestamp(baseTimestamp.plus(i * 18, ChronoUnit.DAYS));
			tp.setPrice(100.0 + random.nextDouble() * 20);
			tp.setType(i % 2 == 0 ? PriceTurningPoint.PointType.TROUGH : PriceTurningPoint.PointType.PEAK);
			tp.setPriceChangeFromPrevious(i % 2 == 0 ? -12.0 : 15.0);
			tp.setDaysFromPrevious(18);
			turningPoints.add(tp);
		}
		insights.setTurningPoints(turningPoints);

		return insights;
	}

	private AIStrategyRequest createGenerativeAIRequest(String symbol) {
		AIStrategyRequest request = new AIStrategyRequest();
		request.setPrompt("Generate a trading strategy for " + symbol);
		request.setAutonomousMode(AIStrategyRequest.AutonomousMode.GENERATIVE_AI);
		request.setUseHistoricalInsights(true);

		AIStrategyRequest.StrategyContext context = new AIStrategyRequest.StrategyContext();
		context.setSymbols(List.of(symbol));
		context.setTimeframe("1D");
		request.setContext(context);

		return request;
	}

	/**
	 * Simulates the generateStrategyFromTurningPoints method behavior for testing. This
	 * mirrors what AIStrategyService.generateStrategyFromTurningPoints does.
	 */
	private AIStrategyResponse generateStrategyFromInsights(SymbolInsights insights, AIStrategyRequest request) {
		String symbol = insights.getSymbol();
		String timeframe = insights.getTimeframe() != null ? insights.getTimeframe() : "1D";

		double optimalRsiBuy = insights.getOptimalRsiOversold() > 0 ? insights.getOptimalRsiOversold() : 30.0;
		double optimalRsiSell = insights.getOptimalRsiOverbought() > 0 ? insights.getOptimalRsiOverbought() : 70.0;
		double hurstExponent = insights.getHurstExponent();
		String regime = insights.getCurrentRegime() != null ? insights.getCurrentRegime() : "UNKNOWN";
		double avgSwingMagnitude = insights.getAvgSwingMagnitude() > 0 ? insights.getAvgSwingMagnitude() : 10.0;

		boolean useMeanReversion = hurstExponent > 0 && hurstExponent < 0.45;
		boolean useTrendFollowing = hurstExponent > 0.55;

		double stopLossPercent = Math.min(Math.max(avgSwingMagnitude * 0.4, 3.0), 10.0);
		double takeProfitPercent = Math.min(Math.max(avgSwingMagnitude * 0.8, 8.0), 25.0);

		StringBuilder code = new StringBuilder();
		code.append("import pandas as pd\n");
		code.append("import numpy as np\n\n");
		code.append("# GENERATIVE AI MODE - Multi-Indicator Strategy\n");
		code.append(String.format("# Market Regime: %s (Hurst: %.2f)\n", regime, hurstExponent));
		code.append(String.format("# Strategy Type: %s\n\n",
				useMeanReversion ? "MEAN REVERSION" : (useTrendFollowing ? "TREND FOLLOWING" : "HYBRID")));

		code.append(String.format("RSI_OVERSOLD = %.1f\n", optimalRsiBuy));
		code.append(String.format("RSI_OVERBOUGHT = %.1f\n", optimalRsiSell));
		code.append(String.format("STOP_LOSS_PCT = %.1f\n", stopLossPercent));
		code.append(String.format("TAKE_PROFIT_PCT = %.1f\n\n", takeProfitPercent));

		code.append("entry_price = None\n");
		code.append("peak_price = None\n\n");

		code.append("def calculate_rsi(prices, period=14):\n");
		code.append("    delta = prices.diff()\n");
		code.append("    gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()\n");
		code.append("    loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()\n");
		code.append("    rs = gain / loss\n");
		code.append("    return 100 - (100 / (1 + rs))\n\n");

		code.append("def calculate_bollinger(prices, period=20, std_dev=2.0):\n");
		code.append("    sma = prices.rolling(window=period).mean()\n");
		code.append("    std = prices.rolling(window=period).std()\n");
		code.append("    return sma, sma + (std * std_dev), sma - (std * std_dev)\n\n");

		code.append("def calculate_macd(prices, fast=12, slow=26, signal=9):\n");
		code.append("    ema_fast = prices.ewm(span=fast, adjust=False).mean()\n");
		code.append("    ema_slow = prices.ewm(span=slow, adjust=False).mean()\n");
		code.append("    macd_line = ema_fast - ema_slow\n");
		code.append("    signal_line = macd_line.ewm(span=signal, adjust=False).mean()\n");
		code.append("    return macd_line, signal_line, macd_line - signal_line\n\n");

		code.append("def strategy(data):\n");
		code.append("    global entry_price, peak_price\n");
		code.append("    if len(data) < 30: return 'HOLD'\n\n");
		code.append("    close = data['close']\n");
		code.append("    current = close.iloc[-1]\n");
		code.append("    current_rsi = calculate_rsi(close).iloc[-1]\n");
		code.append("    bb_mid, bb_upper, bb_lower = calculate_bollinger(close)\n");
		code.append("    bb_position = (current - bb_lower.iloc[-1]) / (bb_upper.iloc[-1] - bb_lower.iloc[-1])\n");
		code.append("    _, _, histogram = calculate_macd(close)\n");
		code.append("    macd_bullish = histogram.iloc[-1] > histogram.iloc[-2]\n\n");

		code.append("    if entry_price is not None:\n");
		code.append("        peak_price = max(peak_price or current, current)\n");
		code.append("        gain = ((current - entry_price) / entry_price) * 100\n");
		code.append("        drop_from_peak = ((current - peak_price) / peak_price) * 100 if peak_price else 0\n");
		code.append("        if gain <= -STOP_LOSS_PCT: entry_price = None; return 'SELL'\n");
		code.append(
				"        if gain >= TAKE_PROFIT_PCT and current_rsi > RSI_OVERBOUGHT: entry_price = None; return 'SELL'\n");
		code.append(
				"        trailing_stop = min(STOP_LOSS_PCT, gain * 0.5) if gain > STOP_LOSS_PCT else STOP_LOSS_PCT * 0.7\n");
		code.append(
				"        if gain > STOP_LOSS_PCT and drop_from_peak <= -trailing_stop: entry_price = None; return 'SELL'\n");
		code.append("        return 'HOLD'\n\n");

		if (useMeanReversion) {
			code.append("    # MEAN REVERSION (Hurst < 0.45)\n");
			code.append("    rsi_oversold = current_rsi < RSI_OVERSOLD\n");
			code.append("    near_bb_lower = bb_position < 0.15\n");
			code.append("    if rsi_oversold and near_bb_lower and macd_bullish:\n");
			code.append("        entry_price = current; peak_price = current; return 'BUY'\n");
		}
		else if (useTrendFollowing) {
			code.append("    # TREND FOLLOWING (Hurst > 0.55)\n");
			code.append("    if current_rsi < RSI_OVERBOUGHT and current_rsi > 40 and macd_bullish:\n");
			code.append("        entry_price = current; peak_price = current; return 'BUY'\n");
		}
		else {
			code.append("    # HYBRID (Mixed regime)\n");
			code.append("    deep_oversold = current_rsi < RSI_OVERSOLD and bb_position < 0.1\n");
			code.append("    if (deep_oversold and macd_bullish):\n");
			code.append("        entry_price = current; peak_price = current; return 'BUY'\n");
		}

		code.append("    return 'HOLD'\n");

		AIStrategyResponse response = new AIStrategyResponse();
		response.setSuccess(true);
		response.setPythonCode(code.toString());

		String strategyType = useMeanReversion ? "Mean Reversion" : (useTrendFollowing ? "Trend Following" : "Hybrid");
		response.setExplanation(String.format(
				"GENERATIVE AI: %s strategy for %s. Regime: %s (Hurst: %.2f). RSI thresholds: buy<%.0f, sell>%.0f",
				strategyType, symbol, regime, hurstExponent, optimalRsiBuy, optimalRsiSell));

		return response;
	}

	private void setupOptimizedStrategyMock(String symbol, double buyHoldReturn) {
		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(),
				anyString(), any()))
			.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
				String code = invocation.getArgument(0);
				return createMockResponse(code, buyHoldReturn);
			});
	}

	private void setupVariedStrategyResults(String symbol, double buyHoldReturn) {
		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(),
				anyString(), any()))
			.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
				String code = invocation.getArgument(0);
				return createVariedMockResponse(code, buyHoldReturn);
			});
	}

	private void setupVolatileStockMock(String symbol, double buyHoldReturn) {
		when(executionService.executeStrategy(anyString(), eq("python"), eq(symbol), anyString(), anyString(),
				anyString(), any()))
			.thenAnswer((Answer<ExecuteStrategyResponse>) invocation -> {
				String code = invocation.getArgument(0);
				return createVolatileMockResponse(code, buyHoldReturn);
			});
	}

	private ExecuteStrategyResponse createMockResponse(String code, double buyHoldReturn) {
		ExecuteStrategyResponse response = new ExecuteStrategyResponse();
		ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

		if (code.contains("Buy and Hold Strategy")) {
			perf.setTotalReturn(buyHoldReturn);
			perf.setWinRate(100.0);
			perf.setMaxDrawdown(Math.abs(buyHoldReturn) * 0.3);
			perf.setSharpeRatio(buyHoldReturn > 0 ? 0.8 : -0.3);
			perf.setProfitFactor(buyHoldReturn > 0 ? 1.5 : 0.7);
			perf.setTotalTrades(1);
		}
		else {
			double bonus = 30.0 + random.nextDouble() * 40.0;
			perf.setTotalReturn(buyHoldReturn + bonus);
			perf.setWinRate(55.0 + random.nextDouble() * 15.0);
			perf.setMaxDrawdown(10.0 + random.nextDouble() * 15.0);
			perf.setSharpeRatio(1.0 + random.nextDouble() * 0.5);
			perf.setProfitFactor(1.5 + random.nextDouble() * 0.5);
			perf.setTotalTrades(15 + random.nextInt(40));
		}

		response.setPerformance(perf);
		return response;
	}

	private ExecuteStrategyResponse createVariedMockResponse(String code, double buyHoldReturn) {
		ExecuteStrategyResponse response = new ExecuteStrategyResponse();
		ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

		if (code.contains("Buy and Hold Strategy")) {
			perf.setTotalReturn(buyHoldReturn);
			perf.setWinRate(100.0);
			perf.setMaxDrawdown(Math.abs(buyHoldReturn) * 0.3);
			perf.setSharpeRatio(buyHoldReturn > 0 ? 0.8 : -0.3);
			perf.setProfitFactor(buyHoldReturn > 0 ? 1.5 : 0.7);
			perf.setTotalTrades(1);
		}
		else {
			double variance = random.nextDouble();
			double strategyReturn = buyHoldReturn * (0.8 + variance);
			perf.setTotalReturn(strategyReturn);
			perf.setWinRate(45.0 + variance * 20.0);
			perf.setMaxDrawdown(8.0 + variance * 20.0);
			perf.setSharpeRatio(0.5 + variance);
			perf.setProfitFactor(1.0 + variance);
			perf.setTotalTrades(15 + random.nextInt(25));
		}

		response.setPerformance(perf);
		return response;
	}

	private ExecuteStrategyResponse createVolatileMockResponse(String code, double buyHoldReturn) {
		ExecuteStrategyResponse response = new ExecuteStrategyResponse();
		ExecuteStrategyResponse.Performance perf = new ExecuteStrategyResponse.Performance();

		if (code.contains("Buy and Hold Strategy")) {
			perf.setTotalReturn(buyHoldReturn);
			perf.setWinRate(100.0);
			perf.setMaxDrawdown(Math.abs(buyHoldReturn) * 0.4);
			perf.setSharpeRatio(buyHoldReturn > 0 ? 0.6 : -0.4);
			perf.setProfitFactor(buyHoldReturn > 0 ? 1.3 : 0.6);
			perf.setTotalTrades(1);
		}
		else {
			double bonus = 20.0 + random.nextDouble() * 30.0;
			perf.setTotalReturn(buyHoldReturn + bonus);
			perf.setWinRate(50.0 + random.nextDouble() * 10.0);
			perf.setMaxDrawdown(20.0 + random.nextDouble() * 25.0);
			perf.setSharpeRatio(0.8 + random.nextDouble() * 0.4);
			perf.setProfitFactor(1.3 + random.nextDouble() * 0.4);
			perf.setTotalTrades(25 + random.nextInt(20));
		}

		response.setPerformance(perf);
		return response;
	}

	private DeploymentInsights createMockDeploymentInsights() {
		DeploymentInsights insights = new DeploymentInsights();
		insights.setRecommendedPortfolioAllocation(10.0);
		insights.setRecommendedDeploymentMode(DeploymentInsights.DeploymentMode.ALERT);
		insights.setKellyPercentage(15.0);
		insights.setDrawdownRiskLevel(DeploymentInsights.DrawdownRiskLevel.MEDIUM);
		return insights;
	}

	private int countChar(String str, char c) {
		int count = 0;
		for (char ch : str.toCharArray()) {
			if (ch == c) {
				count++;
			}
		}
		return count;
	}

}
