package io.strategiz.service.console.controller;

import io.strategiz.data.testing.entity.TestRunEntity;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.service.tests.TestDiscoveryService;
import io.strategiz.service.console.service.tests.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for test hierarchy browsing and execution. Provides endpoints for
 * frontend journeys/pages and backend modules/APIs as per the Test Runner UI design.
 */
@RestController
@RequestMapping("/v1/console/tests")
@Tag(name = "Test Hierarchy", description = "Browse and execute tests by hierarchy")
public class TestHierarchyController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(TestHierarchyController.class);

	private final TestDiscoveryService testDiscoveryService;

	private final TestExecutionService testExecutionService;

	@Autowired
	public TestHierarchyController(TestDiscoveryService testDiscoveryService,
			TestExecutionService testExecutionService) {
		this.testDiscoveryService = testDiscoveryService;
		this.testExecutionService = testExecutionService;
	}

	@Override
	protected String getModuleName() {
		return "TEST_HIERARCHY_CONTROLLER";
	}

	// ===============================
	// Frontend Hierarchy Endpoints
	// ===============================

	/**
	 * Get all frontend journeys (auth, labs, portfolio, settings)
	 */
	@GetMapping("/frontend/journeys")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get frontend journeys", description = "Returns list of user journey test groups")
	public ResponseEntity<List<Map<String, Object>>> getFrontendJourneys(@AuthUser AuthenticatedUser user) {
		log.info("Getting frontend journeys for user: {}", user.getUserId());
		List<Map<String, Object>> journeys = testDiscoveryService.getFrontendJourneys();
		return ResponseEntity.ok(journeys);
	}

	/**
	 * Get all frontend pages (/dashboard, /labs, /portfolio, etc.)
	 */
	@GetMapping("/frontend/pages")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get frontend pages", description = "Returns list of page-based test groups")
	public ResponseEntity<List<Map<String, Object>>> getFrontendPages(@AuthUser AuthenticatedUser user) {
		log.info("Getting frontend pages for user: {}", user.getUserId());
		List<Map<String, Object>> pages = testDiscoveryService.getFrontendPages();
		return ResponseEntity.ok(pages);
	}

	/**
	 * Run tests for a specific journey
	 */
	@PostMapping("/frontend/journeys/{journey}/run")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run journey tests", description = "Execute all tests for a user journey")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runFrontendJourney(@PathVariable String journey,
			@AuthUser AuthenticatedUser user) {
		log.info("Running frontend journey tests: {} for user: {}", journey, user.getUserId());
		return testExecutionService.runFrontendJourney(journey, user.getUserId()).thenApply(ResponseEntity::ok);
	}

	/**
	 * Run tests for a specific page
	 */
	@PostMapping("/frontend/pages/{page}/run")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run page tests", description = "Execute all tests for a specific page")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runFrontendPage(@PathVariable String page,
			@AuthUser AuthenticatedUser user) {
		log.info("Running frontend page tests: {} for user: {}", page, user.getUserId());
		return testExecutionService.runFrontendPage(page, user.getUserId()).thenApply(ResponseEntity::ok);
	}

	/**
	 * Run all frontend tests
	 */
	@PostMapping("/frontend/run-all")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run all frontend tests", description = "Execute all Playwright E2E tests")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runAllFrontend(@AuthUser AuthenticatedUser user) {
		log.info("Running all frontend tests for user: {}", user.getUserId());
		return testExecutionService.runAllFrontend(user.getUserId()).thenApply(ResponseEntity::ok);
	}

	// ===============================
	// Backend Hierarchy Endpoints
	// ===============================

	/**
	 * Get all backend modules (service-auth, service-labs, etc.)
	 */
	@GetMapping("/backend/modules")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get backend modules", description = "Returns list of backend service modules")
	public ResponseEntity<List<Map<String, Object>>> getBackendModules(@AuthUser AuthenticatedUser user) {
		log.info("Getting backend modules for user: {}", user.getUserId());
		List<Map<String, Object>> modules = testDiscoveryService.getBackendModules();
		return ResponseEntity.ok(modules);
	}

	/**
	 * Get all backend APIs (/v1/auth/*, /v1/labs/*, etc.)
	 */
	@GetMapping("/backend/apis")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get backend APIs", description = "Returns list of API endpoint test groups")
	public ResponseEntity<List<Map<String, Object>>> getBackendApis(@AuthUser AuthenticatedUser user) {
		log.info("Getting backend APIs for user: {}", user.getUserId());
		List<Map<String, Object>> apis = testDiscoveryService.getBackendApis();
		return ResponseEntity.ok(apis);
	}

	/**
	 * Run tests for a specific backend module
	 */
	@PostMapping("/backend/modules/{module}/run")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run module tests", description = "Execute all tests for a backend module")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runBackendModule(@PathVariable String module,
			@AuthUser AuthenticatedUser user) {
		log.info("Running backend module tests: {} for user: {}", module, user.getUserId());
		return testExecutionService.runBackendModule(module, user.getUserId()).thenApply(ResponseEntity::ok);
	}

	/**
	 * Run tests for a specific API path
	 */
	@PostMapping("/backend/apis/{api}/run")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run API tests", description = "Execute all tests for an API path")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runBackendApi(@PathVariable String api,
			@AuthUser AuthenticatedUser user) {
		log.info("Running backend API tests: {} for user: {}", api, user.getUserId());
		// Convert api from v1-auth to /v1/auth
		String apiPath = "/" + api.replace("-", "/");
		return testExecutionService.runBackendApi(apiPath, user.getUserId()).thenApply(ResponseEntity::ok);
	}

	/**
	 * Run all backend tests
	 */
	@PostMapping("/backend/run-all")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Run all backend tests", description = "Execute all Maven/JUnit tests")
	public CompletableFuture<ResponseEntity<TestRunEntity>> runAllBackend(@AuthUser AuthenticatedUser user) {
		log.info("Running all backend tests for user: {}", user.getUserId());
		return testExecutionService.runAllBackend(user.getUserId()).thenApply(ResponseEntity::ok);
	}

	// ===============================
	// Test Run Results Endpoints
	// ===============================

	/**
	 * Get test run status and results
	 */
	@GetMapping("/runs/{runId}")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test run", description = "Get status and results of a test run")
	public ResponseEntity<TestRunEntity> getTestRun(@PathVariable String runId, @AuthUser AuthenticatedUser user) {
		log.info("Getting test run: {} for user: {}", runId, user.getUserId());
		TestRunEntity testRun = testExecutionService.getTestRun(runId);
		if (testRun == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(testRun);
	}

	/**
	 * Re-run failed tests from a previous run
	 */
	@PostMapping("/runs/{runId}/rerun-failed")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Re-run failed tests", description = "Execute only the failed tests from a previous run")
	public CompletableFuture<ResponseEntity<TestRunEntity>> rerunFailedTests(@PathVariable String runId,
			@AuthUser AuthenticatedUser user) {
		log.info("Re-running failed tests from run: {} for user: {}", runId, user.getUserId());
		return testExecutionService.rerunFailedTests(runId, user.getUserId()).thenApply(run -> {
			if (run == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(run);
		});
	}

	/**
	 * Re-run specific tests by their IDs
	 */
	@PostMapping("/runs/rerun")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Re-run specific tests", description = "Execute specific tests by their IDs")
	public CompletableFuture<ResponseEntity<TestRunEntity>> rerunSpecificTests(@RequestBody List<String> testIds,
			@AuthUser AuthenticatedUser user) {
		log.info("Re-running {} specific tests for user: {}", testIds.size(), user.getUserId());
		return testExecutionService.rerunSpecificTests(testIds, user.getUserId()).thenApply(ResponseEntity::ok);
	}

}
