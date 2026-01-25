package io.strategiz.service.console.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket message for test progress updates
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestProgressMessage extends TestStreamMessage {

	@JsonProperty("totalTests")
	private int totalTests;

	@JsonProperty("completedTests")
	private int completedTests;

	@JsonProperty("passedTests")
	private int passedTests;

	@JsonProperty("failedTests")
	private int failedTests;

	@JsonProperty("skippedTests")
	private int skippedTests;

	@JsonProperty("progressPercent")
	private double progressPercent;

	@JsonProperty("currentTest")
	private String currentTest;

	@JsonProperty("currentSuite")
	private String currentSuite;

	@JsonProperty("elapsedMs")
	private long elapsedMs;

	@JsonProperty("estimatedRemainingMs")
	private Long estimatedRemainingMs;

	public TestProgressMessage() {
		super(MessageType.PROGRESS, null);
	}

	public TestProgressMessage(String runId) {
		super(MessageType.PROGRESS, runId);
	}

	// Builder pattern
	public TestProgressMessage withCounts(int total, int completed, int passed, int failed, int skipped) {
		this.totalTests = total;
		this.completedTests = completed;
		this.passedTests = passed;
		this.failedTests = failed;
		this.skippedTests = skipped;
		this.progressPercent = total > 0 ? (double) completed / total * 100 : 0;
		return this;
	}

	public TestProgressMessage withCurrentTest(String test, String suite) {
		this.currentTest = test;
		this.currentSuite = suite;
		return this;
	}

	public TestProgressMessage withTiming(long elapsedMs, Long estimatedRemainingMs) {
		this.elapsedMs = elapsedMs;
		this.estimatedRemainingMs = estimatedRemainingMs;
		return this;
	}

	// Getters and Setters
	public int getTotalTests() {
		return totalTests;
	}

	public void setTotalTests(int totalTests) {
		this.totalTests = totalTests;
	}

	public int getCompletedTests() {
		return completedTests;
	}

	public void setCompletedTests(int completedTests) {
		this.completedTests = completedTests;
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

	public double getProgressPercent() {
		return progressPercent;
	}

	public void setProgressPercent(double progressPercent) {
		this.progressPercent = progressPercent;
	}

	public String getCurrentTest() {
		return currentTest;
	}

	public void setCurrentTest(String currentTest) {
		this.currentTest = currentTest;
	}

	public String getCurrentSuite() {
		return currentSuite;
	}

	public void setCurrentSuite(String currentSuite) {
		this.currentSuite = currentSuite;
	}

	public long getElapsedMs() {
		return elapsedMs;
	}

	public void setElapsedMs(long elapsedMs) {
		this.elapsedMs = elapsedMs;
	}

	public Long getEstimatedRemainingMs() {
		return estimatedRemainingMs;
	}

	public void setEstimatedRemainingMs(Long estimatedRemainingMs) {
		this.estimatedRemainingMs = estimatedRemainingMs;
	}

}
