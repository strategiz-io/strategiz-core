package io.strategiz.client.fmp.error;

import io.strategiz.framework.exception.ErrorDetails;

/**
 * Error details for FMP API errors.
 */
public enum FmpErrorDetails implements ErrorDetails {

	API_KEY_MISSING("FMP_API_KEY_MISSING", "FMP API key is not configured"),
	API_ERROR_RESPONSE("FMP_API_ERROR_RESPONSE", "FMP API returned an error response"),
	RATE_LIMIT_EXCEEDED("FMP_RATE_LIMIT_EXCEEDED", "Rate limit exceeded for FMP API"),
	NO_DATA_AVAILABLE("FMP_NO_DATA_AVAILABLE", "No fundamentals data available for symbol"),
	INVALID_SYMBOL("FMP_INVALID_SYMBOL", "Invalid or empty symbol provided"),
	PARSING_ERROR("FMP_PARSING_ERROR", "Failed to parse FMP API response");

	private final String propertyKey;

	private final String defaultMessage;

	FmpErrorDetails(String propertyKey, String defaultMessage) {
		this.propertyKey = propertyKey;
		this.defaultMessage = defaultMessage;
	}

	@Override
	public String getPropertyKey() {
		return propertyKey;
	}

	@Override
	public String getDefaultMessage() {
		return defaultMessage;
	}

}
