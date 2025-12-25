package io.strategiz.client.alpaca.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.alpaca.error.AlpacaErrorDetails;
import io.strategiz.client.alpaca.model.AlpacaBar;
import io.strategiz.client.alpaca.model.AlpacaBarsResponse;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.framework.secrets.service.VaultSecretService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Alpaca Historical Data Client - Fetches historical OHLCV bars
 *
 * API Documentation: https://docs.alpaca.markets/reference/stockbars
 *
 * Features:
 * - Automatic pagination (10,000 bars per request limit)
 * - Supports multiple timeframes (1Min, 5Min, 15Min, 1Hour, 1Day)
 * - Rate limiting with configurable delay
 */
@Component
public class AlpacaHistoricalClient {

    private static final Logger log = LoggerFactory.getLogger(AlpacaHistoricalClient.class);
    private static final String MODULE_NAME = "AlpacaHistoricalClient";

    @Autowired(required = false)
    private VaultSecretService vaultSecretService;

    private String apiUrl;
    private String apiKey;
    private String apiSecret;
    private String feed;

    @Value("${alpaca.batch.delay-ms:500}")
    private int delayMs;

    @Value("${alpaca.batch.max-retries:5}")
    private int maxRetries;

    @Value("${alpaca.batch.retry-delay-ms:2000}")
    private int retryDelayMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlpacaHistoricalClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("AlpacaHistoricalClient initializing...");

        // Load credentials from Vault ONLY - no fallback to properties files
        if (vaultSecretService == null) {
            log.error("VaultSecretService not available! Cannot load Alpaca credentials.");
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "VaultSecretService is required but not available");
        }

        try {
            apiKey = vaultSecretService.readSecret("alpaca.marketdata.api-key");
            apiSecret = vaultSecretService.readSecret("alpaca.marketdata.api-secret");
            apiUrl = vaultSecretService.readSecret("alpaca.marketdata.base-url");
            feed = vaultSecretService.readSecret("alpaca.marketdata.feed");

            log.info("Successfully loaded Alpaca credentials from Vault");
        } catch (Exception e) {
            log.error("Failed to load Alpaca credentials from Vault: {}", e.getMessage());
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, e, "Failed to load required Alpaca credentials from Vault");
        }

        // Set defaults only for non-sensitive configuration
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://data.alpaca.markets";
            log.info("Using default API URL: {}", apiUrl);
        }
        if (feed == null || feed.isEmpty()) {
            feed = "iex";
            log.info("Using default feed: {}", feed);
        }

        log.info("AlpacaHistoricalClient initialized");
        log.info("API URL: {}", apiUrl);
        log.info("API Key: {}", apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "NOT SET");
        log.info("API Secret: {}", apiSecret != null && !apiSecret.isEmpty() ? "SET (length: " + apiSecret.length() + ")" : "NOT SET");
        log.info("Feed: {}", feed);

        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            log.error("Alpaca API credentials are not configured!");
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "Alpaca API credentials must be configured in Vault");
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("APCA-API-KEY-ID", apiKey);
        headers.add("APCA-API-SECRET-KEY", apiSecret);
        headers.add("Accept", "application/json");
        log.debug("Created headers with API Key: {}", apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, 8) + "..." : "MISSING");
        return headers;
    }

    /**
     * Fetch all historical bars with automatic pagination
     */
    public List<AlpacaBar> getBars(String symbol, LocalDateTime startDate, LocalDateTime endDate, String timeframe) {
        log.debug("Fetching bars for {} from {} to {} ({})", symbol, startDate, endDate, timeframe);

        List<AlpacaBar> allBars = new ArrayList<>();
        String nextPageToken = null;
        int pageCount = 0;

        do {
            try {
                if (delayMs > 0 && pageCount > 0) {
                    Thread.sleep(delayMs);
                }

                AlpacaBarsResponse response = getBarsPage(symbol, startDate, endDate, timeframe, nextPageToken);

                if (response.getBars() != null && !response.getBars().isEmpty()) {
                    allBars.addAll(response.getBars());
                    log.debug("Page {}: fetched {} bars for {}", pageCount + 1, response.getBars().size(), symbol);
                }

                nextPageToken = response.getNextPageToken();
                pageCount++;

                if (pageCount > 1000) {
                    log.warn("Exceeded 1000 pages for {}, stopping", symbol);
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StrategizException(
                    AlpacaErrorDetails.PAGINATION_ERROR,
                    MODULE_NAME,
                    e,
                    "Thread interrupted during pagination"
                );
            }

        } while (nextPageToken != null && !nextPageToken.isEmpty());

        log.info("Fetched {} total bars for {} across {} pages", allBars.size(), symbol, pageCount);
        return allBars;
    }

    /**
     * Fetch a single page of bars with retry logic for rate limiting
     */
    private AlpacaBarsResponse getBarsPage(String symbol, LocalDateTime startDate, LocalDateTime endDate,
                                           String timeframe, String pageToken) {
        String start = startDate.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String end = endDate.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        StringBuilder path = new StringBuilder(String.format(
            "/v2/stocks/%s/bars?start=%s&end=%s&timeframe=%s&limit=10000&feed=%s",
            symbol, start, end, timeframe, feed
        ));

        if (pageToken != null && !pageToken.isEmpty()) {
            path.append("&page_token=").append(pageToken);
        }

        String fullUrl = apiUrl + path.toString();

        // Retry loop with exponential backoff for 429 errors
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                log.debug(">>> ALPACA API CALL (attempt {}): {}", retryCount + 1, fullUrl);

                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
                );

                String responseBody = responseEntity.getBody();
                log.debug(">>> ALPACA RAW RESPONSE (first 500 chars): {}",
                    responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : "NULL");

                if (responseBody == null || responseBody.isEmpty()) {
                    throw new StrategizException(
                        AlpacaErrorDetails.NO_DATA_AVAILABLE,
                        MODULE_NAME,
                        String.format("Empty response for symbol: %s", symbol)
                    );
                }

                AlpacaBarsResponse response = objectMapper.readValue(responseBody, AlpacaBarsResponse.class);
                log.debug(">>> PARSED RESPONSE: bars count={}, nextPageToken={}",
                    response.getBars() != null ? response.getBars().size() : 0, response.getNextPageToken());

                if (response.getSymbol() == null) {
                    response.setSymbol(symbol);
                }

                return response;

            } catch (HttpClientErrorException.TooManyRequests e) {
                lastException = e;
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("Max retries ({}) exceeded for {} - rate limit still in effect", maxRetries, symbol);
                    break;
                }

                // Exponential backoff: 2s, 4s, 8s, 16s, 32s
                long backoffMs = retryDelayMs * (long) Math.pow(2, retryCount - 1);
                log.warn("Rate limited (429) for {}, attempt {}/{}, waiting {}ms before retry",
                    symbol, retryCount, maxRetries, backoffMs);

                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new StrategizException(
                        AlpacaErrorDetails.PAGINATION_ERROR,
                        MODULE_NAME,
                        ie,
                        "Thread interrupted during rate limit backoff"
                    );
                }
            } catch (Exception e) {
                log.error("Error fetching bars for {}: {}", symbol, e.getMessage());
                throw new StrategizException(
                    AlpacaErrorDetails.API_ERROR_RESPONSE,
                    MODULE_NAME,
                    e,
                    String.format("Failed to fetch bars for symbol: %s", symbol)
                );
            }
        }

        // All retries exhausted
        throw new StrategizException(
            AlpacaErrorDetails.API_ERROR_RESPONSE,
            MODULE_NAME,
            lastException,
            String.format("Failed to fetch bars for %s after %d retries due to rate limiting", symbol, maxRetries)
        );
    }
}
