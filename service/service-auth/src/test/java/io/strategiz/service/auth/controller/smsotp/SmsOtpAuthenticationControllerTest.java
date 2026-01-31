package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.fraud.FraudDetectionService;
import io.strategiz.service.auth.service.smsotp.SmsOtpAuthenticationService;
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
 * Unit tests for SmsOtpAuthenticationController. Tests SMS OTP sign-in flows for
 * passwordless authentication.
 */
@ExtendWith(MockitoExtension.class)
class SmsOtpAuthenticationControllerTest {

	@Mock
	private SmsOtpAuthenticationService smsOtpAuthenticationService;

	@Mock
	private CookieUtil cookieUtil;

	@Mock
	private FraudDetectionService fraudDetectionService;

	@Mock
	private HttpServletResponse httpServletResponse;

	@InjectMocks
	private SmsOtpAuthenticationController controller;

	private static final String TEST_USER_ID = "user-123";

	private static final String TEST_PHONE = "+15551234567";

	private static final String TEST_COUNTRY_CODE = "US";

	private static final String TEST_IP_ADDRESS = "192.168.1.1";

	private static final String TEST_OTP_CODE = "123456";

	private static final String TEST_SESSION_ID = "session-abc-123";

	private static final String TEST_RECAPTCHA_TOKEN = "recaptcha-token";

	@Nested
	@DisplayName("POST /authentications - Request Authentication OTP (with userId)")
	class RequestAuthenticationOtpTests {

		private SmsOtpAuthenticationController.AuthenticationOtpRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpAuthenticationController.AuthenticationOtpRequest(TEST_USER_ID, TEST_PHONE,
					TEST_COUNTRY_CODE, TEST_RECAPTCHA_TOKEN);
		}

		@Test
		@DisplayName("Should successfully send authentication OTP")
		void requestOtp_Success() {
			// Given
			when(smsOtpAuthenticationService.sendAuthenticationOtp(eq(TEST_USER_ID), eq(TEST_PHONE),
					eq(TEST_COUNTRY_CODE)))
				.thenReturn(TEST_SESSION_ID);

			// When
			ResponseEntity<Map<String, Object>> response = controller.requestAuthenticationOtp(validRequest,
					TEST_IP_ADDRESS);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_SESSION_ID, response.getBody().get("sessionId"));
			assertTrue(response.getBody().get("phoneNumber").toString().contains("****"));

			verify(fraudDetectionService).verifyLogin(TEST_RECAPTCHA_TOKEN, TEST_PHONE);
			verify(smsOtpAuthenticationService).sendAuthenticationOtp(TEST_USER_ID, TEST_PHONE, TEST_COUNTRY_CODE);
		}

		@Test
		@DisplayName("Should use default country code when not provided")
		void requestOtp_DefaultCountryCode() {
			// Given
			SmsOtpAuthenticationController.AuthenticationOtpRequest requestNoCountry = new SmsOtpAuthenticationController.AuthenticationOtpRequest(
					TEST_USER_ID, TEST_PHONE, null, null);
			when(smsOtpAuthenticationService.sendAuthenticationOtp(eq(TEST_USER_ID), eq(TEST_PHONE), eq("US")))
				.thenReturn(TEST_SESSION_ID);

			// When
			ResponseEntity<Map<String, Object>> response = controller.requestAuthenticationOtp(requestNoCountry, null);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			verify(smsOtpAuthenticationService).sendAuthenticationOtp(TEST_USER_ID, TEST_PHONE, "US");
		}

		@Test
		@DisplayName("Should work without fraud detection service")
		void requestOtp_NoFraudDetection() {
			// Given - fraud detection service is null
			SmsOtpAuthenticationController controllerWithoutFraud = new SmsOtpAuthenticationController();

			// Inject mocks manually
			java.lang.reflect.Field serviceField;
			try {
				serviceField = SmsOtpAuthenticationController.class.getDeclaredField("smsOtpAuthenticationService");
				serviceField.setAccessible(true);
				serviceField.set(controllerWithoutFraud, smsOtpAuthenticationService);
			}
			catch (Exception e) {
				fail("Failed to inject mock: " + e.getMessage());
			}

			when(smsOtpAuthenticationService.sendAuthenticationOtp(anyString(), anyString(), anyString()))
				.thenReturn(TEST_SESSION_ID);

			// When
			ResponseEntity<Map<String, Object>> response = controllerWithoutFraud.requestAuthenticationOtp(validRequest,
					TEST_IP_ADDRESS);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
		}

	}

	@Nested
	@DisplayName("POST /authentications/send - Request Authentication by Phone Only")
	class RequestAuthenticationByPhoneTests {

		private SmsOtpAuthenticationController.PhoneAuthenticationRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpAuthenticationController.PhoneAuthenticationRequest(TEST_PHONE, TEST_COUNTRY_CODE,
					TEST_RECAPTCHA_TOKEN);
		}

		@Test
		@DisplayName("Should successfully send OTP using phone number only")
		void requestOtpByPhone_Success() {
			// Given
			when(smsOtpAuthenticationService.sendAuthenticationOtpByPhone(eq(TEST_PHONE), eq(TEST_COUNTRY_CODE)))
				.thenReturn(TEST_SESSION_ID);

			// When
			ResponseEntity<Map<String, Object>> response = controller.requestAuthenticationByPhone(validRequest,
					TEST_IP_ADDRESS);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals(TEST_SESSION_ID, response.getBody().get("sessionId"));

			verify(fraudDetectionService).verifyLogin(TEST_RECAPTCHA_TOKEN, TEST_PHONE);
			verify(smsOtpAuthenticationService).sendAuthenticationOtpByPhone(TEST_PHONE, TEST_COUNTRY_CODE);
		}

		@Test
		@DisplayName("Should throw exception when phone number not registered")
		void requestOtpByPhone_PhoneNotRegistered() {
			// Given
			doThrow(new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, "Phone number not registered"))
				.when(smsOtpAuthenticationService)
				.sendAuthenticationOtpByPhone(anyString(), anyString());

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.requestAuthenticationByPhone(validRequest, TEST_IP_ADDRESS));

			assertEquals(AuthErrors.INVALID_PHONE_NUMBER.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("PUT /authentications/{sessionId} - Verify Authentication OTP")
	class VerifyAuthenticationOtpTests {

		private SmsOtpAuthenticationController.AuthenticationVerifyRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpAuthenticationController.AuthenticationVerifyRequest(TEST_PHONE, TEST_OTP_CODE);
		}

		@Test
		@DisplayName("Should successfully verify OTP and return tokens")
		void verifyOtp_Success() {
			// Given
			Map<String, Object> authResult = new HashMap<>();
			authResult.put("accessToken", "new-access-token");
			authResult.put("refreshToken", "new-refresh-token");
			authResult.put("identityToken", "new-identity-token");
			authResult.put("userId", TEST_USER_ID);

			when(smsOtpAuthenticationService.authenticateWithOtp(eq(TEST_PHONE), eq(TEST_OTP_CODE),
					eq(TEST_SESSION_ID)))
				.thenReturn(authResult);

			// When
			ResponseEntity<Map<String, Object>> response = controller.verifyAuthenticationOtp(TEST_SESSION_ID,
					validRequest, httpServletResponse);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals("new-access-token", response.getBody().get("accessToken"));
			assertEquals("new-refresh-token", response.getBody().get("refreshToken"));
			assertEquals("new-identity-token", response.getBody().get("identityToken"));
			assertEquals(TEST_USER_ID, response.getBody().get("userId"));

			verify(cookieUtil).setAccessTokenCookie(httpServletResponse, "new-access-token");
			verify(cookieUtil).setRefreshTokenCookie(httpServletResponse, "new-refresh-token");
		}

		@Test
		@DisplayName("Should throw exception when OTP is invalid")
		void verifyOtp_InvalidOtp() {
			// Given
			when(smsOtpAuthenticationService.authenticateWithOtp(anyString(), anyString(), anyString()))
				.thenReturn(null);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.verifyAuthenticationOtp(TEST_SESSION_ID, validRequest, httpServletResponse));

			assertEquals(AuthErrors.OTP_EXPIRED.name(), exception.getErrorCode());
		}

		@Test
		@DisplayName("Should not set cookies when verification fails")
		void verifyOtp_NoCookiesOnFailure() {
			// Given
			when(smsOtpAuthenticationService.authenticateWithOtp(anyString(), anyString(), anyString()))
				.thenReturn(null);

			// When & Then
			assertThrows(StrategizException.class,
					() -> controller.verifyAuthenticationOtp(TEST_SESSION_ID, validRequest, httpServletResponse));

			verifyNoInteractions(cookieUtil);
		}

	}

	@Nested
	@DisplayName("POST /authentications/verify - Verify OTP without Session ID")
	class VerifyOtpWithoutSessionTests {

		private SmsOtpAuthenticationController.AuthenticationVerifyRequest validRequest;

		@BeforeEach
		void setUp() {
			validRequest = new SmsOtpAuthenticationController.AuthenticationVerifyRequest(TEST_PHONE, TEST_OTP_CODE);
		}

		@Test
		@DisplayName("Should successfully verify OTP by phone number lookup")
		void verifyOtpWithoutSession_Success() {
			// Given
			Map<String, Object> authResult = new HashMap<>();
			authResult.put("accessToken", "new-access-token");
			authResult.put("refreshToken", "new-refresh-token");
			authResult.put("userId", TEST_USER_ID);

			when(smsOtpAuthenticationService.authenticateWithOtp(eq(TEST_PHONE), eq(TEST_OTP_CODE), isNull()))
				.thenReturn(authResult);

			// When
			ResponseEntity<Map<String, Object>> response = controller
				.verifyAuthenticationOtpWithoutSession(validRequest, httpServletResponse);

			// Then
			assertNotNull(response);
			assertEquals(HttpStatus.OK, response.getStatusCode());
			assertNotNull(response.getBody());
			assertEquals(true, response.getBody().get("success"));
			assertEquals("new-access-token", response.getBody().get("accessToken"));
			assertEquals(TEST_USER_ID, response.getBody().get("userId"));

			// Verify cookies are now set
			verify(cookieUtil).setAccessTokenCookie(httpServletResponse, "new-access-token");
			verify(cookieUtil).setRefreshTokenCookie(httpServletResponse, "new-refresh-token");
		}

		@Test
		@DisplayName("Should throw exception when authentication fails")
		void verifyOtpWithoutSession_Fails() {
			// Given - null return triggers OTP_EXPIRED exception in controller
			when(smsOtpAuthenticationService.authenticateWithOtp(eq(TEST_PHONE), eq(TEST_OTP_CODE), isNull()))
				.thenReturn(null);

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.verifyAuthenticationOtpWithoutSession(validRequest, httpServletResponse));

			assertEquals(AuthErrors.OTP_EXPIRED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("Rate Limiting Tests")
	class RateLimitingTests {

		@Test
		@DisplayName("Should throw exception when rate limited")
		void requestOtp_RateLimited() {
			// Given
			SmsOtpAuthenticationController.AuthenticationOtpRequest request = new SmsOtpAuthenticationController.AuthenticationOtpRequest(
					TEST_USER_ID, TEST_PHONE, TEST_COUNTRY_CODE, null);

			doThrow(new StrategizException(AuthErrors.OTP_RATE_LIMITED,
					"Too many SMS requests. Please wait before requesting another OTP."))
				.when(smsOtpAuthenticationService)
				.sendAuthenticationOtp(anyString(), anyString(), anyString());

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.requestAuthenticationOtp(request, TEST_IP_ADDRESS));

			assertEquals(AuthErrors.OTP_RATE_LIMITED.name(), exception.getErrorCode());
		}

	}

	@Nested
	@DisplayName("Max Attempts Tests")
	class MaxAttemptsTests {

		@Test
		@DisplayName("Should throw exception when max attempts exceeded")
		void verifyOtp_MaxAttemptsExceeded() {
			// Given
			SmsOtpAuthenticationController.AuthenticationVerifyRequest request = new SmsOtpAuthenticationController.AuthenticationVerifyRequest(
					TEST_PHONE, "wrong-code");

			doThrow(new StrategizException(AuthErrors.OTP_MAX_ATTEMPTS_EXCEEDED, "Too many verification attempts"))
				.when(smsOtpAuthenticationService)
				.authenticateWithOtp(anyString(), anyString(), anyString());

			// When & Then
			StrategizException exception = assertThrows(StrategizException.class,
					() -> controller.verifyAuthenticationOtp(TEST_SESSION_ID, request, httpServletResponse));

			assertEquals(AuthErrors.OTP_MAX_ATTEMPTS_EXCEEDED.name(), exception.getErrorCode());
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
