package io.strategiz.service.auth.service.signup;

import io.strategiz.business.preferences.service.SubscriptionService;
import io.strategiz.client.sendgrid.EmailProvider;
import io.strategiz.client.sendgrid.model.EmailDeliveryResult;
import io.strategiz.client.sendgrid.model.EmailMessage;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.base.constants.SubscriptionTierConstants;
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

    @Value("${email.from:no-reply@strategiz.io}")
    private String fromEmail;

    @Value("${email.otp.admin.bypass.enabled:true}")
    private boolean adminBypassEnabled;

    @Value("${email.signup.admin.emails:}")
    private String adminEmails;

    @Autowired(required = false)
    private EmailProvider emailProvider;

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

    @Autowired
    private EmailReservationService emailReservationService;

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

        // Check for admin bypass
        if (adminBypassEnabled && isAdminEmail(email)) {
            log.info("Admin email detected - bypass enabled for: {}", email);
        }

        // Generate session ID and OTP
        String sessionId = UUID.randomUUID().toString();
        String otpCode = generateOtpCode();
        Instant expiration = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(expirationMinutes));

        // Reserve email and get pre-generated userId
        // This throws EMAIL_ALREADY_EXISTS if email is taken or reserved
        String userId = emailReservationService.reserveEmail(email, "email_otp", sessionId);
        log.info("Email reserved for signup - email: {}, userId: {}, sessionId: {}", email, userId, sessionId);

        // Store pending signup with pre-generated userId (no password - passwordless flow)
        PendingSignup pending = new PendingSignup(
            request.name(),
            email,
            otpCode,
            expiration,
            userId
        );
        pendingSignups.put(sessionId, pending);

        // Send OTP email (skip for admin bypass in dev)
        if (!adminBypassEnabled || !isAdminEmail(email)) {
            boolean sent = sendOtpEmail(email, otpCode);
            if (!sent) {
                pendingSignups.remove(sessionId);
                // Release the email reservation since signup failed
                emailReservationService.releaseReservation(email);
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
                // Confirm the email reservation within transaction
                // This ensures atomicity with user creation
                emailReservationService.confirmReservation(normalizedEmail);

                // Use pre-generated userId from reservation
                String userId = pending.userId();
                UserProfileEntity profile = new UserProfileEntity(
                    pending.name(),
                    normalizedEmail,
                    null, // No photo
                    true, // Email verified through OTP
                    SubscriptionTierConstants.DEFAULT, // Explorer (free) tier
                    true // Demo mode
                );

                // Set ADMIN role for admin emails
                if (isAdminEmail(normalizedEmail)) {
                    profile.setRole("ADMIN");
                    log.info("Setting ADMIN role for admin email: {}", normalizedEmail);
                }

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
            // Release reservation on failure (only for non-email-exists errors)
            if (e.getErrorDetails() != AuthErrors.EMAIL_ALREADY_EXISTS) {
                emailReservationService.releaseReservation(normalizedEmail);
            }
            throw e;
        } catch (Exception e) {
            log.error("Error completing email signup: {}", e.getMessage(), e);
            // Release reservation on failure
            emailReservationService.releaseReservation(normalizedEmail);
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
     * Build futuristic HTML email template for verification code.
     */
    private String buildVerificationEmailHtml(String otpCode) {
        // Split OTP into individual digits for stylized display
        StringBuilder otpDigits = new StringBuilder();
        for (char digit : otpCode.toCharArray()) {
            otpDigits.append("<span style=\"display: inline-block; width: 52px; height: 64px; ")
                .append("background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%); ")
                .append("border: 1px solid #334155; border-radius: 12px; margin: 0 4px; ")
                .append("line-height: 64px; font-size: 28px; font-weight: 700; color: #00ff88; ")
                .append("font-family: 'SF Mono', 'Fira Code', monospace; ")
                .append("box-shadow: 0 4px 16px rgba(0, 255, 136, 0.15), inset 0 1px 0 rgba(255,255,255,0.05);\">")
                .append(digit)
                .append("</span>");
        }

        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
            "<body style=\"margin: 0; padding: 0; background-color: #0a0a0f; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\">" +
            "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #0a0a0f;\">" +
            "<tr><td align=\"center\" style=\"padding: 40px 20px;\">" +
            "<table role=\"presentation\" width=\"100%\" style=\"max-width: 520px; background: linear-gradient(180deg, #12121a 0%, #0d0d14 100%); border-radius: 24px; border: 1px solid #1e293b; overflow: hidden;\">" +

            // Header with logo
            "<tr><td style=\"padding: 40px 40px 24px 40px; text-align: center;\">" +
            "<div style=\"display: inline-block; margin-bottom: 8px;\">" +
            "<span style=\"font-size: 32px; font-weight: 800; color: #00ff88; letter-spacing: -1px;\">STRATEGIZ</span>" +
            "</div>" +
            "<div style=\"width: 60px; height: 3px; background: linear-gradient(90deg, #00ff88 0%, #00cc6a 100%); margin: 0 auto; border-radius: 2px;\"></div>" +
            "</td></tr>" +

            // Main content
            "<tr><td style=\"padding: 0 40px;\">" +
            "<h1 style=\"color: #f8fafc; font-size: 24px; font-weight: 600; margin: 0 0 8px 0; text-align: center;\">Verify Your Email</h1>" +
            "<p style=\"color: #94a3b8; font-size: 15px; line-height: 1.6; margin: 0 0 32px 0; text-align: center;\">Enter this code to complete your registration and start building intelligent trading strategies.</p>" +
            "</td></tr>" +

            // OTP Code display
            "<tr><td style=\"padding: 0 40px;\">" +
            "<div style=\"background: linear-gradient(180deg, #0f172a 0%, #020617 100%); border: 1px solid #1e293b; border-radius: 16px; padding: 32px 20px; text-align: center; margin-bottom: 16px;\">" +
            "<p style=\"color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 2px; margin: 0 0 16px 0;\">Your Verification Code</p>" +
            "<div style=\"display: inline-block;\">" + otpDigits + "</div>" +
            // Copy hint
            "<p style=\"color: #475569; font-size: 12px; margin: 16px 0 0 0;\">📋 Select code to copy</p>" +
            "</div>" +
            "</td></tr>" +

            // Timer/expiration notice
            "<tr><td style=\"padding: 0 40px;\">" +
            "<div style=\"text-align: center; margin-bottom: 32px;\">" +
            "<span style=\"display: inline-block; background: rgba(0, 255, 136, 0.1); border: 1px solid rgba(0, 255, 136, 0.2); border-radius: 20px; padding: 8px 16px; color: #00ff88; font-size: 13px;\">" +
            "⏱ Expires in " + expirationMinutes + " minutes" +
            "</span>" +
            "</div>" +
            "</td></tr>" +

            // Divider
            "<tr><td style=\"padding: 0 40px;\">" +
            "<div style=\"height: 1px; background: linear-gradient(90deg, transparent 0%, #1e293b 50%, transparent 100%); margin-bottom: 24px;\"></div>" +
            "</td></tr>" +

            // Security notice
            "<tr><td style=\"padding: 0 40px 40px 40px;\">" +
            "<p style=\"color: #64748b; font-size: 13px; line-height: 1.6; margin: 0; text-align: center;\">" +
            "Didn't request this code? You can safely ignore this email.<br>" +
            "Someone may have entered your email by mistake." +
            "</p>" +
            "</td></tr>" +

            // Footer
            "<tr><td style=\"background: #080810; padding: 24px 40px; border-top: 1px solid #1e293b;\">" +
            "<table role=\"presentation\" width=\"100%\"><tr>" +
            "<td style=\"text-align: center;\">" +
            "<p style=\"color: #475569; font-size: 12px; margin: 0 0 8px 0;\">© 2026 Strategiz. All rights reserved.</p>" +
            "<p style=\"margin: 0;\">" +
            "<a href=\"https://strategiz.io\" style=\"color: #64748b; text-decoration: none; font-size: 12px; margin: 0 8px;\">Website</a>" +
            "<span style=\"color: #334155;\">•</span>" +
            "<a href=\"https://strategiz.io/privacy\" style=\"color: #64748b; text-decoration: none; font-size: 12px; margin: 0 8px;\">Privacy</a>" +
            "<span style=\"color: #334155;\">•</span>" +
            "<a href=\"https://strategiz.io/terms\" style=\"color: #64748b; text-decoration: none; font-size: 12px; margin: 0 8px;\">Terms</a>" +
            "</p>" +
            "</td>" +
            "</tr></table>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr></table>" +
            "</body></html>";
    }

    /**
     * Send OTP email via SendGrid.
     */
    private boolean sendOtpEmail(String email, String otpCode) {
        try {
            if (emailProvider == null || !emailProvider.isAvailable()) {
                log.error("Email provider not configured or unavailable. Cannot send OTP to {}", email);
                return false;
            }

            String subject = "Verify Your Email - Strategiz";
            String textContent = "Welcome to Strategiz!\n\n" +
                "Your verification code is: " + otpCode + "\n\n" +
                "Enter this code to complete your registration.\n" +
                "This code expires in " + expirationMinutes + " minutes.\n\n" +
                "If you didn't request this, please ignore this email.";

            String htmlContent = buildVerificationEmailHtml(otpCode);

            EmailMessage message = EmailMessage.builder()
                .toEmail(email)
                .fromEmail(fromEmail)
                .fromName("Strategiz")
                .subject(subject)
                .bodyText(textContent)
                .bodyHtml(htmlContent)
                .build();

            EmailDeliveryResult result = emailProvider.sendEmail(message);

            if (result.isSuccess()) {
                log.info("Sent verification email to {} via {}", email, result.getProviderName());
                return true;
            } else {
                log.error("Failed to send verification email to {}: {} - {}",
                    email, result.getErrorCode(), result.getErrorMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending verification email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Record for storing pending signup data.
     * No password stored - this is a passwordless signup flow.
     * userId is pre-generated during reservation for consistency.
     */
    private record PendingSignup(
        String name,
        String email,
        String otpCode,
        Instant expiration,
        String userId
    ) {}
}
