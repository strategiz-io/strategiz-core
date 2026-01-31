package io.strategiz.data.strategy.exception;

import io.strategiz.framework.exception.StrategizException;

/**
 * Exception class for data-strategy module operations. Extends StrategizException for
 * integration with the global exception handling framework.
 *
 * Usage: throw new DataStrategyException(DataStrategyErrorDetails.STRATEGY_NOT_FOUND,
 * "data-strategy", strategyId); throw new
 * DataStrategyException(DataStrategyErrorDetails.ALERT_QUERY_FAILED, "data-strategy", e,
 * strategyId, e.getMessage());
 */
public class DataStrategyException extends StrategizException {

	/**
	 * Create exception with error details and optional arguments for message formatting.
	 * @param errorDetails The error details enum value (DataStrategyErrorDetails)
	 * @param moduleName Module name (e.g., "data-strategy")
	 * @param args Optional arguments for message formatting (e.g., strategy ID, error
	 * message)
	 */
	public DataStrategyException(DataStrategyErrorDetails errorDetails, String moduleName, Object... args) {
		super(errorDetails, moduleName, args);
	}

	/**
	 * Create exception with error details, cause, and optional arguments.
	 * @param errorDetails The error details enum value (DataStrategyErrorDetails)
	 * @param moduleName Module name (e.g., "data-strategy")
	 * @param cause The underlying cause
	 * @param args Optional arguments for message formatting
	 */
	public DataStrategyException(DataStrategyErrorDetails errorDetails, String moduleName, Throwable cause,
			Object... args) {
		super(errorDetails, moduleName, cause, args);
	}

}
