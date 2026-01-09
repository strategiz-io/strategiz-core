package io.strategiz.client.finnhub;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.finnhub.config.FinnhubConfig;
import io.strategiz.client.finnhub.dto.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Finnhub client for news-related endpoints
 */
@Component
public class FinnhubNewsClient extends FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubNewsClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;

    public FinnhubNewsClient(
            FinnhubConfig config,
            @Qualifier("finnhubRateLimiter") Bucket rateLimiter,
            ObjectMapper objectMapper) {
        super(config, rateLimiter, objectMapper);
        this.objectMapper = objectMapper;
    }

    /**
     * Get general market news
     * @param category News category (general, forex, crypto, merger)
     * @return List of news articles
     */
    @Cacheable(value = "finnhubMarketNews", key = "#category", unless = "#result.isEmpty()")
    public List<NewsArticle> getMarketNews(String category) {
        log.debug("Fetching market news for category: {}", category);

        Map<String, String> params = Map.of("category", category != null ? category : "general");
        return get("/news", params, NewsArticle[].class)
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    /**
     * Get general market news (defaults to 'general' category)
     */
    public List<NewsArticle> getMarketNews() {
        return getMarketNews("general");
    }

    /**
     * Get company-specific news
     * @param symbol Stock symbol
     * @param fromDate Start date
     * @param toDate End date
     * @return List of news articles for the company
     */
    @Cacheable(value = "finnhubCompanyNews", key = "#symbol + '-' + #fromDate + '-' + #toDate", unless = "#result.isEmpty()")
    public List<NewsArticle> getCompanyNews(String symbol, LocalDate fromDate, LocalDate toDate) {
        log.debug("Fetching company news for {} from {} to {}", symbol, fromDate, toDate);

        Map<String, String> params = Map.of(
                "symbol", symbol.toUpperCase(),
                "from", fromDate.format(DATE_FORMATTER),
                "to", toDate.format(DATE_FORMATTER)
        );

        return get("/company-news", params, NewsArticle[].class)
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    /**
     * Get recent company news (last 7 days)
     */
    public List<NewsArticle> getRecentCompanyNews(String symbol) {
        LocalDate today = LocalDate.now();
        return getCompanyNews(symbol, today.minusDays(7), today);
    }

    /**
     * Get news for multiple symbols
     * @param symbols List of stock symbols
     * @param days Number of days to look back
     * @return Combined list of news articles
     */
    public List<NewsArticle> getNewsForSymbols(List<String> symbols, int days) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(days);

        return symbols.stream()
                .flatMap(symbol -> getCompanyNews(symbol, fromDate, today).stream())
                .sorted((a, b) -> {
                    // Sort by datetime descending (most recent first)
                    if (a.getDatetime() == null) return 1;
                    if (b.getDatetime() == null) return -1;
                    return b.getDatetime().compareTo(a.getDatetime());
                })
                .limit(50) // Limit total results
                .toList();
    }

    /**
     * Format news articles for AI context injection
     */
    public String formatNewsForContext(List<NewsArticle> articles, int maxArticles) {
        if (articles == null || articles.isEmpty()) {
            return "No recent news available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("RECENT NEWS:\n");
        sb.append("-".repeat(50)).append("\n");

        articles.stream()
                .limit(maxArticles)
                .forEach(article -> {
                    sb.append(article.toContextString());
                    sb.append("\n\n");
                });

        return sb.toString();
    }
}
