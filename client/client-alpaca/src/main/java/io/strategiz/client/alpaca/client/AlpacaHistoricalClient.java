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
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
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

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        log.info("AlpacaHistoricalClient initializing...");

        // Load credentials from Vault ONLY - no fallback to properties files
        if (vaultSecretService == null) {
            log.error("VaultSecretService not available! AlpacaHistoricalClient will be unavailable.");
            return;
        }

        try {
            apiKey = vaultSecretService.readSecret("alpaca.marketdata.api-key");
            apiSecret = vaultSecretService.readSecret("alpaca.marketdata.api-secret");
            apiUrl = vaultSecretService.readSecret("alpaca.marketdata.base-url");
            feed = vaultSecretService.readSecret("alpaca.marketdata.feed");

            log.info("Successfully loaded Alpaca credentials from Vault");
        } catch (Exception e) {
            log.error("Failed to load Alpaca credentials from Vault: {} - AlpacaHistoricalClient will be unavailable", e.getMessage());
            return;
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

        log.info("API URL: {}", apiUrl);
        log.info("API Key: {}", apiKey != null && !apiKey.isEmpty() ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "NOT SET");
        log.info("API Secret: {}", apiSecret != null && !apiSecret.isEmpty() ? "SET (length: " + apiSecret.length() + ")" : "NOT SET");
        log.info("Feed: {}", feed);

        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            log.error("Alpaca API credentials are not configured! AlpacaHistoricalClient will be unavailable.");
            return;
        }

        initialized = true;
        log.info("AlpacaHistoricalClient initialized successfully");
    }

    /**
     * Check if the client is available and properly configured.
     */
    public boolean isAvailable() {
        return initialized && apiKey != null && apiSecret != null;
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
        if (!isAvailable()) {
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "AlpacaHistoricalClient is not available - credentials not configured");
        }
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

    /**
     * Get latest quotes for multiple stock symbols.
     * Uses Alpaca's snapshots endpoint for accurate daily change calculation.
     * Snapshots include prevDailyBar which gives us yesterday's close for proper % change.
     *
     * @param symbols List of stock symbols (e.g., ["AAPL", "MSFT", "GOOG"])
     * @return Map of symbol to quote data (price, change, etc.)
     */
    public Map<String, LatestQuote> getLatestStockQuotes(List<String> symbols) {
        if (!isAvailable()) {
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "AlpacaHistoricalClient is not available - credentials not configured");
        }

        Map<String, LatestQuote> result = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        try {
            // Join symbols with comma
            String symbolsParam = String.join(",", symbols);
            // Use snapshots endpoint which includes prevDailyBar for accurate daily change
            String fullUrl = apiUrl + "/v2/stocks/snapshots?symbols=" + symbolsParam + "&feed=" + feed;

            log.debug("Fetching snapshots for: {}", symbolsParam);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                String.class
            );

            String responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response for snapshots");
                return result;
            }

            // Parse the response - snapshots returns { "AAPL": { "latestTrade": {...}, "dailyBar": {...}, "prevDailyBar": {...} }, ... }
            Map<String, Object> snapshots = objectMapper.readValue(responseBody, Map.class);

            for (Map.Entry<String, Object> entry : snapshots.entrySet()) {
                String symbol = entry.getKey();
                Map<String, Object> snapshotData = (Map<String, Object>) entry.getValue();

                try {
                    // Get current price from latestTrade (most accurate real-time price)
                    Map<String, Object> latestTrade = (Map<String, Object>) snapshotData.get("latestTrade");
                    Map<String, Object> prevDailyBar = (Map<String, Object>) snapshotData.get("prevDailyBar");

                    if (latestTrade == null || prevDailyBar == null) {
                        log.warn("Missing latestTrade or prevDailyBar for {}", symbol);
                        continue;
                    }

                    // Current price from latest trade
                    BigDecimal currentPrice = new BigDecimal(latestTrade.get("p").toString());
                    // Previous day's close for accurate daily change
                    BigDecimal prevClose = new BigDecimal(prevDailyBar.get("c").toString());

                    // Calculate daily change (current price vs previous close)
                    BigDecimal change = currentPrice.subtract(prevClose);
                    BigDecimal changePercent = prevClose.compareTo(BigDecimal.ZERO) != 0
                        ? change.multiply(new BigDecimal("100")).divide(prevClose, 2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                    result.put(symbol, new LatestQuote(symbol, currentPrice, change, changePercent));
                    log.debug("Snapshot for {}: price={}, prevClose={}, change={}%",
                        symbol, currentPrice, prevClose, changePercent);

                } catch (Exception e) {
                    log.warn("Failed to parse snapshot for {}: {}", symbol, e.getMessage());
                }
            }

            log.info("Fetched {} snapshots", result.size());
            return result;

        } catch (Exception e) {
            log.error("Error fetching snapshots: {}", e.getMessage());
            throw new StrategizException(
                AlpacaErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Failed to fetch stock snapshots"
            );
        }
    }

    /**
     * Fetch all historical crypto bars with automatic pagination.
     * Uses Alpaca's crypto data endpoint: /v1beta3/crypto/us/bars
     *
     * @param symbol Canonical crypto symbol (e.g., "BTC", "ETH")
     * @param startDate Start date for data
     * @param endDate End date for data
     * @param timeframe Timeframe (1Min, 5Min, 15Min, 1Hour, 1Day)
     * @return List of AlpacaBar objects
     */
    public List<AlpacaBar> getCryptoBars(String symbol, LocalDateTime startDate, LocalDateTime endDate, String timeframe) {
        if (!isAvailable()) {
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "AlpacaHistoricalClient is not available - credentials not configured");
        }

        // Convert to Alpaca crypto format (BTC -> BTC/USD)
        String cryptoPair = symbol + "/USD";
        log.debug("Fetching crypto bars for {} ({}) from {} to {} ({})", symbol, cryptoPair, startDate, endDate, timeframe);

        List<AlpacaBar> allBars = new ArrayList<>();
        String nextPageToken = null;
        int pageCount = 0;

        do {
            try {
                if (delayMs > 0 && pageCount > 0) {
                    Thread.sleep(delayMs);
                }

                AlpacaBarsResponse response = getCryptoBarsPage(cryptoPair, symbol, startDate, endDate, timeframe, nextPageToken);

                if (response.getBars() != null && !response.getBars().isEmpty()) {
                    allBars.addAll(response.getBars());
                    log.debug("Page {}: fetched {} crypto bars for {}", pageCount + 1, response.getBars().size(), symbol);
                }

                nextPageToken = response.getNextPageToken();
                pageCount++;

                if (pageCount > 1000) {
                    log.warn("Exceeded 1000 pages for crypto {}, stopping", symbol);
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StrategizException(
                    AlpacaErrorDetails.PAGINATION_ERROR,
                    MODULE_NAME,
                    e,
                    "Thread interrupted during crypto pagination"
                );
            }

        } while (nextPageToken != null && !nextPageToken.isEmpty());

        log.info("Fetched {} total crypto bars for {} across {} pages", allBars.size(), symbol, pageCount);
        return allBars;
    }

    /**
     * Fetch a single page of crypto bars with retry logic for rate limiting
     */
    private AlpacaBarsResponse getCryptoBarsPage(String cryptoPair, String canonicalSymbol, LocalDateTime startDate,
                                                  LocalDateTime endDate, String timeframe, String pageToken) {
        String start = startDate.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String end = endDate.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        StringBuilder path = new StringBuilder(String.format(
            "/v1beta3/crypto/us/bars?symbols=%s&start=%s&end=%s&timeframe=%s&limit=10000",
            cryptoPair, start, end, timeframe
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
                log.debug(">>> ALPACA CRYPTO API CALL (attempt {}): {}", retryCount + 1, fullUrl);

                HttpEntity<String> entity = new HttpEntity<>(createHeaders());
                ResponseEntity<String> responseEntity = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
                );

                String responseBody = responseEntity.getBody();
                log.debug(">>> ALPACA CRYPTO RAW RESPONSE (first 500 chars): {}",
                    responseBody != null ? responseBody.substring(0, Math.min(500, responseBody.length())) : "NULL");

                if (responseBody == null || responseBody.isEmpty()) {
                    throw new StrategizException(
                        AlpacaErrorDetails.NO_DATA_AVAILABLE,
                        MODULE_NAME,
                        String.format("Empty response for crypto symbol: %s", canonicalSymbol)
                    );
                }

                // Parse crypto response - structure is different from stocks
                // Crypto returns: { "bars": { "BTC/USD": [ {...}, {...} ] }, "next_page_token": "..." }
                Map<String, Object> rawResponse = objectMapper.readValue(responseBody, Map.class);
                Map<String, Object> barsMap = (Map<String, Object>) rawResponse.get("bars");
                String nextToken = (String) rawResponse.get("next_page_token");

                AlpacaBarsResponse response = new AlpacaBarsResponse();
                response.setSymbol(canonicalSymbol);
                response.setNextPageToken(nextToken);

                if (barsMap != null && barsMap.containsKey(cryptoPair)) {
                    List<Map<String, Object>> barsList = (List<Map<String, Object>>) barsMap.get(cryptoPair);
                    List<AlpacaBar> bars = new ArrayList<>();

                    for (Map<String, Object> barData : barsList) {
                        AlpacaBar bar = new AlpacaBar();
                        bar.setTimestamp((String) barData.get("t"));
                        bar.setOpen(new BigDecimal(barData.get("o").toString()));
                        bar.setHigh(new BigDecimal(barData.get("h").toString()));
                        bar.setLow(new BigDecimal(barData.get("l").toString()));
                        bar.setClose(new BigDecimal(barData.get("c").toString()));
                        bar.setVolume(((Number) barData.get("v")).longValue());
                        bars.add(bar);
                    }

                    response.setBars(bars);
                }

                log.debug(">>> PARSED CRYPTO RESPONSE: bars count={}, nextPageToken={}",
                    response.getBars() != null ? response.getBars().size() : 0, response.getNextPageToken());

                return response;

            } catch (HttpClientErrorException.TooManyRequests e) {
                lastException = e;
                retryCount++;
                if (retryCount > maxRetries) {
                    log.error("Max retries ({}) exceeded for crypto {} - rate limit still in effect", maxRetries, canonicalSymbol);
                    break;
                }

                long backoffMs = retryDelayMs * (long) Math.pow(2, retryCount - 1);
                log.warn("Rate limited (429) for crypto {}, attempt {}/{}, waiting {}ms before retry",
                    canonicalSymbol, retryCount, maxRetries, backoffMs);

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
                log.error("Error fetching crypto bars for {}: {}", canonicalSymbol, e.getMessage());
                throw new StrategizException(
                    AlpacaErrorDetails.API_ERROR_RESPONSE,
                    MODULE_NAME,
                    e,
                    String.format("Failed to fetch crypto bars for symbol: %s", canonicalSymbol)
                );
            }
        }

        throw new StrategizException(
            AlpacaErrorDetails.API_ERROR_RESPONSE,
            MODULE_NAME,
            lastException,
            String.format("Failed to fetch crypto bars for %s after %d retries due to rate limiting", canonicalSymbol, maxRetries)
        );
    }

    /**
     * Get latest quotes for crypto symbols.
     * Uses Alpaca's crypto snapshots endpoint for accurate daily change calculation.
     *
     * @param symbols List of crypto symbols (e.g., ["BTC", "ETH"])
     * @return Map of symbol to quote data
     */
    public Map<String, LatestQuote> getLatestCryptoQuotes(List<String> symbols) {
        if (!isAvailable()) {
            throw new StrategizException(AlpacaErrorDetails.CONFIGURATION_ERROR,
                MODULE_NAME, "AlpacaHistoricalClient is not available - credentials not configured");
        }

        Map<String, LatestQuote> result = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return result;
        }

        try {
            // Convert symbols to Alpaca crypto format (BTC -> BTC/USD)
            List<String> cryptoPairs = symbols.stream()
                .map(s -> s + "/USD")
                .toList();
            String symbolsParam = String.join(",", cryptoPairs);

            // Use snapshots endpoint for accurate daily change calculation
            String fullUrl = apiUrl + "/v1beta3/crypto/us/snapshots?symbols=" + symbolsParam;

            log.debug("Fetching crypto snapshots for: {}", symbolsParam);

            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                fullUrl,
                HttpMethod.GET,
                entity,
                String.class
            );

            String responseBody = responseEntity.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Empty response for crypto snapshots");
                return result;
            }

            // Parse the response - crypto snapshots returns { "snapshots": { "BTC/USD": { "latestTrade": {...}, "prevDailyBar": {...} }, ... } }
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
            Map<String, Object> snapshots = (Map<String, Object>) response.get("snapshots");

            if (snapshots != null) {
                for (Map.Entry<String, Object> entry : snapshots.entrySet()) {
                    String cryptoPair = entry.getKey(); // e.g., "BTC/USD"
                    String symbol = cryptoPair.replace("/USD", ""); // Back to "BTC"
                    Map<String, Object> snapshotData = (Map<String, Object>) entry.getValue();

                    try {
                        // Get current price from latestTrade
                        Map<String, Object> latestTrade = (Map<String, Object>) snapshotData.get("latestTrade");
                        Map<String, Object> prevDailyBar = (Map<String, Object>) snapshotData.get("prevDailyBar");

                        if (latestTrade == null || prevDailyBar == null) {
                            log.warn("Missing latestTrade or prevDailyBar for crypto {}", symbol);
                            continue;
                        }

                        // Current price from latest trade
                        BigDecimal currentPrice = new BigDecimal(latestTrade.get("p").toString());
                        // Previous day's close for accurate daily change
                        BigDecimal prevClose = new BigDecimal(prevDailyBar.get("c").toString());

                        // Calculate daily change (current price vs previous close)
                        BigDecimal change = currentPrice.subtract(prevClose);
                        BigDecimal changePercent = prevClose.compareTo(BigDecimal.ZERO) != 0
                            ? change.multiply(new BigDecimal("100")).divide(prevClose, 2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                        result.put(symbol, new LatestQuote(symbol, currentPrice, change, changePercent));
                        log.debug("Crypto snapshot for {}: price={}, prevClose={}, change={}%",
                            symbol, currentPrice, prevClose, changePercent);

                    } catch (Exception e) {
                        log.warn("Failed to parse crypto snapshot for {}: {}", symbol, e.getMessage());
                    }
                }
            }

            log.info("Fetched {} crypto snapshots", result.size());
            return result;

        } catch (Exception e) {
            log.error("Error fetching crypto snapshots: {}", e.getMessage());
            throw new StrategizException(
                AlpacaErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Failed to fetch crypto snapshots"
            );
        }
    }

    /**
     * Simple data class for latest quote information
     */
    public static class LatestQuote {
        private final String symbol;
        private final BigDecimal price;
        private final BigDecimal change;
        private final BigDecimal changePercent;

        public LatestQuote(String symbol, BigDecimal price, BigDecimal change, BigDecimal changePercent) {
            this.symbol = symbol;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
        }

        public String getSymbol() { return symbol; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getChange() { return change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public boolean isPositive() { return change.compareTo(BigDecimal.ZERO) >= 0; }
    }
}
