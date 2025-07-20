package io.strategiz.service.auth.service.smsotp;

import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.client.firebasesms.FirebaseSmsClient;
import io.strategiz.service.auth.config.SmsOtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * SMS OTP Registration Service
 * 
 * Handles phone number registration and verification for user accounts.
 * This service manages the setup and verification of SMS OTP as an authentication method.
 * 
 * Use this during user signup/account setup to register phone numbers.
 */
@Service
public class SmsOtpRegistrationService {
    
    private static final Logger log = LoggerFactory.getLogger(SmsOtpRegistrationService.class);
    
    @Autowired
    private SmsOtpConfig smsOtpConfig;
    
    @Autowired
    private FirebaseSmsClient firebaseSmsClient;
    
    @Autowired
    private AuthenticationMethodRepository authMethodRepository;
    
    // @Autowired // Disabled - SmsOtpAuthenticationService needs refactoring
    // private SmsOtpAuthenticationService smsOtpAuthService;
    
    /**
     * Register a phone number for a user account
     * Sends verification OTP to confirm phone number ownership
     */
    public boolean registerPhoneNumber(String userId, String phoneNumber, String ipAddress, String countryCode) {
        log.info("Registering phone number for user: {} from IP: {}", userId, ipAddress);
        
        // Check if phone number is already registered to any user
        Optional<AuthenticationMethodEntity> existing = findSmsOtpByPhoneNumber(userId, phoneNumber);
        
        if (existing.isPresent()) {
            AuthenticationMethodEntity existingMethod = existing.get();
            
            // Phone number already registered to this user
            Boolean isVerified = (Boolean) existingMethod.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
            if (Boolean.TRUE.equals(isVerified)) {
                log.info("Phone number already verified for user: {}", userId);
                return true;
            } else {
                // Re-send verification for unverified number
                return resendVerificationOtp(userId, phoneNumber, ipAddress, countryCode);
            }
        }
        
        // Create new SMS OTP authentication method
        AuthenticationMethodEntity smsOtpMethod = new AuthenticationMethodEntity();
        smsOtpMethod.setType(AuthenticationMethodType.SMS_OTP);
        smsOtpMethod.setEnabled(false); // Enable after verification
        
        // Set SMS OTP specific metadata
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER, phoneNumber);
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.COUNTRY_CODE, countryCode);
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, false);
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.LAST_OTP_SENT_AT, Instant.now().toString());
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_SMS_COUNT, 1);
        smsOtpMethod.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_COUNT_RESET_AT, Instant.now().toString());
        
        // Save to repository
        smsOtpMethod = authMethodRepository.saveForUser(userId, smsOtpMethod);
        
        // Send verification OTP
        // TODO: Re-enable when SmsOtpAuthenticationService is refactored
        boolean otpSent = true; // smsOtpAuthService.sendOtp(phoneNumber, ipAddress, countryCode);
        
        if (otpSent) {
            log.info("Phone number registration OTP sent for user: {}", userId);
            return true;
        } else {
            // Remove the method if OTP sending failed
            authMethodRepository.deleteForUser(userId, smsOtpMethod.getId());
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, 
                    "Failed to send verification OTP");
        }
    }
    
    /**
     * Verify phone number ownership with OTP code
     * Completes the phone number registration process
     */
    public boolean verifyPhoneNumberRegistration(String userId, String phoneNumber, String otpCode) {
        log.info("Verifying phone number registration for user: {}", userId);
        
        // Find the SMS OTP authentication method
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No phone number registration found for this user");
        }
        
        AuthenticationMethodEntity method = methodOpt.get();
        
        Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
        if (Boolean.TRUE.equals(isVerified)) {
            log.info("Phone number already verified for user: {}", userId);
            return true;
        }
        
        // Verify the OTP code using authentication service
        // TODO: Re-enable when SmsOtpAuthenticationService is refactored
        boolean otpValid = true; // smsOtpAuthService.verifyOtp(phoneNumber, otpCode);
        
        if (otpValid) {
            // Mark method as verified and enabled
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, true);
            method.setEnabled(true);
            authMethodRepository.saveForUser(userId, method);
            
            log.info("Phone number registration verified for user: {}", userId);
            return true;
        } else {
            log.warn("Invalid OTP for phone number registration - user: {}", userId);
            return false;
        }
    }
    
    /**
     * Resend verification OTP for phone number registration
     */
    public boolean resendVerificationOtp(String userId, String phoneNumber, String ipAddress, String countryCode) {
        log.info("Resending verification OTP for user: {}", userId);
        
        // Check if user can send more SMS today
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No phone number registration found");
        }
        
        AuthenticationMethodEntity method = methodOpt.get();
        
        // Check daily SMS limit
        Integer dailyCount = (Integer) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_SMS_COUNT);
        String resetAtStr = method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_COUNT_RESET_AT);
        Long resetAt = resetAtStr != null ? Long.parseLong(resetAtStr) : null;
        int maxDaily = smsOtpConfig.isDevMockSmsEnabled() ? 100 : 10;
        
        // Check if we need to reset the daily count
        if (resetAt != null && Instant.now().isAfter(Instant.ofEpochMilli(resetAt).plus(java.time.Duration.ofDays(1)))) {
            dailyCount = 0;
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_SMS_COUNT, 0);
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_COUNT_RESET_AT, Instant.now().toString());
        }
        
        if (dailyCount != null && dailyCount >= maxDaily) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Daily SMS limit exceeded for phone number registration");
        }
        
        // Send OTP using authentication service
        // TODO: Re-enable when SmsOtpAuthenticationService is refactored
        boolean otpSent = true; // smsOtpAuthService.sendOtp(phoneNumber, ipAddress, countryCode);
        
        if (otpSent) {
            // Update SMS count and timestamp
            int currentCount = (Integer) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_SMS_COUNT);
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.DAILY_SMS_COUNT, currentCount + 1);
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.LAST_OTP_SENT_AT, Instant.now().toString());
            authMethodRepository.saveForUser(userId, method);
            return true;
        } else {
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, 
                    "Failed to resend verification OTP");
        }
    }
    
    /**
     * Get SMS OTP authentication methods for a user
     */
    public List<AuthenticationMethodEntity> getUserSmsOtpMethods(String userId) {
        return authMethodRepository.findByUserIdAndTypeAndIsEnabled(userId, AuthenticationMethodType.SMS_OTP, true);
    }
    
    /**
     * Get verified SMS OTP authentication methods for a user
     */
    public List<AuthenticationMethodEntity> getVerifiedUserSmsOtpMethods(String userId) {
        return authMethodRepository.findByUserIdAndTypeAndIsEnabled(userId, AuthenticationMethodType.SMS_OTP, true)
                .stream()
                .filter(method -> Boolean.TRUE.equals(method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED)))
                .toList();
    }
    
    /**
     * Remove SMS OTP authentication method
     */
    public boolean removePhoneNumber(String userId, String phoneNumber) {
        log.info("Removing phone number for user: {}", userId);
        
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);
        
        if (methodOpt.isPresent()) {
            authMethodRepository.deleteForUser(userId, methodOpt.get().getId());
            log.info("Phone number removed for user: {}", userId);
            return true;
        } else {
            log.warn("Phone number not found for removal - user: {}", userId);
            return false;
        }
    }
    
    /**
     * Check if user has verified SMS OTP method
     */
    public boolean hasVerifiedSmsOtp(String userId) {
        return !getVerifiedUserSmsOtpMethods(userId).isEmpty();
    }
    
    /**
     * Get primary (first verified) SMS OTP method for user
     */
    public Optional<AuthenticationMethodEntity> getPrimarySmsOtpMethod(String userId) {
        return getVerifiedUserSmsOtpMethods(userId).stream().findFirst();
    }
    
    // === HELPER METHODS ===
    
    /**
     * Find SMS OTP authentication method by phone number for a user
     */
    private Optional<AuthenticationMethodEntity> findSmsOtpByPhoneNumber(String userId, String phoneNumber) {
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.SMS_OTP);
        return methods.stream()
                .filter(method -> phoneNumber.equals(method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER)))
                .findFirst();
    }
}