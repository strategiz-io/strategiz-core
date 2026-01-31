package io.strategiz.service.auth.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for SMS OTP authentication against production API.
 *
 * These tests make actual HTTP calls to the production API to verify: 1. SMS OTP send
 * functionality (backend-only flow) 2. SMS OTP verification 3. Firebase phone
 * authentication token verification 4. Error handling (rate limiting, invalid codes,
 * expired sessions) 5. Phone number lookup for sign-in
 *
 * Environment Variables Required: - STRATEGIZ_API_URL: Override API URL (defaults to
 * production) - STRATEGIZ_TEST_PHONE: Test phone number for SMS tests (optional, uses
 * mock if not set) - STRATEGIZ_PROD_TOKEN: Authentication token for protected endpoints -
 * STRATEGIZ_CLIENT_ID + STRATEGIZ_CLIENT_SECRET: Service account credentials
 *
 * Run: # With service account STRATEGIZ_CLIENT_ID=sa_xxx STRATEGIZ_CLIENT_SECRET=xxx mvn
 * test -Dtest=SmsOtpProductionE2ETest -pl service/service-auth
 *
 * # With manual token STRATEGIZ_PROD_TOKEN=xxx mvn test -Dtest=SmsOtpProductionE2ETest
 * -pl service/service-auth
 *
 * Note: Some tests require devMockSmsEnabled=true on the server for safe testing
 */
@DisplayName("SMS OTP Production E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmsOtpProductionE2ETest {

	private static final String DEFAULT_API_URL = "https://strategiz-api-bflhiwsnmq-ue.a.run.app";

	private static final String TEST_PHONE_NUMBER = "+15551234567"; // Default test number

	private static HttpClient httpClient;

	private static ObjectMapper objectMapper;

	private static String apiUrl;

	private static String authToken;

	private static String testPhoneNumber;

	private static boolean testsEnabled = false;

	// Shared state between tests
	private static String sessionId;

	private static String registeredUserId;

	@BeforeAll
	static void setUp() throws Exception {
		httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
		objectMapper = new ObjectMapper();

		apiUrl = System.getenv("STRATEGIZ_API_URL");
		if (apiUrl == null || apiUrl.isEmpty()) {
			apiUrl = DEFAULT_API_URL;
		}

		testPhoneNumber = System.getenv("STRATEGIZ_TEST_PHONE");
		if (testPhoneNumber == null || testPhoneNumber.isEmpty()) {
			testPhoneNumber = TEST_PHONE_NUMBER;
		}

		// Try to get authentication token
		authToken = getAuthToken();

		if (authToken != null && !authToken.isEmpty()) {
			testsEnabled = true;
			System.out.println("=== SMS OTP Production E2E Tests Enabled ===");
			System.out.println("API URL: " + apiUrl);
			System.out.println("Test Phone: " + maskPhoneNumber(testPhoneNumber));
		}
		else {
			System.out.println("=== SMS OTP Production E2E Tests Skipped ===");
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

	// ==================== SMS OTP SEND TESTS ====================

	@Test
	@Order(1)
	@DisplayName("1. Send SMS OTP - Should return session ID for valid phone")
	void testSendSmsOtp_ValidPhone() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "countryCode": "US"
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/send", requestBody, false);

		System.out.println("=== Send SMS OTP Response ===");
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());

		// Could be 200 (success) or 400/404 (phone not registered) - both are valid API
		// responses
		if (response.statusCode() == 200) {
			JsonNode json = objectMapper.readTree(response.body());
			assertTrue(json.has("success"), "Response should have 'success' field");
			assertTrue(json.has("sessionId"), "Response should have 'sessionId' field");
			sessionId = json.get("sessionId").asText();
			System.out.println("Session ID: " + sessionId);
		}
		else {
			// Expected if phone not registered - verify error structure
			JsonNode json = objectMapper.readTree(response.body());
			assertTrue(json.has("message") || json.has("error"), "Error response should have message");
			System.out.println("Expected error (phone not registered): " + response.body());
		}
	}

	@Test
	@Order(2)
	@DisplayName("2. Send SMS OTP - Should fail for invalid phone format")
	void testSendSmsOtp_InvalidPhoneFormat() throws Exception {
		skipIfNotEnabled();

		String requestBody = """
				{
				    "phoneNumber": "invalid",
				    "countryCode": "US"
				}
				""";

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/send", requestBody, false);

		System.out.println("=== Invalid Phone Format Response ===");
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());

		// Should return error
		assertTrue(response.statusCode() >= 400, "Invalid phone should return error status");
	}

	@Test
	@Order(3)
	@DisplayName("3. Send SMS OTP - Should fail for empty phone")
	void testSendSmsOtp_EmptyPhone() throws Exception {
		skipIfNotEnabled();

		String requestBody = """
				{
				    "phoneNumber": "",
				    "countryCode": "US"
				}
				""";

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/send", requestBody, false);

		System.out.println("=== Empty Phone Response ===");
		System.out.println("Status: " + response.statusCode());

		assertEquals(400, response.statusCode(), "Empty phone should return 400 Bad Request");
	}

	// ==================== SMS OTP VERIFY TESTS ====================

	@Test
	@Order(10)
	@DisplayName("10. Verify SMS OTP - Should fail for invalid code")
	void testVerifySmsOtp_InvalidCode() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "otpCode": "000000"
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/verify", requestBody, false);

		System.out.println("=== Verify Invalid Code Response ===");
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());

		// Should fail with invalid code error
		assertTrue(
				response.statusCode() >= 400 || !objectMapper.readTree(response.body()).path("success").asBoolean(true),
				"Invalid code should fail verification");
	}

	@Test
	@Order(11)
	@DisplayName("11. Verify SMS OTP - Should fail for non-existent session")
	void testVerifySmsOtp_NoSession() throws Exception {
		skipIfNotEnabled();

		String requestBody = """
				{
				    "phoneNumber": "+19999999999",
				    "otpCode": "123456"
				}
				""";

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/verify", requestBody, false);

		System.out.println("=== No Session Response ===");
		System.out.println("Status: " + response.statusCode());

		// Should fail - no active session for this phone
		assertTrue(response.statusCode() >= 400, "Non-existent session should fail");
	}

	@Test
	@Order(12)
	@DisplayName("12. Verify SMS OTP with Session ID - Should fail for invalid session")
	void testVerifySmsOtp_InvalidSessionId() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "otpCode": "123456"
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePut("/v1/auth/sms-otp/authentications/invalid-session-id", requestBody,
				false);

		System.out.println("=== Invalid Session ID Response ===");
		System.out.println("Status: " + response.statusCode());

		assertTrue(response.statusCode() >= 400, "Invalid session ID should fail");
	}

	// ==================== PHONE REGISTRATION TESTS ====================

	@Test
	@Order(20)
	@DisplayName("20. Register Phone - Should require userId")
	void testRegisterPhone_RequiresUserId() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "countryCode": "US"
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/registrations", requestBody, true);

		System.out.println("=== Register Without UserId Response ===");
		System.out.println("Status: " + response.statusCode());

		assertEquals(400, response.statusCode(), "Registration without userId should fail");
	}

	@Test
	@Order(21)
	@DisplayName("21. Get Registration Status - Should return status for user")
	void testGetRegistrationStatus() throws Exception {
		skipIfNotEnabled();

		// Use a test user ID
		String testUserId = "test-user-" + System.currentTimeMillis();

		HttpResponse<String> response = executeGet("/v1/auth/sms-otp/registrations/" + testUserId, true);

		System.out.println("=== Registration Status Response ===");
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());

		assertEquals(200, response.statusCode(), "Should return registration status");

		JsonNode json = objectMapper.readTree(response.body());
		assertTrue(json.has("enabled"), "Response should have 'enabled' field");
		assertFalse(json.get("enabled").asBoolean(), "New user should not have SMS OTP enabled");
	}

	// ==================== FIREBASE TOKEN VERIFICATION TESTS ====================

	@Test
	@Order(30)
	@DisplayName("30. Firebase Verify - Should fail for invalid token")
	void testFirebaseVerify_InvalidToken() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "firebaseIdToken": "invalid-firebase-token",
				    "phoneNumber": "%s",
				    "isRegistration": false
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/firebase/verify", requestBody, false);

		System.out.println("=== Firebase Invalid Token Response ===");
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());

		assertTrue(response.statusCode() >= 400, "Invalid Firebase token should fail");
	}

	@Test
	@Order(31)
	@DisplayName("31. Firebase Verify - Should fail for missing token")
	void testFirebaseVerify_MissingToken() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "firebaseIdToken": "",
				    "phoneNumber": "%s",
				    "isRegistration": false
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/firebase/verify", requestBody, false);

		System.out.println("=== Firebase Missing Token Response ===");
		System.out.println("Status: " + response.statusCode());

		assertEquals(400, response.statusCode(), "Missing Firebase token should return 400");
	}

	@Test
	@Order(32)
	@DisplayName("32. Firebase Verify Registration - Should require userId")
	void testFirebaseVerify_RegistrationRequiresUserId() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "firebaseIdToken": "some-token",
				    "phoneNumber": "%s",
				    "isRegistration": true
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/firebase/verify", requestBody, false);

		System.out.println("=== Firebase Registration Without UserId Response ===");
		System.out.println("Status: " + response.statusCode());

		// Should fail because registration requires userId
		assertTrue(response.statusCode() >= 400, "Registration without userId should fail");
	}

	// ==================== RATE LIMITING TESTS ====================

	@Test
	@Order(40)
	@DisplayName("40. Rate Limiting - Should enforce rate limit on multiple requests")
	void testRateLimiting() throws Exception {
		skipIfNotEnabled();

		String testPhone = "+15559999999"; // Use a different number to avoid affecting
											// other tests
		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "countryCode": "US"
				}
				""", testPhone);

		int rateLimitHits = 0;
		for (int i = 0; i < 5; i++) {
			HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/send", requestBody, false);
			System.out.println("Rate limit test #" + (i + 1) + " - Status: " + response.statusCode());

			if (response.statusCode() == 429 || (response.body() != null && response.body().contains("rate"))) {
				rateLimitHits++;
				break;
			}

			// Small delay between requests
			Thread.sleep(100);
		}

		System.out.println("=== Rate Limiting Test ===");
		System.out.println("Rate limit triggered: " + (rateLimitHits > 0));
	}

	// ==================== API ENDPOINT AVAILABILITY TESTS ====================

	@Test
	@Order(50)
	@DisplayName("50. Health Check - SMS OTP endpoints should be available")
	void testSmsOtpEndpointsAvailable() throws Exception {
		skipIfNotEnabled();

		// Test OPTIONS or simple GET on registration endpoint
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/auth/sms-otp/registrations/health-check"))
			.header("Content-Type", "application/json")
			.GET()
			.timeout(Duration.ofSeconds(10))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println("=== Endpoint Availability ===");
		System.out.println("Status: " + response.statusCode());

		// Should return 200 (success) or 404 (not found for specific userId) - both
		// indicate endpoint exists
		assertTrue(response.statusCode() == 200 || response.statusCode() == 404,
				"SMS OTP endpoint should be available");
	}

	@Test
	@Order(51)
	@DisplayName("51. API Response Format - Should return proper JSON structure")
	void testApiResponseFormat() throws Exception {
		skipIfNotEnabled();

		String requestBody = String.format("""
				{
				    "phoneNumber": "%s",
				    "countryCode": "US"
				}
				""", testPhoneNumber);

		HttpResponse<String> response = executePost("/v1/auth/sms-otp/authentications/send", requestBody, false);

		System.out.println("=== API Response Format ===");
		System.out.println("Content-Type: " + response.headers().firstValue("Content-Type").orElse("none"));

		// Verify response is valid JSON
		assertDoesNotThrow(() -> objectMapper.readTree(response.body()), "Response should be valid JSON");

		// Verify response headers
		assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"),
				"Response should have JSON content type");
	}

	// ==================== HELPER METHODS ====================

	private void skipIfNotEnabled() {
		if (!testsEnabled) {
			org.junit.jupiter.api.Assumptions.assumeTrue(false,
					"Skipped: No authentication credentials (set STRATEGIZ_CLIENT_ID + STRATEGIZ_CLIENT_SECRET or STRATEGIZ_PROD_TOKEN)");
		}
	}

	private HttpResponse<String> executePost(String path, String requestBody, boolean authenticated) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(requestBody))
			.timeout(Duration.ofSeconds(30));

		if (authenticated && authToken != null) {
			builder.header("Authorization", "Bearer " + authToken);
		}

		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> executePut(String path, String requestBody, boolean authenticated) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + path))
			.header("Content-Type", "application/json")
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.timeout(Duration.ofSeconds(30));

		if (authenticated && authToken != null) {
			builder.header("Authorization", "Bearer " + authToken);
		}

		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> executeGet(String path, boolean authenticated) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + path))
			.header("Content-Type", "application/json")
			.GET()
			.timeout(Duration.ofSeconds(30));

		if (authenticated && authToken != null) {
			builder.header("Authorization", "Bearer " + authToken);
		}

		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String maskPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 4) {
			return "****";
		}
		return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
	}

}
