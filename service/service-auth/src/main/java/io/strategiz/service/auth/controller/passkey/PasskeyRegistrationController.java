package io.strategiz.service.auth.controller.passkey;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationCompletionRequest;
import io.strategiz.service.auth.model.passkey.PasskeyRegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.AuthTokens;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationCompletion;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationResult;
import io.strategiz.service.auth.service.emailotp.EmailOtpAuthenticationService;
import io.strategiz.service.auth.service.signup.AccountCreationService;
import io.strategiz.service.auth.service.signup.SignupTokenService;
import io.strategiz.service.auth.service.signup.SignupTokenService.SignupTokenClaims;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for passkey registration using resource-based REST endpoints
 *
 * This controller handles passkey (WebAuthn) registration operations following REST best
 * practices with plural resource naming and proper HTTP verbs.
 *
 * Supports two flows: 1. Signup flow: signup token cookie present → create account
 * atomically with passkey 2. Existing user flow: identity token or email lookup → add
 * passkey to existing account
 *
 * Endpoints: - POST /auth/passkeys/registrations - Begin registration (create challenge)
 * - PUT /auth/passkeys/registrations/{id} - Complete registration (submit credential)
 */
@RestController
@RequestMapping("/v1/auth/passkeys")
public class PasskeyRegistrationController extends BaseController {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationController.class);

	@Autowired
	private PasskeyRegistrationService registrationService;

	@Autowired
	private EmailOtpAuthenticationService emailOtpAuthenticationService;

	@Autowired
	private SessionAuthBusiness sessionAuthBusiness;

	@Autowired
	private CookieUtil cookieUtil;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SignupTokenService signupTokenService;

	@Autowired
	private AccountCreationService accountCreationService;

	/**
	 * Validate temporary token from Step 1 and extract user ID
	 * @param temporaryToken The token from profile creation
	 * @param expectedEmail The email that should match the token
	 * @return The user ID if validation passes
	 * @throws StrategizException if validation fails
	 */
	private String validateTemporaryToken(String temporaryToken, String expectedEmail) {
		log.info("=== TOKEN VALIDATION START ===");
		log.info("validateTemporaryToken - email: {}", expectedEmail);

		Optional<String> userIdOpt = sessionAuthBusiness.validateSession(temporaryToken);
		if (userIdOpt.isEmpty()) {
			log.error("Token validation failed for email: {} - session not found or invalid", expectedEmail);
			throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, expectedEmail);
		}

		String userId = userIdOpt.get();

		// ENHANCED VALIDATION: Fail early if userId format is wrong
		if (userId != null && !userId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
			log.error("CRITICAL ERROR: userId extracted from token is NOT a valid UUID: [{}]", userId);
			throwModuleException(ServiceAuthErrorDetails.INVALID_TOKEN, "Invalid userId format in token");
		}

		log.info("Temporary token validated successfully for user: {} with email: {}", userId, expectedEmail);
		return userId;
	}

	/**
	 * Get default public key credential parameters (algorithms)
	 */
	private List<Map<String, Object>> getDefaultPubKeyCredParams() {
		List<Map<String, Object>> credParams = new ArrayList<>();
		credParams.add(Map.of("type", "public-key", "alg", -7));
		credParams.add(Map.of("type", "public-key", "alg", -257));
		credParams.add(Map.of("type", "public-key", "alg", -8));
		return credParams;
	}

	/**
	 * Resolve user ID from signup token cookie, identity token, or email lookup. For
	 * signup flow (cookie present), returns the userId from the signup token WITHOUT
	 * creating the account yet (that happens on completion).
	 */
	private ResolvedUser resolveUser(String identityToken, String email,
			jakarta.servlet.http.HttpServletRequest httpRequest) {
		// Check for signup token cookie first (deferred account creation flow)
		String signupToken = extractCookieValue(httpRequest, CookieUtil.SIGNUP_TOKEN_COOKIE);
		if (signupToken != null && !signupToken.isBlank()) {
			SignupTokenClaims claims = signupTokenService.validateSignupToken(signupToken);
			log.info("Signup token found - userId: {}, email: {}", claims.userId(), claims.email());
			return new ResolvedUser(claims.userId(), claims.email(), true, claims);
		}

		// Fall back to identity token or email lookup (existing user flow)
		String userId;
		if (identityToken != null) {
			userId = validateTemporaryToken(identityToken, email);
		}
		else {
			userId = getExistingUserByEmail(email);
		}
		return new ResolvedUser(userId, email, false, null);
	}

	private record ResolvedUser(String userId, String email, boolean isSignupFlow, SignupTokenClaims signupClaims) {
	}

	/**
	 * Begin passkey registration process - Create registration challenge
	 *
	 * POST /auth/passkeys/registrations
	 */
	@PostMapping("/registrations")
	public ResponseEntity<RegistrationChallenge> beginRegistration(@RequestBody PasskeyRegistrationRequest request,
			jakarta.servlet.http.HttpServletRequest httpRequest) {

		log.info("=== PASSKEY REGISTRATION: beginRegistration START ===");
		logRequest("beginRegistration", request.email());

		ResolvedUser resolved = resolveUser(request.identityToken(), request.email(), httpRequest);
		log.info("beginRegistration - resolved userId: [{}], isSignupFlow: {}", resolved.userId(),
				resolved.isSignupFlow());

		RegistrationRequest registrationRequest = new RegistrationRequest(resolved.userId(), request.email());
		RegistrationChallenge challenge = registrationService.beginRegistration(registrationRequest);

		log.info("=== PASSKEY REGISTRATION: beginRegistration END ===");
		logRequestSuccess("beginRegistration", resolved.userId(), challenge);
		return createCleanResponse(challenge);
	}

	/**
	 * Complete passkey registration process - Submit credential data
	 *
	 * PUT /auth/passkeys/registrations/{id}
	 *
	 * For signup flow: creates account atomically, then registers passkey, then issues
	 * session cookies.
	 */
	@PutMapping("/registrations/{registrationId}")
	public ResponseEntity<AuthTokens> completeRegistration(@PathVariable String registrationId,
			@RequestBody PasskeyRegistrationCompletionRequest request,
			jakarta.servlet.http.HttpServletRequest httpRequest,
			jakarta.servlet.http.HttpServletResponse httpResponse) {

		log.info("=== PASSKEY REGISTRATION: completeRegistration START ===");
		logRequest("completeRegistration", request.email());

		ResolvedUser resolved = resolveUser(request.identityToken(), request.email(), httpRequest);
		log.info("completeRegistration - resolved userId: [{}], isSignupFlow: {}", resolved.userId(),
				resolved.isSignupFlow());

		// If this is a signup flow, create the account first
		if (resolved.isSignupFlow()) {
			log.info("Signup flow detected - creating account atomically for: {}", resolved.email());
			accountCreationService.createAccount(resolved.signupClaims());
			log.info("Account created for userId: {}", resolved.userId());
		}

		// Complete passkey registration
		RegistrationCompletion completion = new RegistrationCompletion(resolved.userId(), request.credentialId(),
				request.attestationObject(), request.clientDataJSON(), request.deviceId());

		RegistrationResult result = registrationService.completeRegistration(completion);

		if (!result.success()) {
			log.warn("Passkey registration failed for: {}", request.email());
			throwModuleException(ServiceAuthErrorDetails.PASSKEY_REGISTRATION_FAILED, request.email());
		}

		AuthTokens tokens = (AuthTokens) result.result();

		// Set session cookies
		cookieUtil.setAccessTokenCookie(httpResponse, tokens.accessToken());
		if (tokens.refreshToken() != null) {
			cookieUtil.setRefreshTokenCookie(httpResponse, tokens.refreshToken());
		}

		// Clear signup token cookie if this was a signup flow
		if (resolved.isSignupFlow()) {
			cookieUtil.clearSignupTokenCookie(httpResponse);
			log.info("Signup flow complete - session cookies set, signup token cleared");
		}

		log.info("=== PASSKEY REGISTRATION: completeRegistration END ===");
		logRequestSuccess("completeRegistration", request.email(), tokens);
		return createCleanResponse(tokens);
	}

	/**
	 * Get registration options for manual WebAuthn configuration
	 *
	 * GET /auth/passkeys/registrations/options
	 */
	@GetMapping("/registrations/options")
	public ResponseEntity<Map<String, Object>> getRegistrationOptions(@RequestParam String temporaryToken,
			@RequestParam String email) {

		logRequest("getRegistrationOptions", email);

		String userId = validateTemporaryToken(temporaryToken, email);

		Map<String, Object> options = new HashMap<>();
		options.put("challenge",
				Base64.getEncoder().encodeToString(("challenge-" + System.currentTimeMillis()).getBytes()));
		options.put("rp", Map.of("name", "Strategiz", "id", "strategiz.io"));
		options.put("user", Map.of("id", Base64.getEncoder().encodeToString(userId.getBytes()), "name", email));
		options.put("pubKeyCredParams", getDefaultPubKeyCredParams());
		options.put("timeout", 60000);
		options.put("attestation", "direct");
		options.put("authenticatorSelection",
				Map.of("authenticatorAttachment", "cross-platform", "userVerification", "preferred"));

		logRequestSuccess("getRegistrationOptions", userId, options);
		return createCleanResponse(options);
	}

	/**
	 * Get existing user's UUID by email.
	 */
	private String getExistingUserByEmail(String email) {
		Optional<UserEntity> existingUser = userRepository.getUserByEmail(email);

		if (existingUser.isPresent()) {
			String existingUserId = existingUser.get().getUserId();
			log.info("Found existing user for email: {}, userId: {}", email, existingUserId);
			return existingUserId;
		}

		log.error("User not found for email: {}. Step 1 (profile creation) must be completed first.", email);
		throwModuleException(ServiceAuthErrorDetails.USER_NOT_FOUND, email);
		return null;
	}

	private String extractCookieValue(jakarta.servlet.http.HttpServletRequest request, String cookieName) {
		if (request.getCookies() != null) {
			for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
				if (cookieName.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

}
