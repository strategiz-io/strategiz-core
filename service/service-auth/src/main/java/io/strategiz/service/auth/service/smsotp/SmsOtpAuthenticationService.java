package io.strategiz.service.auth.service.smsotp;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.client.firebasesms.FirebaseSmsClient;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.config.SmsOtpConfig;
import io.strategiz.service.auth.exception.AuthErrors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import io.strategiz.service.base.BaseService;

/**
 * SMS OTP Authentication Service
 * 
 * Handles SMS OTP for both registration and authentication flows.
 * Features rate limiting, attempt limiting, and automatic expiration.
 */
@Service
public class SmsOtpAuthenticationService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-auth";
    }    
    // Temporary in-memory storage for OTP codes (in production, use Redis or similar)
    private final Map<String, OtpSession> otpSessions = new ConcurrentHashMap<>();
    
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
        
        // Check rate limiting
        if (!canSendOtp(phoneNumber)) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Too many SMS requests. Please wait before requesting another OTP.");
        }
        
        // Generate and send OTP
        String otpCode = generateOtpCode();
        String sessionId = generateSessionId();
        
        // Store OTP session
        OtpSession session = new OtpSession(sessionId, userId, phoneNumber, otpCode, countryCode, false);
        otpSessions.put(sessionId, session);
        
        // Send SMS
        boolean sent = sendSms(phoneNumber, otpCode, countryCode);
        
        if (!sent) {
            otpSessions.remove(sessionId);
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
        
        // Find all users with SMS_OTP authentication methods
        // Note: In a real implementation, you'd have a more efficient way to search by phone
        // For now, we'll search through all SMS_OTP methods to find the matching phone
        String userId = findUserIdByPhoneNumber(phoneNumber);
        
        if (userId == null) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, 
                    "Phone number not registered. Please sign up first.");
        }
        
        // Delegate to the existing method
        return sendAuthenticationOtp(userId, phoneNumber, countryCode);
    }
    
    /**
     * Find user ID by phone number
     * Note: This is inefficient - in production, you'd want an index on phone numbers
     */
    private String findUserIdByPhoneNumber(String phoneNumber) {
        // This is a temporary implementation
        // In production, you'd have a proper index or lookup table
        // For now, check if we have any active OTP sessions for this phone
        OtpSession existingSession = findSessionByPhoneNumber(phoneNumber);
        if (existingSession != null && existingSession.userId != null) {
            // Verify the user actually has this phone registered
            List<AuthenticationMethodEntity> methods = authMethodRepository
                .findByUserIdAndType(existingSession.userId, AuthenticationMethodType.SMS_OTP);
            for (AuthenticationMethodEntity method : methods) {
                String methodPhone = method.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
                if (phoneNumber.equals(methodPhone)) {
                    Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
                    if (Boolean.TRUE.equals(isVerified)) {
                        return existingSession.userId;
                    }
                }
            }
        }
        
        // If no session exists, we can't find the user without searching all users
        // This would require a different repository method or database query
        // For demo purposes, return null (phone not found)
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
        List<AuthenticationMethodEntity> methods = authMethodRepository.findByUserIdAndType(userId, AuthenticationMethodType.SMS_OTP);
        
        // Filter to find the matching phone number
        Optional<AuthenticationMethodEntity> methodOpt = methods.stream()
            .filter(m -> phoneNumber.equals(m.getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER)))
            .findFirst();
            
        if (methodOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, 
                    "Phone number not registered for this user");
        }
        
        AuthenticationMethodEntity method = methodOpt.get();
        
        // Check if phone is verified
        Boolean isVerified = (Boolean) method.getMetadata(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED);
        if (!Boolean.TRUE.equals(isVerified)) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, 
                    "Phone number not verified");
        }
        
        // Check rate limiting
        if (!canSendOtp(phoneNumber)) {
            throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                    "Too many SMS requests. Please wait before requesting another OTP.");
        }
        
        // Generate and send OTP
        String otpCode = generateOtpCode();
        String sessionId = generateSessionId();
        
        // Store OTP session
        OtpSession session = new OtpSession(sessionId, userId, phoneNumber, otpCode, countryCode, true);
        otpSessions.put(sessionId, session);
        
        // Send SMS
        boolean sent = sendSms(phoneNumber, otpCode, countryCode);
        
        if (!sent) {
            otpSessions.remove(sessionId);
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send SMS OTP");
        }
        
        log.info("Authentication OTP sent successfully to {}", maskPhoneNumber(phoneNumber));
        return sessionId; // Return session ID for tracking
    }
    
    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String sessionId, String phoneNumber, String otpCode) {
        log.debug("Verifying OTP for session: {} phone: {}", sessionId, maskPhoneNumber(phoneNumber));
        
        // Find OTP session
        OtpSession session = otpSessions.get(sessionId);
        if (session == null) {
            // Try to find by phone number if session ID not provided
            session = findSessionByPhoneNumber(phoneNumber);
            if (session == null) {
                log.warn("No OTP session found for phone: {}", maskPhoneNumber(phoneNumber));
                return false;
            }
        }
        
        // Check if session is expired
        if (session.isExpired()) {
            otpSessions.remove(session.sessionId);
            log.warn("OTP session expired for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }
        
        // Check attempt count
        if (session.attemptCount >= 5) {
            otpSessions.remove(session.sessionId);
            log.warn("Too many OTP verification attempts for phone: {}", maskPhoneNumber(phoneNumber));
            throw new StrategizException(AuthErrors.OTP_MAX_ATTEMPTS_EXCEEDED, 
                    "Too many verification attempts");
        }
        
        // Verify OTP code
        session.attemptCount++;
        if (!session.otpCode.equals(otpCode)) {
            log.warn("Invalid OTP code for phone: {}", maskPhoneNumber(phoneNumber));
            return false;
        }
        
        // Mark as verified and remove session
        session.verified = true;
        otpSessions.remove(session.sessionId);
        
        log.info("OTP verified successfully for phone: {}", maskPhoneNumber(phoneNumber));
        return true;
    }
    
    /**
     * Authenticate user with SMS OTP and return tokens
     */
    public Map<String, Object> authenticateWithOtp(String phoneNumber, String otpCode, String sessionId) {
        log.info("Authenticating user with SMS OTP for phone: {}", maskPhoneNumber(phoneNumber));
        
        // Verify OTP
        if (!verifyOtp(sessionId, phoneNumber, otpCode)) {
            return null;
        }
        
        // Get user ID from the OTP session
        OtpSession session = otpSessions.get(sessionId);
        if (session == null) {
            session = findSessionByPhoneNumber(phoneNumber);
            if (session == null) {
                log.warn("No OTP session found for authentication");
                return null;
            }
        }
        
        String userId = session.userId;
        
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
            String message = String.format("Your Strategiz verification code is: %s. Valid for %d minutes.", 
                otpCode, smsOtpConfig.getOtpExpiryMinutes());
            
            // Send via Firebase
            return firebaseSmsClient.sendSms(phoneNumber, message, countryCode);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            return false;
        }
    }
    
    private String generateOtpCode() {
        // Generate 6-digit OTP
        int code = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(code);
    }
    
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    private boolean canSendOtp(String phoneNumber) {
        // Simple rate limiting: check if last OTP was sent more than 1 minute ago
        OtpSession existingSession = findSessionByPhoneNumber(phoneNumber);
        if (existingSession != null) {
            long minutesSinceCreated = ChronoUnit.MINUTES.between(existingSession.createdAt, Instant.now());
            return minutesSinceCreated >= 1;
        }
        return true;
    }
    
    private OtpSession findSessionByPhoneNumber(String phoneNumber) {
        return otpSessions.values().stream()
            .filter(s -> s.phoneNumber.equals(phoneNumber) && !s.isExpired())
            .findFirst()
            .orElse(null);
    }
    
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Clean up expired sessions periodically
     */
    public void cleanupExpiredSessions() {
        otpSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    // Inner class for OTP session
    private static class OtpSession {
        final String sessionId;
        final String userId;
        final String phoneNumber;
        final String otpCode;
        final String countryCode;
        final Instant createdAt;
        final boolean isAuthentication; // true for auth, false for registration
        int attemptCount = 0;
        boolean verified = false;
        
        OtpSession(String sessionId, String userId, String phoneNumber, String otpCode, 
                   String countryCode, boolean isAuthentication) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.phoneNumber = phoneNumber;
            this.otpCode = otpCode;
            this.countryCode = countryCode;
            this.createdAt = Instant.now();
            this.isAuthentication = isAuthentication;
        }
        
        boolean isExpired() {
            // Expire after 5 minutes
            return ChronoUnit.MINUTES.between(createdAt, Instant.now()) > 5;
        }
    }
}