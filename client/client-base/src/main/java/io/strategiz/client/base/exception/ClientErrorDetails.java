package io.strategiz.client.base.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for generic client operations. Implements ErrorDetails for integration
 * with the Strategiz exception framework. This can be used by any client module that
 * doesn't have its own specific error codes.
 *
 * Usage: throw new StrategizException(ClientErrorDetails.API_ERROR, MODULE_NAME);
 */
public enum ClientErrorDetails implements ErrorDetails {

	// === Connection/Authentication Errors ===
	API_ERROR(HttpStatus.BAD_GATEWAY, "client-api-error"), AUTH_FAILED(HttpStatus.UNAUTHORIZED, "client-auth-failed"),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "client-invalid-credentials"),
	MISSING_CREDENTIALS(HttpStatus.BAD_REQUEST, "client-missing-credentials"),

	// === Response Errors ===
	INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "client-invalid-response"),
	EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "client-empty-response"),
	RESPONSE_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "client-response-parse-failed"),

	// === Rate Limiting ===
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "client-rate-limited"),

	// === Token Errors ===
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "client-token-expired"),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "client-invalid-token"),

	// === Network/Service Errors ===
	NETWORK_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "client-network-error"),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "client-service-unavailable"),
	REQUEST_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "client-request-timeout"),

	// === Configuration Errors ===
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "client-configuration-error"),
	NOT_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "client-not-initialized"),
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "client-invalid-argument"),

	// === Signature Errors ===
	SIGNATURE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "client-signature-failed"),

	// === Data Errors ===
	DATA_RETRIEVAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "client-data-retrieval-failed"),
	SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "client-search-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	ClientErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
