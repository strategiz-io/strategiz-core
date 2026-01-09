package io.strategiz.service.agents.context;

import io.strategiz.client.finnhub.FinnhubNewsClient;
import io.strategiz.client.finnhub.dto.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Provides market signals context for the Scout Agent
 * Includes market news, sector performance, and sentiment indicators
 */
@Component
public class MarketSignalsContextProvider {

    private static final Logger log = LoggerFactory.getLogger(MarketSignalsContextProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Sector ETFs for tracking rotation
    private static final List<String> SECTOR_ETFS = List.of(
            "XLK", // Technology
            "XLF", // Financials
            "XLE", // Energy
            "XLV", // Healthcare
            "XLI", // Industrials
            "XLY", // Consumer Discretionary
            "XLP", // Consumer Staples
            "XLRE", // Real Estate
            "XLU", // Utilities
            "XLB", // Materials
            "XLC"  // Communication Services
    );

    private final FinnhubNewsClient newsClient;

    public MarketSignalsContextProvider(FinnhubNewsClient newsClient) {
        this.newsClient = newsClient;
    }

    /**
     * Build comprehensive market signals context for AI injection
     *
     * @param symbols User's watchlist symbols (optional)
     * @return Formatted context string for AI consumption
     */
    public String buildMarketSignalsContext(List<String> symbols) {
        StringBuilder context = new StringBuilder();

        // Add timestamp
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        // Market overview
        appendMarketOverview(context);

        // Market news for sentiment
        appendMarketNews(context);

        // Sector analysis hints
        appendSectorAnalysis(context);

        // Technical analysis guidance
        appendTechnicalGuidance(context);

        // If user has watchlist symbols
        if (symbols != null && !symbols.isEmpty()) {
            appendWatchlistContext(context, symbols);
        }

        return context.toString();
    }

    /**
     * Build context focused on market conditions
     */
    public String buildMarketConditionsContext() {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendMarketOverview(context);
        appendMarketNews(context);
        appendSectorAnalysis(context);

        return context.toString();
    }

    /**
     * Build context for sector rotation analysis
     */
    public String buildSectorRotationContext() {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
        context.append("SECTOR ROTATION ANALYSIS:\n");
        context.append("-".repeat(50)).append("\n\n");

        appendSectorAnalysis(context);
        appendSectorGuidance(context);

        return context.toString();
    }

    private void appendMarketOverview(StringBuilder context) {
        context.append("MARKET OVERVIEW:\n");
        context.append("-".repeat(50)).append("\n");

        // Market session
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        String session;
        if (hour < 9 || (hour == 9 && now.getMinute() < 30)) {
            session = "Pre-Market";
        } else if (hour >= 16) {
            session = "After-Hours";
        } else {
            session = "Regular Trading Hours";
        }
        context.append("Session: ").append(session).append(" (EST)\n\n");

        // Key indices reference
        context.append("Key Benchmarks to Consider:\n");
        context.append("  • SPY (S&P 500) - Broad market direction\n");
        context.append("  • QQQ (NASDAQ-100) - Tech sector proxy\n");
        context.append("  • IWM (Russell 2000) - Small cap sentiment\n");
        context.append("  • VIX - Market volatility/fear gauge\n");
        context.append("\n");
    }

    private void appendMarketNews(StringBuilder context) {
        if (!newsClient.isConfigured()) {
            context.append("MARKET NEWS: [News API not configured - provide Finnhub API key for real-time news]\n\n");
            return;
        }

        try {
            List<NewsArticle> marketNews = newsClient.getMarketNews("general");
            if (!marketNews.isEmpty()) {
                context.append("MARKET-MOVING NEWS:\n");
                context.append("-".repeat(50)).append("\n");
                context.append(newsClient.formatNewsForContext(marketNews, 8));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch market news: {}", e.getMessage());
            context.append("MARKET NEWS: [Unable to fetch - service temporarily unavailable]\n\n");
        }
    }

    private void appendSectorAnalysis(StringBuilder context) {
        context.append("SECTOR TRACKING:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Monitor these sector ETFs for rotation signals:\n\n");

        context.append("| Sector | ETF | Description |\n");
        context.append("|--------|-----|-------------|\n");
        context.append("| Technology | XLK | Tech giants, semiconductors |\n");
        context.append("| Financials | XLF | Banks, insurance, asset mgmt |\n");
        context.append("| Energy | XLE | Oil, gas, energy equipment |\n");
        context.append("| Healthcare | XLV | Pharma, biotech, medical devices |\n");
        context.append("| Industrials | XLI | Defense, machinery, airlines |\n");
        context.append("| Consumer Disc. | XLY | Retail, automotive, leisure |\n");
        context.append("| Consumer Staples | XLP | Food, beverage, household |\n");
        context.append("| Real Estate | XLRE | REITs, property |\n");
        context.append("| Utilities | XLU | Electric, gas, water |\n");
        context.append("| Materials | XLB | Chemicals, mining, paper |\n");
        context.append("| Communication | XLC | Media, telecom, internet |\n");
        context.append("\n");

        context.append("Sector Rotation Cycle Reference:\n");
        context.append("  Early Recovery: Consumer Disc., Financials, Industrials\n");
        context.append("  Mid-Cycle: Technology, Industrials, Materials\n");
        context.append("  Late Cycle: Energy, Healthcare, Consumer Staples\n");
        context.append("  Recession: Utilities, Healthcare, Consumer Staples\n");
        context.append("\n");
    }

    private void appendSectorGuidance(StringBuilder context) {
        context.append("SECTOR ANALYSIS GUIDANCE:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("When analyzing sector rotation, consider:\n\n");
        context.append("1. Relative Strength: Compare sector performance vs SPY\n");
        context.append("2. Volume Trends: Rising volume with price = conviction\n");
        context.append("3. Leadership: Which sectors are outperforming this week/month?\n");
        context.append("4. Laggards: Which sectors are underperforming (potential catch-up trades)?\n");
        context.append("5. Economic Cycle: Current phase suggests focus on specific sectors\n");
        context.append("\n");
    }

    private void appendTechnicalGuidance(StringBuilder context) {
        context.append("TECHNICAL ANALYSIS FRAMEWORK:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Key indicators to evaluate:\n\n");

        context.append("Trend Indicators:\n");
        context.append("  • SMA 20/50/200 - Short/medium/long-term trends\n");
        context.append("  • EMA 9/21 - Fast-moving signals\n");
        context.append("  • ADX - Trend strength (>25 = strong trend)\n\n");

        context.append("Momentum Indicators:\n");
        context.append("  • RSI - Oversold (<30) / Overbought (>70)\n");
        context.append("  • MACD - Crossovers signal momentum shifts\n");
        context.append("  • Stochastic - Short-term momentum\n\n");

        context.append("Volume Indicators:\n");
        context.append("  • OBV - Accumulation/distribution\n");
        context.append("  • Volume MA - Above average = interest\n\n");

        context.append("Volatility:\n");
        context.append("  • ATR - Average true range for position sizing\n");
        context.append("  • Bollinger Bands - Volatility expansion/contraction\n\n");
    }

    private void appendWatchlistContext(StringBuilder context, List<String> symbols) {
        context.append("YOUR WATCHLIST:\n");
        context.append("-".repeat(50)).append("\n");
        context.append("Tracking: ").append(String.join(", ", symbols)).append("\n\n");

        // If we have news client, get company-specific news
        if (newsClient.isConfigured()) {
            try {
                List<NewsArticle> watchlistNews = newsClient.getNewsForSymbols(symbols, 3);
                if (!watchlistNews.isEmpty()) {
                    context.append("WATCHLIST NEWS:\n");
                    context.append(newsClient.formatNewsForContext(watchlistNews, 5));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch watchlist news: {}", e.getMessage());
            }
        }
    }

    /**
     * Check if the provider is configured with API key
     */
    public boolean isConfigured() {
        return newsClient.isConfigured();
    }
}
