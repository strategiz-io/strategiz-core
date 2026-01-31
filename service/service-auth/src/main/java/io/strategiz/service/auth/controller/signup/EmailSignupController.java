package io.strategiz.service.auth.controller.signup;

import io.strategiz.service.auth.model.signup.EmailSignupInitiateRequest;
import io.strategiz.service.auth.model.signup.EmailSignupVerifyRequest;
import io.strategiz.service.auth.service.signup.EmailSignupService;
import io.strategiz.service.auth.service.signup.EmailSignupService.EmailVerificationResult;
import io.strategiz.service.auth.service.signup.SignupTokenService;
import io.strategiz.service.auth.service.signup.SignupTokenService.SignupTokenClaims;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for email-based user signup with OTP verification.
 *
 * Flow: 1. POST /initiate - Start signup, sends OTP to email 2. POST /verify - Verify
 * OTP, issue signup token cookie (NO account created) 3. GET /status - Check signup token
 * status (for page refresh recovery)
 *
 * Account creation is deferred to Step 2 (auth method registration).
 */
@RestController
@RequestMapping("/v1/auth/signup/email")
public class EmailSignupController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(EmailSignupController.class);

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	@Autowired
	private EmailSignupService emailSignupService;

	@Autowired
	private SignupTokenService signupTokenService;

	@Autowired
	private CookieUtil cookieUtil;

	/**
	 * Initiate email signup by sending OTP verification code.
	 *
	 * POST /v1/auth/signup/email/initiate
	 * @param request Signup request with name, email
	 * @return Session ID for verification step
	 */
	@PostMapping("/initiate")
	public ResponseEntity<Map<String, Object>> initiateSignup(@Valid @RequestBody EmailSignupInitiateRequest request) {

		logRequest("initiateEmailSignup", request.email());

		String sessionId = emailSignupService.initiateSignup(request);

		Map<String, Object> response = Map.of("success", true, "sessionId", sessionId, "message",
				"Verification code sent to your email", "expiresInMinutes", 10);

		logRequestSuccess("initiateEmailSignup", request.email(), response);
		return createCleanResponse(response);
	}

	/**
	 * Verify OTP and issue signup token cookie. No account is created at this point.
	 *
	 * POST /v1/auth/signup/email/verify
	 * @param request Verification request with email, OTP code, and session ID
	 * @param servletResponse HTTP response for setting cookies
	 * @return Success response with email (no userId, no auth tokens)
	 */
	@PostMapping("/verify")
	public ResponseEntity<Map<String, Object>> verifyAndIssueSignupToken(
			@Valid @RequestBody EmailSignupVerifyRequest request, HttpServletResponse servletResponse) {

		logRequest("verifyEmailSignup", request.email());

		EmailVerificationResult result = emailSignupService.verifyEmailOtp(request.email(), request.otpCode(),
				request.sessionId());

		// Issue signup token and set as HTTP-only cookie
		String signupToken = signupTokenService.issueSignupToken(result.email(), result.name());
		cookieUtil.setSignupTokenCookie(servletResponse, signupToken);

		Map<String, Object> response = Map.of("success", true, "email", result.email(), "message",
				"Email verified successfully. Please complete authentication setup.");

		log.info("Email verified for: {} - signup token cookie set (no account created yet)", result.email());
		logRequestSuccess("verifyEmailSignup", request.email(), response);
		return createCleanResponse(response);
	}

	/**
	 * Check signup token status for page refresh recovery.
	 *
	 * GET /v1/auth/signup/status
	 * @param servletRequest HTTP request for reading cookies
	 * @return Signup status if token is valid
	 */
	@GetMapping("/status")
	public ResponseEntity<Map<String, Object>> getSignupStatus(HttpServletRequest servletRequest) {

		logRequest("getSignupStatus", "cookie-check");

		String signupToken = extractCookieValue(servletRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		if (signupToken == null || signupToken.isBlank()) {
			return createCleanResponse(Map.of("active", false));
		}

		try {
			SignupTokenClaims claims = signupTokenService.validateSignupToken(signupToken);
			Map<String, Object> response = Map.of("active", true, "email", claims.email(), "name", claims.name(),
					"expiresInSeconds", claims.expiresInSeconds());

			logRequestSuccess("getSignupStatus", claims.email(), response);
			return createCleanResponse(response);
		}
		catch (Exception e) {
			log.debug("Signup token invalid or expired: {}", e.getMessage());
			return createCleanResponse(Map.of("active", false));
		}
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
