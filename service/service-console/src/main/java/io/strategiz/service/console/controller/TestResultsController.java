package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.model.response.TestRunDetailResponse;
import io.strategiz.service.console.model.response.TestRunHistoryResponse;
import io.strategiz.service.console.model.response.TestTrendsResponse;
import io.strategiz.service.console.service.tests.TestResultsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for viewing test results and history
 */
@RestController
@RequestMapping("/v1/console/tests/runs")
@Tag(name = "Test Results", description = "View test execution results and history")
public class TestResultsController extends BaseController {

	private final TestResultsService testResultsService;

	@Autowired
	public TestResultsController(TestResultsService testResultsService) {
		this.testResultsService = testResultsService;
	}

	protected String getModuleName() {
		return "TEST_RESULTS_CONTROLLER";
	}

	/**
	 * Get detailed test run with all results
	 *
	 * @param runId Test run ID
	 * @param user Authenticated user (admin only)
	 * @return Test run details with all test results
	 */
	@GetMapping("/{runId}")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test run details", description = "Retrieves detailed information about a test run including all test results")
	public ResponseEntity<TestRunDetailResponse> getRunDetails(@PathVariable String runId,
			@AuthUser AuthenticatedUser user) {
		TestRunDetailResponse response = testResultsService.getRunDetails(runId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get paginated test run history
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @param trigger Optional trigger filter ("manual", "ci-cd", "scheduled")
	 * @param status Optional status filter ("passed", "failed", "error", "running", "pending")
	 * @param page Page number (0-indexed, default: 0)
	 * @param pageSize Page size (default: 20)
	 * @param user Authenticated user (admin only)
	 * @return Paginated test run history
	 */
	@GetMapping("/history")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test run history", description = "Retrieves paginated test run history with optional filters")
	public ResponseEntity<TestRunHistoryResponse> getRunHistory(
			@Parameter(description = "Filter by app ID") @RequestParam(required = false) String appId,
			@Parameter(description = "Filter by module ID") @RequestParam(required = false) String moduleId,
			@Parameter(description = "Filter by suite ID") @RequestParam(required = false) String suiteId,
			@Parameter(description = "Filter by trigger type") @RequestParam(required = false) String trigger,
			@Parameter(description = "Filter by status") @RequestParam(required = false) String status,
			@Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Page size") @RequestParam(defaultValue = "20") int pageSize,
			@AuthUser AuthenticatedUser user) {

		TestRunHistoryResponse response = testResultsService.getRunHistory(appId, moduleId, suiteId, trigger, status,
				page, pageSize);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get latest CI/CD test run
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @param user Authenticated user (admin only)
	 * @return Latest CI/CD test run, or 404 if none found
	 */
	@GetMapping("/latest-ci")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get latest CI/CD run", description = "Retrieves the most recent CI/CD test run with optional filters")
	public ResponseEntity<TestRunDetailResponse> getLatestCiRun(
			@Parameter(description = "Filter by app ID") @RequestParam(required = false) String appId,
			@Parameter(description = "Filter by module ID") @RequestParam(required = false) String moduleId,
			@Parameter(description = "Filter by suite ID") @RequestParam(required = false) String suiteId,
			@AuthUser AuthenticatedUser user) {

		TestRunDetailResponse response = testResultsService.getLatestCiRun(appId, moduleId, suiteId);

		if (response == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Get test trends over time
	 *
	 * @param appId Optional app ID filter
	 * @param moduleId Optional module ID filter
	 * @param suiteId Optional suite ID filter
	 * @param timeRange Time range ("7d", "30d", "90d", default: "7d")
	 * @param user Authenticated user (admin only)
	 * @return Test trends response
	 */
	@GetMapping("/trends")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test trends", description = "Retrieves test execution trends over time with pass rate and performance metrics")
	public ResponseEntity<TestTrendsResponse> getTrends(
			@Parameter(description = "Filter by app ID") @RequestParam(required = false) String appId,
			@Parameter(description = "Filter by module ID") @RequestParam(required = false) String moduleId,
			@Parameter(description = "Filter by suite ID") @RequestParam(required = false) String suiteId,
			@Parameter(description = "Time range (7d, 30d, 90d)") @RequestParam(defaultValue = "7d") String timeRange,
			@AuthUser AuthenticatedUser user) {

		TestTrendsResponse response = testResultsService.getTrends(appId, moduleId, suiteId, timeRange);
		return ResponseEntity.ok(response);
	}

}
