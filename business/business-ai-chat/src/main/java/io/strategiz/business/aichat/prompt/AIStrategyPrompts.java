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
			      - Extract from: "1 minute", "5 min", "hourly", "1 hour", "4H", "daily", "1 day", "weekly", etc.
			      - Convert to standard format: "1Min", "5Min", "15Min", "1H", "4H", "1D", "1W", "1M"
			      - If NO timeframe mentioned, default to: "1D" (daily)
			      - Examples:
			        * "on the 1 hour chart" → TIMEFRAME = '1H'
			        * "intraday 15 minute strategy" → TIMEFRAME = '15Min'
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
			   TIMEFRAME = '1D'          # From "timeframe of 1 day"
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
			   TIMEFRAME = '1H'       # Extracted: "on 1 hour chart" OR default '1D'
			   STOP_LOSS = 3.0        # Extracted: "3% stop loss" OR intelligent default
			   TAKE_PROFIT = 8.0      # Extracted: "8% take profit" OR 3:1 ratio default
			   POSITION_SIZE = 5      # Default 5% unless specified
			   ```

			3. REQUIRED: Define a strategy(data) function that:
			   - Takes a pandas DataFrame 'data' with columns: open, high, low, close, volume
			   - Returns EXACTLY one of these strings: 'BUY', 'SELL', or 'HOLD'
			   - Uses the LAST row of data (data.iloc[-1] or data['close'].iloc[-1]) for current values
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
		prompt.append("2. Use ONLY the TOP 2-3 indicators proven effective from the historical data above\n");
		prompt.append("3. Apply the EXACT OPTIMAL PARAMETERS discovered (not generic defaults)\n");
		prompt.append("4. Set stop-loss/take-profit based on the volatility analysis above\n");
		prompt.append("5. Choose strategy type (trend/mean-reversion) based on the Market Characteristics\n");
		prompt.append("6. Aim for win rate AT LEAST matching the Historical Avg Win Rate shown above\n");
		prompt.append(String.format("7. In summaryCard, explain how this BEATS buy and hold for %s\n\n",
				insights.getSymbol()));

		prompt.append("DATA-DRIVEN APPROACH:\n");
		prompt.append("- The historical data shows EXACTLY which indicators work best\n");
		prompt.append("- The optimal parameters are pre-calculated from 7 years of backtests\n");
		prompt.append("- Don't guess - use the proven indicators and settings from the data\n");
		prompt.append("- If the data says RSI works best, DON'T use MACD\n");
		prompt.append("- If the data says mean-reversion works, DON'T create a trend strategy\n\n");

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
		prompt.append("   - DON'T overtrade (fewer high-quality trades beat many poor ones)\n");
		prompt.append("   - DON'T curve-fit to historical data (avoid overfitting)\n");
		prompt.append("   - DON'T ignore transaction costs and slippage\n");
		prompt.append("   - DON'T use too many indicators (creates conflicting signals)\n\n");
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

		prompt.append("   b) DRAWDOWN MANAGEMENT (Prevents Blowups):\n");
		prompt.append("      - After 15% drawdown: reduce ALL position sizes to 50%\n");
		prompt.append("      - After 25% drawdown: reduce to 25% until recovery to 20%\n");
		prompt.append("      - After 3 consecutive losses: cut size to 25% until 1 win\n");
		prompt.append("      - This prevents revenge trading and catastrophic losses\n\n");

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

		prompt.append("Example summaryCard for Feeling Lucky mode:\n");
		prompt.append(String.format("\"This strategy beats buy-and-hold for %s by using proven patterns from 7 years of data. ",
				insights.getSymbol()));
		prompt.append(String.format("RSI with 28/72 thresholds achieved a 68%% win rate historically. "));
		prompt.append(String.format("This %s volatility symbol responds to mean-reversion with %.1f%% stops and %.1f%% targets, ",
				insights.getVolatilityRegime().toLowerCase(), recommendedStopLoss, recommendedTakeProfit));
		prompt.append("generating alpha through strategic entry/exit timing.\"\n");

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
		// Use 3:1 reward-to-risk ratio as default
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
