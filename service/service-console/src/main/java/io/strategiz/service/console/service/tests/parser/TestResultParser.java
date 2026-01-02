package io.strategiz.service.console.service.tests.parser;

import io.strategiz.data.testing.entity.TestResultEntity;

import java.util.List;

/**
 * Interface for parsing test results from framework output.
 */
public interface TestResultParser {

    /**
     * Parse test results from command output.
     * @param output The stdout/stderr output from test execution
     * @param runId The test run ID these results belong to
     * @return List of parsed test results
     */
    List<TestResultEntity> parseResults(String output, String runId);
}
