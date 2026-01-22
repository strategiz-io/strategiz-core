package io.strategiz.business.aichat.prompt;

import io.strategiz.business.historicalinsights.model.IndicatorRanking;
import io.strategiz.business.historicalinsights.model.SymbolInsights;

import java.util.Map;

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
			    "symbol": "<trading-symbol>",
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
			  "summaryCard": "<1 sentence summary>",
			  "riskLevel": "LOW" | "MEDIUM" | "HIGH" | "AGGRESSIVE"
			}

			IMPORTANT: Keep response concise. Only include visualConfig, pythonCode, summaryCard, and riskLevel.
			Do NOT include explanation, suggestions, or detectedIndicators - they are optional and waste tokens.

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

			Symbol Field:
			- The "symbol" field in visualConfig specifies which symbol/ticker to trade (e.g., "AAPL", "MSFT", "BTC")
			- CRITICAL: This MUST match the SYMBOL constant in the Python code
			- If the user's prompt mentions a specific symbol, use it. Otherwise, choose an appropriate default based on the strategy type

			Python code requirements (CRITICAL - MUST follow this exact format):

			1. EXTRACT CONSTANTS FROM NATURAL LANGUAGE (This is critical!):

			   The user will describe strategies in plain English. YOU must intelligently extract these values:

			   a) SYMBOL (REQUIRED):
			      - Extract from phrases like: "Buy AAPL", "trade Bitcoin", "for MSFT stock"
			      - If multiple symbols mentioned, use the PRIMARY one being traded
			      - If NO symbol mentioned, use: "SPY" (for stock strategies) or "BTC" (for crypto strategies)
			      - Examples:
			        * "Buy AAPL when RSI is oversold" → SYMBOL = 'AAPL'
			        * "MACD strategy for Tesla" → SYMBOL = 'TSLA'
			        * "Bitcoin momentum strategy" → SYMBOL = 'BTC'
			        * "Buy when RSI crosses 30" (no symbol) → SYMBOL = 'SPY' (default)

			   b) TIMEFRAME (REQUIRED):
			      - Extract from: "1 minute", "30 min", "hourly", "1 hour", "4h", "daily", "1 day", "weekly", etc.
			      - Convert to standard format: "1m", "30m", "1h", "4h", "1D", "1W", "1M"
			      - Convention: lowercase for minutes/hours (1m, 30m, 1h, 4h), uppercase for day+ (1D, 1W, 1M)
			      - If NO timeframe mentioned, default to: "1D" (daily)
			      - Examples:
			        * "on the 1 hour chart" → TIMEFRAME = '1h'
			        * "intraday 30 minute strategy" → TIMEFRAME = '30m'
			        * "daily MACD crossover" → TIMEFRAME = '1D'
			        * No mention → TIMEFRAME = '1D' (default)

			   c) STOP_LOSS (REQUIRED):
			      - Extract from: "3% stop", "stop loss at 5%", "cut losses at 2.5 percent", "5 point stop"
			      - Store as PERCENTAGE (not decimal): 3% → 3.0, NOT 0.03
			      - If NO stop loss mentioned, use intelligent default:
			        * Momentum strategies: 3.0
			        * Mean reversion: 2.0
			        * Trend following: 5.0
			        * Breakout strategies: 4.0
			      - Examples:
			        * "set 3% stop loss" → STOP_LOSS = 3.0
			        * "cut losses at 2.5%" → STOP_LOSS = 2.5
			        * No mention (MACD strategy) → STOP_LOSS = 3.0 (momentum default)

			   d) TAKE_PROFIT (REQUIRED):
			      - Extract from: "8% profit target", "take profit at 10%", "target 15 percent gain"
			      - Store as PERCENTAGE: 8% → 8.0
			      - If NO take profit mentioned, use intelligent default (typically 2-3x stop loss):
			        * If stop_loss = 3%, then take_profit = 9.0 (3:1 ratio)
			        * Adjust based on strategy type and risk level
			      - Examples:
			        * "8% take profit" → TAKE_PROFIT = 8.0
			        * "target 15% gain" → TAKE_PROFIT = 15.0
			        * No mention (with STOP_LOSS=3) → TAKE_PROFIT = 9.0 (3:1 ratio)

			   e) POSITION_SIZE (RECOMMENDED):
			      - Extract from: "5% of portfolio", "allocate 10% per trade", "2% position size"
			      - Default: 5.0 (5% of portfolio - conservative)

			   EXTRACTION EXAMPLE:
			   User says: "Create a MACD momentum strategy: Buy AAPL when MACD line crosses above the signal line
			   while both are below zero. Sell AAPL when MACD line crosses below the signal line while both are
			   above zero. Set 3% stop loss and 8% take profit, work on timeframe of 1 day"

			   YOU extract:
			   SYMBOL = 'AAPL'           # From "Buy AAPL"
			   TIMEFRAME = '1D'          # From "timeframe of 1 day" (uppercase for day+)
			   STOP_LOSS = 3.0           # From "3% stop loss"
			   TAKE_PROFIT = 8.0         # From "8% take profit"
			   POSITION_SIZE = 5         # Not specified, use default

			   Another example - minimal input (users often say very little):
			   User says: "RSI oversold strategy"

			   YOU extract:
			   SYMBOL = 'SPY'            # Not specified, default for stocks
			   TIMEFRAME = '1D'          # Not specified, default daily
			   STOP_LOSS = 3.0           # Not specified, momentum default
			   TAKE_PROFIT = 9.0         # Not specified, 3:1 ratio (3.0 * 3)
			   POSITION_SIZE = 5         # Not specified, use default

			   Even more minimal:
			   User says: "MACD crossover"

			   YOU extract:
			   SYMBOL = 'SPY'            # Not specified, default
			   TIMEFRAME = '1D'          # Not specified, default
			   STOP_LOSS = 3.0           # MACD is momentum, use 3.0
			   TAKE_PROFIT = 9.0         # 3:1 ratio
			   POSITION_SIZE = 5         # Default

			   REMEMBER: Users are NOT programmers! They won't specify constants.
			   YOU must extract from plain English and fill in intelligent defaults.
			   NEVER skip these constants - they are REQUIRED for execution.

			2. CODE STRUCTURE (REQUIRED):
			   The constants MUST appear at the top in this EXACT order:

			   ```python
			   import pandas as pd
			   import numpy as np

			   # Configuration (extracted from user's natural language prompt)
			   SYMBOL = 'AAPL'        # Extracted: "Buy AAPL when..."
			   TIMEFRAME = '1h'       # Extracted: "on 1 hour chart" OR default '1D' (lowercase for hours)
			   STOP_LOSS = 3.0        # Extracted: "3% stop loss" OR intelligent default
			   TAKE_PROFIT = 8.0      # Extracted: "8% take profit" OR 3:1 ratio default
			   POSITION_SIZE = 5      # Default 5% unless specified
			   ```

			3. ⚠️ MANDATORY - THIS IS THE MOST CRITICAL REQUIREMENT ⚠️
			   You MUST define a function named EXACTLY: def strategy(data):
			   - Takes a pandas DataFrame 'data' with columns: open, high, low, close, volume
			   - Returns EXACTLY one of these strings: 'BUY', 'SELL', or 'HOLD'
			   - Uses the LAST row of data (data.iloc[-1] or data['close'].iloc[-1]) for current values
			   - WITHOUT this function, the code WILL NOT WORK and will be REJECTED
			   - The function name MUST be exactly "strategy" (not "run_strategy", not "main", etc.)
			   - Example structure:
			     ```python
			     def strategy(data):
			         # Calculate indicators
			         rsi = calculate_rsi(data['close'])

			         # Entry/exit logic
			         if rsi.iloc[-1] < 30:
			             return 'BUY'
			         elif rsi.iloc[-1] > 70:
			             return 'SELL'
			         else:
			             return 'HOLD'
			     ```

			   ⚠️ VERIFY: Before returning, confirm your pythonCode contains "def strategy(data):"

			4. DO NOT:
			   - Download data (data is provided)
			   - Use yfinance, talib, or external data sources
			   - Create loops over the entire DataFrame (calculations on columns are OK)
			   - Use add_signal() or add_indicator() functions (they don't exist)
			   - Track positions or state between calls

			5. Calculate indicators using pandas/numpy:
			   - SMA: data['close'].rolling(window=20).mean()
			   - EMA: data['close'].ewm(span=12).mean()
			   - RSI: Use standard pandas calculation
			   - MACD: Use exponential moving averages
			   - Bollinger Bands: mean ± (std * 2)
			   - Add clear comments explaining the logic
			   - Handle edge cases (NaN values, insufficient data)

			IMPORTANT: The visualConfig and pythonCode MUST implement the EXACT SAME trading logic.
			The visual representation should be a direct mapping of what the code does.

			Risk Level Guidelines:
			- LOW: Conservative strategies with wide stops (3-5%), small position sizes (1-2%)
			- MEDIUM: Balanced strategies with moderate stops (5-8%), medium positions (3-5%)
			- HIGH: Aggressive strategies with tighter stops (8-12%), larger positions (5-10%)
			- AGGRESSIVE: High-frequency or high-risk strategies with tight stops, large positions

			---

			CRITICAL: Visual Rules and Python Code MUST Match Exactly

			Your response MUST include BOTH visualConfig and pythonCode that represent THE SAME STRATEGY.

			Visual Rules Generation Checklist (MANDATORY):
			1. ✓ Extract ALL conditions from your Python code into visual rules
			2. ✓ Use EXACT indicator IDs from schema (e.g., "rsi" NOT "RSI", "sma" NOT "SMA")
			3. ✓ Include BOTH entry AND exit rules (not just entry)
			4. ✓ Set logic to "AND" when all conditions must be true, "OR" when any can trigger
			5. ✓ Generate unique IDs for all rules (format: "rule-entry-1", "rule-exit-1")
			6. ✓ Generate unique IDs for all conditions (format: "cond-1", "cond-2")
			7. ✓ Ensure comparator values match Python code exactly (RSI < 30 → comparator: "lt", value: 30)
			8. ✓ For crossovers, use comparator="crossAbove" or "crossBelow" (not "gt" or "lt")
			9. ✓ For crossAbove/crossBelow comparators, secondaryIndicator is REQUIRED (NOT value)
			10. ✓ For indicator-to-indicator comparisons (MACD>Signal), secondaryIndicator is REQUIRED
			11. ✓ valueType MUST match the comparison: "indicator" when using secondaryIndicator, "number" when using value
			12. ✓ Validate every rule has: id, type, action, logic, conditions[]
			13. ✓ Validate every condition has: id, indicator, comparator, value OR secondaryIndicator, valueType

			CRITICAL: How to Use secondaryIndicator vs value Field

			This is the #1 most common error - PAY CLOSE ATTENTION:

			1. For NUMBER Comparisons (comparing indicator to a fixed number):
			   USE: "value" field with valueType="number"
			   Example: RSI < 30
			   {
			     "indicator": "rsi",
			     "comparator": "lt",
			     "value": 30,
			     "valueType": "number"
			   }

			2. For INDICATOR-TO-INDICATOR Comparisons (comparing two indicators):
			   USE: "secondaryIndicator" field with valueType="indicator"
			   DO NOT USE "value" field!
			   Example: MACD > MACD Signal
			   {
			     "indicator": "macd",
			     "comparator": "gt",
			     "secondaryIndicator": "macd_signal",  // ← REQUIRED!
			     "valueType": "indicator"              // ← MUST be "indicator"!
			   }

			3. For CROSSOVER Comparisons (price/indicator crosses above or below another):
			   USE: "secondaryIndicator" field with valueType="indicator"
			   USE: comparator="crossAbove" or "crossBelow" (NOT "gt" or "lt")
			   Example: Price crosses above Upper Bollinger Band
			   {
			     "indicator": "price",
			     "comparator": "crossAbove",
			     "secondaryIndicator": "bb_upper",     // ← REQUIRED!
			     "valueType": "indicator"              // ← MUST be "indicator"!
			   }

			COMMON MISTAKES TO AVOID (These cause visual rules to display incorrectly):
			❌ Using valueType="number" for indicator comparisons → WRONG!
			❌ Leaving secondaryIndicator null when comparing two indicators → WRONG!
			❌ Using empty string "" for value in crossover comparisons → WRONG!
			❌ Missing secondaryIndicator for MACD > MACD Signal → WRONG!
			❌ Using "value" field instead of "secondaryIndicator" for crossovers → WRONG!

			CORRECT Examples by Type:

			Type 1 - Number Comparison:
			Python: if rsi.iloc[-1] < 30:
			JSON: {"indicator": "rsi", "comparator": "lt", "value": 30, "valueType": "number"}

			Type 2 - Indicator Comparison:
			Python: if macd.iloc[-1] > macd_signal.iloc[-1]:
			JSON: {"indicator": "macd", "comparator": "gt", "secondaryIndicator": "macd_signal", "valueType": "indicator"}

			Type 3 - Crossover:
			Python: if (prev_close <= prev_bb_upper) and (current_close > current_bb_upper):
			JSON: {"indicator": "price", "comparator": "crossAbove", "secondaryIndicator": "bb_upper", "valueType": "indicator"}

			Example of Perfect Alignment:

			Python Code:
			```python
			if rsi < 30 and price > sma_20:
			    return "BUY"
			```

			Visual Rule (CORRECT - exact match):
			```json
			{
			  "id": "rule-entry-1",
			  "type": "entry",
			  "action": "BUY",
			  "logic": "AND",
			  "conditions": [
			    {
			      "id": "cond-1",
			      "indicator": "rsi",
			      "comparator": "lt",
			      "value": 30,
			      "valueType": "number"
			    },
			    {
			      "id": "cond-2",
			      "indicator": "price",
			      "comparator": "gt",
			      "value": "sma",
			      "valueType": "indicator",
			      "secondaryIndicator": "sma"
			    }
			  ]
			}
			```

			Visual Rule (WRONG - missing conditions or incorrect IDs):
			```json
			{
			  "indicator": "RSI",        // ❌ Wrong! Should be lowercase "rsi"
			  "comparator": "<",         // ❌ Wrong! Should be "lt"
			  "logic": "and"            // ❌ Wrong! Should be uppercase "AND"
			}
			```

			---

			COMPLETE EXAMPLE: Bollinger Band Breakout with MACD Confirmation

			This is a PERFECT example showing proper secondaryIndicator usage for crossovers and indicator comparisons:

			User Prompt:
			"Buy when price breaks above upper Bollinger Band with RSI between 50-70 and MACD confirmation.
			Sell when price breaks below lower Bollinger Band or RSI drops below 40."

			CORRECT Visual Config (THIS IS THE GOLD STANDARD):
			```json
			{
			  "visualConfig": {
			    "name": "Bollinger Band Breakout with Filters",
			    "symbol": "AAPL",
			    "rules": [
			      {
			        "id": "rule-entry-1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond-1",
			            "indicator": "price",
			            "comparator": "crossAbove",
			            "secondaryIndicator": "bb_upper",  // ← CRITICAL: What price crosses
			            "valueType": "indicator"           // ← MUST be "indicator"
			          },
			          {
			            "id": "cond-2",
			            "indicator": "rsi",
			            "comparator": "gt",
			            "value": 50,
			            "valueType": "number"
			          },
			          {
			            "id": "cond-3",
			            "indicator": "rsi",
			            "comparator": "lt",
			            "value": 70,
			            "valueType": "number"
			          },
			          {
			            "id": "cond-4",
			            "indicator": "macd",
			            "comparator": "gt",
			            "secondaryIndicator": "macd_signal",  // ← CRITICAL: MACD compared to Signal
			            "valueType": "indicator"              // ← MUST be "indicator"
			          }
			        ]
			      },
			      {
			        "id": "rule-exit-1",
			        "type": "exit",
			        "action": "SELL",
			        "logic": "OR",
			        "conditions": [
			          {
			            "id": "cond-5",
			            "indicator": "price",
			            "comparator": "crossBelow",
			            "secondaryIndicator": "bb_lower",  // ← CRITICAL: What price crosses
			            "valueType": "indicator"           // ← MUST be "indicator"
			          },
			          {
			            "id": "cond-6",
			            "indicator": "rsi",
			            "comparator": "lt",
			            "value": 40,
			            "valueType": "number"
			          }
			        ]
			      }
			    ]
			  }
			}
			```

			Matching Python Code:
			```python
			def strategy(data):
			    # Calculate indicators
			    rsi = calculate_rsi(data['close'])
			    macd, macd_signal, _ = calculate_macd(data['close'])
			    bb_upper, _, bb_lower = calculate_bollinger_bands(data['close'])

			    # Get current and previous values for crossover detection
			    current_close = data['close'].iloc[-1]
			    prev_close = data['close'].iloc[-2]
			    current_bb_upper = bb_upper.iloc[-1]
			    prev_bb_upper = bb_upper.iloc[-2]
			    current_bb_lower = bb_lower.iloc[-1]
			    prev_bb_lower = bb_lower.iloc[-2]
			    current_rsi = rsi.iloc[-1]
			    current_macd = macd.iloc[-1]
			    current_macd_signal = macd_signal.iloc[-1]

			    # Entry: Price crosses above BB upper + RSI 50-70 + MACD > Signal
			    entry_cross = (prev_close <= prev_bb_upper) and (current_close > current_bb_upper)
			    entry_rsi_range = (current_rsi > 50) and (current_rsi < 70)
			    entry_macd = current_macd > current_macd_signal

			    if entry_cross and entry_rsi_range and entry_macd:
			        return 'BUY'

			    # Exit: Price crosses below BB lower OR RSI < 40
			    exit_cross = (prev_close >= prev_bb_lower) and (current_close < current_bb_lower)
			    exit_rsi = current_rsi < 40

			    if exit_cross or exit_rsi:
			        return 'SELL'

			    return 'HOLD'
			```

			Notice how:
			1. Crossover conditions use secondaryIndicator (bb_upper, bb_lower) NOT value
			2. MACD comparison uses secondaryIndicator (macd_signal) NOT value
			3. RSI number comparisons use value NOT secondaryIndicator
			4. All secondaryIndicator conditions have valueType="indicator"
			5. All value conditions have valueType="number"

			---

			VERIFICATION STEPS (BEFORE RETURNING RESPONSE):

			Before submitting your response, you MUST:
			1. Review your Python code line by line
			2. Review your visual rules line by line
			3. Verify they represent EXACTLY the same strategy logic
			4. Check that ALL conditions from code appear in visual rules
			5. Check that ALL visual rules have corresponding code
			6. Verify indicator IDs are lowercase and match schema
			7. Verify comparator IDs are correct (not symbols like "<", ">")
			8. Verify logic operators are uppercase ("AND", "OR")
			9. Verify all IDs are generated and unique
			10. If ANY mismatch found, regenerate the visual rules to match code

			Remember: Users will see BOTH the visual rules and code. If they don't match,
			the user will notice the inconsistency and lose trust in the system.
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

			CRITICAL INSTRUCTIONS:
			1. Extract each condition with EXACT values from the Python code
			2. Python "and" operator → JSON logic: "AND"
			3. Python "or" operator → JSON logic: "OR"
			4. For numeric comparisons (rsi < 30): use valueType: "number"
			5. For indicator comparisons (macd > macd_signal): use valueType: "indicator" with secondaryIndicator
			6. Detect crossovers: if curr > X and prev <= X → use comparator: "crossAbove"
			7. Each rule MUST have an "id" field (generate unique IDs like "rule_1", "rule_2")
			8. Each condition MUST have an "id" field (generate unique IDs like "cond_1", "cond_2")

			EXAMPLE 1 - Simple RSI Strategy:
			Python Code:
			```python
			if rsi.iloc[-1] < 30:
			    return 'BUY'
			elif rsi.iloc[-1] > 70:
			    return 'SELL'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "symbol": "AAPL",
			    "name": "RSI Strategy",
			    "description": "Buy when RSI below 30, sell when above 70",
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "rsi",
			            "comparator": "lt",
			            "value": 30,
			            "valueType": "number"
			          }
			        ]
			      },
			      {
			        "id": "rule_2",
			        "type": "exit",
			        "action": "SELL",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_2",
			            "indicator": "rsi",
			            "comparator": "gt",
			            "value": 70,
			            "valueType": "number"
			          }
			        ]
			      }
			    ],
			    "riskSettings": {
			      "stopLoss": 5.0,
			      "takeProfit": 10.0,
			      "positionSize": 5,
			      "maxPositions": 1
			    }
			  },
			  "canRepresent": true,
			  "extractedIndicators": ["rsi"]
			}

			EXAMPLE 2 - MACD with AND Logic:
			Python Code:
			```python
			if macd.iloc[-1] > macd_signal.iloc[-1] and macd.iloc[-1] < 0:
			    return 'BUY'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "macd",
			            "comparator": "gt",
			            "value": "macd_signal",
			            "valueType": "indicator",
			            "secondaryIndicator": "macd_signal"
			          },
			          {
			            "id": "cond_2",
			            "indicator": "macd",
			            "comparator": "lt",
			            "value": 0,
			            "valueType": "number"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			EXAMPLE 3 - Crossover Detection:
			Python Code:
			```python
			# SMA crossover
			if sma_20.iloc[-1] > sma_50.iloc[-1] and sma_20.iloc[-2] <= sma_50.iloc[-2]:
			    return 'BUY'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "sma",
			            "comparator": "crossAbove",
			            "value": "sma_50",
			            "valueType": "indicator",
			            "secondaryIndicator": "sma_50"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			EXAMPLE 4 - OR Logic:
			Python Code:
			```python
			if rsi.iloc[-1] < 30 or stoch_k.iloc[-1] < 20:
			    return 'BUY'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "OR",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "rsi",
			            "comparator": "lt",
			            "value": 30,
			            "valueType": "number"
			          },
			          {
			            "id": "cond_2",
			            "indicator": "stoch_k",
			            "comparator": "lt",
			            "value": 20,
			            "valueType": "number"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			EXAMPLE 5 - Exit Rule:
			Python Code:
			```python
			if price.iloc[-1] <= bb_middle.iloc[-1]:
			    return 'SELL'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "exit",
			        "action": "SELL",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "price",
			            "comparator": "lte",
			            "value": "bb_middle",
			            "valueType": "indicator",
			            "secondaryIndicator": "bb_middle"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			EXAMPLE 6 - Complex Multi-Condition:
			Python Code:
			```python
			if rsi.iloc[-1] < 30 and price.iloc[-1] < bb_lower.iloc[-1] and volume.iloc[-1] > 100000:
			    return 'BUY'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "rsi",
			            "comparator": "lt",
			            "value": 30,
			            "valueType": "number"
			          },
			          {
			            "id": "cond_2",
			            "indicator": "price",
			            "comparator": "lt",
			            "value": "bb_lower",
			            "valueType": "indicator",
			            "secondaryIndicator": "bb_lower"
			          },
			          {
			            "id": "cond_3",
			            "indicator": "volume",
			            "comparator": "gt",
			            "value": 100000,
			            "valueType": "number"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			EXAMPLE 7 - Complete Strategy with Entry and Exit:
			Python Code:
			```python
			# Entry
			if price.iloc[-1] > sma_20.iloc[-1] and price.iloc[-2] <= sma_20.iloc[-2]:
			    return 'BUY'
			# Exit
			if price.iloc[-1] < sma_20.iloc[-1] and price.iloc[-2] >= sma_20.iloc[-2]:
			    return 'SELL'
			```

			Correct JSON Output:
			{
			  "visualConfig": {
			    "rules": [
			      {
			        "id": "rule_1",
			        "type": "entry",
			        "action": "BUY",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_1",
			            "indicator": "price",
			            "comparator": "crossAbove",
			            "value": "sma",
			            "valueType": "indicator",
			            "secondaryIndicator": "sma"
			          }
			        ]
			      },
			      {
			        "id": "rule_2",
			        "type": "exit",
			        "action": "SELL",
			        "logic": "AND",
			        "conditions": [
			          {
			            "id": "cond_2",
			            "indicator": "price",
			            "comparator": "crossBelow",
			            "value": "sma",
			            "valueType": "indicator",
			            "secondaryIndicator": "sma"
			          }
			        ]
			      }
			    ]
			  },
			  "canRepresent": true
			}

			COMPARATOR MAPPING:
			- Python < → JSON "lt"
			- Python <= → JSON "lte"
			- Python > → JSON "gt"
			- Python >= → JSON "gte"
			- Python == → JSON "eq"
			- Crossover pattern (curr > X and prev <= X) → JSON "crossAbove"
			- Crossunder pattern (curr < X and prev >= X) → JSON "crossBelow"

			FIELD REQUIREMENTS:
			- Every rule MUST have: id, type, action, logic, conditions
			- Every condition MUST have: id, indicator, comparator, value, valueType
			- indicator must be a valid indicator ID (rsi, macd, sma, ema, etc.)
			- comparator must be one of: lt, lte, gt, gte, eq, crossAbove, crossBelow
			- valueType must be either "number" or "indicator"
			- If valueType is "indicator", include secondaryIndicator field

			TOO COMPLEX SCENARIOS (set canRepresent: false):
			- Code uses loops (for, while)
			- Complex nested conditionals (more than 2 levels deep)
			- Custom functions beyond standard indicators
			- Unsupported indicators not in the schema

			Now analyze the provided Python code above and return ONLY valid JSON matching these examples.
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
	public static String buildGenerationPrompt(String symbols, String timeframe, String visualEditorSchema) {
		StringBuilder prompt = new StringBuilder(STRATEGY_GENERATION_SYSTEM);

		if (symbols != null && !symbols.isEmpty()) {
			prompt.append("\n\nTrading symbols: ").append(symbols);
		}
		if (timeframe != null && !timeframe.isEmpty()) {
			prompt.append("\nTimeframe: ").append(timeframe);
		}
		if (visualEditorSchema != null && !visualEditorSchema.isEmpty()) {
			prompt.append("\n\n").append(visualEditorSchema);
		}

		return prompt.toString();
	}

	/**
	 * Build a refinement prompt with current strategy state.
	 */
	public static String buildRefinementPrompt(String currentVisualConfig, String currentCode,
			String userRefinementRequest, String visualEditorSchema) {
		String basePrompt = String.format(REFINEMENT_PROMPT, currentVisualConfig, currentCode, userRefinementRequest);
		if (visualEditorSchema != null && !visualEditorSchema.isEmpty()) {
			return basePrompt + "\n\n" + visualEditorSchema;
		}
		return basePrompt;
	}

	/**
	 * Build a code-to-visual parsing prompt.
	 */
	public static String buildCodeToVisualPrompt(String pythonCode, String visualEditorSchema) {
		String basePrompt = String.format(CODE_TO_VISUAL_PROMPT, pythonCode);

		// Append visual editor schema if provided for better AI guidance
		if (visualEditorSchema != null && !visualEditorSchema.isEmpty()) {
			return basePrompt + "\n\n" + visualEditorSchema;
		}

		return basePrompt;
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
		// Escape % characters in strategy to prevent String.format interpretation
		String escapedStrategy = strategy != null ? strategy.replace("%", "%%") : "";
		return String.format(BACKTEST_OPTIMIZATION_PROMPT, escapedStrategy, String.format("%.2f", totalReturn),
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

	/**
	 * Builds an enhanced prompt for Historical Market Insights (Feeling Lucky mode).
	 * This method generates a comprehensive analysis section based on 7 years of historical data.
	 * Analyzes volatility, indicator effectiveness, optimal parameters, and market characteristics.
	 *
	 * @param insights Historical market analysis results containing volatility, indicators, and trends
	 * @return Formatted prompt section with historical insights for AI strategy generation
	 */
	public static String buildHistoricalInsightsPrompt(SymbolInsights insights) {
		StringBuilder prompt = new StringBuilder();

		// Include comprehensive technical analysis knowledge (split into parts due to Java string length limits)
		prompt.append(TECHNICAL_ANALYSIS_KNOWLEDGE_PART1)
				.append(TECHNICAL_ANALYSIS_KNOWLEDGE_PART2)
				.append(TECHNICAL_ANALYSIS_KNOWLEDGE_PART3)
				.append("\n\n");

		prompt.append("=".repeat(80)).append("\n");
		prompt.append("HISTORICAL MARKET INSIGHTS: 7-YEAR DATA ANALYSIS\n");
		prompt.append("=".repeat(80)).append("\n\n");

		prompt.append(String.format("Symbol: %s\n", insights.getSymbol()));
		prompt.append(String.format("Timeframe: %s\n", insights.getTimeframe()));
		prompt.append(String.format("Analysis Period: %d days (~%.1f years of data)\n\n",
				insights.getDaysAnalyzed(), insights.getDaysAnalyzed() / 365.25));

		// Volatility Profile
		prompt.append("VOLATILITY PROFILE:\n");
		prompt.append(String.format("- Average ATR: $%.2f\n", insights.getAvgVolatility()));
		prompt.append(String.format("- Volatility Regime: %s\n", insights.getVolatilityRegime()));
		prompt.append(String.format("- Average Daily Range: %.2f%%\n", insights.getAvgDailyRange()));

		// Calculate recommended stop-loss and take-profit based on volatility
		double recommendedStopLoss = computeRecommendedStopLoss(insights);
		double recommendedTakeProfit = computeRecommendedTakeProfit(insights);
		prompt.append(String.format("→ Recommended Stop-Loss: %.1f%% | Take-Profit: %.1f%%\n\n",
				recommendedStopLoss, recommendedTakeProfit));

		// Top Performing Indicators
		if (!insights.getTopIndicators().isEmpty()) {
			prompt.append("TOP PERFORMING INDICATORS (Ranked by Historical Effectiveness):\n");
			for (int i = 0; i < insights.getTopIndicators().size(); i++) {
				IndicatorRanking ranking = insights.getTopIndicators().get(i);
				prompt.append(String.format("%d. %s (Score: %.2f) - %s\n",
						i + 1, ranking.getIndicatorName(), ranking.getEffectivenessScore(),
						ranking.getReason()));
				prompt.append(String.format("   Optimal Settings: %s\n\n",
						formatOptimalSettings(ranking.getOptimalSettings())));
			}
		}

		// Optimal Parameters
		if (insights.getOptimalParameters() != null && !insights.getOptimalParameters().isEmpty()) {
			prompt.append("OPTIMAL PARAMETERS DISCOVERED:\n");
			insights.getOptimalParameters().forEach((key, value) ->
					prompt.append(String.format("- %s: %s\n", key, value)));
			prompt.append("\n");
		}

		// Market Characteristics
		prompt.append("MARKET CHARACTERISTICS:\n");
		prompt.append(String.format("- Trend Direction: %s (Strength: %.0f%%)\n",
				insights.getTrendDirection(), insights.getTrendStrength() * 100));
		prompt.append(String.format("- Market Type: %s\n",
				insights.isMeanReverting() ? "Mean-Reverting" : "Trending"));
		prompt.append(String.format("- Recommended Strategy Type: %s\n\n",
				insights.isMeanReverting() ? "Mean-Reversion" : "Trend-Following"));

		// CRITICAL: Add strong guidance for trending markets
		if (!insights.isMeanReverting() && "BULLISH".equals(insights.getTrendDirection())) {
			prompt.append("⚠️ CRITICAL WARNING - STRONGLY TRENDING BULL MARKET ⚠️\n");
			prompt.append("=".repeat(60)).append("\n");
			prompt.append("This market has been in a STRONG UPTREND. In bull markets:\n");
			prompt.append("- BUY AND HOLD will return 50-150%+ over multi-year periods\n");
			prompt.append("- ANY strategy that sits out of the market will LOSE to buy-and-hold\n");
			prompt.append("- Conservative strategies with <20 trades are GUARANTEED TO FAIL\n\n");
			prompt.append("YOU MUST:\n");
			prompt.append("1. Use a TREND-FOLLOWING strategy, NOT mean-reversion\n");
			prompt.append("2. Generate at LEAST 30-50+ trades over the backtest period\n");
			prompt.append("3. Stay INVESTED most of the time (be in a position 60-80% of days)\n");
			prompt.append("4. Use sensitive entry signals that trigger frequently in uptrends\n");
			prompt.append("5. Let winning trades RUN (use trailing stops or wide take-profit)\n");
			prompt.append("6. Only exit on clear trend reversal signals, NOT quick profit-taking\n\n");
			prompt.append("WHAT WILL FAIL:\n");
			prompt.append("- RSI oversold strategies (rarely triggers in bull markets)\n");
			prompt.append("- Mean-reversion (market doesn't revert in strong trends)\n");
			prompt.append("- Strategies waiting for 'perfect' setups (miss the trend)\n");
			prompt.append("- Conservative 5-10 trade strategies (not enough exposure)\n\n");
			prompt.append("WHAT WILL WIN:\n");
			prompt.append("- Trend-following with moving average systems\n");
			prompt.append("- Breakout strategies that enter on momentum\n");
			prompt.append("- Pullback-to-moving-average entries in uptrends\n");
			prompt.append("- STAY IN THE MARKET and ride the trend!\n");
			prompt.append("=".repeat(60)).append("\n\n");
		}
		else if (!insights.isMeanReverting() && "BEARISH".equals(insights.getTrendDirection())) {
			prompt.append("⚠️ CRITICAL WARNING - STRONGLY TRENDING BEAR MARKET ⚠️\n");
			prompt.append("=".repeat(60)).append("\n");
			prompt.append("This market has been in a STRONG DOWNTREND. In bear markets:\n");
			prompt.append("- Staying OUT of the market or going SHORT beats buy-and-hold\n");
			prompt.append("- Any 'buy the dip' strategy will get destroyed\n\n");
			prompt.append("YOU MUST:\n");
			prompt.append("1. Use defensive strategies or SHORT-selling approaches\n");
			prompt.append("2. Consider cash preservation as a valid strategy\n");
			prompt.append("3. Only enter LONG on very strong reversal signals\n");
			prompt.append("=".repeat(60)).append("\n\n");
		}

		// Risk Insights
		prompt.append("RISK INSIGHTS:\n");
		prompt.append(String.format("- Historical Avg Max Drawdown: %.1f%%\n", insights.getAvgMaxDrawdown()));
		prompt.append(String.format("- Historical Avg Win Rate: %.1f%%\n", insights.getAvgWinRate()));
		prompt.append(String.format("- Recommended Risk Level: %s\n\n", insights.getRecommendedRiskLevel()));

		// Fundamentals (if included)
		if (insights.getFundamentals() != null) {
			prompt.append("FUNDAMENTALS SNAPSHOT:\n");
			prompt.append(insights.getFundamentals().getSummary()).append("\n\n");
		}

		// CRITICAL: Major Price Turning Points (Perfect Hindsight for Feeling Lucky)
		if (insights.getTurningPoints() != null && !insights.getTurningPoints().isEmpty()) {
			prompt.append("=".repeat(80)).append("\n");
			prompt.append("🎯 PERFECT HINDSIGHT: OPTIMAL BUY/SELL POINTS\n");
			prompt.append("=".repeat(80)).append("\n");
			prompt.append("Below are the EXACT dates and prices where you should have bought and sold.\n");
			prompt.append("Your strategy MUST trigger signals near these turning points to beat buy-and-hold.\n\n");

			prompt.append("OPTIMAL TRADING POINTS (use these to design your strategy):\n");
			java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
					.ofPattern("MMM dd, yyyy")
					.withZone(java.time.ZoneId.of("UTC"));

			for (var tp : insights.getTurningPoints()) {
				String action = tp.getType().name().equals("TROUGH") ? "🟢 BUY " : "🔴 SELL";
				String date = formatter.format(tp.getTimestamp());
				prompt.append(String.format("  %s @ %s: $%.2f", action, date, tp.getPrice()));
				if (tp.getPriceChangeFromPrevious() != 0) {
					prompt.append(String.format(" (%+.1f%% from previous, %d days later)",
							tp.getPriceChangeFromPrevious(), tp.getDaysFromPrevious()));
				}
				prompt.append("\n");
			}
			prompt.append("\n");

			// Calculate average % moves from turning points for the AI
			double avgDropToTrough = 0, avgRiseFromTrough = 0;
			int troughCount = 0, peakCount = 0;
			for (var tp : insights.getTurningPoints()) {
				if (tp.getPriceChangeFromPrevious() != 0) {
					if (tp.getType().name().equals("TROUGH")) {
						avgDropToTrough += Math.abs(tp.getPriceChangeFromPrevious());
						troughCount++;
					} else {
						avgRiseFromTrough += tp.getPriceChangeFromPrevious();
						peakCount++;
					}
				}
			}
			if (troughCount > 0) avgDropToTrough /= troughCount;
			if (peakCount > 0) avgRiseFromTrough /= peakCount;

			prompt.append("SWING TRADING PATTERN ANALYSIS:\n");
			prompt.append(String.format("- Average %% DROP before BUY points (troughs): %.1f%%\n", avgDropToTrough));
			prompt.append(String.format("- Average %% RISE before SELL points (peaks): %.1f%%\n", avgRiseFromTrough));
			prompt.append("- Use these percentages to create your entry/exit rules!\n\n");

			prompt.append("🎯 SIMPLE SWING STRATEGY APPROACH (RECOMMENDED):\n");
			prompt.append("The turning points above show a clear pattern of swings. Create a strategy that:\n");
			prompt.append(String.format("1. BUY when price drops ~%.0f%% from recent high (20-day lookback)\n", avgDropToTrough * 0.8));
			prompt.append(String.format("2. SELL when price rises ~%.0f%% from entry OR from recent low\n", avgRiseFromTrough * 0.8));
			prompt.append("3. Use simple price-based rules, NOT complex indicators\n");
			prompt.append("4. Track recent high/low to detect swing opportunities\n\n");

			prompt.append("EXAMPLE PYTHON LOGIC:\n");
			prompt.append("```python\n");
			prompt.append("# Track recent high/low for swing detection\n");
			prompt.append("recent_high = data['high'].rolling(20).max()\n");
			prompt.append("recent_low = data['low'].rolling(20).min()\n");
			prompt.append("pct_from_high = (data['close'] - recent_high) / recent_high * 100\n");
			prompt.append("pct_from_low = (data['close'] - recent_low) / recent_low * 100\n\n");
			prompt.append(String.format("# BUY when price drops %.0f%% from recent high\n", avgDropToTrough * 0.8));
			prompt.append(String.format("buy_signal = pct_from_high < -%.0f\n", avgDropToTrough * 0.8));
			prompt.append(String.format("# SELL when price rises %.0f%% from recent low\n", avgRiseFromTrough * 0.8));
			prompt.append(String.format("sell_signal = pct_from_low > %.0f\n", avgRiseFromTrough * 0.8));
			prompt.append("```\n\n");

			prompt.append("⚠️ THIS IS YOUR CHEAT SHEET - USE IT!\n");
			prompt.append("The turning points show EXACTLY where the swings occurred.\n");
			prompt.append("Create a simple swing strategy that captures these moves.\n");
			prompt.append("DO NOT overcomplicate with indicators - simple price rules work best.\n\n");
		}

		// AI Instructions - CRITICAL: Primary goal is beating buy and hold
		prompt.append("=".repeat(80)).append("\n");
		prompt.append("🎯 FEELING LUCKY MODE: CRITICAL MISSION\n");
		prompt.append("=".repeat(80)).append("\n");
		prompt.append("PRIMARY GOAL: Generate a strategy that BEATS BUY AND HOLD returns\n");
		prompt.append("- You have 7 YEARS of historical data showing what works and what doesn't\n");
		prompt.append("- Use this data to create a strategy with ALPHA (returns ABOVE buy and hold)\n");
		prompt.append("- If a simple buy-and-hold would outperform, this strategy is WORTHLESS\n");
		prompt.append("- The user chose 'Feeling Lucky' expecting SUPERIOR performance\n\n");

		prompt.append("HOW FEELING LUCKY WORKS:\n");
		prompt.append("- The user may provide context/hints OR leave it blank for full autonomy\n");
		prompt.append("- If context provided: Use it as a guideline but YOU decide all missing details\n");
		prompt.append("- If NO context provided: Autonomously create a complete strategy from scratch\n");
		prompt.append("- Either way: YOU are the expert making strategic decisions based on 7 years of data\n");
		prompt.append(String.format("- Either way: Strategy MUST beat buy and hold for %s\n\n", insights.getSymbol()));

		prompt.append("MANDATORY REQUIREMENTS:\n");
		prompt.append("1. 🎯 BEAT BUY AND HOLD - This is non-negotiable\n");
		prompt.append("2. Use SIMPLE SWING TRADING rules based on the turning points data above\n");
		prompt.append("3. BUY on dips (price drops X% from recent high), SELL on rallies (price rises Y% from low)\n");
		prompt.append("4. The turning points show you EXACTLY the swing magnitudes - use them!\n");
		prompt.append("5. DO NOT use complex indicators (RSI, MACD, etc.) - simple price rules work better\n");
		prompt.append(String.format("6. In summaryCard, explain how this BEATS buy and hold for %s\n\n",
				insights.getSymbol()));

		prompt.append("SWING TRADING APPROACH:\n");
		prompt.append("- The turning points data shows the EXACT swings that occurred\n");
		prompt.append("- Calculate avg % drop to troughs (buy zones) and avg % rise to peaks (sell zones)\n");
		prompt.append("- Create rules: BUY when price is X% below recent high, SELL when price is Y% above recent low\n");
		prompt.append("- Keep it SIMPLE - price-based swing trading captures these moves best\n");
		prompt.append("- Avoid overcomplicating with indicators - they often miss the obvious swings\n\n");

		prompt.append("TRADING BEST PRACTICES (Critical for Profitability):\n");
		prompt.append("1. RISK MANAGEMENT:\n");
		prompt.append("   - Never risk more than 2-3% per trade\n");
		prompt.append("   - Use proper position sizing based on account equity\n");
		prompt.append("   - Always use stop-losses (NEVER hope and hold losing positions)\n");
		prompt.append("   - Set stop-loss at volatility-adjusted levels, not arbitrary percentages\n");
		prompt.append("   - KELLY CRITERION: Optimal position sizing formula\n");
		prompt.append("     * Formula: f = (p × r - q) / r\n");
		prompt.append("     * Where: p = win rate, r = reward:risk ratio, q = (1 - p)\n");
		prompt.append("     * Example: 60% win rate, 2:1 reward:risk → f = (0.6 × 2 - 0.4) / 2 = 40%\n");
		prompt.append("     * Use FRACTIONAL Kelly (25-50% of full Kelly) to reduce volatility\n");
		prompt.append("     * If Kelly suggests >10%, cap it at 10% (full Kelly can be too aggressive)\n\n");
		prompt.append("2. AVOID COMMON PITFALLS:\n");
		prompt.append("   - DON'T be too conservative - aim for 10-100+ trades over 3 years to beat buy-and-hold\n");
		prompt.append("   - DON'T curve-fit to historical data (avoid overfitting)\n");
		prompt.append("   - DON'T ignore transaction costs and slippage\n");
		prompt.append("   - DON'T use too many indicators (creates conflicting signals)\n");
		prompt.append("   - CRITICAL: A strategy with only 2-5 trades is NOT a real strategy - increase sensitivity!\n\n");
		prompt.append("3. ENTRY/EXIT TIMING:\n");
		prompt.append("   - Wait for confirmation before entry (don't chase)\n");
		prompt.append("   - Use proper take-profit targets (risk:reward ratio minimum 2:1)\n");
		prompt.append("   - Consider market regime (trending vs ranging)\n");
		prompt.append("   - Exit losing trades quickly, let winners run\n\n");
		prompt.append("4. STRATEGY ROBUSTNESS:\n");
		prompt.append("   - Keep it SIMPLE (complex != better)\n");
		prompt.append("   - Use 2-3 complementary indicators max\n");
		prompt.append("   - Strategy should work across multiple market conditions\n");
		prompt.append("   - Avoid strategies that only worked during bull/bear markets\n\n");

		prompt.append("5. ADVANCED PROFITABILITY TECHNIQUES (CRITICAL):\n");
		prompt.append("   a) VOLUME CONFIRMATION:\n");
		prompt.append("      - NEVER enter breakouts without volume confirmation\n");
		prompt.append("      - Require volume > 1.5x recent average for breakout entries\n");
		prompt.append("      - Ignore signals during very low volume periods (< 50% avg)\n");
		prompt.append("      - Volume = validation that institutions are participating\n\n");

		prompt.append("   b) DRAWDOWN MANAGEMENT (Optional - use sparingly):\n");
		prompt.append("      - SKIP drawdown management if it would reduce trade frequency below 10 trades/3 years\n");
		prompt.append("      - Simple stop-loss per trade is usually sufficient\n");
		prompt.append("      - Focus on GENERATING ENOUGH TRADES to beat buy-and-hold, not avoiding losses\n");
		prompt.append("      - A strategy that trades rarely cannot beat buy-and-hold in trending markets\n\n");

		prompt.append("   c) TRAILING STOPS (Lock in Profits):\n");
		prompt.append("      - Once profit reaches 1.5R: move stop to breakeven immediately\n");
		prompt.append("      - Once profit reaches 3R: trail stop at 50% of peak profit\n");
		prompt.append("      - Example: Up $300 → Stop at $150 profit (not breakeven)\n");
		prompt.append("      - Never let a 3R winner turn into a loss\n\n");

		prompt.append("   d) TRANSACTION COSTS (Reality Check):\n");
		prompt.append("      - ALWAYS factor in: 0.1% commission + 0.05% slippage per trade\n");
		prompt.append("      - For day trading strategies: use 0.2% commission + 0.1% slippage\n");
		prompt.append("      - Strategy must be profitable AFTER costs (many aren't!)\n");
		prompt.append("      - Strategies with <50 trades/year: costs less critical\n");
		prompt.append("      - High-frequency (>200 trades/year): costs are EVERYTHING\n\n");

		prompt.append("   e) FALSE BREAKOUT PREVENTION:\n");
		prompt.append("      - Don't enter on first touch of resistance/support\n");
		prompt.append("      - Require 2 consecutive closes ABOVE resistance (not wicks)\n");
		prompt.append("      - For breakouts: wait for pullback to retest before entry\n");
		prompt.append("      - 70% of breakouts fail - confirmation filters out most\n\n");

		// Add CRITICAL section for trending markets
		prompt.append("6. CRITICAL: HOW TO BEAT BUY-AND-HOLD IN TRENDING MARKETS:\n");
		prompt.append("   ⚠️ THE #1 MISTAKE: Sitting in cash waiting for 'perfect' entries in a bull market!\n\n");
		prompt.append("   If Market Type above is 'Trending' or Trend Direction is 'BULLISH':\n");
		prompt.append("   a) DO NOT use mean-reversion strategies (RSI < 30 rarely triggers in bull markets)\n");
		prompt.append("   b) DO NOT use fixed take-profit caps (8% TP in a 100%+ bull run = disaster)\n");
		prompt.append("   c) DO NOT wait for extreme oversold conditions\n\n");
		prompt.append("   INSTEAD, for TRENDING/BULLISH markets:\n");
		prompt.append("   a) USE TREND-FOLLOWING: Buy pullbacks to moving averages (not oversold extremes)\n");
		prompt.append("   b) USE TRAILING STOPS instead of fixed take-profit:\n");
		prompt.append("      * Remove TAKE_PROFIT constant entirely OR set it very high (50%+)\n");
		prompt.append("      * Implement trailing stop in strategy() function:\n");
		prompt.append("        - Track highest price since entry\n");
		prompt.append("        - Exit when price drops X% from peak (e.g., 10% trailing)\n");
		prompt.append("      * This lets winners run in trending markets\n");
		prompt.append("   c) STAY INVESTED: Better to be in market with stop-loss than sitting in cash\n");
		prompt.append("   d) BUY DIPS: Entry on pullbacks to 20-day SMA, not RSI < 30\n");
		prompt.append("   e) PYRAMIDING: Add to winning positions (not losing ones)\n\n");
		prompt.append("   EXAMPLE TREND-FOLLOWING STRATEGY:\n");
		prompt.append("   ```python\n");
		prompt.append("   # Entry: Price pulls back to 20-day SMA in uptrend\n");
		prompt.append("   sma_20 = data['close'].rolling(20).mean()\n");
		prompt.append("   sma_50 = data['close'].rolling(50).mean()\n");
		prompt.append("   uptrend = sma_20.iloc[-1] > sma_50.iloc[-1]  # SMA20 above SMA50\n");
		prompt.append("   pullback = data['close'].iloc[-1] <= sma_20.iloc[-1] * 1.02  # Within 2% of SMA20\n");
		prompt.append("   if uptrend and pullback:\n");
		prompt.append("       return 'BUY'\n");
		prompt.append("   # Exit: Only on trend reversal (SMA cross) or stop-loss hit\n");
		prompt.append("   if sma_20.iloc[-1] < sma_50.iloc[-1]:\n");
		prompt.append("       return 'SELL'\n");
		prompt.append("   ```\n\n");
		prompt.append("   WHY THIS BEATS BUY-AND-HOLD:\n");
		prompt.append("   - Stays invested during uptrends (captures most of the move)\n");
		prompt.append("   - Exits on trend reversal (avoids major drawdowns)\n");
		prompt.append("   - Buys dips (better entry prices than buy-and-hold)\n");
		prompt.append("   - No fixed TP cap (lets winners run to full potential)\n\n");

		// Provide appropriate example based on market type
		if (!insights.isMeanReverting()) {
			prompt.append("Example summaryCard for TRENDING market (Feeling Lucky mode):\n");
			prompt.append(String.format("\"This trend-following strategy beats buy-and-hold for %s by staying invested during uptrends ",
					insights.getSymbol()));
			prompt.append("and exiting on trend reversals. Uses SMA crossover to identify trend direction, ");
			prompt.append(String.format("buys pullbacks to the 20-day SMA with %.1f%% trailing stops. ", recommendedStopLoss * 2));
			prompt.append("No fixed take-profit cap - lets winners run to maximize gains in trending markets.\"\n");
		} else {
			prompt.append("Example summaryCard for MEAN-REVERTING market (Feeling Lucky mode):\n");
			prompt.append(String.format("\"This mean-reversion strategy beats buy-and-hold for %s by buying oversold conditions ",
					insights.getSymbol()));
			prompt.append("and selling at fair value. Uses RSI with optimized 28/72 thresholds from 7 years of data. ");
			prompt.append(String.format("%.1f%% stop-loss and %.1f%% take-profit based on historical volatility.\"\n",
					recommendedStopLoss, recommendedTakeProfit));
		}

		return prompt.toString();
	}

	private static double computeRecommendedStopLoss(SymbolInsights insights) {
		// Base stop-loss on average daily range
		double baseSL = insights.getAvgDailyRange() * 1.5;

		// Adjust for volatility regime
		return switch (insights.getVolatilityRegime()) {
			case "LOW" -> Math.max(baseSL, 1.5);
			case "MEDIUM" -> Math.max(baseSL, 3.0);
			case "HIGH" -> Math.max(baseSL, 5.0);
			case "EXTREME" -> Math.max(baseSL, 8.0);
			default -> 3.0;
		};
	}

	private static double computeRecommendedTakeProfit(SymbolInsights insights) {
		double stopLoss = computeRecommendedStopLoss(insights);

		// For trending markets, use much higher take-profit (or trailing stop)
		// This prevents capping gains in bull/bear markets
		if (!insights.isMeanReverting()) {
			// Trending market: 10:1 ratio or higher (let winners run)
			// The prompt will suggest using trailing stops instead
			return Math.max(stopLoss * 10.0, 30.0);
		}

		// Mean-reverting market: Use standard 3:1 ratio
		return stopLoss * 3.0;
	}

	private static String formatOptimalSettings(Map<String, Object> settings) {
		if (settings == null || settings.isEmpty()) {
			return "Default";
		}

		StringBuilder sb = new StringBuilder();
		settings.forEach((key, value) -> {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(key).append("=").append(value);
		});

		return sb.toString();
	}

	// ========================================
	// TECHNICAL ANALYSIS KNOWLEDGE BASE
	// ========================================

	/**
	 * Comprehensive technical analysis knowledge including all chart patterns,
	 * candlestick patterns, and technical indicators from the Strategiz Learning Center.
	 * This knowledge base teaches the AI to recognize and use proven patterns.
	 */
	// Technical Analysis Knowledge Base split into parts due to Java's 65535 byte string literal limit
	public static final String TECHNICAL_ANALYSIS_KNOWLEDGE_PART1 = """

			===========================================
			TECHNICAL ANALYSIS PATTERN RECOGNITION GUIDE
			===========================================

			Use this knowledge to identify high-probability setups and generate profitable strategies.

			-------------------------------------------
			BULLISH CANDLESTICK PATTERNS (Buy Signals)
			-------------------------------------------

			1. HAMMER (Reliability: HIGH)
			   - Small body at top, long lower shadow (2x body size minimum)
			   - Appears after downtrend at support levels
			   - Entry: Buy on confirmation candle close above hammer high
			   - Best with: RSI oversold (<30), support levels, volume confirmation
			   Python detection:
			   ```
			   lower_shadow = min(open, close) - low
			   body = abs(close - open)
			   is_hammer = lower_shadow > 2 * body and upper_shadow < body * 0.3
			   ```

			2. THREE WHITE SOLDIERS (Reliability: VERY HIGH)
			   - Three consecutive bullish candles, each closing higher
			   - Each opens within previous body, minimal upper shadows
			   - Strong upward momentum signal
			   - Entry: Buy on third candle close or pullback
			   - Best with: Volume confirmation, breakout levels

			3. BULLISH ENGULFING (Reliability: HIGH)
			   - Large green candle completely engulfs previous red candle
			   - Appears after downtrend
			   - Entry: Buy on engulfing candle close
			   - Best with: Support levels, RSI divergence, high volume
			   Python detection:
			   ```
			   bullish_engulfing = (prev_close < prev_open) and (close > open) and
			                       (close > prev_open) and (open < prev_close)
			   ```

			4. MORNING STAR (Reliability: HIGH)
			   - Three-candle pattern: bearish, small body/doji, bullish
			   - Star gaps down, third candle closes into first body
			   - Entry: Buy on third candle close
			   - Best with: Support levels, RSI oversold

			5. BULLISH HARAMI (Reliability: MEDIUM)
			   - Small bullish candle inside large bearish candle
			   - Shows indecision, potential reversal
			   - Entry: Buy on confirmation above harami high
			   - Best with: Volume decrease, RSI divergence

			-------------------------------------------
			BEARISH CANDLESTICK PATTERNS (Sell Signals)
			-------------------------------------------

			1. SHOOTING STAR (Reliability: HIGH)
			   - Small body at bottom, long upper shadow
			   - Appears after uptrend at resistance
			   - Entry: Sell on confirmation candle close below shooting star low
			   - Best with: RSI overbought (>70), resistance levels
			   Python detection:
			   ```
			   upper_shadow = high - max(open, close)
			   body = abs(close - open)
			   is_shooting_star = upper_shadow > 2 * body and lower_shadow < body * 0.3
			   ```

			2. THREE BLACK CROWS (Reliability: VERY HIGH)
			   - Three consecutive bearish candles, each closing lower
			   - Each opens within previous body, minimal lower shadows
			   - Strong downward momentum signal
			   - Entry: Sell/short on third candle close
			   - Best with: Volume confirmation, breakdown levels

			3. BEARISH ENGULFING (Reliability: HIGH)
			   - Large red candle completely engulfs previous green candle
			   - Appears after uptrend
			   - Entry: Sell on engulfing candle close
			   - Best with: Resistance levels, RSI divergence

			4. EVENING STAR (Reliability: HIGH)
			   - Three-candle pattern: bullish, small body/doji, bearish
			   - Star gaps up, third candle closes into first body
			   - Entry: Sell on third candle close
			   - Best with: Resistance levels, RSI overbought

			5. HANGING MAN (Reliability: HIGH)
			   - Same shape as hammer but appears after uptrend
			   - Long lower shadow shows selling pressure
			   - Entry: Sell on confirmation below hanging man low
			   - Best with: RSI overbought, resistance levels

			-------------------------------------------
			CHART PATTERNS (Higher Timeframe Setups)
			-------------------------------------------

			=== BULLISH CONTINUATION/REVERSAL PATTERNS ===

			1. ASCENDING TRIANGLE (Reliability: HIGH, Timeframe: Medium-Long)
			   - Horizontal resistance, rising support (higher lows)
			   - Indicates accumulation, typically breaks UP
			   - Entry: Buy on breakout above resistance with volume
			   - Target: Height of triangle added to breakout point
			   - Stop: Below most recent higher low
			   Python detection:
			   ```
			   resistance_level = data['high'].rolling(20).max().iloc[-1]
			   higher_lows = data['low'].iloc[-10:].is_monotonic_increasing
			   near_resistance = data['close'].iloc[-1] > resistance_level * 0.98
			   ascending_triangle = higher_lows and near_resistance
			   ```

			2. DOUBLE BOTTOM (Reliability: HIGH, Timeframe: Medium-Long)
			   - Two distinct lows at same level, separated by peak
			   - "W" shape formation
			   - Entry: Buy on breakout above neckline (peak)
			   - Target: Neckline + distance to lows
			   - Stop: Below the double bottom lows

			3. TRIPLE BOTTOM (Reliability: VERY HIGH, Timeframe: Long)
			   - Three lows at same level - very strong support
			   - Entry: Buy on breakout above resistance
			   - Highest reliability reversal pattern

			4. INVERTED HEAD AND SHOULDERS (Reliability: VERY HIGH, Timeframe: Medium-Long)
			   - Three troughs: left shoulder, head (lowest), right shoulder
			   - Most reliable bullish reversal pattern
			   - Entry: Buy on breakout above neckline
			   - Target: Neckline + distance from head to neckline
			   Python detection:
			   ```
			   # Simplified detection
			   head = data['low'].iloc[-20:-10].min()
			   left_shoulder = data['low'].iloc[-30:-20].min()
			   right_shoulder = data['low'].iloc[-10:].min()
			   inv_hs = head < left_shoulder and head < right_shoulder and
			            abs(left_shoulder - right_shoulder) / left_shoulder < 0.05
			   ```

			5. BULL FLAG (Reliability: HIGH, Timeframe: Short-Medium)
			   - Strong upward move (flagpole) + brief consolidation (flag)
			   - Flag slopes slightly downward or sideways
			   - Entry: Buy on breakout above flag with volume
			   - Target: Flagpole height added to breakout
			   - Best continuation pattern in trending markets

			6. FALLING WEDGE (Reliability: MEDIUM, Timeframe: Medium)
			   - Both trendlines slope downward and converge
			   - Bullish despite downward slope
			   - Entry: Buy on breakout above upper trendline
			   - Shows weakening selling pressure

			7. CUP AND HANDLE (Reliability: VERY HIGH, Timeframe: Medium-Long)
			   - U-shaped recovery (cup) + small pullback (handle)
			   - Handle should be in upper half of cup
			   - Entry: Buy on breakout above handle resistance
			   - One of the most reliable continuation patterns

			=== BEARISH CONTINUATION/REVERSAL PATTERNS ===

			1. DESCENDING TRIANGLE (Reliability: HIGH, Timeframe: Medium-Long)
			   - Horizontal support, falling resistance (lower highs)
			   - Indicates distribution, typically breaks DOWN
			   - Entry: Sell on breakdown below support with volume
			   - Target: Height of triangle subtracted from breakdown
			   - Stop: Above most recent lower high

			2. DOUBLE TOP (Reliability: HIGH, Timeframe: Medium-Long)
			   - Two distinct highs at same level, separated by trough
			   - "M" shape formation
			   - Entry: Sell on breakdown below neckline
			   - Target: Neckline - distance to highs

			3. TRIPLE TOP (Reliability: VERY HIGH, Timeframe: Long)
			   - Three highs at same level - very strong resistance
			   - Entry: Sell on breakdown below support
			   - Highest reliability bearish reversal

			4. HEAD AND SHOULDERS (Reliability: VERY HIGH, Timeframe: Medium-Long)
			   - Three peaks: left shoulder, head (highest), right shoulder
			   - Most reliable bearish reversal pattern
			   - Entry: Sell on breakdown below neckline
			   - Target: Neckline - distance from head to neckline
			   Python detection:
			   ```
			   head = data['high'].iloc[-20:-10].max()
			   left_shoulder = data['high'].iloc[-30:-20].max()
			   right_shoulder = data['high'].iloc[-10:].max()
			   hs = head > left_shoulder and head > right_shoulder and
			        abs(left_shoulder - right_shoulder) / left_shoulder < 0.05
			   ```

			5. BEAR FLAG (Reliability: HIGH, Timeframe: Short-Medium)
			   - Strong downward move (flagpole) + brief consolidation (flag)
			   - Flag slopes slightly upward or sideways
			   - Entry: Sell on breakdown below flag with volume
			   - Best continuation pattern in downtrends

			6. RISING WEDGE (Reliability: MEDIUM, Timeframe: Medium)
			   - Both trendlines slope upward and converge
			   - Bearish despite upward slope
			   - Entry: Sell on breakdown below lower trendline
			   - Shows weakening buying pressure

			-------------------------------------------
			KEY MOVING AVERAGE SIGNALS
			-------------------------------------------

			1. GOLDEN CROSS (Reliability: HIGH, Timeframe: Long-term)
			   - 50-day MA crosses ABOVE 200-day MA
			   - Major bullish signal - start of potential long-term uptrend
			   - Entry: Buy on cross or pullback to 50-day MA
			   Python:
			   ```
			   sma_50 = data['close'].rolling(50).mean()
			   sma_200 = data['close'].rolling(200).mean()
			   golden_cross = (sma_50.iloc[-1] > sma_200.iloc[-1]) and (sma_50.iloc[-2] <= sma_200.iloc[-2])
			   ```

			2. DEATH CROSS (Reliability: HIGH, Timeframe: Long-term)
			   - 50-day MA crosses BELOW 200-day MA
			   - Major bearish signal - start of potential long-term downtrend
			   - Entry: Sell on cross or rally to 50-day MA
			   Python:
			   ```
			   death_cross = (sma_50.iloc[-1] < sma_200.iloc[-1]) and (sma_50.iloc[-2] >= sma_200.iloc[-2])
			   ```

			3. PRICE ABOVE ALL MAJOR MAs (BULLISH ALIGNMENT)
			   - Price > 20 MA > 50 MA > 200 MA
			   - Strongest bullish configuration
			   - Buy dips to 20-day or 50-day MA

			4. PRICE BELOW ALL MAJOR MAs (BEARISH ALIGNMENT)
			   - Price < 20 MA < 50 MA < 200 MA
			   - Strongest bearish configuration
			   - Sell rallies to 20-day or 50-day MA

			-------------------------------------------
			MOMENTUM INDICATOR SIGNALS
			-------------------------------------------

			1. RSI (Relative Strength Index)
			   - Oversold: RSI < 30 → potential buy signal
			   - Overbought: RSI > 70 → potential sell signal
			   - Bullish divergence: Price makes lower low, RSI makes higher low
			   - Bearish divergence: Price makes higher high, RSI makes lower high

			2. MACD
			   - Bullish: MACD line crosses above signal line
			   - Bearish: MACD line crosses below signal line
			   - Stronger signal when crossover happens below/above zero line

			3. STOCHASTIC
			   - Oversold: Below 20 → potential buy
			   - Overbought: Above 80 → potential sell
			   - %K crossing %D for entry signals

			-------------------------------------------
			STANDARD DEVIATION & VOLATILITY BANDS
			-------------------------------------------

			UNDERSTANDING STANDARD DEVIATION (σ):
			- Measures how spread out prices are from the mean (average)
			- 1 Standard Deviation: ~68% of prices fall within this range
			- 2 Standard Deviations: ~95% of prices fall within this range
			- 3 Standard Deviations: ~99.7% of prices fall within this range
			- Higher σ = More volatile, Lower σ = Less volatile

			Python calculation:
			```
			# Standard deviation of closing prices over 20 periods
			std_dev = data['close'].rolling(20).std()
			mean_price = data['close'].rolling(20).mean()

			# Price is X standard deviations from mean
			z_score = (data['close'] - mean_price) / std_dev
			```

			=== BOLLINGER BANDS (Most Popular Volatility Indicator) ===

			COMPONENTS:
			- Middle Band: 20-period Simple Moving Average (SMA)
			- Upper Band: Middle Band + (2 × Standard Deviation)
			- Lower Band: Middle Band - (2 × Standard Deviation)
			- Band Width: (Upper - Lower) / Middle × 100

			Python calculation:
			```
			period = 20
			num_std = 2

			sma = data['close'].rolling(period).mean()
			std = data['close'].rolling(period).std()

			bb_upper = sma + (std * num_std)
			bb_middle = sma
			bb_lower = sma - (std * num_std)
			bb_width = (bb_upper - bb_lower) / bb_middle * 100
			```

			BOLLINGER BAND STRATEGIES:

			1. MEAN REVERSION (Range-Bound Markets)
			   - Buy when price touches/crosses BELOW lower band
			   - Sell when price touches/crosses ABOVE upper band
			   - Best in: Sideways, non-trending markets
			   - Confirmation: RSI oversold/overbought
			   Python:
			   ```
			   # Buy signal: Price at or below lower band
			   buy_signal = data['close'].iloc[-1] <= bb_lower.iloc[-1]

			   # Sell signal: Price at or above upper band
			   sell_signal = data['close'].iloc[-1] >= bb_upper.iloc[-1]
			   ```

			2. BOLLINGER BAND SQUEEZE (Volatility Breakout)
			   - Squeeze: Bands contract to narrowest point (low volatility)
			   - Breakout: Price explodes out of squeeze with volume
			   - Direction: Follow the breakout direction
			   - Best predictor of big moves!
			   Python:
			   ```
			   # Detect squeeze: Band width at 6-month low
			   current_width = bb_width.iloc[-1]
			   min_width_6m = bb_width.rolling(126).min().iloc[-1]
			   is_squeeze = current_width <= min_width_6m * 1.1

			   # Breakout from squeeze
			   breakout_up = is_squeeze and data['close'].iloc[-1] > bb_upper.iloc[-1]
			   breakout_down = is_squeeze and data['close'].iloc[-1] < bb_lower.iloc[-1]
			   ```

			3. BOLLINGER BAND WALK (Trending Markets)
			   - Strong uptrend: Price "walks" along upper band
			   - Strong downtrend: Price "walks" along lower band
			   - DO NOT fade the trend! (Don't sell just because at upper band)
			   - Exit only when price crosses back to middle band
			   Python:
			   ```
			   # Bullish band walk: Price consistently above middle band
			   bullish_walk = (data['close'].iloc[-5:] > bb_middle.iloc[-5:]).all()

			   # Bearish band walk: Price consistently below middle band
			   bearish_walk = (data['close'].iloc[-5:] < bb_middle.iloc[-5:]).all()
			   ```

			4. DOUBLE BOTTOM AT LOWER BAND (High Reliability)
			   - First touch of lower band → bounce
			   - Second touch of lower band → forms double bottom
			   - Entry: Buy on second bounce with RSI divergence
			   - Very high probability reversal setup

			5. %B INDICATOR (Position within Bands)
			   - %B = (Price - Lower Band) / (Upper Band - Lower Band)
			   - %B > 1.0: Price above upper band (overbought)
			   - %B < 0.0: Price below lower band (oversold)
			   - %B = 0.5: Price at middle band
			   Python:
			   ```
			   percent_b = (data['close'] - bb_lower) / (bb_upper - bb_lower)

			   # Trading signals
			   oversold = percent_b.iloc[-1] < 0
			   overbought = percent_b.iloc[-1] > 1
			   ```

			=== KELTNER CHANNELS (ATR-Based Bands) ===

			COMPONENTS:
			- Middle Line: 20-period EMA
			- Upper Channel: EMA + (2 × ATR)
			- Lower Channel: EMA - (2 × ATR)

			DIFFERENCE FROM BOLLINGER:
			- Bollinger uses Standard Deviation (more reactive)
			- Keltner uses ATR (smoother, less whipsaw)
			- Keltner better for trend-following
			- Bollinger better for mean-reversion

			Python calculation:
			```
			period = 20
			atr_mult = 2

			ema = data['close'].ewm(span=period).mean()

			# ATR calculation
			high_low = data['high'] - data['low']
			high_close = abs(data['high'] - data['close'].shift(1))
			low_close = abs(data['low'] - data['close'].shift(1))
			tr = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
			atr = tr.rolling(period).mean()

			kc_upper = ema + (atr * atr_mult)
			kc_middle = ema
			kc_lower = ema - (atr * atr_mult)
			```

			KELTNER CHANNEL STRATEGIES:

			1. BREAKOUT STRATEGY
			   - Buy: Price closes above upper channel
			   - Sell: Price closes below lower channel
			   - Best with volume confirmation

			2. PULLBACK IN TREND
			   - Uptrend: Buy when price pulls back to middle line
			   - Downtrend: Sell when price rallies to middle line

			=== BOLLINGER + KELTNER SQUEEZE (TTM Squeeze) ===

			POWERFUL COMBINATION:
			- Squeeze ON: Bollinger Bands INSIDE Keltner Channels
			- Squeeze OFF: Bollinger Bands OUTSIDE Keltner Channels
			- Momentum: Use histogram to determine direction

			Python:
			```
			# Squeeze detection
			squeeze_on = (bb_lower.iloc[-1] > kc_lower.iloc[-1]) and (bb_upper.iloc[-1] < kc_upper.iloc[-1])
			squeeze_off = not squeeze_on

			# Momentum (using linear regression or MACD histogram)
			momentum = data['close'].iloc[-1] - sma.iloc[-1]
			momentum_rising = momentum > 0 and momentum > data['close'].iloc[-2] - sma.iloc[-2]

			# Trade signal
			if squeeze_off and momentum_rising:
			    signal = 'BUY'  # Breakout with bullish momentum
			```

			=== ATR BANDS (Dynamic Stop-Loss Bands) ===

			USE CASE: Setting dynamic stop-losses based on volatility

			Python:
			```
			atr_multiplier = 2.0  # Adjust based on risk tolerance

			# Chandelier Exit (trailing stop)
			highest_high = data['high'].rolling(22).max()
			chandelier_exit_long = highest_high - (atr * atr_multiplier)

			lowest_low = data['low'].rolling(22).min()
			chandelier_exit_short = lowest_low + (atr * atr_multiplier)

			# Exit long if price drops below chandelier exit
			exit_long = data['close'].iloc[-1] < chandelier_exit_long.iloc[-1]
			```

			=== VOLATILITY BAND BEST PRACTICES ===

			1. IDENTIFY MARKET REGIME FIRST:
			   - Trending: Use band walks, don't fade the trend
			   - Ranging: Use mean reversion, buy low band, sell high band
			   - Squeezing: Prepare for breakout, trade the direction

			2. COMBINE WITH OTHER INDICATORS:
			   - RSI for overbought/oversold confirmation
			   - Volume for breakout validation
			   - MACD for momentum direction

			3. ADJUST PARAMETERS FOR TIMEFRAME:
			   - Intraday (1m-1h): Use 10-period, 1.5 std dev
			   - Swing (4h-1D): Use 20-period, 2.0 std dev (default)
			   - Position (1W+): Use 50-period, 2.5 std dev

			4. AVOID COMMON MISTAKES:
			   - DON'T sell just because price hits upper band in uptrend
			   - DON'T buy just because price hits lower band in downtrend
			   - DO use band walks to ride trends
			   - DO wait for squeeze breakouts with volume

			-------------------------------------------
			FIBONACCI RETRACEMENT & EXTENSIONS
			-------------------------------------------

			THE FIBONACCI SEQUENCE IN TRADING:
			Based on the golden ratio (1.618), Fibonacci levels identify potential
			support/resistance zones where price may reverse or consolidate.

			KEY FIBONACCI RETRACEMENT LEVELS:
			- 23.6% - Shallow retracement (strong trend)
			- 38.2% - Common retracement level
			- 50.0% - Psychological halfway point (not true Fib but widely used)
			- 61.8% - "Golden ratio" - MOST IMPORTANT level
			- 78.6% - Deep retracement (trend may be weakening)

			HOW TO DRAW FIBONACCI RETRACEMENTS:
			- UPTREND: Draw from swing LOW to swing HIGH
			- DOWNTREND: Draw from swing HIGH to swing LOW
			- Retracement levels show where pullbacks may find support/resistance

			Python calculation:
			```
			def calculate_fib_levels(high, low, is_uptrend=True):
			    diff = high - low

			    if is_uptrend:
			        # In uptrend, measure retracements DOWN from high
			        levels = {
			            '0.0%': high,           # Swing high
			            '23.6%': high - (diff * 0.236),
			            '38.2%': high - (diff * 0.382),
			            '50.0%': high - (diff * 0.500),
			            '61.8%': high - (diff * 0.618),
			            '78.6%': high - (diff * 0.786),
			            '100%': low             # Swing low
			        }
			    else:
			        # In downtrend, measure retracements UP from low
			        levels = {
			            '0.0%': low,            # Swing low
			            '23.6%': low + (diff * 0.236),
			            '38.2%': low + (diff * 0.382),
			            '50.0%': low + (diff * 0.500),
			            '61.8%': low + (diff * 0.618),
			            '78.6%': low + (diff * 0.786),
			            '100%': high            # Swing high
			        }
			    return levels

			# Find swing high/low over lookback period
			lookback = 50
			swing_high = data['high'].rolling(lookback).max().iloc[-1]
			swing_low = data['low'].rolling(lookback).min().iloc[-1]
			fib_levels = calculate_fib_levels(swing_high, swing_low, is_uptrend=True)
			```

			FIBONACCI TRADING STRATEGIES:

			1. BUY THE DIP AT FIB SUPPORT (Uptrend)
			   - Wait for pullback to 38.2%, 50%, or 61.8% level
			   - Look for bullish candle pattern at level (hammer, engulfing)
			   - Enter with stop below next Fib level
			   - Target: Previous high or Fib extension
			   Python:
			   ```
			   current_price = data['close'].iloc[-1]
			   fib_38 = fib_levels['38.2%']
			   fib_50 = fib_levels['50.0%']
			   fib_61 = fib_levels['61.8%']

			   # Price at Fibonacci support zone
			   at_fib_support = (current_price >= fib_61 * 0.99) and (current_price <= fib_38 * 1.01)
			   ```

			2. SELL THE RALLY AT FIB RESISTANCE (Downtrend)
			   - Wait for bounce to 38.2%, 50%, or 61.8% level
			   - Look for bearish candle pattern (shooting star, engulfing)
			   - Enter short with stop above next Fib level
			   - Target: Previous low or Fib extension

			3. FIBONACCI CONFLUENCE (High Probability)
			   - When multiple Fib levels align from different swings
			   - Or when Fib aligns with moving average, trendline, or S/R
			   - These "confluence zones" have highest probability

			FIBONACCI EXTENSIONS (Profit Targets):
			Used to project where price might go AFTER breaking previous high/low

			KEY EXTENSION LEVELS:
			- 100% - Equal move (measured move)
			- 127.2% - Common first target
			- 161.8% - Golden extension - MOST IMPORTANT
			- 200% - Double the original move
			- 261.8% - Extended target

			Python:
			```
			def calculate_fib_extensions(high, low, is_uptrend=True):
			    diff = high - low

			    if is_uptrend:
			        # Project above the high
			        extensions = {
			            '100%': high,
			            '127.2%': high + (diff * 0.272),
			            '161.8%': high + (diff * 0.618),
			            '200%': high + diff,
			            '261.8%': high + (diff * 1.618)
			        }
			    else:
			        # Project below the low
			        extensions = {
			            '100%': low,
			            '127.2%': low - (diff * 0.272),
			            '161.8%': low - (diff * 0.618),
			            '200%': low - diff,
			            '261.8%': low - (diff * 1.618)
			        }
			    return extensions
			```

			FIBONACCI BEST PRACTICES:
			1. Use on higher timeframes (4h, Daily, Weekly) for reliability
			2. Always wait for price action confirmation at levels
			3. Combine with other indicators (RSI, volume) for confluence
			4. 61.8% is the "make or break" level - if broken, trend may reverse
			5. Don't force Fibonacci - use clear, obvious swings

			-------------------------------------------
			SUPPORT & RESISTANCE LEVELS
			-------------------------------------------

			WHAT IS SUPPORT?
			- Price level where buying pressure exceeds selling pressure
			- Price tends to "bounce" off support
			- Previous lows often become support
			- When broken, support becomes resistance ("polarity flip")

			WHAT IS RESISTANCE?
			- Price level where selling pressure exceeds buying pressure
			- Price tends to "reject" at resistance
			- Previous highs often become resistance
			- When broken, resistance becomes support ("polarity flip")

			TYPES OF SUPPORT/RESISTANCE:

			1. HORIZONTAL S/R (Most Common)
			   - Based on previous price levels (highs/lows)
			   - The more times tested, the stronger the level
			   Python detection:
			   ```
			   def find_horizontal_sr(data, lookback=100, tolerance=0.02):
			       highs = data['high'].iloc[-lookback:]
			       lows = data['low'].iloc[-lookback:]

			       # Find price levels that were touched multiple times
			       all_levels = pd.concat([highs, lows])

			       # Cluster nearby prices
			       levels = []
			       for price in all_levels:
			           # Check if this price is near an existing level
			           found = False
			           for level in levels:
			               if abs(price - level['price']) / level['price'] < tolerance:
			                   level['touches'] += 1
			                   found = True
			                   break
			           if not found:
			               levels.append({'price': price, 'touches': 1})

			       # Return levels with 3+ touches (significant S/R)
			       significant = [l for l in levels if l['touches'] >= 3]
			       return sorted(significant, key=lambda x: x['touches'], reverse=True)
			   ```

			2. DYNAMIC S/R (Moving Averages)
			   - 20 EMA: Short-term dynamic support/resistance
			   - 50 SMA: Medium-term dynamic support/resistance
			   - 200 SMA: Long-term dynamic support/resistance (most important)
			   Python:
			   ```
			   ema_20 = data['close'].ewm(span=20).mean()
			   sma_50 = data['close'].rolling(50).mean()
			   sma_200 = data['close'].rolling(200).mean()

			   # Price at dynamic support
			   at_20ema = abs(data['close'].iloc[-1] - ema_20.iloc[-1]) / ema_20.iloc[-1] < 0.01
			   at_50sma = abs(data['close'].iloc[-1] - sma_50.iloc[-1]) / sma_50.iloc[-1] < 0.01
			   at_200sma = abs(data['close'].iloc[-1] - sma_200.iloc[-1]) / sma_200.iloc[-1] < 0.01
			   ```

			3. TRENDLINE S/R
			   - Connect swing lows for uptrend support
			   - Connect swing highs for downtrend resistance
			   - The more touches, the more valid the trendline

			4. PSYCHOLOGICAL LEVELS (Round Numbers)
			   - Major round numbers act as S/R: $100, $50, $10
			   - Also works with indices: 4000, 5000, 20000
			   Python:
			   ```
			   def nearest_round_number(price):
			       if price > 1000:
			           return round(price / 100) * 100
			       elif price > 100:
			           return round(price / 10) * 10
			       elif price > 10:
			           return round(price)
			       else:
			           return round(price, 1)

			   psychological_level = nearest_round_number(data['close'].iloc[-1])
			   near_psych_level = abs(data['close'].iloc[-1] - psychological_level) / psychological_level < 0.01
			   ```

			5. PIVOT POINTS (Intraday S/R)
			   - Calculated from previous day's high, low, close
			   - Popular for day trading
			   Python:
			   ```
			   prev_high = data['high'].iloc[-2]
			   prev_low = data['low'].iloc[-2]
			   prev_close = data['close'].iloc[-2]

			   pivot = (prev_high + prev_low + prev_close) / 3
			   r1 = (2 * pivot) - prev_low      # Resistance 1
			   r2 = pivot + (prev_high - prev_low)  # Resistance 2
			   s1 = (2 * pivot) - prev_high     # Support 1
			   s2 = pivot - (prev_high - prev_low)  # Support 2
			   ```

			S/R TRADING STRATEGIES:

			1. BOUNCE STRATEGY (Mean Reversion)
			   - Buy at support with stop below
			   - Sell at resistance with stop above
			   - Best in ranging/sideways markets
			   Python:
			   ```
			   support_level = find_nearest_support(data)
			   resistance_level = find_nearest_resistance(data)

			   # Buy near support
			   near_support = data['close'].iloc[-1] <= support_level * 1.01

			   # Sell near resistance
			   near_resistance = data['close'].iloc[-1] >= resistance_level * 0.99
			   ```

			2. BREAKOUT STRATEGY (Trend Following)
			   - Buy when price breaks ABOVE resistance with volume
			   - Sell when price breaks BELOW support with volume
			   - Wait for candle CLOSE above/below level (avoid fakeouts)
			   Python:
			   ```
			   resistance = data['high'].rolling(20).max().iloc[-2]  # Previous resistance
			   support = data['low'].rolling(20).min().iloc[-2]      # Previous support

			   # Breakout with volume confirmation
			   avg_volume = data['volume'].rolling(20).mean().iloc[-1]
			   high_volume = data['volume'].iloc[-1] > avg_volume * 1.5

			   breakout_up = data['close'].iloc[-1] > resistance and high_volume
			   breakout_down = data['close'].iloc[-1] < support and high_volume
			   ```

			3. RETEST STRATEGY (Safest Entry)
			   - Wait for breakout above resistance
			   - Wait for price to pull back and RETEST the broken level
			   - Old resistance becomes new support
			   - Enter on successful retest (level holds)
			   Python:
			   ```
			   # Previous resistance now acting as support
			   old_resistance = resistance_level

			   # Price broke above, pulled back, and holding
			   broke_above = data['high'].iloc[-5:-1].max() > old_resistance
			   pulled_back = data['low'].iloc[-1] <= old_resistance * 1.02
			   holding = data['close'].iloc[-1] > old_resistance

			   retest_buy = broke_above and pulled_back and holding
			   ```

			4. S/R CONFLUENCE (Highest Probability)
			   - Multiple S/R types at same level
			   - Example: 200 SMA + Fibonacci 61.8% + Previous high
			   - These zones have very high probability of reaction

			S/R STRENGTH ASSESSMENT:
			Stronger levels have:
			- More touches/tests (3+ is significant)
			- Higher timeframe significance (weekly > daily > hourly)
			- Higher volume at the level
			- Confluence with other technical factors
			- Round psychological numbers

			S/R BEST PRACTICES:
			1. Use ZONES not exact prices (±1-2% tolerance)
			2. The more times tested, the weaker it gets (eventually breaks)
			3. Broken S/R often gets retested before continuing
			4. Always use stops beyond the S/R zone
			5. Volume confirms breakouts - low volume = likely fakeout

			-------------------------------------------
			VOLUME CONFIRMATION RULES
			-------------------------------------------

			CRITICAL: Volume confirms or denies price action!

			1. BULLISH VOLUME PATTERNS:
			   - Breakout with 2x+ average volume = strong confirmation
			   - Higher volume on up days than down days = accumulation
			   - Volume surge on bullish candles = institutional buying

			2. BEARISH VOLUME PATTERNS:
			   - Breakdown with 2x+ average volume = strong confirmation
			   - Higher volume on down days than up days = distribution
			   - Volume surge on bearish candles = institutional selling

			3. WARNING SIGNS:
			   - Breakout without volume = likely false breakout
			   - Price rise on declining volume = weakening momentum
			   - Price fall on declining volume = selling exhaustion

			""";

	public static final String TECHNICAL_ANALYSIS_KNOWLEDGE_PART2 = """
			-------------------------------------------
			MARKET REGIME DETECTION
			-------------------------------------------

			CRITICAL: The #1 reason strategies fail is using the WRONG strategy type
			for the current market regime. ALWAYS identify the regime FIRST!

			THREE MARKET REGIMES:
			1. TRENDING (Directional) - Use trend-following strategies
			2. RANGING (Sideways) - Use mean-reversion strategies
			3. VOLATILE (Choppy) - Reduce position size or stay out

			=== ADX (Average Directional Index) - Best Regime Indicator ===

			ADX measures TREND STRENGTH (not direction):
			- ADX < 20: NO TREND (ranging/sideways market)
			- ADX 20-25: Trend EMERGING
			- ADX 25-50: STRONG TREND
			- ADX 50-75: VERY STRONG TREND
			- ADX > 75: EXTREME TREND (may be exhausted)

			+DI and -DI show trend DIRECTION:
			- +DI > -DI: Bullish trend
			- -DI > +DI: Bearish trend

			Python calculation:
			```
			def calculate_adx(data, period=14):
			    high = data['high']
			    low = data['low']
			    close = data['close']

			    # True Range
			    tr1 = high - low
			    tr2 = abs(high - close.shift(1))
			    tr3 = abs(low - close.shift(1))
			    tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
			    atr = tr.rolling(period).mean()

			    # Directional Movement
			    up_move = high - high.shift(1)
			    down_move = low.shift(1) - low

			    plus_dm = np.where((up_move > down_move) & (up_move > 0), up_move, 0)
			    minus_dm = np.where((down_move > up_move) & (down_move > 0), down_move, 0)

			    plus_di = 100 * pd.Series(plus_dm).rolling(period).mean() / atr
			    minus_di = 100 * pd.Series(minus_dm).rolling(period).mean() / atr

			    # ADX
			    dx = 100 * abs(plus_di - minus_di) / (plus_di + minus_di)
			    adx = dx.rolling(period).mean()

			    return adx, plus_di, minus_di

			adx, plus_di, minus_di = calculate_adx(data)

			# Regime detection
			is_trending = adx.iloc[-1] > 25
			is_ranging = adx.iloc[-1] < 20
			trend_direction = 'BULLISH' if plus_di.iloc[-1] > minus_di.iloc[-1] else 'BEARISH'
			```

			=== SLOPE ANALYSIS (Simple Alternative) ===

			Measure the slope of a moving average:
			```
			sma_20 = data['close'].rolling(20).mean()
			slope = (sma_20.iloc[-1] - sma_20.iloc[-20]) / sma_20.iloc[-20] * 100

			# Regime based on slope
			if abs(slope) > 5:  # More than 5% move in 20 periods
			    regime = 'TRENDING'
			    direction = 'BULLISH' if slope > 0 else 'BEARISH'
			else:
			    regime = 'RANGING'
			```

			=== BOLLINGER BAND WIDTH (Volatility Regime) ===

			```
			bb_width = (bb_upper - bb_lower) / bb_middle * 100
			avg_width = bb_width.rolling(100).mean().iloc[-1]
			current_width = bb_width.iloc[-1]

			if current_width < avg_width * 0.5:
			    volatility_regime = 'LOW'  # Squeeze forming
			elif current_width > avg_width * 1.5:
			    volatility_regime = 'HIGH'  # Expanded, may contract
			else:
			    volatility_regime = 'NORMAL'
			```

			REGIME-BASED STRATEGY SELECTION:

			| Regime    | ADX   | Strategy Type      | Examples                    |
			|-----------|-------|--------------------|-----------------------------|
			| Trending  | > 25  | Trend-following    | MA crossover, breakouts     |
			| Ranging   | < 20  | Mean-reversion     | RSI oversold/overbought, BB |
			| Volatile  | Any   | Reduce size/wait   | Smaller positions, wider SL |

			-------------------------------------------
			MULTI-TIMEFRAME ANALYSIS (MTF)
			-------------------------------------------

			THE POWER OF MTF:
			Use HIGHER timeframes for DIRECTION (trend bias)
			Use LOWER timeframes for ENTRY (precise timing)

			COMMON MTF COMBINATIONS:
			| Trading Style | Higher TF (Trend) | Lower TF (Entry) |
			|---------------|-------------------|------------------|
			| Scalping      | 1h                | 1m, 5m           |
			| Day Trading   | 4h, Daily         | 15m, 1h          |
			| Swing Trading | Weekly            | Daily, 4h        |
			| Position      | Monthly           | Weekly, Daily    |

			MTF ALIGNMENT RULES:

			1. TOP-DOWN ANALYSIS:
			   - Start with highest timeframe (Weekly/Daily)
			   - Identify major trend direction
			   - Move to lower timeframe for entry
			   - Only take trades IN DIRECTION of higher TF trend

			2. THREE-TIMEFRAME RULE:
			   - TF1 (Highest): Trend direction
			   - TF2 (Middle): Trade setup
			   - TF3 (Lowest): Entry trigger

			Python implementation:
			```
			def get_mtf_bias(data_daily, data_4h, data_1h):
			    # Daily trend (highest TF)
			    sma_50_daily = data_daily['close'].rolling(50).mean()
			    daily_trend = 'BULLISH' if data_daily['close'].iloc[-1] > sma_50_daily.iloc[-1] else 'BEARISH'

			    # 4H setup (middle TF)
			    rsi_4h = calculate_rsi(data_4h, 14)
			    setup_4h = None
			    if daily_trend == 'BULLISH' and rsi_4h.iloc[-1] < 40:
			        setup_4h = 'BUY_SETUP'  # Pullback in uptrend
			    elif daily_trend == 'BEARISH' and rsi_4h.iloc[-1] > 60:
			        setup_4h = 'SELL_SETUP'  # Rally in downtrend

			    # 1H entry (lowest TF)
			    entry_1h = None
			    if setup_4h == 'BUY_SETUP':
			        # Look for bullish candle pattern on 1H
			        if is_hammer(data_1h) or is_bullish_engulfing(data_1h):
			            entry_1h = 'BUY'
			    elif setup_4h == 'SELL_SETUP':
			        if is_shooting_star(data_1h) or is_bearish_engulfing(data_1h):
			            entry_1h = 'SELL'

			    return {
			        'daily_trend': daily_trend,
			        'setup': setup_4h,
			        'entry': entry_1h
			    }
			```

			MTF CONFLUENCE EXAMPLE:
			```
			# All timeframes aligned = highest probability

			# Weekly: Price above 50 SMA (BULLISH)
			weekly_bullish = weekly_close > weekly_sma_50

			# Daily: RSI pulled back to 40-50 zone
			daily_pullback = daily_rsi > 40 and daily_rsi < 50

			# 4H: Hammer at 20 EMA support
			hourly_entry = is_hammer and price_at_ema_20

			# MTF aligned buy signal
			if weekly_bullish and daily_pullback and hourly_entry:
			    signal = 'STRONG_BUY'
			```

			MTF BEST PRACTICES:
			1. NEVER trade against higher timeframe trend
			2. Use higher TF for stop-loss placement (more room)
			3. Higher TF signals > Lower TF signals in importance
			4. Wait for alignment - no alignment = no trade
			5. The more timeframes aligned, the higher probability

			-------------------------------------------
			DIVERGENCE DETECTION
			-------------------------------------------

			WHAT IS DIVERGENCE?
			When price and an indicator (RSI, MACD) move in OPPOSITE directions.
			Divergence signals potential trend REVERSAL or CONTINUATION.

			TYPES OF DIVERGENCE:

			=== REGULAR DIVERGENCE (Reversal Signal) ===

			1. BULLISH REGULAR DIVERGENCE:
			   - Price makes LOWER LOW
			   - Indicator makes HIGHER LOW
			   - Signal: Selling exhaustion, potential reversal UP
			   - Action: Look for BUY entry

			2. BEARISH REGULAR DIVERGENCE:
			   - Price makes HIGHER HIGH
			   - Indicator makes LOWER HIGH
			   - Signal: Buying exhaustion, potential reversal DOWN
			   - Action: Look for SELL entry

			=== HIDDEN DIVERGENCE (Continuation Signal) ===

			1. BULLISH HIDDEN DIVERGENCE:
			   - Price makes HIGHER LOW
			   - Indicator makes LOWER LOW
			   - Signal: Uptrend continuation
			   - Action: Buy the dip

			2. BEARISH HIDDEN DIVERGENCE:
			   - Price makes LOWER HIGH
			   - Indicator makes HIGHER HIGH
			   - Signal: Downtrend continuation
			   - Action: Sell the rally

			Python divergence detection:
			```
			def detect_divergence(price, indicator, lookback=14):
			    # Find recent swing points
			    price_data = price.iloc[-lookback:]
			    ind_data = indicator.iloc[-lookback:]

			    # Price highs and lows
			    price_high_idx = price_data.idxmax()
			    price_low_idx = price_data.idxmin()
			    prev_price_high = price_data.iloc[:-5].max()
			    prev_price_low = price_data.iloc[:-5].min()
			    curr_price_high = price_data.iloc[-5:].max()
			    curr_price_low = price_data.iloc[-5:].min()

			    # Indicator highs and lows
			    prev_ind_high = ind_data.iloc[:-5].max()
			    prev_ind_low = ind_data.iloc[:-5].min()
			    curr_ind_high = ind_data.iloc[-5:].max()
			    curr_ind_low = ind_data.iloc[-5:].min()

			    divergence = None

			    # Bullish Regular: Price lower low, indicator higher low
			    if curr_price_low < prev_price_low and curr_ind_low > prev_ind_low:
			        divergence = 'BULLISH_REGULAR'

			    # Bearish Regular: Price higher high, indicator lower high
			    elif curr_price_high > prev_price_high and curr_ind_high < prev_ind_high:
			        divergence = 'BEARISH_REGULAR'

			    # Bullish Hidden: Price higher low, indicator lower low
			    elif curr_price_low > prev_price_low and curr_ind_low < prev_ind_low:
			        divergence = 'BULLISH_HIDDEN'

			    # Bearish Hidden: Price lower high, indicator higher high
			    elif curr_price_high < prev_price_high and curr_ind_high > prev_ind_high:
			        divergence = 'BEARISH_HIDDEN'

			    return divergence

			# Usage
			rsi = calculate_rsi(data['close'], 14)
			divergence = detect_divergence(data['close'], rsi)

			if divergence == 'BULLISH_REGULAR':
			    signal = 'BUY'  # Potential bottom
			elif divergence == 'BEARISH_REGULAR':
			    signal = 'SELL'  # Potential top
			```

			RSI DIVERGENCE SPECIFICS:
			```
			# RSI Bullish Divergence at oversold
			rsi_oversold = rsi.iloc[-1] < 35
			bullish_div = detect_divergence(data['close'], rsi) == 'BULLISH_REGULAR'

			if rsi_oversold and bullish_div:
			    signal = 'STRONG_BUY'  # High probability reversal

			# RSI Bearish Divergence at overbought
			rsi_overbought = rsi.iloc[-1] > 65
			bearish_div = detect_divergence(data['close'], rsi) == 'BEARISH_REGULAR'

			if rsi_overbought and bearish_div:
			    signal = 'STRONG_SELL'  # High probability reversal
			```

			MACD DIVERGENCE:
			```
			macd_line = ema_12 - ema_26
			macd_histogram = macd_line - signal_line

			# MACD histogram divergence (more sensitive)
			macd_div = detect_divergence(data['close'], macd_histogram)
			```

			DIVERGENCE BEST PRACTICES:
			1. Regular divergence = REVERSAL (counter-trend)
			2. Hidden divergence = CONTINUATION (with-trend)
			3. Works best at key S/R levels
			4. Multiple indicator divergence = stronger signal
			5. Always wait for price confirmation (don't front-run)
			6. Higher timeframe divergence > lower timeframe

			-------------------------------------------
			PRICE ACTION PATTERNS
			-------------------------------------------

			Pure price action patterns without indicators.
			These work on any timeframe and any market.

			=== SINGLE BAR PATTERNS ===

			1. PIN BAR (Rejection Candle)
			   - Long wick (tail) showing rejection
			   - Small body at opposite end
			   - Bullish pin bar: Long lower wick, body at top
			   - Bearish pin bar: Long upper wick, body at bottom
			   Python:
			   ```
			   def is_pin_bar(data):
			       o, h, l, c = data['open'].iloc[-1], data['high'].iloc[-1], data['low'].iloc[-1], data['close'].iloc[-1]
			       body = abs(c - o)
			       upper_wick = h - max(o, c)
			       lower_wick = min(o, c) - l
			       total_range = h - l

			       # Bullish pin bar
			       if lower_wick > 2 * body and upper_wick < body * 0.5:
			           return 'BULLISH_PIN'
			       # Bearish pin bar
			       elif upper_wick > 2 * body and lower_wick < body * 0.5:
			           return 'BEARISH_PIN'
			       return None
			   ```

			2. INSIDE BAR (Consolidation)
			   - Current bar's high/low WITHIN previous bar's range
			   - Shows consolidation/indecision
			   - Trade the breakout direction
			   Python:
			   ```
			   def is_inside_bar(data):
			       curr_high = data['high'].iloc[-1]
			       curr_low = data['low'].iloc[-1]
			       prev_high = data['high'].iloc[-2]
			       prev_low = data['low'].iloc[-2]

			       return curr_high < prev_high and curr_low > prev_low

			   # Inside bar breakout
			   if is_inside_bar(data):
			       mother_bar_high = data['high'].iloc[-2]
			       mother_bar_low = data['low'].iloc[-2]

			       # Wait for breakout
			       if data['close'].iloc[-1] > mother_bar_high:
			           signal = 'BUY'
			       elif data['close'].iloc[-1] < mother_bar_low:
			           signal = 'SELL'
			   ```

			3. OUTSIDE BAR (Engulfing Range)
			   - Current bar's range EXCEEDS previous bar
			   - High is higher AND low is lower
			   - Strong momentum signal
			   Python:
			   ```
			   def is_outside_bar(data):
			       curr_high = data['high'].iloc[-1]
			       curr_low = data['low'].iloc[-1]
			       prev_high = data['high'].iloc[-2]
			       prev_low = data['low'].iloc[-2]

			       return curr_high > prev_high and curr_low < prev_low

			   if is_outside_bar(data):
			       # Direction based on close
			       if data['close'].iloc[-1] > data['open'].iloc[-1]:
			           signal = 'BULLISH_OUTSIDE'
			       else:
			           signal = 'BEARISH_OUTSIDE'
			   ```

			4. DOJI (Indecision)
			   - Open and close nearly equal
			   - Shows market indecision
			   - Often precedes reversals at key levels
			   Python:
			   ```
			   def is_doji(data, threshold=0.1):
			       o, h, l, c = data['open'].iloc[-1], data['high'].iloc[-1], data['low'].iloc[-1], data['close'].iloc[-1]
			       body = abs(c - o)
			       total_range = h - l

			       return body < total_range * threshold if total_range > 0 else False
			   ```

			=== MULTI-BAR PATTERNS ===

			1. TWO-BAR REVERSAL
			   - Two consecutive bars that reverse direction
			   - First bar extends in trend direction
			   - Second bar reverses and closes beyond first bar's open

			2. THREE-BAR REVERSAL
			   - Middle bar makes extreme (highest high or lowest low)
			   - Third bar reverses and closes beyond first bar
			   - Classic swing point formation

			3. FAKEY (False Breakout)
			   - Inside bar forms
			   - Breakout occurs but immediately reverses
			   - Trade in direction of reversal (trapped traders)
			   Python:
			   ```
			   def is_fakey(data):
			       # Check for inside bar 2 bars ago
			       was_inside = data['high'].iloc[-3] < data['high'].iloc[-4] and data['low'].iloc[-3] > data['low'].iloc[-4]

			       if was_inside:
			           mother_high = data['high'].iloc[-4]
			           mother_low = data['low'].iloc[-4]

			           # False breakout up, then reversal down
			           broke_up = data['high'].iloc[-2] > mother_high
			           reversed_down = data['close'].iloc[-1] < mother_low
			           if broke_up and reversed_down:
			               return 'BEARISH_FAKEY'

			           # False breakout down, then reversal up
			           broke_down = data['low'].iloc[-2] < mother_low
			           reversed_up = data['close'].iloc[-1] > mother_high
			           if broke_down and reversed_up:
			               return 'BULLISH_FAKEY'

			       return None
			   ```

			=== MARKET STRUCTURE ===

			1. HIGHER HIGHS & HIGHER LOWS (Uptrend)
			   - Each swing high > previous swing high
			   - Each swing low > previous swing low
			   - Trend intact while this continues

			2. LOWER HIGHS & LOWER LOWS (Downtrend)
			   - Each swing high < previous swing high
			   - Each swing low < previous swing low
			   - Trend intact while this continues

			3. BREAK OF STRUCTURE (BOS)
			   - In uptrend: Price breaks below previous swing low
			   - In downtrend: Price breaks above previous swing high
			   - Signals potential trend change

			4. CHANGE OF CHARACTER (ChoCH)
			   - First sign of trend weakness
			   - Uptrend: Fails to make new high, breaks structure
			   - Downtrend: Fails to make new low, breaks structure

			Python:
			```
			def detect_market_structure(data, lookback=20):
			    highs = data['high'].iloc[-lookback:]
			    lows = data['low'].iloc[-lookback:]

			    # Find swing points (simplified)
			    swing_highs = highs.rolling(5, center=True).max() == highs
			    swing_lows = lows.rolling(5, center=True).min() == lows

			    recent_swing_highs = highs[swing_highs].tail(3)
			    recent_swing_lows = lows[swing_lows].tail(3)

			    if len(recent_swing_highs) >= 2 and len(recent_swing_lows) >= 2:
			        hh = recent_swing_highs.iloc[-1] > recent_swing_highs.iloc[-2]  # Higher high
			        hl = recent_swing_lows.iloc[-1] > recent_swing_lows.iloc[-2]    # Higher low
			        lh = recent_swing_highs.iloc[-1] < recent_swing_highs.iloc[-2]  # Lower high
			        ll = recent_swing_lows.iloc[-1] < recent_swing_lows.iloc[-2]    # Lower low

			        if hh and hl:
			            return 'UPTREND'
			        elif lh and ll:
			            return 'DOWNTREND'
			        else:
			            return 'TRANSITIONING'

			    return 'UNKNOWN'
			```

			-------------------------------------------
			ICHIMOKU CLOUD
			-------------------------------------------

			Complete trading system from Japan. Provides:
			- Trend direction
			- Support/resistance levels
			- Momentum
			- Entry/exit signals

			=== ICHIMOKU COMPONENTS ===

			1. TENKAN-SEN (Conversion Line) - Period: 9
			   - (Highest High + Lowest Low) / 2 over 9 periods
			   - Short-term trend indicator
			   - Similar to fast moving average

			2. KIJUN-SEN (Base Line) - Period: 26
			   - (Highest High + Lowest Low) / 2 over 26 periods
			   - Medium-term trend indicator
			   - Key support/resistance level

			3. SENKOU SPAN A (Leading Span A)
			   - (Tenkan + Kijun) / 2, plotted 26 periods ahead
			   - First cloud boundary

			4. SENKOU SPAN B (Leading Span B) - Period: 52
			   - (Highest High + Lowest Low) / 2 over 52 periods, plotted 26 ahead
			   - Second cloud boundary

			5. CHIKOU SPAN (Lagging Span)
			   - Current close plotted 26 periods BACK
			   - Confirms trend by comparing to past price

			6. KUMO (Cloud)
			   - Area between Senkou Span A and B
			   - Green cloud (bullish): Span A > Span B
			   - Red cloud (bearish): Span A < Span B

			Python calculation:
			```
			def calculate_ichimoku(data):
			    high = data['high']
			    low = data['low']
			    close = data['close']

			    # Tenkan-sen (Conversion Line): 9-period
			    tenkan = (high.rolling(9).max() + low.rolling(9).min()) / 2

			    # Kijun-sen (Base Line): 26-period
			    kijun = (high.rolling(26).max() + low.rolling(26).min()) / 2

			    # Senkou Span A (Leading Span A): (Tenkan + Kijun) / 2, shifted 26 forward
			    senkou_a = ((tenkan + kijun) / 2).shift(26)

			    # Senkou Span B (Leading Span B): 52-period, shifted 26 forward
			    senkou_b = ((high.rolling(52).max() + low.rolling(52).min()) / 2).shift(26)

			    # Chikou Span (Lagging Span): Close shifted 26 back
			    chikou = close.shift(-26)

			    return {
			        'tenkan': tenkan,
			        'kijun': kijun,
			        'senkou_a': senkou_a,
			        'senkou_b': senkou_b,
			        'chikou': chikou
			    }

			ichi = calculate_ichimoku(data)
			```

			=== ICHIMOKU SIGNALS ===

			1. TK CROSS (Tenkan/Kijun Cross)
			   - Bullish: Tenkan crosses ABOVE Kijun
			   - Bearish: Tenkan crosses BELOW Kijun
			   - Stronger when above/below cloud
			   ```
			   bullish_tk = ichi['tenkan'].iloc[-1] > ichi['kijun'].iloc[-1] and ichi['tenkan'].iloc[-2] <= ichi['kijun'].iloc[-2]
			   bearish_tk = ichi['tenkan'].iloc[-1] < ichi['kijun'].iloc[-1] and ichi['tenkan'].iloc[-2] >= ichi['kijun'].iloc[-2]
			   ```

			2. PRICE vs CLOUD
			   - Price ABOVE cloud = Bullish (only take longs)
			   - Price BELOW cloud = Bearish (only take shorts)
			   - Price IN cloud = No trade (consolidation)
			   ```
			   cloud_top = max(ichi['senkou_a'].iloc[-1], ichi['senkou_b'].iloc[-1])
			   cloud_bottom = min(ichi['senkou_a'].iloc[-1], ichi['senkou_b'].iloc[-1])

			   above_cloud = data['close'].iloc[-1] > cloud_top
			   below_cloud = data['close'].iloc[-1] < cloud_bottom
			   in_cloud = not above_cloud and not below_cloud
			   ```

			3. CLOUD COLOR (Future Trend)
			   - Green cloud ahead = Bullish bias
			   - Red cloud ahead = Bearish bias
			   ```
			   bullish_cloud = ichi['senkou_a'].iloc[-1] > ichi['senkou_b'].iloc[-1]
			   ```

			4. CHIKOU CONFIRMATION
			   - Chikou above price (26 bars ago) = Bullish
			   - Chikou below price (26 bars ago) = Bearish
			   ```
			   chikou_bullish = ichi['chikou'].iloc[-27] > data['close'].iloc[-27]
			   ```

			=== COMPLETE ICHIMOKU STRATEGY ===
			```
			def ichimoku_signal(data):
			    ichi = calculate_ichimoku(data)
			    close = data['close'].iloc[-1]

			    cloud_top = max(ichi['senkou_a'].iloc[-1], ichi['senkou_b'].iloc[-1])
			    cloud_bottom = min(ichi['senkou_a'].iloc[-1], ichi['senkou_b'].iloc[-1])

			    # All 5 conditions for STRONG signal
			    # 1. Price above cloud
			    above_cloud = close > cloud_top

			    # 2. Tenkan above Kijun
			    tk_bullish = ichi['tenkan'].iloc[-1] > ichi['kijun'].iloc[-1]

			    # 3. Chikou above past price
			    chikou_bullish = ichi['chikou'].iloc[-27] > data['close'].iloc[-27] if len(data) > 27 else True

			    # 4. Future cloud is bullish
			    future_bullish = ichi['senkou_a'].iloc[-1] > ichi['senkou_b'].iloc[-1]

			    # 5. TK cross just occurred
			    tk_cross = ichi['tenkan'].iloc[-2] <= ichi['kijun'].iloc[-2]

			    if above_cloud and tk_bullish and chikou_bullish and future_bullish and tk_cross:
			        return 'STRONG_BUY'
			    elif above_cloud and tk_bullish:
			        return 'BUY'
			    elif close < cloud_bottom and not tk_bullish:
			        return 'SELL'
			    else:
			        return 'HOLD'
			```

			ICHIMOKU BEST PRACTICES:
			1. Best on Daily and Weekly timeframes
			2. Wait for price to clear the cloud (not in cloud)
			3. Cloud acts as support/resistance
			4. More conditions aligned = stronger signal
			5. Flat Kijun = strong S/R level

			-------------------------------------------
			GAP ANALYSIS
			-------------------------------------------

			WHAT IS A GAP?
			When price opens significantly higher or lower than previous close.
			No trading occurred at prices in between.

			GAP TYPES:

			1. COMMON GAP
			   - Occurs in ranging markets
			   - Usually fills quickly (within days)
			   - Low significance

			2. BREAKAWAY GAP
			   - Occurs at start of new trend
			   - Breaks through S/R with gap
			   - High volume confirms
			   - Often does NOT fill

			3. RUNAWAY/CONTINUATION GAP
			   - Occurs in middle of trend
			   - Shows strong momentum
			   - Partial fill then continues

			4. EXHAUSTION GAP
			   - Occurs at end of trend
			   - Final push before reversal
			   - Usually fills quickly

			Python gap detection:
			```
			def detect_gap(data, min_gap_pct=1.0):
			    prev_close = data['close'].iloc[-2]
			    curr_open = data['open'].iloc[-1]
			    curr_close = data['close'].iloc[-1]

			    gap_pct = (curr_open - prev_close) / prev_close * 100

			    if abs(gap_pct) < min_gap_pct:
			        return None

			    gap_type = 'GAP_UP' if gap_pct > 0 else 'GAP_DOWN'

			    # Check if gap filled during the day
			    if gap_type == 'GAP_UP':
			        gap_filled = data['low'].iloc[-1] <= prev_close
			    else:
			        gap_filled = data['high'].iloc[-1] >= prev_close

			    return {
			        'type': gap_type,
			        'size_pct': gap_pct,
			        'filled': gap_filled
			    }
			```

			GAP TRADING STRATEGIES:

			1. GAP AND GO (Momentum)
			   - Trade in direction of gap
			   - Works best with breakaway gaps
			   - Enter on first pullback after gap
			   ```
			   gap = detect_gap(data)
			   if gap and gap['type'] == 'GAP_UP' and not gap['filled']:
			       # Gap held, momentum continues
			       if data['close'].iloc[-1] > data['open'].iloc[-1]:
			           signal = 'BUY'
			   ```

			2. GAP FILL (Mean Reversion)
			   - Bet that gap will fill
			   - Works best with common gaps
			   - Fade the gap direction
			   ```
			   gap = detect_gap(data)
			   if gap and gap['type'] == 'GAP_UP' and gap['size_pct'] < 3:
			       # Small gap likely to fill
			       signal = 'SELL'  # Fade the gap
			       target = prev_close  # Gap fill level
			   ```

			3. GAP SUPPORT/RESISTANCE
			   - Unfilled gaps act as S/R zones
			   - Gap up creates support at gap bottom
			   - Gap down creates resistance at gap top

			GAP STATISTICS:
			- ~70% of common gaps fill within a few days
			- Breakaway gaps at new highs/lows often don't fill
			- Gaps into earnings usually fill (overreaction)
			- Monday gaps often fill by week end

			-------------------------------------------
			SENTIMENT INDICATORS
			-------------------------------------------

			Measure crowd psychology and positioning.
			Often contrarian - extreme sentiment = potential reversal.

			=== VIX (Volatility Index) ===

			"Fear Gauge" - measures expected S&P 500 volatility

			VIX LEVELS:
			- VIX < 12: Extreme complacency (potential top)
			- VIX 12-20: Normal/low volatility
			- VIX 20-30: Elevated fear
			- VIX > 30: High fear (potential bottom)
			- VIX > 40: Extreme fear (major bottom likely)

			CONTRARIAN VIX STRATEGY:
			```
			# VIX data (if available)
			vix = get_vix_data()

			if vix.iloc[-1] > 35:
			    # Extreme fear - contrarian buy signal
			    sentiment = 'EXTREME_FEAR'
			    contrarian_signal = 'BUY'
			elif vix.iloc[-1] < 12:
			    # Extreme complacency - contrarian sell signal
			    sentiment = 'EXTREME_GREED'
			    contrarian_signal = 'SELL'
			else:
			    sentiment = 'NEUTRAL'
			```

			VIX TERM STRUCTURE:
			- Contango (normal): VIX futures > VIX spot = bullish
			- Backwardation: VIX futures < VIX spot = bearish/fearful

			=== PUT/CALL RATIO ===

			Measures options market sentiment

			- Ratio = Put Volume / Call Volume
			- High ratio (>1.0): More puts = bearish sentiment
			- Low ratio (<0.7): More calls = bullish sentiment

			CONTRARIAN SIGNALS:
			```
			put_call_ratio = calculate_put_call_ratio()

			if put_call_ratio > 1.2:
			    # Extreme put buying = fear = contrarian buy
			    signal = 'CONTRARIAN_BUY'
			elif put_call_ratio < 0.5:
			    # Extreme call buying = greed = contrarian sell
			    signal = 'CONTRARIAN_SELL'
			```

			=== FEAR & GREED INDEX ===

			Composite sentiment indicator (0-100 scale)

			COMPONENTS:
			- Market momentum (S&P 500 vs 125-day MA)
			- Stock price strength (52-week highs vs lows)
			- Stock price breadth (advancing vs declining volume)
			- Put/Call ratio
			- Market volatility (VIX)
			- Safe haven demand (stocks vs bonds)
			- Junk bond demand (yield spread)

			LEVELS:
			- 0-25: Extreme Fear (buy signal)
			- 25-45: Fear
			- 45-55: Neutral
			- 55-75: Greed
			- 75-100: Extreme Greed (sell signal)

			```
			# Simplified Fear & Greed calculation
			def calculate_fear_greed(data, spy_data):
			    score = 50  # Start neutral

			    # Momentum: Price vs 125-day MA
			    ma_125 = spy_data['close'].rolling(125).mean().iloc[-1]
			    if spy_data['close'].iloc[-1] > ma_125 * 1.05:
			        score += 15  # Greed
			    elif spy_data['close'].iloc[-1] < ma_125 * 0.95:
			        score -= 15  # Fear

			    # Volatility: VIX level
			    # (would need VIX data)

			    # 52-week highs vs lows
			    high_52w = spy_data['high'].rolling(252).max().iloc[-1]
			    low_52w = spy_data['low'].rolling(252).min().iloc[-1]
			    price_position = (spy_data['close'].iloc[-1] - low_52w) / (high_52w - low_52w)
			    score += (price_position - 0.5) * 30

			    return max(0, min(100, score))
			```

			=== AAII SENTIMENT SURVEY ===

			Weekly survey of individual investors

			- Bullish %: Expect market to rise
			- Bearish %: Expect market to fall
			- Neutral %: Expect market flat

			CONTRARIAN SIGNALS:
			- Bull-Bear Spread > 30%: Extreme optimism (sell)
			- Bull-Bear Spread < -30%: Extreme pessimism (buy)

			SENTIMENT BEST PRACTICES:
			1. Sentiment is CONTRARIAN - go against extremes
			2. Sentiment alone is not timing - combine with technicals
			3. "Be fearful when others are greedy, greedy when others are fearful"
			4. Extreme sentiment can persist - wait for price confirmation
			5. Works best at major market turning points

			-------------------------------------------
			PATTERN COMBINATION STRATEGIES
			-------------------------------------------

			HIGHEST PROBABILITY SETUPS (combine multiple confirmations):

			1. BULLISH SETUP EXAMPLE:
			   - Golden Cross occurred (long-term bullish)
			   - Price pulls back to 50-day MA (entry opportunity)
			   - Hammer candle forms at MA (reversal signal)
			   - RSI rising from below 40 (momentum confirmation)
			   - Volume increases on bounce (institutional interest)
			   → HIGH PROBABILITY LONG ENTRY

			2. BEARISH SETUP EXAMPLE:
			   - Death Cross occurred (long-term bearish)
			   - Price rallies to 50-day MA (entry opportunity)
			   - Shooting star at MA (reversal signal)
			   - RSI falling from above 60 (momentum confirmation)
			   - Volume increases on rejection (institutional selling)
			   → HIGH PROBABILITY SHORT ENTRY

			-------------------------------------------
			PATTERN-BASED STRATEGY IMPLEMENTATION
			-------------------------------------------

			When generating strategies, USE these patterns by:

			1. DETECT the pattern in code (see Python examples above)
			2. CONFIRM with supporting indicators
			3. SET entry at pattern completion/breakout
			4. SET stop-loss at pattern invalidation level
			5. SET target based on pattern measurement rules

			Example Pattern-Based Strategy Structure:
			```python
			def strategy(data):
			    # Pattern detection
			    golden_cross = sma_50.iloc[-1] > sma_200.iloc[-1] and sma_50.iloc[-2] <= sma_200.iloc[-2]
			    price_at_support = data['close'].iloc[-1] <= sma_50.iloc[-1] * 1.02

			    # Candlestick confirmation
			    hammer = detect_hammer(data)
			    bullish_engulfing = detect_engulfing(data, 'bullish')

			    # Momentum confirmation
			    rsi_oversold_reversal = rsi.iloc[-1] > 30 and rsi.iloc[-2] < 30

			    # Entry: Multiple confirmations
			    if golden_cross and price_at_support and (hammer or bullish_engulfing) and rsi_oversold_reversal:
			        return 'BUY'

			    # Exit on pattern failure
			    if data['close'].iloc[-1] < sma_200.iloc[-1]:
			        return 'SELL'

			    return 'HOLD'
			```

			""";

	public static final String TECHNICAL_ANALYSIS_KNOWLEDGE_PART3 = """
			============================================
			PROVEN WINNING STRATEGY TEMPLATES
			============================================

			These are battle-tested strategies with documented historical performance.
			USE THESE AS TEMPLATES when generating strategies.

			=== STRATEGY 1: GOLDEN CROSS TREND FOLLOWING ===
			Historical Win Rate: 55-60% | Profit Factor: 1.8-2.2 | Best For: Trending Markets

			LOGIC:
			- Enter LONG when 50 SMA crosses above 200 SMA (Golden Cross)
			- Stay in trade while price remains above 50 SMA
			- Exit when price closes below 50 SMA OR 50 SMA crosses below 200 SMA

			WHY IT WORKS:
			- Captures major trends (months to years)
			- Avoids choppy markets (no signal when MAs flat)
			- Lets winners run, cuts losers short

			COMPLETE IMPLEMENTATION:
			```python
			import pandas as pd
			import numpy as np

			SYMBOL = 'SPY'
			TIMEFRAME = '1D'
			STOP_LOSS = 8.0      # Wide stop for trend following
			TAKE_PROFIT = 50.0   # Let winners run (or use trailing)
			POSITION_SIZE = 10

			def strategy(data):
			    sma_50 = data['close'].rolling(50).mean()
			    sma_200 = data['close'].rolling(200).mean()

			    # Current values
			    curr_price = data['close'].iloc[-1]
			    curr_sma50 = sma_50.iloc[-1]
			    curr_sma200 = sma_200.iloc[-1]
			    prev_sma50 = sma_50.iloc[-2]
			    prev_sma200 = sma_200.iloc[-2]

			    # Golden Cross: 50 SMA crosses above 200 SMA
			    golden_cross = prev_sma50 <= prev_sma200 and curr_sma50 > curr_sma200

			    # Death Cross: 50 SMA crosses below 200 SMA
			    death_cross = prev_sma50 >= prev_sma200 and curr_sma50 < curr_sma200

			    # Trend confirmation: Price above both MAs
			    bullish_trend = curr_price > curr_sma50 > curr_sma200

			    # Entry on Golden Cross
			    if golden_cross:
			        return 'BUY'

			    # Exit on Death Cross or price breakdown
			    if death_cross or curr_price < curr_sma50:
			        return 'SELL'

			    return 'HOLD'
			```

			=== STRATEGY 2: RSI DIVERGENCE REVERSAL ===
			Historical Win Rate: 62-68% | Profit Factor: 1.6-2.0 | Best For: Range-Bound Markets

			LOGIC:
			- Identify bullish divergence: Price makes lower low, RSI makes higher low
			- Enter LONG when RSI crosses back above 30 after divergence
			- Exit at RSI 60-70 or at resistance

			WHY IT WORKS:
			- Catches exhaustion moves before reversal
			- High win rate at key support levels
			- Works in ranging and early trend markets

			COMPLETE IMPLEMENTATION:
			```python
			SYMBOL = 'AAPL'
			TIMEFRAME = '1D'
			STOP_LOSS = 4.0
			TAKE_PROFIT = 12.0   # 3:1 reward/risk
			POSITION_SIZE = 5

			def strategy(data):
			    # RSI calculation
			    delta = data['close'].diff()
			    gain = delta.where(delta > 0, 0).rolling(14).mean()
			    loss = (-delta.where(delta < 0, 0)).rolling(14).mean()
			    rs = gain / loss
			    rsi = 100 - (100 / (1 + rs))

			    # Detect bullish divergence (last 14 bars)
			    price_low_curr = data['close'].iloc[-7:].min()
			    price_low_prev = data['close'].iloc[-14:-7].min()
			    rsi_low_curr = rsi.iloc[-7:].min()
			    rsi_low_prev = rsi.iloc[-14:-7].min()

			    bullish_divergence = (price_low_curr < price_low_prev) and (rsi_low_curr > rsi_low_prev)

			    # Entry: RSI crossing above 30 after divergence
			    rsi_cross_up = rsi.iloc[-1] > 30 and rsi.iloc[-2] <= 30

			    if bullish_divergence and rsi_cross_up:
			        return 'BUY'

			    # Exit: RSI overbought
			    if rsi.iloc[-1] > 65:
			        return 'SELL'

			    return 'HOLD'
			```

			=== STRATEGY 3: BOLLINGER SQUEEZE BREAKOUT ===
			Historical Win Rate: 58-65% | Profit Factor: 2.0-2.5 | Best For: All Markets

			LOGIC:
			- Identify squeeze: Bollinger Band width at 6-month low
			- Wait for breakout: Price closes outside bands with volume
			- Enter in direction of breakout
			- Exit on return to middle band or opposite band touch

			WHY IT WORKS:
			- Low volatility ALWAYS precedes high volatility
			- Squeeze identifies exact timing of big moves
			- Volume confirms institutional participation

			COMPLETE IMPLEMENTATION:
			```python
			SYMBOL = 'TSLA'
			TIMEFRAME = '1D'
			STOP_LOSS = 5.0
			TAKE_PROFIT = 15.0
			POSITION_SIZE = 5

			def strategy(data):
			    # Bollinger Bands
			    sma = data['close'].rolling(20).mean()
			    std = data['close'].rolling(20).std()
			    bb_upper = sma + (std * 2)
			    bb_lower = sma - (std * 2)
			    bb_width = (bb_upper - bb_lower) / sma * 100

			    # Squeeze detection: Width at 6-month low
			    min_width = bb_width.rolling(126).min()
			    is_squeeze = bb_width.iloc[-1] <= min_width.iloc[-1] * 1.1

			    # Volume confirmation
			    avg_volume = data['volume'].rolling(20).mean()
			    high_volume = data['volume'].iloc[-1] > avg_volume.iloc[-1] * 1.5

			    # Breakout detection
			    curr_close = data['close'].iloc[-1]
			    breakout_up = curr_close > bb_upper.iloc[-1]
			    breakout_down = curr_close < bb_lower.iloc[-1]

			    # Entry: Squeeze breakout with volume
			    if is_squeeze and breakout_up and high_volume:
			        return 'BUY'

			    if is_squeeze and breakout_down and high_volume:
			        return 'SELL'

			    # Exit: Return to middle band
			    at_middle = abs(curr_close - sma.iloc[-1]) / sma.iloc[-1] < 0.01
			    if at_middle:
			        return 'SELL'  # Close position

			    return 'HOLD'
			```

			=== STRATEGY 4: PULLBACK TO 20 EMA IN UPTREND ===
			Historical Win Rate: 60-65% | Profit Factor: 1.8-2.2 | Best For: Trending Markets

			LOGIC:
			- Confirm uptrend: Price > 20 EMA > 50 SMA > 200 SMA
			- Wait for pullback to 20 EMA (within 1%)
			- Enter on bullish candle at EMA
			- Exit on break below 50 SMA

			WHY IT WORKS:
			- Buys dips in confirmed uptrends
			- EMA acts as dynamic support
			- Trend alignment filters losers

			COMPLETE IMPLEMENTATION:
			```python
			SYMBOL = 'NVDA'
			TIMEFRAME = '1D'
			STOP_LOSS = 5.0
			TAKE_PROFIT = 15.0
			POSITION_SIZE = 5

			def strategy(data):
			    ema_20 = data['close'].ewm(span=20).mean()
			    sma_50 = data['close'].rolling(50).mean()
			    sma_200 = data['close'].rolling(200).mean()

			    curr_close = data['close'].iloc[-1]
			    curr_ema20 = ema_20.iloc[-1]
			    curr_sma50 = sma_50.iloc[-1]
			    curr_sma200 = sma_200.iloc[-1]

			    # Uptrend confirmation: Bullish MA alignment
			    uptrend = curr_ema20 > curr_sma50 > curr_sma200

			    # Pullback to 20 EMA (within 1%)
			    at_ema = abs(curr_close - curr_ema20) / curr_ema20 < 0.01

			    # Bullish candle confirmation
			    bullish_candle = data['close'].iloc[-1] > data['open'].iloc[-1]
			    prev_bearish = data['close'].iloc[-2] < data['open'].iloc[-2]

			    # Entry: Pullback to EMA in uptrend
			    if uptrend and at_ema and bullish_candle and prev_bearish:
			        return 'BUY'

			    # Exit: Break below 50 SMA
			    if curr_close < curr_sma50:
			        return 'SELL'

			    return 'HOLD'
			```

			=== STRATEGY 5: MACD HISTOGRAM REVERSAL ===
			Historical Win Rate: 55-60% | Profit Factor: 1.5-1.8 | Best For: All Markets

			LOGIC:
			- Wait for MACD histogram to reach extreme (< -0.5 or > 0.5)
			- Enter when histogram starts reversing (change in direction)
			- Exit when histogram crosses zero or reaches opposite extreme

			COMPLETE IMPLEMENTATION:
			```python
			SYMBOL = 'MSFT'
			TIMEFRAME = '1D'
			STOP_LOSS = 4.0
			TAKE_PROFIT = 10.0
			POSITION_SIZE = 5

			def strategy(data):
			    # MACD calculation
			    ema_12 = data['close'].ewm(span=12).mean()
			    ema_26 = data['close'].ewm(span=26).mean()
			    macd_line = ema_12 - ema_26
			    signal_line = macd_line.ewm(span=9).mean()
			    histogram = macd_line - signal_line

			    curr_hist = histogram.iloc[-1]
			    prev_hist = histogram.iloc[-2]
			    prev_prev_hist = histogram.iloc[-3]

			    # Histogram reversal from negative extreme
			    bullish_reversal = (prev_prev_hist < prev_hist < curr_hist) and (prev_hist < -0.3)

			    # Histogram reversal from positive extreme
			    bearish_reversal = (prev_prev_hist > prev_hist > curr_hist) and (prev_hist > 0.3)

			    if bullish_reversal:
			        return 'BUY'

			    if bearish_reversal:
			        return 'SELL'

			    # Exit on zero cross
			    zero_cross_down = prev_hist > 0 and curr_hist <= 0
			    zero_cross_up = prev_hist < 0 and curr_hist >= 0

			    if zero_cross_down or zero_cross_up:
			        return 'SELL'

			    return 'HOLD'
			```

			============================================
			RISK MANAGEMENT & POSITION SIZING
			============================================

			CRITICAL: Risk management is MORE IMPORTANT than entry signals!
			A mediocre strategy with great risk management beats a great strategy with poor risk management.

			=== THE 2% RULE ===

			Never risk more than 2% of account on a single trade.

			```python
			def calculate_position_size(account_balance, entry_price, stop_loss_price):
			    risk_per_trade = account_balance * 0.02  # 2% max risk
			    risk_per_share = abs(entry_price - stop_loss_price)
			    position_size = risk_per_trade / risk_per_share
			    return int(position_size)

			# Example:
			# Account: $10,000
			# Entry: $100
			# Stop: $95 (5% stop)
			# Risk per share: $5
			# Max risk: $200 (2% of $10,000)
			# Position size: $200 / $5 = 40 shares
			```

			=== KELLY CRITERION (Optimal Position Sizing) ===

			Mathematically optimal position size based on win rate and reward/risk.

			Formula: Kelly % = W - [(1-W) / R]
			Where:
			- W = Win rate (as decimal)
			- R = Reward/Risk ratio

			```python
			def kelly_criterion(win_rate, reward_risk_ratio):
			    kelly = win_rate - ((1 - win_rate) / reward_risk_ratio)
			    # Never use full Kelly - use half or quarter
			    half_kelly = kelly / 2
			    return max(0, min(0.25, half_kelly))  # Cap at 25%

			# Example:
			# Win rate: 55%
			# Reward/Risk: 2:1
			# Kelly = 0.55 - (0.45 / 2) = 0.55 - 0.225 = 0.325 (32.5%)
			# Half Kelly = 16.25%
			# Use: 16% of account per trade

			def calculate_kelly_position(account_balance, win_rate, avg_win, avg_loss):
			    reward_risk = abs(avg_win / avg_loss)
			    kelly_pct = kelly_criterion(win_rate, reward_risk)
			    position_value = account_balance * kelly_pct
			    return position_value
			```

			=== FIXED FRACTIONAL POSITION SIZING ===

			Risk a fixed percentage of current account balance.

			```python
			def fixed_fractional_size(account_balance, risk_pct, entry, stop_loss):
			    risk_amount = account_balance * (risk_pct / 100)
			    risk_per_share = abs(entry - stop_loss)
			    shares = risk_amount / risk_per_share
			    return int(shares)

			# Aggressive: 3% risk per trade
			# Moderate: 2% risk per trade
			# Conservative: 1% risk per trade
			```

			=== VOLATILITY-ADJUSTED POSITION SIZING ===

			Adjust position size based on current volatility (ATR).

			```python
			def volatility_adjusted_size(account_balance, data, risk_pct=2.0):
			    # Calculate ATR
			    tr = pd.concat([
			        data['high'] - data['low'],
			        abs(data['high'] - data['close'].shift(1)),
			        abs(data['low'] - data['close'].shift(1))
			    ], axis=1).max(axis=1)
			    atr = tr.rolling(14).mean().iloc[-1]

			    # Position size inversely proportional to volatility
			    risk_amount = account_balance * (risk_pct / 100)
			    # Use 2x ATR as stop distance
			    stop_distance = atr * 2
			    shares = risk_amount / stop_distance
			    return int(shares)
			```

			=== MAXIMUM PORTFOLIO HEAT ===

			Total portfolio risk should not exceed 6-10%.

			```python
			def check_portfolio_heat(open_positions, account_balance, max_heat=6.0):
			    total_risk = 0
			    for position in open_positions:
			        risk = position['shares'] * abs(position['entry'] - position['stop'])
			        total_risk += risk

			    heat_pct = (total_risk / account_balance) * 100

			    if heat_pct >= max_heat:
			        return False  # Don't add new positions
			    return True

			# Rules:
			# - Max 3-5 open positions at once
			# - Total portfolio risk < 6%
			# - No correlated positions (e.g., AAPL + QQQ)
			```

			=== MAXIMUM DRAWDOWN RULES ===

			```python
			def check_drawdown(current_balance, peak_balance, max_drawdown=15.0):
			    drawdown = ((peak_balance - current_balance) / peak_balance) * 100

			    if drawdown >= max_drawdown:
			        return 'STOP_TRADING'  # Hit max drawdown, stop
			    elif drawdown >= max_drawdown * 0.75:
			        return 'REDUCE_SIZE'   # Near max, reduce position sizes
			    else:
			        return 'NORMAL'

			# Drawdown rules:
			# - 10% drawdown: Reduce position size by 50%
			# - 15% drawdown: Stop trading, review strategy
			# - 20% drawdown: Major review required
			```

			============================================
			BACKTESTING BEST PRACTICES
			============================================

			Avoid OVERFITTING - the #1 killer of trading strategies!

			=== WHAT IS OVERFITTING? ===

			Overfitting = Strategy is "curve-fitted" to historical data
			Result = Great backtest, terrible live performance

			Signs of overfitting:
			- Too many parameters (>5 is suspicious)
			- Very specific parameter values (RSI 37.2 instead of 30)
			- Perfect entries/exits in backtest
			- Strategy fails on different time periods

			=== WALK-FORWARD OPTIMIZATION ===

			The GOLD STANDARD for strategy validation.

			Process:
			1. Divide data into segments (e.g., 12 months each)
			2. Optimize on segment 1, test on segment 2
			3. Optimize on segments 1-2, test on segment 3
			4. Continue walking forward
			5. Strategy must work on ALL out-of-sample segments

			```python
			def walk_forward_test(data, strategy, window_size=252, test_size=63):
			    results = []
			    for i in range(window_size, len(data) - test_size, test_size):
			        train_data = data.iloc[i-window_size:i]
			        test_data = data.iloc[i:i+test_size]

			        # Optimize on train data
			        best_params = optimize_strategy(train_data, strategy)

			        # Test on out-of-sample data
			        test_result = backtest(test_data, strategy, best_params)
			        results.append(test_result)

			    # Strategy is valid if profitable in >70% of test periods
			    profitable_periods = sum(1 for r in results if r['return'] > 0)
			    return profitable_periods / len(results) > 0.7
			```

			=== OUT-OF-SAMPLE TESTING ===

			ALWAYS reserve 20-30% of data for final validation.

			```python
			def split_data_for_testing(data):
			    split_point = int(len(data) * 0.7)
			    in_sample = data.iloc[:split_point]    # 70% for development
			    out_of_sample = data.iloc[split_point:]  # 30% for validation

			    # NEVER look at out_of_sample during development!
			    # Only test ONCE when strategy is finalized
			    return in_sample, out_of_sample
			```

			=== MINIMUM TRADE REQUIREMENTS ===

			Statistical significance requires enough trades.

			```python
			def validate_sample_size(total_trades, win_rate):
			    # Minimum trades for statistical significance
			    if total_trades < 30:
			        return 'INSUFFICIENT_DATA'

			    # For 95% confidence:
			    # n >= (1.96^2 * p * (1-p)) / E^2
			    # Where E is margin of error (typically 5%)
			    required = (1.96**2 * win_rate * (1 - win_rate)) / (0.05**2)

			    if total_trades >= required:
			        return 'STATISTICALLY_SIGNIFICANT'
			    else:
			        return 'NEED_MORE_TRADES'

			# Rules:
			# - Minimum 30 trades for any conclusions
			# - Minimum 100 trades for reliable statistics
			# - Minimum 3 years of data for daily strategies
			```

			=== SLIPPAGE AND COMMISSION AWARENESS ===

			Real trading has costs that kill marginal strategies.

			```python
			def apply_realistic_costs(backtest_result, trades):
			    # Typical costs
			    commission_per_trade = 1.00  # $1 per trade
			    slippage_pct = 0.05  # 0.05% slippage per trade

			    total_commission = len(trades) * commission_per_trade * 2  # Entry + exit
			    total_slippage = sum(t['value'] * slippage_pct / 100 * 2 for t in trades)

			    adjusted_return = backtest_result['gross_return'] - total_commission - total_slippage

			    # Strategy must be profitable AFTER costs
			    return adjusted_return

			# Rules:
			# - Include $1-2 commission per trade
			# - Include 0.05-0.1% slippage
			# - High-frequency strategies need even more buffer
			# - If strategy barely profitable before costs, it will lose money
			```

			=== ROBUSTNESS TESTING ===

			Strategy should work with slightly different parameters.

			```python
			def test_parameter_robustness(strategy, base_params):
			    variations = []
			    for param, value in base_params.items():
			        # Test +/- 20% of parameter value
			        for mult in [0.8, 0.9, 1.0, 1.1, 1.2]:
			            test_params = base_params.copy()
			            test_params[param] = value * mult
			            result = backtest(strategy, test_params)
			            variations.append(result)

			    # Strategy is robust if profitable with all variations
			    profitable = sum(1 for v in variations if v['return'] > 0)
			    return profitable / len(variations) > 0.8  # 80%+ should be profitable
			```

			============================================
			STATISTICAL EDGE REQUIREMENTS
			============================================

			A strategy MUST have a quantifiable edge to be worth trading.

			=== MINIMUM PERFORMANCE THRESHOLDS ===

			REQUIRE these minimums before considering a strategy:

			```python
			def validate_strategy_edge(backtest_result):
			    checks = {
			        'win_rate': backtest_result['win_rate'] >= 0.45,  # Min 45%
			        'profit_factor': backtest_result['profit_factor'] >= 1.5,  # Min 1.5
			        'sharpe_ratio': backtest_result['sharpe'] >= 1.0,  # Min 1.0
			        'max_drawdown': backtest_result['max_dd'] <= 20.0,  # Max 20%
			        'total_trades': backtest_result['trades'] >= 50,  # Min 50 trades
			        'avg_trade': backtest_result['avg_trade_pct'] >= 0.5,  # Min 0.5% avg
			    }

			    passed = all(checks.values())
			    return passed, checks

			# Ideal targets:
			# - Win rate: 50-60%
			# - Profit factor: 2.0+
			# - Sharpe ratio: 1.5+
			# - Max drawdown: <15%
			# - Avg trade: >1%
			```

			=== EXPECTANCY CALCULATION ===

			Expected value per trade.

			```python
			def calculate_expectancy(win_rate, avg_win, avg_loss):
			    # Expectancy = (Win% × Avg Win) - (Loss% × Avg Loss)
			    expectancy = (win_rate * avg_win) - ((1 - win_rate) * abs(avg_loss))
			    return expectancy

			# Example:
			# Win rate: 55%
			# Avg win: $200
			# Avg loss: $100
			# Expectancy = (0.55 × 200) - (0.45 × 100) = 110 - 45 = $65 per trade

			# Rules:
			# - Expectancy MUST be positive
			# - Higher expectancy = better strategy
			# - Expectancy × Trade frequency = Expected monthly return
			```

			=== PROFIT FACTOR ===

			Ratio of gross profits to gross losses.

			```python
			def calculate_profit_factor(trades):
			    gross_profit = sum(t['pnl'] for t in trades if t['pnl'] > 0)
			    gross_loss = abs(sum(t['pnl'] for t in trades if t['pnl'] < 0))

			    if gross_loss == 0:
			        return float('inf')

			    profit_factor = gross_profit / gross_loss
			    return profit_factor

			# Interpretation:
			# - PF < 1.0: Losing strategy
			# - PF 1.0-1.5: Marginal (probably not worth trading after costs)
			# - PF 1.5-2.0: Good strategy
			# - PF 2.0-3.0: Excellent strategy
			# - PF > 3.0: Exceptional (verify not overfitted!)
			```

			=== SHARPE RATIO ===

			Risk-adjusted return measurement.

			```python
			def calculate_sharpe(returns, risk_free_rate=0.02):
			    excess_returns = returns - (risk_free_rate / 252)  # Daily risk-free
			    sharpe = (excess_returns.mean() / excess_returns.std()) * np.sqrt(252)
			    return sharpe

			# Interpretation:
			# - Sharpe < 0: Losing money
			# - Sharpe 0-1: Subpar risk-adjusted returns
			# - Sharpe 1-2: Good risk-adjusted returns
			# - Sharpe 2-3: Excellent
			# - Sharpe > 3: Exceptional (verify not overfitted!)
			```

			=== SORTINO RATIO ===

			Like Sharpe but only penalizes downside volatility.

			```python
			def calculate_sortino(returns, risk_free_rate=0.02):
			    excess_returns = returns - (risk_free_rate / 252)
			    downside_returns = excess_returns[excess_returns < 0]
			    downside_std = downside_returns.std()

			    if downside_std == 0:
			        return float('inf')

			    sortino = (excess_returns.mean() / downside_std) * np.sqrt(252)
			    return sortino

			# Sortino > Sharpe means strategy has more upside than downside volatility
			# This is desirable!
			```

			============================================
			ADAPTIVE PARAMETERS
			============================================

			Static parameters fail in changing markets.
			ADAPT parameters to current conditions.

			=== ATR-BASED STOP LOSS ===

			Dynamic stop loss based on current volatility.

			```python
			def calculate_atr_stop(data, atr_multiplier=2.0):
			    # ATR calculation
			    tr = pd.concat([
			        data['high'] - data['low'],
			        abs(data['high'] - data['close'].shift(1)),
			        abs(data['low'] - data['close'].shift(1))
			    ], axis=1).max(axis=1)
			    atr = tr.rolling(14).mean().iloc[-1]

			    entry_price = data['close'].iloc[-1]
			    stop_loss = entry_price - (atr * atr_multiplier)

			    return stop_loss

			# Advantages:
			# - Wider stops in volatile markets (avoid noise)
			# - Tighter stops in calm markets (protect profits)
			# - Automatically adjusts to market conditions
			```

			=== VOLATILITY-ADJUSTED TAKE PROFIT ===

			```python
			def calculate_atr_target(data, reward_risk=3.0, atr_multiplier=2.0):
			    tr = pd.concat([
			        data['high'] - data['low'],
			        abs(data['high'] - data['close'].shift(1)),
			        abs(data['low'] - data['close'].shift(1))
			    ], axis=1).max(axis=1)
			    atr = tr.rolling(14).mean().iloc[-1]

			    entry_price = data['close'].iloc[-1]
			    stop_distance = atr * atr_multiplier
			    target_distance = stop_distance * reward_risk

			    take_profit = entry_price + target_distance

			    return take_profit
			```

			=== REGIME-ADAPTIVE PARAMETERS ===

			Different parameters for different market regimes.

			```python
			def get_adaptive_params(data):
			    # Detect regime using ADX
			    adx = calculate_adx(data)

			    if adx.iloc[-1] > 25:
			        # Trending market
			        return {
			            'strategy_type': 'trend_following',
			            'stop_loss_atr_mult': 2.5,  # Wider stops
			            'take_profit_atr_mult': 5.0,  # Let winners run
			            'rsi_oversold': 40,  # Less extreme threshold
			            'rsi_overbought': 60
			        }
			    else:
			        # Ranging market
			        return {
			            'strategy_type': 'mean_reversion',
			            'stop_loss_atr_mult': 1.5,  # Tighter stops
			            'take_profit_atr_mult': 2.0,  # Take profits quickly
			            'rsi_oversold': 30,  # Standard thresholds
			            'rsi_overbought': 70
			        }
			```

			=== TRAILING STOP IMPLEMENTATION ===

			Let winners run while protecting profits.

			```python
			def trailing_stop(data, entry_price, position_type='long', trail_pct=10.0):
			    if position_type == 'long':
			        highest_since_entry = data['high'].max()
			        trail_stop = highest_since_entry * (1 - trail_pct/100)
			        current_stop = max(entry_price * 0.95, trail_stop)  # Never below initial stop
			    else:
			        lowest_since_entry = data['low'].min()
			        trail_stop = lowest_since_entry * (1 + trail_pct/100)
			        current_stop = min(entry_price * 1.05, trail_stop)

			    return current_stop

			# Or ATR-based trailing:
			def atr_trailing_stop(data, atr_mult=2.5):
			    atr = calculate_atr(data)
			    highest = data['high'].iloc[-20:].max()
			    trail_stop = highest - (atr * atr_mult)
			    return trail_stop
			```

			=== DYNAMIC POSITION SIZING ===

			Adjust size based on strategy confidence.

			```python
			def confidence_adjusted_size(base_size, signal_strength, max_multiplier=2.0):
			    # signal_strength from 0 to 1 (0 = weak, 1 = strong)

			    # More conditions aligned = larger position
			    if signal_strength > 0.8:
			        multiplier = max_multiplier
			    elif signal_strength > 0.6:
			        multiplier = 1.5
			    elif signal_strength > 0.4:
			        multiplier = 1.0
			    else:
			        multiplier = 0.5  # Weak signal = smaller position

			    return base_size * multiplier

			# Example usage:
			# signal_strength = count_confirmations() / total_possible_confirmations
			# position = confidence_adjusted_size(1000, signal_strength)
			```

			=== MARKET CONDITION FILTERS ===

			Don't trade in unfavorable conditions.

			```python
			def check_trading_conditions(data, spy_data=None):
			    conditions = {
			        'sufficient_volume': data['volume'].iloc[-1] > data['volume'].rolling(20).mean().iloc[-1] * 0.5,
			        'not_extreme_volatility': calculate_atr(data) < calculate_atr(data).rolling(100).mean().iloc[-1] * 2,
			        'market_open': True,  # Would check actual market hours
			    }

			    # Check market regime if SPY data available
			    if spy_data is not None:
			        spy_trend = spy_data['close'].iloc[-1] > spy_data['close'].rolling(50).mean().iloc[-1]
			        conditions['market_favorable'] = spy_trend

			    # Only trade if ALL conditions met
			    return all(conditions.values())
			```

			============================================
			STRATEGY GENERATION CHECKLIST
			============================================

			Before returning ANY strategy, verify:

			□ ENTRY RULES
			  - Clear, specific entry conditions
			  - Multiple confirmations (2-3 minimum)
			  - Not over-optimized (simple parameters)

			□ EXIT RULES
			  - Stop loss defined (ATR-based preferred)
			  - Take profit defined OR trailing stop
			  - Time-based exit if needed

			□ RISK MANAGEMENT
			  - Position sizing formula included
			  - Maximum 2% risk per trade
			  - No more than 5 open positions

			□ MARKET REGIME
			  - Strategy type matches market regime
			  - Trend-following for trending markets
			  - Mean-reversion for ranging markets

			□ VALIDATION
			  - Win rate > 45%
			  - Profit factor > 1.5
			  - Max drawdown < 20%
			  - Minimum 50 trades in backtest

			□ ROBUSTNESS
			  - Works on different time periods
			  - Works with slightly different parameters
			  - Accounts for slippage and commissions
			""";

	// ========================================
	// STRATEGY OPTIMIZATION PROMPTS
	// ========================================

	/**
	 * Prompt for generating a completely new strategy that outperforms a baseline.
	 * Used when GENERATE_NEW optimization mode is selected.
	 */
	public static final String OPTIMIZE_GENERATE_NEW_PROMPT = """
			You are optimizing a backtested trading strategy that underperformed.

			BASELINE STRATEGY PERFORMANCE:
			- Total Return: %s%%
			- Win Rate: %s%%
			- Sharpe Ratio: %s
			- Max Drawdown: %s%%
			- Profit Factor: %s
			- Total Trades: %s

			YOUR GOAL: Generate a COMPLETELY NEW strategy that OUTPERFORMS these baseline metrics.

			Requirements:
			1. Use different technical indicators or combinations
			2. Target at least 20%% improvement in Sharpe Ratio
			3. Reduce max drawdown by at least 30%%
			4. Maintain win rate above 50%%

			%s

			CRITICAL: Return full strategy in standard JSON format with:
			- visualConfig (with optimized parameters)
			- pythonCode (implementing the new approach)
			- optimizationSummary (explaining how this beats baseline)

			In optimizationSummary.changes, list what indicators you changed and why.
			""";

	/**
	 * Prompt for enhancing an existing strategy by optimizing parameters and logic.
	 * Used when ENHANCE_EXISTING optimization mode is selected.
	 */
	public static final String OPTIMIZE_ENHANCE_EXISTING_PROMPT = """
			You are refining an existing backtested trading strategy to improve performance.

			CURRENT STRATEGY:
			Visual Config: %s
			Python Code: %s

			BACKTEST RESULTS:
			- Total Return: %s%%
			- Win Rate: %s%%
			- Sharpe Ratio: %s
			- Max Drawdown: %s%%
			- Profit Factor: %s
			- Total Trades: %s

			%s

			YOUR GOAL: ENHANCE this strategy by optimizing parameters and refining logic.

			Optimization Strategies:
			1. Parameter Tuning: Adjust stop-loss/take-profit based on volatility
			2. Logic Refinement: Add confirmation from top performing indicators
			3. Risk Management: Adjust position sizing based on volatility regime

			Return optimized strategy with detailed changes list in optimizationSummary.
			""";

	/**
	 * Build prompt for generating a completely new optimized strategy.
	 *
	 * @param totalReturn Baseline total return percentage
	 * @param winRate Baseline win rate percentage
	 * @param sharpeRatio Baseline Sharpe ratio
	 * @param maxDrawdown Baseline max drawdown percentage
	 * @param profitFactor Baseline profit factor
	 * @param totalTrades Baseline total trades
	 * @param insights Optional historical insights
	 * @return Formatted prompt
	 */
	public static String buildGenerateNewOptimizedPrompt(double totalReturn, double winRate, double sharpeRatio,
			double maxDrawdown, double profitFactor, int totalTrades, SymbolInsights insights) {

		String insightsSection = insights != null ? buildHistoricalInsightsPrompt(insights) : "";

		return String.format(OPTIMIZE_GENERATE_NEW_PROMPT, totalReturn, winRate, sharpeRatio, maxDrawdown,
				profitFactor, totalTrades, insightsSection);
	}

	/**
	 * Build prompt for enhancing an existing strategy.
	 *
	 * @param visualConfig Current visual configuration JSON string
	 * @param currentCode Current Python code
	 * @param totalReturn Baseline total return percentage
	 * @param winRate Baseline win rate percentage
	 * @param sharpeRatio Baseline Sharpe ratio
	 * @param maxDrawdown Baseline max drawdown percentage
	 * @param profitFactor Baseline profit factor
	 * @param totalTrades Baseline total trades
	 * @param insights Optional historical insights
	 * @return Formatted prompt
	 */
	public static String buildEnhanceExistingPrompt(String visualConfig, String currentCode, double totalReturn,
			double winRate, double sharpeRatio, double maxDrawdown, double profitFactor, int totalTrades,
			SymbolInsights insights) {

		String insightsSection = insights != null ? buildHistoricalInsightsPrompt(insights) : "";

		return String.format(OPTIMIZE_ENHANCE_EXISTING_PROMPT, visualConfig, currentCode, totalReturn, winRate,
				sharpeRatio, maxDrawdown, profitFactor, totalTrades, insightsSection);
	}

}
