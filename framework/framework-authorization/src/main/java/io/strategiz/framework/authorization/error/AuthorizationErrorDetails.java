package io.strategiz.framework.authorization.error;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for authorization failures.
 *
 * <p>
 * Uses generic messages to avoid leaking security-sensitive information. Detailed
 * information is logged server-side for debugging.
 *
 * @see io.strategiz.framework.exception.StrategizException
 */
public enum AuthorizationErrorDetails implements ErrorDetails {

	// Authentication errors (401 Unauthorized)

	/** No valid authentication token provided. */
	NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "not-authenticated"),

	/** Token validation failed (invalid signature, format, etc.). */
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid-token"),

	/** Token has expired. */
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token-expired"),

	// Authorization errors (403 Forbidden)

	/**
	 * User lacks required scope(s) for this action. Note: Do NOT expose which scopes are
	 * required in the error message.
	 */
	INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "insufficient-permissions"),

	/**
	 * User lacks relationship-based access to the resource. Note: Do NOT expose which
	 * resource or relation in the error message.
	 */
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "access-denied"),

	/** User's authentication level (ACR) is insufficient. */
	AUTH_LEVEL_REQUIRED(HttpStatus.FORBIDDEN, "auth-level-required"),

	/** Action not permitted in demo mode. */
	DEMO_MODE_RESTRICTED(HttpStatus.FORBIDDEN, "demo-mode-restricted"),

	// Configuration errors (500 Internal Server Error)

	/** Token configuration error (missing or invalid keys). */
	CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "auth-configuration-error"),

	/** Invalid argument provided to authorization component. */
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "auth-invalid-argument");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	AuthorizationErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
