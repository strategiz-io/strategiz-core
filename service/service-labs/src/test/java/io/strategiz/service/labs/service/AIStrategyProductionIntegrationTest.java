package io.strategiz.service.labs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Production Integration Tests for AI Strategy Generation.
 *
 * These tests run against the REAL API (local or production) to verify: 1. Both
 * GENERATIVE_AI and AUTONOMOUS modes generate valid strategies 2. Generated strategies
 * BEAT buy-and-hold performance 3. Tests pass for multiple symbols
 *
 * Configuration: - STRATEGIZ_API_URL: Base URL (default: http://localhost:8080) -
 * STRATEGIZ_AUTH_TOKEN: Authentication token (required) - STRATEGIZ_PROD_TEST: Set to
 * "true" to enable these tests
 *
 * Run with: STRATEGIZ_PROD_TEST=true STRATEGIZ_AUTH_TOKEN=your-token \ mvn test
 * -Dtest=AIStrategyProductionIntegrationTest
 *
 * For local testing, start the server first: mvn spring-boot:run -pl application-api
 */
@EnabledIfEnvironmentVariable(named = "STRATEGIZ_PROD_TEST", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AI Strategy Production Integration Tests")
class AIStrategyProductionIntegrationTest {

	private static final String DEFAULT_API_URL = "http://localhost:8080";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static HttpClient httpClient;

	private static String apiUrl;

	private static String authToken;

	// Track results across all tests for final summary
	private static final ConcurrentHashMap<String, TestResult> generativeResults = new ConcurrentHashMap<>();

	private static final ConcurrentHashMap<String, TestResult> autonomousResults = new ConcurrentHashMap<>();

	private static final AtomicInteger generativePassed = new AtomicInteger(0);

	private static final AtomicInteger generativeFailed = new AtomicInteger(0);

	private static final AtomicInteger autonomousPassed = new AtomicInteger(0);

	private static final AtomicInteger autonomousFailed = new AtomicInteger(0);

	// Symbols to test - covers stocks, ETFs, and different market caps
	private static final String[] TEST_SYMBOLS = { "AAPL", "MSFT", "SPY", "QQQ", "TSLA" };

	// Minimum outperformance required (strategy return - buy-and-hold return)
	private static final double MIN_OUTPERFORMANCE = 0.0; // Must beat buy-and-hold

	@BeforeAll
	static void setup() {
		// Initialize HTTP client
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

		// Get configuration from environment
		apiUrl = System.getenv("STRATEGIZ_API_URL");
		if (apiUrl == null || apiUrl.isEmpty()) {
			apiUrl = DEFAULT_API_URL;
		}

		authToken = System.getenv("STRATEGIZ_AUTH_TOKEN");
		if (authToken == null || authToken.isEmpty()) {
			fail("STRATEGIZ_AUTH_TOKEN environment variable is required. "
					+ "Get a token by logging into the app and extracting it from browser dev tools.");
		}

		System.out.println("\n" + "=".repeat(80));
		System.out.println("AI STRATEGY PRODUCTION INTEGRATION TESTS");
		System.out.println("=".repeat(80));
		System.out.println("API URL: " + apiUrl);
		System.out.println("Testing both GENERATIVE_AI and AUTONOMOUS modes");
		System.out.println("Symbols: " + String.join(", ", TEST_SYMBOLS));
		System.out.println("Requirement: Strategy must beat buy-and-hold");
		System.out.println("=".repeat(80) + "\n");
	}

	@AfterAll
	static void printSummary() {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("FINAL RESULTS SUMMARY");
		System.out.println("=".repeat(80));

		System.out.println("\n--- GENERATIVE AI MODE ---");
		for (String symbol : TEST_SYMBOLS) {
			TestResult result = generativeResults.get(symbol);
			if (result != null) {
				System.out.printf("  %s: %s (Strategy: %.2f%%, B&H: %.2f%%, Outperformance: %.2f%%)%n", symbol,
						result.passed ? "✅ PASS" : "❌ FAIL", result.strategyReturn, result.buyAndHoldReturn,
						result.outperformance);
			}
			else {
				System.out.printf("  %s: ⚠️ NOT RUN%n", symbol);
			}
		}
		System.out.printf("  TOTAL: %d passed, %d failed%n", generativePassed.get(), generativeFailed.get());

		System.out.println("\n--- AUTONOMOUS MODE ---");
		for (String symbol : TEST_SYMBOLS) {
			TestResult result = autonomousResults.get(symbol);
			if (result != null) {
				System.out.printf("  %s: %s (Strategy: %.2f%%, B&H: %.2f%%, Outperformance: %.2f%%)%n", symbol,
						result.passed ? "✅ PASS" : "❌ FAIL", result.strategyReturn, result.buyAndHoldReturn,
						result.outperformance);
			}
			else {
				System.out.printf("  %s: ⚠️ NOT RUN%n", symbol);
			}
		}
		System.out.printf("  TOTAL: %d passed, %d failed%n", autonomousPassed.get(), autonomousFailed.get());

		System.out.println("\n" + "=".repeat(80));
		boolean allPassed = generativeFailed.get() == 0 && autonomousFailed.get() == 0 && generativePassed.get() > 0
				&& autonomousPassed.get() > 0;
		System.out.println(allPassed ? "✅ ALL TESTS PASSED" : "❌ SOME TESTS FAILED");
		System.out.println("=".repeat(80) + "\n");
	}

	// ========================================================================
	// GENERATIVE AI MODE TESTS
	// ========================================================================

	@Nested
	@DisplayName("GENERATIVE AI Mode - Production Tests")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class GenerativeAIModeTests {

		@ParameterizedTest(name = "GENERATIVE_AI: {0} must beat buy-and-hold")
		@ValueSource(strings = { "AAPL", "MSFT", "SPY", "QQQ", "TSLA" })
		@Order(1)
		@DisplayName("GENERATIVE_AI mode must beat buy-and-hold")
		void testGenerativeAIMustBeatBuyAndHold(String symbol) throws Exception {
			System.out.printf("%n--- Testing GENERATIVE_AI mode for %s ---%n", symbol);

			try {
				// Step 1: Generate strategy using GENERATIVE_AI mode
				System.out.printf("  [1/3] Generating strategy for %s...%n", symbol);
				JsonNode generateResponse = callGenerateStrategy(symbol, "GENERATIVE_AI");

				assertTrue(generateResponse.path("success").asBoolean(false),
						"Strategy generation should succeed: " + generateResponse.path("error").asText());

				String pythonCode = generateResponse.path("pythonCode").asText();
				assertNotNull(pythonCode, "Python code should be generated");
				assertFalse(pythonCode.isEmpty(), "Python code should not be empty");

				assertEquals("GENERATIVE_AI", generateResponse.path("autonomousModeUsed").asText(),
						"Should use GENERATIVE_AI mode");

				System.out.printf("  [2/3] Strategy generated (%d chars). Executing backtest...%n",
						pythonCode.length());

				// Step 2: Execute the generated strategy
				JsonNode execResponse = callExecuteCode(pythonCode, symbol);
				JsonNode perf = execResponse.path("performance");

				assertFalse(perf.isMissingNode(), "Performance metrics should be present");

				double strategyReturn = perf.path("totalReturn").asDouble(0);
				double buyAndHoldReturn = perf.path("buyAndHoldReturn").asDouble(0);
				double outperformance = perf.path("outperformance").asDouble(strategyReturn - buyAndHoldReturn);

				System.out.printf("  [3/3] Results: Strategy=%.2f%%, B&H=%.2f%%, Outperformance=%.2f%%%n",
						strategyReturn, buyAndHoldReturn, outperformance);

				// Step 3: Record result and assert
				boolean passed = outperformance >= MIN_OUTPERFORMANCE;
				generativeResults.put(symbol, new TestResult(passed, strategyReturn, buyAndHoldReturn, outperformance));

				if (passed) {
					generativePassed.incrementAndGet();
					System.out.printf("  ✅ PASS: %s GENERATIVE_AI beats buy-and-hold by %.2f%%%n", symbol,
							outperformance);
				}
				else {
					generativeFailed.incrementAndGet();
					System.out.printf("  ❌ FAIL: %s GENERATIVE_AI underperformed by %.2f%%%n", symbol, outperformance);
				}

				assertTrue(passed,
						String.format(
								"%s GENERATIVE_AI: Strategy (%.2f%%) must beat buy-and-hold (%.2f%%). "
										+ "Outperformance: %.2f%% (required: >= %.2f%%)",
								symbol, strategyReturn, buyAndHoldReturn, outperformance, MIN_OUTPERFORMANCE));

			}
			catch (Exception e) {
				generativeFailed.incrementAndGet();
				generativeResults.put(symbol, new TestResult(false, 0, 0, 0));
				System.out.printf("  ❌ ERROR: %s GENERATIVE_AI failed with exception: %s%n", symbol, e.getMessage());
				throw e;
			}
		}

	}

	// ========================================================================
	// AUTONOMOUS MODE TESTS
	// ========================================================================

	@Nested
	@DisplayName("AUTONOMOUS Mode - Production Tests")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AutonomousModeTests {

		@ParameterizedTest(name = "AUTONOMOUS: {0} must beat buy-and-hold")
		@ValueSource(strings = { "AAPL", "MSFT", "SPY", "QQQ", "TSLA" })
		@Order(2)
		@DisplayName("AUTONOMOUS mode must beat buy-and-hold")
		void testAutonomousMustBeatBuyAndHold(String symbol) throws Exception {
			System.out.printf("%n--- Testing AUTONOMOUS mode for %s ---%n", symbol);

			try {
				// Step 1: Generate strategy using AUTONOMOUS mode
				System.out.printf("  [1/2] Running AUTONOMOUS optimization for %s...%n", symbol);
				JsonNode generateResponse = callGenerateStrategy(symbol, "AUTONOMOUS");

				assertTrue(generateResponse.path("success").asBoolean(false),
						"Strategy generation should succeed: " + generateResponse.path("error").asText());

				assertEquals("AUTONOMOUS", generateResponse.path("autonomousModeUsed").asText(),
						"Should use AUTONOMOUS mode");

				String pythonCode = generateResponse.path("pythonCode").asText();
				assertNotNull(pythonCode, "Python code should be generated");
				assertFalse(pythonCode.isEmpty(), "Python code should not be empty");

				// Step 2: Execute the optimized strategy
				System.out.printf("  [2/2] Executing optimized strategy...%n");
				JsonNode execResponse = callExecuteCode(pythonCode, symbol);
				JsonNode perf = execResponse.path("performance");

				assertFalse(perf.isMissingNode(), "Performance metrics should be present");

				double strategyReturn = perf.path("totalReturn").asDouble(0);
				double buyAndHoldReturn = perf.path("buyAndHoldReturn").asDouble(0);
				double outperformance = perf.path("outperformance").asDouble(strategyReturn - buyAndHoldReturn);

				System.out.printf("  Results: Strategy=%.2f%%, B&H=%.2f%%, Outperformance=%.2f%%%n", strategyReturn,
						buyAndHoldReturn, outperformance);

				// Record result and assert
				boolean passed = outperformance >= MIN_OUTPERFORMANCE;
				autonomousResults.put(symbol, new TestResult(passed, strategyReturn, buyAndHoldReturn, outperformance));

				if (passed) {
					autonomousPassed.incrementAndGet();
					System.out.printf("  ✅ PASS: %s AUTONOMOUS beats buy-and-hold by %.2f%%%n", symbol, outperformance);
				}
				else {
					autonomousFailed.incrementAndGet();
					System.out.printf("  ❌ FAIL: %s AUTONOMOUS underperformed by %.2f%%%n", symbol, outperformance);
				}

				assertTrue(passed,
						String.format(
								"%s AUTONOMOUS: Strategy (%.2f%%) must beat buy-and-hold (%.2f%%). "
										+ "Outperformance: %.2f%% (required: >= %.2f%%)",
								symbol, strategyReturn, buyAndHoldReturn, outperformance, MIN_OUTPERFORMANCE));

			}
			catch (Exception e) {
				autonomousFailed.incrementAndGet();
				autonomousResults.put(symbol, new TestResult(false, 0, 0, 0));
				System.out.printf("  ❌ ERROR: %s AUTONOMOUS failed with exception: %s%n", symbol, e.getMessage());
				throw e;
			}
		}

	}

	// ========================================================================
	// CROSS-MODE VALIDATION
	// ========================================================================

	@Test
	@Order(100)
	@DisplayName("Both modes must pass for all symbols")
	void validateAllTestsPassed() {
		System.out.println("\n--- Final Validation ---");

		List<String> failures = new ArrayList<>();

		for (String symbol : TEST_SYMBOLS) {
			TestResult genResult = generativeResults.get(symbol);
			TestResult autoResult = autonomousResults.get(symbol);

			if (genResult == null || !genResult.passed) {
				failures.add(symbol + " GENERATIVE_AI");
			}
			if (autoResult == null || !autoResult.passed) {
				failures.add(symbol + " AUTONOMOUS");
			}
		}

		if (!failures.isEmpty()) {
			fail("The following tests failed to beat buy-and-hold: " + String.join(", ", failures));
		}

		System.out.println("✅ All modes for all symbols beat buy-and-hold!");
	}

	// ========================================================================
	// HTTP CLIENT HELPERS
	// ========================================================================

	private JsonNode callGenerateStrategy(String symbol, String autonomousMode) throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("prompt", "Generate a trading strategy for " + symbol, "autonomousMode", autonomousMode,
						"useHistoricalInsights", true, "context", Map.of("symbols", List.of(symbol), "timeframe", "1D"),
						"historicalInsightsOptions", Map.of("lookbackDays", 750, "fastMode", true)));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/labs/ai/generate-strategy"))
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofMinutes(5)) // Long timeout for AI operations
			.POST(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new RuntimeException(
					"Generate strategy failed with status " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}

	private JsonNode callExecuteCode(String code, String symbol) throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("code", code, "language", "python", "symbol", symbol, "timeframe", "1D", "period", "3y"));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/strategies/execute-code"))
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofMinutes(2))
			.POST(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new RuntimeException(
					"Execute code failed with status " + response.statusCode() + ": " + response.body());
		}

		return objectMapper.readTree(response.body());
	}

	// ========================================================================
	// HELPER CLASS
	// ========================================================================

	/**
	 * Helper class to store test results for summary reporting.
	 */
	private static class TestResult {

		final boolean passed;

		final double strategyReturn;

		final double buyAndHoldReturn;

		final double outperformance;

		TestResult(boolean passed, double strategyReturn, double buyAndHoldReturn, double outperformance) {
			this.passed = passed;
			this.strategyReturn = strategyReturn;
			this.buyAndHoldReturn = buyAndHoldReturn;
			this.outperformance = outperformance;
		}

	}

}
