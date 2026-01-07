package io.strategiz.batch.base.exception;

import io.strategiz.framework.exception.ErrorDetails;

/**
 * Standardized error codes for batch job operations.
 */
public enum BatchErrorDetails implements ErrorDetails {

	// General batch errors (BATCH-001 to BATCH-099)
	JOB_ALREADY_RUNNING("BATCH-001", "Job is already running"),
	JOB_DISABLED("BATCH-002", "Job is disabled"),
	JOB_NOT_FOUND("BATCH-003", "Job not found"),
	JOB_EXECUTION_FAILED("BATCH-004", "Job execution failed"),
	JOB_TIMEOUT("BATCH-005", "Job execution timed out"),
	JOB_CANCELLED("BATCH-006", "Job was cancelled"),

	// Configuration errors (BATCH-100 to BATCH-199)
	INVALID_CONFIGURATION("BATCH-100", "Invalid job configuration"),
	MISSING_REQUIRED_CONFIG("BATCH-101", "Missing required configuration"),
	INVALID_SCHEDULE("BATCH-102", "Invalid schedule configuration"),

	// Lock errors (BATCH-200 to BATCH-299)
	LOCK_ACQUISITION_FAILED("BATCH-200", "Failed to acquire distributed lock"),
	LOCK_ALREADY_HELD("BATCH-201", "Lock already held by another instance"),
	LOCK_EXPIRED("BATCH-202", "Lock expired during execution"),
	LOCK_RELEASE_FAILED("BATCH-203", "Failed to release lock"),

	// Retry errors (BATCH-300 to BATCH-399)
	MAX_RETRIES_EXCEEDED("BATCH-300", "Maximum retry attempts exceeded"),
	NON_RETRYABLE_ERROR("BATCH-301", "Non-retryable error occurred"),
	RETRY_INTERRUPTED("BATCH-302", "Retry was interrupted"),

	// Data processing errors (BATCH-400 to BATCH-499)
	NO_DATA_TO_PROCESS("BATCH-400", "No data available to process"),
	DATA_VALIDATION_FAILED("BATCH-401", "Data validation failed"),
	PARTIAL_FAILURE("BATCH-402", "Some items failed to process"),
	DATA_SOURCE_UNAVAILABLE("BATCH-403", "Data source is unavailable"),

	// Resource errors (BATCH-500 to BATCH-599)
	RESOURCE_EXHAUSTED("BATCH-500", "Resource limit exceeded"),
	MEMORY_LIMIT_EXCEEDED("BATCH-501", "Memory limit exceeded"),
	RATE_LIMIT_EXCEEDED("BATCH-502", "Rate limit exceeded"),
	QUOTA_EXCEEDED("BATCH-503", "Quota exceeded");

	private final String code;

	private final String message;

	BatchErrorDetails(String code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getModuleName() {
		return "batch-framework-base";
	}

}
