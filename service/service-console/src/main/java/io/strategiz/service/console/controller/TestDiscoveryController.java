package io.strategiz.service.console.controller;

import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.data.testing.entity.TestAppEntity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.console.service.tests.TestDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for test discovery
 * Scans codebase for tests and populates Firestore with test hierarchy
 */
@RestController
@RequestMapping("/v1/console/tests/discovery")
@Tag(name = "Test Discovery", description = "Discover tests in the codebase")
public class TestDiscoveryController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(TestDiscoveryController.class);

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
	 * Discover all tests across all frameworks (Playwright, Maven, Pytest)
	 * @param user Authenticated user
	 * @return Discovery response with counts
	 */
	@PostMapping("/refresh")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Discover all tests", description = "Scans codebase for Playwright, Maven (JUnit/Cucumber), and Pytest tests")
	public ResponseEntity<DiscoveryResponse> refreshAll(@AuthUser AuthenticatedUser user) {
		log.info("Starting full test discovery for user: {}", user.getUserId());

		try {
			List<TestAppEntity> apps = testDiscoveryService.discoverAllTests();

			// Calculate totals
			int totalApps = apps.size();
			int totalModules = apps.stream().mapToInt(TestAppEntity::getTotalModules).sum();
			int totalTests = apps.stream().mapToInt(TestAppEntity::getTotalTests).sum();

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(true);
			response.setMessage("Test discovery completed successfully");
			response.setAppsDiscovered(totalApps);
			response.setModulesDiscovered(totalModules);
			response.setTestsDiscovered(totalTests);

			log.info("Test discovery completed: {} apps, {} modules, {} tests", totalApps, totalModules, totalTests);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Test discovery failed", e);

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(false);
			response.setMessage("Test discovery failed: " + e.getMessage());
			response.setAppsDiscovered(0);
			response.setModulesDiscovered(0);
			response.setTestsDiscovered(0);

			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Discover only Playwright tests (frontend)
	 * @param user Authenticated user
	 * @return Discovery response with counts
	 */
	@PostMapping("/refresh/playwright")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Discover Playwright tests", description = "Scans frontend apps for Playwright E2E tests")
	public ResponseEntity<DiscoveryResponse> refreshPlaywright(@AuthUser AuthenticatedUser user) {
		log.info("Starting Playwright test discovery for user: {}", user.getUserId());

		try {
			String uiDirectory = "../strategiz-ui";
			List<TestAppEntity> apps = testDiscoveryService.discoverPlaywrightTests(uiDirectory);

			int totalApps = apps.size();
			int totalModules = apps.stream().mapToInt(TestAppEntity::getTotalModules).sum();
			int totalTests = apps.stream().mapToInt(TestAppEntity::getTotalTests).sum();

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(true);
			response.setMessage("Playwright test discovery completed successfully");
			response.setAppsDiscovered(totalApps);
			response.setModulesDiscovered(totalModules);
			response.setTestsDiscovered(totalTests);

			log.info("Playwright discovery completed: {} apps, {} modules, {} tests", totalApps, totalModules,
					totalTests);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Playwright test discovery failed", e);

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(false);
			response.setMessage("Playwright test discovery failed: " + e.getMessage());
			response.setAppsDiscovered(0);
			response.setModulesDiscovered(0);
			response.setTestsDiscovered(0);

			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Discover only Maven tests (backend)
	 * @param user Authenticated user
	 * @return Discovery response with counts
	 */
	@PostMapping("/refresh/maven")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Discover Maven tests", description = "Scans backend modules for JUnit and Cucumber tests")
	public ResponseEntity<DiscoveryResponse> refreshMaven(@AuthUser AuthenticatedUser user) {
		log.info("Starting Maven test discovery for user: {}", user.getUserId());

		try {
			String backendDirectory = ".";
			List<TestAppEntity> apps = testDiscoveryService.discoverMavenTests(backendDirectory);

			int totalApps = apps.size();
			int totalModules = apps.stream().mapToInt(TestAppEntity::getTotalModules).sum();
			int totalTests = apps.stream().mapToInt(TestAppEntity::getTotalTests).sum();

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(true);
			response.setMessage("Maven test discovery completed successfully");
			response.setAppsDiscovered(totalApps);
			response.setModulesDiscovered(totalModules);
			response.setTestsDiscovered(totalTests);

			log.info("Maven discovery completed: {} apps, {} modules, {} tests", totalApps, totalModules, totalTests);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Maven test discovery failed", e);

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(false);
			response.setMessage("Maven test discovery failed: " + e.getMessage());
			response.setAppsDiscovered(0);
			response.setModulesDiscovered(0);
			response.setTestsDiscovered(0);

			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * Discover only Pytest tests (Python execution service)
	 * @param user Authenticated user
	 * @return Discovery response with counts
	 */
	@PostMapping("/refresh/pytest")
	@RequireAuth(minAcr = "1")
	@Operation(summary = "Discover Pytest tests", description = "Scans Python service for Pytest tests")
	public ResponseEntity<DiscoveryResponse> refreshPytest(@AuthUser AuthenticatedUser user) {
		log.info("Starting Pytest test discovery for user: {}", user.getUserId());

		try {
			String pythonDirectory = "./application-strategy-execution";
			List<TestAppEntity> apps = testDiscoveryService.discoverPytestTests(pythonDirectory);

			int totalApps = apps.size();
			int totalModules = apps.stream().mapToInt(TestAppEntity::getTotalModules).sum();
			int totalTests = apps.stream().mapToInt(TestAppEntity::getTotalTests).sum();

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(true);
			response.setMessage("Pytest test discovery completed successfully");
			response.setAppsDiscovered(totalApps);
			response.setModulesDiscovered(totalModules);
			response.setTestsDiscovered(totalTests);

			log.info("Pytest discovery completed: {} apps, {} modules, {} tests", totalApps, totalModules, totalTests);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			log.error("Pytest test discovery failed", e);

			DiscoveryResponse response = new DiscoveryResponse();
			response.setSuccess(false);
			response.setMessage("Pytest test discovery failed: " + e.getMessage());
			response.setAppsDiscovered(0);
			response.setModulesDiscovered(0);
			response.setTestsDiscovered(0);

			return ResponseEntity.status(500).body(response);
		}
	}

	/**
	 * DTO for discovery response
	 */
	public static class DiscoveryResponse {

		private boolean success;

		private String message;

		private int appsDiscovered;

		private int modulesDiscovered;

		private int testsDiscovered;

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getAppsDiscovered() {
			return appsDiscovered;
		}

		public void setAppsDiscovered(int appsDiscovered) {
			this.appsDiscovered = appsDiscovered;
		}

		public int getModulesDiscovered() {
			return modulesDiscovered;
		}

		public void setModulesDiscovered(int modulesDiscovered) {
			this.modulesDiscovered = modulesDiscovered;
		}

		public int getTestsDiscovered() {
			return testsDiscovered;
		}

		public void setTestsDiscovered(int testsDiscovered) {
			this.testsDiscovered = testsDiscovered;
		}

	}

}
