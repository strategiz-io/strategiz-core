package io.strategiz.framework.exception;

import org.springframework.http.HttpStatus;

/**
 * Interface for error enums to provide structured error information.
 *
 * <p>
 * This interface allows error enums to define: - HTTP status codes - Property key for
 * message. resolution.
 *
 * <p>
 * Usage: public enum AuthErrors implements ErrorDetails {.
 * INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid-credentials"),.
 * SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "session-expired"); }.
 */
public interface ErrorDetails {

	/**
	 * Get the HTTP status code for this error.
	 * @return Appropriate HTTP status.
	 */
	HttpStatus getHttpStatus();

	/**
	 * Get the property key for message resolution.
	 * @return Property key (e.g., "invalid-credentials").
	 */
	String getPropertyKey();

	/**
	 * Get the error code (enum name).
	 * @return Error code string.
	 */
	default String getCode() {
		return ((Enum<?>) this).name();
	}

}
