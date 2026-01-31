package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestResultStatus;
import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.data.testing.entity.TestRunStatus;
import io.strategiz.data.testing.entity.TestExecutionLevel;
import io.strategiz.data.testing.entity.TestTrigger;
import io.strategiz.data.testing.repository.TestAppRepository;
import io.strategiz.data.testing.repository.TestCaseRepository;
import io.strategiz.data.testing.repository.TestModuleRepository;
import io.strategiz.data.testing.repository.TestRunRepository;
import io.strategiz.data.testing.repository.TestResultRepository;
import io.strategiz.service.console.service.tests.model.TestRunRequest;
import io.strategiz.service.console.websocket.TestStreamingHandler;
import io.strategiz.service.console.websocket.message.TestCompletionMessage;
import io.strategiz.service.console.websocket.message.TestProgressMessage;
import io.strategiz.service.console.websocket.message.TestResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for executing tests at various granularity levels. Supports Playwright
 * (frontend), Maven/JUnit (backend), and Pytest (Python) test execution.
 */
@Service
public class TestExecutionService {

	private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);

	// Output parsing patterns
	private static final Pattern PLAYWRIGHT_RESULT_PATTERN = Pattern
		.compile("(\\d+) passed.*?(\\d+)? failed.*?(\\d+)? skipped");

	private static final Pattern MAVEN_RESULT_PATTERN = Pattern
		.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");

	private static final Pattern PYTEST_RESULT_PATTERN = Pattern
		.compile("(\\d+) passed.*?(\\d+)? failed.*?(\\d+)? error");

	@Value("${strategiz.tests.ui-directory:../strategiz-ui}")
	private String uiDirectory;

	@Value("${strategiz.tests.backend-directory:.}")
	private String backendDirectory;

	@Value("${strategiz.tests.timeout-minutes:10}")
	private int timeoutMinutes;

	private final TestAppRepository testAppRepository;

	private final TestModuleRepository testModuleRepository;

	private final TestCaseRepository testCaseRepository;

	private final TestRunRepository testRunRepository;

	private final TestResultRepository testResultRepository;

	private final TestStreamingHandler streamingHandler;

	@Autowired
	public TestExecutionService(TestAppRepository testAppRepository, TestModuleRepository testModuleRepository,
			TestCaseRepository testCaseRepository, TestRunRepository testRunRepository,
			TestResultRepository testResultRepository, TestStreamingHandler streamingHandler) {
		this.testAppRepository = testAppRepository;
		this.testModuleRepository = testModuleRepository;
		this.testCaseRepository = testCaseRepository;
		this.testRunRepository = testRunRepository;
		this.testResultRepository = testResultRepository;
		this.streamingHandler = streamingHandler;
	}

	/**
	 * Execute all tests for an application
	 */
	public CompletableFuture<TestRunEntity> executeAppTests(String appId, TestRunRequest request, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting app tests for appId: {}, userId: {}", appId, userId);

			TestRunEntity testRun = createTestRun(appId, null, null, null, TestExecutionLevel.APP, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				TestAppEntity app = testAppRepository.findById(appId).orElse(null);
				if (app == null) {
					return failTestRun(testRun, "Application not found: " + appId);
				}

				// Execute based on app type
				if ("frontend-e2e".equals(appId)) {
					executePlaywrightTests(testRun, null, null, request);
				}
				else if ("backend-api".equals(appId)) {
					executeMavenTests(testRun, null, request);
				}
				else if ("python-execution".equals(appId)) {
					executePytestTests(testRun, null, request);
				}

				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing app tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Execute all tests for a module
	 */
	public CompletableFuture<TestRunEntity> executeModuleTests(String appId, String moduleId, TestRunRequest request,
			String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting module tests for appId: {}, moduleId: {}, userId: {}", appId, moduleId, userId);

			TestRunEntity testRun = createTestRun(appId, moduleId, null, null, TestExecutionLevel.MODULE, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				// Execute based on app type
				if ("frontend-e2e".equals(appId)) {
					// moduleId is journey-auth, journey-labs, etc.
					String journey = moduleId.replace("journey-", "");
					executePlaywrightTests(testRun, journey, null, request);
				}
				else if ("backend-api".equals(appId)) {
					// moduleId is service-auth, service-labs, etc.
					executeMavenTests(testRun, moduleId, request);
				}
				else if ("python-execution".equals(appId)) {
					executePytestTests(testRun, moduleId, request);
				}

				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing module tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Execute all tests for a suite
	 */
	public CompletableFuture<TestRunEntity> executeSuiteTests(String appId, String moduleId, String suiteId,
			TestRunRequest request, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting suite tests for suiteId: {}, userId: {}", suiteId, userId);

			TestRunEntity testRun = createTestRun(appId, moduleId, suiteId, null, TestExecutionLevel.SUITE, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				// Execute tests for this specific suite
				if ("frontend-e2e".equals(appId)) {
					executePlaywrightTests(testRun, null, suiteId, request);
				}
				else if ("backend-api".equals(appId)) {
					executeMavenSuite(testRun, moduleId, suiteId, request);
				}

				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing suite tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Execute a single test
	 */
	public CompletableFuture<TestRunEntity> executeIndividualTest(String appId, String moduleId, String suiteId,
			String testId, TestRunRequest request, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting individual test for testId: {}, userId: {}", testId, userId);

			TestRunEntity testRun = createTestRun(appId, moduleId, suiteId, testId, TestExecutionLevel.TEST, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				TestCaseEntity testCase = testCaseRepository.findById(testId).orElse(null);
				if (testCase == null) {
					return failTestRun(testRun, "Test case not found: " + testId);
				}

				// Execute single test
				if ("frontend-e2e".equals(appId)) {
					executePlaywrightSingleTest(testRun, testCase, request);
				}
				else if ("backend-api".equals(appId)) {
					executeMavenSingleTest(testRun, testCase, request);
				}

				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing individual test", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	// ===============================
	// Frontend Journey/Page Execution
	// ===============================

	/**
	 * Run frontend tests by journey (auth, labs, portfolio, settings)
	 */
	public CompletableFuture<TestRunEntity> runFrontendJourney(String journey, String userId) {
		return executeModuleTests("frontend-e2e", "journey-" + journey, new TestRunRequest(), userId);
	}

	/**
	 * Run frontend tests by page (/dashboard, /labs, etc.)
	 */
	public CompletableFuture<TestRunEntity> runFrontendPage(String page, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting page tests for page: {}, userId: {}", page, userId);

			TestRunEntity testRun = createTestRun("frontend-e2e", null, null, null, TestExecutionLevel.MODULE, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				// Use Playwright grep to filter by page tag
				String grepPattern = "@page:" + page;
				executePlaywrightWithGrep(testRun, grepPattern);
				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing page tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Run all frontend tests
	 */
	public CompletableFuture<TestRunEntity> runAllFrontend(String userId) {
		return executeAppTests("frontend-e2e", new TestRunRequest(), userId);
	}

	// ===============================
	// Backend Module/API Execution
	// ===============================

	/**
	 * Run backend tests by module (service-auth, service-labs, etc.)
	 */
	public CompletableFuture<TestRunEntity> runBackendModule(String module, String userId) {
		return executeModuleTests("backend-api", module, new TestRunRequest(), userId);
	}

	/**
	 * Run backend tests by API path (/v1/auth/*, /v1/labs/*, etc.)
	 */
	public CompletableFuture<TestRunEntity> runBackendApi(String apiPath, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Starting API tests for apiPath: {}, userId: {}", apiPath, userId);

			TestRunEntity testRun = createTestRun("backend-api", null, null, null, TestExecutionLevel.MODULE, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRunRepository.save(testRun, "system");

			try {
				// Map API path to service module
				String module = mapApiPathToModule(apiPath);
				if (module != null) {
					executeMavenTests(testRun, module, new TestRunRequest());
				}
				else {
					return failTestRun(testRun, "No module found for API path: " + apiPath);
				}
				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error executing API tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Run all backend tests
	 */
	public CompletableFuture<TestRunEntity> runAllBackend(String userId) {
		return executeAppTests("backend-api", new TestRunRequest(), userId);
	}

	// ===============================
	// Test Run Management
	// ===============================

	/**
	 * Get test run by ID
	 */
	public TestRunEntity getTestRun(String runId) {
		return testRunRepository.findById(runId).orElse(null);
	}

	/**
	 * Get test results for a run
	 */
	public List<TestResultEntity> getTestResults(String runId) {
		return testResultRepository.findByRunId(runId);
	}

	// ===============================
	// Re-run Failed Tests
	// ===============================

	/**
	 * Re-run all failed tests from a previous run
	 */
	public CompletableFuture<TestRunEntity> rerunFailedTests(String previousRunId, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Re-running failed tests from run: {} for user: {}", previousRunId, userId);

			// Get the previous test run
			TestRunEntity previousRun = testRunRepository.findById(previousRunId).orElse(null);
			if (previousRun == null) {
				log.warn("Previous test run not found: {}", previousRunId);
				return null;
			}

			// Get failed test results from the previous run
			List<TestResultEntity> previousResults = testResultRepository.findByRunId(previousRunId);
			List<String> failedTestIds = previousResults.stream()
				.filter(r -> r.getStatus() == TestResultStatus.FAILED || r.getStatus() == TestResultStatus.ERROR)
				.map(TestResultEntity::getTestName)
				.toList();

			if (failedTestIds.isEmpty()) {
				log.info("No failed tests to re-run from run: {}", previousRunId);
				return previousRun; // Return the original run if no failures
			}

			log.info("Re-running {} failed tests from run: {}", failedTestIds.size(), previousRunId);

			// Create a new test run for the re-run
			TestRunEntity newRun = createTestRun(previousRun.getAppId(), previousRun.getModuleId(),
					previousRun.getSuiteId(), null, TestExecutionLevel.TEST, userId);
			newRun.setStatus(TestRunStatus.RUNNING);
			newRun.setTotalTests(failedTestIds.size());
			testRunRepository.save(newRun, "system");

			try {
				// Execute each failed test
				for (String testId : failedTestIds) {
					TestCaseEntity testCase = testCaseRepository.findById(testId).orElse(null);
					if (testCase != null) {
						executeIndividualTestInRun(newRun, testCase);
					}
				}
				return completeTestRun(newRun);
			}
			catch (Exception e) {
				log.error("Error re-running failed tests", e);
				return failTestRun(newRun, e.getMessage());
			}
		});
	}

	/**
	 * Re-run specific tests by their IDs
	 */
	public CompletableFuture<TestRunEntity> rerunSpecificTests(List<String> testIds, String userId) {
		return CompletableFuture.supplyAsync(() -> {
			log.info("Re-running {} specific tests for user: {}", testIds.size(), userId);

			if (testIds.isEmpty()) {
				log.warn("No test IDs provided for re-run");
				return null;
			}

			// Get the first test to determine app/module
			TestCaseEntity firstTest = testCaseRepository.findById(testIds.get(0)).orElse(null);
			String appId = firstTest != null ? firstTest.getAppId() : "backend-api";
			String moduleId = firstTest != null ? firstTest.getModuleId() : null;

			// Create a new test run
			TestRunEntity testRun = createTestRun(appId, moduleId, null, null, TestExecutionLevel.TEST, userId);
			testRun.setStatus(TestRunStatus.RUNNING);
			testRun.setTotalTests(testIds.size());
			testRunRepository.save(testRun, "system");

			try {
				// Execute each test
				for (String testId : testIds) {
					TestCaseEntity testCase = testCaseRepository.findById(testId).orElse(null);
					if (testCase != null) {
						executeIndividualTestInRun(testRun, testCase);
					}
				}
				return completeTestRun(testRun);
			}
			catch (Exception e) {
				log.error("Error re-running specific tests", e);
				return failTestRun(testRun, e.getMessage());
			}
		});
	}

	/**
	 * Execute an individual test as part of a larger run (for re-runs)
	 */
	private void executeIndividualTestInRun(TestRunEntity testRun, TestCaseEntity testCase) {
		try {
			if ("frontend-e2e".equals(testCase.getAppId())) {
				executePlaywrightSingleTest(testRun, testCase, new TestRunRequest());
			}
			else if ("backend-api".equals(testCase.getAppId())) {
				executeMavenSingleTest(testRun, testCase, new TestRunRequest());
			}
		}
		catch (Exception e) {
			log.error("Error executing test: {}", testCase.getId(), e);
		}
	}

	// ===============================
	// Private Helper Methods
	// ===============================

	private TestRunEntity createTestRun(String appId, String moduleId, String suiteId, String testId,
			TestExecutionLevel level, String userId) {
		TestRunEntity testRun = new TestRunEntity();
		testRun.setId(UUID.randomUUID().toString());
		testRun.setAppId(appId);
		testRun.setModuleId(moduleId);
		testRun.setSuiteId(suiteId);
		testRun.setTestId(testId);
		testRun.setLevel(level);
		testRun.setTrigger(TestTrigger.MANUAL);
		testRun.setStatus(TestRunStatus.PENDING);
		testRun.setStartTime(Instant.now());
		testRun.setExecutedBy(userId);
		return testRun;
	}

	private TestRunEntity failTestRun(TestRunEntity testRun, String error) {
		testRun.setStatus(TestRunStatus.FAILED);
		testRun.setEndTime(Instant.now());
		testRun.setErrorMessage(error);
		testRunRepository.save(testRun, "system");

		// Broadcast failure completion
		broadcastCompletion(testRun, "failed");
		return testRun;
	}

	private TestRunEntity completeTestRun(TestRunEntity testRun) {
		testRun.setEndTime(Instant.now());
		testRun.setDurationMs(testRun.getEndTime().toEpochMilli() - testRun.getStartTime().toEpochMilli());

		// Determine final status based on results
		String status;
		if (testRun.getFailedTests() > 0 || testRun.getErrorTests() > 0) {
			testRun.setStatus(TestRunStatus.FAILED);
			status = "failed";
		}
		else if (testRun.getPassedTests() > 0) {
			testRun.setStatus(TestRunStatus.PASSED);
			status = "passed";
		}
		else {
			testRun.setStatus(TestRunStatus.PASSED);
			status = "passed";
		}

		testRunRepository.save(testRun, "system");

		// Broadcast completion
		broadcastCompletion(testRun, status);
		return testRun;
	}

	private void executePlaywrightTests(TestRunEntity testRun, String journey, String suiteId, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("npx");
			command.add("playwright");
			command.add("test");

			// Add grep filter for journey
			if (journey != null) {
				command.add("--grep");
				command.add("@journey:" + journey);
			}

			// Add specific test file for suite
			if (suiteId != null) {
				command.add(suiteId);
			}

			command.add("--reporter=list");

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(uiDirectory, "apps/web"));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parsePlaywrightOutput(testRun, line);
				}
			}

			boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			if (!finished) {
				process.destroyForcibly();
				testRun.setErrorMessage("Test execution timed out after " + timeoutMinutes + " minutes");
			}

			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Playwright tests", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executePlaywrightWithGrep(TestRunEntity testRun, String grepPattern) {
		try {
			List<String> command = new ArrayList<>();
			command.add("npx");
			command.add("playwright");
			command.add("test");
			command.add("--grep");
			command.add(grepPattern);
			command.add("--reporter=list");

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(uiDirectory, "apps/web"));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parsePlaywrightOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Playwright tests with grep", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executePlaywrightSingleTest(TestRunEntity testRun, TestCaseEntity testCase, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("npx");
			command.add("playwright");
			command.add("test");
			command.add("--grep");
			command.add(testCase.getDisplayName());
			command.add("--reporter=list");

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(uiDirectory, "apps/web"));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parsePlaywrightOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Playwright single test", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executeMavenTests(TestRunEntity testRun, String module, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("mvn");
			command.add("test");

			if (module != null) {
				command.add("-pl");
				command.add("service/" + module);
				command.add("-am");
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(backendDirectory));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parseMavenOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Maven tests", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executeMavenSuite(TestRunEntity testRun, String module, String suiteId, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("mvn");
			command.add("test");
			command.add("-pl");
			command.add("service/" + module);
			command.add("-Dtest=" + suiteId);

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(backendDirectory));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parseMavenOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Maven suite", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executeMavenSingleTest(TestRunEntity testRun, TestCaseEntity testCase, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("mvn");
			command.add("test");
			command.add("-pl");
			command.add("service/" + testCase.getModuleId());
			command.add("-Dtest=" + testCase.getClassName() + "#" + testCase.getMethodName());

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(backendDirectory));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parseMavenOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Maven single test", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void executePytestTests(TestRunEntity testRun, String module, TestRunRequest request) {
		try {
			List<String> command = new ArrayList<>();
			command.add("pytest");
			command.add("-v");

			if (module != null) {
				command.add("tests/" + module);
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(new File(backendDirectory, "application-strategy-execution"));
			pb.redirectErrorStream(true);

			Process process = pb.start();
			StringBuilder output = new StringBuilder();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					parsePytestOutput(testRun, line);
				}
			}

			process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
			testRun.setLogs(output.toString());
		}
		catch (Exception e) {
			log.error("Error executing Pytest tests", e);
			testRun.setErrorMessage(e.getMessage());
		}
	}

	private void parsePlaywrightOutput(TestRunEntity testRun, String line) {
		// Broadcast raw log line
		streamingHandler.broadcastLogLine(testRun.getId(), line);

		// Parse individual test results (Playwright list reporter format)
		// Format: " ✓ 1 [chromium] › auth/signin.spec.ts:12:5 › should sign in (1.2s)"
		// Format: " ✘ 2 [chromium] › auth/signin.spec.ts:24:5 › should fail login"
		if (line.contains("✓") || line.contains("✘") || line.contains("passed") || line.contains("failed")) {
			Pattern testLinePattern = Pattern
				.compile("([✓✘-])\\s+\\d+\\s+\\[\\w+\\]\\s+›\\s+(.+?)\\s+›\\s+(.+?)(?:\\s+\\((\\d+\\.?\\d*)s\\))?$");
			Matcher testMatcher = testLinePattern.matcher(line.trim());

			if (testMatcher.find()) {
				String statusIcon = testMatcher.group(1);
				String testFile = testMatcher.group(2);
				String testName = testMatcher.group(3);
				String durationStr = testMatcher.group(4);

				String status = "✓".equals(statusIcon) ? "passed" : "failed";
				Long durationMs = durationStr != null ? (long) (Double.parseDouble(durationStr) * 1000) : null;

				// Broadcast individual test result
				TestResultMessage resultMsg = new TestResultMessage(testRun.getId(), testFile + ":" + testName,
						testName, status);
				resultMsg.withSuiteName(testFile);
				if (durationMs != null) {
					resultMsg.withDuration(durationMs);
				}
				streamingHandler.broadcastTestResult(resultMsg);
			}
		}

		// Parse final summary
		if (line.contains("passed")) {
			Matcher matcher = PLAYWRIGHT_RESULT_PATTERN.matcher(line);
			if (matcher.find()) {
				testRun.setPassedTests(Integer.parseInt(matcher.group(1)));
				if (matcher.group(2) != null) {
					testRun.setFailedTests(Integer.parseInt(matcher.group(2)));
				}
				if (matcher.group(3) != null) {
					testRun.setSkippedTests(Integer.parseInt(matcher.group(3)));
				}

				// Update total and broadcast progress
				int total = testRun.getPassedTests() + testRun.getFailedTests() + testRun.getSkippedTests();
				testRun.setTotalTests(total);
				broadcastProgress(testRun);
			}
		}
	}

	private void parseMavenOutput(TestRunEntity testRun, String line) {
		// Broadcast raw log line
		streamingHandler.broadcastLogLine(testRun.getId(), line);

		// Parse individual test results from Surefire output
		// Format: "Running io.strategiz.service.auth.controller.AuthControllerTest"
		// Format: "Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.123
		// s - in
		// io.strategiz...Test"
		Pattern runningPattern = Pattern.compile("Running\\s+([\\w.]+)");
		Matcher runningMatcher = runningPattern.matcher(line);
		if (runningMatcher.find()) {
			String className = runningMatcher.group(1);
			// Broadcast test start
			TestResultMessage startMsg = new TestResultMessage(testRun.getId(), className, className, "running");
			streamingHandler.broadcastTestResult(startMsg);
		}

		// Parse individual test method results
		// Format: "[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed:
		// 0.056 s --
		// in ...ClassName"
		Pattern suiteResultPattern = Pattern
			.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+).*?in ([\\w.]+)");
		Matcher suiteResultMatcher = suiteResultPattern.matcher(line);
		if (suiteResultMatcher.find()) {
			int suiteTotal = Integer.parseInt(suiteResultMatcher.group(1));
			int suiteFailures = Integer.parseInt(suiteResultMatcher.group(2));
			int suiteErrors = Integer.parseInt(suiteResultMatcher.group(3));
			int suiteSkipped = Integer.parseInt(suiteResultMatcher.group(4));
			String className = suiteResultMatcher.group(5);

			int suitePassed = suiteTotal - suiteFailures - suiteErrors - suiteSkipped;
			String status = (suiteFailures > 0 || suiteErrors > 0) ? "failed" : "passed";

			// Broadcast suite result
			TestResultMessage resultMsg = new TestResultMessage(testRun.getId(), className, className, status);
			resultMsg.withSuiteName(className);
			streamingHandler.broadcastTestResult(resultMsg);
		}

		// Update totals from summary line
		if (line.contains("Tests run:")) {
			Matcher matcher = MAVEN_RESULT_PATTERN.matcher(line);
			if (matcher.find()) {
				int total = Integer.parseInt(matcher.group(1));
				int failures = Integer.parseInt(matcher.group(2));
				int errors = Integer.parseInt(matcher.group(3));
				int skipped = Integer.parseInt(matcher.group(4));

				testRun.setTotalTests(testRun.getTotalTests() + total);
				testRun.setPassedTests(testRun.getPassedTests() + (total - failures - errors - skipped));
				testRun.setFailedTests(testRun.getFailedTests() + failures);
				testRun.setErrorTests(testRun.getErrorTests() + errors);
				testRun.setSkippedTests(testRun.getSkippedTests() + skipped);

				// Broadcast progress
				broadcastProgress(testRun);
			}
		}
	}

	private void parsePytestOutput(TestRunEntity testRun, String line) {
		// Broadcast raw log line
		streamingHandler.broadcastLogLine(testRun.getId(), line);

		// Parse individual test results from pytest verbose output
		// Format: "tests/test_module.py::test_function PASSED [25%]"
		// Format: "tests/test_module.py::TestClass::test_method FAILED [50%]"
		Pattern testResultPattern = Pattern.compile("(tests/[\\w/]+\\.py::[\\w:]+)\\s+(PASSED|FAILED|SKIPPED|ERROR)");
		Matcher testMatcher = testResultPattern.matcher(line);
		if (testMatcher.find()) {
			String testId = testMatcher.group(1);
			String status = testMatcher.group(2).toLowerCase();

			// Extract test name from full path
			String testName = testId.substring(testId.lastIndexOf("::") + 2);

			// Broadcast individual test result
			TestResultMessage resultMsg = new TestResultMessage(testRun.getId(), testId, testName, status);
			resultMsg.withSuiteName(testId.substring(0, testId.indexOf("::")));
			streamingHandler.broadcastTestResult(resultMsg);
		}

		// Parse final summary
		if (line.contains("passed") || line.contains("failed") || line.contains("error")) {
			Matcher matcher = PYTEST_RESULT_PATTERN.matcher(line);
			if (matcher.find()) {
				testRun.setPassedTests(Integer.parseInt(matcher.group(1)));
				if (matcher.group(2) != null) {
					testRun.setFailedTests(Integer.parseInt(matcher.group(2)));
				}
				if (matcher.group(3) != null) {
					testRun.setErrorTests(Integer.parseInt(matcher.group(3)));
				}

				// Update total and broadcast progress
				int total = testRun.getPassedTests() + testRun.getFailedTests() + testRun.getErrorTests();
				testRun.setTotalTests(total);
				broadcastProgress(testRun);
			}
		}
	}

	private String mapApiPathToModule(String apiPath) {
		if (apiPath.contains("/auth")) {
			return "service-auth";
		}
		if (apiPath.contains("/labs")) {
			return "service-labs";
		}
		if (apiPath.contains("/users") || apiPath.contains("/profiles")) {
			return "service-profile";
		}
		if (apiPath.contains("/console")) {
			return "service-console";
		}
		if (apiPath.contains("/strategies")) {
			return "service-my-strategies";
		}
		if (apiPath.contains("/portfolio")) {
			return "service-portfolio";
		}
		if (apiPath.contains("/marketdata")) {
			return "service-marketdata";
		}
		return null;
	}

	// ===============================
	// WebSocket Broadcasting Helpers
	// ===============================

	/**
	 * Broadcast progress update to connected WebSocket clients
	 */
	private void broadcastProgress(TestRunEntity testRun) {
		int total = testRun.getTotalTests();
		int completed = testRun.getPassedTests() + testRun.getFailedTests() + testRun.getSkippedTests()
				+ testRun.getErrorTests();
		long elapsedMs = Instant.now().toEpochMilli() - testRun.getStartTime().toEpochMilli();

		// Estimate remaining time based on current rate
		Long estimatedRemainingMs = null;
		if (completed > 0 && total > completed) {
			long avgTimePerTest = elapsedMs / completed;
			estimatedRemainingMs = avgTimePerTest * (total - completed);
		}

		TestProgressMessage progressMsg = new TestProgressMessage(testRun.getId())
			.withCounts(total, completed, testRun.getPassedTests(), testRun.getFailedTests(), testRun.getSkippedTests())
			.withTiming(elapsedMs, estimatedRemainingMs);

		streamingHandler.broadcastProgress(progressMsg);
	}

	/**
	 * Broadcast completion message to connected WebSocket clients
	 */
	private void broadcastCompletion(TestRunEntity testRun, String status) {
		TestCompletionMessage completionMsg = new TestCompletionMessage(testRun.getId(), status)
			.withSummary(testRun.getTotalTests(), testRun.getPassedTests(), testRun.getFailedTests(),
					testRun.getSkippedTests(), testRun.getErrorTests())
			.withDuration(testRun.getDurationMs());

		if (testRun.getErrorMessage() != null) {
			completionMsg.withError(testRun.getErrorMessage());
		}

		streamingHandler.broadcastTestCompletion(completionMsg);
	}

}
