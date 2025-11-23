package io.strategiz.client.yahoofinance.client;

import io.strategiz.client.base.http.BaseHttpClient;
import io.strategiz.client.yahoofinance.exception.YahooFinanceErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Yahoo Finance Historical Data Client
 * Fetches historical OHLCV (Open, High, Low, Close, Volume) data for backtesting
 *
 * Unlike the real-time quote client, this fetches historical daily data going back years
 * Uses Yahoo Finance's chart API endpoint which is free and has no strict rate limits
 */
@Component
public class YahooFinanceHistoricalClient extends BaseHttpClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceHistoricalClient.class);
    private static final String MODULE_NAME = "YahooFinanceHistoricalClient";

    @Value("${yahoofinance.api.url:https://query2.finance.yahoo.com}")
    private String apiUrl;

    @Value("${yahoo.batch.delay-ms:100}")
    private int delayMs;

    public YahooFinanceHistoricalClient() {
        super("https://query2.finance.yahoo.com");
    }

    @Override
    protected void configureDefaultHeaders(HttpHeaders headers) {
        super.configureDefaultHeaders(headers);
        headers.add("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        headers.add("Accept", "*/*");
        headers.add("Accept-Language", "en-US,en;q=0.9");
        headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("Referer", "https://finance.yahoo.com/");
        headers.add("Origin", "https://finance.yahoo.com");
        headers.add("Cache-Control", "no-cache");
    }

    /**
     * Fetch historical data for a single symbol
     *
     * @param symbol Stock symbol (e.g., "AAPL", "TSLA", "BTC-USD")
     * @param from Start date (inclusive)
     * @param to End date (inclusive)
     * @param interval Time interval ("1d" for daily, "1wk" for weekly, "1mo" for monthly)
     * @return List of historical data points
     */
    public List<HistoricalDataPoint> getHistoricalData(String symbol, LocalDate from, LocalDate to, String interval) {
        log.debug("Fetching historical data for {} from {} to {} (interval: {})", symbol, from, to, interval);

        // Convert LocalDate to Unix timestamps (seconds since epoch)
        long period1 = from.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond();
        long period2 = to.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond() + 86400; // Add 1 day to include end date

        String path = String.format("/v8/finance/chart/%s?period1=%d&period2=%d&interval=%s&events=history",
                                    symbol, period1, period2, interval);

        try {
            // Rate limiting - sleep to avoid overwhelming the API
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }

            Map<String, Object> response = getRawApiResponse(path, Map.class);
            validateResponse(response, symbol);

            List<HistoricalDataPoint> dataPoints = parseHistoricalData(response, symbol);

            log.info("Fetched {} historical data points for {}", dataPoints.size(), symbol);
            return dataPoints;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while fetching data for {}", symbol);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Thread interrupted"
            );
        } catch (HttpClientErrorException e) {
            handleClientError(e, symbol);
            return Collections.emptyList(); // Won't reach here due to exception throw
        } catch (HttpServerErrorException e) {
            handleServerError(e, symbol);
            return Collections.emptyList(); // Won't reach here due to exception throw
        } catch (ResourceAccessException e) {
            log.error("Connection error to Yahoo Finance API for {}: {}", symbol, e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_CONNECTION_ERROR,
                MODULE_NAME,
                e,
                String.format("Cannot connect to Yahoo Finance for %s", symbol)
            );
        } catch (RestClientException e) {
            log.error("Unexpected error calling Yahoo Finance API for {}: {}", symbol, e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                e.getMessage()
            );
        }
    }

    /**
     * Fetch historical data for multiple symbols in bulk
     * This is more efficient than calling getHistoricalData() in a loop
     * as it allows for better rate limiting management
     *
     * @param symbols List of stock symbols
     * @param from Start date
     * @param to End date
     * @return Map of symbol to historical data points
     */
    public Map<String, List<HistoricalDataPoint>> getBulkHistoricalData(List<String> symbols, LocalDate from, LocalDate to) {
        log.info("Fetching bulk historical data for {} symbols from {} to {}", symbols.size(), from, to);

        Map<String, List<HistoricalDataPoint>> results = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;

        for (String symbol : symbols) {
            try {
                List<HistoricalDataPoint> data = getHistoricalData(symbol, from, to, "1d");
                results.put(symbol, data);
                successCount++;

                if (successCount % 10 == 0) {
                    log.info("Progress: {}/{} symbols processed", successCount, symbols.size());
                }

            } catch (Exception e) {
                log.error("Error fetching data for {}: {}", symbol, e.getMessage());
                errorCount++;
                // Continue with next symbol instead of failing entire bulk operation
            }
        }

        log.info("Bulk fetch completed: {} successful, {} errors", successCount, errorCount);
        return results;
    }

    /**
     * Parse the Yahoo Finance response into our HistoricalDataPoint format
     */
    @SuppressWarnings("unchecked")
    private List<HistoricalDataPoint> parseHistoricalData(Map<String, Object> response, String symbol) {
        try {
            Map<String, Object> chart = (Map<String, Object>) response.get("chart");
            if (chart == null) {
                log.warn("No chart data in response for {}", symbol);
                return Collections.emptyList();
            }

            List<Map<String, Object>> resultList = (List<Map<String, Object>>) chart.get("result");
            if (resultList == null || resultList.isEmpty()) {
                log.warn("No result data in chart for {}", symbol);
                return Collections.emptyList();
            }

            Map<String, Object> result = resultList.get(0);

            // Get timestamps array
            List<Long> timestamps = (List<Long>) result.get("timestamp");
            if (timestamps == null || timestamps.isEmpty()) {
                log.warn("No timestamp data for {}", symbol);
                return Collections.emptyList();
            }

            // Get quote data (OHLCV)
            Map<String, Object> indicators = (Map<String, Object>) result.get("indicators");
            if (indicators == null) {
                log.warn("No indicators data for {}", symbol);
                return Collections.emptyList();
            }

            List<Map<String, Object>> quoteList = (List<Map<String, Object>>) indicators.get("quote");
            if (quoteList == null || quoteList.isEmpty()) {
                log.warn("No quote data for {}", symbol);
                return Collections.emptyList();
            }

            Map<String, Object> quote = quoteList.get(0);
            List<Object> opens = (List<Object>) quote.get("open");
            List<Object> highs = (List<Object>) quote.get("high");
            List<Object> lows = (List<Object>) quote.get("low");
            List<Object> closes = (List<Object>) quote.get("close");
            List<Object> volumes = (List<Object>) quote.get("volume");

            // Build data points
            List<HistoricalDataPoint> dataPoints = new ArrayList<>();

            for (int i = 0; i < timestamps.size(); i++) {
                try {
                    HistoricalDataPoint point = new HistoricalDataPoint();
                    point.symbol = symbol;
                    point.timestamp = timestamps.get(i);
                    point.date = LocalDate.ofInstant(
                        Instant.ofEpochSecond(timestamps.get(i)),
                        ZoneId.of("America/New_York")
                    );

                    // Yahoo Finance returns null for missing data points
                    if (opens.get(i) != null) point.open = convertToBigDecimal(opens.get(i));
                    if (highs.get(i) != null) point.high = convertToBigDecimal(highs.get(i));
                    if (lows.get(i) != null) point.low = convertToBigDecimal(lows.get(i));
                    if (closes.get(i) != null) point.close = convertToBigDecimal(closes.get(i));
                    if (volumes.get(i) != null) point.volume = convertToBigDecimal(volumes.get(i));

                    // Only add if we have at least OHLC data
                    if (point.isValid()) {
                        dataPoints.add(point);
                    } else {
                        log.debug("Skipping invalid data point for {} at index {}", symbol, i);
                    }

                } catch (Exception e) {
                    log.warn("Error parsing data point {} for {}: {}", i, symbol, e.getMessage());
                    // Skip this data point and continue
                }
            }

            return dataPoints;

        } catch (Exception e) {
            log.error("Error parsing historical data for {}: {}", symbol, e.getMessage(), e);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                String.format("Failed to parse data for %s", symbol)
            );
        }
    }

    /**
     * Convert various number types to BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return null;

        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }

        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("Could not convert string to BigDecimal: {}", value);
                return null;
            }
        }

        log.warn("Unexpected value type: {}", value.getClass());
        return null;
    }

    /**
     * Validate API response
     */
    @SuppressWarnings("unchecked")
    private void validateResponse(Map<String, Object> response, String symbol) {
        if (response == null || response.isEmpty()) {
            throw new StrategizException(
                YahooFinanceErrorDetails.EMPTY_RESPONSE,
                MODULE_NAME,
                String.format("Empty response from Yahoo Finance API for %s", symbol)
            );
        }

        // Check for error in response
        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            String errorMessage = error != null ? error.toString() : "Unknown error";
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                String.format("Error in response for %s: %s", symbol, errorMessage)
            );
        }

        // Check for rate limit message
        if (response.containsKey("Information")) {
            String info = (String) response.get("Information");
            if (info != null && info.contains("rate limit")) {
                throw new StrategizException(
                    YahooFinanceErrorDetails.API_RATE_LIMIT,
                    MODULE_NAME,
                    String.format("Rate limit hit for %s: %s", symbol, info)
                );
            }
        }
    }

    /**
     * Handle client errors (4xx)
     */
    private void handleClientError(HttpClientErrorException e, String symbol) {
        if (e.getStatusCode().value() == 429) {
            log.warn("Rate limit exceeded for Yahoo Finance API (symbol: {})", symbol);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_RATE_LIMIT,
                MODULE_NAME,
                e,
                String.format("Rate limit exceeded for %s", symbol)
            );
        } else if (e.getStatusCode().value() == 404) {
            log.warn("Symbol not found in Yahoo Finance: {}", symbol);
            throw new StrategizException(
                YahooFinanceErrorDetails.SYMBOL_NOT_FOUND,
                MODULE_NAME,
                e,
                String.format("Symbol not found: %s", symbol)
            );
        }

        log.error("Client error from Yahoo Finance API for {}: {} - {}",
                  symbol, e.getStatusCode(), e.getMessage());
        throw new StrategizException(
            YahooFinanceErrorDetails.API_ERROR_RESPONSE,
            MODULE_NAME,
            e,
            String.format("Client error for %s: %s", symbol, e.getStatusCode())
        );
    }

    /**
     * Handle server errors (5xx)
     */
    private void handleServerError(HttpServerErrorException e, String symbol) {
        if (e.getStatusCode().value() == 503) {
            log.error("Yahoo Finance API is unavailable for {}", symbol);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_CONNECTION_ERROR,
                MODULE_NAME,
                e,
                String.format("Service unavailable for %s", symbol)
            );
        } else if (e.getStatusCode().value() == 504) {
            log.error("Yahoo Finance API timeout for {}", symbol);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_TIMEOUT,
                MODULE_NAME,
                e,
                String.format("Gateway timeout for %s", symbol)
            );
        }

        log.error("Server error from Yahoo Finance API for {}: {} - {}",
                  symbol, e.getStatusCode(), e.getMessage());
        throw new StrategizException(
            YahooFinanceErrorDetails.API_ERROR_RESPONSE,
            MODULE_NAME,
            e,
            String.format("Server error for %s: %s", symbol, e.getStatusCode())
        );
    }

    /**
     * Historical data point representing a single OHLCV candle
     */
    public static class HistoricalDataPoint {
        public String symbol;
        public LocalDate date;
        public BigDecimal open;
        public BigDecimal high;
        public BigDecimal low;
        public BigDecimal close;
        public BigDecimal volume;
        public Long timestamp;

        /**
         * Check if this data point has valid OHLC data
         */
        public boolean isValid() {
            return symbol != null &&
                   date != null &&
                   open != null &&
                   high != null &&
                   low != null &&
                   close != null &&
                   open.compareTo(BigDecimal.ZERO) > 0 &&
                   high.compareTo(BigDecimal.ZERO) > 0 &&
                   low.compareTo(BigDecimal.ZERO) > 0 &&
                   close.compareTo(BigDecimal.ZERO) > 0;
        }

        @Override
        public String toString() {
            return String.format("%s %s: O=%.2f H=%.2f L=%.2f C=%.2f V=%s",
                symbol, date, open, high, low, close, volume);
        }
    }
}
