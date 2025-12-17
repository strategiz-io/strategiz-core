package io.strategiz.business.portfolio.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Portfolio business error definitions with HTTP status and property key mapping.
 */
public enum PortfolioErrorDetails implements ErrorDetails {

    // Data retrieval errors
    PORTFOLIO_DATA_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-data-retrieval-failed"),
    PROVIDER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "portfolio-provider-data-not-found"),

    // Calculation errors
    METRICS_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-metrics-calculation-failed"),
    AGGREGATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-aggregation-failed"),

    // General errors
    PORTFOLIO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "portfolio-general-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    PortfolioErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
