package io.strategiz.data.testing.entity;

/**
 * What triggered the test execution
 */
public enum TestTrigger {

	/**
	 * Manually triggered by user from console UI
	 */
	MANUAL,

	/**
	 * Triggered by CI/CD system (GitHub Actions)
	 */
	CI_CD,

	/**
	 * Triggered by scheduled job
	 */
	SCHEDULED

}
