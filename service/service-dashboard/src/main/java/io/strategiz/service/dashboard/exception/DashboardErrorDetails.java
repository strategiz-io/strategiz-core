package io.strategiz.service.dashboard.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Dashboard service error definitions with HTTP status and property key mapping.
 * 
 * Each error maps to properties in dashboard-errors.properties file which contains:
 * - Error code
 * - User-friendly message  
 * - Developer message template with placeholders
 * - Documentation path
 * 
 * Usage:
 * throw new StrategizException(DashboardErrorDetails.PORTFOLIO_NOT_FOUND, "service-dashboard", portfolioId);
 * throw new StrategizException(DashboardErrorDetails.METRICS_CALCULATION_FAILED, "service-dashboard", "ROI", "30D", "Division by zero");
 */
public enum DashboardErrorDetails implements ErrorDetails {
    
    // Portfolio errors
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "portfolio-not-found"),
    PORTFOLIO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "portfolio-access-denied"),
    PORTFOLIO_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-calculation-failed"),
    
    // Asset allocation errors
    ASSET_ALLOCATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "asset-allocation-failed"),
    INVALID_ALLOCATION_PERCENTAGE(HttpStatus.BAD_REQUEST, "invalid-allocation-percentage"),
    
    // Market sentiment errors
    MARKET_DATA_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "market-data-unavailable"),
    SENTIMENT_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "sentiment-calculation-failed"),
    
    // Performance metrics errors
    METRICS_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "metrics-calculation-failed"),
    INSUFFICIENT_DATA(HttpStatus.BAD_REQUEST, "insufficient-data"),
    INVALID_TIME_PERIOD(HttpStatus.BAD_REQUEST, "invalid-time-period"),
    
    // Risk analysis errors
    RISK_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "risk-calculation-failed"),
    INVALID_RISK_PARAMETERS(HttpStatus.BAD_REQUEST, "invalid-risk-parameters"),
    
    // Watchlist errors
    WATCHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "watchlist-not-found"),
    WATCHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "watchlist-item-not-found"),
    WATCHLIST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "watchlist-limit-exceeded"),
    DUPLICATE_WATCHLIST_ITEM(HttpStatus.CONFLICT, "duplicate-watchlist-item"),
    
    // Data source errors
    DATA_SOURCE_ERROR(HttpStatus.BAD_GATEWAY, "data-source-error"),
    API_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "api-rate-limit-exceeded"),
    
    // Validation errors
    INVALID_SYMBOL(HttpStatus.BAD_REQUEST, "invalid-symbol"),
    INVALID_CURRENCY(HttpStatus.BAD_REQUEST, "invalid-currency"),
    INVALID_PORTFOLIO_DATA(HttpStatus.BAD_REQUEST, "invalid-portfolio-data"),
    
    // Cache errors
    CACHE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "cache-error"),
    
    // Configuration errors
    CONFIGURATION_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "configuration-missing"),
    
    // General dashboard errors
    DASHBOARD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "dashboard-error");
    
    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    DashboardErrorDetails(HttpStatus httpStatus, String propertyKey) {
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