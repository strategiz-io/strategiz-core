package io.strategiz.data.testing.entity;

/**
 * Status of an individual test result
 */
public enum TestResultStatus {

	/**
	 * Test passed successfully
	 */
	PASSED,

	/**
	 * Test failed (assertion failure)
	 */
	FAILED,

	/**
	 * Test was skipped (e.g., conditional skip, disabled)
	 */
	SKIPPED,

	/**
	 * Test encountered an error (exception, timeout)
	 */
	ERROR

}
