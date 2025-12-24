package io.strategiz.data.base.exception;

import io.strategiz.framework.exception.StrategizException;

/**
 * Exception class for all data repository operations.
 * Extends StrategizException for integration with the global exception handling framework.
 *
 * Usage:
 * throw new DataRepositoryException(DataRepositoryErrorDetails.ENTITY_NOT_FOUND, "UserEntity", userId);
 */
public class DataRepositoryException extends StrategizException {

	private static final String MODULE_NAME = "data-repository";

	/**
	 * Create exception with error details and optional arguments for message formatting.
	 * @param errorDetails The error details enum value
	 * @param args Optional arguments for message formatting (e.g., entity name, ID)
	 */
	public DataRepositoryException(DataRepositoryErrorDetails errorDetails, Object... args) {
		super(errorDetails, MODULE_NAME, args);
	}

	/**
	 * Create exception with error details, cause, and optional arguments.
	 * @param errorDetails The error details enum value
	 * @param cause The underlying cause
	 * @param args Optional arguments for message formatting
	 */
	public DataRepositoryException(DataRepositoryErrorDetails errorDetails, Throwable cause, Object... args) {
		super(errorDetails, MODULE_NAME, cause, args);
	}

	/**
	 * Create exception with custom module name for more specific error tracking.
	 * @param errorDetails The error details enum value
	 * @param moduleName Custom module name (e.g., "user-repository", "provider-repository")
	 * @param args Optional arguments for message formatting
	 */
	public DataRepositoryException(DataRepositoryErrorDetails errorDetails, String moduleName, Object... args) {
		super(errorDetails, moduleName, args);
	}

	/**
	 * Create exception with custom module name, cause, and optional arguments.
	 * @param errorDetails The error details enum value
	 * @param moduleName Custom module name
	 * @param cause The underlying cause
	 * @param args Optional arguments for message formatting
	 */
	public DataRepositoryException(DataRepositoryErrorDetails errorDetails, String moduleName, Throwable cause,
			Object... args) {
		super(errorDetails, moduleName, cause, args);
	}

}
