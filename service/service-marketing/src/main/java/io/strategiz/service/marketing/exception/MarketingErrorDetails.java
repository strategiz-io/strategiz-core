package io.strategiz.service.marketing.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Marketing service error definitions with HTTP status and property key mapping.
 * 
 * Each error maps to properties in marketing-errors.properties file which contains:
 * - Error code
 * - User-friendly message  
 * - Developer message template with placeholders
 * - Documentation path
 * 
 * Usage:
 * throw new StrategizException(MarketingErrorDetails.MARKET_DATA_UNAVAILABLE, "service-marketing", symbol);
 * throw new StrategizException(MarketingErrorDetails.API_RATE_LIMIT_EXCEEDED, "service-marketing", "CoinGecko", "60", "requests per minute");
 */
public enum MarketingErrorDetails implements ErrorDetails {
    
    // Market data errors
    MARKET_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "market-data-unavailable"),
    INVALID_SYMBOL(HttpStatus.BAD_REQUEST, "invalid-symbol"),
    SYMBOL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "symbol-not-supported"),
    
    // API provider errors
    API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "api-rate-limit-exceeded"),
    API_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "api-connection-failed"),
    API_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "api-response-invalid"),
    
    // Ticker service errors
    TICKER_DATA_STALE(HttpStatus.SERVICE_UNAVAILABLE, "ticker-data-stale"),
    TICKER_CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ticker-cache-error"),
    TICKER_AGGREGATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ticker-aggregation-failed"),
    
    // Data source errors
    COINBASE_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "coinbase-connection-failed"),
    COINGECKO_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "coingecko-connection-failed"),
    ALPHAVANTAGE_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "alphavantage-connection-failed"),
    
    // Price calculation errors
    PRICE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "price-calculation-failed"),
    CHANGE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "change-calculation-failed"),
    PERCENTAGE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "percentage-calculation-failed"),
    
    // Validation errors
    INVALID_PRICE_DATA(HttpStatus.BAD_REQUEST, "invalid-price-data"),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "invalid-time-range"),
    INVALID_CURRENCY(HttpStatus.BAD_REQUEST, "invalid-currency"),
    
    // Configuration errors
    CONFIGURATION_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "configuration-missing"),
    API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "api-key-missing"),
    
    // General marketing errors
    MARKETING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "marketing-error");
    
    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    MarketingErrorDetails(HttpStatus httpStatus, String propertyKey) {
        this.httpStatus = httpStatus;
        this.propertyKey = propertyKey;
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    @Override
    public String getPropertyKey() {
        return propertyKey;
    }
}