package io.strategiz.framework.secrets.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error codes for secrets framework operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(SecretsErrors.VAULT_CONNECTION_FAILED, MODULE_NAME);
 */
public enum SecretsErrors implements ErrorDetails {

	VAULT_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "vault-connection-failed"),
	VAULT_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "vault-authentication-failed"),
	VAULT_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "vault-read-failed"),
	VAULT_WRITE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "vault-write-failed"),
	VAULT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "vault-delete-failed"),
	SECRET_NOT_FOUND(HttpStatus.NOT_FOUND, "secret-not-found"),
	SECRET_ACCESS_DENIED(HttpStatus.FORBIDDEN, "secret-access-denied"),
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "secrets-configuration-error"),
	ENCRYPTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "encryption-error"),
	DECRYPTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "decryption-error"),
	INVALID_SECRET_KEY(HttpStatus.BAD_REQUEST, "invalid-secret-key"),
	SECRET_EXPIRED(HttpStatus.GONE, "secret-expired"),
	OPERATION_NOT_SUPPORTED(HttpStatus.NOT_IMPLEMENTED, "secrets-operation-not-supported");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	SecretsErrors(HttpStatus httpStatus, String propertyKey) {
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
 