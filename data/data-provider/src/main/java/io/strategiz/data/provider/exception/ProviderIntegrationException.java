package io.strategiz.data.provider.exception;

import io.strategiz.framework.exception.StrategizException;

/**
 * Exception class for provider integration operations.
 * Extends StrategizException for integration with the global exception handling framework.
 */
public class ProviderIntegrationException extends StrategizException {

	private static final String MODULE_NAME = "data-provider";

	public ProviderIntegrationException(DataProviderErrorDetails errorDetails) {
		super(errorDetails, MODULE_NAME);
	}

	public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Object... args) {
		super(errorDetails, MODULE_NAME, args);
	}

	public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Throwable cause) {
		super(errorDetails, MODULE_NAME, cause);
	}

	public ProviderIntegrationException(DataProviderErrorDetails errorDetails, Throwable cause, Object... args) {
		super(errorDetails, MODULE_NAME, cause, args);
	}

}