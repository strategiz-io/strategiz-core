package io.strategiz.business.fundamentals.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for fundamentals business logic operations.
 *
 * Covers errors related to:
 * - Fundamentals collection and batch processing
 * - Data validation and storage
 * - Symbol lookup and queries
 */
public enum FundamentalsErrorDetails implements ErrorDetails {

	// Collection Errors
	COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "fundamentals-collection-failed"),

	SYMBOL_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "fundamentals-symbol-collection-failed"),

	BATCH_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "fundamentals-batch-timeout"),

	// Data Errors
	NO_FUNDAMENTALS_FOUND(HttpStatus.NOT_FOUND, "fundamentals-not-found"),

	INVALID_PERIOD_TYPE(HttpStatus.BAD_REQUEST, "fundamentals-invalid-period-type"),

	MISSING_REQUIRED_DATA(HttpStatus.UNPROCESSABLE_ENTITY, "fundamentals-missing-required-data"),

	// Storage Errors
	SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "fundamentals-save-failed"),

	// Query Errors
	INVALID_QUERY_PARAMETERS(HttpStatus.BAD_REQUEST, "fundamentals-invalid-query-params"),

	QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "fundamentals-query-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	FundamentalsErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
