package io.strategiz.framework.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all Strategiz application exceptions. Enforces the use of enum
 * error. codes for consistency.
 *
 * <p>
 * Enhanced to support ErrorDetails interface for structured error information.
 */
public class StrategizException extends RuntimeException {

	private final String errorCode;

	private final ErrorDetails errorDetails;

	private final String moduleName;

	private final Object[] args;

	private final HttpStatus httpStatus;

	/** Create exception with ErrorDetails enum (recommended approach). */
	public StrategizException(ErrorDetails errorDetails, String moduleName, Object... args) {
		super(errorDetails.getCode());
		this.errorCode = errorDetails.getCode();
		this.errorDetails = errorDetails;
		this.moduleName = moduleName;
		this.args = args;
		this.httpStatus = errorDetails.getHttpStatus();
	}

	/** Create exception with ErrorDetails enum and cause. */
	public StrategizException(ErrorDetails errorDetails, String moduleName, Throwable cause, Object... args) {
		super(errorDetails.getCode(), cause);
		this.errorCode = errorDetails.getCode();
		this.errorDetails = errorDetails;
		this.moduleName = moduleName;
		this.args = args;
		this.httpStatus = errorDetails.getHttpStatus();
	}

	/** Create exception with basic enum error code (legacy support). */
	public StrategizException(Enum<?> errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode.name();
		this.errorDetails = null;
		this.moduleName = "unknown";
		this.args = new Object[0];
		this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Create exception with basic enum error code and custom message (legacy support).
	 */
	public StrategizException(Enum<?> errorCode, String message) {
		super(message);
		this.errorCode = errorCode.name();
		this.errorDetails = null;
		this.moduleName = "unknown";
		this.args = new Object[] { message };
		this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Create exception with basic enum error code, message, and cause (legacy support).
	 */
	public StrategizException(Enum<?> errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode.name();
		this.errorDetails = null;
		this.moduleName = "unknown";
		this.args = new Object[] { message };
		this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/** Create exception with basic enum error code and cause (legacy support). */
	public StrategizException(Enum<?> errorCode, Throwable cause) {
		super(errorCode.name(), cause);
		this.errorCode = errorCode.name();
		this.errorDetails = null;
		this.moduleName = "unknown";
		this.args = new Object[0];
		this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
	}

	// Getters
	public String getErrorCode() {
		return errorCode;
	}

	public ErrorDetails getErrorDetails() {
		return errorDetails;
	}

	public String getModuleName() {
		return moduleName;
	}

	public Object[] getArgs() {
		return args;
	}

	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	/** Check if this exception was created with ErrorDetails. */
	public boolean hasErrorDetails() {
		return errorDetails != null;
	}

}
