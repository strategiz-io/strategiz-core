package io.strategiz.client.polygon;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Polygon.io API Client with rate limiting
 * 
 * Free Tier Limits:
 * - 5 calls per minute
 * - 100 calls per day
 * - Real-time data delayed by 15 minutes
 * - Historical data available
 */
@Component
public class PolygonClient {
    
    private static final Logger log = LoggerFactory.getLogger(PolygonClient.class);
    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final Bucket rateLimitBucket;
    
    // API endpoints
    private static final String AGGREGATES_ENDPOINT = "/v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}";
    private static final String GROUPED_DAILY_ENDPOINT = "/v2/aggs/grouped/locale/us/market/stocks/{date}";
    
    public PolygonClient(RestTemplate restTemplate,
                        @Value("${polygon.api.key}") String apiKey,
                        @Value("${polygon.api.base-url:https://api.polygon.io}") String baseUrl,
                        @Value("${polygon.api.rate-limit.per-minute:5}") int rateLimit) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        
        // Create rate limiting bucket
        // Free tier: 5 calls per minute = 1 call every 12 seconds
        Bandwidth limit = Bandwidth.classic(rateLimit, Refill.intervally(rateLimit, Duration.ofMinutes(1)));
        this.rateLimitBucket = Bucket.builder()
            .addLimit(limit)
            .build();
        
        log.info("Initialized Polygon.io client with rate limit: {} calls/minute", rateLimit);
    }
    
    /**
     * Get historical aggregated data for a ticker
     * 
     * @param symbol Stock symbol (e.g., "AAPL")
     * @param multiplier Size of the timespan (e.g., 1 for 1 day)
     * @param timespan Type of timespan (day, hour, minute)
     * @param from Start date (YYYY-MM-DD)
     * @param to End date (YYYY-MM-DD)
     * @return Historical data response
     */
    public PolygonAggregatesResponse getAggregates(String symbol, int multiplier, 
                                                   String timespan, LocalDate from, LocalDate to) {
        try {
            // Rate limiting - wait for available token
            rateLimitBucket.asBlocking().consume(1);
            
            String fromStr = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String toStr = to.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            String url = baseUrl + AGGREGATES_ENDPOINT + "?adjusted=true&sort=asc&apikey=" + apiKey;
            
            log.info("Fetching Polygon aggregates: {} {} {} from {} to {}", 
                     symbol, multiplier, timespan, fromStr, toStr);
            
            ResponseEntity<PolygonAggregatesResponse> response = restTemplate.getForEntity(
                url, 
                PolygonAggregatesResponse.class,
                symbol, multiplier, timespan, fromStr, toStr
            );
            
            PolygonAggregatesResponse data = response.getBody();
            if (data != null && data.getResults() != null) {
                log.info("Retrieved {} data points for {}", data.getResults().size(), symbol);
            }
            
            return data;
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Rate limit exceeded for Polygon API");
                return createErrorResponse("RATE_LIMIT_EXCEEDED", "API rate limit exceeded");
            }
            log.error("HTTP error fetching Polygon data for {}: {} - {}", 
                      symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return createErrorResponse("HTTP_ERROR", e.getResponseBodyAsString());
            
        } catch (Exception e) {
            log.error("Error fetching Polygon data for {}", symbol, e);
            return createErrorResponse("FETCH_ERROR", e.getMessage());
        }
    }
    
    /**
     * Get daily aggregated data for a single symbol (convenience method)
     */
    public PolygonAggregatesResponse getDailyAggregates(String symbol, LocalDate from, LocalDate to) {
        return getAggregates(symbol, 1, "day", from, to);
    }
    
    /**
     * Get previous day's data for a symbol
     */
    public PolygonAggregatesResponse getPreviousClose(String symbol) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return getDailyAggregates(symbol, yesterday, yesterday);
    }
    
    /**
     * Get grouped daily data for all stocks on a specific date
     * Note: This uses significantly more of your daily quota!
     */
    public PolygonGroupedDailyResponse getGroupedDaily(LocalDate date) {
        try {
            rateLimitBucket.asBlocking().consume(1);
            
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = baseUrl + GROUPED_DAILY_ENDPOINT + "?adjusted=true&apikey=" + apiKey;
            
            log.info("Fetching Polygon grouped daily data for {}", dateStr);
            
            ResponseEntity<PolygonGroupedDailyResponse> response = restTemplate.getForEntity(
                url, PolygonGroupedDailyResponse.class, dateStr
            );
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error fetching grouped daily data for {}", date, e);
            return null;
        }
    }
    
    /**
     * Check rate limit status
     */
    public RateLimitStatus getRateLimitStatus() {
        long availableTokens = rateLimitBucket.getAvailableTokens();
        return new RateLimitStatus(availableTokens, 5); // Free tier max
    }
    
    /**
     * Create error response
     */
    private PolygonAggregatesResponse createErrorResponse(String status, String message) {
        PolygonAggregatesResponse response = new PolygonAggregatesResponse();
        response.setStatus(status);
        response.setRequestId("error");
        response.setCount(0);
        response.setResults(Collections.emptyList());
        return response;
    }
    
    /**
     * Rate limit status
     */
    public static class RateLimitStatus {
        private final long availableTokens;
        private final long maxTokens;
        
        public RateLimitStatus(long availableTokens, long maxTokens) {
            this.availableTokens = availableTokens;
            this.maxTokens = maxTokens;
        }
        
        public long getAvailableTokens() { return availableTokens; }
        public long getMaxTokens() { return maxTokens; }
        public boolean canMakeRequest() { return availableTokens > 0; }
        public double getUsagePercentage() { 
            return ((double) (maxTokens - availableTokens) / maxTokens) * 100; 
        }
    }
}