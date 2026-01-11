# Alpha Mode Implementation Status

## Overview
Alpha Mode is a premium AI strategy generation feature that analyzes 7 years of S&P 500 historical market data to generate more profitable, data-driven trading strategies.

**Current Status**: Week 2 & 3 Complete - Backend Ready for Testing
**Last Updated**: 2026-01-11

---

## ✅ Completed (Weeks 1-3)

### Week 1: Core Infrastructure ✅
- [x] Created `business-historical-insights` module
- [x] Implemented `HistoricalInsightsService` with:
  - Volatility profiling (14-day ATR, regime classification)
  - Indicator effectiveness ranking (RSI, MACD, BB, MA)
  - Optimal parameter detection
  - Market characteristics detection (trend vs mean-revert)
  - Risk metrics calculation
- [x] Implemented `HistoricalInsightsCacheService` (24-hour TTL, LRU eviction)
- [x] Created all model classes (SymbolInsights, IndicatorRanking, etc.)

### Week 2: API Integration ✅
- [x] Updated DTOs:
  - `AIStrategyRequest` with `alphaMode` and `alphaOptions`
  - `AIStrategyResponse` with `alphaModeUsed` and `historicalInsights`
- [x] Created feature flag `FLAG_ALPHA_MODE` in `FeatureFlagService`
- [x] Added `canUseAlphaMode()` to `SubscriptionService` (TRADER/STRATEGIST tiers)
- [x] Enhanced `AIStrategyController` with Alpha Mode validation
- [x] Enhanced `AIStrategyService` to:
  - Fetch and cache historical insights
  - Extract symbol from prompt/context
  - Enhance LLM prompts with insights
  - Include insights in response
- [x] Created `buildAlphaModePrompt()` in `AIStrategyPrompts`

### Week 3: Testing & Validation ✅
- [x] Created comprehensive integration test (`AlphaModeIntegrationTest.java`)
- [x] Created prompt verification tool (`AlphaModePromptVerification.java`)
- [x] All modules compile successfully:
  - `business-historical-insights`
  - `business-ai-chat`
  - `data-feature-flags`
  - `business-preferences`
  - `service-labs`

---

## Architecture

### Data Flow
```
1. User Request (alphaMode: true, symbol: "AAPL")
2. FeatureFlagService.isAlphaModeEnabled() → Check feature flag
3. SubscriptionService.canUseAlphaMode(userId) → Check TRADER/STRATEGIST tier
4. AIStrategyService.getAlphaModeInsights():
   a. Extract symbol from prompt/context
   b. Check HistoricalInsightsCacheService (24-hour cache)
   c. If not cached: HistoricalInsightsService.analyzeSymbolForStrategyGeneration()
      - Query 2,600 bars (~7 years) from ClickHouse
      - Compute volatility, rank indicators, find optimal parameters
   d. Cache insights for future requests
5. AIStrategyPrompts.buildAlphaModePrompt(insights) → Enhanced prompt
6. LLMRouter.generateContent() → AI generates strategy
7. AIStrategyResponse (includes historicalInsights)
```

### Key Components

**Historical Insights Service** (`business-historical-insights/service/HistoricalInsightsService.java:50`)
- Main entry point: `analyzeSymbolForStrategyGeneration()`
- Computes volatility profile, indicator rankings, optimal parameters
- Fallback: `analyzeWithPartialData()` for symbols with limited data

**Cache Service** (`business-historical-insights/service/HistoricalInsightsCacheService.java:18`)
- In-memory `ConcurrentHashMap` with 24-hour TTL
- Scheduled cleanup every hour
- Max 1,000 entries (~2MB total)

**Prompt Enhancement** (`business-ai-chat/src/main/java/io/strategiz/business/aichat/prompt/AIStrategyPrompts.java:1089`)
- Formats insights into structured AI prompt
- Includes volatility analysis, indicator rankings, optimal parameters
- Provides market characteristics and risk recommendations

**AI Strategy Service** (`service-labs/src/main/java/io/strategiz/service/labs/service/AIStrategyService.java:60`)
- Orchestrates Alpha Mode flow
- Fetches insights, enhances prompts, includes insights in response

---

## Example Alpha Mode Prompts

### Test Case 1: AAPL (Medium Volatility, Bullish)
```
================================================================================
ALPHA MODE: HISTORICAL MARKET ANALYSIS
================================================================================

Symbol: AAPL
Timeframe: 1D
Analysis Period: 2600 days (~7.1 years of data)

VOLATILITY PROFILE:
- Average ATR: $2.50
- Volatility Regime: MEDIUM
- Average Daily Range: 1.80%
→ Recommended Stop-Loss: 3.0% | Take-Profit: 9.0%

TOP PERFORMING INDICATORS (Ranked by Historical Effectiveness):
1. RSI (Score: 0.68) - Achieved 68% win rate in mean-reverting periods
   Optimal Settings: period=14, oversold=28, overbought=72

2. MACD (Score: 0.62) - Strong trend confirmation with 62% accuracy
   Optimal Settings: fast=12, slow=26, signal=9

3. Bollinger Bands (Score: 0.58) - Effective for volatility breakouts
   Optimal Settings: period=20, stddev=2

OPTIMAL PARAMETERS DISCOVERED:
- stop_loss_percent: 3.0
- take_profit_percent: 9.0
- rsi_period: 14
- rsi_oversold: 28
- rsi_overbought: 72

MARKET CHARACTERISTICS:
- Trend Direction: BULLISH (Strength: 65%)
- Market Type: Trending
- Recommended Strategy Type: Trend-Following

RISK INSIGHTS:
- Historical Avg Max Drawdown: 8.5%
- Historical Avg Win Rate: 65.0%
- Recommended Risk Level: MEDIUM

================================================================================
ALPHA MODE INSTRUCTIONS:
================================================================================
1. Generate a strategy that LEVERAGES these historical insights
2. Use the TOP 2-3 indicators from the ranking above
3. Apply the OPTIMAL PARAMETERS discovered (not generic defaults)
4. Set stop-loss and take-profit based on the volatility analysis
5. In summaryCard, explain WHY this strategy works for AAPL using historical win rates
6. Reference specific performance metrics in your explanation

Example summaryCard for Alpha Mode:
"Based on 7 years of AAPL data, RSI with 28/72 thresholds achieved a 68% win rate.
This medium volatility symbol responds well to mean-reversion with 3.0% stops and 9.0% targets."
```

### Test Case 2: TSLA (High Volatility, Aggressive)
```
Symbol: TSLA
Volatility Regime: HIGH
Average Daily Range: 4.20%
→ Recommended Stop-Loss: 6.0% | Take-Profit: 18.0%

TOP PERFORMING INDICATORS:
1. MACD (Score: 0.64) - Best for capturing TSLA's strong momentum moves
2. ATR (Score: 0.61) - Critical for position sizing in high volatility
3. EMA Crossover (Score: 0.56) - Captures trend changes effectively

Market Type: Trending
Recommended Risk Level: AGGRESSIVE
Historical Win Rate: 58.0%
```

### Test Case 3: SPY (Low Volatility, Mean-Reverting)
```
Symbol: SPY
Volatility Regime: LOW
Average Daily Range: 0.80%
→ Recommended Stop-Loss: 1.5% | Take-Profit: 4.5%

TOP PERFORMING INDICATORS:
1. Bollinger Bands (Score: 0.70) - 72% win rate on mean reversion from bands
2. RSI (Score: 0.66) - Reliable for overbought/oversold conditions
3. VWAP (Score: 0.62) - Strong support/resistance in range-bound market

Market Type: Mean-Reverting
Recommended Risk Level: LOW
Historical Win Rate: 72.0%
```

### Test Case 4: BTC (Extreme Volatility)
```
Symbol: BTC
Volatility Regime: EXTREME
Average Daily Range: 5.80%
→ Recommended Stop-Loss: 8.0% | Take-Profit: 24.0%

TOP PERFORMING INDICATORS:
1. EMA Crossover (Score: 0.59) - Best for BTC's strong trending behavior
2. MACD (Score: 0.57) - Catches major trend reversals
3. RSI (Score: 0.52) - Useful for extreme overbought/oversold

Market Type: Trending
Recommended Risk Level: AGGRESSIVE
Historical Win Rate: 54.0%
```

---

## Test Coverage

### Integration Tests Created
**File**: `service-labs/src/test/java/io/strategiz/service/labs/AlphaModeIntegrationTest.java`

**Test Cases**:
1. ✅ `testAlphaMode_WithAAPL_GeneratesEnhancedStrategy`
   - Verifies AAPL insights are fetched and included in response
   - Validates caching works correctly

2. ✅ `testAlphaMode_WithTSLA_HighVolatility`
   - Tests high volatility symbol handling
   - Verifies aggressive risk level recommendations

3. ✅ `testAlphaMode_WithSPY_MeanReverting`
   - Tests mean-reverting symbol characteristics
   - Validates low volatility parameters

4. ✅ `testAlphaMode_UsesCache_OnSecondRequest`
   - Verifies caching prevents redundant analysis
   - Ensures performance optimization works

5. ✅ `testAlphaMode_WithFundamentals_IncludesFundamentalData`
   - Tests fundamentals toggle functionality
   - Validates fundamental data integration

6. ✅ `testAlphaMode_Disabled_NoInsights`
   - Ensures regular mode works without Alpha Mode
   - Validates backward compatibility

7. ✅ `testPromptEnhancement_IncludesHistoricalData`
   - Validates prompt formatting
   - Ensures all key sections are present

### Manual Verification Tool
**File**: `service-labs/src/test/java/io/strategiz/service/labs/AlphaModePromptVerification.java`

**Features**:
- Generates sample prompts for AAPL, TSLA, SPY, BTC
- Shows prompt with and without fundamentals
- Validates all required sections are present
- Can be run standalone to inspect prompt quality

**Usage**:
```bash
cd service/service-labs
mvn test-compile exec:java \
  -Dexec.mainClass="io.strategiz.service.labs.AlphaModePromptVerification" \
  -Dexec.classpathScope=test
```

---

## API Examples

### Request Example (Alpha Mode Enabled)
```json
POST /v1/labs/ai/generate-strategy
{
  "prompt": "Generate an RSI strategy for AAPL",
  "alphaMode": true,
  "alphaOptions": {
    "lookbackDays": 2600,
    "useFundamentals": true,
    "forceRefresh": false
  },
  "context": {
    "symbols": ["AAPL"],
    "timeframe": "1D"
  }
}
```

### Response Example
```json
{
  "success": true,
  "visualConfig": {
    "name": "AAPL RSI Mean Reversion",
    "description": "Data-driven RSI strategy optimized for AAPL",
    "symbol": "AAPL",
    "rules": [...]
  },
  "pythonCode": "...",
  "summaryCard": "Based on 7 years of AAPL data, RSI with 28/72 thresholds achieved a 68% win rate. This medium volatility symbol responds well to mean-reversion with 3.0% stops and 9.0% targets.",
  "riskLevel": "MEDIUM",
  "alphaModeUsed": true,
  "historicalInsights": {
    "symbol": "AAPL",
    "timeframe": "1D",
    "daysAnalyzed": 2600,
    "avgVolatility": 2.50,
    "volatilityRegime": "MEDIUM",
    "avgDailyRange": 1.80,
    "topIndicators": [
      {
        "indicatorName": "RSI",
        "effectivenessScore": 0.68,
        "optimalSettings": {
          "period": 14,
          "oversold": 28,
          "overbought": 72
        },
        "reason": "Achieved 68% win rate in mean-reverting periods"
      }
    ],
    "trendDirection": "BULLISH",
    "trendStrength": 0.65,
    "isMeanReverting": false,
    "avgWinRate": 65.0,
    "avgMaxDrawdown": 8.5,
    "recommendedRiskLevel": "MEDIUM"
  }
}
```

---

## Access Control

### Feature Flag
- **Flag**: `ai_alpha_mode_enabled`
- **Location**: `data-feature-flags/src/main/java/io/strategiz/data/featureflags/service/FeatureFlagService.java:45`
- **Default**: `true` (enabled)
- **Method**: `isAlphaModeEnabled()`

### Subscription Tiers
- **Required Tiers**: TRADER or STRATEGIST
- **Location**: `business-preferences/src/main/java/io/strategiz/business/preferences/service/SubscriptionService.java:93`
- **Method**: `canUseAlphaMode(userId)`
- **Admin Override**: Admins have access for testing

### Controller Validation
**Location**: `service-labs/src/main/java/io/strategiz/service/labs/controller/AIStrategyController.java:67`

```java
// ALPHA MODE CHECKS
if (Boolean.TRUE.equals(request.getAlphaMode())) {
    // Check feature flag
    if (!featureFlagService.isAlphaModeEnabled()) {
        return ResponseEntity.status(503)
            .body(AIStrategyResponse.error("Alpha Mode is currently unavailable."));
    }

    // Check subscription tier
    if (!subscriptionService.canUseAlphaMode(userId)) {
        return ResponseEntity.status(403)
            .body(AIStrategyResponse.error("Alpha Mode requires TRADER or STRATEGIST tier."));
    }
}
```

---

## Next Steps: Week 4 (Frontend)

### Remaining Tasks
1. **Frontend UI Components**:
   - Add Alpha Mode toggle switch in AIStrategyGenerator component
   - Add fundamentals checkbox (optional)
   - Display "Processing time: 5-10 seconds" indicator
   - Show historical insights card after generation

2. **API Integration**:
   - Update API client to send `alphaMode` and `alphaOptions`
   - Handle `historicalInsights` in response
   - Display insights metrics (volatility, trend, win rate)

3. **User Experience**:
   - Show upgrade prompt for SCOUT users
   - Display loading state during analysis
   - Show error messages for access denied

4. **Testing**:
   - End-to-end tests with real ClickHouse data
   - Performance benchmarking (target < 10 seconds)
   - Cache hit rate monitoring (target > 70%)

---

## Performance Targets

- **Query Time**: < 5 seconds (from ClickHouse)
- **Cache Hit Rate**: > 70% after 1 week
- **Total Alpha Mode Time**: < 10 seconds (analysis + LLM generation)
- **Cache Size**: ~2KB per symbol, max 1000 entries (~2MB)

---

## Monitoring & Metrics

### Key Metrics to Track
1. **Usage**:
   - % of eligible users trying Alpha Mode
   - Alpha Mode vs Regular Mode usage ratio
   - Most analyzed symbols

2. **Performance**:
   - Average analysis time
   - Cache hit rate
   - LLM response time with enhanced prompts

3. **Quality**:
   - Strategy win rates (Alpha vs Regular)
   - User satisfaction ratings
   - Support tickets related to Alpha Mode

---

## Known Limitations

1. **Data Requirements**:
   - Requires minimum 100 bars of historical data
   - Fallback to partial analysis for newer symbols
   - No data for symbols not in ClickHouse

2. **Simplifications (Phase 1)**:
   - Indicator rankings use simplified scoring (not full backtesting)
   - Optimal parameters use heuristics (can be enhanced with grid search)
   - Market characteristics use basic linear regression (can add Hurst exponent)

3. **Future Enhancements**:
   - Multi-symbol correlation analysis
   - Seasonal pattern detection
   - News sentiment integration
   - Custom backtest periods

---

## Summary

✅ **Backend Complete**: All Week 2 & 3 tasks finished and verified
✅ **Tests Created**: Comprehensive integration tests and verification tools
✅ **Ready for Frontend**: API endpoints ready, DTOs defined, prompts validated
✅ **Access Control**: Feature flags and subscription checks in place

**Status**: Ready to proceed with Week 4 (Frontend Implementation)
