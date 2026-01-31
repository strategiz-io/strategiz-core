package io.strategiz.service.auth.controller.totp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.signup.AccountCreationService;
import io.strategiz.service.auth.service.signup.SignupTokenService;
import io.strategiz.service.auth.service.signup.SignupTokenService.SignupTokenClaims;
import io.strategiz.service.auth.service.totp.TotpRegistrationService;
import io.strategiz.service.auth.model.totp.TotpRegistrationRequest;
import io.strategiz.service.auth.model.totp.TotpRegistrationCompletionRequest;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for TOTP registration using resource-based REST endpoints
 *
 * Supports two flows:
 * 1. Signup flow: signup token cookie present → create account atomically with TOTP
 * 2. Existing user flow: userId in request → add TOTP to existing account
 *
 * Endpoints: - POST /auth/totp/registrations - Begin TOTP setup (generate secret) - PUT
 * /auth/totp/registrations/{registrationId} - Complete TOTP setup (verify code) - GET
 * /auth/totp/registrations/{userId} - Get TOTP status for user - DELETE
 * /auth/totp/registrations/{userId} - Disable TOTP for user
 */
@RestController
@RequestMapping("/v1/auth/totp")
public class TotpRegistrationController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private static final Logger log = LoggerFactory.getLogger(TotpRegistrationController.class);

	@Autowired
	private TotpRegistrationService totpRegistrationService;

	@Autowired
	private SignupTokenService signupTokenService;

	@Autowired
	private AccountCreationService accountCreationService;

	@Autowired
	private CookieUtil cookieUtil;

	/**
	 * Begin TOTP registration process - Generate TOTP secret and QR code
	 *
	 * POST /auth/totp/registrations
	 *
	 * For signup flow: userId is resolved from signup token cookie.
	 * For existing users: userId comes from request body.
	 */
	@PostMapping("/registrations")
	public ResponseEntity<Map<String, Object>> beginRegistration(@RequestBody @Valid TotpRegistrationRequest request,
			HttpServletRequest httpRequest) {

		// Resolve userId: signup token cookie takes priority
		String userId = resolveUserId(request.userId(), httpRequest);
		logRequest("beginTotpRegistration", userId);

		var totpResult = totpRegistrationService.generateTotpSecretWithDetails(userId);

		Map<String, Object> response = Map.of("success", true, "secret", totpResult.getSecret(), "qrCodeUri",
				totpResult.getQrCodeUri(), "userId", userId, "registrationId",
				"totp-" + System.currentTimeMillis(), "message", "TOTP setup initialized successfully");

		logRequestSuccess("beginTotpRegistration", userId, response);
		return createCleanResponse(response);
	}

	/**
	 * Complete TOTP registration process - Verify TOTP code and finalize setup
	 *
	 * PUT /auth/totp/registrations/{id}
	 *
	 * For signup flow: creates account atomically, then enables TOTP.
	 */
	@PutMapping("/registrations/{registrationId}")
	public ResponseEntity<Map<String, Object>> completeRegistration(@PathVariable String registrationId,
			@RequestBody @Valid TotpRegistrationCompletionRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {

		// Check for signup flow
		String signupToken = extractCookieValue(httpRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		boolean isSignupFlow = signupToken != null && !signupToken.isBlank();
		SignupTokenClaims signupClaims = null;

		String userId;
		if (isSignupFlow) {
			signupClaims = signupTokenService.validateSignupToken(signupToken);
			userId = signupClaims.userId();
			log.info("Signup flow detected for TOTP completion - creating account for: {}", signupClaims.email());
			accountCreationService.createAccount(signupClaims);
			log.info("Account created for userId: {}", userId);
		}
		else {
			userId = request.userId();
		}

		logRequest("completeTotpRegistration", userId);

		// Resolve access token: prefer request body, fall back to cookie
		String accessToken = request.accessToken();
		if (accessToken == null || accessToken.isBlank()) {
			accessToken = extractCookieValue(httpRequest, "strategiz-access-token");
		}

		// Complete TOTP registration and get updated tokens
		Map<String, Object> authResult = totpRegistrationService.enableTotpWithTokenUpdate(userId, accessToken,
				request.totpCode());

		if (authResult == null) {
			log.warn("TOTP registration failed for user: {}", userId);
			throw new StrategizException(AuthErrors.TOTP_VERIFICATION_FAILED, userId);
		}

		// Create success response with updated tokens
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("registrationId", registrationId);
		response.put("userId", userId);
		response.put("message", "TOTP registration completed successfully");

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

		logRequestSuccess("completeTotpRegistration", userId, response);
		return createCleanResponse(response);
	}

	/**
	 * Get TOTP status for a specific user
	 */
	@GetMapping("/registrations/{userId}")
	public ResponseEntity<Map<String, Object>> getRegistrationStatus(@PathVariable String userId) {
		logRequest("getTotpRegistrationStatus", userId);

		boolean enabled = totpRegistrationService.isTotpSetUp(userId);

		Map<String, Object> result = Map.of("enabled", enabled, "userId", userId, "registrationType", "totp");

		logRequestSuccess("getTotpRegistrationStatus", userId, result);
		return createCleanResponse(result);
	}

	/**
	 * Disable TOTP for a specific user
	 */
	@DeleteMapping("/registrations/{userId}")
	public ResponseEntity<Map<String, Object>> disableRegistration(@PathVariable String userId) {
		logRequest("disableTotpRegistration", userId);

		totpRegistrationService.disableTotp(userId);

		Map<String, Object> result = Map.of("disabled", true, "userId", userId, "message",
				"TOTP registration disabled successfully");

		logRequestSuccess("disableTotpRegistration", userId, result);
		return createCleanResponse(result);
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

}
