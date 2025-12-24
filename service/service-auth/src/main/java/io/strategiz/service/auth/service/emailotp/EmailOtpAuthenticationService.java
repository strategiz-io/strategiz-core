package io.strategiz.service.auth.service.emailotp;

import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // In-memory store of OTP codes (for demo purposes)
    // In production, this should use a persistent store
    private final Map<String, OtpEntry> otpCodes = new ConcurrentHashMap<>();
    
    // Configurable OTP expiration time (in minutes)
    @Value("${email.otp.expiration.minutes:10}")
    private int expirationMinutes;
    
    @Value("${email.otp.code.length:6}")
    private int codeLength;
    
    @Value("${spring.mail.username:no-reply@strategiz.io}")
    private String fromEmail;

    // Made optional to allow service to run even if mail dependency isn't properly resolved
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    /**
     * Generate and send an OTP code to the specified email for authentication
     * Only works with verified email addresses
     * 
     * @param email Email address to send OTP to (must be verified)
     * @param purpose Purpose of OTP (e.g., "login", "mfa")
     * @return true if the OTP was generated and sent, false otherwise
     */
    public boolean sendOtp(String email, String purpose) {
        if (email == null || email.isBlank()) {
            log.error("Cannot send OTP to null or empty email");
            return false;
        }
        
        // Generate verification code
        String otpCode = generateOtpCode();
        
        // Store the code with expiration time
        String key = createKey(email, purpose);
        Instant expiration = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(expirationMinutes));
        otpCodes.put(key, new OtpEntry(otpCode, expiration));

        log.info("Generated OTP for {}, purpose: {}, expires at: {}",
                email, purpose, expiration);
        
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
        if (email == null || email.isBlank() || submittedCode == null || submittedCode.isBlank()) {
            log.error("Cannot verify with null or empty email or code");
            return false;
        }

        String key = createKey(email, purpose);
        OtpEntry entry = otpCodes.get(key);

        if (entry == null) {
            log.warn("No OTP found for {}, purpose: {}", email, purpose);
            return false;
        }

        if (Instant.now().isAfter(entry.expiration())) {
            log.warn("OTP expired for {}, purpose: {}", email, purpose);
            otpCodes.remove(key);
            return false;
        }

        boolean isValid = entry.code().equals(submittedCode);

        if (isValid) {
            log.info("OTP verified successfully for {}, purpose: {}", email, purpose);
            otpCodes.remove(key); // Remove after successful verification (one-time use)
        } else {
            log.warn("Invalid OTP submitted for {}, purpose: {}", email, purpose);
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
        
        String key = createKey(email, purpose);
        OtpEntry entry = otpCodes.get(key);
        
        if (entry == null) {
            return false;
        }
        
        if (Instant.now().isAfter(entry.expiration())) {
            otpCodes.remove(key);
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
     * Create a unique key for storing OTP entries
     * 
     * @param email Email address
     * @param purpose Purpose of verification
     * @return The key
     */
    private String createKey(String email, String purpose) {
        return email.toLowerCase() + ":" + purpose;
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
     * Record for storing OTP code with expiration time
     */
    private record OtpEntry(String code, Instant expiration) {}
}
