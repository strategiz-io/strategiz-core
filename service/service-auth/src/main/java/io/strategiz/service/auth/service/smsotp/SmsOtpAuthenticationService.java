package io.strategiz.service.auth.service.smsotp;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.client.firebasesms.FirebaseSmsClient;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.SmsOtpCodeEntity;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.auth.repository.SmsOtpCodeRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.config.SmsOtpConfig;
import io.strategiz.service.auth.exception.AuthErrors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import io.strategiz.service.base.BaseService;

/**
 * SMS OTP Authentication Service
 *
 * Handles SMS OTP for both registration and authentication flows.
 * Features rate limiting, attempt limiting, and automatic expiration.
 *
 * OTP sessions are stored in Firestore for production-ready distributed deployment.
 * Industry standard: 5-minute expiration, 5 max attempts, 60-second rate limit.
 */
@Service
public class SmsOtpAuthenticationService extends BaseService {

    private static final String SYSTEM_USER = "system";

    @Override
    protected String getModuleName() {
        return "service-auth";
    }

    @Autowired
    private SmsOtpCodeRepository smsOtpCodeRepository;

    @Autowired
    private SmsOtpConfig smsOtpConfig;

    @Autowired
    private FirebaseSmsClient firebaseSmsClient;

    @Autowired
    private AuthenticationMethodRepository authMethodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionAuthBusiness sessionAuthBusiness;
    
    /**
     * Send OTP for registration (new phone number)
     */
    public boolean sendRegistrationOtp(String userId, String phoneNumber, String countryCode) {
        log.info("Sending registration OTP to phone: {} for user: {}", maskPhoneNumber(phoneNumber), userId);

        // Check rate limiting using Firestore
        if (!smsOtpCodeRepository.canSendOtp(phoneNumber)) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED,
                    "Too many SMS requests. Please wait before requesting another OTP.");
        }

        // Generate OTP code
        String otpCode = generateOtpCode();

        // Create session entity with hashed OTP code
        SmsOtpCodeEntity session = new SmsOtpCodeEntity(phoneNumber, countryCode, hashOtpCode(otpCode),
                SmsOtpCodeEntity.Purpose.REGISTRATION, smsOtpConfig.getOtpExpiryMinutes());
        session.setUserId(userId);

        // Save to Firestore
        session = smsOtpCodeRepository.save(session, SYSTEM_USER);

        // Send SMS
        boolean sent = sendSms(phoneNumber, otpCode, countryCode);

        if (!sent) {
            // Cleanup failed session
            smsOtpCodeRepository.deleteById(session.getId());
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send SMS OTP");
        }

        log.info("Registration OTP sent successfully to {}", maskPhoneNumber(phoneNumber));
        return true;
    }
    
    /**
     * Send OTP for authentication by phone number only (finds user automatically)
     * This is used on the sign-in page where users only enter their phone number
     */
    public String sendAuthenticationOtpByPhone(String phoneNumber, String countryCode) {
        log.info("Sending authentication OTP to phone: {} (looking up user)", maskPhoneNumber(phoneNumber));

        // Find user by phone number from authentication methods
        String userId = findUserIdByPhoneNumber(phoneNumber);

        if (userId == null) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER,
                    "Phone number not registered. Please sign up first.");
        }

        // Delegate to the existing method
        return sendAuthenticationOtp(userId, phoneNumber, countryCode);
    }

    /**
     * Find user ID by phone number from authentication methods.
     * Searches the authentication_methods collection for verified SMS_OTP methods.
     */
    private String findUserIdByPhoneNumber(String phoneNumber) {
        // First check if there's an existing session in Firestore for this phone
        Optional<SmsOtpCodeEntity> existingSession = smsOtpCodeRepository.findActiveByPhoneNumber(phoneNumber);
        if (existingSession.isPresent() && existingSession.get().getUserId() != null) {
            String userId = existingSession.get().getUserId();
            // Verify the user actually has this phone registered
            List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId,
                    AuthenticationMethodType.SMS_OTP);
            for (AuthenticationMethodEntity method : methods) {
                String methodPhone = method
                    .getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
                if (phoneNumber.equals(methodPhone)) {
                    Boolean isVerified = (Boolean) method
                        .getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
                    if (Boolean.TRUE.equals(isVerified)) {
                        return userId;
                    }
                }
            }
        }

        // Search all SMS_OTP authentication methods for this phone number
        // This uses the repository's search capability with collection group query
        List<AuthenticationMethodEntity> allSmsMethods = authMethodRepository
            .findByType(AuthenticationMethodType.SMS_OTP);
        for (AuthenticationMethodEntity method : allSmsMethods) {
            String methodPhone = method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
            if (phoneNumber.equals(methodPhone)) {
                Boolean isVerified = (Boolean) method
                    .getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
                if (Boolean.TRUE.equals(isVerified)) {
                    // userId is stored in metadata by the repository during collection group query
                    return method.getMetadataAsString("userId");
                }
            }
        }

        log.warn("Unable to find user for phone number: {}", maskPhoneNumber(phoneNumber));
        return null;
    }
    
    /**
     * Send OTP for authentication (existing verified phone)
     * Note: This requires the userId to be provided since we can't search across users
     */
    public String sendAuthenticationOtp(String userId, String phoneNumber, String countryCode) {
        log.info("Sending authentication OTP to phone: {} for user: {}", maskPhoneNumber(phoneNumber), userId);

        // Find the SMS OTP method for this user
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId,
                AuthenticationMethodType.SMS_OTP);

        // Filter to find the matching phone number
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(m -> phoneNumber
                .equals(m.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER)))
            .findFirst();

        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, "Phone number not registered for this user");
        }

        AuthenticationMethodEntity method = methodOpt.get();

        // Check if phone is verified
        Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
        if (!Boolean.TRUE.equals(isVerified)) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, "Phone number not verified");
        }

        // Check rate limiting using Firestore
        if (!smsOtpCodeRepository.canSendOtp(phoneNumber)) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED,
                    "Too many SMS requests. Please wait before requesting another OTP.");
        }

        // Generate OTP code
        String otpCode = generateOtpCode();

        // Create session entity with hashed OTP code
        SmsOtpCodeEntity session = new SmsOtpCodeEntity(phoneNumber, countryCode, hashOtpCode(otpCode),
                SmsOtpCodeEntity.Purpose.AUTHENTICATION, smsOtpConfig.getOtpExpiryMinutes());
        session.setUserId(userId);

        // Save to Firestore
        session = smsOtpCodeRepository.save(session, SYSTEM_USER);

        // Send SMS
        boolean sent = sendSms(phoneNumber, otpCode, countryCode);

        if (!sent) {
            // Cleanup failed session
            smsOtpCodeRepository.deleteById(session.getId());
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send SMS OTP");
        }

        log.info("Authentication OTP sent successfully to {}", maskPhoneNumber(phoneNumber));
        return session.getId(); // Return session ID for tracking
    }
    
    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String sessionId, String phoneNumber, String otpCode) {
        log.debug("Verifying OTP for session: {} phone: {}", sessionId, maskPhoneNumber(phoneNumber));

        // Find OTP session from Firestore
        Optional<SmsOtpCodeEntity> sessionOpt;
        if (sessionId != null && !sessionId.isEmpty()) {
            sessionOpt = smsOtpCodeRepository.findById(sessionId);
        }
        else {
            // Try to find by phone number if session ID not provided
            sessionOpt = smsOtpCodeRepository.findActiveByPhoneNumber(phoneNumber);
        }

        if (sessionOpt.isEmpty()) {
            log.warn("No OTP session found for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        SmsOtpCodeEntity session = sessionOpt.get();

        // Check if session is expired
        if (session.isExpired()) {
            smsOtpCodeRepository.deleteById(session.getId());
            log.warn("OTP session expired for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Check attempt count
        if (session.hasExceededMaxAttempts()) {
            smsOtpCodeRepository.deleteById(session.getId());
            log.warn("Too many OTP verification attempts for phone: {}", maskPhoneNumber(phoneNumber));
            throw new StrategizException(AuthErrors.OTP_MAX_ATTEMPTS_EXCEEDED, "Too many verification attempts");
        }

        // Increment attempt count and save
        session.incrementAttempts();

        // Verify OTP code by comparing hashes
        String providedCodeHash = hashOtpCode(otpCode);
        if (!session.getCodeHash().equals(providedCodeHash)) {
            // Save updated attempt count
            smsOtpCodeRepository.update(session, SYSTEM_USER);
            log.warn("Invalid OTP code for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }

        // Mark as verified and delete session (successful verification)
        session.markVerified();
        smsOtpCodeRepository.deleteById(session.getId());

        log.info("OTP verified successfully for phone: {}", maskPhoneNumber(phoneNumber));
        return true;
    }
    
    /**
     * Authenticate user with SMS OTP and return tokens
     */
    public Map<String, Object> authenticateWithOtp(String phoneNumber, String otpCode, String sessionId) {
        log.info("Authenticating user with SMS OTP for phone: {}", maskPhoneNumber(phoneNumber));

        // Find session first to get userId before verification deletes it
        Optional<SmsOtpCodeEntity> sessionOpt;
        if (sessionId != null && !sessionId.isEmpty()) {
            sessionOpt = smsOtpCodeRepository.findById(sessionId);
        }
        else {
            sessionOpt = smsOtpCodeRepository.findActiveByPhoneNumber(phoneNumber);
        }

        if (sessionOpt.isEmpty()) {
            log.warn("No OTP session found for authentication");
            return null;
        }

        String userId = sessionOpt.get().getUserId();

        // Verify OTP (this will delete the session on success)
        if (!verifyOtp(sessionId, phoneNumber, otpCode)) {
            return null;
        }
        
        // Get user for email
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for SMS OTP authentication: {}", userId);
            return null;
        }
        UserEntity user = userOpt.get();
        
        // Find the authentication method for marking as used
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.SMS_OTP);
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(m -> phoneNumber.equals(m.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER)))
            .findFirst();
        
        // Mark method as recently used if found
        if (methodOpt.isPresent()) {
            AuthenticationMethodEntity method = methodOpt.get();
            method.setLastUsedAt(Instant.now());
            method.putMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED, true);
            // Store last verified time in metadata directly (without using a constant that doesn't exist)
            method.putMetadata("lastVerifiedAt", Instant.now().toString());
            authMethodRepository.saveForUser(userId, method);
        }
        
        // Create session and tokens using SessionAuthBusiness (same as TOTP)
        SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
            userId,                                                      // userId
            user.getProfile().getEmail(),                                // userEmail
            java.util.List.of("SMS_OTP"),                               // authenticationMethods  
            false,                                                       // isPartialAuth
            "sms-otp-auth-device",                                      // deviceId
            null,                                                        // deviceFingerprint
            null,                                                        // ipAddress
            null,                                                        // userAgent
            false // demoMode
        );
        SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
        
        // Return proper authentication tokens
        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("userId", userId);
        authResponse.put("accessToken", authResult.accessToken());
        authResponse.put("refreshToken", authResult.refreshToken());
        authResponse.put("identityToken", authResult.accessToken()); // Use access token as identity token for now
        authResponse.put("authMethod", "sms_otp");
        authResponse.put("authenticated", true);
        authResponse.put("phoneNumber", maskPhoneNumber(phoneNumber));
        
        log.info("User authenticated successfully with SMS OTP: {}", userId);
        return authResponse;
    }
    
    // Helper methods

    private boolean sendSms(String phoneNumber, String otpCode, String countryCode) {
        try {
            // In development, log the OTP instead of sending SMS
            if (smsOtpConfig.isDevMockSmsEnabled()) {
                log.info("MOCK SMS OTP: {} to phone: {}", otpCode, phoneNumber);
                return true;
            }

            // Format message
            String message = String.format("Your Strategiz verification code is: %s. Valid for %d minutes.", otpCode,
                    smsOtpConfig.getOtpExpiryMinutes());

            // Send via Firebase
            return firebaseSmsClient.sendSms(phoneNumber, message, countryCode);
        }
        catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }

    private String generateOtpCode() {
        // Generate 6-digit OTP
        int code = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(code);
    }

    /**
     * Hash OTP code using SHA-256 for secure storage.
     * Never store plain-text OTP codes in the database.
     */
    private String hashOtpCode(String otpCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otpCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }

    /**
     * Scheduled cleanup of expired SMS OTP sessions.
     * Runs every 5 minutes to remove expired sessions from Firestore.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredSessions() {
        try {
            int deleted = smsOtpCodeRepository.deleteExpired();
            if (deleted > 0) {
                log.info("Cleaned up {} expired SMS OTP sessions", deleted);
            }
        }
        catch (Exception e) {
            log.error("Failed to cleanup expired SMS OTP sessions: {}", e.getMessage());
        }
    }

}
