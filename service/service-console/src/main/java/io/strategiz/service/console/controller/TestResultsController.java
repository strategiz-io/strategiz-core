package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.service.tests.TestResultsService;
import io.strategiz.service.console.model.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for viewing test results and history
 * Provides endpoints for test run details, history, and trends
 */
@RestController
@RequestMapping("/v1/console/tests/runs")
@Tag(name = "Test Results", description = "View test execution results and history")
public class TestResultsController extends BaseController {

	private final TestResultsService resultsService;

	@Autowired
	public TestResultsController(TestResultsService resultsService) {
		this.resultsService = resultsService;
	}

	@Override
	protected String getModuleName() {
		return "TEST_RESULTS_CONTROLLER";
	}

	/**
	 * Get details for a specific test run
	 */
	@GetMapping("/{runId}")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test run details", description = "Fetch detailed results for a specific test run")
	public ResponseEntity<TestRunDetailResponse> getRunDetails(
			@PathVariable String runId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching run details for runId: {}, user: {}", runId, user.getUserId());
		TestRunDetailResponse details = resultsService.getRunDetails(runId);
		return ResponseEntity.ok(details);
	}

	/**
	 * Get test run history with filtering and pagination
	 */
	@GetMapping("/history")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test run history", description = "Fetch paginated test execution history with optional filters")
	public ResponseEntity<TestRunHistoryResponse> getRunHistory(
			@RequestParam(required = false) String appId,
			@RequestParam(required = false) String moduleId,
			@RequestParam(required = false) String suiteId,
			@RequestParam(required = false) String trigger,
			@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int pageSize,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching test run history for user: {}, filters: appId={}, moduleId={}, suiteId={}, trigger={}, status={}, page={}, pageSize={}",
				user.getUserId(), appId, moduleId, suiteId, trigger, status, page, pageSize);

		TestRunHistoryResponse history = resultsService.getRunHistory(
				appId, moduleId, suiteId, trigger, status, page, pageSize);

		return ResponseEntity.ok(history);
	}

	/**
	 * Get latest CI/CD test run
	 */
	@GetMapping("/latest-ci")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get latest CI/CD run", description = "Fetch the most recent CI/CD test run")
	public ResponseEntity<TestRunDetailResponse> getLatestCiRun(
			@RequestParam(required = false) String appId,
			@RequestParam(required = false) String moduleId,
			@RequestParam(required = false) String suiteId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching latest CI run for user: {}, filters: appId={}, moduleId={}, suiteId={}",
				user.getUserId(), appId, moduleId, suiteId);

		TestRunDetailResponse latestRun = resultsService.getLatestCiRun(appId, moduleId, suiteId);

		return ResponseEntity.ok(latestRun);
	}

	/**
	 * Get test trends over time
	 */
	@GetMapping("/trends")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test trends", description = "Fetch test pass rate and performance trends over time")
	public ResponseEntity<TestTrendsResponse> getTrends(
			@RequestParam(required = false) String appId,
			@RequestParam(required = false) String moduleId,
			@RequestParam(required = false) String suiteId,
			@RequestParam(defaultValue = "7d") String timeRange,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching test trends for user: {}, filters: appId={}, moduleId={}, suiteId={}, timeRange={}",
				user.getUserId(), appId, moduleId, suiteId, timeRange);

		TestTrendsResponse trends = resultsService.getTrends(appId, moduleId, suiteId, timeRange);

		return ResponseEntity.ok(trends);
	}

}
