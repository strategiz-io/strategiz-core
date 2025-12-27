package io.strategiz.client.yahoofinance.client;

import io.strategiz.client.base.http.BaseHttpClient;
import io.strategiz.client.yahoofinance.error.YahooFinanceErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Yahoo Finance API client for fetching real-time stock and cryptocurrency prices.
 * Operates as a pure DAO with no business logic.
 * Uses Spring RestClient (imperative) for HTTP communication.
 */
@Component
public class YahooFinanceClient extends BaseHttpClient {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceClient.class);
    private static final String MODULE_NAME = "YahooFinanceClient";

    @Value("${yahoofinance.api.url:https://query2.finance.yahoo.com}")
    private String apiUrl;

    public YahooFinanceClient() {
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
     * Fetch price data using v10 API.
     * 
     * @param symbols Comma-separated list of symbols
     * @return Raw API response as Map
     */
    public Map<String, Object> fetchQuotesV10(String symbols) {
        log.debug("Fetching quotes from v10 API for symbols: {}", symbols);
        
        String path = String.format("/v10/finance/quoteSummary/%s?modules=price&formatted=false", symbols);
        
        try {
            Map<String, Object> response = getRawApiResponse(path, Map.class);
            validateResponse(response);
            return response;
        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Client error: " + e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {
            handleServerError(e);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Server error: " + e.getStatusCode()
            );
        } catch (ResourceAccessException e) {
            log.error("Connection error to Yahoo Finance API: {}", e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_CONNECTION_ERROR,
                MODULE_NAME,
                e,
                "Cannot connect to Yahoo Finance"
            );
        } catch (RestClientException e) {
            log.error("Unexpected error calling Yahoo Finance API: {}", e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                e.getMessage()
            );
        }
    }
    
    /**
     * Fetch price data using v8 API (fallback).
     * 
     * @param symbols Comma-separated list of symbols
     * @return Raw API response as Map
     */
    public Map<String, Object> fetchQuotesV8(String symbols) {
        log.debug("Fetching quotes from v8 API for symbols: {}", symbols);
        
        String path = String.format("/v8/finance/quote?symbols=%s&fields=regularMarketPrice,price", symbols);
        
        try {
            Map<String, Object> response = getRawApiResponse(path, Map.class);
            validateResponse(response);
            return response;
        } catch (HttpClientErrorException e) {
            handleClientError(e);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Client error: " + e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {
            handleServerError(e);
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                "Server error: " + e.getStatusCode()
            );
        } catch (ResourceAccessException e) {
            log.error("Connection error to Yahoo Finance API: {}", e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_CONNECTION_ERROR,
                MODULE_NAME,
                e,
                "Cannot connect to Yahoo Finance"
            );
        } catch (RestClientException e) {
            log.error("Unexpected error calling Yahoo Finance API: {}", e.getMessage());
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                e,
                e.getMessage()
            );
        }
    }
    
    /**
     * Validate API response.
     */
    private void validateResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            throw new StrategizException(
                YahooFinanceErrorDetails.EMPTY_RESPONSE,
                MODULE_NAME,
                "Empty response from Yahoo Finance API"
            );
        }
        
        // Check for error messages in response
        if (response.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) response.get("error");
            String errorMessage = error != null ? error.toString() : "Unknown error";
            throw new StrategizException(
                YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                MODULE_NAME,
                errorMessage
            );
        }
        
        // Check for rate limit message
        if (response.containsKey("Information")) {
            String info = (String) response.get("Information");
            if (info != null && info.contains("rate limit")) {
                throw new StrategizException(
                    YahooFinanceErrorDetails.API_RATE_LIMIT,
                    MODULE_NAME,
                    info
                );
            }
        }
    }
    
    /**
     * Handle client errors (4xx).
     */
    private void handleClientError(HttpClientErrorException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Rate limit exceeded for Yahoo Finance API");
            throw new StrategizException(
                YahooFinanceErrorDetails.API_RATE_LIMIT,
                MODULE_NAME,
                e,
                "Rate limit exceeded"
            );
        } else if (status == HttpStatus.NOT_FOUND) {
            log.warn("Symbol not found in Yahoo Finance");
            throw new StrategizException(
                YahooFinanceErrorDetails.SYMBOL_NOT_FOUND,
                MODULE_NAME,
                e,
                "Symbol not found"
            );
        }
        
        log.error("Client error from Yahoo Finance API: {} - {}", status, e.getMessage());
    }
    
    /**
     * Handle server errors (5xx).
     */
    private void handleServerError(HttpServerErrorException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());

        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            log.error("Yahoo Finance API is unavailable");
            throw new StrategizException(
                YahooFinanceErrorDetails.API_CONNECTION_ERROR,
                MODULE_NAME,
                e,
                "Service unavailable"
            );
        } else if (status == HttpStatus.GATEWAY_TIMEOUT) {
            log.error("Yahoo Finance API timeout");
            throw new StrategizException(
                YahooFinanceErrorDetails.API_TIMEOUT,
                MODULE_NAME,
                e,
                "Gateway timeout"
            );
        }

        log.error("Server error from Yahoo Finance API: {} - {}", status, e.getMessage());
    }

    /**
     * Fetch raw price data for multiple symbols.
     * Returns raw API response - no business logic or transformation.
     *
     * @param symbols Comma-separated list of Yahoo Finance formatted symbols
     * @return Raw API response as Map
     */
    public Map<String, Object> fetchBulkQuotes(String symbols) {
        log.debug("Fetching quotes for symbols: {}", symbols);

        // Try v10 API first
        try {
            return fetchQuotesV10(symbols);
        } catch (Exception e) {
            log.warn("v10 API failed, trying v8 fallback: {}", e.getMessage());

            // Fallback to v8 API
            try {
                return fetchQuotesV8(symbols);
            } catch (Exception e2) {
                log.error("Both v10 and v8 APIs failed: {}", e2.getMessage());
                throw new StrategizException(
                    YahooFinanceErrorDetails.API_ERROR_RESPONSE,
                    MODULE_NAME,
                    e2,
                    "Failed to fetch quotes from Yahoo Finance"
                );
            }
        }
    }

    /**
     * Fetch raw price data for a single symbol.
     * Returns raw API response - no business logic or transformation.
     *
     * @param symbol Yahoo Finance formatted symbol (e.g., "BTC-USD", "AAPL")
     * @return Raw API response as Map
     */
    public Map<String, Object> fetchQuote(String symbol) {
        return fetchBulkQuotes(symbol);
    }
}