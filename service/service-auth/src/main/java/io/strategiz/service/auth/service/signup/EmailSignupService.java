package io.strategiz.service.auth.service.signup;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.base.transaction.FirestoreTransactionTemplate;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.signup.EmailSignupInitiateRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.auth.service.fraud.FraudDetectionService;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling email-based user signup with OTP verification.
 *
 * Flow:
 * 1. User initiates signup with name and email (passwordless)
 * 2. System sends OTP to verify email ownership
 * 3. User verifies OTP to create account
 * 4. User sets up authentication method (Passkey, TOTP, or SMS) in Step 2
 *
 * Admin users can bypass OTP verification when adminBypassEnabled is true.
 */
@Service
public class EmailSignupService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    // Store pending signups until OTP verification
    private final Map<String, PendingSignup> pendingSignups = new ConcurrentHashMap<>();

    @Value("${email.otp.expiration.minutes:10}")
    private int expirationMinutes;

    @Value("${email.otp.code.length:6}")
    private int codeLength;

    @Value("${spring.mail.username:no-reply@strategiz.io}")
    private String fromEmail;

    @Value("${email.otp.admin.bypass.enabled:true}")
    private boolean adminBypassEnabled;

    @Value("${email.signup.admin.emails:}")
    private String adminEmails;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationMethodRepository authenticationMethodRepository;

    @Autowired
    private FirestoreTransactionTemplate transactionTemplate;

    @Autowired(required = false)
    private FraudDetectionService fraudDetectionService;

    @Autowired(required = false)
    private SubscriptionService subscriptionService;

    @Autowired
    private SignupResponseBuilder responseBuilder;

    @Autowired
    private FeatureFlagService featureFlagService;

    /**
     * Initiate email signup by sending OTP verification code.
     *
     * @param request Signup request with name and email (passwordless)
     * @return Session ID to use for verification
     */
    public String initiateSignup(EmailSignupInitiateRequest request) {
        String email = request.email().toLowerCase();
        log.info("Initiating email signup for: {}", email);

        // Check if email OTP signup is enabled
        if (!featureFlagService.isEmailOtpSignupEnabled()) {
            log.warn("Email OTP signup is disabled - rejecting signup for: {}", email);
            throw new StrategizException(AuthErrors.AUTH_METHOD_DISABLED,
                "Email OTP signup is currently disabled");
        }

        // Verify reCAPTCHA token for fraud detection
        if (fraudDetectionService != null) {
            fraudDetectionService.verifySignup(request.recaptchaToken(), email);
        }

        // Check if email already exists
        if (userRepository.getUserByEmail(email).isPresent()) {
            throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already registered");
        }

        // Check for admin bypass
        if (adminBypassEnabled && isAdminEmail(email)) {
            log.info("Admin email detected - bypass enabled for: {}", email);
        }

        // Generate session ID and OTP
        String sessionId = UUID.randomUUID().toString();
        String otpCode = generateOtpCode();
        Instant expiration = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(expirationMinutes));

        // Store pending signup (no password - passwordless flow)
        PendingSignup pending = new PendingSignup(
            request.name(),
            email,
            otpCode,
            expiration
        );
        pendingSignups.put(sessionId, pending);

        // Send OTP email (skip for admin bypass in dev)
        if (!adminBypassEnabled || !isAdminEmail(email)) {
            boolean sent = sendOtpEmail(email, otpCode);
            if (!sent) {
                pendingSignups.remove(sessionId);
                throw new StrategizException(AuthErrors.EMAIL_SEND_FAILED, "Failed to send verification email");
            }
        } else {
            log.info("Admin bypass - OTP for {}: {}", email, otpCode);
        }

        log.info("Email signup initiated - sessionId: {}, email: {}", sessionId, email);
        return sessionId;
    }

    /**
     * Verify OTP and complete signup.
     *
     * @param email User's email
     * @param otpCode OTP code entered by user
     * @param sessionId Session ID from initiation
     * @param deviceId Device ID for token generation
     * @param ipAddress IP address for token generation
     * @return Signup response with user details and tokens
     */
    public OAuthSignupResponse verifyAndCompleteSignup(String email, String otpCode, String sessionId,
                                                       String deviceId, String ipAddress) {
        String normalizedEmail = email.toLowerCase();
        log.info("Verifying email signup for: {}", normalizedEmail);

        // Get pending signup
        PendingSignup pending = pendingSignups.get(sessionId);
        if (pending == null) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, "No pending signup found for this session");
        }

        // Verify email matches
        if (!pending.email().equals(normalizedEmail)) {
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, "Email does not match pending signup");
        }

        // Check expiration
        if (Instant.now().isAfter(pending.expiration())) {
            pendingSignups.remove(sessionId);
            throw new StrategizException(AuthErrors.OTP_EXPIRED, "Verification code has expired");
        }

        // Verify OTP (admin bypass check)
        boolean isAdmin = adminBypassEnabled && isAdminEmail(normalizedEmail);
        if (!isAdmin && !pending.otpCode().equals(otpCode)) {
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, "Invalid verification code");
        }

        if (isAdmin) {
            log.info("Admin bypass - auto-verifying OTP for: {}", normalizedEmail);
        }

        // Create user account
        try {
            UserEntity createdUser = transactionTemplate.execute(transaction -> {
                // Double-check email uniqueness within transaction
                if (userRepository.getUserByEmail(normalizedEmail).isPresent()) {
                    throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "Email address is already registered");
                }

                // Create user entity
                String userId = UUID.randomUUID().toString();
                UserProfileEntity profile = new UserProfileEntity(
                    pending.name(),
                    normalizedEmail,
                    null, // No photo
                    true, // Email verified through OTP
                    "trial",
                    true // Demo mode
                );

                UserEntity user = new UserEntity();
                user.setUserId(userId);
                user.setProfile(profile);

                // Create user
                UserEntity created = userRepository.createUser(user);

                // Create EMAIL_OTP authentication method (passwordless - user will add Passkey/TOTP/SMS in Step 2)
                AuthenticationMethodEntity authMethod = new AuthenticationMethodEntity(
                    AuthenticationMethodType.EMAIL_OTP,
                    "Email: " + normalizedEmail
                );
                authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS, normalizedEmail);
                authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED, true);
                authMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.VERIFICATION_TIME, Instant.now().toString());
                authMethod.setIsActive(true);

                authenticationMethodRepository.saveForUser(created.getUserId(), authMethod);
                log.info("Created EMAIL_OTP authentication method for user: {}", created.getUserId());

                return created;
            });

            // Clean up pending signup
            pendingSignups.remove(sessionId);

            // Initialize trial subscription
            if (subscriptionService != null) {
                try {
                    subscriptionService.initializeTrial(createdUser.getUserId());
                    log.info("Initialized 30-day trial for user: {}", createdUser.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to initialize trial subscription: {}", e.getMessage());
                }
            }

            // Build response with tokens
            List<String> authMethods = List.of("email_otp");
            return responseBuilder.buildSuccessResponse(
                createdUser,
                "Email signup completed successfully",
                authMethods,
                deviceId,
                ipAddress
            );

        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error completing email signup: {}", e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "Failed to complete signup");
        }
    }

    /**
     * Check if email is in the admin bypass list.
     */
    private boolean isAdminEmail(String email) {
        if (adminEmails == null || adminEmails.isBlank()) {
            return false;
        }
        String[] emails = adminEmails.split(",");
        for (String adminEmail : emails) {
            if (adminEmail.trim().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a random OTP code.
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Send OTP email.
     */
    private boolean sendOtpEmail(String email, String otpCode) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. OTP for {}: {}", email, otpCode);
                return true; // Allow in development
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Verify Your Email - Strategiz");
            message.setText(
                "Welcome to Strategiz!\n\n" +
                "Your verification code is: " + otpCode + "\n\n" +
                "Enter this code to complete your registration.\n" +
                "This code expires in " + expirationMinutes + " minutes.\n\n" +
                "If you didn't request this, please ignore this email."
            );

            mailSender.send(message);
            log.info("Sent verification email to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Record for storing pending signup data.
     * No password stored - this is a passwordless signup flow.
     */
    private record PendingSignup(
        String name,
        String email,
        String otpCode,
        Instant expiration
    ) {}
}
