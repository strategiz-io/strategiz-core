package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.service.tests.TestHierarchyService;
import io.strategiz.service.console.model.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for browsing test hierarchy
 * Provides endpoints to navigate Apps → Modules → Suites → Tests
 */
@RestController
@RequestMapping("/v1/console/tests")
@Tag(name = "Test Hierarchy", description = "Browse test structure")
public class TestHierarchyController extends BaseController {

	private final TestHierarchyService hierarchyService;

	@Autowired
	public TestHierarchyController(TestHierarchyService hierarchyService) {
		this.hierarchyService = hierarchyService;
	}

	@Override
	protected String getModuleName() {
		return "TEST_HIERARCHY_CONTROLLER";
	}

	/**
	 * Get all test applications
	 */
	@GetMapping("/apps")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "List all test applications")
	public ResponseEntity<List<TestAppResponse>> getApps(@AuthUser AuthenticatedUser user) {
		log.debug("Fetching all test applications for user: {}", user.getUserId());
		List<TestAppResponse> apps = hierarchyService.getAllApps();
		return ResponseEntity.ok(apps);
	}

	/**
	 * Get specific app details
	 */
	@GetMapping("/apps/{appId}")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Get test application details")
	public ResponseEntity<TestAppDetailResponse> getApp(
			@PathVariable String appId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching app details for appId: {}, user: {}", appId, user.getUserId());
		TestAppDetailResponse app = hierarchyService.getAppDetail(appId);
		return ResponseEntity.ok(app);
	}

	/**
	 * Get modules for an app
	 */
	@GetMapping("/apps/{appId}/modules")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "List modules for a test application")
	public ResponseEntity<List<TestModuleResponse>> getModules(
			@PathVariable String appId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching modules for appId: {}, user: {}", appId, user.getUserId());
		List<TestModuleResponse> modules = hierarchyService.getModulesByApp(appId);
		return ResponseEntity.ok(modules);
	}

	/**
	 * Get suites for a module
	 */
	@GetMapping("/apps/{appId}/modules/{moduleId}/suites")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "List test suites for a module")
	public ResponseEntity<List<TestSuiteResponse>> getSuites(
			@PathVariable String appId,
			@PathVariable String moduleId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching suites for appId: {}, moduleId: {}, user: {}",
				appId, moduleId, user.getUserId());
		List<TestSuiteResponse> suites = hierarchyService.getSuitesByModule(appId, moduleId);
		return ResponseEntity.ok(suites);
	}

	/**
	 * Get tests for a suite
	 */
	@GetMapping("/apps/{appId}/modules/{moduleId}/suites/{suiteId}/tests")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "List individual tests for a suite")
	public ResponseEntity<List<TestCaseResponse>> getTests(
			@PathVariable String appId,
			@PathVariable String moduleId,
			@PathVariable String suiteId,
			@AuthUser AuthenticatedUser user) {
		log.debug("Fetching tests for appId: {}, moduleId: {}, suiteId: {}, user: {}",
				appId, moduleId, suiteId, user.getUserId());
		List<TestCaseResponse> tests = hierarchyService.getTestsBySuite(appId, moduleId, suiteId);
		return ResponseEntity.ok(tests);
	}

}
