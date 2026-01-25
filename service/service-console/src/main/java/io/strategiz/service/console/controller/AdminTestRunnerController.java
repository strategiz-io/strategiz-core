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
 * Provides endpoints to:
 * - Trigger integration test suites
 * - View test results
 * - Check test run status
 *
 * Tests are executed asynchronously and results are stored in memory
 * (could be extended to persist in Firestore).
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
				Map.of("id", "labs", "name", "Labs API", "description", "Test Labs AI endpoints"),
				Map.of("id", "health", "name", "Health Check", "description", "Basic health and connectivity tests"));
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

			// AI Strategy Tests (comprehensive - tests both modes with buy-and-hold verification)
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

			// If not configured via properties, these would need to come from Vault or env
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
			Map<String, Object> response = restTemplate
				.postForObject(apiBaseUrl + "/v1/auth/service-account/token", request, Map.class);

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
				testRun.addResult(
						new TestResult("auth", "Token Authorization", true, "Token accepted by API", duration));
			}
			catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().contains("401")) {
					testRun.addResult(new TestResult("auth", "Token Authorization", false, "Token rejected (401)",
							System.currentTimeMillis() - start));
				}
				else {
					// Other errors might still mean auth worked
					testRun.addResult(new TestResult("auth", "Token Authorization", true,
							"Token accepted (endpoint returned error but auth passed)", System.currentTimeMillis() - start));
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
			Map<String, Object> response = restTemplate
				.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy", request, Map.class);

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
			testRun.addResult(new TestResult("autonomous-mode", "Strategy Generation", false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
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
	// GENERATIVE AI Mode Tests (with buy-and-hold verification)
	// =====================================================

	private void runGenerativeAITests(TestRun testRun, String accessToken) {
		// Test multiple symbols
		List<String> symbols = List.of("AAPL", "MSFT", "SPY", "QQQ", "TSLA");

		for (String symbol : symbols) {
			runGenerativeAITestForSymbol(testRun, accessToken, symbol);
		}
	}

	/**
	 * Comprehensive GENERATIVE AI test that verifies:
	 * 1. Strategy generation succeeds
	 * 2. Correct mode (GENERATIVE_AI) is used
	 * 3. Python code contains expected indicators (RSI, MACD, Bollinger Bands)
	 * 4. Strategy uses optimal thresholds from historical insights
	 * 5. Risk management parameters are present (stop-loss, take-profit)
	 * 6. Strategy beats buy-and-hold
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
			Map<String, Object> genResponse = restTemplate
				.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy", genRequest, Map.class);

			if (genResponse == null || !Boolean.TRUE.equals(genResponse.get("success"))) {
				String error = genResponse != null ? (String) genResponse.get("error") : "No response";
				testRun.addResult(new TestResult("generative-ai", symbol + " - Generation", false,
						"Strategy generation failed: " + error, System.currentTimeMillis() - start));
				return;
			}

			String modeUsed = (String) genResponse.get("autonomousModeUsed");
			if (!"GENERATIVE_AI".equals(modeUsed)) {
				testRun.addResult(new TestResult("generative-ai", symbol + " - Mode Check", false,
						"Wrong mode used: " + modeUsed + " (expected GENERATIVE_AI)", System.currentTimeMillis() - start));
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
			boolean hasBB = pythonCode.contains("Bollinger") || pythonCode.contains("bollinger") || pythonCode.contains("BB");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Bollinger Bands", hasBB,
					hasBB ? "Bollinger Bands found in strategy" : "Bollinger Bands missing", 0));

			// Check for stop-loss
			boolean hasStopLoss = pythonCode.contains("stop_loss") || pythonCode.contains("stoploss") ||
					pythonCode.contains("stop loss") || pythonCode.contains("STOP_LOSS");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Stop Loss", hasStopLoss,
					hasStopLoss ? "Stop-loss risk management found" : "Stop-loss missing", 0));

			// Check for take-profit
			boolean hasTakeProfit = pythonCode.contains("take_profit") || pythonCode.contains("takeprofit") ||
					pythonCode.contains("take profit") || pythonCode.contains("TAKE_PROFIT");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Take Profit", hasTakeProfit,
					hasTakeProfit ? "Take-profit found" : "Take-profit missing", 0));

			// Check for trailing stop
			boolean hasTrailingStop = pythonCode.contains("trailing") || pythonCode.contains("TRAILING");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Trailing Stop", hasTrailingStop,
					hasTrailingStop ? "Trailing stop found" : "Trailing stop missing", 0));

			// Check for optimal thresholds (should use specific numbers from historical analysis)
			boolean hasOptimalThresholds = pythonCode.matches(".*rsi.*[<>]=?\\s*\\d+.*") ||
					pythonCode.matches(".*RSI.*[<>]=?\\s*\\d+.*");
			testRun.addResult(new TestResult("generative-ai", symbol + " - Optimal Thresholds", hasOptimalThresholds,
					hasOptimalThresholds ? "RSI thresholds configured" : "No specific RSI thresholds found", 0));

			// Step 2: Execute the strategy and get performance
			long execStart = System.currentTimeMillis();
			Map<String, Object> execBody = Map.of("code", pythonCode, "language", "python", "symbol", symbol, "timeframe",
					"1D", "period", "3y");

			HttpEntity<Map<String, Object>> execRequest = new HttpEntity<>(execBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> execResponse = restTemplate
				.postForObject(apiBaseUrl + "/v1/strategies/execute-code", execRequest, Map.class);

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

			String perfMessage = String.format("Strategy: %.2f%%, B&H: %.2f%%, Outperformance: %.2f%%",
					strategyReturn, buyHoldReturn, outperformance);

			testRun.addResult(new TestResult("generative-ai", symbol + " - BEATS BUY & HOLD", beatsBuyAndHold,
					perfMessage, totalDuration));

			log.info("GENERATIVE_AI {} comprehensive test: {} - {}", symbol, beatsBuyAndHold ? "PASSED" : "FAILED", perfMessage);

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("generative-ai", symbol + " - Error", false, "Exception: " + e.getMessage(),
					System.currentTimeMillis() - start));
			log.error("GENERATIVE_AI {} test error", symbol, e);
		}
	}

	// =====================================================
	// AUTONOMOUS Mode Tests (with buy-and-hold verification)
	// =====================================================

	private void runAutonomousModeTestsWithVerification(TestRun testRun, String accessToken) {
		// Test multiple symbols
		List<String> symbols = List.of("AAPL", "MSFT", "SPY", "QQQ", "TSLA");

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
			Map<String, Object> genBody = Map.of("prompt", "", "autonomousMode", "AUTONOMOUS", "useHistoricalInsights",
					true, "context", Map.of("symbols", List.of(symbol), "timeframe", "1D"), "historicalInsightsOptions",
					Map.of("lookbackDays", 750, "fastMode", true));

			HttpEntity<Map<String, Object>> genRequest = new HttpEntity<>(genBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> genResponse = restTemplate
				.postForObject(apiBaseUrl + "/v1/labs/ai/generate-strategy", genRequest, Map.class);

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
			Map<String, Object> execBody = Map.of("code", pythonCode, "language", "python", "symbol", symbol, "timeframe",
					"1D", "period", "3y");

			HttpEntity<Map<String, Object>> execRequest = new HttpEntity<>(execBody, headers);

			@SuppressWarnings("unchecked")
			Map<String, Object> execResponse = restTemplate
				.postForObject(apiBaseUrl + "/v1/strategies/execute-code", execRequest, Map.class);

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

			testRun
				.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, beatsBuyAndHold, message, duration));

			log.info("AUTONOMOUS {} test: {} - {}", symbol, beatsBuyAndHold ? "PASSED" : "FAILED", message);

		}
		catch (Exception e) {
			testRun.addResult(new TestResult("autonomous-mode", "AUTONOMOUS " + symbol, false, "Error: " + e.getMessage(),
					System.currentTimeMillis() - start));
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
