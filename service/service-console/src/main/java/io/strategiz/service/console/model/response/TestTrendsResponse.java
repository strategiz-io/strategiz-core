package io.strategiz.service.console.model.response;

import java.time.Instant;
import java.util.List;

/**
 * Response for test trends over time Shows pass rate, execution count, and performance
 * metrics
 */
public class TestTrendsResponse {

	private String appId;

	private String moduleId;

	private String suiteId;

	private String timeRange; // "7d", "30d", "90d"

	private List<TrendDataPoint> dataPoints;

	private TrendSummary summary;

	// Constructors
	public TestTrendsResponse() {
	}

	// Getters and Setters
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

	public String getTimeRange() {
		return timeRange;
	}

	public void setTimeRange(String timeRange) {
		this.timeRange = timeRange;
	}

	public List<TrendDataPoint> getDataPoints() {
		return dataPoints;
	}

	public void setDataPoints(List<TrendDataPoint> dataPoints) {
		this.dataPoints = dataPoints;
	}

	public TrendSummary getSummary() {
		return summary;
	}

	public void setSummary(TrendSummary summary) {
		this.summary = summary;
	}

	/**
	 * Single data point in the trend
	 */
	public static class TrendDataPoint {

		private Instant timestamp;

		private Integer totalRuns;

		private Integer passedRuns;

		private Integer failedRuns;

		private Double passRate; // Percentage (0-100)

		private Long avgDurationMs;

		private Integer totalTests;

		private Integer passedTests;

		private Integer failedTests;

		// Constructors
		public TrendDataPoint() {
		}

		// Getters and Setters
		public Instant getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
		}

		public Integer getTotalRuns() {
			return totalRuns;
		}

		public void setTotalRuns(Integer totalRuns) {
			this.totalRuns = totalRuns;
		}

		public Integer getPassedRuns() {
			return passedRuns;
		}

		public void setPassedRuns(Integer passedRuns) {
			this.passedRuns = passedRuns;
		}

		public Integer getFailedRuns() {
			return failedRuns;
		}

		public void setFailedRuns(Integer failedRuns) {
			this.failedRuns = failedRuns;
		}

		public Double getPassRate() {
			return passRate;
		}

		public void setPassRate(Double passRate) {
			this.passRate = passRate;
		}

		public Long getAvgDurationMs() {
			return avgDurationMs;
		}

		public void setAvgDurationMs(Long avgDurationMs) {
			this.avgDurationMs = avgDurationMs;
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

	}

	/**
	 * Overall summary statistics for the trend period
	 */
	public static class TrendSummary {

		private Integer totalRuns;

		private Integer passedRuns;

		private Integer failedRuns;

		private Double overallPassRate; // Percentage (0-100)

		private Long avgDurationMs;

		private Long minDurationMs;

		private Long maxDurationMs;

		private Instant firstRunTime;

		private Instant lastRunTime;

		// Constructors
		public TrendSummary() {
		}

		// Getters and Setters
		public Integer getTotalRuns() {
			return totalRuns;
		}

		public void setTotalRuns(Integer totalRuns) {
			this.totalRuns = totalRuns;
		}

		public Integer getPassedRuns() {
			return passedRuns;
		}

		public void setPassedRuns(Integer passedRuns) {
			this.passedRuns = passedRuns;
		}

		public Integer getFailedRuns() {
			return failedRuns;
		}

		public void setFailedRuns(Integer failedRuns) {
			this.failedRuns = failedRuns;
		}

		public Double getOverallPassRate() {
			return overallPassRate;
		}

		public void setOverallPassRate(Double overallPassRate) {
			this.overallPassRate = overallPassRate;
		}

		public Long getAvgDurationMs() {
			return avgDurationMs;
		}

		public void setAvgDurationMs(Long avgDurationMs) {
			this.avgDurationMs = avgDurationMs;
		}

		public Long getMinDurationMs() {
			return minDurationMs;
		}

		public void setMinDurationMs(Long minDurationMs) {
			this.minDurationMs = minDurationMs;
		}

		public Long getMaxDurationMs() {
			return maxDurationMs;
		}

		public void setMaxDurationMs(Long maxDurationMs) {
			this.maxDurationMs = maxDurationMs;
		}

		public Instant getFirstRunTime() {
			return firstRunTime;
		}

		public void setFirstRunTime(Instant firstRunTime) {
			this.firstRunTime = firstRunTime;
		}

		public Instant getLastRunTime() {
			return lastRunTime;
		}

		public void setLastRunTime(Instant lastRunTime) {
			this.lastRunTime = lastRunTime;
		}

	}

}
