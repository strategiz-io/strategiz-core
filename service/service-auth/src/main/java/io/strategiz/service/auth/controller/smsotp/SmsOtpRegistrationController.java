package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.signup.AccountCreationService;
import io.strategiz.service.auth.service.signup.SignupTokenService;
import io.strategiz.service.auth.service.signup.SignupTokenService.SignupTokenClaims;
import io.strategiz.service.auth.service.smsotp.SmsOtpRegistrationService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for SMS OTP registration (phone number setup)
 *
 * Supports two flows: 1. Signup flow: signup token cookie present → create account
 * atomically with SMS auth 2. Existing user flow: userId in request → add SMS auth to
 * existing account
 */
@RestController
@RequestMapping("/v1/auth/sms-otp")
public class SmsOtpRegistrationController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(SmsOtpRegistrationController.class);

	@Autowired
	private SmsOtpRegistrationService smsOtpRegistrationService;

	@Autowired
	private CookieUtil cookieUtil;

	@Autowired
	private SignupTokenService signupTokenService;

	@Autowired
	private AccountCreationService accountCreationService;

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	/**
	 * Register a phone number and send verification OTP
	 *
	 * POST /auth/sms-otp/registrations
	 */
	@PostMapping("/registrations")
	public ResponseEntity<Map<String, Object>> registerPhoneNumber(@RequestBody @Valid RegistrationRequest request,
			@RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
			HttpServletRequest httpRequest) {

		// Resolve userId from signup token or request
		String userId = resolveUserId(request.userId, httpRequest);
		logRequest("registerPhoneNumber", request.phoneNumber);

		if (ipAddress == null || ipAddress.isEmpty()) {
			ipAddress = "127.0.0.1";
		}

		boolean sent = smsOtpRegistrationService.registerPhoneNumber(userId, request.phoneNumber, ipAddress,
				request.countryCode != null ? request.countryCode : "US");

		if (!sent) {
			throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send verification SMS");
		}

		Map<String, Object> response = Map.of("success", true, "phoneNumber", maskPhoneNumber(request.phoneNumber),
				"userId", userId, "registrationId", "sms-" + System.currentTimeMillis(), "message",
				"Verification code sent to your phone");

		logRequestSuccess("registerPhoneNumber", request.phoneNumber, response);
		return createCleanResponse(response);
	}

	/**
	 * Verify phone number with OTP code
	 *
	 * PUT /auth/sms-otp/registrations/{registrationId}
	 *
	 * For signup flow: creates account atomically, then verifies SMS.
	 */
	@PutMapping("/registrations/{registrationId}")
	public ResponseEntity<Map<String, Object>> verifyPhoneNumber(@PathVariable String registrationId,
			@RequestBody @Valid VerificationRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {

		logRequest("verifyPhoneNumber", request.phoneNumber);

		// Check for signup flow
		String signupToken = extractCookieValue(httpRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		boolean isSignupFlow = signupToken != null && !signupToken.isBlank();
		SignupTokenClaims signupClaims = null;

		String userId;
		if (isSignupFlow) {
			signupClaims = signupTokenService.validateSignupToken(signupToken);
			userId = signupClaims.userId();
			log.info("Signup flow detected for SMS completion - creating account for: {}", signupClaims.email());
			accountCreationService.createAccount(signupClaims);
			log.info("Account created for userId: {}", userId);
		}
		else {
			userId = request.userId;
		}

		// Resolve access token
		String accessToken = request.accessToken;
		if (accessToken == null || accessToken.isBlank()) {
			accessToken = extractCookieValue(httpRequest, "strategiz-access-token");
		}

		Map<String, Object> authResult = smsOtpRegistrationService.verifySmsOtpWithTokenUpdate(userId, accessToken,
				request.phoneNumber, request.otpCode);

		if (authResult == null) {
			log.warn("SMS OTP verification failed for user: {}", userId);
			throw new StrategizException(AuthErrors.OTP_EXPIRED, "Invalid or expired OTP code");
		}

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("registrationId", registrationId);
		response.put("userId", userId);
		response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
		response.put("message", "Phone number verified successfully");

		if (authResult.containsKey("accessToken")) {
			response.put("accessToken", authResult.get("accessToken"));
		}
		if (authResult.containsKey("refreshToken")) {
			response.put("refreshToken", authResult.get("refreshToken"));
		}
		if (authResult.containsKey("identityToken")) {
			response.put("identityToken", authResult.get("identityToken"));
		}

		// For signup flow: set session cookies and clear signup token
		if (isSignupFlow) {
			if (authResult.containsKey("accessToken")) {
				cookieUtil.setAccessTokenCookie(httpResponse, (String) authResult.get("accessToken"));
			}
			if (authResult.containsKey("refreshToken")) {
				cookieUtil.setRefreshTokenCookie(httpResponse, (String) authResult.get("refreshToken"));
			}
			cookieUtil.clearSignupTokenCookie(httpResponse);
			log.info("Signup flow complete - session cookies set, signup token cleared");
		}

		logRequestSuccess("verifyPhoneNumber", request.phoneNumber, response);
		return createCleanResponse(response);
	}

	/**
	 * Resend verification OTP
	 */
	@PostMapping("/registrations/{registrationId}/resend")
	public ResponseEntity<Map<String, Object>> resendVerificationOtp(@PathVariable String registrationId,
			@RequestBody @Valid RegistrationRequest request,
			@RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress,
			HttpServletRequest httpRequest) {

		String userId = resolveUserId(request.userId, httpRequest);
		logRequest("resendVerificationOtp", request.phoneNumber);

		if (ipAddress == null || ipAddress.isEmpty()) {
			ipAddress = "127.0.0.1";
		}

		boolean sent = smsOtpRegistrationService.resendVerificationOtp(userId, request.phoneNumber, ipAddress,
				request.countryCode != null ? request.countryCode : "US");

		if (!sent) {
			throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to resend verification SMS");
		}

		Map<String, Object> response = Map.of("success", true, "registrationId", registrationId, "phoneNumber",
				maskPhoneNumber(request.phoneNumber), "message", "Verification code resent to your phone");

		logRequestSuccess("resendVerificationOtp", request.phoneNumber, response);
		return createCleanResponse(response);
	}

	/**
	 * Get SMS OTP registration status
	 */
	@GetMapping("/registrations/{userId}")
	public ResponseEntity<Map<String, Object>> getRegistrationStatus(@PathVariable String userId) {
		logRequest("getSmsOtpRegistrationStatus", userId);

		boolean hasVerified = smsOtpRegistrationService.hasVerifiedSmsOtp(userId);

		Map<String, Object> result = Map.of("enabled", hasVerified, "userId", userId, "registrationType", "sms_otp");

		logRequestSuccess("getSmsOtpRegistrationStatus", userId, result);
		return createCleanResponse(result);
	}

	/**
	 * Remove phone number registration
	 */
	@DeleteMapping("/registrations/{userId}")
	public ResponseEntity<Map<String, Object>> removePhoneNumber(@PathVariable String userId,
			@RequestParam String phoneNumber) {

		logRequest("removePhoneNumber", userId);

		boolean removed = smsOtpRegistrationService.removePhoneNumber(userId, phoneNumber);

		Map<String, Object> result = Map.of("success", removed, "userId", userId, "phoneNumber",
				maskPhoneNumber(phoneNumber), "message",
				removed ? "Phone number removed successfully" : "Phone number not found");

		logRequestSuccess("removePhoneNumber", userId, result);
		return createCleanResponse(result);
	}

	/**
	 * Verify Firebase phone authentication token
	 */
	@PostMapping("/firebase/verify")
	public ResponseEntity<Map<String, Object>> verifyFirebaseToken(@RequestBody @Valid FirebaseTokenRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

		logRequest("verifyFirebaseToken", request.phoneNumber);

		// Check for signup flow
		String signupToken = extractCookieValue(httpRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		boolean isSignupFlow = signupToken != null && !signupToken.isBlank();

		String userId = request.userId;
		if (isSignupFlow) {
			SignupTokenClaims signupClaims = signupTokenService.validateSignupToken(signupToken);
			userId = signupClaims.userId();
			log.info("Signup flow detected for Firebase SMS - creating account for: {}", signupClaims.email());
			accountCreationService.createAccount(signupClaims);
			log.info("Account created for userId: {}", userId);
		}

		Map<String, Object> authResult = smsOtpRegistrationService.verifyFirebaseTokenAndComplete(
				request.firebaseIdToken, userId, request.phoneNumber, request.isRegistration);

		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("verified", true);
		response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
		response.put("message", request.isRegistration ? "Phone number verified and registered successfully"
				: "Phone authentication successful");

		if (authResult.containsKey("accessToken")) {
			String accessToken = (String) authResult.get("accessToken");
			response.put("accessToken", accessToken);
			cookieUtil.setAccessTokenCookie(httpResponse, accessToken);
		}
		if (authResult.containsKey("refreshToken")) {
			String refreshToken = (String) authResult.get("refreshToken");
			response.put("refreshToken", refreshToken);
			cookieUtil.setRefreshTokenCookie(httpResponse, refreshToken);
		}
		if (authResult.containsKey("identityToken")) {
			response.put("identityToken", authResult.get("identityToken"));
		}
		if (authResult.containsKey("userId")) {
			response.put("userId", authResult.get("userId"));
		}

		if (isSignupFlow) {
			cookieUtil.clearSignupTokenCookie(httpResponse);
			log.info("Signup flow complete - session cookies set, signup token cleared");
		}

		log.info("Authentication cookies set for phone: {}", maskPhoneNumber(request.phoneNumber));
		logRequestSuccess("verifyFirebaseToken", request.phoneNumber, response);
		return createCleanResponse(response);
	}

	/**
	 * Resolve userId from signup token cookie or request body.
	 */
	private String resolveUserId(String requestUserId, HttpServletRequest httpRequest) {
		String signupToken = extractCookieValue(httpRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		if (signupToken != null && !signupToken.isBlank()) {
			SignupTokenClaims claims = signupTokenService.validateSignupToken(signupToken);
			log.info("Resolved userId from signup token: {}", claims.userId());
			return claims.userId();
		}
		return requestUserId;
	}

	public record RegistrationRequest(@NotBlank(message = "User ID is required") String userId,

			@NotBlank(message = "Phone number is required") String phoneNumber,

			String countryCode) {
	}

	public record VerificationRequest(@NotBlank(message = "User ID is required") String userId,

			String accessToken,

			@NotBlank(message = "Phone number is required") String phoneNumber,

			@NotBlank(message = "OTP code is required") String otpCode) {
	}

	public record FirebaseTokenRequest(@NotBlank(message = "Firebase ID token is required") String firebaseIdToken,

			String userId,

			@NotBlank(message = "Phone number is required") String phoneNumber,

			boolean isRegistration) {
	}

	private String extractCookieValue(HttpServletRequest request, String cookieName) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	private String maskPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 4) {
			return "****";
		}
		return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
	}

}
