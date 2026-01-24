package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.smsotp.SmsOtpRegistrationService;
import io.strategiz.service.auth.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for SmsOtpRegistrationController.
 * Tests phone number registration and verification flows for SMS OTP.
 */
@ExtendWith(MockitoExtension.class)
class SmsOtpRegistrationControllerTest {

	@Mock
	private SmsOtpRegistrationService smsOtpRegistrationService;

	@Mock
	private CookieUtil cookieUtil;

	@Mock
	private HttpServletResponse httpServletResponse;

	@InjectMocks
	private SmsOtpRegistrationController controller;

	private static final String TEST_USER_ID = "user-123";

	private static final String TEST_PHONE = "+15551234567";

	private static final String TEST_COUNTRY_CODE = "US";

	private static final String TEST_IP_ADDRESS = "192.168.1.1";

	private static final String TEST_OTP_CODE = "123456";

	private static final String TEST_ACCESS_TOKEN = "test-access-token";

	private static final String TEST_REGISTRATION_ID = "sms-123456789";

	@Nested
	@DisplayName("POST /registrations - Register Phone Number")
	class RegisterPhoneNumberTests {

		private SmsOtpRegistrationController.RegistrationRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpRegistrationController.RegistrationRequest(TEST_USER_ID, TEST_PHONE,
					TEST_COUNTRY_CODE);
		}

		@Test
		@DisplayName("Should successfully register phone number and send OTP")
		void registerPhoneNumber_Success() {
			// Given
			when(smsOtpRegistrationService.registerPhoneNumber(eq(TEST_USER_ID), eq(TEST_PHONE), eq(TEST_IP_ADDRESS),
					eq(TEST_COUNTRY_CODE)))
				.thenReturn(true);

			// When
			ResponseEntity<Map<String, Object>> response = controller.registerPhoneNumber(validRequest, TEST_IP_ADDRESS);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_USER_ID, response.getBody().get("userId"));
			assertNotNull(response.getBody().get("registrationId"));
			assertTrue(response.getBody().get("phoneNumber").toString().contains("****"));

			verify(smsOtpRegistrationService).registerPhoneNumber(TEST_USER_ID, TEST_PHONE, TEST_IP_ADDRESS,
					TEST_COUNTRY_CODE);
		}

		@Test
		@DisplayName("Should use default country code when not provided")
		void registerPhoneNumber_DefaultCountryCode() {
			// Given
			SmsOtpRegistrationController.RegistrationRequest requestNoCountry = new SmsOtpRegistrationController.RegistrationRequest(
					TEST_USER_ID, TEST_PHONE, null);
			when(smsOtpRegistrationService.registerPhoneNumber(eq(TEST_USER_ID), eq(TEST_PHONE), anyString(), eq("US")))
				.thenReturn(true);

			// When
			ResponseEntity<Map<String, Object>> response = controller.registerPhoneNumber(requestNoCountry, null);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			verify(smsOtpRegistrationService).registerPhoneNumber(TEST_USER_ID, TEST_PHONE, "127.0.0.1", "US");
		}

		@Test
		@DisplayName("Should throw exception when SMS send fails")
		void registerPhoneNumber_SmsSendFails() {
			// Given
			when(smsOtpRegistrationService.registerPhoneNumber(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(false);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.registerPhoneNumber(validRequest, TEST_IP_ADDRESS));

			assertEquals(AuthErrors.SMS_SEND_FAILED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("PUT /registrations/{registrationId} - Verify Phone Number")
	class VerifyPhoneNumberTests {

		private SmsOtpRegistrationController.VerificationRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpRegistrationController.VerificationRequest(TEST_USER_ID, TEST_ACCESS_TOKEN,
					TEST_PHONE, TEST_OTP_CODE);
		}

		@Test
		@DisplayName("Should successfully verify phone number with OTP")
		void verifyPhoneNumber_Success() {
			// Given
			Map<String, Object> authResult = new HashMap<>();
			authResult.put("accessToken", "new-access-token");
			authResult.put("refreshToken", "new-refresh-token");
			authResult.put("identityToken", "new-identity-token");

			when(smsOtpRegistrationService.verifySmsOtpWithTokenUpdate(eq(TEST_USER_ID), eq(TEST_ACCESS_TOKEN),
					eq(TEST_PHONE), eq(TEST_OTP_CODE)))
				.thenReturn(authResult);

			// When
			ResponseEntity<Map<String, Object>> response = controller.verifyPhoneNumber(TEST_REGISTRATION_ID,
					validRequest);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_REGISTRATION_ID, response.getBody().get("registrationId"));
			assertEquals("new-access-token", response.getBody().get("accessToken"));
			assertEquals("new-refresh-token", response.getBody().get("refreshToken"));
			assertEquals("new-identity-token", response.getBody().get("identityToken"));
		}

		@Test
		@DisplayName("Should throw exception when OTP is invalid or expired")
		void verifyPhoneNumber_InvalidOtp() {
			// Given
			when(smsOtpRegistrationService.verifySmsOtpWithTokenUpdate(anyString(), anyString(), anyString(),
					anyString()))
				.thenReturn(null);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.verifyPhoneNumber(TEST_REGISTRATION_ID, validRequest));

			assertEquals(AuthErrors.OTP_EXPIRED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("POST /registrations/{registrationId}/resend - Resend OTP")
	class ResendOtpTests {

		private SmsOtpRegistrationController.RegistrationRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpRegistrationController.RegistrationRequest(TEST_USER_ID, TEST_PHONE,
					TEST_COUNTRY_CODE);
		}

		@Test
		@DisplayName("Should successfully resend OTP")
		void resendOtp_Success() {
			// Given
			when(smsOtpRegistrationService.resendVerificationOtp(eq(TEST_USER_ID), eq(TEST_PHONE), eq(TEST_IP_ADDRESS),
					eq(TEST_COUNTRY_CODE)))
				.thenReturn(true);

			// When
			ResponseEntity<Map<String, Object>> response = controller.resendVerificationOtp(TEST_REGISTRATION_ID,
					validRequest, TEST_IP_ADDRESS);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_REGISTRATION_ID, response.getBody().get("registrationId"));
		}

		@Test
		@DisplayName("Should throw exception when resend fails")
		void resendOtp_Fails() {
			// Given
			when(smsOtpRegistrationService.resendVerificationOtp(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(false);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.resendVerificationOtp(TEST_REGISTRATION_ID, validRequest, TEST_IP_ADDRESS));

			assertEquals(AuthErrors.SMS_SEND_FAILED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("GET /registrations/{userId} - Get Registration Status")
	class GetRegistrationStatusTests {

		@Test
		@DisplayName("Should return enabled status when user has verified SMS OTP")
		void getStatus_Enabled() {
			// Given
			when(smsOtpRegistrationService.hasVerifiedSmsOtp(TEST_USER_ID)).thenReturn(true);

			// When
			ResponseEntity<Map<String, Object>> response = controller.getRegistrationStatus(TEST_USER_ID);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("enabled"));
			assertEquals(TEST_USER_ID, response.getBody().get("userId"));
			assertEquals("sms_otp", response.getBody().get("registrationType"));
		}

		@Test
		@DisplayName("Should return disabled status when user has no verified SMS OTP")
		void getStatus_Disabled() {
			// Given
			when(smsOtpRegistrationService.hasVerifiedSmsOtp(TEST_USER_ID)).thenReturn(false);

			// When
			ResponseEntity<Map<String, Object>> response = controller.getRegistrationStatus(TEST_USER_ID);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(false, response.getBody().get("enabled"));
		}

	}

	@Nested
	@DisplayName("DELETE /registrations/{userId} - Remove Phone Number")
	class RemovePhoneNumberTests {

		@Test
		@DisplayName("Should successfully remove phone number")
		void removePhoneNumber_Success() {
			// Given
			when(smsOtpRegistrationService.removePhoneNumber(TEST_USER_ID, TEST_PHONE)).thenReturn(true);

			// When
			ResponseEntity<Map<String, Object>> response = controller.removePhoneNumber(TEST_USER_ID, TEST_PHONE);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_USER_ID, response.getBody().get("userId"));
		}

		@Test
		@DisplayName("Should return not found when phone number doesn't exist")
		void removePhoneNumber_NotFound() {
			// Given
			when(smsOtpRegistrationService.removePhoneNumber(TEST_USER_ID, TEST_PHONE)).thenReturn(false);

			// When
			ResponseEntity<Map<String, Object>> response = controller.removePhoneNumber(TEST_USER_ID, TEST_PHONE);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(false, response.getBody().get("success"));
			assertTrue(response.getBody().get("message").toString().contains("not found"));
		}

	}

	@Nested
	@DisplayName("POST /firebase/verify - Firebase Token Verification")
	class FirebaseTokenVerificationTests {

		@Test
		@DisplayName("Should successfully verify Firebase token for registration")
		void verifyFirebaseToken_Registration_Success() {
			// Given
			SmsOtpRegistrationController.FirebaseTokenRequest request = new SmsOtpRegistrationController.FirebaseTokenRequest(
					"firebase-id-token", TEST_USER_ID, TEST_PHONE, true);

			Map<String, Object> authResult = new HashMap<>();
			authResult.put("accessToken", "new-access-token");
			authResult.put("refreshToken", "new-refresh-token");
			authResult.put("userId", TEST_USER_ID);

			when(smsOtpRegistrationService.verifyFirebaseTokenAndComplete("firebase-id-token", TEST_USER_ID, TEST_PHONE,
					true))
				.thenReturn(authResult);

			// When
			ResponseEntity<Map<String, Object>> response = controller.verifyFirebaseToken(request, httpServletResponse);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(true, response.getBody().get("verified"));
			assertEquals("new-access-token", response.getBody().get("accessToken"));
			assertTrue(response.getBody().get("message").toString().contains("registered"));

			// Verify cookies are set
			verify(cookieUtil).setAccessTokenCookie(httpServletResponse, "new-access-token");
			verify(cookieUtil).setRefreshTokenCookie(httpServletResponse, "new-refresh-token");
		}

		@Test
		@DisplayName("Should successfully verify Firebase token for authentication")
		void verifyFirebaseToken_Authentication_Success() {
			// Given
			SmsOtpRegistrationController.FirebaseTokenRequest request = new SmsOtpRegistrationController.FirebaseTokenRequest(
					"firebase-id-token", TEST_USER_ID, TEST_PHONE, false);

			Map<String, Object> authResult = new HashMap<>();
			authResult.put("accessToken", "new-access-token");
			authResult.put("refreshToken", "new-refresh-token");

			when(smsOtpRegistrationService.verifyFirebaseTokenAndComplete("firebase-id-token", TEST_USER_ID, TEST_PHONE,
					false))
				.thenReturn(authResult);

			// When
			ResponseEntity<Map<String, Object>> response = controller.verifyFirebaseToken(request, httpServletResponse);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertTrue(response.getBody().get("message").toString().contains("successful"));

			// Verify cookies are set
			verify(cookieUtil).setAccessTokenCookie(httpServletResponse, "new-access-token");
			verify(cookieUtil).setRefreshTokenCookie(httpServletResponse, "new-refresh-token");
		}

	}

	@Nested
	@DisplayName("Module Name")
	class ModuleNameTests {

		@Test
		@DisplayName("Should return correct module name")
		void getModuleName() {
			assertEquals("service-auth", controller.getModuleName());
		}

	}

}
