package io.strategiz.service.auth.service.smsotp;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
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
import java.util.Map;
import java.util.HashMap;
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
    
    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;
    
    @Autowired
    private SmsOtpAuthenticationService smsOtpAuthService;
    
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
        smsOtpMethod.setAuthenticationMethod(AuthenticationMethodType.SMS_OTP);
        smsOtpMethod.setIsActive(false); // Enable after verification
        
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
        boolean otpSent = smsOtpAuthService.sendRegistrationOtp(userId, phoneNumber, countryCode);
        
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
     * Verify phone number ownership with OTP code and return tokens
     * Completes the phone number registration process with authentication
     *
     * @param userId User ID
     * @param accessToken Current access token (if adding SMS OTP to existing session)
     * @param phoneNumber Phone number being verified
     * @param otpCode OTP code to verify
     * @return Map containing success status and updated tokens, or null if failed
     */
    public java.util.Map<String, Object> verifySmsOtpWithTokenUpdate(String userId, String accessToken,
                                                                     String phoneNumber, String otpCode) {
        log.info("Verifying SMS OTP registration for user: {}", userId);

        // Find the SMS OTP authentication method
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);

        if (methodOpt.isEmpty()) {
            log.warn("No phone number registration found for user: {}", userId);
            return null;
        }

        AuthenticationMethodEntity method = methodOpt.get();

        Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
        if (Boolean.TRUE.equals(isVerified)) {
            log.info("Phone number already verified for user: {}", userId);
            // If already verified, still return success with updated tokens
        }

        // Verify the OTP code using authentication service
        boolean otpValid = smsOtpAuthService.verifyOtp(null, phoneNumber, otpCode);

        if (!otpValid) {
            log.warn("Invalid OTP for phone number registration - user: {}", userId);
            return null;
        }

        // Mark method as verified and enabled
        method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, true);
        method.setIsActive(true);
        method.markAsUsed();
        authMethodRepository.saveForUser(userId, method);

        // Update the session with SMS OTP as an authenticated method
        // This generates a new PASETO token with ACR=2 and SMS OTP in auth methods
        java.util.Map<String, Object> authResult = sessionAuthBusiness.addAuthenticationMethod(
            accessToken,
            "sms_otp",
            2 // ACR level 2 for MFA with SMS OTP
        );

        log.info("SMS OTP enabled for user {} with updated session", userId);
        return authResult;
    }
    
    /**
     * Verify phone number ownership with OTP code
     * Completes the phone number registration process
     * @deprecated Use verifySmsOtpWithTokenUpdate for token management
     */
    @Deprecated
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
        boolean otpValid = smsOtpAuthService.verifyOtp(null, phoneNumber, otpCode);
        
        if (otpValid) {
            // Mark method as verified and enabled
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, true);
            method.setIsActive(true);
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
        boolean otpSent = smsOtpAuthService.sendRegistrationOtp(userId, phoneNumber, countryCode);
        
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
    
    /**
     * Verify Firebase phone authentication token and complete registration/authentication
     * 
     * This method validates the Firebase ID token from client-side phone authentication
     * and completes the SMS OTP process server-side.
     * 
     * @param firebaseIdToken The Firebase ID token from client
     * @param userId The user ID (for registration)
     * @param phoneNumber The phone number being verified
     * @param isRegistration True for signup, false for signin
     * @return Map containing session tokens on success
     */
    public Map<String, Object> verifyFirebaseTokenAndComplete(
            String firebaseIdToken,
            String userId,
            String phoneNumber,
            boolean isRegistration) {

        log.info("Verifying Firebase token for phone: {}", maskPhoneNumber(phoneNumber));

        try {
            // Verify the Firebase ID token
            boolean tokenValid = firebaseSmsClient.verifyFirebaseIdToken(firebaseIdToken, phoneNumber);

            if (!tokenValid) {
                log.warn("Firebase ID token validation failed for phone: {}", maskPhoneNumber(phoneNumber));
                throw new StrategizException(AuthErrors.INVALID_CREDENTIALS,
                    "Invalid Firebase authentication token");
            }

            if (isRegistration) {
                // Complete phone number registration
                return completePhoneRegistration(userId, phoneNumber);
            } else {
                // Complete phone authentication (signin)
                return completeSmsAuthentication(phoneNumber);
            }

        } catch (StrategizException e) {
            // Re-throw StrategizException without wrapping
            throw e;
        } catch (Exception e) {
            log.error("Firebase token verification failed: {}", e.getMessage(), e);
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED,
                "Phone verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Complete phone number registration after Firebase verification
     */
    private Map<String, Object> completePhoneRegistration(String userId, String phoneNumber) {
        log.info("Completing phone registration for user: {}", userId);
        
        // Find or create the SMS OTP method
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);
        
        AuthenticationMethodEntity method;
        if (methodOpt.isEmpty()) {
            // Create new SMS OTP authentication method
            method = new AuthenticationMethodEntity();
            method.setAuthenticationMethod(AuthenticationMethodType.SMS_OTP);
            method.setIsActive(true);
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER, phoneNumber);
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.COUNTRY_CODE, "US");
        } else {
            method = methodOpt.get();
        }
        
        // Mark as verified
        method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, true);
        method.setIsActive(true);
        
        // Save the method
        authMethodRepository.saveForUser(userId, method);
        
        // Generate session tokens
        SessionAuthBusiness.TokenPair tokens = sessionAuthBusiness.createAuthenticationTokenPair(
            userId,
            List.of("SMS_OTP"),
            "sms",
            null, // deviceId
            null  // ipAddress
        );
        
        Map<String, Object> tokenResult = new HashMap<>();
        tokenResult.put("accessToken", tokens.accessToken());
        tokenResult.put("refreshToken", tokens.refreshToken());
        tokenResult.put("userId", userId);
        
        log.info("Phone registration completed for user: {}", userId);
        return tokenResult;
    }
    
    /**
     * Complete SMS authentication after Firebase verification
     */
    private Map<String, Object> completeSmsAuthentication(String phoneNumber) {
        log.info("Completing SMS authentication for phone: {}", maskPhoneNumber(phoneNumber));
        
        // Find user by phone number - search across all users
        // This is a simplified implementation - in production you might want a more efficient search
        String userId = null;
        
        // Since we can't directly search by phone number, we need to find the user
        // In a real implementation, you might have a phone-to-user mapping
        // For now, we'll throw an error suggesting to pass userId
        
        throw new StrategizException(AuthErrors.USER_NOT_FOUND, 
            "Phone-only authentication not yet implemented. Please use registration flow.");
    }
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + 
               phoneNumber.substring(phoneNumber.length() - 2);
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