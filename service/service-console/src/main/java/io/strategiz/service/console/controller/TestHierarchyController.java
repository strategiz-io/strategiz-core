package io.strategiz.service.console.controller;

import io.strategiz.business.tokenauth.AuthenticatedUser;
import io.strategiz.business.tokenauth.RequireAuth;
import io.strategiz.service.base.BaseController;
import io.strategiz.service.console.model.response.*;
import io.strategiz.service.console.service.tests.TestHierarchyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for browsing test hierarchy
 * Provides endpoints to navigate apps, modules, test suites, and individual tests
 */
@RestController
@RequestMapping("/v1/console/tests")
@Tag(name = "Test Hierarchy", description = "Browse and navigate test structure")
public class TestHierarchyController extends BaseController {

	private final TestHierarchyService testHierarchyService;

	@Autowired
	public TestHierarchyController(TestHierarchyService testHierarchyService) {
		this.testHierarchyService = testHierarchyService;
	}

	@Override
	protected String getModuleName() {
		return "TEST_HIERARCHY_CONTROLLER";
	}

	/**
	 * Get all test applications
	 *
	 * @param user Authenticated user (admin only)
	 * @return List of test applications
	 */
	@GetMapping("/apps")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get all test applications", description = "Retrieves list of all test applications in the system")
	public ResponseEntity<List<TestAppResponse>> getAllApps(@AuthUser AuthenticatedUser user) {
		List<TestAppResponse> apps = testHierarchyService.getAllApps();
		return ResponseEntity.ok(apps);
	}

	/**
	 * Get detailed information for a specific app
	 *
	 * @param appId App ID
	 * @param user Authenticated user (admin only)
	 * @return Detailed app information with nested modules
	 */
	@GetMapping("/apps/{appId}")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get app details", description = "Retrieves detailed information for a specific test application including nested modules")
	public ResponseEntity<TestAppDetailResponse> getAppDetail(@PathVariable String appId,
			@AuthUser AuthenticatedUser user) {
		TestAppDetailResponse detail = testHierarchyService.getAppDetail(appId);
		return ResponseEntity.ok(detail);
	}

	/**
	 * Get all modules for an app
	 *
	 * @param appId App ID
	 * @param user Authenticated user (admin only)
	 * @return List of modules for the app
	 */
	@GetMapping("/apps/{appId}/modules")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get modules for app", description = "Retrieves all test modules within a specific application")
	public ResponseEntity<List<TestModuleResponse>> getModulesByApp(@PathVariable String appId,
			@AuthUser AuthenticatedUser user) {
		List<TestModuleResponse> modules = testHierarchyService.getModulesByApp(appId);
		return ResponseEntity.ok(modules);
	}

	/**
	 * Get all test suites for a module
	 *
	 * @param appId App ID
	 * @param moduleId Module ID
	 * @param user Authenticated user (admin only)
	 * @return List of test suites for the module
	 */
	@GetMapping("/apps/{appId}/modules/{moduleId}/suites")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get suites for module", description = "Retrieves all test suites within a specific module")
	public ResponseEntity<List<TestSuiteResponse>> getSuitesByModule(@PathVariable String appId,
			@PathVariable String moduleId, @AuthUser AuthenticatedUser user) {
		List<TestSuiteResponse> suites = testHierarchyService.getSuitesByModule(appId, moduleId);
		return ResponseEntity.ok(suites);
	}

	/**
	 * Get all individual tests for a suite
	 *
	 * @param appId App ID
	 * @param moduleId Module ID
	 * @param suiteId Suite ID
	 * @param user Authenticated user (admin only)
	 * @return List of individual test cases in the suite
	 */
	@GetMapping("/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get tests for suite", description = "Retrieves all individual test cases within a specific suite")
	public ResponseEntity<List<TestCaseResponse>> getTestsBySuite(@PathVariable String appId,
			@PathVariable String moduleId, @PathVariable String suiteId, @AuthUser AuthenticatedUser user) {
		List<TestCaseResponse> tests = testHierarchyService.getTestsBySuite(appId, moduleId, suiteId);
		return ResponseEntity.ok(tests);
	}

}
