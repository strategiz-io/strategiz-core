package io.strategiz.data.testing.entity;

/**
 * Level at which a test run was executed
 */
public enum TestExecutionLevel {
    /**
     * Run all tests for an application
     * Example: Run all tests for "Web App"
     */
    APP,

    /**
     * Run all tests for a module
     * Example: Run all tests in "service-auth" module
     */
    MODULE,

    /**
     * Run all tests in a suite
     * Example: Run "Smoke Tests" suite
     */
    SUITE,

    /**
     * Run a single test method
     * Example: Run "test_simple_buy_strategy"
     */
    TEST
}
