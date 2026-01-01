package io.strategiz.service.console.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request to run Playwright E2E tests.
 */
@Schema(description = "Request to run Playwright E2E tests")
public class TestRunRequest {

	@Schema(description = "Test suite ID to run (system, smoke, journeys-auth, etc.)", required = true, example = "system")
	private String suiteId;

	public TestRunRequest() {
	}

	public TestRunRequest(String suiteId) {
		this.suiteId = suiteId;
	}

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}

}
