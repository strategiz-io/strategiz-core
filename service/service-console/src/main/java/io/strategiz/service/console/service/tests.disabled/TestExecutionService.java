package io.strategiz.service.console.service.tests;

import io.strategiz.data.testing.entity.*;
import io.strategiz.data.testing.repository.*;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.service.tests.executor.TestExecutor;
import io.strategiz.service.console.service.tests.executor.TestExecutorFactory;
import io.strategiz.service.console.service.tests.model.TestRunRequest;
import io.strategiz.service.console.service.tests.parser.TestResultParser;
import io.strategiz.service.console.service.tests.parser.TestResultParserFactory;
import io.strategiz.service.console.websocket.TestStreamingHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for orchestrating test execution at all granularity levels.
 *
 * Supports:
 * - App-level execution (run all tests for an app)
 * - Module-level execution (run all tests in a module)
 * - Suite-level execution (run all tests in a suite)
 * - Individual test execution (run a single test method)
 *
 * Features:
 * - Real-time log streaming via WebSocket
 * - Result parsing and storage in Firestore
 * - Timeout handling
 * - Support for multiple frameworks (Playwright, Jest, JUnit, pytest, Cucumber)
 */
@Service
public class TestExecutionService extends BaseService {

    private static final String MODULE_NAME = "TEST_EXECUTION";
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;

    private final TestAppRepository testAppRepository;
    private final TestModuleRepository testModuleRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;
    private final TestExecutorFactory executorFactory;
    private final TestResultParserFactory parserFactory;
    private final TestStreamingHandler streamingHandler;

    @Autowired
    public TestExecutionService(
            TestAppRepository testAppRepository,
            TestModuleRepository testModuleRepository,
            TestSuiteRepository testSuiteRepository,
            TestCaseRepository testCaseRepository,
            TestRunRepository testRunRepository,
            TestResultRepository testResultRepository,
            TestExecutorFactory executorFactory,
            TestResultParserFactory parserFactory,
            TestStreamingHandler streamingHandler) {
        this.testAppRepository = testAppRepository;
        this.testModuleRepository = testModuleRepository;
        this.testSuiteRepository = testSuiteRepository;
        this.testCaseRepository = testCaseRepository;
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
        this.executorFactory = executorFactory;
        this.parserFactory = parserFactory;
        this.streamingHandler = streamingHandler;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Execute all tests for an application.
     */
    @Async
    public CompletableFuture<TestRunEntity> executeAppTests(String appId, TestRunRequest request, String executedBy) {
        log.info("Starting app-level test execution for app: {}", appId);

        TestAppEntity app = testAppRepository.findById(appId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_APP_NOT_FOUND,
                        "Test app not found: " + appId));

        TestRunEntity run = createTestRun(TestExecutionLevel.APP, appId, null, null, null, request, executedBy);
        run = testRunRepository.save(run, executedBy);

        try {
            // Get all modules for this app and execute them
            List<TestModuleEntity> modules = testModuleRepository.findByAppId(appId);

            int totalPassed = 0;
            int totalFailed = 0;
            int totalSkipped = 0;
            int totalErrors = 0;

            for (TestModuleEntity module : modules) {
                TestRunEntity moduleRun = executeModuleTestsSync(appId, module.getId(), request, executedBy);
                totalPassed += moduleRun.getPassedTests();
                totalFailed += moduleRun.getFailedTests();
                totalSkipped += moduleRun.getSkippedTests();
                totalErrors += moduleRun.getErrorTests();
            }

            run.setStatus(totalFailed > 0 || totalErrors > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
            run.setTotalTests(totalPassed + totalFailed + totalSkipped + totalErrors);
            run.setPassedTests(totalPassed);
            run.setFailedTests(totalFailed);
            run.setSkippedTests(totalSkipped);
            run.setErrorTests(totalErrors);
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());

            run = testRunRepository.save(run, executedBy);
            log.info("App-level test execution completed for app: {} - Status: {}", appId, run.getStatus());

            return CompletableFuture.completedFuture(run);

        } catch (Exception e) {
            log.error("Error executing app-level tests for app: {}", appId, e);
            run.setStatus(TestRunStatus.ERROR);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());
            testRunRepository.save(run, executedBy);
            throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                    "Failed to execute app-level tests: " + e.getMessage());
        }
    }

    /**
     * Execute all tests for a module.
     */
    @Async
    public CompletableFuture<TestRunEntity> executeModuleTests(String appId, String moduleId, TestRunRequest request, String executedBy) {
        log.info("Starting module-level test execution for module: {}", moduleId);
        TestRunEntity run = executeModuleTestsSync(appId, moduleId, request, executedBy);
        return CompletableFuture.completedFuture(run);
    }

    /**
     * Synchronous module test execution (for internal use).
     */
    private TestRunEntity executeModuleTestsSync(String appId, String moduleId, TestRunRequest request, String executedBy) {
        TestModuleEntity module = testModuleRepository.findById(moduleId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_MODULE_NOT_FOUND,
                        "Test module not found: " + moduleId));

        TestRunEntity run = createTestRun(TestExecutionLevel.MODULE, appId, moduleId, null, null, request, executedBy);
        run = testRunRepository.save(run, executedBy);

        try {
            // Get executor for this framework
            TestExecutor executor = executorFactory.getExecutor(module.getFramework());

            // Execute tests
            Process process = executor.executeModule(module, request);

            // Stream and capture output
            StringBuilder logs = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                    // Stream to WebSocket clients in real-time
                    streamingHandler.broadcastLogLine(run.getId(), line);
                    log.debug("Test output: {}", line);
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!completed) {
                process.destroy();
                throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                        "Test execution timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes");
            }

            int exitCode = process.exitValue();
            String output = logs.toString();

            // Parse results
            TestResultParser parser = parserFactory.getParser(module.getFramework());
            List<TestResultEntity> results = parser.parseResults(output, run.getId());

            // Save results
            for (TestResultEntity result : results) {
                testResultRepository.saveInSubcollection(run.getId(), result, executedBy);
            }

            // Calculate summary
            int passed = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.PASSED).count();
            int failed = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.FAILED).count();
            int skipped = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.SKIPPED).count();
            int errors = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.ERROR).count();

            run.setStatus(failed > 0 || errors > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
            run.setTotalTests(results.size());
            run.setPassedTests(passed);
            run.setFailedTests(failed);
            run.setSkippedTests(skipped);
            run.setErrorTests(errors);
            run.setLogs(output.substring(0, Math.min(output.length(), 10_000_000))); // Limit to 10MB
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());

            run = testRunRepository.save(run, executedBy);
            log.info("Module-level test execution completed for module: {} - Status: {}", moduleId, run.getStatus());

            return run;

        } catch (Exception e) {
            log.error("Error executing module-level tests for module: {}", moduleId, e);
            run.setStatus(TestRunStatus.ERROR);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());
            testRunRepository.save(run, executedBy);
            throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                    "Failed to execute module-level tests: " + e.getMessage());
        }
    }

    /**
     * Execute all tests in a suite.
     */
    @Async
    public CompletableFuture<TestRunEntity> executeSuiteTests(String appId, String moduleId, String suiteId, TestRunRequest request, String executedBy) {
        log.info("Starting suite-level test execution for suite: {}", suiteId);

        TestSuiteEntity suite = testSuiteRepository.findById(suiteId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_SUITE_NOT_FOUND,
                        "Test suite not found: " + suiteId));

        TestModuleEntity module = testModuleRepository.findById(moduleId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_MODULE_NOT_FOUND,
                        "Test module not found: " + moduleId));

        TestRunEntity run = createTestRun(TestExecutionLevel.SUITE, appId, moduleId, suiteId, null, request, executedBy);
        run = testRunRepository.save(run, executedBy);

        try {
            // Get executor for this framework
            TestExecutor executor = executorFactory.getExecutor(module.getFramework());

            // Execute suite
            Process process = executor.executeSuite(suite, request);

            // Stream and capture output
            StringBuilder logs = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                    // Stream to WebSocket clients in real-time
                    streamingHandler.broadcastLogLine(run.getId(), line);
                    log.debug("Test output: {}", line);
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!completed) {
                process.destroy();
                throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                        "Test execution timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes");
            }

            String output = logs.toString();

            // Parse results
            TestResultParser parser = parserFactory.getParser(module.getFramework());
            List<TestResultEntity> results = parser.parseResults(output, run.getId());

            // Save results
            for (TestResultEntity result : results) {
                testResultRepository.saveInSubcollection(run.getId(), result, executedBy);
            }

            // Calculate summary
            int passed = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.PASSED).count();
            int failed = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.FAILED).count();
            int skipped = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.SKIPPED).count();
            int errors = (int) results.stream().filter(r -> r.getStatus() == TestResultStatus.ERROR).count();

            run.setStatus(failed > 0 || errors > 0 ? TestRunStatus.FAILED : TestRunStatus.PASSED);
            run.setTotalTests(results.size());
            run.setPassedTests(passed);
            run.setFailedTests(failed);
            run.setSkippedTests(skipped);
            run.setErrorTests(errors);
            run.setLogs(output.substring(0, Math.min(output.length(), 10_000_000))); // Limit to 10MB
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());

            run = testRunRepository.save(run, executedBy);
            log.info("Suite-level test execution completed for suite: {} - Status: {}", suiteId, run.getStatus());

            return CompletableFuture.completedFuture(run);

        } catch (Exception e) {
            log.error("Error executing suite-level tests for suite: {}", suiteId, e);
            run.setStatus(TestRunStatus.ERROR);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());
            testRunRepository.save(run, executedBy);
            throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                    "Failed to execute suite-level tests: " + e.getMessage());
        }
    }

    /**
     * Execute a single test method.
     */
    @Async
    public CompletableFuture<TestRunEntity> executeIndividualTest(String appId, String moduleId, String suiteId,
                                                                   String testId, TestRunRequest request, String executedBy) {
        log.info("Starting individual test execution for test: {}", testId);

        TestCaseEntity testCase = testCaseRepository.findById(testId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_CASE_NOT_FOUND,
                        "Test case not found: " + testId));

        TestModuleEntity module = testModuleRepository.findById(moduleId)
                .orElseThrow(() -> new StrategizException(ServiceConsoleErrorDetails.TEST_MODULE_NOT_FOUND,
                        "Test module not found: " + moduleId));

        TestRunEntity run = createTestRun(TestExecutionLevel.TEST, appId, moduleId, suiteId, testId, request, executedBy);
        run = testRunRepository.save(run, executedBy);

        try {
            // Get executor for this framework
            TestExecutor executor = executorFactory.getExecutor(module.getFramework());

            // Execute individual test
            Process process = executor.executeTest(testCase, request);

            // Stream and capture output
            StringBuilder logs = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                    // Stream to WebSocket clients in real-time
                    streamingHandler.broadcastLogLine(run.getId(), line);
                    log.debug("Test output: {}", line);
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!completed) {
                process.destroy();
                throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                        "Test execution timed out after " + DEFAULT_TIMEOUT_MINUTES + " minutes");
            }

            String output = logs.toString();

            // Parse results
            TestResultParser parser = parserFactory.getParser(module.getFramework());
            List<TestResultEntity> results = parser.parseResults(output, run.getId());

            // Should only be one result for individual test
            if (!results.isEmpty()) {
                TestResultEntity result = results.get(0);
                testResultRepository.saveInSubcollection(run.getId(), result, executedBy);

                run.setStatus(result.getStatus() == TestResultStatus.PASSED ? TestRunStatus.PASSED : TestRunStatus.FAILED);
                run.setTotalTests(1);
                run.setPassedTests(result.getStatus() == TestResultStatus.PASSED ? 1 : 0);
                run.setFailedTests(result.getStatus() == TestResultStatus.FAILED ? 1 : 0);
                run.setSkippedTests(result.getStatus() == TestResultStatus.SKIPPED ? 1 : 0);
                run.setErrorTests(result.getStatus() == TestResultStatus.ERROR ? 1 : 0);
            }

            run.setLogs(output.substring(0, Math.min(output.length(), 10_000_000))); // Limit to 10MB
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());

            run = testRunRepository.save(run, executedBy);
            log.info("Individual test execution completed for test: {} - Status: {}", testId, run.getStatus());

            return CompletableFuture.completedFuture(run);

        } catch (Exception e) {
            log.error("Error executing individual test: {}", testId, e);
            run.setStatus(TestRunStatus.ERROR);
            run.setErrorMessage(e.getMessage());
            run.setEndTime(Instant.now());
            run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());
            testRunRepository.save(run, executedBy);
            throw new StrategizException(ServiceConsoleErrorDetails.TEST_EXECUTION_FAILED,
                    "Failed to execute individual test: " + e.getMessage());
        }
    }

    /**
     * Create a new test run entity.
     */
    private TestRunEntity createTestRun(TestExecutionLevel level, String appId, String moduleId, String suiteId,
                                       String testId, TestRunRequest request, String executedBy) {
        TestRunEntity run = new TestRunEntity();
        run.setLevel(level);
        run.setAppId(appId);
        run.setModuleId(moduleId);
        run.setSuiteId(suiteId);
        run.setTestId(testId);
        run.setTrigger(request.getTrigger() != null ? request.getTrigger() : TestTrigger.MANUAL);
        run.setStatus(TestRunStatus.RUNNING);
        run.setStartTime(Instant.now());
        run.setExecutedBy(executedBy);
        run.setTotalTests(0);
        run.setPassedTests(0);
        run.setFailedTests(0);
        run.setSkippedTests(0);
        run.setErrorTests(0);
        return run;
    }
}
