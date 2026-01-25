package io.strategiz.service.console.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket message for individual test result
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestResultMessage extends TestStreamMessage {

	@JsonProperty("testId")
	private String testId;

	@JsonProperty("testName")
	private String testName;

	@JsonProperty("suiteName")
	private String suiteName;

	@JsonProperty("status")
	private String status; // passed, failed, skipped, error

	@JsonProperty("durationMs")
	private Long durationMs;

	@JsonProperty("errorMessage")
	private String errorMessage;

	@JsonProperty("stackTrace")
	private String stackTrace;

	@JsonProperty("retryCount")
	private Integer retryCount;

	public TestResultMessage() {
		super(MessageType.TEST_RESULT, null);
	}

	public TestResultMessage(String runId, String testId, String testName, String status) {
		super(MessageType.TEST_RESULT, runId);
		this.testId = testId;
		this.testName = testName;
		this.status = status;
	}

	// Builder pattern for fluent API
	public TestResultMessage withSuiteName(String suiteName) {
		this.suiteName = suiteName;
		return this;
	}

	public TestResultMessage withDuration(Long durationMs) {
		this.durationMs = durationMs;
		return this;
	}

	public TestResultMessage withError(String errorMessage, String stackTrace) {
		this.errorMessage = errorMessage;
		this.stackTrace = stackTrace;
		return this;
	}

	public TestResultMessage withRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
		return this;
	}

	// Getters and Setters
	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public String getSuiteName() {
		return suiteName;
	}

	public void setSuiteName(String suiteName) {
		this.suiteName = suiteName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Long durationMs) {
		this.durationMs = durationMs;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public Integer getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(Integer retryCount) {
		this.retryCount = retryCount;
	}

}
