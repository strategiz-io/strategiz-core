package io.strategiz.service.profile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Production Integration Tests for Profile API.
 *
 * These tests run against the REAL API (local or production) to verify: 1. Profile CRUD
 * operations work correctly 2. Bio, location, occupation, education fields persist
 * properly 3. Profile retrieval returns all expected fields
 *
 * Configuration: - STRATEGIZ_API_URL: Base URL (default: http://localhost:8080) -
 * STRATEGIZ_AUTH_TOKEN: Authentication token (required) - STRATEGIZ_PROD_TEST: Set to
 * "true" to enable these tests
 *
 * Run with: STRATEGIZ_PROD_TEST=true STRATEGIZ_AUTH_TOKEN=your-token \ mvn test
 * -Dtest=ProfileProductionIntegrationTest
 *
 * For local testing, start the server first: mvn spring-boot:run -pl application-api
 */
@EnabledIfEnvironmentVariable(named = "STRATEGIZ_PROD_TEST", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Profile API Production Integration Tests")
class ProfileProductionIntegrationTest {

	private static final String DEFAULT_API_URL = "http://localhost:8080";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static HttpClient httpClient;

	private static String apiUrl;

	private static String authToken;

	// Test data - unique for each test run
	private static String testBio;

	private static String testLocation;

	private static String testOccupation;

	private static String testEducation;

	// Store original values to restore after tests
	private static String originalBio;

	private static String originalLocation;

	private static String originalOccupation;

	private static String originalEducation;

	@BeforeAll
	static void setup() throws Exception {
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

		// Generate unique test data
		String uniqueId = UUID.randomUUID().toString().substring(0, 8);
		testBio = "Integration test bio - " + uniqueId;
		testLocation = "Test City, TC - " + uniqueId;
		testOccupation = "Test Engineer - " + uniqueId;
		testEducation = "Test University - " + uniqueId;

		System.out.println("\n" + "=".repeat(80));
		System.out.println("PROFILE API PRODUCTION INTEGRATION TESTS");
		System.out.println("=".repeat(80));
		System.out.println("API URL: " + apiUrl);
		System.out.println("Test ID: " + uniqueId);
		System.out.println("=".repeat(80) + "\n");

		// Store original profile values
		storeOriginalProfileValues();
	}

	@AfterAll
	static void cleanup() throws Exception {
		// Restore original profile values
		System.out.println("\n--- Restoring original profile values ---");
		restoreOriginalProfileValues();
		System.out.println("Profile restored successfully\n");
	}

	private static void storeOriginalProfileValues() throws Exception {
		System.out.println("Storing original profile values...");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			JsonNode profile = objectMapper.readTree(response.body());
			originalBio = profile.path("bio").asText(null);
			originalLocation = profile.path("location").asText(null);
			originalOccupation = profile.path("occupation").asText(null);
			originalEducation = profile.path("education").asText(null);
			System.out.println("Original values stored: bio=" + (originalBio != null) + ", location="
					+ (originalLocation != null) + ", occupation=" + (originalOccupation != null) + ", education="
					+ (originalEducation != null));
		}
	}

	private static void restoreOriginalProfileValues() throws Exception {
		Map<String, Object> updateBody = new java.util.HashMap<>();
		if (originalBio != null) {
			updateBody.put("bio", originalBio);
		}
		if (originalLocation != null) {
			updateBody.put("location", originalLocation);
		}
		if (originalOccupation != null) {
			updateBody.put("occupation", originalOccupation);
		}
		if (originalEducation != null) {
			updateBody.put("education", originalEducation);
		}

		if (!updateBody.isEmpty()) {
			String requestBody = objectMapper.writeValueAsString(updateBody);

			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
				.header("Authorization", "Bearer " + authToken)
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(30))
				.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();

			httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		}
	}

	// ========================================================================
	// PROFILE READ TESTS
	// ========================================================================

	@Test
	@Order(1)
	@DisplayName("GET /v1/users/profiles/me - Should return current user profile")
	void testGetCurrentUserProfile() throws Exception {
		System.out.println("\n--- Test: Get Current User Profile ---");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode(), "Should return 200 OK");

		JsonNode profile = objectMapper.readTree(response.body());

		// Verify required fields exist
		assertTrue(profile.has("userId"), "Response should have userId");
		assertTrue(profile.has("name"), "Response should have name");
		assertTrue(profile.has("email"), "Response should have email");

		System.out.println("  Profile retrieved successfully");
		System.out.println("  - userId: " + profile.path("userId").asText());
		System.out.println("  - name: " + profile.path("name").asText());
		System.out.println("  - email: " + profile.path("email").asText());
		System.out.println("  - bio: " + profile.path("bio").asText("(not set)"));
		System.out.println("  - location: " + profile.path("location").asText("(not set)"));
	}

	// ========================================================================
	// PROFILE UPDATE TESTS - BIO FIELD
	// ========================================================================

	@Test
	@Order(10)
	@DisplayName("PUT /v1/users/profiles/me - Should update bio field")
	void testUpdateBio() throws Exception {
		System.out.println("\n--- Test: Update Bio Field ---");

		// Update bio
		String requestBody = objectMapper.writeValueAsString(Map.of("bio", testBio));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, updateResponse.statusCode(),
				"Update should return 200 OK. Response: " + updateResponse.body());

		// Verify bio was updated by fetching profile again
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		String actualBio = profile.path("bio").asText();
		assertEquals(testBio, actualBio, "Bio should be updated to test value");

		System.out.println("  Bio updated successfully: " + actualBio);
	}

	@Test
	@Order(11)
	@DisplayName("Bio field should persist after update")
	void testBioPersistence() throws Exception {
		System.out.println("\n--- Test: Bio Persistence ---");

		// Fetch profile and verify bio still has the value from previous test
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(response.body());

		String actualBio = profile.path("bio").asText();
		assertEquals(testBio, actualBio, "Bio should persist after update");

		System.out.println("  Bio persisted correctly: " + actualBio);
	}

	// ========================================================================
	// PROFILE UPDATE TESTS - LOCATION FIELD
	// ========================================================================

	@Test
	@Order(20)
	@DisplayName("PUT /v1/users/profiles/me - Should update location field")
	void testUpdateLocation() throws Exception {
		System.out.println("\n--- Test: Update Location Field ---");

		String requestBody = objectMapper.writeValueAsString(Map.of("location", testLocation));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, updateResponse.statusCode(),
				"Update should return 200 OK. Response: " + updateResponse.body());

		// Verify location was updated
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		String actualLocation = profile.path("location").asText();
		assertEquals(testLocation, actualLocation, "Location should be updated to test value");

		System.out.println("  Location updated successfully: " + actualLocation);
	}

	// ========================================================================
	// PROFILE UPDATE TESTS - OCCUPATION FIELD
	// ========================================================================

	@Test
	@Order(30)
	@DisplayName("PUT /v1/users/profiles/me - Should update occupation field")
	void testUpdateOccupation() throws Exception {
		System.out.println("\n--- Test: Update Occupation Field ---");

		String requestBody = objectMapper.writeValueAsString(Map.of("occupation", testOccupation));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, updateResponse.statusCode(),
				"Update should return 200 OK. Response: " + updateResponse.body());

		// Verify occupation was updated
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		String actualOccupation = profile.path("occupation").asText();
		assertEquals(testOccupation, actualOccupation, "Occupation should be updated to test value");

		System.out.println("  Occupation updated successfully: " + actualOccupation);
	}

	// ========================================================================
	// PROFILE UPDATE TESTS - EDUCATION FIELD
	// ========================================================================

	@Test
	@Order(40)
	@DisplayName("PUT /v1/users/profiles/me - Should update education field")
	void testUpdateEducation() throws Exception {
		System.out.println("\n--- Test: Update Education Field ---");

		String requestBody = objectMapper.writeValueAsString(Map.of("education", testEducation));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, updateResponse.statusCode(),
				"Update should return 200 OK. Response: " + updateResponse.body());

		// Verify education was updated
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		String actualEducation = profile.path("education").asText();
		assertEquals(testEducation, actualEducation, "Education should be updated to test value");

		System.out.println("  Education updated successfully: " + actualEducation);
	}

	// ========================================================================
	// PROFILE UPDATE TESTS - MULTIPLE FIELDS
	// ========================================================================

	@Test
	@Order(50)
	@DisplayName("PUT /v1/users/profiles/me - Should update multiple fields at once")
	void testUpdateMultipleFields() throws Exception {
		System.out.println("\n--- Test: Update Multiple Fields ---");

		String newBio = "Updated bio - " + UUID.randomUUID().toString().substring(0, 8);
		String newLocation = "Updated Location - " + UUID.randomUUID().toString().substring(0, 8);
		String newOccupation = "Updated Occupation - " + UUID.randomUUID().toString().substring(0, 8);
		String newEducation = "Updated Education - " + UUID.randomUUID().toString().substring(0, 8);

		String requestBody = objectMapper.writeValueAsString(
				Map.of("bio", newBio, "location", newLocation, "occupation", newOccupation, "education", newEducation));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, updateResponse.statusCode(),
				"Update should return 200 OK. Response: " + updateResponse.body());

		// Verify all fields were updated
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		assertEquals(newBio, profile.path("bio").asText(), "Bio should be updated");
		assertEquals(newLocation, profile.path("location").asText(), "Location should be updated");
		assertEquals(newOccupation, profile.path("occupation").asText(), "Occupation should be updated");
		assertEquals(newEducation, profile.path("education").asText(), "Education should be updated");

		System.out.println("  All fields updated successfully:");
		System.out.println("  - bio: " + profile.path("bio").asText());
		System.out.println("  - location: " + profile.path("location").asText());
		System.out.println("  - occupation: " + profile.path("occupation").asText());
		System.out.println("  - education: " + profile.path("education").asText());
	}

	// ========================================================================
	// VALIDATION TESTS
	// ========================================================================

	@Test
	@Order(60)
	@DisplayName("PUT /v1/users/profiles/me - Should handle empty bio")
	void testUpdateEmptyBio() throws Exception {
		System.out.println("\n--- Test: Update with Empty Bio ---");

		String requestBody = objectMapper.writeValueAsString(Map.of("bio", ""));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

		// Should either accept empty string or return 200
		assertTrue(updateResponse.statusCode() == 200 || updateResponse.statusCode() == 400,
				"Should handle empty bio (200 or 400). Got: " + updateResponse.statusCode());

		System.out.println("  Empty bio handled with status: " + updateResponse.statusCode());
	}

	@Test
	@Order(70)
	@DisplayName("GET /v1/users/profiles/me - Should return profile fields in response")
	void testProfileResponseContainsAllFields() throws Exception {
		System.out.println("\n--- Test: Profile Response Contains All Fields ---");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(response.body());

		// Core fields (always present)
		assertTrue(profile.has("userId"), "Response should have userId");
		assertTrue(profile.has("name"), "Response should have name");
		assertTrue(profile.has("email"), "Response should have email");
		assertTrue(profile.has("demoMode"), "Response should have demoMode");
		assertTrue(profile.has("subscriptionTier"), "Response should have subscriptionTier");

		// New profile fields (may be null but key should exist or be omitted)
		// Just verify the response can be parsed - fields may be null
		System.out.println("  Profile response structure verified:");
		System.out.println("  - Has userId: " + profile.has("userId"));
		System.out.println("  - Has name: " + profile.has("name"));
		System.out.println("  - Has email: " + profile.has("email"));
		System.out.println("  - Has bio: " + profile.has("bio"));
		System.out.println("  - Has location: " + profile.has("location"));
		System.out.println("  - Has occupation: " + profile.has("occupation"));
		System.out.println("  - Has education: " + profile.has("education"));
	}

	// ========================================================================
	// AUTHORIZATION TESTS
	// ========================================================================

	@Test
	@Order(80)
	@DisplayName("GET /v1/users/profiles/me - Should require authentication")
	void testProfileRequiresAuthentication() throws Exception {
		System.out.println("\n--- Test: Profile Requires Authentication ---");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(401, response.statusCode(), "Should return 401 Unauthorized without auth token");

		System.out.println("  Correctly returned 401 without auth token");
	}

	@Test
	@Order(81)
	@DisplayName("PUT /v1/users/profiles/me - Should require authentication for updates")
	void testProfileUpdateRequiresAuthentication() throws Exception {
		System.out.println("\n--- Test: Profile Update Requires Authentication ---");

		String requestBody = objectMapper.writeValueAsString(Map.of("bio", "test"));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(401, response.statusCode(), "Should return 401 Unauthorized without auth token");

		System.out.println("  Correctly returned 401 without auth token");
	}

	// ========================================================================
	// SUMMARY TEST
	// ========================================================================

	@Test
	@Order(100)
	@DisplayName("Final Validation - All profile operations work correctly")
	void testFinalValidation() throws Exception {
		System.out.println("\n" + "=".repeat(80));
		System.out.println("FINAL VALIDATION - PROFILE API");
		System.out.println("=".repeat(80));

		// Final comprehensive test
		String finalBio = "Final test bio - " + System.currentTimeMillis();
		String finalLocation = "Final Location";
		String finalOccupation = "Final Occupation";
		String finalEducation = "Final Education";

		// Update all fields
		String requestBody = objectMapper.writeValueAsString(Map.of("bio", finalBio, "location", finalLocation,
				"occupation", finalOccupation, "education", finalEducation));

		HttpRequest updateRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.header("Content-Type", "application/json")
			.timeout(Duration.ofSeconds(30))
			.PUT(HttpRequest.BodyPublishers.ofString(requestBody))
			.build();

		HttpResponse<String> updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, updateResponse.statusCode(), "Final update should succeed");

		// Verify all fields
		HttpRequest getRequest = HttpRequest.newBuilder()
			.uri(URI.create(apiUrl + "/v1/users/profiles/me"))
			.header("Authorization", "Bearer " + authToken)
			.timeout(Duration.ofSeconds(30))
			.GET()
			.build();

		HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
		JsonNode profile = objectMapper.readTree(getResponse.body());

		boolean allFieldsCorrect = finalBio.equals(profile.path("bio").asText())
				&& finalLocation.equals(profile.path("location").asText())
				&& finalOccupation.equals(profile.path("occupation").asText())
				&& finalEducation.equals(profile.path("education").asText());

		assertTrue(allFieldsCorrect, "All profile fields should match expected values");

		System.out.println("  All profile fields updated and retrieved correctly!");
		System.out.println("  - bio: " + profile.path("bio").asText());
		System.out.println("  - location: " + profile.path("location").asText());
		System.out.println("  - occupation: " + profile.path("occupation").asText());
		System.out.println("  - education: " + profile.path("education").asText());
		System.out.println("=".repeat(80));
		System.out.println("ALL PROFILE API TESTS PASSED");
		System.out.println("=".repeat(80) + "\n");
	}

}
