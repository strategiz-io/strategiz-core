package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.data.testing.entity.TestSuiteEntity;
import io.strategiz.data.testing.entity.TestTrigger;
import io.strategiz.data.testing.repository.TestAppRepository;
import io.strategiz.data.testing.repository.TestModuleRepository;
import io.strategiz.data.testing.repository.TestResultRepository;
import io.strategiz.data.testing.repository.TestRunRepository;
import io.strategiz.data.testing.repository.TestSuiteRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.TestRunDetailResponse;
import io.strategiz.service.console.model.response.TestRunDetailResponse.TestResultDetail;
import io.strategiz.service.console.model.response.TestRunHistoryResponse;
import io.strategiz.service.console.model.response.TestRunHistoryResponse.TestRunSummary;
import io.strategiz.service.console.model.response.TestTrendsResponse;
import io.strategiz.service.console.model.response.TestTrendsResponse.TrendDataPoint;
import io.strategiz.service.console.model.response.TestTrendsResponse.TrendSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for retrieving test results and history
 */
@Service
public class TestResultsService extends BaseService {

	private static final Logger log = LoggerFactory.getLogger(TestResultsService.class);

	private final TestRunRepository testRunRepository;

	private final TestResultRepository testResultRepository;

	private final TestAppRepository testAppRepository;

	private final TestModuleRepository testModuleRepository;

	private final TestSuiteRepository testSuiteRepository;

	@Autowired
	public TestResultsService(TestRunRepository testRunRepository, TestResultRepository testResultRepository,
			TestAppRepository testAppRepository, TestModuleRepository testModuleRepository,
			TestSuiteRepository testSuiteRepository) {
		this.testRunRepository = testRunRepository;
		this.testResultRepository = testResultRepository;
		this.testAppRepository = testAppRepository;
		this.testModuleRepository = testModuleRepository;
		this.testSuiteRepository = testSuiteRepository;
	}

	@Override
	protected String getModuleName() {
		return "TEST_RESULTS_SERVICE";
	}

	/**
	 * Get detailed test run with all results
	 *
	 * @param runId Test run ID
	 * @return Detailed test run response
	 */
	public TestRunDetailResponse getRunDetails(String runId) {
		log.debug("Fetching detailed results for test run: {}", runId);

		TestRunEntity run = testRunRepository.findById(runId)
			.orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_RUN_NOT_FOUND,
					"Test run not found: " + runId));

		// Fetch all test results for this run
		List<TestResultEntity> results = testResultRepository.findByRunId(runId);

		return convertToDetailResponse(run, results);
	}

	/**
	 * Get paginated test run history
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @param trigger Optional trigger filter ("manual", "ci-cd", "scheduled")
	 * @param status Optional status filter
	 * @param page Page number (0-indexed)
	 * @param pageSize Page size
	 * @return Paginated test run history
	 */
	public TestRunHistoryResponse getRunHistory(String appId, String moduleId, String suiteId, String trigger,
			String status, int page, int pageSize) {
		log.debug("Fetching test run history: appId={}, moduleId={}, suiteId={}, page={}, pageSize={}", appId,
				moduleId, suiteId, page, pageSize);

		// Build query based on filters
		List<TestRunEntity> allRuns = testRunRepository.findAll();

		// Apply filters
		List<TestRunEntity> filteredRuns = allRuns.stream()
			.filter(run -> appId == null || appId.equals(run.getAppId()))
			.filter(run -> moduleId == null || moduleId.equals(run.getModuleId()))
			.filter(run -> suiteId == null || suiteId.equals(run.getSuiteId()))
			.filter(run -> trigger == null || trigger.equals(run.getTrigger()))
			.filter(run -> status == null || status.equals(run.getStatus()))
			.sorted((r1, r2) -> r2.getStartTime().compareTo(r1.getStartTime())) // Most recent first
			.collect(Collectors.toList());

		int totalCount = filteredRuns.size();

		// Apply pagination
		int startIndex = page * pageSize;
		int endIndex = Math.min(startIndex + pageSize, totalCount);

		List<TestRunEntity> pageRuns = startIndex < totalCount
				? filteredRuns.subList(startIndex, endIndex)
				: new ArrayList<>();

		// Convert to summaries
		List<TestRunSummary> summaries = pageRuns.stream()
			.map(this::convertToSummary)
			.collect(Collectors.toList());

		return new TestRunHistoryResponse(summaries, totalCount, pageSize, page);
	}

	/**
	 * Get latest CI/CD test run
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @return Latest CI/CD test run, or null if none found
	 */
	public TestRunDetailResponse getLatestCiRun(String appId, String moduleId, String suiteId) {
		log.debug("Fetching latest CI/CD run: appId={}, moduleId={}, suiteId={}", appId, moduleId, suiteId);

		List<TestRunEntity> ciRuns = testRunRepository.findByTrigger(TestTrigger.CI_CD);

		// Apply filters and sort by most recent
		TestRunEntity latestRun = ciRuns.stream()
			.filter(run -> appId == null || appId.equals(run.getAppId()))
			.filter(run -> moduleId == null || moduleId.equals(run.getModuleId()))
			.filter(run -> suiteId == null || suiteId.equals(run.getSuiteId()))
			.sorted((r1, r2) -> r2.getStartTime().compareTo(r1.getStartTime()))
			.findFirst()
			.orElse(null);

		if (latestRun == null) {
			return null;
		}

		List<TestResultEntity> results = testResultRepository.findByRunId(latestRun.getId());
		return convertToDetailResponse(latestRun, results);
	}

	/**
	 * Get test trends over time
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @param timeRange Time range ("7d", "30d", "90d")
	 * @return Test trends response
	 */
	public TestTrendsResponse getTrends(String appId, String moduleId, String suiteId, String timeRange) {
		log.debug("Fetching test trends: appId={}, moduleId={}, suiteId={}, timeRange={}", appId, moduleId, suiteId,
				timeRange);

		// Calculate time range
		int days = parseTimeRange(timeRange);
		Instant cutoffTime = Instant.now().minus(days, ChronoUnit.DAYS);

		// Fetch all runs within time range
		List<TestRunEntity> runs = testRunRepository.findAll().stream()
			.filter(run -> run.getStartTime().isAfter(cutoffTime))
			.filter(run -> appId == null || appId.equals(run.getAppId()))
			.filter(run -> moduleId == null || moduleId.equals(run.getModuleId()))
			.filter(run -> suiteId == null || suiteId.equals(run.getSuiteId()))
			.sorted((r1, r2) -> r1.getStartTime().compareTo(r2.getStartTime())) // Oldest first for trend
			.collect(Collectors.toList());

		// Group runs by day
		Map<String, List<TestRunEntity>> runsByDay = groupRunsByDay(runs);

		// Create data points
		List<TrendDataPoint> dataPoints = runsByDay.entrySet().stream().map(entry -> {
			List<TestRunEntity> dayRuns = entry.getValue();
			return createDataPoint(dayRuns);
		}).collect(Collectors.toList());

		// Create summary
		TrendSummary summary = createTrendSummary(runs);

		TestTrendsResponse response = new TestTrendsResponse();
		response.setAppId(appId);
		response.setModuleId(moduleId);
		response.setSuiteId(suiteId);
		response.setTimeRange(timeRange);
		response.setDataPoints(dataPoints);
		response.setSummary(summary);

		return response;
	}

	// === Private Helper Methods ===

	/**
	 * Convert TestRunEntity and results to detailed response
	 */
	private TestRunDetailResponse convertToDetailResponse(TestRunEntity run, List<TestResultEntity> results) {
		TestRunDetailResponse response = new TestRunDetailResponse();
		response.setRunId(run.getId());
		response.setAppId(run.getAppId());
		response.setModuleId(run.getModuleId());
		response.setSuiteId(run.getSuiteId());
		response.setTestId(run.getTestId());
		response.setLevel(run.getLevel().name().toLowerCase());
		response.setTrigger(run.getTrigger().name().toLowerCase().replace("_", "-"));
		response.setStatus(run.getStatus().name().toLowerCase());
		response.setStartTime(run.getStartTime());
		response.setEndTime(run.getEndTime());
		response.setDurationMs(run.getDurationMs());

		// Summary statistics
		response.setTotalTests(run.getTotalTests());
		response.setPassedTests(run.getPassedTests());
		response.setFailedTests(run.getFailedTests());
		response.setSkippedTests(run.getSkippedTests());
		response.setErrorTests(run.getErrorTests());

		// CI/CD metadata
		response.setCommitHash(run.getCommitHash());
		response.setBranch(run.getBranch());
		response.setWorkflowRunId(run.getWorkflowRunId());
		response.setWorkflowRunUrl(run.getWorkflowRunUrl());

		// Convert results
		List<TestResultDetail> resultDetails = results.stream()
			.map(this::convertToResultDetail)
			.collect(Collectors.toList());
		response.setResults(resultDetails);

		// Logs
		response.setLogs(run.getLogs());

		// Metadata
		response.setCreatedAt(run.getCreatedDate() != null ? run.getCreatedDate().toDate().toInstant() : null);
		response.setExecutedBy(run.getExecutedBy());

		return response;
	}

	/**
	 * Convert TestResultEntity to result detail
	 */
	private TestResultDetail convertToResultDetail(TestResultEntity result) {
		TestResultDetail detail = new TestResultDetail();
		detail.setTestId(result.getId());
		detail.setTestName(result.getTestName());
		detail.setClassName(result.getClassName());
		detail.setMethodName(result.getMethodName());
		detail.setStatus(result.getStatus().name().toLowerCase());
		detail.setDurationMs(result.getDurationMs());
		detail.setErrorMessage(result.getErrorMessage());
		detail.setStackTrace(result.getStackTrace());
		detail.setScreenshots(result.getScreenshots());
		detail.setVideos(result.getVideos());
		return detail;
	}

	/**
	 * Convert TestRunEntity to summary
	 */
	private TestRunSummary convertToSummary(TestRunEntity run) {
		TestRunSummary summary = new TestRunSummary();
		summary.setRunId(run.getId());
		summary.setAppId(run.getAppId());
		summary.setModuleId(run.getModuleId());
		summary.setSuiteId(run.getSuiteId());
		summary.setLevel(run.getLevel().name().toLowerCase());
		summary.setTrigger(run.getTrigger().name().toLowerCase().replace("_", "-"));
		summary.setStatus(run.getStatus().name().toLowerCase());
		summary.setStartTime(run.getStartTime());
		summary.setEndTime(run.getEndTime());
		summary.setDurationMs(run.getDurationMs());
		summary.setTotalTests(run.getTotalTests());
		summary.setPassedTests(run.getPassedTests());
		summary.setFailedTests(run.getFailedTests());
		summary.setSkippedTests(run.getSkippedTests());
		summary.setErrorTests(run.getErrorTests());
		summary.setCommitHash(run.getCommitHash());
		summary.setBranch(run.getBranch());
		summary.setExecutedBy(run.getExecutedBy());

		// Fetch names for display
		if (run.getAppId() != null) {
			testAppRepository.findById(run.getAppId())
				.ifPresent(app -> summary.setAppName(app.getDisplayName()));
		}
		if (run.getModuleId() != null) {
			testModuleRepository.findById(run.getModuleId())
				.ifPresent(module -> summary.setModuleName(module.getDisplayName()));
		}
		if (run.getSuiteId() != null) {
			testSuiteRepository.findById(run.getSuiteId())
				.ifPresent(suite -> summary.setSuiteName(suite.getDisplayName()));
		}

		return summary;
	}

	/**
	 * Parse time range string to number of days
	 */
	private int parseTimeRange(String timeRange) {
		if (timeRange == null) {
			return 7; // Default 7 days
		}

		switch (timeRange.toLowerCase()) {
			case "7d":
				return 7;
			case "30d":
				return 30;
			case "90d":
				return 90;
			default:
				return 7;
		}
	}

	/**
	 * Group test runs by day (ISO date string)
	 */
	private Map<String, List<TestRunEntity>> groupRunsByDay(List<TestRunEntity> runs) {
		Map<String, List<TestRunEntity>> grouped = new HashMap<>();

		for (TestRunEntity run : runs) {
			String day = run.getStartTime().truncatedTo(ChronoUnit.DAYS).toString();
			grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(run);
		}

		return grouped;
	}

	/**
	 * Create trend data point from runs in a single day
	 */
	private TrendDataPoint createDataPoint(List<TestRunEntity> runs) {
		TrendDataPoint point = new TrendDataPoint();

		if (runs.isEmpty()) {
			return point;
		}

		point.setTimestamp(runs.get(0).getStartTime().truncatedTo(ChronoUnit.DAYS));
		point.setTotalRuns(runs.size());

		int passedRuns = 0;
		int failedRuns = 0;
		long totalDuration = 0;
		int totalTests = 0;
		int passedTests = 0;
		int failedTests = 0;

		for (TestRunEntity run : runs) {
			if ("passed".equals(run.getStatus())) {
				passedRuns++;
			}
			else if ("failed".equals(run.getStatus()) || "error".equals(run.getStatus())) {
				failedRuns++;
			}

			if (run.getDurationMs() != null) {
				totalDuration += run.getDurationMs();
			}

			if (run.getTotalTests() != null) {
				totalTests += run.getTotalTests();
			}
			if (run.getPassedTests() != null) {
				passedTests += run.getPassedTests();
			}
			if (run.getFailedTests() != null) {
				failedTests += run.getFailedTests();
			}
		}

		point.setPassedRuns(passedRuns);
		point.setFailedRuns(failedRuns);

		double passRate = runs.size() > 0 ? (passedRuns * 100.0) / runs.size() : 0.0;
		point.setPassRate(passRate);

		long avgDuration = runs.size() > 0 ? totalDuration / runs.size() : 0;
		point.setAvgDurationMs(avgDuration);

		point.setTotalTests(totalTests);
		point.setPassedTests(passedTests);
		point.setFailedTests(failedTests);

		return point;
	}

	/**
	 * Create trend summary from all runs
	 */
	private TrendSummary createTrendSummary(List<TestRunEntity> runs) {
		TrendSummary summary = new TrendSummary();

		if (runs.isEmpty()) {
			summary.setTotalRuns(0);
			summary.setPassedRuns(0);
			summary.setFailedRuns(0);
			summary.setOverallPassRate(0.0);
			return summary;
		}

		summary.setTotalRuns(runs.size());

		int passedRuns = 0;
		int failedRuns = 0;
		long totalDuration = 0;
		long minDuration = Long.MAX_VALUE;
		long maxDuration = Long.MIN_VALUE;

		for (TestRunEntity run : runs) {
			if ("passed".equals(run.getStatus())) {
				passedRuns++;
			}
			else if ("failed".equals(run.getStatus()) || "error".equals(run.getStatus())) {
				failedRuns++;
			}

			if (run.getDurationMs() != null) {
				totalDuration += run.getDurationMs();
				minDuration = Math.min(minDuration, run.getDurationMs());
				maxDuration = Math.max(maxDuration, run.getDurationMs());
			}
		}

		summary.setPassedRuns(passedRuns);
		summary.setFailedRuns(failedRuns);

		double overallPassRate = runs.size() > 0 ? (passedRuns * 100.0) / runs.size() : 0.0;
		summary.setOverallPassRate(overallPassRate);

		long avgDuration = runs.size() > 0 ? totalDuration / runs.size() : 0;
		summary.setAvgDurationMs(avgDuration);

		summary.setMinDurationMs(minDuration != Long.MAX_VALUE ? minDuration : 0L);
		summary.setMaxDurationMs(maxDuration != Long.MIN_VALUE ? maxDuration : 0L);

		summary.setFirstRunTime(runs.get(0).getStartTime());
		summary.setLastRunTime(runs.get(runs.size() - 1).getStartTime());

		return summary;
	}

}
