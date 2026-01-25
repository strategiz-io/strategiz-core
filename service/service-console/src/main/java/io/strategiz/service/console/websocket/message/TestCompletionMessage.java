package io.strategiz.service.console.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * WebSocket message for test run completion
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCompletionMessage extends TestStreamMessage {

	@JsonProperty("status")
	private String status; // passed, failed, error, cancelled

	@JsonProperty("totalTests")
	private int totalTests;

	@JsonProperty("passedTests")
	private int passedTests;

	@JsonProperty("failedTests")
	private int failedTests;

	@JsonProperty("skippedTests")
	private int skippedTests;

	@JsonProperty("errorTests")
	private int errorTests;

	@JsonProperty("durationMs")
	private long durationMs;

	@JsonProperty("passRate")
	private double passRate;

	@JsonProperty("failedTestIds")
	private List<String> failedTestIds;

	@JsonProperty("slowTestIds")
	private List<String> slowTestIds;

	@JsonProperty("errorMessage")
	private String errorMessage;

	public TestCompletionMessage() {
		super(MessageType.COMPLETION, null);
	}

	public TestCompletionMessage(String runId, String status) {
		super(MessageType.COMPLETION, runId);
		this.status = status;
	}

	// Builder pattern
	public TestCompletionMessage withSummary(int total, int passed, int failed, int skipped, int errors) {
		this.totalTests = total;
		this.passedTests = passed;
		this.failedTests = failed;
		this.skippedTests = skipped;
		this.errorTests = errors;
		this.passRate = total > 0 ? (double) passed / total * 100 : 0;
		return this;
	}

	public TestCompletionMessage withDuration(long durationMs) {
		this.durationMs = durationMs;
		return this;
	}

	public TestCompletionMessage withFailedTests(List<String> failedTestIds) {
		this.failedTestIds = failedTestIds;
		return this;
	}

	public TestCompletionMessage withSlowTests(List<String> slowTestIds) {
		this.slowTestIds = slowTestIds;
		return this;
	}

	public TestCompletionMessage withError(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	// Getters and Setters
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getTotalTests() {
		return totalTests;
	}

	public void setTotalTests(int totalTests) {
		this.totalTests = totalTests;
	}

	public int getPassedTests() {
		return passedTests;
	}

	public void setPassedTests(int passedTests) {
		this.passedTests = passedTests;
	}

	public int getFailedTests() {
		return failedTests;
	}

	public void setFailedTests(int failedTests) {
		this.failedTests = failedTests;
	}

	public int getSkippedTests() {
		return skippedTests;
	}

	public void setSkippedTests(int skippedTests) {
		this.skippedTests = skippedTests;
	}

	public int getErrorTests() {
		return errorTests;
	}

	public void setErrorTests(int errorTests) {
		this.errorTests = errorTests;
	}

	public long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(long durationMs) {
		this.durationMs = durationMs;
	}

	public double getPassRate() {
		return passRate;
	}

	public void setPassRate(double passRate) {
		this.passRate = passRate;
	}

	public List<String> getFailedTestIds() {
		return failedTestIds;
	}

	public void setFailedTestIds(List<String> failedTestIds) {
		this.failedTestIds = failedTestIds;
	}

	public List<String> getSlowTestIds() {
		return slowTestIds;
	}

	public void setSlowTestIds(List<String> slowTestIds) {
		this.slowTestIds = slowTestIds;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
