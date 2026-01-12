package io.strategiz.service.labs;

import io.strategiz.business.aichat.prompt.AIStrategyPrompts;
import io.strategiz.business.historicalinsights.model.FundamentalsInsights;
import io.strategiz.business.historicalinsights.model.IndicatorRanking;
import io.strategiz.business.historicalinsights.model.SymbolInsights;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manual verification tool for Historical Market Insights prompt enhancement.
 * Run this to see how the enhanced prompts look for different symbols.
 */
public class HistoricalInsightsPromptVerification {

	public static void main(String[] args) {
		System.out.println("=".repeat(100));
		System.out.println("ALPHA MODE PROMPT VERIFICATION");
		System.out.println("=".repeat(100));
		System.out.println();

		// Test Case 1: AAPL - Medium volatility, bullish trend
		System.out.println("TEST CASE 1: AAPL - MEDIUM VOLATILITY, BULLISH TREND");
		System.out.println("-".repeat(100));
		testSymbol(createAAPLInsights());
		System.out.println("\n\n");

		// Test Case 2: TSLA - High volatility, aggressive trend
		System.out.println("TEST CASE 2: TSLA - HIGH VOLATILITY, AGGRESSIVE TREND");
		System.out.println("-".repeat(100));
		testSymbol(createTSLAInsights());
		System.out.println("\n\n");

		// Test Case 3: SPY - Low volatility, mean-reverting
		System.out.println("TEST CASE 3: SPY - LOW VOLATILITY, MEAN-REVERTING");
		System.out.println("-".repeat(100));
		testSymbol(createSPYInsights());
		System.out.println("\n\n");

		// Test Case 4: BTC - Extreme volatility, trending
		System.out.println("TEST CASE 4: BTC - EXTREME VOLATILITY, TRENDING");
		System.out.println("-".repeat(100));
		testSymbol(createBTCInsights());
		System.out.println("\n\n");

		// Test Case 5: AAPL with Fundamentals
		System.out.println("TEST CASE 5: AAPL WITH FUNDAMENTALS");
		System.out.println("-".repeat(100));
		testSymbol(createAAPLWithFundamentals());
		System.out.println("\n\n");

		System.out.println("=".repeat(100));
		System.out.println("VERIFICATION COMPLETE");
		System.out.println("=".repeat(100));
	}

	private static void testSymbol(SymbolInsights insights) {
		// Build the enhanced prompt
		String enhancedPrompt = AIStrategyPrompts.buildHistoricalInsightsPrompt(insights);

		// Print the enhanced prompt
		System.out.println(enhancedPrompt);

		// Validate prompt contains key elements
		List<String> validations = new ArrayList<>();
		validations.add(checkContains(enhancedPrompt, "HISTORICAL MARKET INSIGHTS: 7-YEAR DATA ANALYSIS", "Historical Market Insights header"));
		validations.add(checkContains(enhancedPrompt, "Symbol: " + insights.getSymbol(), "Symbol"));
		validations
			.add(checkContains(enhancedPrompt, "Volatility Regime: " + insights.getVolatilityRegime(), "Volatility"));
		validations.add(checkContains(enhancedPrompt, "Trend Direction: " + insights.getTrendDirection(), "Trend"));
		validations
			.add(checkContains(enhancedPrompt, String.format("%.1f%%", insights.getAvgWinRate()), "Win Rate"));
		validations.add(checkContains(enhancedPrompt, "TOP PERFORMING INDICATORS", "Indicators section"));
		validations.add(checkContains(enhancedPrompt, "HISTORICAL INSIGHTS INSTRUCTIONS", "Instructions section"));

		// Print validation results
		System.out.println("\n" + "-".repeat(100));
		System.out.println("VALIDATION RESULTS:");
		boolean allPassed = true;
		for (String result : validations) {
			System.out.println(result);
			if (result.contains("❌")) {
				allPassed = false;
			}
		}
		System.out.println("\nOverall: " + (allPassed ? "✅ ALL CHECKS PASSED" : "❌ SOME CHECKS FAILED"));
		System.out.println("-".repeat(100));
	}

	private static String checkContains(String prompt, String expected, String description) {
		boolean contains = prompt.contains(expected);
		return String.format("%s %s: %s", contains ? "✅" : "❌", description, contains ? "Present" : "MISSING");
	}

	// Test Data Generators

	private static SymbolInsights createAAPLInsights() {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol("AAPL");
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(2600);
		insights.setAvgVolatility(2.50);
		insights.setVolatilityRegime("MEDIUM");
		insights.setAvgDailyRange(1.8);
		insights.setTrendDirection("BULLISH");
		insights.setTrendStrength(0.65);
		insights.setMeanReverting(false);
		insights.setAvgMaxDrawdown(8.5);
		insights.setAvgWinRate(65.0);
		insights.setRecommendedRiskLevel("MEDIUM");

		// Top indicators
		List<IndicatorRanking> rankings = new ArrayList<>();
		rankings.add(createIndicatorRanking("RSI", 0.68, Map.of("period", 14, "oversold", 28, "overbought", 72),
				"Achieved 68% win rate in mean-reverting periods"));
		rankings.add(createIndicatorRanking("MACD", 0.62, Map.of("fast", 12, "slow", 26, "signal", 9),
				"Strong trend confirmation with 62% accuracy"));
		rankings.add(createIndicatorRanking("Bollinger Bands", 0.58, Map.of("period", 20, "stddev", 2),
				"Effective for volatility breakouts"));
		insights.setTopIndicators(rankings);

		// Optimal parameters
		Map<String, Object> params = new HashMap<>();
		params.put("stop_loss_percent", 3.0);
		params.put("take_profit_percent", 9.0);
		params.put("rsi_period", 14);
		params.put("rsi_oversold", 28);
		params.put("rsi_overbought", 72);
		insights.setOptimalParameters(params);

		return insights;
	}

	private static SymbolInsights createTSLAInsights() {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol("TSLA");
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(1800);
		insights.setAvgVolatility(8.50);
		insights.setVolatilityRegime("HIGH");
		insights.setAvgDailyRange(4.2);
		insights.setTrendDirection("BULLISH");
		insights.setTrendStrength(0.78);
		insights.setMeanReverting(false);
		insights.setAvgMaxDrawdown(22.5);
		insights.setAvgWinRate(58.0);
		insights.setRecommendedRiskLevel("AGGRESSIVE");

		List<IndicatorRanking> rankings = new ArrayList<>();
		rankings.add(createIndicatorRanking("MACD", 0.64, Map.of("fast", 9, "slow", 21, "signal", 7),
				"Best for capturing TSLA's strong momentum moves"));
		rankings.add(createIndicatorRanking("ATR", 0.61, Map.of("period", 14),
				"Critical for position sizing in high volatility"));
		rankings.add(createIndicatorRanking("EMA Crossover", 0.56, Map.of("fast", 12, "slow", 26),
				"Captures trend changes effectively"));
		insights.setTopIndicators(rankings);

		Map<String, Object> params = new HashMap<>();
		params.put("stop_loss_percent", 6.0);
		params.put("take_profit_percent", 18.0);
		params.put("macd_fast", 9);
		params.put("macd_slow", 21);
		insights.setOptimalParameters(params);

		return insights;
	}

	private static SymbolInsights createSPYInsights() {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol("SPY");
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(2600);
		insights.setAvgVolatility(1.20);
		insights.setVolatilityRegime("LOW");
		insights.setAvgDailyRange(0.8);
		insights.setTrendDirection("SIDEWAYS");
		insights.setTrendStrength(0.35);
		insights.setMeanReverting(true);
		insights.setAvgMaxDrawdown(5.2);
		insights.setAvgWinRate(72.0);
		insights.setRecommendedRiskLevel("LOW");

		List<IndicatorRanking> rankings = new ArrayList<>();
		rankings.add(createIndicatorRanking("Bollinger Bands", 0.70, Map.of("period", 20, "stddev", 2),
				"72% win rate on mean reversion from bands"));
		rankings.add(createIndicatorRanking("RSI", 0.66, Map.of("period", 14, "oversold", 30, "overbought", 70),
				"Reliable for overbought/oversold conditions"));
		rankings.add(
				createIndicatorRanking("VWAP", 0.62, Map.of(), "Strong support/resistance in range-bound market"));
		insights.setTopIndicators(rankings);

		Map<String, Object> params = new HashMap<>();
		params.put("stop_loss_percent", 1.5);
		params.put("take_profit_percent", 4.5);
		params.put("bb_period", 20);
		params.put("bb_stddev", 2);
		insights.setOptimalParameters(params);

		return insights;
	}

	private static SymbolInsights createBTCInsights() {
		SymbolInsights insights = new SymbolInsights();
		insights.setSymbol("BTC");
		insights.setTimeframe("1D");
		insights.setDaysAnalyzed(2600);
		insights.setAvgVolatility(1250.00);
		insights.setVolatilityRegime("EXTREME");
		insights.setAvgDailyRange(5.8);
		insights.setTrendDirection("BULLISH");
		insights.setTrendStrength(0.82);
		insights.setMeanReverting(false);
		insights.setAvgMaxDrawdown(35.0);
		insights.setAvgWinRate(54.0);
		insights.setRecommendedRiskLevel("AGGRESSIVE");

		List<IndicatorRanking> rankings = new ArrayList<>();
		rankings.add(createIndicatorRanking("EMA Crossover", 0.59, Map.of("fast", 20, "slow", 50),
				"Best for BTC's strong trending behavior"));
		rankings.add(createIndicatorRanking("MACD", 0.57, Map.of("fast", 12, "slow", 26, "signal", 9),
				"Catches major trend reversals"));
		rankings.add(
				createIndicatorRanking("RSI", 0.52, Map.of("period", 14), "Useful for extreme overbought/oversold"));
		insights.setTopIndicators(rankings);

		Map<String, Object> params = new HashMap<>();
		params.put("stop_loss_percent", 8.0);
		params.put("take_profit_percent", 24.0);
		params.put("ema_fast", 20);
		params.put("ema_slow", 50);
		insights.setOptimalParameters(params);

		return insights;
	}

	private static SymbolInsights createAAPLWithFundamentals() {
		SymbolInsights insights = createAAPLInsights();

		// Add fundamentals data
		FundamentalsInsights fundamentals = new FundamentalsInsights();
		fundamentals.setSymbol("AAPL");
		fundamentals.setPeRatio(new BigDecimal("28.5"));
		fundamentals.setPbRatio(new BigDecimal("42.3"));
		fundamentals.setPsRatio(new BigDecimal("7.8"));
		fundamentals.setRoe(new BigDecimal("147.3"));
		fundamentals.setProfitMargin(new BigDecimal("25.8"));
		fundamentals.setRevenueGrowthYoY(new BigDecimal("8.2"));
		fundamentals.setEpsGrowthYoY(new BigDecimal("12.5"));
		fundamentals.setDividendYield(new BigDecimal("0.52"));
		fundamentals.setSummary("P/E: 28.5, ROE: 147.3%, Margin: 25.8%");

		insights.setFundamentals(fundamentals);

		return insights;
	}

	private static IndicatorRanking createIndicatorRanking(String name, double score, Map<String, Object> settings,
			String reason) {
		return new IndicatorRanking(name, score, settings, reason);
	}

}
