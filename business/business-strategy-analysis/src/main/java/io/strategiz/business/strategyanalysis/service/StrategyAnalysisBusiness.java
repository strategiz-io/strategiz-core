package io.strategiz.business.strategyanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.business.aichat.LLMRouter;
import io.strategiz.business.historicalinsights.model.IndicatorRanking;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsService;
import io.strategiz.business.strategyanalysis.model.*;
import io.strategiz.client.base.llm.model.LLMMessage;
import io.strategiz.client.base.llm.model.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified strategy analysis business logic. Provides mode-based AI analysis for strategies.
 *
 * <p>Architecture: This is a BUSINESS module following the pattern: Controller → Service → Business
 *
 * <p>Modes:
 * - NO_SIGNALS: Diagnostic analysis when strategy produces no signals
 * - OPTIMIZATION: Improvement suggestions for working strategies
 * - GENERAL: General code review and analysis
 */
@Service
public class StrategyAnalysisBusiness {

	private static final Logger log = LoggerFactory.getLogger(StrategyAnalysisBusiness.class);

	private final LLMRouter llmRouter;

	private final HistoricalInsightsService historicalInsightsService;

	private final ObjectMapper objectMapper;

	public StrategyAnalysisBusiness(LLMRouter llmRouter, HistoricalInsightsService historicalInsightsService,
			ObjectMapper objectMapper) {
		this.llmRouter = llmRouter;
		this.historicalInsightsService = historicalInsightsService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Unified strategy analysis entry point.
	 * @param code Strategy Python code
	 * @param context Analysis context (symbol, timeframe, backtest results, etc.)
	 * @param mode Analysis mode (NO_SIGNALS, OPTIMIZATION, GENERAL)
	 * @return Structured analysis result with suggestions
	 */
	public AnalysisResult analyzeStrategy(String code, AnalysisContext context, AnalysisMode mode) {

		log.info("Analyzing strategy with mode: {}, symbol: {}", mode, context.getSymbol());

		try {
			// 1. Enrich context with market data (if not already present)
			if (context.getMarketContext() == null) {
				MarketContext marketContext = getMarketContext(context.getSymbol(), context.getPeriod());
				context.setMarketContext(marketContext);
			}

			// 2. Analyze code structure (if not already present)
			if (context.getDiagnostic() == null) {
				StrategyDiagnostic diagnostic = analyzeCode(code);
				context.setDiagnostic(diagnostic);
			}

			// 3. Build mode-specific prompt
			String systemPrompt = buildPrompt(code, context, mode);

			// 4. Build history with system prompt
			List<LLMMessage> history = new ArrayList<>();
			history.add(new LLMMessage("system", systemPrompt));

			// 5. Call AI
			String userPrompt = "Analyze this strategy and provide suggestions in the specified JSON format.";

			LLMResponse llmResponse = llmRouter.generateContent(userPrompt, history, "gemini-2.5-flash").block();

			if (llmResponse == null || llmResponse.getContent() == null) {
				log.error("LLM returned null response");
				return getFallbackResult(mode, context);
			}

			// 6. Parse response
			AnalysisResult result = parseResponse(llmResponse.getContent(), mode);
			result.setContext(context);
			result.setMode(mode);

			log.info("✅ Analysis complete: {} suggestions generated", result.getSuggestions().size());

			return result;

		}
		catch (Exception e) {
			log.error("Analysis failed, returning fallback", e);
			return getFallbackResult(mode, context);
		}
	}

	/**
	 * Get market context using Historical Insights (7-year analysis).
	 */
	private MarketContext getMarketContext(String symbol, String period) {
		log.debug("Fetching Historical Insights for {}", symbol);

		try {
			// Default: 7 years = 2600 days, 1D timeframe, no fundamentals
			String timeframe = "1D";
			int lookbackDays = 2600; // ~7 years
			boolean includeFundamentals = false;

			SymbolInsights insights = historicalInsightsService.analyzeSymbolForStrategyGeneration(symbol, timeframe,
					lookbackDays, includeFundamentals);

			MarketContext context = new MarketContext();
			context.setVolatilityRegime(insights.getVolatilityRegime());
			context.setTrendDirection(insights.getTrendDirection());
			context.setAverageVolatility(insights.getAvgVolatility());

			// Extract top indicator names as recommended indicators
			List<String> recommendedIndicators = insights.getTopIndicators().stream()
					.limit(5)
					.map(ranking -> ranking.getIndicatorName())
					.toList();
			context.setRecommendedIndicators(recommendedIndicators);

			// Build market condition description
			String marketCondition = String.format("%s trend with %s volatility", insights.getTrendDirection(),
					insights.getVolatilityRegime());
			context.setMarketCondition(marketCondition);

			return context;

		}
		catch (Exception e) {
			log.warn("Failed to fetch Historical Insights, using fallback", e);
			return MarketContext.createDefault();
		}
	}

	/**
	 * Analyze strategy code for common patterns and issues.
	 */
	private StrategyDiagnostic analyzeCode(String code) {
		log.debug("Analyzing strategy code structure");

		StrategyDiagnostic diagnostic = new StrategyDiagnostic();

		// Parse strategy structure using simple pattern matching
		boolean hasEntryRules = code.contains("return 'BUY'") || code.contains("return \"BUY\"");
		boolean hasExitRules = code.contains("return 'SELL'") || code.contains("return \"SELL\"");
		boolean hasIndicators = code.contains("indicators.") || code.contains("data[");
		boolean hasComplexConditions = code.contains(" and ") || code.contains(" or ");

		diagnostic.setHasEntryRules(hasEntryRules);
		diagnostic.setHasExitRules(hasExitRules);
		diagnostic.setHasIndicators(hasIndicators);
		diagnostic.setHasComplexConditions(hasComplexConditions);

		// Count conditions (rough estimate)
		int conditionCount = code.split("if |and |or ").length - 1;
		diagnostic.setConditionCount(Math.max(0, conditionCount));

		// Detect specific patterns
		diagnostic.setCrossoverDetected(
				code.contains("crossed_above") || code.contains("crossed_below") || code.contains("crossover"));
		diagnostic.setThresholdComparisons(code.contains(">") || code.contains("<"));

		return diagnostic;
	}

	/**
	 * Build mode-specific AI prompt.
	 */
	private String buildPrompt(String code, AnalysisContext context, AnalysisMode mode) {
		return switch (mode) {
			case NO_SIGNALS -> buildNoSignalsPrompt(code, context);
			case OPTIMIZATION -> buildOptimizationPrompt(code, context);
			case GENERAL -> buildGeneralAnalysisPrompt(code, context);
		};
	}

	/**
	 * Prompt for NO_SIGNALS mode: diagnostic focus.
	 */
	private String buildNoSignalsPrompt(String code, AnalysisContext context) {
		MarketContext mc = context.getMarketContext();
		StrategyDiagnostic diag = context.getDiagnostic();

		return String.format("""
				You are a trading strategy expert analyzing why a backtest produced ZERO signals.

				**Strategy Code:**
				```python
				%s
				```

				**Symbol:** %s
				**Timeframe:** %s
				**Period:** %s

				**Market Context (7-year analysis):**
				- Volatility Regime: %s
				- Trend Direction: %s
				- Average Volatility: %.2f%%
				- Market Condition: %s

				**Code Diagnostic:**
				- Has Entry Rules: %s
				- Has Exit Rules: %s
				- Condition Count: %d
				- Crossover Logic: %s

				**Task:** Identify why no signals were generated and provide 3-5 specific fixes.

				**Common Issues:**
				1. Conditions too strict (e.g., RSI < 10 rarely triggers)
				2. Indicator parameters unsuitable for timeframe (e.g., 200-day SMA on 1-month backtest)
				3. Logic errors (impossible conditions like x > 70 AND x < 30)
				4. Missing crossover implementation
				5. Strategy misaligned with market regime

				**Output Format (JSON ONLY):**
				{
				  "suggestions": [
				    {
				      "issue": "Brief problem description",
				      "recommendation": "Specific fix to apply",
				      "example": "Code example showing the fix",
				      "priority": "high|medium|low"
				    }
				  ],
				  "summary": "Overall explanation of why no signals occurred"
				}

				CRITICAL: Return ONLY valid JSON, no markdown formatting.
				""", code, context.getSymbol(), context.getTimeframe(), context.getPeriod(),
				mc.getVolatilityRegime(), mc.getTrendDirection(), mc.getAverageVolatility(), mc.getMarketCondition(),
				diag.isHasEntryRules(), diag.isHasExitRules(), diag.getConditionCount(), diag.isCrossoverDetected());
	}

	/**
	 * Prompt for OPTIMIZATION mode: improvement focus.
	 */
	private String buildOptimizationPrompt(String code, AnalysisContext context) {
		MarketContext mc = context.getMarketContext();

		// Extract backtest metrics
		double totalReturn = getDouble(context.getBacktestResults(), "totalReturn", 0.0);
		double winRate = getDouble(context.getBacktestResults(), "winRate", 0.0);
		double sharpeRatio = getDouble(context.getBacktestResults(), "sharpeRatio", 0.0);
		double maxDrawdown = getDouble(context.getBacktestResults(), "maxDrawdown", 0.0);
		double profitFactor = getDouble(context.getBacktestResults(), "profitFactor", 0.0);
		int totalTrades = getInt(context.getBacktestResults(), "totalTrades", 0);

		return String.format("""
				You are a trading strategy expert optimizing a backtested strategy.

				**Strategy Code:**
				```python
				%s
				```

				**Backtest Results:**
				- Total Return: %.2f%%
				- Win Rate: %.2f%%
				- Sharpe Ratio: %.2f
				- Max Drawdown: %.2f%%
				- Profit Factor: %.2f
				- Total Trades: %d

				**Market Context (7-year analysis):**
				- Volatility Regime: %s
				- Trend Direction: %s
				- Average Volatility: %.2f%%

				**Task:** Suggest 3-5 improvements to boost performance.

				**Focus Areas:**
				1. Parameter optimization (indicator periods, thresholds)
				2. Risk management (stop-loss, take-profit, position sizing)
				3. Entry/exit timing refinements
				4. Filter additions (volume, volatility, trend confirmation)
				5. Market regime alignment

				**Output Format (JSON ONLY):**
				{
				  "suggestions": [
				    {
				      "issue": "Current weakness",
				      "recommendation": "Specific improvement",
				      "example": "Code example",
				      "priority": "high|medium|low"
				    }
				  ],
				  "summary": "Overall optimization strategy"
				}

				CRITICAL: Return ONLY valid JSON, no markdown formatting.
				""", code, totalReturn, winRate, sharpeRatio, maxDrawdown, profitFactor, totalTrades,
				mc.getVolatilityRegime(), mc.getTrendDirection(), mc.getAverageVolatility());
	}

	/**
	 * Prompt for GENERAL mode: broad analysis.
	 */
	private String buildGeneralAnalysisPrompt(String code, AnalysisContext context) {
		return String.format("""
				You are a trading strategy expert providing general code review.

				**Strategy Code:**
				```python
				%s
				```

				**Task:** Provide 3-5 suggestions for improvement.

				**Output Format (JSON ONLY):**
				{
				  "suggestions": [
				    {
				      "issue": "Issue description",
				      "recommendation": "How to fix",
				      "example": "Code example",
				      "priority": "high|medium|low"
				    }
				  ],
				  "summary": "Overall assessment"
				}

				CRITICAL: Return ONLY valid JSON, no markdown formatting.
				""", code);
	}

	/**
	 * Parse AI response into AnalysisResult.
	 */
	private AnalysisResult parseResponse(String aiResponse, AnalysisMode mode) {
		try {
			// Clean response (remove markdown code blocks if present)
			String cleaned = aiResponse.trim();
			if (cleaned.startsWith("```json")) {
				cleaned = cleaned.substring(7);
			}
			if (cleaned.startsWith("```")) {
				cleaned = cleaned.substring(3);
			}
			if (cleaned.endsWith("```")) {
				cleaned = cleaned.substring(0, cleaned.length() - 3);
			}
			cleaned = cleaned.trim();

			// Parse JSON
			JsonNode root = objectMapper.readTree(cleaned);

			AnalysisResult result = new AnalysisResult();
			result.setSuccess(true);

			// Extract summary
			if (root.has("summary")) {
				result.setSummary(root.get("summary").asText());
			}

			// Extract suggestions
			List<Suggestion> suggestions = new ArrayList<>();
			if (root.has("suggestions") && root.get("suggestions").isArray()) {
				for (JsonNode suggNode : root.get("suggestions")) {
					Suggestion suggestion = new Suggestion();
					suggestion.setIssue(suggNode.has("issue") ? suggNode.get("issue").asText() : "");
					suggestion.setRecommendation(
							suggNode.has("recommendation") ? suggNode.get("recommendation").asText() : "");
					suggestion.setExample(suggNode.has("example") ? suggNode.get("example").asText() : "");
					suggestion.setPriority(suggNode.has("priority") ? suggNode.get("priority").asText() : "medium");
					suggestions.add(suggestion);
				}
			}
			result.setSuggestions(suggestions);

			return result;

		}
		catch (Exception e) {
			log.error("Failed to parse AI response as JSON: {}", aiResponse, e);
			// Return fallback
			AnalysisResult result = new AnalysisResult();
			result.setSuccess(false);
			result.setError("Failed to parse AI response. Using fallback suggestions.");
			result.setSuggestions(getFallbackSuggestions(mode));
			result.setSummary(
					"AI analysis failed. Here are some general suggestions based on common patterns.");
			return result;
		}
	}

	/**
	 * Fallback suggestions when AI fails.
	 */
	private AnalysisResult getFallbackResult(AnalysisMode mode, AnalysisContext context) {
		AnalysisResult result = new AnalysisResult();
		result.setContext(context);
		result.setMode(mode);
		result.setSuccess(true);
		result.setSuggestions(getFallbackSuggestions(mode));

		if (mode == AnalysisMode.NO_SIGNALS) {
			result.setSummary(
					"Unable to analyze with AI. Common issues: conditions too strict, parameter mismatches, or market regime misalignment.");
		}
		else {
			result.setSummary(
					"Unable to analyze with AI. Consider: risk management improvements, parameter optimization, and filter additions.");
		}

		return result;
	}

	private List<Suggestion> getFallbackSuggestions(AnalysisMode mode) {
		List<Suggestion> suggestions = new ArrayList<>();

		if (mode == AnalysisMode.NO_SIGNALS) {
			suggestions.add(new Suggestion("Conditions Too Strict",
					"Your entry conditions may be too restrictive. Try relaxing thresholds (e.g., RSI < 30 instead of < 20).",
					"if indicators['rsi'] < 30:  # Was: < 20", "high"));

			suggestions.add(new Suggestion("Indicator Parameters",
					"Check if indicator periods match your backtest timeframe.",
					"Use 20-day SMA for 1-year backtest, not 200-day", "high"));

			suggestions.add(new Suggestion("Market Alignment",
					"Ensure your strategy matches current market conditions.",
					"Add volatility filter or use mean-reversion in ranging markets", "medium"));
		}
		else {
			// OPTIMIZATION fallback
			suggestions.add(new Suggestion("Risk Management",
					"Add or tighten stop-loss levels to reduce drawdown.",
					"Add 2% stop loss: if position_pnl < -0.02: return 'SELL'", "high"));

			suggestions.add(new Suggestion("Parameter Tuning",
					"Optimize indicator periods using walk-forward analysis.", "Try RSI period = 21 instead of 14",
					"medium"));
		}

		return suggestions;
	}

	// Helper methods

	private double getDouble(Object map, String key, double defaultValue) {
		if (map == null)
			return defaultValue;
		try {
			Object value = ((java.util.Map<?, ?>) map).get(key);
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
		}
		catch (Exception e) {
			// Ignore
		}
		return defaultValue;
	}

	private int getInt(Object map, String key, int defaultValue) {
		if (map == null)
			return defaultValue;
		try {
			Object value = ((java.util.Map<?, ?>) map).get(key);
			if (value instanceof Number) {
				return ((Number) value).intValue();
			}
		}
		catch (Exception e) {
			// Ignore
		}
		return defaultValue;
	}

}
