package io.strategiz.service.auth.service.emailotp;

import io.strategiz.data.auth.entity.OtpCodeEntity;
import io.strategiz.data.auth.repository.OtpCodeRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Email OTP Authentication Service
 * 
 * Handles email OTP authentication for user login/signin.
 * This service sends OTP codes to verified email addresses and verifies them for authentication.
 * 
 * Use this during user signin/authentication flows.
 */
@Service
public class EmailOtpAuthenticationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Configurable OTP expiration time (in minutes)
    @Value("${email.otp.expiration.minutes:10}")
    private int expirationMinutes;

    @Value("${email.otp.code.length:6}")
    private int codeLength;

    @Value("${spring.mail.username:no-reply@strategiz.io}")
    private String fromEmail;

    @Value("${email.otp.admin.bypass.enabled:true}")
    private boolean adminBypassEnabled;

    @Value("${email.otp.max.attempts:3}")
    private int maxAttempts;

    // Made optional to allow service to run even if mail dependency isn't properly resolved
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private OtpCodeRepository otpCodeRepository;
    
    /**
     * Generate and send an OTP code to the specified email for authentication
     * Only works with verified email addresses
     *
     * @param email Email address to send OTP to (must be verified)
     * @param purpose Purpose of OTP (e.g., "login", "mfa")
     * @return true if the OTP was generated and sent, false otherwise
     */
    public boolean sendOtp(String email, String purpose) {
        return sendOtp(email, purpose, null);
    }

    /**
     * Generate and send an OTP code to the specified email for authentication.
     * Admin users can bypass OTP if adminBypassEnabled is true.
     *
     * @param email Email address to send OTP to (must be verified)
     * @param purpose Purpose of OTP (e.g., "login", "mfa")
     * @param userId Optional user ID for admin bypass check
     * @return true if the OTP was generated and sent (or bypassed for admin), false otherwise
     */
    public boolean sendOtp(String email, String purpose, String userId) {
        if (email == null || email.isBlank()) {
            log.error("Cannot send OTP to null or empty email");
            return false;
        }

        // Check for admin bypass
        if (adminBypassEnabled && isAdminUser(userId)) {
            log.info("Admin bypass - skipping OTP send for admin user: {}", userId);
            return true;
        }

        // Generate verification code
        String otpCode = generateOtpCode();
        String normalizedEmail = email.toLowerCase();

        // Delete any existing OTP for this email/purpose
        if (otpCodeRepository != null) {
            otpCodeRepository.deleteByEmailAndPurpose(normalizedEmail, purpose);

            // Store the code with expiration time (hashed for security)
            Instant expiration = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(expirationMinutes));
            OtpCodeEntity entity = new OtpCodeEntity(
                    normalizedEmail,
                    purpose,
                    passwordEncoder.encode(otpCode),
                    expiration);
            entity.setMaxAttempts(maxAttempts);
            otpCodeRepository.save(entity, "system");

            log.info("Generated OTP for {}, purpose: {}, expires at: {}",
                    normalizedEmail, purpose, expiration);
        }
        else {
            log.warn("OtpCodeRepository not available - OTP will not be persisted");
        }

        // Send the email
        return sendOtpEmail(email, otpCode, purpose);
    }
    
    /**
     * Verify an OTP code submitted by a user
     *
     * @param email Email address
     * @param purpose Purpose of OTP
     * @param submittedCode Code submitted by the user
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyOtp(String email, String purpose, String submittedCode) {
        return verifyOtp(email, purpose, submittedCode, null);
    }

    /**
     * Verify an OTP code submitted by a user.
     * Admin users can bypass OTP verification if adminBypassEnabled is true.
     *
     * @param email Email address
     * @param purpose Purpose of OTP
     * @param submittedCode Code submitted by the user
     * @param userId Optional user ID for admin bypass check
     * @return true if the code is valid (or bypassed for admin), false otherwise
     */
    public boolean verifyOtp(String email, String purpose, String submittedCode, String userId) {
        // Check for admin bypass first
        if (adminBypassEnabled && isAdminUser(userId)) {
            log.info("Admin bypass - auto-verifying OTP for admin user: {}", userId);
            // Clean up any pending OTP for this email/purpose
            if (otpCodeRepository != null) {
                otpCodeRepository.deleteByEmailAndPurpose(email.toLowerCase(), purpose);
            }
            return true;
        }

        if (email == null || email.isBlank() || submittedCode == null || submittedCode.isBlank()) {
            log.error("Cannot verify with null or empty email or code");
            return false;
        }

        String normalizedEmail = email.toLowerCase();

        if (otpCodeRepository == null) {
            log.error("OtpCodeRepository not available - cannot verify OTP");
            return false;
        }

        Optional<OtpCodeEntity> entityOpt = otpCodeRepository.findByEmailAndPurpose(normalizedEmail, purpose);

        if (entityOpt.isEmpty()) {
            log.warn("No OTP found for {}, purpose: {}", normalizedEmail, purpose);
            return false;
        }

        OtpCodeEntity entity = entityOpt.get();

        // Check expiration
        if (entity.isExpired()) {
            log.warn("OTP expired for {}, purpose: {}", normalizedEmail, purpose);
            otpCodeRepository.deleteById(entity.getId());
            return false;
        }

        // Check max attempts
        if (entity.hasExceededMaxAttempts()) {
            log.warn("Max attempts exceeded for {}, purpose: {}", normalizedEmail, purpose);
            otpCodeRepository.deleteById(entity.getId());
            return false;
        }

        // Verify the code using BCrypt
        boolean isValid = passwordEncoder.matches(submittedCode, entity.getCodeHash());

        if (isValid) {
            log.info("OTP verified successfully for {}, purpose: {}", normalizedEmail, purpose);
            otpCodeRepository.deleteById(entity.getId()); // Remove after successful verification
        }
        else {
            log.warn("Invalid OTP submitted for {}, purpose: {}", normalizedEmail, purpose);
            // Increment attempts
            entity.incrementAttempts();
            otpCodeRepository.update(entity, "system");
        }

        return isValid;
    }
    
    /**
     * Check if an email has a pending OTP verification
     *
     * @param email Email address
     * @param purpose Purpose of OTP
     * @return true if a pending verification exists, false otherwise
     */
    public boolean hasPendingOtp(String email, String purpose) {
        if (email == null || email.isBlank()) {
            return false;
        }

        if (otpCodeRepository == null) {
            log.warn("OtpCodeRepository not available - cannot check pending OTP");
            return false;
        }

        String normalizedEmail = email.toLowerCase();
        Optional<OtpCodeEntity> entityOpt = otpCodeRepository.findByEmailAndPurpose(normalizedEmail, purpose);

        if (entityOpt.isEmpty()) {
            return false;
        }

        OtpCodeEntity entity = entityOpt.get();

        // Check if expired
        if (entity.isExpired()) {
            otpCodeRepository.deleteById(entity.getId());
            return false;
        }

        return true;
    }
    
    /**
     * Generate a random OTP code
     * 
     * @return The generated code
     */
    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(codeLength);
        
        for (int i = 0; i < codeLength; i++) {
            sb.append(random.nextInt(10)); // 0-9 digits
        }
        
        return sb.toString();
    }

    /**
     * Send an email containing the OTP code
     * 
     * @param email Recipient email address
     * @param otpCode OTP code to include in the email
     * @param purpose Purpose of OTP
     * @return true if the email was sent, false otherwise
     */
    private boolean sendOtpEmail(String email, String otpCode, String purpose) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. Would have sent OTP {} to {}", otpCode, email);
                return true; // Return true for development without email server
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            
            // Customize subject and content based on purpose
            String subject;
            String content;
            
            switch (purpose) {
                case "signup":
                    subject = "Complete Your Registration";
                    content = "Your verification code is: " + otpCode + 
                              "\nUse this code to complete your account registration.";
                    break;
                case "reset-password":
                    subject = "Password Reset Request";
                    content = "Your password reset code is: " + otpCode + 
                              "\nUse this code to reset your password.";
                    break;
                case "change-email":
                    subject = "Confirm Email Change";
                    content = "Your verification code is: " + otpCode + 
                              "\nUse this code to confirm your email change.";
                    break;
                case "account-recovery":
                    subject = "Account Recovery";
                    content = "Your account recovery code is: " + otpCode + 
                              "\nUse this code to recover your account.";
                    break;
                default:
                    subject = "Verification Code";
                    content = "Your verification code is: " + otpCode;
            }
            
            message.setSubject(subject);
            message.setText(content);
            
            try {
                ((JavaMailSender)mailSender).send(message);
                log.info("Sent OTP email to {}", email);
                return true;
            } catch (Exception e) {
                log.error("Failed to send OTP email: {}", e.getMessage(), e);
                return false;
            }

        } catch (Exception e) {
            log.error("Error preparing OTP email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if a user has admin role.
     *
     * @param userId the user ID to check
     * @return true if user is admin, false otherwise
     */
    private boolean isAdminUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        if (userRepository == null) {
            log.debug("UserRepository not available - admin bypass not possible");
            return false;
        }

        try {
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                UserEntity user = userOpt.get();
                if (user.getProfile() != null) {
                    return "ADMIN".equals(user.getProfile().getRole());
                }
            }
        } catch (Exception e) {
            log.warn("Error checking admin status for user {}: {}", userId, e.getMessage());
        }

        return false;
    }

}
