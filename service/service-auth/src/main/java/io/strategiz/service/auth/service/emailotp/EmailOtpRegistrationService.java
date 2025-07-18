package io.strategiz.service.auth.service.emailotp;

import io.strategiz.data.auth.model.emailotp.EmailOtpAuthenticationMethod;
import io.strategiz.data.auth.repository.emailotp.EmailOtpAuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class EmailOtpRegistrationService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailOtpRegistrationService.class);
    
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
    private EmailOtpAuthenticationMethodRepository emailOtpAuthMethodRepository;
    
    @Autowired
    private EmailOtpAuthenticationService emailOtpAuthService;
    
    /**
     * Register an email address for a user account
     * Sends verification OTP to confirm email ownership
     */
    public boolean registerEmailAddress(String userId, String email) {
        log.info("Registering email address for user: {}", userId);
        
        // Check if email is already registered to any user
        if (emailOtpAuthMethodRepository.existsByEmail(email)) {
            Optional<EmailOtpAuthenticationMethod> existing = emailOtpAuthMethodRepository.findByEmail(email)
                    .map(this::convertToModel);
            
            if (existing.isPresent()) {
                EmailOtpAuthenticationMethod existingMethod = existing.get();
                if (!userId.equals(existingMethod.getUserId())) {
                    throw new StrategizException(AuthErrors.INVALID_EMAIL, 
                            "Email address is already registered to another account");
                }
                
                // Email already registered to this user
                if (existingMethod.isVerified()) {
                    log.info("Email address already verified for user: {}", userId);
                    return true;
                } else {
                    // Re-send verification for unverified email
                    return resendVerificationOtp(userId, email);
                }
            }
        }
        
        // Create new Email OTP authentication method
        EmailOtpAuthenticationMethod emailOtpMethod = new EmailOtpAuthenticationMethod(email);
        emailOtpMethod.setUserId(userId);
        emailOtpMethod.setVerified(false);
        emailOtpMethod.setEnabled(false); // Enable after verification
        
        // Save to repository
        emailOtpAuthMethodRepository.save(convertToEntity(emailOtpMethod));
        
        // Send verification OTP
        boolean otpSent = sendRegistrationOtp(email, "signup");
        
        if (otpSent) {
            emailOtpMethod.markOtpSent();
            emailOtpAuthMethodRepository.save(convertToEntity(emailOtpMethod));
            log.info("Email address registration OTP sent for user: {}", userId);
            return true;
        } else {
            // Remove the method if OTP sending failed
            emailOtpAuthMethodRepository.deleteById(emailOtpMethod.getId());
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
        Optional<EmailOtpAuthenticationMethod> methodOpt = emailOtpAuthMethodRepository
                .findByUserIdAndEmail(userId, email)
                .map(this::convertToModel);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No email registration found for this user");
        }
        
        EmailOtpAuthenticationMethod method = methodOpt.get();
        
        if (method.isVerified()) {
            log.info("Email address already verified for user: {}", userId);
            return true;
        }
        
        // Verify the OTP code
        boolean otpValid = verifyRegistrationOtp(email, "signup", otpCode);
        
        if (otpValid) {
            // Mark method as verified and enabled
            method.markVerified();
            method.setEnabled(true);
            emailOtpAuthMethodRepository.save(convertToEntity(method));
            
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
        
        Optional<EmailOtpAuthenticationMethod> methodOpt = emailOtpAuthMethodRepository
                .findByUserIdAndEmail(userId, email)
                .map(this::convertToModel);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No email registration found");
        }
        
        EmailOtpAuthenticationMethod method = methodOpt.get();
        
        // Check daily email limit
        if (!method.canSendEmailToday(10)) { // Max 10 emails per day
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Daily email limit exceeded for email registration");
        }
        
        // Send OTP
        boolean otpSent = sendRegistrationOtp(email, "signup");
        
        if (otpSent) {
            method.markOtpSent();
            emailOtpAuthMethodRepository.save(convertToEntity(method));
            return true;
        } else {
            throw new StrategizException(AuthErrors.EMAIL_SEND_FAILED, 
                    "Failed to resend verification email");
        }
    }
    
    /**
     * Get Email OTP authentication methods for a user
     */
    public List<EmailOtpAuthenticationMethod> getUserEmailOtpMethods(String userId) {
        return emailOtpAuthMethodRepository.findByUserIdAndEnabled(userId, true)
                .stream()
                .map(this::convertToModel)
                .toList();
    }
    
    /**
     * Get verified Email OTP authentication methods for a user
     */
    public List<EmailOtpAuthenticationMethod> getVerifiedUserEmailOtpMethods(String userId) {
        return emailOtpAuthMethodRepository.findByUserIdAndVerified(userId, true)
                .stream()
                .map(this::convertToModel)
                .toList();
    }
    
    /**
     * Remove Email OTP authentication method
     */
    public boolean removeEmailAddress(String userId, String email) {
        log.info("Removing email address for user: {}", userId);
        
        Optional<EmailOtpAuthenticationMethod> methodOpt = emailOtpAuthMethodRepository
                .findByUserIdAndEmail(userId, email)
                .map(this::convertToModel);
        
        if (methodOpt.isPresent()) {
            emailOtpAuthMethodRepository.deleteById(methodOpt.get().getId());
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
    public Optional<EmailOtpAuthenticationMethod> getPrimaryEmailOtpMethod(String userId) {
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
    
    // === CONVERSION METHODS ===
    
    private EmailOtpAuthenticationMethod convertToModel(io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity entity) {
        // TODO: Implement proper entity to model conversion
        EmailOtpAuthenticationMethod model = new EmailOtpAuthenticationMethod();
        model.setId(entity.getMethodId());
        model.setUserId(entity.getUserId());
        model.setEmail(entity.getEmail());
        model.setVerified(entity.isVerified());
        model.setEnabled(entity.isEnabled());
        model.setLastOtpSentAt(entity.getLastOtpSentAt());
        model.setDailyEmailCount(entity.getDailyEmailCount());
        model.setDailyCountResetAt(entity.getDailyCountResetAt());
        return model;
    }
    
    private io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity convertToEntity(EmailOtpAuthenticationMethod model) {
        // TODO: Implement proper model to entity conversion
        io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity entity = 
                new io.strategiz.data.auth.entity.emailotp.EmailOtpAuthenticationMethodEntity();
        entity.setMethodId(model.getId());
        entity.setUserId(model.getUserId());
        entity.setEmail(model.getEmail());
        entity.setVerified(model.isVerified());
        entity.setEnabled(model.isEnabled());
        entity.setLastOtpSentAt(model.getLastOtpSentAt());
        entity.setDailyEmailCount(model.getDailyEmailCount());
        entity.setDailyCountResetAt(model.getDailyCountResetAt());
        return entity;
    }
    
    /**
     * Record for storing registration OTP code with expiration time
     */
    private record RegistrationOtpEntry(String code, Instant expiration) {}
}