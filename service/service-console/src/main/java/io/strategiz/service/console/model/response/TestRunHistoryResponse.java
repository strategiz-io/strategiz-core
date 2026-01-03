package io.strategiz.service.console.model.response;

import java.time.Instant;
import java.util.List;

/**
 * Paginated response for test run history
 */
public class TestRunHistoryResponse {

	private List<TestRunSummary> runs;

	private int totalCount;

	private int pageSize;

	private int currentPage;

	private int totalPages;

	// Constructors
	public TestRunHistoryResponse() {
	}

	public TestRunHistoryResponse(List<TestRunSummary> runs, int totalCount, int pageSize, int currentPage) {
		this.runs = runs;
		this.totalCount = totalCount;
		this.pageSize = pageSize;
		this.currentPage = currentPage;
		this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
	}

	// Getters and Setters
	public List<TestRunSummary> getRuns() {
		return runs;
	}

	public void setRuns(List<TestRunSummary> runs) {
		this.runs = runs;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	/**
	 * Summary of a single test run for history view
	 */
	public static class TestRunSummary {

		private String runId;

		private String appId;

		private String appName;

		private String moduleId;

		private String moduleName;

		private String suiteId;

		private String suiteName;

		private String level; // "app", "module", "suite", "test"

		private String trigger; // "manual", "ci-cd", "scheduled"

		private String status; // "pending", "running", "passed", "failed", "error"

		private Instant startTime;

		private Instant endTime;

		private Long durationMs;

		private Integer totalTests;

		private Integer passedTests;

		private Integer failedTests;

		private Integer skippedTests;

		private Integer errorTests;

		private String commitHash;

		private String branch;

		private String executedBy;

		// Constructors
		public TestRunSummary() {
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

		public String getAppName() {
			return appName;
		}

		public void setAppName(String appName) {
			this.appName = appName;
		}

		public String getModuleId() {
			return moduleId;
		}

		public void setModuleId(String moduleId) {
			this.moduleId = moduleId;
		}

		public String getModuleName() {
			return moduleName;
		}

		public void setModuleName(String moduleName) {
			this.moduleName = moduleName;
		}

		public String getSuiteId() {
			return suiteId;
		}

		public void setSuiteId(String suiteId) {
			this.suiteId = suiteId;
		}

		public String getSuiteName() {
			return suiteName;
		}

		public void setSuiteName(String suiteName) {
			this.suiteName = suiteName;
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

		public String getExecutedBy() {
			return executedBy;
		}

		public void setExecutedBy(String executedBy) {
			this.executedBy = executedBy;
		}

	}

}
