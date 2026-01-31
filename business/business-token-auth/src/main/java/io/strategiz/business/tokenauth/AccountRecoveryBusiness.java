package io.strategiz.business.tokenauth;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.RecoveryRequestEntity;
import io.strategiz.data.auth.entity.RecoveryStatus;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.auth.repository.RecoveryRequestRepository;
import io.strategiz.data.preferences.entity.SecurityPreferences;
import io.strategiz.data.preferences.repository.SecurityPreferencesRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.token.issuer.PasetoTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Business logic for account recovery flow.
 *
 * <p>
 * Recovery flow:
 * </p>
 * <ol>
 * <li>User initiates recovery with email</li>
 * <li>System sends email verification code</li>
 * <li>User verifies email code</li>
 * <li>If MFA enabled: System sends SMS to recovery phone</li>
 * <li>User verifies SMS code</li>
 * <li>System issues recovery token</li>
 * <li>User can disable MFA, setup new passkey, etc.</li>
 * </ol>
 *
 * <p>
 * Recovery token properties:
 * </p>
 * <ul>
 * <li>token_type: "recovery"</li>
 * <li>scope: "account:recover"</li>
 * <li>acr: "0" (unauthenticated)</li>
 * <li>validity: 15 minutes</li>
 * </ul>
 */
@Component
public class AccountRecoveryBusiness {

	private static final Logger log = LoggerFactory.getLogger(AccountRecoveryBusiness.class);

	private static final String SYSTEM_USER = "system";

	private static final int MAX_EMAIL_ATTEMPTS = 5;

	private static final int MAX_SMS_ATTEMPTS = 5;

	private static final int CODE_EXPIRY_MINUTES = 10;

	private static final int RATE_LIMIT_HOURS = 24;

	private static final int MAX_REQUESTS_PER_EMAIL = 3;

	@Value("${recovery.code.length:6}")
	private int codeLength = 6;

	@Autowired
	private RecoveryRequestRepository recoveryRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthenticationMethodRepository authMethodRepository;

	@Autowired
	private SecurityPreferencesRepository securityPreferencesRepository;

	@Autowired
	private PasetoTokenIssuer tokenIssuer;

	@Autowired
	private SessionAuthBusiness sessionAuthBusiness;

	/**
	 * Complete recovery and create full authentication session. This should be called
	 * after recovery is COMPLETED to log the user in.
	 * @param recoveryId the recovery request ID
	 * @param ipAddress the client IP address
	 * @param userAgent the client user agent
	 * @return RecoveryAuthResult with session tokens if successful
	 */
	public RecoveryAuthResult completeRecoveryAndAuthenticate(String recoveryId, String ipAddress, String userAgent) {
		log.info("Completing recovery and authenticating for: {}", recoveryId);

		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			log.warn("Recovery request not found: {}", recoveryId);
			return RecoveryAuthResult.notFound();
		}

		RecoveryRequestEntity request = requestOpt.get();

		// Check status - must be COMPLETED
		if (request.getStatus() != RecoveryStatus.COMPLETED) {
			log.warn("Recovery request {} is not completed (status: {})", recoveryId, request.getStatus());
			return RecoveryAuthResult.notCompleted();
		}

		// Check if already used (prevent replay)
		if (Boolean.TRUE.equals(request.getUsedForAuthentication())) {
			log.warn("Recovery request {} has already been used for authentication", recoveryId);
			return RecoveryAuthResult.alreadyUsed();
		}

		// Get user
		Optional<UserEntity> userOpt = userRepository.findById(request.getUserId());
		if (userOpt.isEmpty()) {
			log.error("User not found for recovery: {}", request.getUserId());
			return RecoveryAuthResult.userNotFound();
		}

		UserEntity user = userOpt.get();

		// Create authentication via recovery method
		List<String> authMethods = List.of("recovery", "email");
		if (Boolean.TRUE.equals(request.getSmsVerified())) {
			authMethods = List.of("recovery", "email", "sms");
		}

		String userEmail = user.getProfile().getEmail();
		Boolean demoMode = user.getProfile().getDemoMode();

		SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(user.getId(), userEmail,
				authMethods, false, // not partial auth
				null, // device ID (optional)
				null, // device fingerprint (optional)
				ipAddress, userAgent, demoMode);

		SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);

		// Mark recovery as used
		request.setUsedForAuthentication(true);
		recoveryRequestRepository.update(request, SYSTEM_USER);

		log.info("Recovery authentication completed for user: {}", user.getId());

		return RecoveryAuthResult.success(authResult.accessToken(), authResult.refreshToken(), user.getId(),
				user.getProfile().getEmail(), user.getProfile().getName());
	}

	/**
	 * Initiate account recovery for an email address.
	 * @param email the email address
	 * @param ipAddress the client IP address
	 * @param userAgent the client user agent
	 * @return RecoveryInitiationResult with recovery ID and MFA status
	 */
	public RecoveryInitiationResult initiateRecovery(String email, String ipAddress, String userAgent) {
		log.info("Initiating account recovery for email: {}", maskEmail(email));

		// Rate limiting
		long recentRequests = recoveryRequestRepository.countByEmailInLastHours(email, RATE_LIMIT_HOURS);
		if (recentRequests >= MAX_REQUESTS_PER_EMAIL) {
			log.warn("Rate limit exceeded for email: {}", maskEmail(email));
			return RecoveryInitiationResult.rateLimited();
		}

		// Find user by email
		Optional<UserEntity> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty()) {
			log.info("No user found for email: {} - returning generic response for security", maskEmail(email));
			// Return success to prevent email enumeration, but don't create a request
			return RecoveryInitiationResult.successNoUser();
		}

		UserEntity user = userOpt.get();
		String userId = user.getId();

		// Cancel any existing active recovery requests for this user
		recoveryRequestRepository.cancelAllActiveForUser(userId, SYSTEM_USER);

		// Check if MFA is enabled
		boolean mfaRequired = hasMfaEnabled(userId);
		String phoneNumberHint = null;
		String recoveryPhone = null;

		if (mfaRequired) {
			recoveryPhone = getRecoveryPhoneNumber(userId);
			if (recoveryPhone != null) {
				phoneNumberHint = maskPhoneNumber(recoveryPhone);
			}
		}

		// Create recovery request
		RecoveryRequestEntity request = new RecoveryRequestEntity(userId, email);
		request.setMfaRequired(mfaRequired);
		request.setPhoneNumber(recoveryPhone);
		request.setPhoneNumberHint(phoneNumberHint);
		request.setIpAddress(ipAddress);
		request.setUserAgent(userAgent);

		// Generate email verification code
		String emailCode = generateCode();
		request.setEmailCode(emailCode);
		request.setEmailCodeExpiresAt(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(CODE_EXPIRY_MINUTES)));

		// Save the request
		RecoveryRequestEntity savedRequest = recoveryRequestRepository.save(request, SYSTEM_USER);

		log.info("Created recovery request {} for user {} (MFA required: {})", savedRequest.getId(), userId,
				mfaRequired);

		return RecoveryInitiationResult.success(savedRequest.getId(), mfaRequired, phoneNumberHint, emailCode // In
																												// production,
																												// this
																												// would
																												// be
																												// sent
																												// via
																												// email,
																												// not
																												// returned
		);
	}

	/**
	 * Verify the email code for a recovery request.
	 * @param recoveryId the recovery request ID
	 * @param code the verification code
	 * @return RecoveryStepResult indicating next step or completion
	 */
	public RecoveryStepResult verifyEmailCode(String recoveryId, String code) {
		log.info("Verifying email code for recovery: {}", recoveryId);

		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			log.warn("Recovery request not found: {}", recoveryId);
			return RecoveryStepResult.notFound();
		}

		RecoveryRequestEntity request = requestOpt.get();

		// Check status
		if (request.getStatus() != RecoveryStatus.PENDING_EMAIL) {
			log.warn("Recovery request {} is not in PENDING_EMAIL status", recoveryId);
			return RecoveryStepResult.invalidStatus();
		}

		// Check expiration
		if (request.isExpired()) {
			request.markExpired();
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Recovery request {} has expired", recoveryId);
			return RecoveryStepResult.expired();
		}

		// Check email code expiration
		if (request.isEmailCodeExpired()) {
			log.warn("Email code expired for recovery: {}", recoveryId);
			return RecoveryStepResult.codeExpired();
		}

		// Check attempt limit
		if (request.hasExceededEmailAttempts(MAX_EMAIL_ATTEMPTS)) {
			request.markCancelled();
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Too many email verification attempts for recovery: {}", recoveryId);
			return RecoveryStepResult.tooManyAttempts();
		}

		// Verify code
		request.incrementEmailAttempts();
		if (!code.equals(request.getEmailCode())) {
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Invalid email code for recovery: {}", recoveryId);
			return RecoveryStepResult.invalidCode();
		}

		// Mark email as verified
		request.markEmailVerified();

		// Check if SMS verification is needed
		if (Boolean.TRUE.equals(request.getMfaRequired()) && request.getPhoneNumber() != null) {
			// Generate SMS code
			String smsCode = generateCode();
			request.setSmsCode(smsCode);
			request.setSmsCodeExpiresAt(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(CODE_EXPIRY_MINUTES)));
			request.setStatus(RecoveryStatus.PENDING_SMS);
			recoveryRequestRepository.update(request, SYSTEM_USER);

			log.info("Email verified for recovery {} - SMS verification required", recoveryId);
			return RecoveryStepResult.needsSms(request.getPhoneNumberHint(), smsCode // In
																						// production,
																						// this
																						// would
																						// be
																						// sent
																						// via
																						// SMS,
																						// not
																						// returned
			);
		}

		// No SMS needed - issue recovery token
		String recoveryToken = tokenIssuer.createRecoveryToken(request.getUserId(), recoveryId);
		request.markCompleted();
		recoveryRequestRepository.update(request, SYSTEM_USER);

		log.info("Recovery completed for {} - token issued", recoveryId);
		return RecoveryStepResult.complete(recoveryToken);
	}

	/**
	 * Verify the SMS code for a recovery request.
	 * @param recoveryId the recovery request ID
	 * @param code the verification code
	 * @return RecoveryCompletionResult with recovery token if successful
	 */
	public RecoveryCompletionResult verifySmsCode(String recoveryId, String code) {
		log.info("Verifying SMS code for recovery: {}", recoveryId);

		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			log.warn("Recovery request not found: {}", recoveryId);
			return RecoveryCompletionResult.notFound();
		}

		RecoveryRequestEntity request = requestOpt.get();

		// Check status
		if (request.getStatus() != RecoveryStatus.PENDING_SMS) {
			log.warn("Recovery request {} is not in PENDING_SMS status", recoveryId);
			return RecoveryCompletionResult.invalidStatus();
		}

		// Check expiration
		if (request.isExpired()) {
			request.markExpired();
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Recovery request {} has expired", recoveryId);
			return RecoveryCompletionResult.expired();
		}

		// Check SMS code expiration
		if (request.isSmsCodeExpired()) {
			log.warn("SMS code expired for recovery: {}", recoveryId);
			return RecoveryCompletionResult.codeExpired();
		}

		// Check attempt limit
		if (request.hasExceededSmsAttempts(MAX_SMS_ATTEMPTS)) {
			request.markCancelled();
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Too many SMS verification attempts for recovery: {}", recoveryId);
			return RecoveryCompletionResult.tooManyAttempts();
		}

		// Verify code
		request.incrementSmsAttempts();
		if (!code.equals(request.getSmsCode())) {
			recoveryRequestRepository.update(request, SYSTEM_USER);
			log.warn("Invalid SMS code for recovery: {}", recoveryId);
			return RecoveryCompletionResult.invalidCode();
		}

		// Mark SMS as verified and complete recovery
		request.markSmsVerified();
		String recoveryToken = tokenIssuer.createRecoveryToken(request.getUserId(), recoveryId);
		request.markCompleted();
		recoveryRequestRepository.update(request, SYSTEM_USER);

		log.info("Recovery completed for {} - token issued", recoveryId);
		return RecoveryCompletionResult.success(recoveryToken);
	}

	/**
	 * Get the status of a recovery request.
	 * @param recoveryId the recovery request ID
	 * @return RecoveryStatusResult with current status
	 */
	public RecoveryStatusResult getRecoveryStatus(String recoveryId) {
		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			return RecoveryStatusResult.notFound();
		}

		RecoveryRequestEntity request = requestOpt.get();

		// Check and update expiration
		if (request.isExpired() && request.getStatus() != RecoveryStatus.EXPIRED
				&& request.getStatus() != RecoveryStatus.COMPLETED && request.getStatus() != RecoveryStatus.CANCELLED) {
			request.markExpired();
			recoveryRequestRepository.update(request, SYSTEM_USER);
		}

		return RecoveryStatusResult.success(request.getStatus(), request.getMfaRequired(), request.getEmailVerified(),
				request.getSmsVerified(), request.getPhoneNumberHint());
	}

	/**
	 * Cancel a recovery request.
	 * @param recoveryId the recovery request ID
	 * @return true if cancelled successfully
	 */
	public boolean cancelRecovery(String recoveryId) {
		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			return false;
		}

		RecoveryRequestEntity request = requestOpt.get();
		if (request.getStatus() == RecoveryStatus.COMPLETED || request.getStatus() == RecoveryStatus.CANCELLED) {
			return false;
		}

		request.markCancelled();
		recoveryRequestRepository.update(request, SYSTEM_USER);
		log.info("Recovery request {} cancelled", recoveryId);
		return true;
	}

	/**
	 * Resend the email verification code.
	 * @param recoveryId the recovery request ID
	 * @return the new code (in production, would be sent via email)
	 */
	public Optional<String> resendEmailCode(String recoveryId) {
		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			return Optional.empty();
		}

		RecoveryRequestEntity request = requestOpt.get();
		if (request.getStatus() != RecoveryStatus.PENDING_EMAIL) {
			return Optional.empty();
		}

		if (request.isExpired()) {
			return Optional.empty();
		}

		// Generate new code
		String newCode = generateCode();
		request.setEmailCode(newCode);
		request.setEmailCodeExpiresAt(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(CODE_EXPIRY_MINUTES)));
		request.setEmailAttempts(0); // Reset attempts for new code
		recoveryRequestRepository.update(request, SYSTEM_USER);

		log.info("Resent email code for recovery: {}", recoveryId);
		return Optional.of(newCode);
	}

	/**
	 * Resend the SMS verification code.
	 * @param recoveryId the recovery request ID
	 * @return the new code (in production, would be sent via SMS)
	 */
	public Optional<String> resendSmsCode(String recoveryId) {
		Optional<RecoveryRequestEntity> requestOpt = recoveryRequestRepository.findById(recoveryId);
		if (requestOpt.isEmpty()) {
			return Optional.empty();
		}

		RecoveryRequestEntity request = requestOpt.get();
		if (request.getStatus() != RecoveryStatus.PENDING_SMS) {
			return Optional.empty();
		}

		if (request.isExpired()) {
			return Optional.empty();
		}

		// Generate new code
		String newCode = generateCode();
		request.setSmsCode(newCode);
		request.setSmsCodeExpiresAt(Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(CODE_EXPIRY_MINUTES)));
		request.setSmsAttempts(0); // Reset attempts for new code
		recoveryRequestRepository.update(request, SYSTEM_USER);

		log.info("Resent SMS code for recovery: {}", recoveryId);
		return Optional.of(newCode);
	}

	// Helper methods

	private boolean hasMfaEnabled(String userId) {
		List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(userId);
		boolean hasActiveMfaMethods = methods.stream()
			.filter(m -> Boolean.TRUE.equals(m.getIsActive()))
			.anyMatch(m -> isMfaMethod(m.getAuthenticationMethod()));

		// Also check if user explicitly requires MFA (single toggle applies to recovery
		// too)
		boolean userEnforcesMfa = securityPreferencesRepository.findByUserId(userId)
			.map(SecurityPreferences::getMfaEnforced)
			.orElse(false);

		return hasActiveMfaMethods || userEnforcesMfa;
	}

	private boolean isMfaMethod(AuthenticationMethodType type) {
		return type == AuthenticationMethodType.TOTP || type == AuthenticationMethodType.PASSKEY
				|| type == AuthenticationMethodType.SMS_OTP;
	}

	private String getRecoveryPhoneNumber(String userId) {
		List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserId(userId);

		// First, try to find a verified SMS_OTP method
		for (AuthenticationMethodEntity method : methods) {
			if (method.getAuthenticationMethod() == AuthenticationMethodType.SMS_OTP
					&& Boolean.TRUE.equals(method.getIsActive())) {
				Boolean isVerified = (Boolean) method
					.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
				if (Boolean.TRUE.equals(isVerified)) {
					return method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
				}
			}
		}

		// No SMS_OTP method found with verified phone
		return null;
	}

	private String generateCode() {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(codeLength);
		for (int i = 0; i < codeLength; i++) {
			sb.append(random.nextInt(10));
		}
		return sb.toString();
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return "****";
		}
		String[] parts = email.split("@");
		String local = parts[0];
		String domain = parts[1];
		if (local.length() <= 2) {
			return "**@" + domain;
		}
		return local.substring(0, 2) + "**@" + domain;
	}

	private String maskPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 4) {
			return "****";
		}
		return "***-***-" + phoneNumber.substring(phoneNumber.length() - 4);
	}

	// Result records

	public record RecoveryInitiationResult(boolean success, String recoveryId, boolean mfaRequired,
			String phoneNumberHint, String message, String emailCode // For development
																		// only - remove
																		// in production
	) {
		public static RecoveryInitiationResult success(String recoveryId, boolean mfaRequired, String phoneNumberHint,
				String emailCode) {
			return new RecoveryInitiationResult(true, recoveryId, mfaRequired, phoneNumberHint,
					"Recovery email sent. Please check your inbox.", emailCode);
		}

		public static RecoveryInitiationResult successNoUser() {
			// Same message to prevent email enumeration
			return new RecoveryInitiationResult(true, null, false, null,
					"If this email is registered, you will receive a recovery email.", null);
		}

		public static RecoveryInitiationResult rateLimited() {
			return new RecoveryInitiationResult(false, null, false, null,
					"Too many recovery attempts. Please try again later.", null);
		}
	}

	public record RecoveryStepResult(boolean success, String nextStep, // "SMS" or
																		// "COMPLETE"
			String phoneNumberHint, String recoveryToken, String message, String smsCode // For
																							// development
																							// only
																							// -
																							// remove
																							// in
																							// production
	) {
		public static RecoveryStepResult notFound() {
			return new RecoveryStepResult(false, null, null, null, "Recovery request not found.", null);
		}

		public static RecoveryStepResult invalidStatus() {
			return new RecoveryStepResult(false, null, null, null, "Invalid recovery request status.", null);
		}

		public static RecoveryStepResult expired() {
			return new RecoveryStepResult(false, null, null, null, "Recovery request has expired.", null);
		}

		public static RecoveryStepResult codeExpired() {
			return new RecoveryStepResult(false, null, null, null, "Verification code has expired.", null);
		}

		public static RecoveryStepResult tooManyAttempts() {
			return new RecoveryStepResult(false, null, null, null, "Too many verification attempts.", null);
		}

		public static RecoveryStepResult invalidCode() {
			return new RecoveryStepResult(false, null, null, null, "Invalid verification code.", null);
		}

		public static RecoveryStepResult needsSms(String phoneNumberHint, String smsCode) {
			return new RecoveryStepResult(true, "SMS", phoneNumberHint, null,
					"Email verified. SMS code sent to your recovery phone.", smsCode);
		}

		public static RecoveryStepResult complete(String recoveryToken) {
			return new RecoveryStepResult(true, "COMPLETE", null, recoveryToken,
					"Email verified. Recovery token issued.", null);
		}
	}

	public record RecoveryCompletionResult(boolean success, String recoveryToken, String message) {
		public static RecoveryCompletionResult success(String recoveryToken) {
			return new RecoveryCompletionResult(true, recoveryToken,
					"Recovery completed. You can now reset your account.");
		}

		public static RecoveryCompletionResult notFound() {
			return new RecoveryCompletionResult(false, null, "Recovery request not found.");
		}

		public static RecoveryCompletionResult invalidStatus() {
			return new RecoveryCompletionResult(false, null, "Invalid recovery request status.");
		}

		public static RecoveryCompletionResult expired() {
			return new RecoveryCompletionResult(false, null, "Recovery request has expired.");
		}

		public static RecoveryCompletionResult codeExpired() {
			return new RecoveryCompletionResult(false, null, "Verification code has expired.");
		}

		public static RecoveryCompletionResult tooManyAttempts() {
			return new RecoveryCompletionResult(false, null, "Too many verification attempts.");
		}

		public static RecoveryCompletionResult invalidCode() {
			return new RecoveryCompletionResult(false, null, "Invalid verification code.");
		}
	}

	public record RecoveryStatusResult(boolean found, RecoveryStatus status, Boolean mfaRequired, Boolean emailVerified,
			Boolean smsVerified, String phoneNumberHint) {
		public static RecoveryStatusResult notFound() {
			return new RecoveryStatusResult(false, null, null, null, null, null);
		}

		public static RecoveryStatusResult success(RecoveryStatus status, Boolean mfaRequired, Boolean emailVerified,
				Boolean smsVerified, String phoneNumberHint) {
			return new RecoveryStatusResult(true, status, mfaRequired, emailVerified, smsVerified, phoneNumberHint);
		}
	}

	public record RecoveryAuthResult(boolean success, String accessToken, String refreshToken, String userId,
			String email, String name, String message) {
		public static RecoveryAuthResult success(String accessToken, String refreshToken, String userId, String email,
				String name) {
			return new RecoveryAuthResult(true, accessToken, refreshToken, userId, email, name,
					"Recovery authentication successful.");
		}

		public static RecoveryAuthResult notFound() {
			return new RecoveryAuthResult(false, null, null, null, null, null, "Recovery request not found.");
		}

		public static RecoveryAuthResult notCompleted() {
			return new RecoveryAuthResult(false, null, null, null, null, null, "Recovery verification not completed.");
		}

		public static RecoveryAuthResult alreadyUsed() {
			return new RecoveryAuthResult(false, null, null, null, null, null,
					"Recovery has already been used for authentication.");
		}

		public static RecoveryAuthResult userNotFound() {
			return new RecoveryAuthResult(false, null, null, null, null, null, "User not found.");
		}
	}

}
