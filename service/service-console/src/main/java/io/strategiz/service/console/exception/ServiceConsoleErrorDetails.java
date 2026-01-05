package io.strategiz.service.console.exception;

import io.strategiz.framework.exception.ErrorDetails;
import org.springframework.http.HttpStatus;

/**
 * Error details for admin console operations.
 * Implements ErrorDetails for integration with the Strategiz exception framework.
 *
 * Usage: throw new StrategizException(ServiceConsoleErrorDetails.XXX, "service-console");
 */
public enum ServiceConsoleErrorDetails implements ErrorDetails {

	// === Job Management Errors ===
	JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "console-job-not-found"),
	JOB_ALREADY_RUNNING(HttpStatus.CONFLICT, "console-job-already-running"),
	JOB_NOT_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "console-job-not-available"),
	JOB_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "console-job-execution-failed"),
	JOB_NOT_SCHEDULED(HttpStatus.BAD_REQUEST, "console-job-not-scheduled"),

	// === User Management Errors ===
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "console-user-not-found"),
	CANNOT_MODIFY_OWN_ACCOUNT(HttpStatus.FORBIDDEN, "console-cannot-modify-own-account"),
	SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "console-session-not-found"),
	SESSION_USER_MISMATCH(HttpStatus.FORBIDDEN, "console-session-user-mismatch"),

	// === Provider Management Errors ===
	PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND, "console-provider-not-found"),

	// === Test Management Errors ===
	TEST_APP_NOT_FOUND(HttpStatus.NOT_FOUND, "console-test-app-not-found"),
	TEST_MODULE_NOT_FOUND(HttpStatus.NOT_FOUND, "console-test-module-not-found"),
	TEST_SUITE_NOT_FOUND(HttpStatus.NOT_FOUND, "console-test-suite-not-found"),
	TEST_CASE_NOT_FOUND(HttpStatus.NOT_FOUND, "console-test-case-not-found"),
	TEST_RUN_NOT_FOUND(HttpStatus.NOT_FOUND, "console-test-run-not-found"),
	TEST_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "console-test-execution-failed"),

	// === Authentication/Authorization Errors ===
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "console-unauthorized"),

	// === System Errors ===
	OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "console-operation-failed");

	private final HttpStatus httpStatus;

	private final String propertyKey;

	ServiceConsoleErrorDetails(HttpStatus httpStatus, String propertyKey) {
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
