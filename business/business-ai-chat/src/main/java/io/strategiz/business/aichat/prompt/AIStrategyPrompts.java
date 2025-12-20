package io.strategiz.business.aichat.prompt;

/**
 * AI prompts for the AI-First Strategy Builder. These prompts instruct Gemini to
 * generate trading strategies in a structured format that includes both visual
 * configuration (for the rule builder UI) and executable Python code.
 */
public class AIStrategyPrompts {

	/**
	 * System prompt for generating trading strategies. Instructs the AI to return
	 * structured JSON with both visual config and Python code.
	 */
	public static final String STRATEGY_GENERATION_SYSTEM = """
			You are an expert algorithmic trading strategy designer for Strategiz Labs.
			Your role is to generate trading strategies that include BOTH a visual configuration AND executable Python code.

			CRITICAL: Your response must be valid JSON with this exact structure:
			{
			  "visualConfig": {
			    "name": "<strategy-name>",
			    "description": "<brief-description>",
			    "rules": [
			      {
			        "id": "<unique-id>",
			        "type": "entry" | "exit",
			        "action": "BUY" | "SELL",
			        "conditions": [
			          {
			            "id": "<unique-id>",
			            "indicator": "<indicator-name>",
			            "comparator": "gt" | "lt" | "gte" | "lte" | "eq" | "crosses_above" | "crosses_below",
			            "value": <number>,
			            "valueType": "number" | "indicator",
			            "secondaryIndicator": "<optional-indicator-for-comparisons>"
			          }
			        ],
			        "logic": "AND" | "OR"
			      }
			    ],
			    "riskSettings": {
			      "stopLoss": <percentage>,
			      "takeProfit": <percentage>,
			      "positionSize": <percentage>,
			      "maxPositions": <number>
			    },
			    "aiSettings": {
			      "enabled": false,
			      "mode": "hybrid",
			      "minConfidence": 70
			    }
			  },
			  "pythonCode": "<complete-python-code-as-escaped-string>",
			  "summaryCard": "<1-2 sentence plain-English summary of the strategy>",
			  "riskLevel": "LOW" | "MEDIUM" | "HIGH" | "AGGRESSIVE",
			  "detectedIndicators": ["<indicator1>", "<indicator2>"],
			  "explanation": "<detailed explanation of the strategy logic>",
			  "suggestions": ["<refinement-suggestion-1>", "<refinement-suggestion-2>"]
			}

			Available indicators for visualConfig:
			- Price: price, open, high, low, close, volume
			- Trend: sma, ema (with period parameter, e.g., sma_20, ema_12)
			- Momentum: rsi, macd, macd_signal, macd_hist, stoch_k, stoch_d
			- Volatility: bb_upper, bb_middle, bb_lower, atr
			- Volume: vwap
			- AI-powered: ai_confidence, ai_technical_score, ai_sentiment_score, ai_pattern_score

			Comparators:
			- gt (>), lt (<), gte (>=), lte (<=), eq (=)
			- crosses_above (for crossover strategies)
			- crosses_below (for crossunder strategies)

			Python code requirements:
			- Use pandas and numpy for calculations
			- Access market data via 'data' DataFrame with columns: open, high, low, close, volume, timestamp
			- Generate signals using: add_signal(timestamp, 'BUY' | 'SELL', price, reason)
			- Track indicators using: add_indicator(name, timestamp, value)
			- Include proper indicator calculations (SMA, EMA, RSI, MACD, Bollinger Bands, etc.)
			- Add clear comments explaining the logic
			- Handle edge cases (NaN values, insufficient data)

			IMPORTANT: The visualConfig and pythonCode MUST implement the EXACT SAME trading logic.
			The visual representation should be a direct mapping of what the code does.

			Risk Level Guidelines:
			- LOW: Conservative strategies with wide stops (3-5%), small position sizes (1-2%)
			- MEDIUM: Balanced strategies with moderate stops (5-8%), medium positions (3-5%)
			- HIGH: Aggressive strategies with tighter stops (8-12%), larger positions (5-10%)
			- AGGRESSIVE: High-frequency or high-risk strategies with tight stops, large positions
			""";

	/**
	 * Prompt for refining an existing strategy based on user feedback.
	 */
	public static final String REFINEMENT_PROMPT = """
			The user wants to refine their existing strategy. Here is the current state:

			Current Visual Config:
			%s

			Current Python Code:
			%s

			User's refinement request: %s

			Apply the user's refinement request while maintaining consistency between visual config and code.
			Return the complete updated strategy in the same JSON format as the original generation.
			Ensure both visualConfig and pythonCode reflect the requested changes.
			""";

	/**
	 * Prompt for parsing Python code back into visual configuration.
	 */
	public static final String CODE_TO_VISUAL_PROMPT = """
			Analyze this Python trading strategy code and extract a visual rule configuration.

			Code:
			```python
			%s
			```

			Return a JSON object with:
			{
			  "visualConfig": { ... },
			  "canRepresent": true | false,
			  "warning": "<optional warning if code is too complex for visual representation>",
			  "extractedIndicators": ["<indicator1>", "<indicator2>"],
			  "extractedRules": "<plain-English description of the rules>"
			}

			Focus on extracting:
			- Entry conditions (what triggers BUY signals)
			- Exit conditions (what triggers SELL signals)
			- Risk settings if defined (stop loss, take profit, position size)
			- Logic operators (AND/OR) between conditions

			If the code is too complex for visual representation (e.g., uses loops, complex conditionals,
			or unsupported indicators), set canRepresent to false and provide a warning explaining why.
			Still extract as much as possible into the visualConfig.
			""";

	/**
	 * Prompt for explaining a specific element (rule, condition, or code section).
	 */
	public static final String EXPLAIN_ELEMENT_PROMPT = """
			Explain this trading strategy element in plain English for a user who may not be familiar with technical trading concepts.

			Element to explain:
			%s

			Context (full strategy if available):
			%s

			Provide:
			{
			  "explanation": "<clear, beginner-friendly explanation>",
			  "whyItMatters": "<why this element is important for the strategy>",
			  "potentialRisks": "<any risks or considerations>",
			  "alternatives": ["<alternative approach 1>", "<alternative approach 2>"]
			}

			Keep the explanation concise but thorough. Use analogies where helpful.
			Avoid jargon unless you explain it immediately after using it.
			""";

	/**
	 * Prompt for generating optimization suggestions based on backtest results.
	 */
	public static final String BACKTEST_OPTIMIZATION_PROMPT = """
			Analyze these backtest results and suggest optimizations for the trading strategy.

			Strategy:
			%s

			Backtest Results:
			- Total Return: %s%%
			- Total P&L: $%s
			- Win Rate: %s%%
			- Total Trades: %s
			- Profitable Trades: %s
			- Average Win: $%s
			- Average Loss: $%s
			- Profit Factor: %s
			- Max Drawdown: %s%%
			- Sharpe Ratio: %s

			Generate optimization suggestions in this format:
			{
			  "analysis": "<brief analysis of the strategy's performance>",
			  "suggestions": [
			    {
			      "id": "<unique-id>",
			      "priority": "high" | "medium" | "low",
			      "description": "<what to change>",
			      "rationale": "<why this would help>",
			      "impact": {
			        "metric": "<which metric this improves>",
			        "expectedBefore": <current-value>,
			        "expectedAfter": <estimated-new-value>
			      },
			      "patch": {
			        "visualConfig": { ... partial config changes ... },
			        "codeSnippet": "<code change to apply>"
			      }
			    }
			  ],
			  "overallAssessment": "<overall assessment of the strategy>"
			}

			Focus on:
			1. If drawdown is high (>30%), suggest risk management improvements (stops, position sizing)
			2. If win rate is low (<40%), suggest entry condition refinements
			3. If Sharpe ratio is low (<1.0), suggest volatility adjustments
			4. If profit factor is low (<1.5), suggest exit optimization
			5. If trades are too few, suggest loosening entry conditions
			6. If trades are too many, suggest adding confirmation indicators
			""";

	/**
	 * Prompt for parsing natural language backtest queries (e.g., "How would this do
	 * in the 2022 crash?").
	 */
	public static final String BACKTEST_QUERY_PROMPT = """
			Parse this natural language backtest query and extract structured parameters.

			Query: "%s"

			Current date context: %s

			Return:
			{
			  "queryType": "historical_period" | "comparison" | "scenario" | "date_range",
			  "startDate": "<YYYY-MM-DD or null>",
			  "endDate": "<YYYY-MM-DD or null>",
			  "periodDescription": "<human-readable period description>",
			  "comparisonStrategy": "<if comparing to another strategy, describe it>",
			  "scenario": "<if a specific scenario, describe it>",
			  "additionalContext": "<any relevant context for the backtest>"
			}

			Common period mappings:
			- "2022 crash" or "crypto winter" → 2022-01-01 to 2022-12-31
			- "COVID crash" → 2020-02-15 to 2020-04-15
			- "bull run 2021" → 2021-01-01 to 2021-11-15
			- "last month" → calculate from current date
			- "last year" → calculate from current date
			- "YTD" or "year to date" → January 1 of current year to now

			If comparing to "buy and hold" or "simple strategy", set queryType to "comparison".
			""";

	/**
	 * Prompt for detecting indicators from a partial prompt (for live preview).
	 */
	public static final String INDICATOR_PREVIEW_PROMPT = """
			Analyze this partial strategy description and identify which technical indicators are being referenced.

			Partial prompt: "%s"

			Return:
			{
			  "detectedIndicators": [
			    {
			      "name": "<indicator-name>",
			      "confidence": <0.0-1.0>,
			      "suggestedParameters": { ... }
			    }
			  ],
			  "strategyType": "momentum" | "mean-reversion" | "trend-following" | "volatility" | "hybrid" | "unknown",
			  "suggestedRiskLevel": "LOW" | "MEDIUM" | "HIGH" | "AGGRESSIVE" | null
			}

			Indicator detection hints:
			- "RSI", "oversold", "overbought" → rsi
			- "moving average", "SMA", "EMA", "crossover" → sma, ema
			- "MACD", "momentum", "divergence" → macd
			- "Bollinger", "bands", "squeeze" → bb_upper, bb_middle, bb_lower
			- "volume", "VWAP" → volume, vwap
			- "ATR", "volatility" → atr
			- "stochastic", "%K", "%D" → stoch_k, stoch_d

			Be conservative with confidence scores. Only report high confidence (>0.8) when the indicator is explicitly mentioned.
			""";

	/**
	 * Build the full system prompt with optional context.
	 */
	public static String buildGenerationPrompt(String symbols, String timeframe) {
		StringBuilder prompt = new StringBuilder(STRATEGY_GENERATION_SYSTEM);

		if (symbols != null && !symbols.isEmpty()) {
			prompt.append("\n\nTrading symbols: ").append(symbols);
		}
		if (timeframe != null && !timeframe.isEmpty()) {
			prompt.append("\nTimeframe: ").append(timeframe);
		}

		return prompt.toString();
	}

	/**
	 * Build a refinement prompt with current strategy state.
	 */
	public static String buildRefinementPrompt(String currentVisualConfig, String currentCode,
			String userRefinementRequest) {
		return String.format(REFINEMENT_PROMPT, currentVisualConfig, currentCode, userRefinementRequest);
	}

	/**
	 * Build a code-to-visual parsing prompt.
	 */
	public static String buildCodeToVisualPrompt(String pythonCode) {
		return String.format(CODE_TO_VISUAL_PROMPT, pythonCode);
	}

	/**
	 * Build an explanation prompt for a specific element.
	 */
	public static String buildExplainPrompt(String element, String fullStrategyContext) {
		return String.format(EXPLAIN_ELEMENT_PROMPT, element, fullStrategyContext != null ? fullStrategyContext : "N/A");
	}

	/**
	 * Build an optimization prompt with backtest results.
	 */
	public static String buildOptimizationPrompt(String strategy, double totalReturn, double totalPnL, double winRate,
			int totalTrades, int profitableTrades, double avgWin, double avgLoss, double profitFactor,
			double maxDrawdown, double sharpeRatio) {
		return String.format(BACKTEST_OPTIMIZATION_PROMPT, strategy, String.format("%.2f", totalReturn),
				String.format("%.2f", totalPnL), String.format("%.2f", winRate), totalTrades, profitableTrades,
				String.format("%.2f", avgWin), String.format("%.2f", avgLoss), String.format("%.2f", profitFactor),
				String.format("%.2f", maxDrawdown), String.format("%.2f", sharpeRatio));
	}

	/**
	 * Build a backtest query parsing prompt.
	 */
	public static String buildBacktestQueryPrompt(String query, String currentDate) {
		return String.format(BACKTEST_QUERY_PROMPT, query, currentDate);
	}

	/**
	 * Build an indicator preview prompt for live detection.
	 */
	public static String buildIndicatorPreviewPrompt(String partialPrompt) {
		return String.format(INDICATOR_PREVIEW_PROMPT, partialPrompt);
	}

}
