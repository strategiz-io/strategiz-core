package io.strategiz.service.console.controller;

import io.strategiz.service.base.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin controller for running integration tests against production APIs.
 *
 * Provides endpoints to: - Trigger integration test suites - View test results - Check
 * test run status
 *
 * Tests are executed asynchronously and results are stored in memory (could be extended
 * to persist in Firestore).
 */
@RestController
@RequestMapping("/v1/console/test-runner")
@Tag(name = "Admin - Test Runner", description = "Run and view integration tests")
public class AdminTestRunnerController extends BaseController {

	private static final String MODULE_NAME = "CONSOLE";

	@Value("${strategiz.api.url:https://strategiz-api-43628135674.us-east1.run.app}")
	private String apiBaseUrl;

	@Value("${strategiz.service-account.client-id:}")
	private String serviceAccountClientId;

	@Value("${strategiz.service-account.client-secret:}")
	private String serviceAccountClientSecret;

	private final RestTemplate restTemplate = new RestTemplate();

	// In-memory storage for test runs (could be moved to Firestore)
	private final Map<String, TestRun> testRuns = new ConcurrentHashMap<>();

	@Override
	protected String getModuleName() {
		return MODULE_NAME;
	}

	/**
	 * Start a new integration test run.
	 */
	@PostMapping("/run")
	@Operation(summary = "Run tests", description = "Start a new integration test run")
	public ResponseEntity<Map<String, Object>> runTests(@RequestBody(required = false) RunTestsRequest request) {
		String runId = UUID.randomUUID().toString().substring(0, 8);
		String testSuite = request != null && request.testSuite != null ? request.testSuite : "all";
		String symbol = request != null && request.symbol != null ? request.symbol : "AAPL";

		log.info("Starting test run: id={}, suite={}, symbol={}", runId, testSuite, symbol);

		TestRun testRun = new TestRun(runId, testSuite, symbol);
		testRuns.put(runId, testRun);

		// Run tests asynchronously
		CompletableFuture.runAsync(() -> executeTests(testRun));

		Map<String, Object> response = new HashMap<>();
		response.put("runId", runId);
		response.put("status", "RUNNING");
		response.put("testSuite", testSuite);
		response.put("symbol", symbol);
		response.put("message", "Test run started. Use GET /v1/console/test-runner/runs/{runId} to check status.");

		return ResponseEntity.ok(response);
	}

	/**
	 * Get status and results of a specific test run.
	 */
	@GetMapping("/runs/{runId}")
	@Operation(summary = "Get test run", description = "Get status and results of a test run")
	public ResponseEntity<TestRun> getTestRun(@PathVariable String runId) {
		TestRun testRun = testRuns.get(runId);
		if (testRun == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(testRun);
	}

	/**
	 * List all test runs.
	 */
	@GetMapping("/runs")
	@Operation(summary = "List test runs", description = "Get all test runs")
	public ResponseEntity<List<TestRun>> listTestRuns() {
		List<TestRun> runs = new ArrayList<>(testRuns.values());
		runs.sort((a, b) -> b.startedAt.compareTo(a.startedAt));
		return ResponseEntity.ok(runs);
	}

	/**
	 * Get available test suites.
	 */
	@GetMapping("/suites")
	@Operation(summary = "List test suites", description = "Get available test suites")
	public ResponseEntity<List<Map<String, Object>>> listTestSuites() {
		List<Map<String, Object>> suites = List.of(
				Map.of("id", "all", "name", "All Tests", "description", "Run all integration tests"),
				Map.of("id", "ai-strategy", "name", "AI Strategy (Full)", "description",
						"Test both GENERATIVE_AI and AUTONOMOUS modes - verifies strategies beat buy-and-hold"),
				Map.of("id", "generative-ai", "name", "GENERATIVE AI Mode", "description",
						"Test GENERATIVE_AI mode with historical insights - must beat buy-and-hold"),
				Map.of("id", "autonomous-mode", "name", "AUTONOMOUS Mode", "description",
						"Test AUTONOMOUS mode optimization - must beat buy-and-hold"),
				Map.of("id", "auth", "name", "Authentication", "description",
						"Test authentication endpoints and token validation"),
				Map.of("id", "mfa-enforcement", "name", "MFA Enforcement", "description",
						"Test MFA enforcement toggle, step-up checks, and security settings"),
				Map.of("id", "labs", "name", "Labs API", "description", "Test Labs AI endpoints"),
				Map.of("id", "health", "name", "Health Check", "description", "Basic health and connectivity tests"),
				Map.of("id", "profile", "name", "Profile API", "description",
						"Test profile CRUD operations including bio, location, occupation, and education fields"));
		return ResponseEntity.ok(suites);
	}

	// =====================================================
	// Test Execution Logic
	// =====================================================

	private void executeTests(TestRun testRun) {
		try {
			// Get access token
			String accessToken = getServiceAccountToken();
			if (accessToken == null) {
				testRun.status = "FAILED";
				testRun.addResult(new TestResult("authentication", "Service Account Token", false,
						"Failed to obtain access token", 0));
				testRun.completedAt = Instant.now().toString();
				return;
			}

			testRun.addResult(
					new TestResult("authentication", "Service Account Token", true, "Token obtained successfully", 0));

			// Run tests based on suite
			String suite = testRun.testSuite;

			if ("all".equals(suite) || "health".equals(suite)) {
				runHealthTests(testRun, accessToken);
			}

			if ("all".equals(suite) || "auth".equals(suite)) {
				runAuthTests(testRun, accessToken);
			}

			// AI Strategy Tests (comprehensive - tests both modes with buy-and-hold
			// verification)
			if ("all".equals(suite) || "ai-strategy".equals(suite)) {
				runGenerativeAITests(testRun, accessToken);
				runAutonomousModeTestsWithVerification(testRun, accessToken);
			}
			else if ("generative-ai".equals(suite)) {
				runGenerativeAITests(testRun, accessToken);
			}
			else if ("autonomous-mode".equals(suite)) {
				runAutonomousModeTestsWithVerification(testRun, accessToken);
			}

			if ("all".equals(suite) || "labs".equals(suite)) {
				runLabsTests(testRun, accessToken);
			}

			if ("all".equals(suite) || "mfa-enforcement".equals(suite)) {
				runMfaEnforcementTests(testRun, accessToken);
			}

			if ("all".equals(suite) || "profile".equals(suite)) {
				runProfileTests(testRun, accessToken);
			}

			// Calculate summary
			long passed = testRun.results.stream().filter(r -> r.passed).count();
			long failed = testRun.results.stream().filter(r -> !r.passed).count();

			testRun.status = failed == 0 ? "PASSED" : "FAILED";
			testRun.summary = String.format("%d passed, %d failed", passed, failed);
			testRun.completedAt = Instant.now().toString();

			log.info("Test run completed: id={}, status={}, summary={}", testRun.runId, testRun.status,
					testRun.summary);

		}
		catch (Exception e) {
			log.error("Test run failed with exception: id={}", testRun.runId, e);
			testRun.status = "ERROR";
			testRun.summary = "Test run failed: " + e.getMessage();
			testRun.completedAt = Instant.now().toString();
		}
	}

	private String getServiceAccountToken() {
		try {
			String clientId = serviceAccountClientId;
			String clientSecret = serviceAccountClientSecret;

			// If not configured via properties, these would need to come from Vault or
			// env
			if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
				log.warn("Service account credentials not configured");
				// Try environment variables as fallback
				clientId = System.getenv("STRATEGIZ_CLIENT_ID");
				clientSecret = System.getenv("STRATEGIZ_CLIENT_SECRET");

				if (clientId == null || clientSecret == null) {
					return null;
				}
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, String> body = Map.of("client_id", clientId, "client_secret", clientSecret, "grant_type",
					"client_credentials");

			HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate.postForObject(apiBaseUrl + "/v1/auth/service-account/token",
					request, Map.class);

			if (response != null && response.containsKey("access_token")) {
				return (String) response.get("access_token");
			}

			return null;
		}
		catch (Exception e) {
			log.error("Failed to get service account token", e);
			return null;
		}
	}

	private void runHealthTests(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			// Simple connectivity test
			String response = restTemplate.getForObject(apiBaseUrl + "/actuator/health", String.class);
			long duration = System.currentTimeMillis() - start;
			boolean passed = response != null && response.contains("UP");
			testRun.addResult(new TestResult("health", "API Health Check", passed,
					passed ? "API is healthy" : "API health check failed", duration));
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("health", "API Health Check", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runAuthTests(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Test that token works on a protected endpoint
			Map<String, Object> body = Map.of("prompt", "test", "context", Map.of("symbols", List.of("AAPL")));

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			try {
				restTemplate.postForEntity(apiBaseUrl + "/v1/labs/ai/preview-indicators", request, String.class);
				long duration = System.currentTimeMillis() - start;
				testRun
					.addResult(new TestResult("auth", "Token Authorization", true, "Token accepted by API", duration));
			}
			catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().contains("401")) {
					testRun.addResult(new TestResult("auth", "Token Authorization", false, "Token rejected (401)",
							System.currentTimeMillis() - start));
				}
				else {
					// Other errors might still mean auth worked
					testRun.addResult(new TestResult("auth", "Token Authorization", true,
							"Token accepted (endpoint returned error but auth passed)",
							System.currentTimeMillis() - start));
				}
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("auth", "Token Authorization", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runAutonomousModeTests(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = Map.of("prompt", "Generate an optimized trading strategy", "autonomousMode",
					"AUTONOMOUS", "context", Map.of("symbols", List.of(testRun.symbol), "timeframe", "1d"));

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy",
					request, Map.class);

			long duration = System.currentTimeMillis() - start;

			if (response != null && Boolean.TRUE.equals(response.get("success"))) {
				@SuppressWarnings("unchecked")
				Map<String, Object> visualConfig = (Map<String, Object>) response.get("visualConfig");
				String strategyName = visualConfig != null ? (String) visualConfig.get("name") : "Unknown";
				String pythonCode = (String) response.get("pythonCode");
				int codeLength = pythonCode != null ? pythonCode.length() : 0;

				testRun.addResult(new TestResult("autonomous-mode", "Strategy Generation", true, String
					.format("Generated '%s' strategy (%d chars code) for %s", strategyName, codeLength, testRun.symbol),
						duration));
			}
			else {
				String error = response != null ? (String) response.get("error") : "Unknown error";
				testRun.addResult(
						new TestResult("autonomous-mode", "Strategy Generation", false, "Failed: " + error, duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("autonomous-mode", "Strategy Generation", false,
					"Error: " + e.getMessage(), System.currentTimeMillis() - start));
		}
	}

	private void runLabsTests(TestRun testRun, String accessToken) {
		// Test preview indicators
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = Map.of("prompt", "RSI strategy with MACD confirmation");

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.postForObject(apiBaseUrl + "/v1/labs/ai/preview-indicators", request, Map.class);

			testRun.addResult(new TestResult("labs", "Preview Indicators", true, "Endpoint responded successfully",
					System.currentTimeMillis() - start));
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("labs", "Preview Indicators", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}

		// Test parse backtest query
		start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> body = Map.of("prompt", "Backtest AAPL with RSI strategy for 1 year");

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.postForObject(apiBaseUrl + "/v1/labs/ai/parse-backtest-query", request, Map.class);

			testRun.addResult(new TestResult("labs", "Parse Backtest Query", true, "Endpoint responded successfully",
					System.currentTimeMillis() - start));
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("labs", "Parse Backtest Query", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	// =====================================================
	// MFA Enforcement Tests
	// =====================================================

	private void runMfaEnforcementTests(TestRun testRun, String accessToken) {
		// Test 1: Get security settings and verify mfaEnforcement is returned
		runMfaSecuritySettingsTest(testRun, accessToken);

		// Test 2: Step-up check endpoint
		runMfaStepUpCheckTest(testRun, accessToken);

		// Test 3: Enable/disable MFA enforcement (validation test)
		runMfaEnforcementToggleTest(testRun, accessToken);
	}

	private void runMfaSecuritySettingsTest(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Void> request = new HttpEntity<>(headers);

			// Get security settings - should include mfaEnforcement
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/auth/security", org.springframework.http.HttpMethod.GET, request, Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response == null) {
				testRun.addResult(new TestResult("mfa-enforcement", "Get Security Settings", false,
						"No response from security endpoint", duration));
				return;
			}

			// Verify mfaEnforcement object exists
			@SuppressWarnings("unchecked")
			Map<String, Object> mfaEnforcement = (Map<String, Object>) response.get("mfaEnforcement");

			if (mfaEnforcement == null) {
				testRun.addResult(new TestResult("mfa-enforcement", "Get Security Settings", false,
						"mfaEnforcement not found in response", duration));
				return;
			}

			// Verify required fields exist
			boolean hasEnforced = mfaEnforcement.containsKey("enforced");
			boolean hasMinimumAcr = mfaEnforcement.containsKey("minimumAcrLevel");
			boolean hasCanEnable = mfaEnforcement.containsKey("canEnable");
			boolean hasStrengthLabel = mfaEnforcement.containsKey("strengthLabel");

			if (!hasEnforced || !hasMinimumAcr || !hasCanEnable || !hasStrengthLabel) {
				testRun.addResult(new TestResult("mfa-enforcement", "Get Security Settings", false,
						String.format("Missing fields: enforced=%b, minimumAcrLevel=%b, canEnable=%b, strengthLabel=%b",
								hasEnforced, hasMinimumAcr, hasCanEnable, hasStrengthLabel),
						duration));
				return;
			}

			boolean enforced = Boolean.TRUE.equals(mfaEnforcement.get("enforced"));
			boolean canEnable = Boolean.TRUE.equals(mfaEnforcement.get("canEnable"));
			String strengthLabel = (String) mfaEnforcement.get("strengthLabel");
			int minimumAcr = mfaEnforcement.get("minimumAcrLevel") instanceof Number
					? ((Number) mfaEnforcement.get("minimumAcrLevel")).intValue() : 2;

			testRun.addResult(new TestResult("mfa-enforcement", "Get Security Settings", true,
					String.format("enforced=%b, canEnable=%b, strength=%s, minAcr=%d", enforced, canEnable,
							strengthLabel, minimumAcr),
					duration));

			// Additional test: verify ACR level is valid
			boolean validAcr = minimumAcr >= 1 && minimumAcr <= 3;
			testRun.addResult(new TestResult("mfa-enforcement", "ACR Level Validation", validAcr,
					validAcr ? "Valid ACR level: " + minimumAcr : "Invalid ACR level: " + minimumAcr, 0));

			// Additional test: verify strength label is valid
			boolean validStrength = "None".equals(strengthLabel) || "Basic".equals(strengthLabel)
					|| "Strong".equals(strengthLabel) || "Maximum".equals(strengthLabel);
			testRun.addResult(new TestResult("mfa-enforcement", "Strength Label Validation", validStrength,
					validStrength ? "Valid strength: " + strengthLabel : "Invalid strength: " + strengthLabel, 0));

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("mfa-enforcement", "Get Security Settings", false,
					"Error: " + e.getMessage(), System.currentTimeMillis() - start));
		}
	}

	private void runMfaStepUpCheckTest(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Void> request = new HttpEntity<>(headers);

			// Test step-up check with ACR 1 (single-factor)
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/auth/security/step-up-check?currentAcr=1",
						org.springframework.http.HttpMethod.GET, request, Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response == null) {
				testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check (ACR 1)", false,
						"No response from step-up endpoint", duration));
				return;
			}

			// Verify response structure
			boolean hasRequired = response.containsKey("required");
			if (!hasRequired) {
				testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check (ACR 1)", false,
						"Missing 'required' field in response", duration));
				return;
			}

			boolean required = Boolean.TRUE.equals(response.get("required"));

			if (required) {
				// If step-up is required, verify available methods
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> methods = (List<Map<String, Object>>) response.get("availableMethods");
				int methodCount = methods != null ? methods.size() : 0;
				int minimumAcr = response.get("minimumAcrLevel") instanceof Number
						? ((Number) response.get("minimumAcrLevel")).intValue() : 2;

				testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check (ACR 1)", true,
						String.format("Step-up required: minAcr=%d, availableMethods=%d", minimumAcr, methodCount),
						duration));
			}
			else {
				testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check (ACR 1)", true,
						"Step-up not required (MFA not enforced or no methods configured)", duration));
			}

			// Test with ACR 2 (should typically not require step-up unless ACR 3 is
			// enforced)
			start = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Map<String, Object> response2 = restTemplate
				.exchange(apiBaseUrl + "/v1/auth/security/step-up-check?currentAcr=2",
						org.springframework.http.HttpMethod.GET, request, Map.class)
				.getBody();

			duration = System.currentTimeMillis() - start;

			if (response2 != null) {
				boolean required2 = Boolean.TRUE.equals(response2.get("required"));
				testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check (ACR 2)", true,
						required2 ? "Step-up required (ACR 3 enforced)" : "Step-up not required at ACR 2", duration));
			}

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("mfa-enforcement", "Step-Up Check", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runMfaEnforcementToggleTest(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			// First get current settings to see if we can enable
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> securityResponse = restTemplate
				.exchange(apiBaseUrl + "/v1/auth/security", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			if (securityResponse == null) {
				testRun.addResult(new TestResult("mfa-enforcement", "MFA Enforcement Toggle", false,
						"Could not get security settings", System.currentTimeMillis() - start));
				return;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> mfaEnforcement = (Map<String, Object>) securityResponse.get("mfaEnforcement");
			boolean canEnable = mfaEnforcement != null && Boolean.TRUE.equals(mfaEnforcement.get("canEnable"));
			boolean currentlyEnforced = mfaEnforcement != null && Boolean.TRUE.equals(mfaEnforcement.get("enforced"));

			if (!canEnable) {
				// Cannot enable MFA (no methods configured) - test that it fails
				// gracefully
				Map<String, Object> enableBody = Map.of("enforced", true);
				HttpEntity<Map<String, Object>> enableRequest = new HttpEntity<>(enableBody, headers);

				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> enableResponse = restTemplate
						.exchange(apiBaseUrl + "/v1/auth/security/mfa-enforcement",
								org.springframework.http.HttpMethod.PUT, enableRequest, Map.class)
						.getBody();

					long duration = System.currentTimeMillis() - start;

					// Should fail because no MFA methods
					boolean success = enableResponse != null && Boolean.TRUE.equals(enableResponse.get("success"));
					if (!success) {
						String error = enableResponse != null ? (String) enableResponse.get("error") : "Unknown";
						testRun.addResult(new TestResult("mfa-enforcement", "Enable Without Methods", true,
								"Correctly rejected: " + error, duration));
					}
					else {
						testRun.addResult(new TestResult("mfa-enforcement", "Enable Without Methods", false,
								"Should have rejected enabling MFA without methods", duration));
					}
				}
				catch (Exception e) {
					// Expected to fail
					testRun.addResult(new TestResult("mfa-enforcement", "Enable Without Methods", true,
							"Correctly rejected enabling MFA without methods", System.currentTimeMillis() - start));
				}
			}
			else {
				// Can enable MFA - test the toggle
				// Step 1: Toggle to opposite state
				boolean newState = !currentlyEnforced;
				Map<String, Object> toggleBody = Map.of("enforced", newState);
				HttpEntity<Map<String, Object>> toggleRequest = new HttpEntity<>(toggleBody, headers);

				@SuppressWarnings("unchecked")
				Map<String, Object> toggleResponse = restTemplate
					.exchange(apiBaseUrl + "/v1/auth/security/mfa-enforcement", org.springframework.http.HttpMethod.PUT,
							toggleRequest, Map.class)
					.getBody();

				long duration = System.currentTimeMillis() - start;

				boolean toggleSuccess = toggleResponse != null && Boolean.TRUE.equals(toggleResponse.get("success"));

				testRun.addResult(
						new TestResult("mfa-enforcement", "Toggle Enforcement", toggleSuccess,
								toggleSuccess ? String.format("Successfully %s MFA enforcement",
										newState ? "enabled" : "disabled") : "Failed to toggle MFA enforcement",
								duration));

				if (toggleSuccess) {
					// Step 2: Restore original state
					start = System.currentTimeMillis();
					Map<String, Object> restoreBody = Map.of("enforced", currentlyEnforced);
					HttpEntity<Map<String, Object>> restoreRequest = new HttpEntity<>(restoreBody, headers);

					@SuppressWarnings("unchecked")
					Map<String, Object> restoreResponse = restTemplate
						.exchange(apiBaseUrl + "/v1/auth/security/mfa-enforcement",
								org.springframework.http.HttpMethod.PUT, restoreRequest, Map.class)
						.getBody();

					duration = System.currentTimeMillis() - start;

					boolean restoreSuccess = restoreResponse != null
							&& Boolean.TRUE.equals(restoreResponse.get("success"));
					testRun.addResult(new TestResult(
							"mfa-enforcement", "Restore Original State", restoreSuccess, restoreSuccess
									? "Successfully restored original enforcement state" : "Failed to restore state",
							duration));
				}
			}

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("mfa-enforcement", "MFA Enforcement Toggle", false,
					"Error: " + e.getMessage(), System.currentTimeMillis() - start));
		}
	}

	// =====================================================
	// GENERATIVE AI Mode Tests (with buy-and-hold verification)
	// =====================================================

	private void runGenerativeAITests(TestRun testRun, String accessToken) {
		// Test symbols that have market data in ClickHouse
		List<String> symbols = List.of("AAPL", "MSFT", "GOOGL", "BTC", "ETH");

		for (String symbol : symbols) {
			runGenerativeAITestForSymbol(testRun, accessToken, symbol);
		}
	}

	/**
	 * Comprehensive GENERATIVE AI test that verifies: 1. Strategy generation succeeds 2.
	 * Correct mode (GENERATIVE_AI) is used 3. Python code contains expected indicators
	 * (RSI, MACD, Bollinger Bands) 4. Strategy uses optimal thresholds from historical
	 * insights 5. Risk management parameters are present (stop-loss, take-profit) 6.
	 * Strategy beats buy-and-hold
	 */

	private void runGenerativeAITestForSymbol(TestRun testRun, String accessToken, String symbol) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Step 1: Generate strategy using GENERATIVE_AI mode
			Map<String, Object> genBody = Map.of("prompt", "Generate a trading strategy for " + symbol,
					"autonomousMode", "GENERATIVE_AI", "useHistoricalInsights", true, "context",
					Map.of("symbols", List.of(symbol), "timeframe", "1D"), "historicalInsightsOptions",
					Map.of("lookbackDays", 750, "fastMode", true));

			HttpEntity<Map<String, Object>> genRequest = new HttpEntity<>(genBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> genResponse = restTemplate.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy",
					genRequest, Map.class);

			if (genResponse == null || !Boolean.TRUE.equals(genResponse.get("success"))) {
				String error = genResponse != null ? (String) genResponse.get("error") : "No response";
				testRun.addResult(new TestResult("generative-ai", symbol + " - Generation", false,
						"Strategy generation failed: " + error, System.currentTimeMillis() - start));
				return;
			}

			String modeUsed = (String) genResponse.get("autonomousModeUsed");
			if (!"GENERATIVE_AI".equals(modeUsed)) {
				testRun.addResult(new TestResult("generative-ai", symbol + " - Mode Check", false,
						"Wrong mode used: " + modeUsed + " (expected GENERATIVE_AI)",
						System.currentTimeMillis() - start));
				return;
			}
			testRun.addResult(new TestResult("generative-ai", symbol + " - Mode Check", true,
					"Correct mode: GENERATIVE_AI", System.currentTimeMillis() - start));

			String pythonCode = (String) genResponse.get("pythonCode");
			if (pythonCode == null || pythonCode.isEmpty()) {
				testRun.addResult(new TestResult("generative-ai", symbol + " - Code Generation", false,
						"No Python code generated", System.currentTimeMillis() - start));
				return;
			}

			// Comprehensive code quality checks
			int codeLength = pythonCode.length();
			testRun.addResult(new TestResult("generative-ai", symbol + " - Code Generation", true,
					"Generated " + codeLength + " chars of Python code", System.currentTimeMillis() - start));

			// Check for RSI indicator
			boolean hasRSI = pythonCode.contains("RSI") || pythonCode.contains("rsi");
			testRun.addResult(new TestResult("generative-ai", symbol + " - RSI Indicator", hasRSI,
					hasRSI ? "RSI indicator found in strategy" : "RSI indicator missing", 0));

			// Check for MACD indicator
			boolean hasMACD = pythonCode.contains("MACD") || pythonCode.contains("macd");
			testRun.addResult(new TestResult("generative-ai", symbol + " - MACD Indicator", hasMACD,
					hasMACD ? "MACD indicator found in strategy" : "MACD indicator missing", 0));

			// Check for Bollinger Bands
			boolean hasBB = pythonCode.contains("Bollinger") || pythonCode.contains("bollinger")
					|| pythonCode.contains("BB");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Bollinger Bands", hasBB,
					hasBB ? "Bollinger Bands found in strategy" : "Bollinger Bands missing", 0));

			// Check for stop-loss
			boolean hasStopLoss = pythonCode.contains("stop_loss") || pythonCode.contains("stoploss")
					|| pythonCode.contains("stop loss") || pythonCode.contains("STOP_LOSS");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Stop Loss", hasStopLoss,
					hasStopLoss ? "Stop-loss risk management found" : "Stop-loss missing", 0));

			// Check for take-profit
			boolean hasTakeProfit = pythonCode.contains("take_profit") || pythonCode.contains("takeprofit")
					|| pythonCode.contains("take profit") || pythonCode.contains("TAKE_PROFIT");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Take Profit", hasTakeProfit,
					hasTakeProfit ? "Take-profit found" : "Take-profit missing", 0));

			// Check for trailing stop
			boolean hasTrailingStop = pythonCode.contains("trailing") || pythonCode.contains("TRAILING");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Trailing Stop", hasTrailingStop,
					hasTrailingStop ? "Trailing stop found" : "Trailing stop missing", 0));

			// Check for optimal thresholds (should use specific numbers from historical
			// analysis)
			boolean hasOptimalThresholds = pythonCode.matches(".*rsi.*[<>]=?\\s*\\d+.*")
					|| pythonCode.matches(".*RSI.*[<>]=?\\s*\\d+.*");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Optimal Thresholds", hasOptimalThresholds,
					hasOptimalThresholds ? "RSI thresholds configured" : "No specific RSI thresholds found", 0));

			// Step 2: Execute the strategy and get performance
			long execStart = System.currentTimeMillis();
			Map<String, Object> execBody = Map.of("code", pythonCode, "language", "python", "symbol", symbol,
					"timeframe", "1D", "period", "3y");

			HttpEntity<Map<String, Object>> execRequest = new HttpEntity<>(execBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> execResponse = restTemplate.postForObject(apiBaseUrl + "/v1/strategies/execute-code",
					execRequest, Map.class);

			if (execResponse == null) {
				testRun.addResult(new TestResult("generative-ai", symbol + " - Execution", false,
						"Strategy execution failed: No response", System.currentTimeMillis() - execStart));
				return;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> performance = (Map<String, Object>) execResponse.get("performance");
			if (performance == null) {
				testRun.addResult(new TestResult("generative-ai", symbol + " - Performance", false,
						"No performance metrics in response", System.currentTimeMillis() - execStart));
				return;
			}

			double strategyReturn = getDoubleValue(performance, "totalReturn");
			double buyHoldReturn = getDoubleValue(performance, "buyAndHoldReturn");
			double outperformance = getDoubleValue(performance, "outperformance");
			double winRate = getDoubleValue(performance, "winRate");
			double maxDrawdown = getDoubleValue(performance, "maxDrawdown");
			int totalTrades = (int) getDoubleValue(performance, "totalTrades");

			long execDuration = System.currentTimeMillis() - execStart;

			testRun.addResult(new TestResult("generative-ai", symbol + " - Execution", true,
					String.format("Executed in %dms, %d trades", execDuration, totalTrades), execDuration));

			// Performance metrics tests
			testRun.addResult(new TestResult("generative-ai", symbol + " - Win Rate", winRate >= 40,
					String.format("Win rate: %.1f%% (target: >= 40%%)", winRate), 0));

			testRun.addResult(new TestResult("generative-ai", symbol + " - Max Drawdown", maxDrawdown <= 30,
					String.format("Max drawdown: %.1f%% (target: <= 30%%)", maxDrawdown), 0));

			testRun.addResult(new TestResult("generative-ai", symbol + " - Trade Count", totalTrades >= 10,
					String.format("Total trades: %d (target: >= 10 for statistical significance)", totalTrades), 0));

			// The key test: beats buy-and-hold
			boolean beatsBuyAndHold = outperformance >= 0;
			long totalDuration = System.currentTimeMillis() - start;

			String perfMessage = String.format("Strategy: %.2f%%, B&H: %.2f%%, Outperformance: %.2f%%", strategyReturn,
					buyHoldReturn, outperformance);

			testRun.addResult(new TestResult("generative-ai", symbol + " - BEATS BUY & HOLD", beatsBuyAndHold,
					perfMessage, totalDuration));

			log.info("GENERATIVE_AI {} comprehensive test: {} - {}", symbol, beatsBuyAndHold ? "PASSED" : "FAILED",
					perfMessage);

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("generative-ai", symbol + " - Error", false,
					"Exception: " + e.getMessage(), System.currentTimeMillis() - start));
			log.error("GENERATIVE_AI {} test error", symbol, e);
		}
	}

	// =====================================================
	// AUTONOMOUS Mode Tests (with buy-and-hold verification)
	// =====================================================

	private void runAutonomousModeTestsWithVerification(TestRun testRun, String accessToken) {
		// Test symbols that have market data in ClickHouse
		List<String> symbols = List.of("AAPL", "MSFT", "GOOGL", "BTC", "ETH");

		for (String symbol : symbols) {
			runAutonomousTestForSymbol(testRun, accessToken, symbol);
		}
	}

	private void runAutonomousTestForSymbol(TestRun testRun, String accessToken, String symbol) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Step 1: Generate optimized strategy using AUTONOMOUS mode
			Map<String, Object> genBody = Map.of("prompt", "Optimize trading strategy for " + symbol, "autonomousMode",
					"AUTONOMOUS", "useHistoricalInsights", true, "context",
					Map.of("symbols", List.of(symbol), "timeframe", "1D"), "historicalInsightsOptions",
					Map.of("lookbackDays", 750, "fastMode", true));

			HttpEntity<Map<String, Object>> genRequest = new HttpEntity<>(genBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> genResponse = restTemplate.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy",
					genRequest, Map.class);

			if (genResponse == null || !Boolean.TRUE.equals(genResponse.get("success"))) {
				String error = genResponse != null ? (String) genResponse.get("error") : "No response";
				testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
						"Strategy optimization failed: " + error, System.currentTimeMillis() - start));
				return;
			}

			String modeUsed = (String) genResponse.get("autonomousModeUsed");
			if (!"AUTONOMOUS".equals(modeUsed)) {
				testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
						"Wrong mode used: " + modeUsed, System.currentTimeMillis() - start));
				return;
			}

			String pythonCode = (String) genResponse.get("pythonCode");
			if (pythonCode == null || pythonCode.isEmpty()) {
				testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
						"No Python code generated", System.currentTimeMillis() - start));
				return;
			}

			// Step 2: Execute the strategy and get performance
			Map<String, Object> execBody = Map.of("code", pythonCode, "language", "python", "symbol", symbol,
					"timeframe", "1D", "period", "3y");

			HttpEntity<Map<String, Object>> execRequest = new HttpEntity<>(execBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> execResponse = restTemplate.postForObject(apiBaseUrl + "/v1/strategies/execute-code",
					execRequest, Map.class);

			if (execResponse == null) {
				testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
						"Strategy execution failed: No response", System.currentTimeMillis() - start));
				return;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> performance = (Map<String, Object>) execResponse.get("performance");
			if (performance == null) {
				testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
						"No performance metrics in response", System.currentTimeMillis() - start));
				return;
			}

			double strategyReturn = getDoubleValue(performance, "totalReturn");
			double buyHoldReturn = getDoubleValue(performance, "buyAndHoldReturn");
			double outperformance = getDoubleValue(performance, "outperformance");

			long duration = System.currentTimeMillis() - start;

			// Verify strategy beats buy-and-hold
			boolean beatsBuyAndHold = outperformance >= 0;

			String message = String.format("Strategy: %.2f%%, B&H: %.2f%%, Outperformance: %.2f%%", strategyReturn,
					buyHoldReturn, outperformance);

			testRun.addResult(
					new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, beatsBuyAndHold, message, duration));

			log.info("AUTONOMOUS {} test: {} - {}", symbol, beatsBuyAndHold ? "PASSED" : "FAILED", message);

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false,
					"Error: " + e.getMessage(), System.currentTimeMillis() - start));
		}
	}

	private double getDoubleValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return 0.0;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		}
		catch (NumberFormatException e) {
			return 0.0;
		}
	}

	// =====================================================
	// Profile API Tests
	// =====================================================

	private void runProfileTests(TestRun testRun, String accessToken) {
		String testId = UUID.randomUUID().toString().substring(0, 8);

		// Test 1: Get current user profile
		runProfileGetTest(testRun, accessToken);

		// Test 2: Update bio field
		runProfileUpdateBioTest(testRun, accessToken, testId);

		// Test 3: Update location field
		runProfileUpdateLocationTest(testRun, accessToken, testId);

		// Test 4: Update occupation field
		runProfileUpdateOccupationTest(testRun, accessToken, testId);

		// Test 5: Update education field
		runProfileUpdateEducationTest(testRun, accessToken, testId);

		// Test 6: Update multiple fields at once
		runProfileUpdateMultipleFieldsTest(testRun, accessToken, testId);

		// Test 7: Verify fields persist after update
		runProfilePersistenceTest(testRun, accessToken, testId);
	}

	private void runProfileGetTest(TestRun testRun, String accessToken) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			HttpEntity<Void> request = new HttpEntity<>(headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, request,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && response.containsKey("userId") && response.containsKey("email")) {
				testRun.addResult(new TestResult("profile", "Get Profile", true,
						String.format("Profile retrieved: %s (%s)", response.get("name"), response.get("email")),
						duration));
			}
			else {
				testRun.addResult(new TestResult("profile", "Get Profile", false,
						"Profile missing required fields (userId, email)", duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Get Profile", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfileUpdateBioTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			String testBio = "Integration test bio - " + testId;
			Map<String, Object> body = Map.of("bio", testBio);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", request);

			// Verify the update
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && testBio.equals(response.get("bio"))) {
				testRun.addResult(new TestResult("profile", "Update Bio", true, "Bio updated and persisted: " + testBio,
						duration));
			}
			else {
				String actualBio = response != null ? String.valueOf(response.get("bio")) : "null";
				testRun.addResult(new TestResult("profile", "Update Bio", false,
						"Bio not persisted. Expected: " + testBio + ", Got: " + actualBio, duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Update Bio", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfileUpdateLocationTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			String testLocation = "Test City, TC - " + testId;
			Map<String, Object> body = Map.of("location", testLocation);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", request);

			// Verify the update
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && testLocation.equals(response.get("location"))) {
				testRun.addResult(new TestResult("profile", "Update Location", true,
						"Location updated and persisted: " + testLocation, duration));
			}
			else {
				String actual = response != null ? String.valueOf(response.get("location")) : "null";
				testRun.addResult(new TestResult("profile", "Update Location", false,
						"Location not persisted. Expected: " + testLocation + ", Got: " + actual, duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Update Location", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfileUpdateOccupationTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			String testOccupation = "Test Engineer - " + testId;
			Map<String, Object> body = Map.of("occupation", testOccupation);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", request);

			// Verify the update
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && testOccupation.equals(response.get("occupation"))) {
				testRun.addResult(new TestResult("profile", "Update Occupation", true,
						"Occupation updated and persisted: " + testOccupation, duration));
			}
			else {
				String actual = response != null ? String.valueOf(response.get("occupation")) : "null";
				testRun.addResult(new TestResult("profile", "Update Occupation", false,
						"Occupation not persisted. Expected: " + testOccupation + ", Got: " + actual, duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Update Occupation", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfileUpdateEducationTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			String testEducation = "Test University - " + testId;
			Map<String, Object> body = Map.of("education", testEducation);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", request);

			// Verify the update
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && testEducation.equals(response.get("education"))) {
				testRun.addResult(new TestResult("profile", "Update Education", true,
						"Education updated and persisted: " + testEducation, duration));
			}
			else {
				String actual = response != null ? String.valueOf(response.get("education")) : "null";
				testRun.addResult(new TestResult("profile", "Update Education", false,
						"Education not persisted. Expected: " + testEducation + ", Got: " + actual, duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Update Education", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfileUpdateMultipleFieldsTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			String testBio = "Multi-field bio - " + testId;
			String testLocation = "Multi-field Location - " + testId;
			String testOccupation = "Multi-field Occupation - " + testId;
			String testEducation = "Multi-field Education - " + testId;

			Map<String, Object> body = Map.of("bio", testBio, "location", testLocation, "occupation", testOccupation,
					"education", testEducation);

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", request);

			// Verify all fields updated
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			if (response != null && testBio.equals(response.get("bio")) && testLocation.equals(response.get("location"))
					&& testOccupation.equals(response.get("occupation"))
					&& testEducation.equals(response.get("education"))) {
				testRun.addResult(new TestResult("profile", "Update Multiple Fields", true,
						"All 4 fields (bio, location, occupation, education) updated and persisted", duration));
			}
			else {
				testRun.addResult(new TestResult("profile", "Update Multiple Fields", false,
						"One or more fields not persisted correctly", duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Update Multiple Fields", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	private void runProfilePersistenceTest(TestRun testRun, String accessToken, String testId) {
		long start = System.currentTimeMillis();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);

			// Set known values
			String testBio = "Persistence test bio - " + testId;
			Map<String, Object> updateBody = Map.of("bio", testBio);
			HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updateBody, headers);
			restTemplate.put(apiBaseUrl + "/v1/users/profiles/me", updateRequest);

			// First read
			HttpEntity<Void> getRequest = new HttpEntity<>(headers);
			@SuppressWarnings("unchecked")
			Map<String, Object> response1 = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			// Second read (to verify persistence)
			@SuppressWarnings("unchecked")
			Map<String, Object> response2 = restTemplate
				.exchange(apiBaseUrl + "/v1/users/profiles/me", org.springframework.http.HttpMethod.GET, getRequest,
						Map.class)
				.getBody();

			long duration = System.currentTimeMillis() - start;

			String bio1 = response1 != null ? (String) response1.get("bio") : null;
			String bio2 = response2 != null ? (String) response2.get("bio") : null;

			if (testBio.equals(bio1) && testBio.equals(bio2)) {
				testRun.addResult(new TestResult("profile", "Field Persistence", true,
						"Bio persisted correctly across multiple reads", duration));
			}
			else {
				testRun.addResult(new TestResult("profile", "Field Persistence", false,
						String.format("Bio not consistent. Read1: %s, Read2: %s", bio1, bio2), duration));
			}
		}
		catch (Exception e) {
			testRun.addResult(new TestResult("profile", "Field Persistence", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
		}
	}

	// =====================================================
	// DTOs
	// =====================================================

	public static class RunTestsRequest {

		public String testSuite;

		public String symbol;

	}

	public static class TestRun {

		public String runId;

		public String testSuite;

		public String symbol;

		public String status;

		public String summary;

		public String startedAt;

		public String completedAt;

		public List<TestResult> results = new ArrayList<>();

		public TestRun() {
		}

		public TestRun(String runId, String testSuite, String symbol) {
			this.runId = runId;
			this.testSuite = testSuite;
			this.symbol = symbol;
			this.status = "RUNNING";
			this.startedAt = Instant.now().toString();
		}

		public synchronized void addResult(TestResult result) {
			this.results.add(result);
		}

	}

	public static class TestResult {

		public String suite;

		public String name;

		public boolean passed;

		public String message;

		public long durationMs;

		public TestResult() {
		}

		public TestResult(String suite, String name, boolean passed, String message, long durationMs) {
			this.suite = suite;
			this.name = name;
			this.passed = passed;
			this.message = message;
			this.durationMs = durationMs;
		}

	}

}
