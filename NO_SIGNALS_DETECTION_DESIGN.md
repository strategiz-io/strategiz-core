# No-Signals Detection & AI-Powered Suggestions

## Problem Statement

**Current Behavior:**
When a strategy backtest produces zero signals (no BUY or SELL actions), the UI displays nothing to the user:

```typescript
// BacktestResults.tsx line 34-36
if (!results || (results.buyCount === 0 && results.sellCount === 0)) {
  return null;  // User sees NOTHING! 😱
}
```

**User Impact:**
- Confusion: "Did it crash? Is it loading?"
- No feedback on WHY no signals were generated
- No guidance on HOW to fix the strategy
- Wasted time trial-and-error

**Solution:**
AI-powered diagnostic system that:
1. Detects when backtest produces no signals
2. Analyzes strategy code and market data
3. Provides actionable suggestions
4. Uses Historical Insights historical insights for context

---

## Architecture Design

### 1. System Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User Clicks "Backtest"                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  ExecuteStrategyController.executeCode()                    │
│  - Validates request                                        │
│  - Delegates to service layer                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  StrategyExecutionService.executeStrategy()                 │
│  - Fetches market data                                      │
│  - Calls gRPC Python executor                               │
│  - Maps response to DTO                                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  Check: Are there signals?                                  │
│  if (buyCount == 0 && sellCount == 0)                       │
└────────┬────────────────────────────────┬───────────────────┘
         │ NO SIGNALS                     │ HAS SIGNALS
         ↓                                ↓
┌────────────────────────────┐   ┌───────────────────────────┐
│  🆕 NoSignalAnalysisBusiness│   │ Return results to frontend│
│  - Analyze strategy code   │   │ Display charts & metrics  │
│  - Analyze market data     │   └───────────────────────────┘
│  - Get Historical Insights insights │
│  - Generate suggestions    │
└────────────┬───────────────┘
             │
             ↓
┌─────────────────────────────────────────────────────────────┐
│  AI Analysis (OpenAI GPT-4o)                                │
│  - Parse strategy logic                                     │
│  - Identify issues (conditions too strict, wrong indicators)│
│  - Provide 3-5 specific suggestions                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│  Frontend: NoSignalsWarning Component                       │
│  - Display friendly message                                 │
│  - Show AI suggestions                                      │
│  - Show market context (Historical Insights)                         │
└─────────────────────────────────────────────────────────────┘
```

### 2. Module Structure (Following Existing Pattern)

```
business/
└── business-nosignal-analysis/
    ├── pom.xml
    └── src/main/java/io/strategiz/business/nosignal/
        ├── model/
        │   ├── NoSignalAnalysisResult.java
        │   ├── Suggestion.java
        │   ├── MarketContext.java
        │   └── StrategyDiagnostic.java
        └── service/
            └── NoSignalAnalysisBusiness.java
```

**Dependencies:**
- `business-ai-chat` (for OpenAI integration and prompts)
- `business-historical-insights` (for Historical Insights data)
- `business-strategy-execution` (for execution models)
- `data-marketdata` (for market data analysis)

---

## Implementation Details

### Backend: NoSignalAnalysisBusiness

**File:** `business/business-nosignal-analysis/src/main/java/io/strategiz/business/nosignal/service/NoSignalAnalysisBusiness.java`

```java
package io.strategiz.business.nosignal.service;

import io.strategiz.business.aichat.service.OpenAIService;
import io.strategiz.business.historicalinsights.service.HistoricalInsightsService;
import io.strategiz.business.historicalinsights.model.SymbolInsights;
import io.strategiz.business.nosignal.model.*;
import io.strategiz.business.strategy.execution.model.ExecutionResult;
import io.strategiz.data.marketdata.entity.MarketDataEntity;
import io.strategiz.data.marketdata.repository.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Business logic for analyzing strategies that produce no signals.
 *
 * <p>Architecture: This is a BUSINESS module, not just a service layer.
 * Following pattern: Controller → Service → Business
 *
 * <p>Responsibilities:
 * 1. Detect no-signal scenarios
 * 2. Analyze strategy code for common issues
 * 3. Analyze market conditions during test period
 * 4. Generate AI-powered suggestions
 * 5. Integrate Historical Insights insights for context
 */
@Service
public class NoSignalAnalysisBusiness {

    private static final Logger log = LoggerFactory.getLogger(NoSignalAnalysisBusiness.class);

    private final OpenAIService openAIService;
    private final HistoricalInsightsService historicalInsightsService;
    private final MarketDataRepository marketDataRepository;

    public NoSignalAnalysisBusiness(
            OpenAIService openAIService,
            HistoricalInsightsService historicalInsightsService,
            MarketDataRepository marketDataRepository) {
        this.openAIService = openAIService;
        this.historicalInsightsService = historicalInsightsService;
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Analyze why a strategy produced no signals.
     *
     * @param strategyCode Python strategy code
     * @param symbol Trading symbol
     * @param timeframe Timeframe (1D, 1H, etc.)
     * @param period Backtest period (2y, 5y, etc.)
     * @param executionResult Execution result with empty signals
     * @return NoSignalAnalysisResult with diagnostics and suggestions
     */
    public NoSignalAnalysisResult analyzeNoSignals(
            String strategyCode,
            String symbol,
            String timeframe,
            String period,
            ExecutionResult executionResult) {

        log.info("🔍 NO SIGNAL ANALYSIS - Analyzing strategy for symbol={}, timeframe={}, period={}",
            symbol, timeframe, period);

        NoSignalAnalysisResult result = new NoSignalAnalysisResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
        result.setPeriod(period);

        try {
            // 1. Get market context (Historical Insights insights)
            MarketContext marketContext = getMarketContext(symbol, period);
            result.setMarketContext(marketContext);

            // 2. Analyze strategy code for issues
            StrategyDiagnostic diagnostic = analyzeStrategyCode(strategyCode);
            result.setDiagnostic(diagnostic);

            // 3. Get AI-powered suggestions
            List<Suggestion> suggestions = generateAISuggestions(
                strategyCode,
                symbol,
                marketContext,
                diagnostic
            );
            result.setSuggestions(suggestions);

            log.info("✅ Analysis complete: {} suggestions generated", suggestions.size());

        } catch (Exception e) {
            log.error("Failed to analyze no-signal scenario", e);
            // Return graceful fallback
            result.setSuggestions(getFallbackSuggestions());
        }

        return result;
    }

    /**
     * Get market context using Historical Insights historical insights.
     */
    private MarketContext getMarketContext(String symbol, String period) {
        log.debug("Fetching Historical Insights insights for {}", symbol);

        try {
            // Use Historical Insights to analyze market conditions
            SymbolInsights insights = historicalInsightsService
                .analyzeSymbolForStrategyGeneration(symbol);

            MarketContext context = new MarketContext();
            context.setVolatilityRegime(insights.getVolatilityRegime());
            context.setTrendDirection(insights.getTrendDirection());
            context.setAverageVolatility(insights.getAverageVolatility());
            context.setRecommendedIndicators(insights.getRecommendedIndicators());
            context.setMarketCondition(insights.getMarketCondition());

            return context;

        } catch (Exception e) {
            log.warn("Failed to fetch Historical Insights insights, using fallback", e);
            return MarketContext.createDefault();
        }
    }

    /**
     * Analyze strategy code for common issues.
     */
    private StrategyDiagnostic analyzeStrategyCode(String code) {
        log.debug("Analyzing strategy code structure");

        StrategyDiagnostic diagnostic = new StrategyDiagnostic();

        // Parse strategy structure
        boolean hasEntryRules = code.contains("return 'BUY'") || code.contains("return \"BUY\"");
        boolean hasExitRules = code.contains("return 'SELL'") || code.contains("return \"SELL\"");
        boolean hasIndicators = code.contains("indicators.") || code.contains("data[");
        boolean hasComplexConditions = code.contains("and") || code.contains("or");

        diagnostic.setHasEntryRules(hasEntryRules);
        diagnostic.setHasExitRules(hasExitRules);
        diagnostic.setHasIndicators(hasIndicators);
        diagnostic.setHasComplexConditions(hasComplexConditions);

        // Count conditions (rough estimate)
        int conditionCount = code.split("if |and |or ").length - 1;
        diagnostic.setConditionCount(conditionCount);

        // Detect specific patterns
        diagnostic.setCrossoverDetected(code.contains("crossed_above") || code.contains("crossed_below"));
        diagnostic.setThresholdComparisons(code.contains(">") || code.contains("<"));

        return diagnostic;
    }

    /**
     * Generate AI-powered suggestions using OpenAI.
     */
    private List<Suggestion> generateAISuggestions(
            String strategyCode,
            String symbol,
            MarketContext marketContext,
            StrategyDiagnostic diagnostic) {

        log.debug("Generating AI suggestions for no-signal scenario");

        // Build prompt for AI
        String prompt = buildNoSignalAnalysisPrompt(
            strategyCode,
            symbol,
            marketContext,
            diagnostic
        );

        try {
            // Call OpenAI
            String aiResponse = openAIService.generateCompletion(
                prompt,
                "gpt-4o",  // Use latest model
                2000,      // Max tokens
                0.3        // Low temperature for focused suggestions
            );

            // Parse AI response into suggestions
            return parseAISuggestions(aiResponse);

        } catch (Exception e) {
            log.error("Failed to generate AI suggestions", e);
            return getFallbackSuggestions();
        }
    }

    /**
     * Build prompt for AI analysis.
     */
    private String buildNoSignalAnalysisPrompt(
            String code,
            String symbol,
            MarketContext context,
            StrategyDiagnostic diagnostic) {

        return String.format("""
            You are a trading strategy expert analyzing why a backtest produced ZERO signals.

            **Strategy Code:**
            ```python
            %s
            ```

            **Symbol:** %s
            **Market Context (Last 7 Years):**
            - Volatility Regime: %s
            - Trend Direction: %s
            - Average Volatility: %.2f%%
            - Market Condition: %s

            **Diagnostic Analysis:**
            - Has Entry Rules: %s
            - Has Exit Rules: %s
            - Has Indicators: %s
            - Condition Count: %d
            - Crossover Logic: %s

            **Task:** Provide 3-5 specific, actionable suggestions to fix this strategy.

            **Common Issues to Check:**
            1. Conditions too strict (e.g., RSI < 10 rarely triggers)
            2. Wrong indicator parameters (e.g., 200-day SMA on 1-month backtest)
            3. Logic errors (impossible conditions)
            4. Misaligned with market regime (trend strategy in ranging market)
            5. Missing crossover logic implementation

            **Output Format (JSON):**
            {
              "suggestions": [
                {
                  "issue": "Brief description of the problem",
                  "recommendation": "Specific fix to apply",
                  "example": "Example code or parameter change",
                  "priority": "high|medium|low"
                }
              ]
            }
            """,
            code,
            symbol,
            context.getVolatilityRegime(),
            context.getTrendDirection(),
            context.getAverageVolatility(),
            context.getMarketCondition(),
            diagnostic.isHasEntryRules(),
            diagnostic.isHasExitRules(),
            diagnostic.isHasIndicators(),
            diagnostic.getConditionCount(),
            diagnostic.isCrossoverDetected()
        );
    }

    /**
     * Parse AI response into Suggestion objects.
     */
    private List<Suggestion> parseAISuggestions(String aiResponse) {
        // TODO: Implement JSON parsing
        // For now, return fallback
        return getFallbackSuggestions();
    }

    /**
     * Fallback suggestions when AI fails.
     */
    private List<Suggestion> getFallbackSuggestions() {
        List<Suggestion> suggestions = new ArrayList<>();

        suggestions.add(new Suggestion(
            "Conditions Too Strict",
            "Your entry conditions may be too restrictive. Try relaxing thresholds (e.g., RSI < 30 instead of < 20).",
            "if indicators['rsi'] < 30:  # Was: < 20",
            "high"
        ));

        suggestions.add(new Suggestion(
            "Indicator Parameters",
            "Check if indicator periods are appropriate for your timeframe. Long periods need longer backtests.",
            "Use 20-day SMA for 1-year backtest, not 200-day",
            "high"
        ));

        suggestions.add(new Suggestion(
            "Market Alignment",
            "Ensure your strategy matches current market conditions. Trend strategies need trending markets.",
            "Add volatility filter or use mean-reversion in ranging markets",
            "medium"
        ));

        return suggestions;
    }
}
```

### Backend: Integration with StrategyExecutionService

**Modify:** `service/service-labs/src/main/java/io/strategiz/service/labs/service/StrategyExecutionService.java`

```java
// Add dependency
private final NoSignalAnalysisBusiness noSignalAnalysisBusiness;

// In executeStrategy() method, after mapping performance:
public ExecuteStrategyResponse executeStrategy(...) {
    // ... existing code ...

    ExecuteStrategyResponse dto = mapToRestDto(grpcResponse, symbol, timeframe);

    // 🆕 NO SIGNAL DETECTION
    boolean hasNoSignals = (dto.getPerformance() == null) ||
                          (dto.getPerformance().getBuyCount() == 0 &&
                           dto.getPerformance().getSellCount() == 0);

    if (hasNoSignals) {
        log.warn("⚠️  NO SIGNALS GENERATED - Triggering AI analysis");

        try {
            NoSignalAnalysisResult analysis = noSignalAnalysisBusiness.analyzeNoSignals(
                code,
                symbol,
                timeframe,
                period,
                grpcResponse  // Contains execution details
            );

            // Add analysis to response
            dto.setNoSignalAnalysis(analysis);

            log.info("✅ No-signal analysis complete: {} suggestions",
                analysis.getSuggestions().size());

        } catch (Exception e) {
            log.error("Failed to analyze no-signal scenario (non-critical)", e);
            // Continue - don't block response if analysis fails
        }
    }

    return dto;
}
```

### Backend: DTOs

**File:** `business/business-nosignal-analysis/src/main/java/io/strategiz/business/nosignal/model/NoSignalAnalysisResult.java`

```java
package io.strategiz.business.nosignal.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoSignalAnalysisResult {
    private String symbol;
    private String timeframe;
    private String period;
    private MarketContext marketContext;
    private StrategyDiagnostic diagnostic;
    private List<Suggestion> suggestions;

    // Getters and setters...
}
```

**File:** `business/business-nosignal-analysis/src/main/java/io/strategiz/business/nosignal/model/Suggestion.java`

```java
package io.strategiz.business.nosignal.model;

public class Suggestion {
    private String issue;          // What's wrong
    private String recommendation; // How to fix
    private String example;        // Code example
    private String priority;       // high, medium, low

    // Constructors, getters, setters...
}
```

**File:** `business/business-nosignal-analysis/src/main/java/io/strategiz/business/nosignal/model/MarketContext.java`

```java
package io.strategiz.business.nosignal.model;

import java.util.List;

public class MarketContext {
    private String volatilityRegime;    // "Low", "Moderate", "High", "Extreme"
    private String trendDirection;      // "Bullish", "Bearish", "Sideways"
    private Double averageVolatility;   // Percentage
    private List<String> recommendedIndicators;
    private String marketCondition;     // Human-readable summary

    public static MarketContext createDefault() {
        MarketContext context = new MarketContext();
        context.setVolatilityRegime("Moderate");
        context.setTrendDirection("Mixed");
        context.setMarketCondition("Normal market conditions");
        return context;
    }

    // Getters and setters...
}
```

**File:** `business/business-nosignal-analysis/src/main/java/io/strategiz/business/nosignal/model/StrategyDiagnostic.java`

```java
package io.strategiz.business.nosignal.model;

public class StrategyDiagnostic {
    private boolean hasEntryRules;
    private boolean hasExitRules;
    private boolean hasIndicators;
    private boolean hasComplexConditions;
    private int conditionCount;
    private boolean crossoverDetected;
    private boolean thresholdComparisons;

    // Getters and setters...
}
```

**Modify:** `service/service-labs/src/main/java/io/strategiz/service/labs/model/ExecuteStrategyResponse.java`

```java
// Add field
@JsonProperty("noSignalAnalysis")
@JsonInclude(JsonInclude.Include.NON_NULL)
private NoSignalAnalysisResult noSignalAnalysis;

// Getter and setter...
```

---

## Frontend Implementation

### Component: NoSignalsWarning

**File:** `apps/web/src/features/labs/components/NoSignalsWarning.tsx`

```typescript
import React, { useState } from 'react';
import {
  Box,
  Typography,
  Alert,
  AlertTitle,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider,
  CircularProgress,
  useTheme,
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Lightbulb as LightbulbIcon,
  TrendingUp as TrendingUpIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { NoSignalAnalysisResult } from '../types/backtestTypes';

export interface NoSignalsWarningProps {
  analysis: NoSignalAnalysisResult;
  isLoading?: boolean;
}

export const NoSignalsWarning: React.FC<NoSignalsWarningProps> = ({
  analysis,
  isLoading = false,
}) => {
  const theme = useTheme();
  const [expanded, setExpanded] = useState<string | false>('suggestions');

  const handleAccordionChange = (panel: string) => (
    event: React.SyntheticEvent,
    isExpanded: boolean
  ) => {
    setExpanded(isExpanded ? panel : false);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, p: 3 }}>
        <CircularProgress size={24} />
        <Typography>Analyzing strategy...</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* Main Alert */}
      <Alert severity="warning" sx={{ mb: 3 }}>
        <AlertTitle sx={{ fontWeight: 600 }}>
          No Signals Generated
        </AlertTitle>
        Your strategy didn't produce any buy or sell signals during the backtest period.
        This usually means the entry conditions were never met.
      </Alert>

      {/* Market Context (Historical Insights Insights) */}
      {analysis.marketContext && (
        <Accordion
          expanded={expanded === 'context'}
          onChange={handleAccordionChange('context')}
          sx={{ mb: 2 }}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <TrendingUpIcon color="info" />
              <Typography variant="h6">
                Market Context ({analysis.symbol})
              </Typography>
            </Box>
          </AccordionSummary>
          <AccordionDetails>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Trend Direction
                </Typography>
                <Chip
                  label={analysis.marketContext.trendDirection}
                  color={
                    analysis.marketContext.trendDirection === 'Bullish'
                      ? 'success'
                      : analysis.marketContext.trendDirection === 'Bearish'
                      ? 'error'
                      : 'default'
                  }
                  size="small"
                  sx={{ mt: 0.5 }}
                />
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Volatility Regime
                </Typography>
                <Chip
                  label={analysis.marketContext.volatilityRegime}
                  size="small"
                  sx={{ mt: 0.5 }}
                />
              </Box>
              {analysis.marketContext.marketCondition && (
                <Typography variant="body2">
                  {analysis.marketContext.marketCondition}
                </Typography>
              )}
            </Box>
          </AccordionDetails>
        </Accordion>
      )}

      {/* AI Suggestions */}
      <Accordion
        expanded={expanded === 'suggestions'}
        onChange={handleAccordionChange('suggestions')}
        sx={{ mb: 2 }}
      >
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <LightbulbIcon color="primary" />
            <Typography variant="h6">
              AI-Powered Suggestions ({analysis.suggestions?.length || 0})
            </Typography>
          </Box>
        </AccordionSummary>
        <AccordionDetails>
          <List>
            {analysis.suggestions?.map((suggestion, index) => (
              <React.Fragment key={index}>
                {index > 0 && <Divider sx={{ my: 2 }} />}
                <ListItem alignItems="flex-start" sx={{ px: 0 }}>
                  <ListItemIcon>
                    {suggestion.priority === 'high' ? (
                      <WarningIcon color="error" />
                    ) : (
                      <CheckCircleIcon color="success" />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <Typography variant="subtitle1" fontWeight={600}>
                          {suggestion.issue}
                        </Typography>
                        <Chip
                          label={suggestion.priority}
                          size="small"
                          color={
                            suggestion.priority === 'high'
                              ? 'error'
                              : suggestion.priority === 'medium'
                              ? 'warning'
                              : 'default'
                          }
                        />
                      </Box>
                    }
                    secondary={
                      <>
                        <Typography variant="body2" paragraph>
                          {suggestion.recommendation}
                        </Typography>
                        {suggestion.example && (
                          <Box
                            sx={{
                              backgroundColor: theme.palette.background.default,
                              border: `1px solid ${theme.palette.divider}`,
                              borderRadius: 1,
                              p: 1.5,
                              fontFamily: 'monospace',
                              fontSize: '0.85rem',
                            }}
                          >
                            {suggestion.example}
                          </Box>
                        )}
                      </>
                    }
                  />
                </ListItem>
              </React.Fragment>
            ))}
          </List>
        </AccordionDetails>
      </Accordion>

      {/* Strategy Diagnostic */}
      {analysis.diagnostic && (
        <Accordion
          expanded={expanded === 'diagnostic'}
          onChange={handleAccordionChange('diagnostic')}
        >
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="h6">Strategy Diagnostic</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <List dense>
              <ListItem>
                <ListItemText
                  primary="Has Entry Rules"
                  secondary={analysis.diagnostic.hasEntryRules ? 'Yes' : 'No ⚠️'}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Has Exit Rules"
                  secondary={analysis.diagnostic.hasExitRules ? 'Yes' : 'No ⚠️'}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Uses Indicators"
                  secondary={analysis.diagnostic.hasIndicators ? 'Yes' : 'No'}
                />
              </ListItem>
              <ListItem>
                <ListItemText
                  primary="Condition Count"
                  secondary={analysis.diagnostic.conditionCount}
                />
              </ListItem>
            </List>
          </AccordionDetails>
        </Accordion>
      )}
    </Box>
  );
};
```

### Modify: BacktestResults.tsx

**File:** `apps/web/src/features/labs/components/BacktestResults.tsx`

```typescript
import { NoSignalsWarning } from './NoSignalsWarning';

export const BacktestResults: React.FC<BacktestResultsProps> = ({
  results,
  isLoading = false,
}) => {
  const theme = useTheme();
  const [activeTab, setActiveTab] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // 🆕 CHANGED: Show no-signals warning instead of returning null
  if (!results || (results.buyCount === 0 && results.sellCount === 0)) {
    // Check if we have AI analysis
    if (results?.noSignalAnalysis) {
      return <NoSignalsWarning analysis={results.noSignalAnalysis} />;
    }

    // Fallback if no analysis available
    return (
      <Alert severity="warning" sx={{ m: 2 }}>
        <AlertTitle>No Signals Generated</AlertTitle>
        Your strategy didn't produce any buy or sell signals during the backtest period.
        Try adjusting your entry conditions or indicator parameters.
      </Alert>
    );
  }

  // ... rest of existing code ...
};
```

### TypeScript Types

**File:** `apps/web/src/features/labs/types/backtestTypes.ts`

```typescript
export interface NoSignalAnalysisResult {
  symbol: string;
  timeframe: string;
  period: string;
  marketContext: MarketContext;
  diagnostic: StrategyDiagnostic;
  suggestions: Suggestion[];
}

export interface MarketContext {
  volatilityRegime: string;
  trendDirection: string;
  averageVolatility: number;
  recommendedIndicators: string[];
  marketCondition: string;
}

export interface StrategyDiagnostic {
  hasEntryRules: boolean;
  hasExitRules: boolean;
  hasIndicators: boolean;
  hasComplexConditions: boolean;
  conditionCount: number;
  crossoverDetected: boolean;
  thresholdComparisons: boolean;
}

export interface Suggestion {
  issue: string;
  recommendation: string;
  example: string;
  priority: 'high' | 'medium' | 'low';
}

// Add to BacktestResults interface
export interface BacktestResults {
  // ... existing fields ...
  noSignalAnalysis?: NoSignalAnalysisResult;
}
```

---

## AI Prompt Template

**File:** `business/business-ai-chat/src/main/java/io/strategiz/business/aichat/prompt/NoSignalAnalysisPrompts.java`

```java
package io.strategiz.business.aichat.prompt;

public class NoSignalAnalysisPrompts {

    public static final String NO_SIGNAL_ANALYSIS_SYSTEM_PROMPT = """
        You are an expert trading strategy analyst specializing in debugging strategies
        that fail to generate signals during backtesting.

        Your task is to:
        1. Identify why the strategy produced no signals
        2. Provide specific, actionable fixes
        3. Explain technical issues in simple terms
        4. Consider market conditions when making suggestions

        Common issues you look for:
        - Overly restrictive conditions (e.g., RSI < 10)
        - Indicator parameters unsuitable for timeframe
        - Logic errors (impossible combinations)
        - Missing crossover implementation
        - Strategy misaligned with market regime
        """;

    public static String buildNoSignalAnalysisPrompt(
            String code,
            String symbol,
            MarketContext context,
            StrategyDiagnostic diagnostic) {

        return String.format("""
            Analyze this trading strategy that generated ZERO signals:

            **Strategy Code:**
            ```python
            %s
            ```

            **Trading Symbol:** %s
            **Timeframe:** %s
            **Backtest Period:** %s

            **Market Context (Historical Analysis):**
            - Volatility: %s (%.2f%% average)
            - Trend: %s
            - Condition: %s
            - Recommended Indicators: %s

            **Code Analysis:**
            - Entry rules present: %s
            - Exit rules present: %s
            - Indicators used: %s
            - Conditions count: %d
            - Crossover logic: %s

            **Required Output (JSON):**
            {
              "suggestions": [
                {
                  "issue": "Short description of problem",
                  "recommendation": "Specific solution",
                  "example": "Code example showing the fix",
                  "priority": "high|medium|low"
                }
              ]
            }

            Provide 3-5 suggestions ordered by priority.
            """,
            code,
            symbol,
            context.getTimeframe(),
            context.getPeriod(),
            context.getVolatilityRegime(),
            context.getAverageVolatility(),
            context.getTrendDirection(),
            context.getMarketCondition(),
            context.getRecommendedIndicators(),
            diagnostic.isHasEntryRules() ? "Yes" : "No (CRITICAL!)",
            diagnostic.isHasExitRules() ? "Yes" : "No (CRITICAL!)",
            diagnostic.isHasIndicators() ? "Yes" : "No",
            diagnostic.getConditionCount(),
            diagnostic.isCrossoverDetected() ? "Yes" : "No"
        );
    }
}
```

---

## Testing Strategy

### Unit Tests

**File:** `business/business-nosignal-analysis/src/test/java/io/strategiz/business/nosignal/NoSignalAnalysisBusinessTest.java`

```java
@SpringBootTest
class NoSignalAnalysisBusinessTest {

    @Autowired
    private NoSignalAnalysisBusiness business;

    @Test
    void testNoSignalAnalysis_WithStrictConditions() {
        String code = """
            def generate_signal(data, indicators):
                rsi = indicators['rsi']
                if rsi < 5:  # Too strict!
                    return 'BUY'
                return None
            """;

        NoSignalAnalysisResult result = business.analyzeNoSignals(
            code, "AAPL", "1D", "2y", null
        );

        assertNotNull(result);
        assertFalse(result.getSuggestions().isEmpty());
        assertTrue(result.getSuggestions().get(0).getIssue()
            .contains("strict"));
    }

    @Test
    void testMarketContextIntegration() {
        NoSignalAnalysisResult result = business.analyzeNoSignals(
            "test code", "AAPL", "1D", "2y", null
        );

        assertNotNull(result.getMarketContext());
        assertNotNull(result.getMarketContext().getVolatilityRegime());
    }
}
```

### Integration Test

**File:** `service/service-labs/src/test/java/io/strategiz/service/labs/NoSignalDetectionIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
class NoSignalDetectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testExecuteStrategy_NoSignals_ReturnsAnalysis() throws Exception {
        String requestBody = """
            {
              "code": "def generate_signal(data, indicators): return None",
              "language": "python",
              "symbol": "AAPL",
              "timeframe": "1D",
              "period": "2y"
            }
            """;

        mockMvc.perform(post("/v1/strategies/execute-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .header("Authorization", "Bearer " + getTestToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.noSignalAnalysis").exists())
            .andExpect(jsonPath("$.noSignalAnalysis.suggestions").isArray())
            .andExpect(jsonPath("$.noSignalAnalysis.marketContext").exists());
    }
}
```

---

## Implementation Plan

### Phase 1: Backend Foundation (Week 1)

**Day 1-2: Create Business Module**
- [ ] Create `business-nosignal-analysis` module
- [ ] Add POM dependencies
- [ ] Create model classes (NoSignalAnalysisResult, Suggestion, MarketContext, StrategyDiagnostic)
- [ ] Write unit tests for models

**Day 3-4: Implement NoSignalAnalysisBusiness**
- [ ] Implement `analyzeNoSignals()` method
- [ ] Implement `getMarketContext()` (Historical Insights integration)
- [ ] Implement `analyzeStrategyCode()` (code parsing)
- [ ] Write unit tests

**Day 5: AI Integration**
- [ ] Create NoSignalAnalysisPrompts class
- [ ] Implement `generateAISuggestions()` method
- [ ] Add JSON parsing for AI response
- [ ] Test with real OpenAI calls

**Day 6: Service Integration**
- [ ] Modify StrategyExecutionService
- [ ] Add no-signal detection logic
- [ ] Add noSignalAnalysis field to ExecuteStrategyResponse
- [ ] Write integration tests

**Day 7: Testing & Refinement**
- [ ] End-to-end testing
- [ ] Performance testing (ensure < 50ms overhead)
- [ ] Error handling and fallbacks

### Phase 2: Frontend Implementation (Week 1)

**Day 8-9: Create NoSignalsWarning Component**
- [ ] Build component structure
- [ ] Add market context display
- [ ] Add suggestions list with accordion
- [ ] Add strategy diagnostic view
- [ ] Style with MUI theme

**Day 10: Integrate with BacktestResults**
- [ ] Modify BacktestResults.tsx to use NoSignalsWarning
- [ ] Add TypeScript types
- [ ] Test rendering with mock data

**Day 11: Polish & UX**
- [ ] Add loading states
- [ ] Add animations
- [ ] Add "Apply Fix" button (future feature)
- [ ] Mobile responsive design

**Day 12: Testing & Deployment**
- [ ] Manual testing with various no-signal scenarios
- [ ] Cross-browser testing
- [ ] Deploy to staging
- [ ] User acceptance testing

---

## Performance Considerations

**Backend:**
- Historical Insights insights: Cached (< 10ms)
- Code analysis: Regex parsing (< 5ms)
- AI call: Async, timeout 5s
- Total overhead: < 50ms for non-AI path

**Frontend:**
- Lazy load NoSignalsWarning component
- Memoize suggestion rendering
- Accordion for progressive disclosure
- Target: < 100ms render time

---

## Success Metrics

**User Experience:**
- 0% users seeing blank screen after no-signal backtest
- 100% users seeing actionable suggestions
- < 5 second analysis time (95th percentile)

**Quality:**
- 80% of suggestions rated helpful by users
- 50% of users apply at least one suggestion
- 30% reduction in support tickets about "strategy not working"

**Technical:**
- < 50ms backend overhead (non-AI path)
- < 5s AI analysis time (95th percentile)
- 99.9% uptime (graceful fallback if AI fails)

---

## Future Enhancements (Post-MVP)

1. **Quick Fix Buttons:** One-click apply suggestions
2. **Historical Examples:** Show similar strategies that worked
3. **Parameter Optimizer:** AI suggests optimal parameter ranges
4. **A/B Testing:** Compare before/after applying suggestions
5. **Learning System:** Track which suggestions users apply most

---

## Questions for Discussion

1. Should AI analysis run **synchronously** (user waits 2-5s) or **asynchronously** (polling/websocket)?
2. Should we cache analysis results for identical code+symbol+period combinations?
3. Should "Apply Fix" buttons automatically edit the code, or just copy to clipboard?
4. How do we handle rate limiting for OpenAI calls? (Per-user quotas?)
5. Should we show partial results if AI times out? (Show market context + diagnostic, skip suggestions)

---

## Dependencies

**Required:**
- `business-ai-chat` (OpenAI integration)
- `business-historical-insights` (Historical Insights)
- `business-strategy-execution` (execution models)
- `data-marketdata` (market data queries)

**Optional:**
- `business-fundamentals` (for fundamental-based strategies)
- `business-ml-predictions` (if we add ML-powered suggestions)

---

## Deployment Notes

**Backend:**
1. Deploy `business-nosignal-analysis` module
2. Update `service-labs` with new dependency
3. Rebuild Docker image
4. Deploy to Cloud Run
5. Monitor logs for AI call performance

**Frontend:**
1. Build React components
2. Add TypeScript types
3. Deploy to Firebase Hosting
4. A/B test with 10% of users first

**Monitoring:**
- Track AI analysis duration (target: < 5s p95)
- Track suggestion quality (user ratings)
- Track error rates (AI failures should not block response)
- Track cost (OpenAI API usage)

---

## Cost Analysis

**OpenAI API Costs:**
- Model: GPT-4o ($2.50 per 1M input tokens)
- Prompt size: ~1500 tokens
- Response size: ~500 tokens
- Cost per analysis: ~$0.005 (half a cent)
- Monthly (1000 users, 5 analyses each): $25/month

**Infrastructure:**
- Minimal - reuses existing services
- No additional Cloud Run instances needed
- No additional database costs

**Total Additional Cost:** < $50/month
