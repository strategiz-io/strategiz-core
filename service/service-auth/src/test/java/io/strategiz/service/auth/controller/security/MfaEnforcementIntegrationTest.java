package io.strategiz.service.auth.controller.security;

import io.strategiz.business.tokenauth.MfaEnforcementBusiness;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.preferences.entity.SecurityPreferences;
import io.strategiz.data.preferences.repository.SecurityPreferencesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MFA enforcement feature.
 *
 * Tests the full stack from controller through business logic to repository, verifying:
 * 1. MFA enforcement settings retrieval 2. Enabling/disabling MFA enforcement 3. Step-up
 * authentication checks 4. Edge cases for method removal
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("MFA Enforcement Integration Tests")
public class MfaEnforcementIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private SecurityPreferencesRepository securityPreferencesRepository;

	@Autowired
	private AuthenticationMethodRepository authMethodRepository;

	@Autowired
	private MfaEnforcementBusiness mfaEnforcementBusiness;

	private String testUserId;

	private String baseUrl;

	@BeforeEach
	void setUp() {
		testUserId = "test-user-" + UUID.randomUUID().toString();
		baseUrl = "http://localhost:" + port + "/v1/auth";
	}

	@AfterEach
	void cleanUp() {
		// Clean up test data
		try {
			// Remove any created auth methods
			List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(testUserId);
			for (AuthenticationMethodEntity method : methods) {
				authMethodRepository.deleteForUser(testUserId, method.getId());
			}
		}
		catch (Exception e) {
			// Ignore cleanup errors
		}
	}

	private AuthenticationMethodEntity createTestMethod(AuthenticationMethodType type) {
		AuthenticationMethodEntity method = new AuthenticationMethodEntity();
		method.setId("method-" + UUID.randomUUID().toString());
		method.setAuthenticationMethod(type);
		method.setIsActive(true);
		method.setName(type.getDisplayName());
		return authMethodRepository.saveForUser(testUserId, method);
	}

	@Nested
	@DisplayName("GET /security - MFA Enforcement Settings")
	class GetSecuritySettingsTests {

		@Test
		@DisplayName("Should return mfaEnforcement in security settings")
		void getSecuritySettings_IncludesMfaEnforcement() {
			// Given - Create a TOTP method for the user
			createTestMethod(AuthenticationMethodType.TOTP);

			// When
			ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/security?userId=" + testUserId,
					Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());

			Map<String, Object> body = response.getBody();
			assertTrue(body.containsKey("mfaEnforcement"), "Response should contain mfaEnforcement");

			Map<String, Object> mfaEnforcement = (Map<String, Object>) body.get("mfaEnforcement");
			assertNotNull(mfaEnforcement);
			assertFalse((Boolean) mfaEnforcement.get("enforced"));
			assertEquals(2, mfaEnforcement.get("minimumAcrLevel"));
			assertTrue((Boolean) mfaEnforcement.get("canEnable"));
			assertEquals("Strong", mfaEnforcement.get("strengthLabel"));
		}

		@Test
		@DisplayName("Should return canEnable=false when no MFA methods")
		void getSecuritySettings_NoMfaMethods_CannotEnable() {
			// Given - No MFA methods for user

			// When
			ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/security?userId=" + testUserId,
					Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			Map<String, Object> mfaEnforcement = (Map<String, Object>) body.get("mfaEnforcement");
			assertFalse((Boolean) mfaEnforcement.get("canEnable"));
		}

		@Test
		@DisplayName("Should calculate ACR 3 for passkey + TOTP")
		void getSecuritySettings_PasskeyAndTotp_AcrThree() {
			// Given
			createTestMethod(AuthenticationMethodType.PASSKEY);
			createTestMethod(AuthenticationMethodType.TOTP);

			// When
			ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/security?userId=" + testUserId,
					Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			Map<String, Object> mfaEnforcement = (Map<String, Object>) body.get("mfaEnforcement");
			assertEquals(3, mfaEnforcement.get("currentAcrLevel"));
			assertEquals("Maximum", mfaEnforcement.get("strengthLabel"));
		}

	}

	@Nested
	@DisplayName("PUT /security/mfa-enforcement - Update Enforcement")
	class UpdateMfaEnforcementTests {

		@Test
		@DisplayName("Should enable MFA enforcement when MFA methods exist")
		void updateMfaEnforcement_Enable_Success() {
			// Given
			createTestMethod(AuthenticationMethodType.TOTP);

			Map<String, Object> request = new HashMap<>();
			request.put("enforced", true);

			// When
			ResponseEntity<Map> response = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(request), Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertTrue((Boolean) body.get("success"));

			Map<String, Object> mfaEnforcement = (Map<String, Object>) body.get("mfaEnforcement");
			assertTrue((Boolean) mfaEnforcement.get("enforced"));
		}

		@Test
		@DisplayName("Should fail to enable MFA enforcement without MFA methods")
		void updateMfaEnforcement_Enable_NoMethods_Fails() {
			// Given - No MFA methods
			Map<String, Object> request = new HashMap<>();
			request.put("enforced", true);

			// When
			ResponseEntity<Map> response = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(request), Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertFalse((Boolean) body.get("success"));
			assertNotNull(body.get("error"));
		}

		@Test
		@DisplayName("Should disable MFA enforcement successfully")
		void updateMfaEnforcement_Disable_Success() {
			// Given - First enable MFA
			createTestMethod(AuthenticationMethodType.TOTP);
			mfaEnforcementBusiness.updateMfaEnforcement(testUserId, true);

			Map<String, Object> request = new HashMap<>();
			request.put("enforced", false);

			// When
			ResponseEntity<Map> response = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(request), Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertTrue((Boolean) body.get("success"));

			Map<String, Object> mfaEnforcement = (Map<String, Object>) body.get("mfaEnforcement");
			assertFalse((Boolean) mfaEnforcement.get("enforced"));
		}

		@Test
		@DisplayName("Should return error for missing enforced field")
		void updateMfaEnforcement_MissingField_BadRequest() {
			// Given
			Map<String, Object> request = new HashMap<>(); // No 'enforced' field

			// When
			ResponseEntity<Map> response = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(request), Map.class);

			// Then
			assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		}

	}

	@Nested
	@DisplayName("GET /security/step-up-check - Step-Up Requirements")
	class StepUpCheckTests {

		@Test
		@DisplayName("Should return not required when MFA not enforced")
		void stepUpCheck_MfaNotEnforced_NotRequired() {
			// Given - User has TOTP but enforcement is off
			createTestMethod(AuthenticationMethodType.TOTP);

			// When
			ResponseEntity<Map> response = restTemplate
				.getForEntity(baseUrl + "/security/step-up-check?userId=" + testUserId + "&currentAcr=1", Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertFalse((Boolean) body.get("required"));
		}

		@Test
		@DisplayName("Should return required when MFA enforced and ACR insufficient")
		void stepUpCheck_MfaEnforced_AcrInsufficient_Required() {
			// Given
			createTestMethod(AuthenticationMethodType.TOTP);
			mfaEnforcementBusiness.updateMfaEnforcement(testUserId, true);

			// When
			ResponseEntity<Map> response = restTemplate
				.getForEntity(baseUrl + "/security/step-up-check?userId=" + testUserId + "&currentAcr=1", Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertTrue((Boolean) body.get("required"));
			assertEquals(2, body.get("minimumAcrLevel"));
			assertNotNull(body.get("availableMethods"));

			List<Map<String, Object>> methods = (List<Map<String, Object>>) body.get("availableMethods");
			assertEquals(1, methods.size());
			assertEquals("totp", methods.get(0).get("type"));
		}

		@Test
		@DisplayName("Should return not required when ACR meets minimum")
		void stepUpCheck_MfaEnforced_AcrSufficient_NotRequired() {
			// Given
			createTestMethod(AuthenticationMethodType.TOTP);
			mfaEnforcementBusiness.updateMfaEnforcement(testUserId, true);

			// When
			ResponseEntity<Map> response = restTemplate
				.getForEntity(baseUrl + "/security/step-up-check?userId=" + testUserId + "&currentAcr=2", Map.class);

			// Then
			assertEquals(HttpStatus.OK, response.getStatusCode());

			Map<String, Object> body = response.getBody();
			assertFalse((Boolean) body.get("required"));
		}

	}

	@Nested
	@DisplayName("MFA Method Removal - Auto-disable Enforcement")
	class MfaMethodRemovalTests {

		@Test
		@DisplayName("Should auto-disable enforcement when last MFA method removed")
		void removeLastMfaMethod_AutoDisablesEnforcement() {
			// Given - User has TOTP and enforcement enabled
			AuthenticationMethodEntity totpMethod = createTestMethod(AuthenticationMethodType.TOTP);
			mfaEnforcementBusiness.updateMfaEnforcement(testUserId, true);

			// Verify enforcement is enabled
			assertTrue(securityPreferencesRepository.isMfaEnforced(testUserId));

			// When - Simulate method removal by calling onMfaMethodRemoved
			authMethodRepository.deleteForUser(testUserId, totpMethod.getId());
			mfaEnforcementBusiness.onMfaMethodRemoved(testUserId);

			// Then - Enforcement should be auto-disabled
			assertFalse(securityPreferencesRepository.isMfaEnforced(testUserId));
		}

		@Test
		@DisplayName("Should keep enforcement when other MFA methods remain")
		void removeMfaMethod_OthersRemain_KeepsEnforcement() {
			// Given - User has TOTP + Passkey and enforcement enabled
			createTestMethod(AuthenticationMethodType.PASSKEY);
			AuthenticationMethodEntity totpMethod = createTestMethod(AuthenticationMethodType.TOTP);
			mfaEnforcementBusiness.updateMfaEnforcement(testUserId, true);

			// When - Remove TOTP (passkey remains)
			authMethodRepository.deleteForUser(testUserId, totpMethod.getId());
			mfaEnforcementBusiness.onMfaMethodRemoved(testUserId);

			// Then - Enforcement should remain active
			assertTrue(securityPreferencesRepository.isMfaEnforced(testUserId));
		}

	}

	@Nested
	@DisplayName("End-to-End Flow Tests")
	class EndToEndFlowTests {

		@Test
		@DisplayName("Full flow: setup TOTP, enable enforcement, check step-up, disable")
		void fullFlow_SetupEnableCheckDisable() {
			// Step 1: Create TOTP method
			createTestMethod(AuthenticationMethodType.TOTP);

			// Step 2: Verify initial state - enforcement not enabled
			ResponseEntity<Map> getResponse1 = restTemplate.getForEntity(baseUrl + "/security?userId=" + testUserId,
					Map.class);
			Map<String, Object> mfa1 = (Map<String, Object>) getResponse1.getBody().get("mfaEnforcement");
			assertFalse((Boolean) mfa1.get("enforced"));
			assertTrue((Boolean) mfa1.get("canEnable"));

			// Step 3: Enable enforcement
			Map<String, Object> enableRequest = new HashMap<>();
			enableRequest.put("enforced", true);
			ResponseEntity<Map> enableResponse = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(enableRequest), Map.class);
			assertTrue((Boolean) enableResponse.getBody().get("success"));

			// Step 4: Verify enforcement is enabled
			ResponseEntity<Map> getResponse2 = restTemplate.getForEntity(baseUrl + "/security?userId=" + testUserId,
					Map.class);
			Map<String, Object> mfa2 = (Map<String, Object>) getResponse2.getBody().get("mfaEnforcement");
			assertTrue((Boolean) mfa2.get("enforced"));

			// Step 5: Check step-up is required with ACR 1
			ResponseEntity<Map> stepUpResponse = restTemplate
				.getForEntity(baseUrl + "/security/step-up-check?userId=" + testUserId + "&currentAcr=1", Map.class);
			assertTrue((Boolean) stepUpResponse.getBody().get("required"));

			// Step 6: Disable enforcement
			Map<String, Object> disableRequest = new HashMap<>();
			disableRequest.put("enforced", false);
			ResponseEntity<Map> disableResponse = restTemplate.exchange(
					baseUrl + "/security/mfa-enforcement?userId=" + testUserId, HttpMethod.PUT,
					new HttpEntity<>(disableRequest), Map.class);
			assertTrue((Boolean) disableResponse.getBody().get("success"));

			// Step 7: Verify step-up no longer required
			ResponseEntity<Map> stepUpResponse2 = restTemplate
				.getForEntity(baseUrl + "/security/step-up-check?userId=" + testUserId + "&currentAcr=1", Map.class);
			assertFalse((Boolean) stepUpResponse2.getBody().get("required"));
		}

	}

}
