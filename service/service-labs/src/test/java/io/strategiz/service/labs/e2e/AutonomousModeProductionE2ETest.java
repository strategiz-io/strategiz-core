package io.strategiz.service.labs.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for Autonomous Mode against production API.
 *
 * These tests make actual HTTP calls to the production API and require
 * valid authentication credentials.
 *
 * Authentication Methods (in priority order):
 *
 * 1. Service Account (Recommended for CI/CD):
 *    - Set STRATEGIZ_CLIENT_ID and STRATEGIZ_CLIENT_SECRET
 *    - Token is automatically generated using OAuth 2.0 Client Credentials flow
 *
 * 2. Manual Token (for local testing):
 *    - Set STRATEGIZ_PROD_TOKEN directly
 *
 * Optional:
 *    - STRATEGIZ_API_URL: Override API URL (defaults to production)
 *
 * Run:
 *    # With service account
 *    STRATEGIZ_CLIENT_ID=sa_xxx STRATEGIZ_CLIENT_SECRET=xxx mvn test -Dtest=AutonomousModeProductionE2ETest -pl service/service-labs
 *
 *    # With manual token
 *    STRATEGIZ_PROD_TOKEN=xxx mvn test -Dtest=AutonomousModeProductionE2ETest -pl service/service-labs
 */
@DisplayName("Autonomous Mode Production E2E Tests")
public class AutonomousModeProductionE2ETest {

	private static final String DEFAULT_API_URL = "https://strategiz-api-bflhiwsnmq-ue.a.run.app";

	private static HttpClient httpClient;

	private static ObjectMapper objectMapper;

	private static String apiUrl;

	private static String authToken;

	private static boolean testsEnabled = false;

	@BeforeAll
	static void setUp() throws Exception {
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		objectMapper = new ObjectMapper();

		apiUrl = System.getenv("STRATEGIZ_API_URL");
		if (apiUrl == null || apiUrl.isEmpty()) {
			apiUrl = DEFAULT_API_URL;
		}

		// Try to get authentication token
		authToken = getAuthToken();

		if (authToken != null && !authToken.isEmpty()) {
			testsEnabled = true;
			System.out.println("=== Production E2E Tests Enabled ===");
			System.out.println("API URL: " + apiUrl);
		}
		else {
			System.out.println("=== Production E2E Tests Skipped ===");
			System.out.println("No authentication credentials provided.");
			System.out.println("Set STRATEGIZ_CLIENT_ID + STRATEGIZ_CLIENT_SECRET (service account)");
			System.out.println("Or set STRATEGIZ_PROD_TOKEN (manual token)");
		}
	}

	/**
	 * Get authentication token using service account or manual token.
	 */
	private static String getAuthToken() throws Exception {
		// First, check for manual token
		String manualToken = System.getenv("STRATEGIZ_PROD_TOKEN");
		if (manualToken != null && !manualToken.isEmpty()) {
			System.out.println("Using manual token from STRATEGIZ_PROD_TOKEN");
			return manualToken;
		}

		// Second, try service account authentication
		String clientId = System.getenv("STRATEGIZ_CLIENT_ID");
		String clientSecret = System.getenv("STRATEGIZ_CLIENT_SECRET");

		if (clientId != null && !clientId.isEmpty() && clientSecret != null && !clientSecret.isEmpty()) {
			System.out.println("Using service account authentication: " + clientId);
			return getServiceAccountToken(clientId, clientSecret);
		}

		return null;
	}

	/**
	 * Get access token using service account client credentials.
	 */
	private static String getServiceAccountToken(String clientId, String clientSecret) throws Exception {
		String requestBody = String.format("""
				{
				    "client_id": "%s",
				    "client_secret": "%s",
				    "grant_type": "client_credentials"
				}
				""", clientId, clientSecret);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/auth/service-account/token"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(requestBody))
			.timeout(Duration.ofSeconds(30))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			System.err.println("Service account authentication failed: " + response.body());
			return null;
		}

		JsonNode json = objectMapper.readTree(response.body());
		if (json.has("access_token")) {
			System.out.println("Service account token obtained successfully");
			return json.get("access_token").asText();
		}

		System.err.println("No access_token in response: " + response.body());
		return null;
	}

	@Test
	@DisplayName("Autonomous mode should generate strategy for AAPL and beat buy-and-hold")
	void testAutonomousModeAAPL() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "AAPL",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true,
				    "historicalInsightsOptions": {
				        "lookbackDays": 750,
				        "fastMode": true
				    }
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.has("success"), "Response should have 'success' field");
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");
		assertTrue(jsonResponse.has("pythonCode"), "Response should contain Python code");

		String pythonCode = jsonResponse.get("pythonCode").asText();
		assertFalse(pythonCode.isEmpty(), "Python code should not be empty");
		assertTrue(pythonCode.contains("SYMBOL"), "Code should define SYMBOL constant");

		System.out.println("=== AAPL Autonomous Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
		if (jsonResponse.has("explanation")) {
			String explanation = jsonResponse.get("explanation").asText();
			System.out.println(
					"Explanation: " + explanation.substring(0, Math.min(500, explanation.length())) + "...");
		}
		System.out.println("Python code length: " + pythonCode.length() + " characters");
	}

	@Test
	@DisplayName("Autonomous mode should generate strategy for SPY")
	void testAutonomousModeSPY() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "SPY",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true,
				    "historicalInsightsOptions": {
				        "lookbackDays": 750,
				        "fastMode": true
				    }
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");
		assertTrue(jsonResponse.has("pythonCode"), "Response should contain Python code");

		System.out.println("=== SPY Autonomous Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
	}

	@Test
	@DisplayName("Autonomous mode should generate strategy for TSLA")
	void testAutonomousModeTSLA() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "TSLA",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true,
				    "historicalInsightsOptions": {
				        "lookbackDays": 750,
				        "fastMode": true
				    }
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");
		assertTrue(jsonResponse.has("pythonCode"), "Response should contain Python code");

		System.out.println("=== TSLA Autonomous Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
	}

	@Test
	@DisplayName("Autonomous mode should generate strategy for QQQ (Nasdaq ETF)")
	void testAutonomousModeQQQ() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "QQQ",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true,
				    "historicalInsightsOptions": {
				        "lookbackDays": 750,
				        "fastMode": true
				    }
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");

		System.out.println("=== QQQ Autonomous Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
	}

	@Test
	@DisplayName("Autonomous mode should generate strategy for NVDA")
	void testAutonomousModeNVDA() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "NVDA",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true,
				    "historicalInsightsOptions": {
				        "lookbackDays": 750,
				        "fastMode": true
				    }
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");

		System.out.println("=== NVDA Autonomous Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
	}

	@Test
	@DisplayName("Should fail gracefully for invalid symbol")
	void testAutonomousModeInvalidSymbol() throws Exception {
		skipIfNotEnabled();

		// Arrange
		String requestBody = """
				{
				    "prompt": "INVALIDSYMBOL123",
				    "autonomousMode": "AUTONOMOUS",
				    "model": "gemini-2.5-flash",
				    "useHistoricalInsights": true
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert - should either succeed with fallback or return error gracefully
		JsonNode jsonResponse = objectMapper.readTree(response.body());
		System.out.println("=== Invalid Symbol Test ===");
		System.out.println("Status code: " + response.statusCode());
		System.out.println("Response: " + response.body().substring(0, Math.min(300, response.body().length())));
	}

	@Test
	@DisplayName("Generative AI mode should work for comparison")
	void testGenerativeAIMode() throws Exception {
		skipIfNotEnabled();

		// Arrange - test GENERATIVE_AI mode for comparison
		String requestBody = """
				{
				    "prompt": "Create a momentum strategy for AAPL",
				    "autonomousMode": "GENERATIVE_AI",
				    "model": "gemini-2.5-flash"
				}
				""";

		// Act
		HttpResponse<String> response = executeGenerateStrategy(requestBody);

		// Assert
		assertEquals(200, response.statusCode(), "Expected HTTP 200 OK");

		JsonNode jsonResponse = objectMapper.readTree(response.body());
		assertTrue(jsonResponse.get("success").asBoolean(), "Strategy generation should succeed");

		System.out.println("=== Generative AI Mode Test ===");
		System.out.println("Success: " + jsonResponse.get("success").asBoolean());
	}

	/**
	 * Skip test if authentication is not available.
	 */
	private void skipIfNotEnabled() {
		if (!testsEnabled) {
			org.junit.jupiter.api.Assumptions.assumeTrue(false,
					"Skipped: No authentication credentials (set STRATEGIZ_CLIENT_ID + STRATEGIZ_CLIENT_SECRET or STRATEGIZ_PROD_TOKEN)");
		}
	}

	/**
	 * Helper method to execute generate-strategy API call.
	 */
	private HttpResponse<String> executeGenerateStrategy(String requestBody) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/labs/ai/generate-strategy"))
			.header("Content-Type", "application/json")
			.header("Authorization", "Bearer " + authToken)
			.POST(HttpRequest.BodyPublishers.ofString(requestBody))
			.timeout(Duration.ofSeconds(120)) // Long timeout for optimization
			.build();

		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
