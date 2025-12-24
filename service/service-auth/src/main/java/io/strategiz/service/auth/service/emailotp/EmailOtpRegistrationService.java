package io.strategiz.service.auth.service.emailotp;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Email OTP Registration Service
 * 
 * Handles email address registration and verification for user accounts.
 * This service manages the setup and verification of email addresses as authentication methods.
 * 
 * Use this during user signup/account setup to register and verify email addresses.
 */
@Service
public class EmailOtpRegistrationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    // In-memory store for registration OTP codes (replace with persistent storage in production)
    private final Map<String, RegistrationOtpEntry> registrationOtpCodes = new ConcurrentHashMap<>();
    
    @Value("${email.otp.expiration.minutes:10}")
    private int expirationMinutes;
    
    @Value("${email.otp.code.length:6}")
    private int codeLength;
    
    @Value("${spring.mail.username:no-reply@strategiz.io}")
    private String fromEmail;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired
    private AuthenticationMethodRepository authMethodRepository;
    
    // Note: EmailOtpAuthenticationService dependency removed to avoid circular dependencies
    
    /**
     * Register an email address for a user account
     * Sends verification OTP to confirm email ownership
     */
    public boolean registerEmailAddress(String userId, String email) {
        log.info("Registering email address for user: {}", userId);
        
        // Check if email is already registered to any user by searching all users
        // Note: This is simplified - in practice you'd want a more efficient lookup
        List<AuthenticationMethodEntity> existingMethods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.EMAIL_OTP);
        
        // Check if this email is already registered for this user
        Optional<AuthenticationMethodEntity> existing = existingMethods.stream()
            .filter(method -> email.equals(method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS)))
            .findFirst();
            
        if (existing.isPresent()) {
            AuthenticationMethodEntity existingMethod = existing.get();
            
            // Email already registered to this user
            boolean isVerified = Boolean.TRUE.equals(existingMethod.getMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED));
            if (isVerified) {
                log.info("Email address already verified for user: {}", userId);
                return true;
            } else {
                // Re-send verification for unverified email
                return resendVerificationOtp(userId, email);
            }
        }
        
        // Create new Email OTP authentication method
        AuthenticationMethodEntity emailOtpMethod = new AuthenticationMethodEntity(AuthenticationMethodType.EMAIL_OTP, "Email: " + email);
        emailOtpMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS, email);
        emailOtpMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED, false);
        emailOtpMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, 0);
        emailOtpMethod.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_DATE, java.time.LocalDate.now().toString());
        emailOtpMethod.setIsActive(false); // Enable after verification
        
        // Save to repository
        AuthenticationMethodEntity saved = authMethodRepository.saveForUser(userId, emailOtpMethod);
        
        // Send verification OTP
        boolean otpSent = sendRegistrationOtp(email, "signup");
        
        if (otpSent) {
            // Update with OTP sent info
            saved.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_SENT, java.time.Instant.now().toString());
            int currentCount = (Integer) saved.getMetadata().getOrDefault(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, 0);
            saved.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, currentCount + 1);
            authMethodRepository.saveForUser(userId, saved);
            log.info("Email address registration OTP sent for user: {}", userId);
            return true;
        } else {
            // Remove the method if OTP sending failed
            authMethodRepository.deleteForUser(userId, saved.getId());
            throw new StrategizException(AuthErrors.EMAIL_SEND_FAILED, 
                    "Failed to send verification email");
        }
    }
    
    /**
     * Verify email address ownership with OTP code
     * Completes the email address registration process
     */
    public boolean verifyEmailRegistration(String userId, String email, String otpCode) {
        log.info("Verifying email registration for user: {}", userId);
        
        // Find the Email OTP authentication method
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.EMAIL_OTP);
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(method -> email.equals(method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS)))
            .findFirst();
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No email registration found for this user");
        }
        
        AuthenticationMethodEntity method = methodOpt.get();
        
        boolean isVerified = Boolean.TRUE.equals(method.getMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED));
        if (isVerified) {
            log.info("Email address already verified for user: {}", userId);
            return true;
        }
        
        // Verify the OTP code
        boolean otpValid = verifyRegistrationOtp(email, "signup", otpCode);
        
        if (otpValid) {
            // Mark method as verified and enabled
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED, true);
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.VERIFICATION_TIME, java.time.Instant.now().toString());
            method.setIsActive(true);
            method.markAsUsed(); // Update last used time
            authMethodRepository.saveForUser(userId, method);
            
            log.info("Email registration verified for user: {}", userId);
            return true;
        } else {
            log.warn("Invalid OTP for email registration - user: {}", userId);
            return false;
        }
    }
    
    /**
     * Resend verification OTP for email registration
     */
    public boolean resendVerificationOtp(String userId, String email) {
        log.info("Resending verification OTP for user: {}", userId);
        
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.EMAIL_OTP);
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(method -> email.equals(method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS)))
            .findFirst();
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No email registration found");
        }
        
        AuthenticationMethodEntity method = methodOpt.get();
        
        // Check daily email limit
        int dailyCount = (Integer) method.getMetadata().getOrDefault(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, 0);
        String lastEmailDate = method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_DATE);
        String today = java.time.LocalDate.now().toString();
        
        // Reset counter if it's a new day
        if (!today.equals(lastEmailDate)) {
            dailyCount = 0;
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_DATE, today);
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, 0);
        }
        
        if (dailyCount >= 10) { // Max 10 emails per day
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Daily email limit exceeded for email registration");
        }
        
        // Send OTP
        boolean otpSent = sendRegistrationOtp(email, "signup");
        
        if (otpSent) {
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_SENT, java.time.Instant.now().toString());
            method.putMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, dailyCount + 1);
            authMethodRepository.saveForUser(userId, method);
            return true;
        } else {
            throw new StrategizException(AuthErrors.EMAIL_SEND_FAILED, 
                    "Failed to resend verification email");
        }
    }
    
    /**
     * Get Email OTP authentication methods for a user
     */
    public List<AuthenticationMethodEntity> getUserEmailOtpMethods(String userId) {
        return authMethodRepository.findByUserIdAndTypeAndIsEnabled(userId, AuthenticationMethodType.EMAIL_OTP, true);
    }
    
    /**
     * Get verified Email OTP authentication methods for a user
     */
    public List<AuthenticationMethodEntity> getVerifiedUserEmailOtpMethods(String userId) {
        return authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.EMAIL_OTP)
                .stream()
                .filter(method -> Boolean.TRUE.equals(method.getMetadata(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED)))
                .toList();
    }
    
    /**
     * Remove Email OTP authentication method
     */
    public boolean removeEmailAddress(String userId, String email) {
        log.info("Removing email address for user: {}", userId);
        
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.EMAIL_OTP);
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(method -> email.equals(method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS)))
            .findFirst();
        
        if (methodOpt.isPresent()) {
            authMethodRepository.deleteForUser(userId, methodOpt.get().getId());
            log.info("Email address removed for user: {}", userId);
            return true;
        } else {
            log.warn("Email address not found for removal - user: {}", userId);
            return false;
        }
    }
    
    /**
     * Check if user has verified Email OTP method
     */
    public boolean hasVerifiedEmailOtp(String userId) {
        return !getVerifiedUserEmailOtpMethods(userId).isEmpty();
    }
    
    /**
     * Get primary (first verified) Email OTP method for user
     */
    public Optional<AuthenticationMethodEntity> getPrimaryEmailOtpMethod(String userId) {
        return getVerifiedUserEmailOtpMethods(userId).stream().findFirst();
    }
    
    // === REGISTRATION OTP METHODS ===
    
    /**
     * Generate and send an OTP code to the specified email for registration
     */
    private boolean sendRegistrationOtp(String email, String purpose) {
        if (email == null || email.isBlank()) {
            log.error("Cannot send registration OTP to null or empty email");
            return false;
        }
        
        // Generate verification code
        String otpCode = generateOtpCode();
        
        // Store the code with expiration time
        String key = createKey(email, purpose);
        Instant expiration = Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(expirationMinutes));
        registrationOtpCodes.put(key, new RegistrationOtpEntry(otpCode, expiration));
        
        log.info("Generated registration OTP for {}, purpose: {}, expires at: {}", 
                email, purpose, expiration);
        
        // Send the email
        return sendOtpEmail(email, otpCode, purpose);
    }
    
    /**
     * Verify a registration OTP code
     */
    private boolean verifyRegistrationOtp(String email, String purpose, String submittedCode) {
        if (email == null || email.isBlank() || submittedCode == null || submittedCode.isBlank()) {
            log.error("Cannot verify registration OTP with null or empty email or code");
            return false;
        }
        
        String key = createKey(email, purpose);
        RegistrationOtpEntry entry = registrationOtpCodes.get(key);
        
        if (entry == null) {
            log.warn("No registration OTP found for {}, purpose: {}", email, purpose);
            return false;
        }
        
        if (Instant.now().isAfter(entry.expiration())) {
            log.warn("Registration OTP expired for {}, purpose: {}", email, purpose);
            registrationOtpCodes.remove(key);
            return false;
        }
        
        boolean isValid = entry.code().equals(submittedCode);
        
        if (isValid) {
            log.info("Registration OTP verified successfully for {}, purpose: {}", email, purpose);
            registrationOtpCodes.remove(key); // Remove after successful verification
        } else {
            log.warn("Invalid registration OTP submitted for {}, purpose: {}", email, purpose);
        }
        
        return isValid;
    }
    
    /**
     * Generate a random OTP code
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
     */
    private String createKey(String email, String purpose) {
        return email.toLowerCase() + ":" + purpose;
    }
    
    /**
     * Send an email containing the OTP code for registration
     */
    private boolean sendOtpEmail(String email, String otpCode, String purpose) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured. Would have sent registration OTP {} to {}", otpCode, email);
                return true; // Return true for development without email server
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            
            String subject = "Complete Your Registration";
            String content = "Welcome to Strategiz!\n\n" +
                           "Your email verification code is: " + otpCode + "\n\n" +
                           "Use this code to complete your account registration.\n" +
                           "This code will expire in " + expirationMinutes + " minutes.\n\n" +
                           "If you didn't request this code, please ignore this email.";
            
            message.setSubject(subject);
            message.setText(content);
            
            try {
                mailSender.send(message);
                log.info("Sent registration OTP email to {}", email);
                return true;
            } catch (Exception e) {
                log.error("Failed to send registration OTP email: {}", e.getMessage(), e);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error preparing registration OTP email: {}", e.getMessage(), e);
            return false;
        }
    }
    
    // === HELPER METHODS ===
    
    /**
     * Check if an email can be sent today based on daily limits
     */
    private boolean canSendEmailToday(AuthenticationMethodEntity method, int maxPerDay) {
        int dailyCount = (Integer) method.getMetadata().getOrDefault(AuthenticationMethodMetadata.EmailOtpMetadata.DAILY_EMAIL_COUNT, 0);
        String lastEmailDate = method.getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.LAST_EMAIL_DATE);
        String today = java.time.LocalDate.now().toString();
        
        // Reset counter if it's a new day
        if (!today.equals(lastEmailDate)) {
            return true; // New day, can send
        }
        
        return dailyCount < maxPerDay;
    }
    
    /**
     * Record for storing registration OTP code with expiration time
     */
    private record RegistrationOtpEntry(String code, Instant expiration) {}
}