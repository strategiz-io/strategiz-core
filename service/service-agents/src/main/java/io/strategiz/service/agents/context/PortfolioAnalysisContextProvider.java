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
 * Provides enhanced portfolio analysis context for the Portfolio Agent
 * Combines portfolio data with benchmarks and market context
 */
@Component
public class PortfolioAnalysisContextProvider {

    private static final Logger log = LoggerFactory.getLogger(PortfolioAnalysisContextProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PortfolioContextProvider portfolioContextProvider;
    private final FinnhubNewsClient newsClient;

    public PortfolioAnalysisContextProvider(
            PortfolioContextProvider portfolioContextProvider,
            FinnhubNewsClient newsClient) {
        this.portfolioContextProvider = portfolioContextProvider;
        this.newsClient = newsClient;
    }

    /**
     * Build comprehensive portfolio analysis context
     */
    public String buildPortfolioAnalysisContext(String userId) {
        StringBuilder context = new StringBuilder();

        // Add timestamp
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        // Portfolio data
        appendPortfolioData(context, userId);

        // Benchmark reference
        appendBenchmarkReference(context);

        // Risk analysis framework
        appendRiskFramework(context);

        // Relevant market news for holdings
        appendHoldingsNews(context, userId);

        return context.toString();
    }

    /**
     * Build context focused on risk analysis
     */
    public String buildRiskAnalysisContext(String userId) {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendPortfolioData(context, userId);
        appendDetailedRiskFramework(context);

        return context.toString();
    }

    /**
     * Build context focused on rebalancing
     */
    public String buildRebalancingContext(String userId) {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendPortfolioData(context, userId);
        appendRebalancingFramework(context);

        return context.toString();
    }

    private void appendPortfolioData(StringBuilder context, String userId) {
        try {
            Map<String, Object> portfolioData = portfolioContextProvider.getPortfolioContext(userId);

            if (portfolioData == null || portfolioData.isEmpty() ||
                    !Boolean.TRUE.equals(portfolioData.get("hasPortfolio"))) {
                context.append("PORTFOLIO STATUS: No portfolio data available\n");
                context.append("User may need to connect their brokerage accounts first.\n\n");
                return;
            }

            context.append("USER PORTFOLIO:\n");
            context.append("-".repeat(50)).append("\n");

            // Summary metrics
            context.append("Total Value: ").append(portfolioData.get("totalValue")).append("\n");
            context.append("Day Change: ").append(portfolioData.get("dayChange"));
            context.append(" (").append(portfolioData.get("dayChangePercent")).append(")\n");
            context.append("Connected Providers: ").append(portfolioData.get("connectedProviders")).append("\n\n");

            // Allocation
            Object allocation = portfolioData.get("allocation");
            if (allocation instanceof Map<?, ?> allocMap && !allocMap.isEmpty()) {
                context.append("ASSET ALLOCATION:\n");
                allocMap.forEach((k, v) -> context.append("  • ").append(k).append(": ").append(v).append("\n"));
                context.append("\n");
            }

            // Top holdings
            Object holdings = portfolioData.get("topHoldings");
            if (holdings instanceof List<?> holdingsList && !holdingsList.isEmpty()) {
                context.append("TOP HOLDINGS:\n");
                context.append("| Symbol | Name | Value | Provider |\n");
                context.append("|--------|------|-------|----------|\n");

                for (Object holdingObj : holdingsList) {
                    if (holdingObj instanceof Map<?, ?> holding) {
                        context.append(String.format("| %s | %s | %s | %s |\n",
                                holding.get("symbol"),
                                truncateName(String.valueOf(holding.get("name"))),
                                holding.get("value"),
                                holding.get("provider")));
                    }
                }
                context.append("\n");
            }

            // Risk metrics
            Object risk = portfolioData.get("riskMetrics");
            if (risk instanceof Map<?, ?> riskMap && !riskMap.isEmpty()) {
                context.append("RISK METRICS:\n");
                riskMap.forEach((k, v) -> context.append("  • ").append(formatKey(String.valueOf(k))).append(": ").append(v).append("\n"));
                context.append("\n");
            }

        } catch (Exception e) {
            log.warn("Failed to fetch portfolio data: {}", e.getMessage());
            context.append("PORTFOLIO: [Unable to fetch data - service temporarily unavailable]\n\n");
        }
    }

    private void appendBenchmarkReference(StringBuilder context) {
        context.append("BENCHMARK REFERENCE:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Compare portfolio performance against:\n");
        context.append("  • SPY (S&P 500) - Broad US market\n");
        context.append("  • QQQ (NASDAQ-100) - Tech-heavy benchmark\n");
        context.append("  • IWM (Russell 2000) - Small caps\n");
        context.append("  • AGG/BND - Bond market\n");
        context.append("  • GLD - Gold/commodities\n");
        context.append("  • VT - Total world stock market\n\n");

        context.append("Performance Periods:\n");
        context.append("  • Daily, Weekly (5D), Monthly (30D)\n");
        context.append("  • YTD, 1-Year, 3-Year, 5-Year\n\n");
    }

    private void appendRiskFramework(StringBuilder context) {
        context.append("RISK ASSESSMENT GUIDELINES:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Concentration Risk Levels:\n");
        context.append("  • Single position >10%: Moderate risk\n");
        context.append("  • Single position >20%: High risk\n");
        context.append("  • Sector >30%: Concentration concern\n\n");

        context.append("Diversification Targets:\n");
        context.append("  • 15-20+ positions for good diversification\n");
        context.append("  • Multiple asset classes recommended\n");
        context.append("  • Geographic diversification helpful\n\n");
    }

    private void appendDetailedRiskFramework(StringBuilder context) {
        appendRiskFramework(context);

        context.append("DETAILED RISK FACTORS:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("1. MARKET RISK:\n");
        context.append("   • Beta exposure to market movements\n");
        context.append("   • Correlation to major indices\n\n");

        context.append("2. CONCENTRATION RISK:\n");
        context.append("   • Top 5 holdings % of portfolio\n");
        context.append("   • Single sector exposure\n\n");

        context.append("3. LIQUIDITY RISK:\n");
        context.append("   • Small cap positions\n");
        context.append("   • Low volume holdings\n\n");

        context.append("4. CORRELATION RISK:\n");
        context.append("   • Highly correlated positions amplify losses\n");
        context.append("   • Include uncorrelated assets for protection\n\n");
    }

    private void appendRebalancingFramework(StringBuilder context) {
        context.append("REBALANCING FRAMEWORK:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Rebalancing Triggers:\n");
        context.append("  • Drift: Position >5% from target allocation\n");
        context.append("  • Time: Quarterly or semi-annual review\n");
        context.append("  • Events: Major market moves, life changes\n");
        context.append("  • Tax: Tax-loss harvesting opportunities\n\n");

        context.append("Considerations:\n");
        context.append("  • Transaction costs and commissions\n");
        context.append("  • Tax implications (short vs long-term gains)\n");
        context.append("  • Available cash for purchases\n");
        context.append("  • Current market conditions\n\n");

        context.append("Methods:\n");
        context.append("  • Calendar rebalancing (fixed intervals)\n");
        context.append("  • Threshold rebalancing (drift-triggered)\n");
        context.append("  • Cash flow rebalancing (new money)\n\n");
    }

    private void appendHoldingsNews(StringBuilder context, String userId) {
        if (!newsClient.isConfigured()) {
            return;
        }

        try {
            // Get top holdings symbols
            Map<String, Object> portfolioData = portfolioContextProvider.getPortfolioContext(userId);
            Object holdings = portfolioData.get("topHoldings");

            if (holdings instanceof List<?> holdingsList && !holdingsList.isEmpty()) {
                List<String> symbols = holdingsList.stream()
                        .filter(h -> h instanceof Map<?, ?>)
                        .map(h -> String.valueOf(((Map<?, ?>) h).get("symbol")))
                        .filter(s -> s != null && !s.equals("null") && s.length() <= 5)
                        .limit(5)
                        .toList();

                if (!symbols.isEmpty()) {
                    List<NewsArticle> news = newsClient.getNewsForSymbols(symbols, 3);
                    if (!news.isEmpty()) {
                        context.append("NEWS FOR YOUR HOLDINGS:\n");
                        context.append("-".repeat(50)).append("\n");
                        context.append(newsClient.formatNewsForContext(news, 5));
                        context.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch news for holdings: {}", e.getMessage());
        }
    }

    private String truncateName(String name) {
        if (name == null) return "";
        return name.length() > 20 ? name.substring(0, 17) + "..." : name;
    }

    private String formatKey(String key) {
        // Convert camelCase to Title Case
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (i == 0) {
                result.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                result.append(' ').append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
