package io.strategiz.service.auth.service.smsotp;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.featureflags.service.FeatureFlagService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.client.firebasesms.FirebaseSmsClient;
import io.strategiz.service.auth.config.SmsOtpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import io.strategiz.service.base.BaseService;

/**
 * SMS OTP Registration Service
 * 
 * Handles phone number registration and verification for user accounts.
 * This service manages the setup and verification of SMS OTP as an authentication method.
 * 
 * Use this during user signup/account setup to register phone numbers.
 */
@Service
public class SmsOtpRegistrationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }    
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

    @Autowired
    private FeatureFlagService featureFlagService;

    /**
     * Register a phone number for a user account
     * Sends verification OTP to confirm phone number ownership
     */
    public boolean registerPhoneNumber(String userId, String phoneNumber, String ipAddress, String countryCode) {
        log.info("Registering phone number for user: {} from IP: {}", userId, ipAddress);

        // Check if SMS OTP signup is enabled
        if (!featureFlagService.isSmsOtpSignupEnabled()) {
            log.warn("SMS OTP signup is disabled - rejecting registration for user: {}", userId);
            throw new StrategizException(AuthErrors.AUTH_METHOD_DISABLED,
                "SMS OTP signup is currently disabled");
        }

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

        log.info("Verifying Firebase token for phone: {} userId: {}", maskPhoneNumber(phoneNumber), userId);

        // Validate userId for registration flow
        if (isRegistration && (userId == null || userId.isBlank())) {
            log.error("User ID is required for SMS registration but was null or blank");
            throw new StrategizException(AuthErrors.USER_NOT_FOUND,
                "User ID is required for phone registration. Please complete profile creation first.");
        }

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
     * Finds the user by phone number and creates a session
     */
    private Map<String, Object> completeSmsAuthentication(String phoneNumber) {
        log.info("Completing SMS authentication for phone: {}", maskPhoneNumber(phoneNumber));

        // Find user by phone number using collection group query
        String userId = findUserIdByPhoneNumber(phoneNumber);

        if (userId == null) {
            log.warn("No user found with phone number: {}", maskPhoneNumber(phoneNumber));
            throw new StrategizException(AuthErrors.USER_NOT_FOUND,
                "Phone number not registered. Please sign up first.");
        }

        // Find the SMS OTP method and mark as used
        Optional<AuthenticationMethodEntity> methodOpt = findSmsOtpByPhoneNumber(userId, phoneNumber);
        if (methodOpt.isPresent()) {
            AuthenticationMethodEntity method = methodOpt.get();
            method.markAsUsed();
            authMethodRepository.saveForUser(userId, method);
        }

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

        log.info("SMS authentication completed for user: {}", userId);
        return tokenResult;
    }

    /**
     * Find user ID by phone number from authentication methods.
     * Searches all SMS_OTP authentication methods using collection group query.
     */
    private String findUserIdByPhoneNumber(String phoneNumber) {
        // Search all SMS_OTP authentication methods
        List<AuthenticationMethodEntity> allSmsMethods = authMethodRepository.findByType(AuthenticationMethodType.SMS_OTP);

        for (AuthenticationMethodEntity method : allSmsMethods) {
            String methodPhone = method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);

            // Normalize phone numbers for comparison
            String normalizedMethodPhone = normalizePhoneNumber(methodPhone);
            String normalizedSearchPhone = normalizePhoneNumber(phoneNumber);

            if (normalizedSearchPhone.equals(normalizedMethodPhone)) {
                Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
                if (Boolean.TRUE.equals(isVerified)) {
                    // userId is stored in metadata by the repository during collection group query
                    return method.getMetadataAsString("userId");
                }
            }
        }

        return null;
    }

    /**
     * Normalize phone number for comparison (removes non-digits, handles country code)
     */
    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        // Remove all non-digit characters
        String digits = phoneNumber.replaceAll("\\D", "");
        // Remove leading 1 if it's a US number with 11 digits
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }
        return digits;
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