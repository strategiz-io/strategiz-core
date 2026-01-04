package io.strategiz.service.labs.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Strategy Service error details enum that maps to service-strategy-errors.properties.
 * Each enum constant references a property key in the properties file.
 * 
 * The actual error messages (code, message, developer, more-info) are defined
 * in the properties file for easy modification without code changes.
 */
public enum ServiceStrategyErrorDetails implements ErrorDetails {
    
    // Strategy CRUD Errors
    STRATEGY_NOT_FOUND(HttpStatus.BAD_REQUEST, "strategy-not-found"),
    STRATEGY_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-creation-failed"),
    STRATEGY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-update-failed"),
    STRATEGY_DELETION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-deletion-failed"),
    
    // Validation Errors
    STRATEGY_INVALID_LANGUAGE(HttpStatus.BAD_REQUEST, "strategy-invalid-language"),
    STRATEGY_INVALID_TYPE(HttpStatus.BAD_REQUEST, "strategy-invalid-type"),
    STRATEGY_INVALID_STATUS(HttpStatus.BAD_REQUEST, "strategy-invalid-status"),
    INVALID_STATUS_COMBINATION(HttpStatus.BAD_REQUEST, "invalid-status-combination"),
    STRATEGY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "strategy-name-required"),
    STRATEGY_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "strategy-code-required"),
    STRATEGY_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "strategy-name-too-long"),
    STRATEGY_DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "strategy-description-too-long"),
    STRATEGY_CODE_TOO_LONG(HttpStatus.BAD_REQUEST, "strategy-code-too-long"),
    STRATEGY_TOO_MANY_TAGS(HttpStatus.BAD_REQUEST, "strategy-too-many-tags"),
    STRATEGY_INVALID_PERFORMANCE(HttpStatus.BAD_REQUEST, "strategy-invalid-performance"),
    STRATEGY_MISSING_PERFORMANCE(HttpStatus.BAD_REQUEST, "strategy-missing-performance"),
    DUPLICATE_STRATEGY_NAME(HttpStatus.CONFLICT, "strategy.duplicate-name"),
    DUPLICATE_PUBLISHED_NAME(HttpStatus.CONFLICT, "strategy.duplicate-published-name"),

    // Execution Errors
    STRATEGY_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-execution-failed"),
    STRATEGY_BACKTEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-backtest-failed"),
    STRATEGY_COMPILATION_FAILED(HttpStatus.BAD_REQUEST, "strategy-compilation-failed"),
    MARKET_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "market-data-not-found"),
    MARKET_DATA_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "market-data-fetch-failed"),
    
    // Permission Errors
    STRATEGY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "strategy-access-denied"),
    STRATEGY_MODIFICATION_DENIED(HttpStatus.FORBIDDEN, "strategy-modification-denied"),
    STRATEGY_HAS_SUBSCRIBERS(HttpStatus.CONFLICT, "strategy-has-subscribers"),
    
    // Database Errors
    STRATEGY_DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-database-error"),
    
    // Performance Errors
    STRATEGY_PERFORMANCE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-performance-update-failed"),

    // Fetch Errors
    STRATEGY_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-fetch-failed"),

    // Versioning Errors
    STRATEGY_VERSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "strategy-version-failed"),
    STRATEGY_VERSION_INVALID_STATE(HttpStatus.BAD_REQUEST, "strategy-version-invalid-state");

    private final HttpStatus httpStatus;
    private final String propertyKey;
    
    ServiceStrategyErrorDetails(HttpStatus httpStatus, String propertyKey) {
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