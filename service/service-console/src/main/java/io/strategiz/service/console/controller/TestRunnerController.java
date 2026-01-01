package io.strategiz.service.console.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.TestRunRequest;
import io.strategiz.service.console.model.response.TestRunResponse;
import io.strategiz.service.console.service.TestRunnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for running Playwright E2E tests.
 */
@RestController
@RequestMapping("/v1/console/tests")
@Tag(name = "Admin - Test Runner", description = "E2E test execution endpoints for administrators")
public class TestRunnerController extends BaseController {

	private static final String MODULE_NAME = "TEST_RUNNER";

	private final TestRunnerService testRunnerService;

	@Autowired
	public TestRunnerController(TestRunnerService testRunnerService) {
		this.testRunnerService = testRunnerService;
	}

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	@PostMapping("/run")
	@Operation(summary = "Run Playwright E2E tests",
			description = "Executes Playwright tests for the specified suite and returns results")
	public ResponseEntity<TestRunResponse> runTests(
			@Parameter(description = "Test run request with suite ID") @RequestBody TestRunRequest request,
			HttpServletRequest httpRequest) {

		String adminUserId = (String) httpRequest.getAttribute("adminUserId");
		logRequest("runTests", adminUserId, "suiteId=" + request.getSuiteId());

		TestRunResponse response = testRunnerService.runTests(request.getSuiteId());

		log.info("Test suite {} completed for admin {}: {} passed, {} failed", request.getSuiteId(),
				adminUserId, response.getPassed(), response.getFailed());

		return ResponseEntity.ok(response);
	}

}
