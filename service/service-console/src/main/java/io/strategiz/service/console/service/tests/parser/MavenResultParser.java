package io.strategiz.service.console.service.tests.parser;

import io.strategiz.data.testing.entity.TestResultEntity;
import io.strategiz.data.testing.entity.TestResultStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for JUnit (Maven Surefire) test results.
 */
@Component
public class MavenResultParser implements TestResultParser {

    private static final Pattern TESTS_RUN_PATTERN = Pattern.compile("Tests run: (\\d+)");
    private static final Pattern FAILURES_PATTERN = Pattern.compile("Failures: (\\d+)");
    private static final Pattern ERRORS_PATTERN = Pattern.compile("Errors: (\\d+)");
    private static final Pattern SKIPPED_PATTERN = Pattern.compile("Skipped: (\\d+)");

    @Override
    public List<TestResultEntity> parseResults(String output, String runId) {
        List<TestResultEntity> results = new ArrayList<>();

        // Parse Maven Surefire output
        int testsRun = extractCount(output, TESTS_RUN_PATTERN);
        int failures = extractCount(output, FAILURES_PATTERN);
        int errors = extractCount(output, ERRORS_PATTERN);
        int skipped = extractCount(output, SKIPPED_PATTERN);

        int passed = testsRun - failures - errors - skipped;

        // Create aggregate result entities
        for (int i = 0; i < passed; i++) {
            results.add(createResult(runId, "test_" + i, TestResultStatus.PASSED));
        }
        for (int i = 0; i < failures; i++) {
            results.add(createResult(runId, "test_failed_" + i, TestResultStatus.FAILED));
        }
        for (int i = 0; i < errors; i++) {
            results.add(createResult(runId, "test_error_" + i, TestResultStatus.ERROR));
        }
        for (int i = 0; i < skipped; i++) {
            results.add(createResult(runId, "test_skipped_" + i, TestResultStatus.SKIPPED));
        }

        return results;
    }

    private int extractCount(String output, Pattern pattern) {
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private TestResultEntity createResult(String runId, String testName, TestResultStatus status) {
        TestResultEntity result = new TestResultEntity();
        result.setRunId(runId);
        result.setTestName(testName);
        result.setStatus(status);
        result.setStartTime(Instant.now());
        result.setEndTime(Instant.now());
        result.setDurationMs(0L);
        return result;
    }
}
