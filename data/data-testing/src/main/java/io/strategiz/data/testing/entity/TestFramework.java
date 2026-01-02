package io.strategiz.data.testing.entity;

/**
 * Test frameworks supported by the test runner
 */
public enum TestFramework {
    /**
     * Playwright - Frontend E2E testing
     */
    PLAYWRIGHT,

    /**
     * Jest - Frontend unit testing
     */
    JEST,

    /**
     * JUnit - Java unit testing
     */
    JUNIT,

    /**
     * Cucumber - BDD testing (Java)
     */
    CUCUMBER,

    /**
     * pytest - Python unit and integration testing
     */
    PYTEST
}
