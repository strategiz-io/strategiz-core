package io.strategiz.data.testing.entity;

/**
 * Status of a test execution run
 */
public enum TestRunStatus {

	/**
	 * Test run has been queued but not started
	 */
	PENDING,

	/**
	 * Test run is currently executing
	 */
	RUNNING,

	/**
	 * All tests passed successfully
	 */
	PASSED,

	/**
	 * One or more tests failed (assertions failed)
	 */
	FAILED,

	/**
	 * Test run encountered an error (exception, timeout, infrastructure failure)
	 */
	ERROR,

	/**
	 * Test run was cancelled by user or system
	 */
	CANCELLED

}
