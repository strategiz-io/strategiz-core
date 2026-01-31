package io.strategiz.business.provider.alpaca.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Alpaca provider error definitions with HTTP status and property key mapping.
 */
public enum AlpacaProviderErrorDetails implements ErrorDetails {

	// OAuth & Authentication errors
	OAUTH_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-oauth-url-failed"),
	OAUTH_FLOW_FAILED(HttpStatus.BAD_GATEWAY, "alpaca-oauth-flow-failed"),
	TOKEN_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-token-storage-failed"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "alpaca-invalid-credentials"),

	// Integration errors
	INTEGRATION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-integration-creation-failed"),

	// Data sync errors
	TOKENS_NOT_FOUND(HttpStatus.NOT_FOUND, "alpaca-tokens-not-found"),
	ACCESS_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "alpaca-access-token-not-found"),
	SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-sync-failed"),
	NO_DATA_AFTER_SYNC(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-no-data-after-sync"),

	// API errors
	PORTFOLIO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "alpaca-portfolio-fetch-failed"),

	// General errors
	PROVIDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "alpaca-provider-error");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	AlpacaProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
