package io.strategiz.service.console.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.base.exception.ServiceBaseErrorDetails;
import io.strategiz.service.console.model.response.TestRunResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for executing Playwright E2E tests.
 */
@Service
public class TestRunnerService extends BaseService {

	private static final String MODULE_NAME = "TEST_RUNNER";

	@Value("${test.runner.ui-directory:../strategiz-ui}")
	private String uiDirectory;

	@Value("${test.runner.timeout-minutes:10}")
	private int timeoutMinutes;

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Execute Playwright tests for the specified suite.
	 * @param suiteId Test suite ID (system, smoke, journeys-auth, etc.)
	 * @return Test results
	 */
	public TestRunResponse runTests(String suiteId) {
		log.info("Running Playwright tests for suite: {}", suiteId);
		Instant startTime = Instant.now();

		TestRunResponse response = new TestRunResponse();
		response.setSuite(suiteId);
		response.setStatus("running");
		response.setStartedAt(startTime);
		response.setProgress(0);

		try {
			// Build Playwright command
			List<String> command = new ArrayList<>();
			command.add("npx");
			command.add("playwright");
			command.add("test");
			command.add("--project=" + suiteId);

			log.debug("Executing command: {}", String.join(" ", command));

			// Execute command
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(new java.io.File(uiDirectory));
			processBuilder.redirectErrorStream(true); // Combine stdout and stderr

			Process process = processBuilder.start();

			// Read output
			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
					log.debug("Test output: {}", line);
				}
			}

			// Wait for completion with timeout
			boolean completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);

			if (!completed) {
				process.destroy();
				log.error("Test execution timed out after {} minutes", timeoutMinutes);
				throw new StrategizException(ServiceBaseErrorDetails.INTERNAL_ERROR,
						"Test execution timed out after " + timeoutMinutes + " minutes");
			}

			int exitCode = process.exitValue();
			String outputStr = output.toString();

			log.debug("Test execution completed with exit code: {}", exitCode);
			log.debug("Full output:\n{}", outputStr);

			// Parse output to extract results
			parseTestResults(outputStr, response);

			// Calculate duration
			long duration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
			response.setDuration(duration);
			response.setProgress(100);

			// Set status based on results
			if (response.getFailed() > 0) {
				response.setStatus("failed");
			}
			else if (response.getPassed() > 0) {
				response.setStatus("passed");
			}
			else {
				response.setStatus("failed");
			}

			log.info("Test suite {} completed: {} passed, {} failed, duration: {}ms", suiteId, response.getPassed(),
					response.getFailed(), duration);

			return response;

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error executing tests for suite {}: {}", suiteId, e.getMessage(), e);
			throw new StrategizException(ServiceBaseErrorDetails.INTERNAL_ERROR,
					"Failed to execute tests: " + e.getMessage());
		}
	}

	/**
	 * Parse Playwright output to extract test results. Playwright output format: "5
	 * passed (15s)"
	 */
	private void parseTestResults(String output, TestRunResponse response) {
		// Pattern for "X passed (Ys)" or "X passed, Y failed (Zs)"
		Pattern passedPattern = Pattern.compile("(\\d+)\\s+passed");
		Pattern failedPattern = Pattern.compile("(\\d+)\\s+failed");

		Matcher passedMatcher = passedPattern.matcher(output);
		Matcher failedMatcher = failedPattern.matcher(output);

		int passed = 0;
		int failed = 0;

		if (passedMatcher.find()) {
			passed = Integer.parseInt(passedMatcher.group(1));
		}

		if (failedMatcher.find()) {
			failed = Integer.parseInt(failedMatcher.group(1));
		}

		response.setPassed(passed);
		response.setFailed(failed);
		response.setTotal(passed + failed);

		log.debug("Parsed results: {} passed, {} failed", passed, failed);
	}

}
