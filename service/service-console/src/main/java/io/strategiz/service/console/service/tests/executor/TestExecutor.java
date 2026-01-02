package io.strategiz.service.console.service.tests.executor;

import io.strategiz.data.testing.entity.TestModuleEntity;
import io.strategiz.data.testing.entity.TestSuiteEntity;
import io.strategiz.data.testing.entity.TestCaseEntity;
import io.strategiz.service.console.service.tests.model.TestRunRequest;

import java.io.IOException;

/**
 * Interface for test execution across different frameworks.
 * Implementations spawn processes and execute tests.
 */
public interface TestExecutor {

    /**
     * Execute all tests in a module.
     * @param module Test module entity
     * @param request Test run request
     * @return Process running the tests
     * @throws IOException If process creation fails
     */
    Process executeModule(TestModuleEntity module, TestRunRequest request) throws IOException;

    /**
     * Execute all tests in a suite.
     * @param suite Test suite entity
     * @param request Test run request
     * @return Process running the tests
     * @throws IOException If process creation fails
     */
    Process executeSuite(TestSuiteEntity suite, TestRunRequest request) throws IOException;

    /**
     * Execute a single test case.
     * @param testCase Test case entity
     * @param request Test run request
     * @return Process running the test
     * @throws IOException If process creation fails
     */
    Process executeTest(TestCaseEntity testCase, TestRunRequest request) throws IOException;
}
