package io.strategiz.service.console.model.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed response for a test run including all results
 */
public class TestRunDetailResponse {

	private String runId;

	private String appId;

	private String moduleId;

	private String suiteId;

	private String testId;

	private String level; // "app", "module", "suite", "test"

	private String trigger; // "manual", "ci-cd", "scheduled"

	private String status; // "pending", "running", "passed", "failed", "error"

	private Instant startTime;

	private Instant endTime;

	private Long durationMs;

	// Summary statistics
	private Integer totalTests;

	private Integer passedTests;

	private Integer failedTests;

	private Integer skippedTests;

	private Integer errorTests;

	// CI/CD metadata
	private String commitHash;

	private String branch;

	private String workflowRunId;

	private String workflowRunUrl;

	// Test results
	private List<TestResultDetail> results;

	// Full execution logs
	private String logs;

	private Instant createdAt;

	private String executedBy;

	// Constructors
	public TestRunDetailResponse() {
	}

	// Getters and Setters
	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}

	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getTrigger() {
		return trigger;
	}

	public void setTrigger(String trigger) {
		this.trigger = trigger;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public Long getDurationMs() {
		return durationMs;
	}

	public void setDurationMs(Long durationMs) {
		this.durationMs = durationMs;
	}

	public Integer getTotalTests() {
		return totalTests;
	}

	public void setTotalTests(Integer totalTests) {
		this.totalTests = totalTests;
	}

	public Integer getPassedTests() {
		return passedTests;
	}

	public void setPassedTests(Integer passedTests) {
		this.passedTests = passedTests;
	}

	public Integer getFailedTests() {
		return failedTests;
	}

	public void setFailedTests(Integer failedTests) {
		this.failedTests = failedTests;
	}

	public Integer getSkippedTests() {
		return skippedTests;
	}

	public void setSkippedTests(Integer skippedTests) {
		this.skippedTests = skippedTests;
	}

	public Integer getErrorTests() {
		return errorTests;
	}

	public void setErrorTests(Integer errorTests) {
		this.errorTests = errorTests;
	}

	public String getCommitHash() {
		return commitHash;
	}

	public void setCommitHash(String commitHash) {
		this.commitHash = commitHash;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getWorkflowRunId() {
		return workflowRunId;
	}

	public void setWorkflowRunId(String workflowRunId) {
		this.workflowRunId = workflowRunId;
	}

	public String getWorkflowRunUrl() {
		return workflowRunUrl;
	}

	public void setWorkflowRunUrl(String workflowRunUrl) {
		this.workflowRunUrl = workflowRunUrl;
	}

	public List<TestResultDetail> getResults() {
		return results;
	}

	public void setResults(List<TestResultDetail> results) {
		this.results = results;
	}

	public String getLogs() {
		return logs;
	}

	public void setLogs(String logs) {
		this.logs = logs;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}

	/**
	 * Individual test result detail
	 */
	public static class TestResultDetail {

		private String testId;

		private String testName;

		private String className;

		private String methodName;

		private String status; // "passed", "failed", "skipped", "error"

		private Long durationMs;

		private String errorMessage;

		private String stackTrace;

		private List<String> screenshots;

		private List<String> videos;

		// Constructors
		public TestResultDetail() {
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

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public String getMethodName() {
			return methodName;
		}

		public void setMethodName(String methodName) {
			this.methodName = methodName;
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

		public List<String> getScreenshots() {
			return screenshots;
		}

		public void setScreenshots(List<String> screenshots) {
			this.screenshots = screenshots;
		}

		public List<String> getVideos() {
			return videos;
		}

		public void setVideos(List<String> videos) {
			this.videos = videos;
		}

	}

}
