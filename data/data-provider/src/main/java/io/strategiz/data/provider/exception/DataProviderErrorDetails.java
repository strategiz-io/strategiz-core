package io.strategiz.data.provider.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details enum for data-provider module.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 */
public enum DataProviderErrorDetails implements ErrorDetails {

	// Provider errors
	PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "provider-not-found"),
	PROVIDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "provider-already-exists"),
	PROVIDER_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "provider-invalid-credentials"),
	PROVIDER_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "provider-connection-failed"),
	PROVIDER_OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "provider-oauth-failed"),
	PROVIDER_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "provider-update-failed"),
	PROVIDER_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "provider-delete-failed"),
	PROVIDER_INVALID_TYPE(HttpStatus.BAD_REQUEST, "provider-invalid-type"),
	PROVIDER_INVALID_STATUS(HttpStatus.BAD_REQUEST, "provider-invalid-status"),
	PROVIDER_SYNC_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "provider-sync-failed"),

	// Repository errors
	REPOSITORY_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-save-failed"),
	REPOSITORY_FIND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-find-failed"),
	REPOSITORY_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-delete-failed"),
	REPOSITORY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "repository-update-failed"),

	// Validation errors
	USER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "validation-user-required"),
	PROVIDER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "validation-provider-required"),
	CREDENTIALS_REQUIRED(HttpStatus.BAD_REQUEST, "validation-credentials-required"),
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "validation-invalid-input"),
	AUDIT_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "validation-audit-fields-missing"),
	ENTITY_EXISTENCE_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "entity-existence-check-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	DataProviderErrorDetails(HttpStatus httpStatus, String propertyKey) {
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