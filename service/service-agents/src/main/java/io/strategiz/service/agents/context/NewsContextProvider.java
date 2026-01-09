package io.strategiz.service.agents.context;

import io.strategiz.client.finnhub.FinnhubEarningsClient;
import io.strategiz.client.finnhub.FinnhubFilingsClient;
import io.strategiz.client.finnhub.FinnhubNewsClient;
import io.strategiz.client.finnhub.dto.EarningsCalendarEvent;
import io.strategiz.client.finnhub.dto.NewsArticle;
import io.strategiz.client.finnhub.dto.SECFiling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Provides real news context for AI agents using Finnhub API
 */
@Component
public class NewsContextProvider {

    private static final Logger log = LoggerFactory.getLogger(NewsContextProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_NEWS_LIMIT = 10;
    private static final int DEFAULT_FILINGS_DAYS = 30;

    private final FinnhubNewsClient newsClient;
    private final FinnhubFilingsClient filingsClient;
    private final FinnhubEarningsClient earningsClient;

    public NewsContextProvider(
            FinnhubNewsClient newsClient,
            FinnhubFilingsClient filingsClient,
            FinnhubEarningsClient earningsClient) {
        this.newsClient = newsClient;
        this.filingsClient = filingsClient;
        this.earningsClient = earningsClient;
    }

    /**
     * Build comprehensive news context for AI injection
     *
     * @param symbols List of symbols the user is interested in (watchlist)
     * @param newsType Type of news to focus on (general, earnings, filings)
     * @param sector Sector to focus on (optional)
     * @return Formatted context string for AI consumption
     */
    public String buildNewsContext(List<String> symbols, String newsType, String sector) {
        StringBuilder context = new StringBuilder();

        // Add timestamp
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        // Always include general market news
        appendMarketNews(context);

        // Add company-specific news if symbols provided
        if (symbols != null && !symbols.isEmpty()) {
            appendCompanyNews(context, symbols);
            appendSECFilings(context, symbols);
        }

        // Add earnings context if relevant
        if (newsType == null || "earnings".equalsIgnoreCase(newsType)) {
            appendUpcomingEarnings(context);
        }

        return context.toString();
    }

    /**
     * Build context focused on a specific symbol
     */
    public String buildSymbolNewsContext(String symbol) {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");
        context.append("NEWS FOCUS: ").append(symbol.toUpperCase()).append("\n\n");

        // Company news
        appendCompanyNewsForSymbol(context, symbol);

        // SEC filings
        appendFilingsForSymbol(context, symbol);

        // Symbol-specific earnings
        List<EarningsCalendarEvent> earnings = earningsClient.getUpcomingEarnings(30).stream()
                .filter(e -> symbol.equalsIgnoreCase(e.getSymbol()))
                .toList();

        if (!earnings.isEmpty()) {
            context.append("UPCOMING EARNINGS FOR ").append(symbol.toUpperCase()).append(":\n");
            context.append(earningsClient.formatEarningsForContext(earnings, true));
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Build context for breaking/market-wide news only
     */
    public String buildMarketNewsContext() {
        StringBuilder context = new StringBuilder();
        context.append("CURRENT TIME: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n\n");

        appendMarketNews(context);
        appendUpcomingEarnings(context);

        return context.toString();
    }

    private void appendMarketNews(StringBuilder context) {
        if (!newsClient.isConfigured()) {
            context.append("MARKET NEWS: [News API not configured - provide API key for real-time news]\n\n");
            return;
        }

        try {
            List<NewsArticle> marketNews = newsClient.getMarketNews("general");
            if (!marketNews.isEmpty()) {
                context.append("MARKET NEWS (Latest):\n");
                context.append("-".repeat(50)).append("\n");
                context.append(newsClient.formatNewsForContext(marketNews, DEFAULT_NEWS_LIMIT));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch market news: {}", e.getMessage());
            context.append("MARKET NEWS: [Unable to fetch - service temporarily unavailable]\n\n");
        }
    }

    private void appendCompanyNews(StringBuilder context, List<String> symbols) {
        if (!newsClient.isConfigured()) {
            return;
        }

        try {
            List<NewsArticle> companyNews = newsClient.getNewsForSymbols(symbols, 7);
            if (!companyNews.isEmpty()) {
                context.append("WATCHLIST NEWS:\n");
                context.append("Tracking: ").append(String.join(", ", symbols)).append("\n");
                context.append("-".repeat(50)).append("\n");
                context.append(newsClient.formatNewsForContext(companyNews, DEFAULT_NEWS_LIMIT));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch company news: {}", e.getMessage());
        }
    }

    private void appendCompanyNewsForSymbol(StringBuilder context, String symbol) {
        if (!newsClient.isConfigured()) {
            context.append("COMPANY NEWS: [News API not configured]\n\n");
            return;
        }

        try {
            List<NewsArticle> news = newsClient.getRecentCompanyNews(symbol);
            if (!news.isEmpty()) {
                context.append("RECENT NEWS:\n");
                context.append("-".repeat(50)).append("\n");
                context.append(newsClient.formatNewsForContext(news, 15));
                context.append("\n");
            } else {
                context.append("RECENT NEWS: No recent news found for ").append(symbol).append("\n\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
            context.append("RECENT NEWS: [Unable to fetch]\n\n");
        }
    }

    private void appendSECFilings(StringBuilder context, List<String> symbols) {
        if (!filingsClient.isConfigured()) {
            return;
        }

        try {
            List<SECFiling> filings = filingsClient.getFilingsForSymbols(symbols, DEFAULT_FILINGS_DAYS);
            if (!filings.isEmpty()) {
                context.append("RECENT SEC FILINGS:\n");
                context.append("-".repeat(50)).append("\n");
                context.append(filingsClient.formatFilingsForContext(filings));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch SEC filings: {}", e.getMessage());
        }
    }

    private void appendFilingsForSymbol(StringBuilder context, String symbol) {
        if (!filingsClient.isConfigured()) {
            context.append("SEC FILINGS: [Filings API not configured]\n\n");
            return;
        }

        try {
            List<SECFiling> filings = filingsClient.getMajorFilings(symbol, 90);
            if (!filings.isEmpty()) {
                context.append("SEC FILINGS (Last 90 days):\n");
                context.append("-".repeat(50)).append("\n");
                context.append(filingsClient.formatFilingsForContext(filings));
                context.append("\n");
            } else {
                context.append("SEC FILINGS: No recent filings for ").append(symbol).append("\n\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch filings for {}: {}", symbol, e.getMessage());
            context.append("SEC FILINGS: [Unable to fetch]\n\n");
        }
    }

    private void appendUpcomingEarnings(StringBuilder context) {
        if (!earningsClient.isConfigured()) {
            return;
        }

        try {
            List<EarningsCalendarEvent> upcoming = earningsClient.getUpcomingEarnings(7);
            if (!upcoming.isEmpty()) {
                context.append("UPCOMING EARNINGS (Next 7 days):\n");
                context.append("-".repeat(50)).append("\n");
                // Limit to notable names (could filter by market cap in future)
                List<EarningsCalendarEvent> limited = upcoming.stream().limit(15).toList();
                context.append(earningsClient.formatEarningsForContext(limited, true));
                context.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch upcoming earnings: {}", e.getMessage());
        }
    }

    /**
     * Check if the provider is configured with API key
     */
    public boolean isConfigured() {
        return newsClient.isConfigured();
    }
}
