package io.strategiz.service.agents.context;

import io.strategiz.business.aichat.context.PortfolioContextProvider;
import io.strategiz.client.finnhub.FinnhubNewsClient;
import io.strategiz.client.finnhub.dto.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Provides strategy optimization context for the Optimizer Agent
 * Includes optimization frameworks, guidelines, and market conditions
 */
@Component
public class StrategyOptimizationContextProvider {

    private static final Logger log = LoggerFactory.getLogger(StrategyOptimizationContextProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PortfolioContextProvider portfolioContextProvider;
    private final FinnhubNewsClient newsClient;

    public StrategyOptimizationContextProvider(
            PortfolioContextProvider portfolioContextProvider,
            FinnhubNewsClient newsClient) {
        this.portfolioContextProvider = portfolioContextProvider;
        this.newsClient = newsClient;
    }

    /**
     * Build comprehensive optimization context for AI injection
     *
     * @param userId User ID for portfolio context
     * @param strategyCode Strategy code to optimize (if provided)
     * @param strategyType Type of strategy (momentum, mean-reversion, etc.)
     * @return Formatted context string for AI consumption
     */
    public String buildOptimizationContext(String userId, String strategyCode, String strategyType) {
        StringBuilder context = new StringBuilder();

        // Add timestamp
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        // Portfolio context
        appendPortfolioContext(context, userId);

        // Strategy being optimized
        if (strategyCode != null && !strategyCode.isBlank()) {
            appendStrategyCode(context, strategyCode);
        }

        // Optimization frameworks
        appendOptimizationFramework(context, strategyType);

        // Current market conditions for optimization decisions
        appendMarketConditionsContext(context);

        return context.toString();
    }

    /**
     * Build context for general strategy analysis
     */
    public String buildStrategyAnalysisContext(String userId) {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendPortfolioContext(context, userId);
        appendStrategyTypesReference(context);
        appendOptimizationMetrics(context);

        return context.toString();
    }

    /**
     * Build context focused on a specific strategy type
     */
    public String buildStrategyTypeContext(String strategyType) {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendOptimizationFramework(context, strategyType);
        appendStrategyTypeDetails(context, strategyType);

        return context.toString();
    }

    private void appendPortfolioContext(StringBuilder context, String userId) {
        try {
            Map<String, Object> portfolioContext = portfolioContextProvider.getPortfolioContext(userId);
            if (portfolioContext != null && !portfolioContext.isEmpty()) {
                context.append("USER PORTFOLIO CONTEXT:\n");
                context.append("-".repeat(50)).append("\n");
                portfolioContext.forEach((key, value) ->
                        context.append("  • ").append(key).append(": ").append(value).append("\n"));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch portfolio context: {}", e.getMessage());
        }
    }

    private void appendStrategyCode(StringBuilder context, String strategyCode) {
        context.append("STRATEGY CODE TO OPTIMIZE:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("```\n");
        context.append(strategyCode);
        context.append("\n```\n\n");
    }

    private void appendOptimizationFramework(StringBuilder context, String strategyType) {
        context.append("OPTIMIZATION FRAMEWORK:\n");
        context.append("-".repeat(50)).append("\n\n");

        context.append("Key Optimization Dimensions:\n");
        context.append("1. ENTRY SIGNALS:\n");
        context.append("   • Signal sensitivity (too early vs too late)\n");
        context.append("   • Confirmation requirements\n");
        context.append("   • Time filters (market hours, day of week)\n\n");

        context.append("2. EXIT SIGNALS:\n");
        context.append("   • Take profit levels (fixed vs trailing)\n");
        context.append("   • Stop loss placement (ATR-based, percentage, support levels)\n");
        context.append("   • Time-based exits (max holding period)\n\n");

        context.append("3. POSITION SIZING:\n");
        context.append("   • Risk per trade (typically 1-2% of capital)\n");
        context.append("   • Position scaling (all-in vs pyramiding)\n");
        context.append("   • Correlation-adjusted sizing\n\n");

        context.append("4. RISK MANAGEMENT:\n");
        context.append("   • Maximum drawdown limits\n");
        context.append("   • Daily/weekly loss limits\n");
        context.append("   • Correlation with portfolio\n\n");

        if (strategyType != null) {
            appendStrategyTypeDetails(context, strategyType);
        }
    }

    private void appendStrategyTypeDetails(StringBuilder context, String strategyType) {
        String typeLower = strategyType != null ? strategyType.toLowerCase() : "";

        context.append("STRATEGY-SPECIFIC GUIDANCE:\n");
        context.append("-".repeat(50)).append("\n");

        if (typeLower.contains("momentum")) {
            context.append("\nMOMENTUM STRATEGY OPTIMIZATION:\n");
            context.append("• Lookback period: Test 10, 20, 50, 100, 200 days\n");
            context.append("• Momentum measure: Rate of change, RSI, MACD crossover\n");
            context.append("• Trend filter: Only long when above 200 SMA\n");
            context.append("• Volatility filter: Avoid entries in high VIX environments\n");
            context.append("• Sector momentum: Consider relative strength ranking\n\n");
        } else if (typeLower.contains("mean") || typeLower.contains("reversion")) {
            context.append("\nMEAN REVERSION STRATEGY OPTIMIZATION:\n");
            context.append("• Oversold threshold: RSI < 30, 2+ std devs from mean\n");
            context.append("• Confirmation: Wait for reversal candle pattern\n");
            context.append("• Holding period: Typically short (1-5 days)\n");
            context.append("• Works best: Range-bound markets, high volume names\n");
            context.append("• Avoid: Strong trend environments\n\n");
        } else if (typeLower.contains("breakout")) {
            context.append("\nBREAKOUT STRATEGY OPTIMIZATION:\n");
            context.append("• Consolidation period: 20-50 bars minimum\n");
            context.append("• Volume confirmation: 1.5x+ average volume on breakout\n");
            context.append("• Retest entry: Often provides better risk/reward\n");
            context.append("• False breakout filter: Close above/below level\n");
            context.append("• Target: Measured move based on consolidation range\n\n");
        } else if (typeLower.contains("pairs") || typeLower.contains("arbitrage")) {
            context.append("\nPAIRS/ARBITRAGE STRATEGY OPTIMIZATION:\n");
            context.append("• Cointegration test: Ensure statistical relationship\n");
            context.append("• Entry z-score: Typically 2+ standard deviations\n");
            context.append("• Position sizing: Dollar-neutral or beta-neutral\n");
            context.append("• Exit: Reversion to mean (z-score approaches 0)\n");
            context.append("• Stop: Divergence exceeds 3+ std devs\n\n");
        } else {
            context.append("\nGENERAL OPTIMIZATION GUIDELINES:\n");
            context.append("• Walk-forward testing: Validate out-of-sample\n");
            context.append("• Parameter sensitivity: Avoid over-fitting\n");
            context.append("• Transaction costs: Include realistic slippage\n");
            context.append("• Market regime: Test across different conditions\n\n");
        }
    }

    private void appendOptimizationMetrics(StringBuilder context) {
        context.append("KEY PERFORMANCE METRICS:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("• Win Rate: Percentage of profitable trades\n");
        context.append("• Profit Factor: Gross profit / Gross loss (>1.5 is good)\n");
        context.append("• Sharpe Ratio: Risk-adjusted returns (>1.0 is acceptable, >2.0 is good)\n");
        context.append("• Sortino Ratio: Downside risk-adjusted returns\n");
        context.append("• Max Drawdown: Peak-to-trough decline (should match risk tolerance)\n");
        context.append("• Recovery Factor: Net profit / Max drawdown\n");
        context.append("• Average Trade: Expectancy per trade\n");
        context.append("• Trade Count: Statistical significance (>30 trades)\n");
        context.append("\n");
    }

    private void appendStrategyTypesReference(StringBuilder context) {
        context.append("STRATEGY TYPES REFERENCE:\n");
        context.append("-".repeat(50)).append("\n\n");

        context.append("| Type | Best Market | Key Indicators | Timeframe |\n");
        context.append("|------|-------------|----------------|----------|\n");
        context.append("| Momentum | Trending | RSI, MACD, ROC | Days-Weeks |\n");
        context.append("| Mean Reversion | Range-bound | Bollinger, RSI | Hours-Days |\n");
        context.append("| Breakout | Consolidation | Volume, ATR | Days |\n");
        context.append("| Trend Following | Strong trends | MA crossovers | Weeks-Months |\n");
        context.append("| Pairs Trading | Correlated assets | Correlation, Z-score | Days-Weeks |\n");
        context.append("| Scalping | High volume | L2, Tape reading | Minutes |\n");
        context.append("\n");
    }

    private void appendMarketConditionsContext(StringBuilder context) {
        if (!newsClient.isConfigured()) {
            return;
        }

        try {
            List<NewsArticle> marketNews = newsClient.getMarketNews("general");
            if (!marketNews.isEmpty() && marketNews.size() >= 3) {
                context.append("CURRENT MARKET NEWS (for context):\n");
                context.append("-".repeat(50)).append("\n");
                context.append(newsClient.formatNewsForContext(marketNews, 5));
                context.append("\nConsider market conditions when optimizing parameters.\n\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch market news for optimization context: {}", e.getMessage());
        }
    }
}
