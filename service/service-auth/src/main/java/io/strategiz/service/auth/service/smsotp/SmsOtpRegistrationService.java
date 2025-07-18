package io.strategiz.service.auth.service.smsotp;

import io.strategiz.data.auth.model.smsotp.SmsOtpAuthenticationMethod;
import io.strategiz.data.auth.repository.smsotp.SmsOtpAuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.client.firebasesms.FirebaseSmsClient;
import io.strategiz.service.auth.config.SmsOtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private SmsOtpAuthenticationMethodRepository smsOtpAuthMethodRepository;
    
    @Autowired
    private SmsOtpAuthenticationService smsOtpAuthService;
    
    /**
     * Register a phone number for a user account
     * Sends verification OTP to confirm phone number ownership
     */
    public boolean registerPhoneNumber(String userId, String phoneNumber, String ipAddress, String countryCode) {
        log.info("Registering phone number for user: {} from IP: {}", userId, ipAddress);
        
        // Check if phone number is already registered to any user
        if (smsOtpAuthMethodRepository.existsByPhoneNumber(phoneNumber)) {
            Optional<SmsOtpAuthenticationMethod> existing = smsOtpAuthMethodRepository.findByPhoneNumber(phoneNumber)
                    .map(this::convertToModel);
            
            if (existing.isPresent()) {
                SmsOtpAuthenticationMethod existingMethod = existing.get();
                if (!userId.equals(existingMethod.getUserId())) {
                    throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, 
                            "Phone number is already registered to another account");
                }
                
                // Phone number already registered to this user
                if (existingMethod.isVerified()) {
                    log.info("Phone number already verified for user: {}", userId);
                    return true;
                } else {
                    // Re-send verification for unverified number
                    return resendVerificationOtp(userId, phoneNumber, ipAddress, countryCode);
                }
            }
        }
        
        // Create new SMS OTP authentication method
        SmsOtpAuthenticationMethod smsOtpMethod = new SmsOtpAuthenticationMethod(phoneNumber, countryCode);
        smsOtpMethod.setUserId(userId);
        smsOtpMethod.setVerified(false);
        smsOtpMethod.setEnabled(false); // Enable after verification
        
        // Save to repository
        smsOtpAuthMethodRepository.save(convertToEntity(smsOtpMethod));
        
        // Send verification OTP
        boolean otpSent = smsOtpAuthService.sendOtp(phoneNumber, ipAddress, countryCode);
        
        if (otpSent) {
            smsOtpMethod.markOtpSent();
            smsOtpAuthMethodRepository.save(convertToEntity(smsOtpMethod));
            log.info("Phone number registration OTP sent for user: {}", userId);
            return true;
        } else {
            // Remove the method if OTP sending failed
            smsOtpAuthMethodRepository.deleteById(smsOtpMethod.getId());
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
        Optional<SmsOtpAuthenticationMethod> methodOpt = smsOtpAuthMethodRepository
                .findByUserIdAndPhoneNumber(userId, phoneNumber)
                .map(this::convertToModel);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No phone number registration found for this user");
        }
        
        SmsOtpAuthenticationMethod method = methodOpt.get();
        
        if (method.isVerified()) {
            log.info("Phone number already verified for user: {}", userId);
            return true;
        }
        
        // Verify the OTP code using authentication service
        boolean otpValid = smsOtpAuthService.verifyOtp(phoneNumber, otpCode);
        
        if (otpValid) {
            // Mark method as verified and enabled
            method.markVerified();
            method.setEnabled(true);
            smsOtpAuthMethodRepository.save(convertToEntity(method));
            
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
        Optional<SmsOtpAuthenticationMethod> methodOpt = smsOtpAuthMethodRepository
                .findByUserIdAndPhoneNumber(userId, phoneNumber)
                .map(this::convertToModel);
        
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.OTP_NOT_FOUND, 
                    "No phone number registration found");
        }
        
        SmsOtpAuthenticationMethod method = methodOpt.get();
        
        // Check daily SMS limit
        if (!method.canSendSmsToday(smsOtpConfig.isDevMockSmsEnabled() ? 100 : 10)) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Daily SMS limit exceeded for phone number registration");
        }
        
        // Send OTP using authentication service
        boolean otpSent = smsOtpAuthService.sendOtp(phoneNumber, ipAddress, countryCode);
        
        if (otpSent) {
            method.markOtpSent();
            smsOtpAuthMethodRepository.save(convertToEntity(method));
            return true;
        } else {
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, 
                    "Failed to resend verification OTP");
        }
    }
    
    /**
     * Get SMS OTP authentication methods for a user
     */
    public List<SmsOtpAuthenticationMethod> getUserSmsOtpMethods(String userId) {
        return smsOtpAuthMethodRepository.findByUserIdAndEnabled(userId, true)
                .stream()
                .map(this::convertToModel)
                .toList();
    }
    
    /**
     * Get verified SMS OTP authentication methods for a user
     */
    public List<SmsOtpAuthenticationMethod> getVerifiedUserSmsOtpMethods(String userId) {
        return smsOtpAuthMethodRepository.findByUserIdAndVerified(userId, true)
                .stream()
                .map(this::convertToModel)
                .toList();
    }
    
    /**
     * Remove SMS OTP authentication method
     */
    public boolean removePhoneNumber(String userId, String phoneNumber) {
        log.info("Removing phone number for user: {}", userId);
        
        Optional<SmsOtpAuthenticationMethod> methodOpt = smsOtpAuthMethodRepository
                .findByUserIdAndPhoneNumber(userId, phoneNumber)
                .map(this::convertToModel);
        
        if (methodOpt.isPresent()) {
            smsOtpAuthMethodRepository.deleteById(methodOpt.get().getId());
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
    public Optional<SmsOtpAuthenticationMethod> getPrimarySmsOtpMethod(String userId) {
        return getVerifiedUserSmsOtpMethods(userId).stream().findFirst();
    }
    
    // === CONVERSION METHODS ===
    
    private SmsOtpAuthenticationMethod convertToModel(io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity entity) {
        // TODO: Implement proper entity to model conversion
        // This is a placeholder - implement actual conversion logic
        SmsOtpAuthenticationMethod model = new SmsOtpAuthenticationMethod();
        model.setId(entity.getMethodId());
        model.setUserId(entity.getUserId());
        model.setPhoneNumber(entity.getPhoneNumber());
        model.setCountryCode(entity.getCountryCode());
        model.setVerified(entity.isVerified());
        model.setEnabled(entity.isEnabled());
        model.setLastOtpSentAt(entity.getLastOtpSentAt());
        model.setDailySmsCount(entity.getDailySmsCount());
        model.setDailyCountResetAt(entity.getDailyCountResetAt());
        return model;
    }
    
    private io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity convertToEntity(SmsOtpAuthenticationMethod model) {
        // TODO: Implement proper model to entity conversion
        // This is a placeholder - implement actual conversion logic
        io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity entity = 
                new io.strategiz.data.auth.entity.smsotp.SmsOtpAuthenticationMethodEntity();
        entity.setMethodId(model.getId());
        entity.setUserId(model.getUserId());
        entity.setPhoneNumber(model.getPhoneNumber());
        entity.setCountryCode(model.getCountryCode());
        entity.setVerified(model.isVerified());
        entity.setEnabled(model.isEnabled());
        entity.setLastOtpSentAt(model.getLastOtpSentAt());
        entity.setDailySmsCount(model.getDailySmsCount());
        entity.setDailyCountResetAt(model.getDailyCountResetAt());
        return entity;
    }
}