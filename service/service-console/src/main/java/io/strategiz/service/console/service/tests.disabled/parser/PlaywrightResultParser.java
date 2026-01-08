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
 * Parser for Playwright and Jest test results.
 */
@Component
public class PlaywrightResultParser implements TestResultParser {

    private static final Pattern PASSED_PATTERN = Pattern.compile("(\\d+)\\s+passed");
    private static final Pattern FAILED_PATTERN = Pattern.compile("(\\d+)\\s+failed");
    private static final Pattern SKIPPED_PATTERN = Pattern.compile("(\\d+)\\s+skipped");

    @Override
    public List<TestResultEntity> parseResults(String output, String runId) {
        List<TestResultEntity> results = new ArrayList<>();

        // Parse summary from output
        int passed = extractCount(output, PASSED_PATTERN);
        int failed = extractCount(output, FAILED_PATTERN);
        int skipped = extractCount(output, SKIPPED_PATTERN);

        // Create aggregate result entities
        // In a full implementation, we would parse individual test names
        // For now, create summary entries
        for (int i = 0; i < passed; i++) {
            results.add(createResult(runId, "test_" + i, TestResultStatus.PASSED));
        }
        for (int i = 0; i < failed; i++) {
            results.add(createResult(runId, "test_failed_" + i, TestResultStatus.FAILED));
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
