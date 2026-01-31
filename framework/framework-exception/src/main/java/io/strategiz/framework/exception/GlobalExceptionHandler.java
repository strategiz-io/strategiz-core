package io.strategiz.framework.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for system-wide exceptions.
 *
 * <p>
 * In the hybrid approach: - BaseController handles StrategizException (business
 * exceptions) -. GlobalExceptionHandler handles system exceptions (unexpected errors).
 *
 * <p>
 * This provides a safety net for exceptions that fall through BaseController.
 *
 * <p>
 * Order is set to LOWEST_PRECEDENCE to ensure BaseController's @ExceptionHandler methods
 * are. tried first for controllers that extend BaseController.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Autowired(required = false)
	private ErrorMessageService errorMessageService;

	/**
	 * Handle StrategizException that falls through BaseController This should rarely be
	 * called -. mainly for controllers that don't extend BaseController.
	 */
	@ExceptionHandler(StrategizException.class)
	public ResponseEntity<StandardErrorResponse> handleStrategizException(StrategizException ex,
			HttpServletRequest request) {

		String traceId = generateTraceId();

		// Log warning that this should be handled by BaseController
		log.warn(
				"StrategizException handled by GlobalExceptionHandler [{}] - "
						+ "Controller should extend BaseController. Module: {}, Error: {}",
				traceId, ex.getModuleName(), ex.getErrorCode());

		// Use ErrorMessageService if available, otherwise create basic response
		StandardErrorResponse errorResponse;
		if (errorMessageService != null) {
			errorResponse = errorMessageService.buildErrorResponse(ex);
		}
		else {
			// Fallback if ErrorMessageService is not available
			errorResponse = new StandardErrorResponse(ex.getErrorCode(), "An error occurred. Please try again.",
					"Error in module: " + ex.getModuleName() + " - " + ex.getErrorCode(),
					"https://docs.strategiz.io/errors/general/error");
		}

		// Add trace ID to MDC for response headers
		MDC.put("traceId", traceId);

		return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
	}

	/** Handle unexpected runtime exceptions (system-level errors). */
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<StandardErrorResponse> handleRuntimeException(RuntimeException ex,
			HttpServletRequest request) {

		String traceId = generateTraceId();

		// Log the unexpected exception with full stack trace
		log.error("Unexpected runtime exception [{}]: {}", traceId, ex.getMessage(), ex);

		StandardErrorResponse errorResponse = new StandardErrorResponse("SYSTEM_RUNTIME_ERROR",
				"A system error occurred. Please contact support.",
				"Runtime exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
				"https://docs.strategiz.io/errors/system/runtime-error");

		MDC.put("traceId", traceId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/** Handle all other unexpected exceptions (system-level errors). */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<StandardErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {

		String traceId = generateTraceId();

		// Log the unexpected exception with full stack trace
		log.error("Unexpected system exception [{}]: {}", traceId, ex.getMessage(), ex);

		StandardErrorResponse errorResponse = new StandardErrorResponse("SYSTEM_ERROR",
				"A system error occurred. Please contact support.",
				"System exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
				"https://docs.strategiz.io/errors/system/error");

		MDC.put("traceId", traceId);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/** Map error codes to appropriate HTTP status codes. */
	private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
		if (errorCode == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}

		// Authentication/Authorization errors
		if (errorCode.contains("INVALID_CREDENTIALS") || errorCode.contains("AUTHENTICATION_FAILED")
				|| errorCode.contains("INVALID_TOKEN") || errorCode.contains("TOKEN_REVOKED")) {
			return HttpStatus.UNAUTHORIZED;
		}

		if (errorCode.contains("ACCESS_DENIED") || errorCode.contains("FORBIDDEN")) {
			return HttpStatus.FORBIDDEN;
		}

		// Not found errors
		if (errorCode.contains("NOT_FOUND") || errorCode.contains("USER_NOT_FOUND")
				|| errorCode.contains("PROFILE_NOT_FOUND")) {
			return HttpStatus.NOT_FOUND;
		}

		// Validation errors
		if (errorCode.contains("VALIDATION_FAILED") || errorCode.contains("INVALID_REQUEST")
				|| errorCode.contains("INVALID_") || errorCode.contains("ALREADY_EXISTS")) {
			return HttpStatus.BAD_REQUEST;
		}

		// Rate limiting
		if (errorCode.contains("RATE_LIMITED") || errorCode.contains("TOO_MANY_REQUESTS")) {
			return HttpStatus.TOO_MANY_REQUESTS;
		}

		// External service errors
		if (errorCode.contains("API_") || errorCode.contains("CONNECTION_FAILED")
				|| errorCode.contains("SERVICE_UNAVAILABLE")) {
			return HttpStatus.BAD_GATEWAY;
		}

		// Default to 500 for unknown errors
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/** Generate a unique trace ID for error tracking. */
	private String generateTraceId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

}
