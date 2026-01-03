package io.strategiz.service.console.controller;

import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestResultStatus;
import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.data.testing.entity.TestRunStatus;
import io.strategiz.data.testing.entity.TestTrigger;
import io.strategiz.data.testing.repository.TestResultRepository;
import io.strategiz.data.testing.repository.TestRunRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for CI/CD integration
 * Accepts test results from GitHub Actions and other CI systems
 */
@RestController
@RequestMapping("/v1/console/tests/ci")
@Tag(name = "CI Integration", description = "Endpoints for CI/CD systems to post test results")
public class CiIntegrationController extends BaseController {

	private final TestRunRepository testRunRepository;

	private final TestResultRepository testResultRepository;

	@Value("${test-runner.ci.auth-token:}")
	private String ciAuthToken;

	@Autowired
	public CiIntegrationController(TestRunRepository testRunRepository, TestResultRepository testResultRepository) {
		this.testRunRepository = testRunRepository;
		this.testResultRepository = testResultRepository;
	}

	@Override
	protected String getModuleName() {
		return "CI_INTEGRATION_CONTROLLER";
	}

	/**
	 * Accept test results from CI/CD systems (GitHub Actions, etc.) This endpoint is
	 * called by CI workflows to post test execution results.
	 *
	 * Authentication: Bearer token in Authorization header Expected format from
	 * aggregate-test-results.py script
	 */
	@PostMapping("/results")
	@Operation(summary = "Post CI/CD test results", description = "Called by CI systems to upload test execution results")
	public ResponseEntity<Map<String, Object>> postCiResults(@RequestHeader("Authorization") String authHeader,
			@RequestBody CiTestResultsRequest request) {

		// Validate auth token
		validateAuthToken(authHeader);

		log.info("Received CI test results: appId={}, status={}, totalTests={}", request.getAppId(),
				request.getStatus(), request.getTotalTests());

		// Create TestRunEntity
		TestRunEntity testRun = new TestRunEntity();
		testRun.setId(UUID.randomUUID().toString());
		testRun.setAppId(request.getAppId());
		testRun.setModuleId(request.getModuleId());
		testRun.setSuiteId(request.getSuiteId());
		testRun.setTestId(request.getTestId());

		// Parse level from request or default to 'app'
		testRun.setLevel(parseExecutionLevel(request.getLevel()));

		// Set trigger to CI_CD
		testRun.setTrigger(TestTrigger.CI_CD);

		// Parse status
		testRun.setStatus(parseRunStatus(request.getStatus()));

		// Timestamps
		testRun.setStartTime(parseInstant(request.getStartTime()));
		testRun.setEndTime(parseInstant(request.getEndTime()));
		testRun.setDurationMs(request.getDurationMs());

		// Summary statistics
		testRun.setTotalTests(request.getTotalTests());
		testRun.setPassedTests(request.getPassedTests());
		testRun.setFailedTests(request.getFailedTests());
		testRun.setSkippedTests(request.getSkippedTests());
		testRun.setErrorTests(request.getErrorTests());

		// CI/CD metadata
		testRun.setCommitHash(request.getCommitHash());
		testRun.setBranch(request.getBranch());
		testRun.setWorkflowRunId(request.getWorkflowRunId());
		testRun.setWorkflowRunUrl(request.getWorkflowRunUrl());

		// Executed by system (CI)
		testRun.setExecutedBy("ci-system");

		// Save test run
		testRunRepository.save(testRun, "system");

		log.debug("Created test run: {}", testRun.getId());

		// Save individual test results
		int savedResults = 0;
		if (request.getResults() != null) {
			for (CiTestResult ciResult : request.getResults()) {
				TestResultEntity result = new TestResultEntity();
				result.setId(UUID.randomUUID().toString());
				result.setRunId(testRun.getId());
				result.setTestName(ciResult.getTestName());
				result.setClassName(ciResult.getClassName());
				result.setMethodName(ciResult.getMethodName());
				result.setStatus(parseResultStatus(ciResult.getStatus()));
				result.setDurationMs(ciResult.getDurationMs());
				result.setErrorMessage(ciResult.getErrorMessage());
				result.setStackTrace(ciResult.getStackTrace());
				result.setScreenshots(ciResult.getScreenshots());
				result.setVideos(ciResult.getVideos());

				testResultRepository.saveInSubcollection(testRun.getId(), result, "system");
				savedResults++;
			}
		}

		log.info("Saved {} test results for run {}", savedResults, testRun.getId());

		// Build response
		Map<String, Object> response = Map.of("success", true, "runId", testRun.getId(), "status", testRun.getStatus(),
				"totalTests", testRun.getTotalTests(), "savedResults", savedResults);

		return ResponseEntity.ok(response);
	}

	/**
	 * Validate Authorization header contains valid CI auth token
	 */
	private void validateAuthToken(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new StrategizException(ServiceConsoleErrorDetails.UNAUTHORIZED,
					"Missing or invalid Authorization header");
		}

		String token = authHeader.substring(7); // Remove "Bearer " prefix

		// For now, check against configured token
		// TODO: Support multiple CI tokens, token rotation, etc.
		if (ciAuthToken == null || ciAuthToken.isEmpty() || !ciAuthToken.equals(token)) {
			throw new StrategizException(ServiceConsoleErrorDetails.UNAUTHORIZED, "Invalid CI authentication token");
		}
	}

	/**
	 * Parse execution level string to enum
	 */
	private io.strategiz.data.testing.entity.TestExecutionLevel parseExecutionLevel(String level) {
		if (level == null || level.isEmpty()) {
			return io.strategiz.data.testing.entity.TestExecutionLevel.APP;
		}

		try {
			return io.strategiz.data.testing.entity.TestExecutionLevel
				.valueOf(level.toUpperCase().replace("-", "_"));
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid execution level '{}', defaulting to APP", level);
			return io.strategiz.data.testing.entity.TestExecutionLevel.APP;
		}
	}

	/**
	 * Parse run status string to enum
	 */
	private TestRunStatus parseRunStatus(String status) {
		if (status == null || status.isEmpty()) {
			return TestRunStatus.PENDING;
		}

		try {
			return TestRunStatus.valueOf(status.toUpperCase().replace("-", "_"));
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid run status '{}', defaulting to PENDING", status);
			return TestRunStatus.PENDING;
		}
	}

	/**
	 * Parse result status string to enum
	 */
	private TestResultStatus parseResultStatus(String status) {
		if (status == null || status.isEmpty()) {
			return TestResultStatus.ERROR;
		}

		try {
			return TestResultStatus.valueOf(status.toUpperCase().replace("-", "_"));
		}
		catch (IllegalArgumentException e) {
			log.warn("Invalid result status '{}', defaulting to ERROR", status);
			return TestResultStatus.ERROR;
		}
	}

	/**
	 * Parse ISO 8601 timestamp string to Instant
	 */
	private Instant parseInstant(String timestamp) {
		if (timestamp == null || timestamp.isEmpty()) {
			return Instant.now();
		}

		try {
			return Instant.parse(timestamp);
		}
		catch (Exception e) {
			log.warn("Invalid timestamp '{}', using current time", timestamp);
			return Instant.now();
		}
	}

	// === Request/Response DTOs ===

	/**
	 * Request body for CI test results Matches output from
	 * aggregate-test-results.py script
	 */
	public static class CiTestResultsRequest {

		private String appId;

		private String moduleId;

		private String suiteId;

		private String testId;

		private String level;

		private String status;

		private String startTime;

		private String endTime;

		private Long durationMs;

		private Integer totalTests;

		private Integer passedTests;

		private Integer failedTests;

		private Integer skippedTests;

		private Integer errorTests;

		private String commitHash;

		private String branch;

		private String workflowRunId;

		private String workflowRunUrl;

		private List<CiTestResult> results;

		// Getters and setters

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

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public String getStartTime() {
			return startTime;
		}

		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public String getEndTime() {
			return endTime;
		}

		public void setEndTime(String endTime) {
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

		public List<CiTestResult> getResults() {
			return results;
		}

		public void setResults(List<CiTestResult> results) {
			this.results = results;
		}

	}

	/**
	 * Individual test result from CI
	 */
	public static class CiTestResult {

		private String testName;

		private String className;

		private String methodName;

		private String status;

		private Long durationMs;

		private String errorMessage;

		private String stackTrace;

		private List<String> screenshots;

		private List<String> videos;

		// Getters and setters

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
