package io.strategiz.service.portfolio.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for portfolio service
 */
public enum ServicePortfolioErrorDetails implements ErrorDetails {
    
    // Portfolio errors
    PORTFOLIO_NOT_FOUND(HttpStatus.NOT_FOUND, "portfolio-not-found"),
    PORTFOLIO_DATA_FETCH_ERROR(HttpStatus.BAD_GATEWAY, "portfolio-data-fetch-error"),
    PORTFOLIO_CALCULATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-calculation-error"),
    
    // Provider errors
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "provider-not-found"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),
    PROVIDER_CONNECTION_ERROR(HttpStatus.BAD_GATEWAY, "provider-connection-error"),
    
    // Data errors
    INVALID_DATA_FORMAT(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-data-format"),
    DATA_SYNC_ERROR(HttpStatus.BAD_GATEWAY, "data-sync-error"),
    
    // Validation errors
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid-request"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "missing-required-field");
    
    private final HttpStatus httpStatus;
    private final String messageKey;
    
    ServicePortfolioErrorDetails(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
    
    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    @Override
    public String getPropertyKey() {
        return "service.portfolio.error." + messageKey;
    }
}