package io.strategiz.business.historicalinsights.template;

import io.strategiz.business.historicalinsights.model.StrategyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StrategyCodeTemplates. Verifies that all strategy templates generate
 * valid Python code.
 */
@DisplayName("Strategy Code Templates Tests")
class StrategyCodeTemplatesTest {

	@Nested
	@DisplayName("RSI Mean Reversion Strategy")
	class RSIStrategyTests {

		@Test
		@DisplayName("Should generate valid RSI code with default parameters")
		void shouldGenerateValidRSICode() {
			Map<String, Object> params = Map.of("period", 14, "oversold", 30, "overbought", 70, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.RSI_MEAN_REVERSION, params);

			// Verify structure
			assertNotNull(code);
			assertTrue(code.contains("import pandas as pd"));
			assertTrue(code.contains("import numpy as np"));

			// Verify parameters are injected
			assertTrue(code.contains("RSI_PERIOD = 14"));
			assertTrue(code.contains("OVERSOLD = 30"));
			assertTrue(code.contains("OVERBOUGHT = 70"));
			assertTrue(code.contains("ATR_MULTIPLIER = 2.0"));

			// Verify RSI calculation function
			assertTrue(code.contains("def calculate_rsi"));
			assertTrue(code.contains("avg_gain"));
			assertTrue(code.contains("avg_loss"));

			// Verify trading logic
			assertTrue(code.contains("if rsi < OVERSOLD"));
			assertTrue(code.contains("if rsi > OVERBOUGHT"));
			assertTrue(code.contains("signal('BUY'"));
			assertTrue(code.contains("signal('SELL'"));
		}

		@Test
		@DisplayName("Should generate RSI with different oversold/overbought levels")
		void shouldGenerateRSIWithCustomLevels() {
			Map<String, Object> params = Map.of("period", 7, "oversold", 20, "overbought", 80, "atr_multiplier", 1.5);

			String code = StrategyCodeTemplates.generateCode(StrategyType.RSI_MEAN_REVERSION, params);

			assertTrue(code.contains("RSI_PERIOD = 7"));
			assertTrue(code.contains("OVERSOLD = 20"));
			assertTrue(code.contains("OVERBOUGHT = 80"));
			assertTrue(code.contains("ATR_MULTIPLIER = 1.5"));
		}

	}

	@Nested
	@DisplayName("MACD Trend Following Strategy")
	class MACDStrategyTests {

		@Test
		@DisplayName("Should generate valid MACD code")
		void shouldGenerateValidMACDCode() {
			Map<String, Object> params = Map.of("fast", 12, "slow", 26, "signal_period", 9, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.MACD_TREND_FOLLOWING, params);

			// Verify parameters
			assertTrue(code.contains("FAST_PERIOD = 12"));
			assertTrue(code.contains("SLOW_PERIOD = 26"));
			assertTrue(code.contains("SIGNAL_PERIOD = 9"));

			// Verify MACD calculation
			assertTrue(code.contains("ema_fast"));
			assertTrue(code.contains("ema_slow"));
			assertTrue(code.contains("macd_signal"));

			// Verify crossover logic
			assertTrue(code.contains("prev_macd <= prev_signal and macd > macd_signal"));
			assertTrue(code.contains("MACD bullish crossover"));
		}

		@Test
		@DisplayName("Should generate MACD with custom parameters")
		void shouldGenerateMACDWithCustomParams() {
			Map<String, Object> params = Map.of("fast", 8, "slow", 20, "signal_period", 7, "atr_multiplier", 2.5);

			String code = StrategyCodeTemplates.generateCode(StrategyType.MACD_TREND_FOLLOWING, params);

			assertTrue(code.contains("FAST_PERIOD = 8"));
			assertTrue(code.contains("SLOW_PERIOD = 20"));
			assertTrue(code.contains("SIGNAL_PERIOD = 7"));
		}

	}

	@Nested
	@DisplayName("Bollinger Bands Strategies")
	class BollingerStrategyTests {

		@Test
		@DisplayName("Should generate Bollinger Mean Reversion code")
		void shouldGenerateBollingerMeanReversionCode() {
			Map<String, Object> params = Map.of("period", 20, "std_mult", 2.0, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.BOLLINGER_MEAN_REVERSION, params);

			// Verify parameters
			assertTrue(code.contains("BB_PERIOD = 20"));
			assertTrue(code.contains("STD_MULT = 2.0"));

			// Verify band calculations
			assertTrue(code.contains("bb_middle"));
			assertTrue(code.contains("bb_upper"));
			assertTrue(code.contains("bb_lower"));
			assertTrue(code.contains("bb_std"));

			// Verify mean reversion logic (buy at lower, sell at upper)
			assertTrue(code.contains("price <= bb_lower"));
			assertTrue(code.contains("price >= bb_upper"));
		}

		@Test
		@DisplayName("Should generate Bollinger Breakout code with volume filter")
		void shouldGenerateBollingerBreakoutCode() {
			Map<String, Object> params = Map.of("period", 20, "std_mult", 2.0, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.BOLLINGER_BREAKOUT, params);

			// Verify breakout logic
			assertTrue(code.contains("price > bb_upper"));
			assertTrue(code.contains("VOLUME_MULT"));
			assertTrue(code.contains("vol_avg"));
			assertTrue(code.contains("Breakout with volume"));
		}

	}

	@Nested
	@DisplayName("Moving Average Crossover Strategies")
	class MACrossoverStrategyTests {

		@Test
		@DisplayName("Should generate EMA crossover code")
		void shouldGenerateEMACrossoverCode() {
			Map<String, Object> params = Map.of("fast", 10, "slow", 50, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.MA_CROSSOVER_EMA, params);

			assertTrue(code.contains("FAST_PERIOD = 10"));
			assertTrue(code.contains("SLOW_PERIOD = 50"));
			assertTrue(code.contains("ema_fast"));
			assertTrue(code.contains("ema_slow"));
			assertTrue(code.contains("ewm(span="));
			assertTrue(code.contains("golden cross"));
		}

		@Test
		@DisplayName("Should generate SMA crossover code")
		void shouldGenerateSMACrossoverCode() {
			Map<String, Object> params = Map.of("fast", 20, "slow", 100, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.MA_CROSSOVER_SMA, params);

			assertTrue(code.contains("sma_fast"));
			assertTrue(code.contains("sma_slow"));
			assertTrue(code.contains("rolling(window="));
			assertFalse(code.contains("ewm(")); // Should not use EMA
		}

	}

	@Nested
	@DisplayName("Stochastic Oscillator Strategy")
	class StochasticStrategyTests {

		@Test
		@DisplayName("Should generate Stochastic code with %K and %D")
		void shouldGenerateStochasticCode() {
			Map<String, Object> params = Map.of("k_period", 14, "d_period", 3, "oversold", 20, "overbought", 80,
					"atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.STOCHASTIC, params);

			assertTrue(code.contains("K_PERIOD = 14"));
			assertTrue(code.contains("D_PERIOD = 3"));
			assertTrue(code.contains("OVERSOLD = 20"));
			assertTrue(code.contains("OVERBOUGHT = 80"));

			// Verify stochastic calculation
			assertTrue(code.contains("stoch_k"));
			assertTrue(code.contains("stoch_d"));
			assertTrue(code.contains("low_min"));
			assertTrue(code.contains("high_max"));

			// Verify crossover in oversold zone
			assertTrue(code.contains("prev_k <= prev_d and k > d"));
		}

	}

	@Nested
	@DisplayName("Swing Trading Strategy")
	class SwingTradingStrategyTests {

		@Test
		@DisplayName("Should generate swing trading code with thresholds")
		void shouldGenerateSwingTradingCode() {
			Map<String, Object> params = Map.of("buy_threshold", 10, "sell_threshold", 15, "lookback", 20,
					"atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.SWING_TRADING, params);

			assertTrue(code.contains("BUY_THRESHOLD = 10"));
			assertTrue(code.contains("SELL_THRESHOLD = 15"));
			assertTrue(code.contains("LOOKBACK = 20"));

			// Verify swing logic
			assertTrue(code.contains("rolling_high"));
			assertTrue(code.contains("rolling_low"));
			assertTrue(code.contains("pct_from_high"));
		}

	}

	@Nested
	@DisplayName("Combined ADX Strategy")
	class CombinedADXStrategyTests {

		@Test
		@DisplayName("Should generate combined RSI + ADX code")
		void shouldGenerateCombinedADXCode() {
			Map<String, Object> params = Map.of("adx_threshold", 25, "rsi_period", 14, "rsi_oversold", 30,
					"rsi_overbought", 70, "atr_multiplier", 2.0);

			String code = StrategyCodeTemplates.generateCode(StrategyType.COMBINED_ADX, params);

			assertTrue(code.contains("ADX_THRESHOLD = 25"));
			assertTrue(code.contains("RSI_PERIOD = 14"));

			// Verify ADX calculation
			assertTrue(code.contains("def calculate_adx"));
			assertTrue(code.contains("plus_di"));
			assertTrue(code.contains("minus_di"));

			// Verify combined logic
			assertTrue(code.contains("rsi < OVERSOLD and adx > ADX_THRESHOLD"));
		}

	}

	@Nested
	@DisplayName("Code Quality Validation")
	class CodeQualityTests {

		@Test
		@DisplayName("All strategy types should generate valid Python imports")
		void shouldGenerateValidImports() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertTrue(code.contains("import pandas as pd"), "Should import pandas for " + type);
				assertTrue(code.contains("import numpy as np"), "Should import numpy for " + type);
			}
		}

		@Test
		@DisplayName("All strategy types should include ATR-based stop loss")
		void shouldIncludeATRStopLoss() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertTrue(code.contains("calculate_atr") || code.contains("atr"),
						"Should include ATR calculation for " + type);
				assertTrue(code.contains("stop_loss") || code.contains("Stop Loss"),
						"Should include stop loss for " + type);
			}
		}

		@Test
		@DisplayName("All strategy types should include signal() calls")
		void shouldIncludeSignalCalls() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertTrue(code.contains("signal('BUY'"), "Should include BUY signal for " + type);
				assertTrue(code.contains("signal('SELL'"), "Should include SELL signal for " + type);
			}
		}

		@Test
		@DisplayName("All strategy types should include plot() calls for visualization")
		void shouldIncludePlotCalls() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertTrue(code.contains("plot("), "Should include plot() call for " + type);
			}
		}

		@Test
		@DisplayName("All strategy types should have position tracking")
		void shouldHavePositionTracking() {
			for (StrategyType type : StrategyType.values()) {
				Map<String, Object> params = getDefaultParamsForType(type);
				String code = StrategyCodeTemplates.generateCode(type, params);

				assertTrue(code.contains("position"), "Should track position for " + type);
			}
		}

		@Test
		@DisplayName("Should handle missing parameters with defaults")
		void shouldHandleMissingParameters() {
			// Empty params map
			Map<String, Object> params = Map.of();

			// Should not throw, should use defaults
			String code = StrategyCodeTemplates.generateCode(StrategyType.RSI_MEAN_REVERSION, params);
			assertNotNull(code);
			assertTrue(code.contains("RSI_PERIOD = 14")); // Default
		}

	}

	// Helper method
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
			case MOMENTUM_TRAILING -> Map.of("lookback", 20, "atr_multiplier", 2.0);
			case BREAKOUT_MOMENTUM -> Map.of("lookback", 20, "atr_multiplier", 2.0);
		};
	}

}
