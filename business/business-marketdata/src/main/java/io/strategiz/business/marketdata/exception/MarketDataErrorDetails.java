package io.strategiz.business.marketdata.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Market data business error definitions with HTTP status and property key mapping.
 */
public enum MarketDataErrorDetails implements ErrorDetails {

    // Collection errors
    SYMBOL_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketdata-symbol-processing-failed"),
    BACKFILL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketdata-backfill-failed"),

    // Data retrieval errors
    DATA_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "marketdata-fetch-failed"),

    // Storage errors
    DATA_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "marketdata-storage-failed"),

    // General errors
    MARKETDATA_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "marketdata-general-error");

    private final HttpStatus httpStatus;
    private final String propertyKey;

    MarketDataErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
