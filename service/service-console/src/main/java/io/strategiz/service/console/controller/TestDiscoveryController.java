package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.service.tests.TestDiscoveryService;
import io.strategiz.data.testing.entity.TestAppEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for test discovery and hierarchy initialization
 * Admin-only endpoints to scan codebase and populate test database
 */
@RestController
@RequestMapping("/v1/console/tests/discovery")
@Tag(name = "Test Discovery", description = "Scan codebase and populate test hierarchy")
public class TestDiscoveryController extends BaseController {

	private final TestDiscoveryService testDiscoveryService;

	@Autowired
	public TestDiscoveryController(TestDiscoveryService testDiscoveryService) {
		this.testDiscoveryService = testDiscoveryService;
	}

	@Override
	protected String getModuleName() {
		return "TEST_DISCOVERY_CONTROLLER";
	}

	/**
	 * Trigger full test discovery for all applications
	 * Scans Playwright, Maven, and Pytest tests and populates Firestore
	 * @param user Authenticated admin user
	 * @return Discovery summary
	 */
	@PostMapping("/refresh")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Refresh test hierarchy", description = "Scans codebase to discover all tests and updates Firestore database. Admin only.")
	public ResponseEntity<Map<String, Object>> refreshTestHierarchy(@AuthUser AuthenticatedUser user) {
		log.info("Test discovery triggered by user: {}", user.getUserId());

		List<TestAppEntity> discoveredApps = testDiscoveryService.discoverAllTests();

		// Build summary response
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Test discovery completed successfully");
		response.put("totalApplications", discoveredApps.size());
		response.put("applications", discoveredApps.stream().map(app -> {
			Map<String, Object> appSummary = new HashMap<>();
			appSummary.put("id", app.getId());
			appSummary.put("name", app.getDisplayName());
			appSummary.put("type", app.getType().name());
			appSummary.put("totalModules", app.getTotalModules());
			appSummary.put("totalTests", app.getTotalTests());
			return appSummary;
		}).toList());

		log.info("Test discovery completed. Found {} applications with {} total tests", discoveredApps.size(),
				discoveredApps.stream().mapToInt(TestAppEntity::getTotalTests).sum());

		return ResponseEntity.ok(response);
	}

	/**
	 * Discover Playwright tests only
	 * @param user Authenticated admin user
	 * @return Discovery summary
	 */
	@PostMapping("/refresh/playwright")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Refresh Playwright tests", description = "Scans strategiz-ui directory for Playwright tests. Admin only.")
	public ResponseEntity<Map<String, Object>> refreshPlaywrightTests(@AuthUser AuthenticatedUser user) {
		log.info("Playwright test discovery triggered by user: {}", user.getUserId());

		List<TestAppEntity> discoveredApps = testDiscoveryService.discoverPlaywrightTests("../strategiz-ui");

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Playwright test discovery completed");
		response.put("totalApplications", discoveredApps.size());
		response.put("applications", discoveredApps);

		return ResponseEntity.ok(response);
	}

	/**
	 * Discover Maven tests only
	 * @param user Authenticated admin user
	 * @return Discovery summary
	 */
	@PostMapping("/refresh/maven")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Refresh Maven tests", description = "Scans strategiz-core directory for Maven/JUnit tests. Admin only.")
	public ResponseEntity<Map<String, Object>> refreshMavenTests(@AuthUser AuthenticatedUser user) {
		log.info("Maven test discovery triggered by user: {}", user.getUserId());

		List<TestAppEntity> discoveredApps = testDiscoveryService.discoverMavenTests(".");

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Maven test discovery completed");
		response.put("totalApplications", discoveredApps.size());
		response.put("applications", discoveredApps);

		return ResponseEntity.ok(response);
	}

	/**
	 * Discover Pytest tests only
	 * @param user Authenticated admin user
	 * @return Discovery summary
	 */
	@PostMapping("/refresh/pytest")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Refresh Pytest tests", description = "Scans Python service directory for Pytest tests. Admin only.")
	public ResponseEntity<Map<String, Object>> refreshPytestTests(@AuthUser AuthenticatedUser user) {
		log.info("Pytest test discovery triggered by user: {}", user.getUserId());

		List<TestAppEntity> discoveredApps = testDiscoveryService
			.discoverPytestTests("./application-strategy-execution");

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("message", "Pytest test discovery completed");
		response.put("totalApplications", discoveredApps.size());
		response.put("applications", discoveredApps);

		return ResponseEntity.ok(response);
	}

}
