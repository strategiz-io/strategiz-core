package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.smsotp.*;
import io.strategiz.service.auth.service.smsotp.SmsOtpAuthenticationService;
import io.strategiz.service.auth.service.smsotp.SmsOtpRegistrationService;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SMS OTP operations
 * 
 * This controller provides endpoints for sending, verifying, and checking the status
 * of SMS-based One-Time Passwords (OTP) using Firebase Phone Authentication.
 * 
 * Endpoints:
 * - POST /auth/smsotp/messages - Send SMS OTP to phone number (create message resource)
 * - POST /auth/smsotp/verifications - Verify SMS OTP code (create verification resource)
 * - GET /auth/smsotp/messages/{messageId} - Get SMS message status
 * 
 * Security features:
 * - Rate limiting (1 SMS per minute per phone number)
 * - Attempt limiting (5 verification attempts per OTP)
 * - IP address tracking for audit purposes
 * - Phone number validation (E.164 format)
 * - Automatic OTP expiration (5 minutes)
 */
@RestController
@RequestMapping("/auth/smsotp")
public class SmsOtpController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(SmsOtpController.class);
    
    @Autowired
    private SmsOtpAuthenticationService smsOtpAuthService;
    
    @Autowired
    private SmsOtpRegistrationService smsOtpRegistrationService;
    
    /**
     * Create SMS message resource - Send SMS OTP to the specified phone number
     * 
     * POST /auth/smsotp/messages
     * 
     * @param request SMS OTP send request containing phone number and optional parameters
     * @param httpRequest HTTP request for extracting client IP
     * @return Response with message resource details including messageId
     */
    @PostMapping("/messages")
    public ResponseEntity<SmsOtpSendResponse> sendSmsOtp(
            @Valid @RequestBody SmsOtpSendRequest request,
            HttpServletRequest httpRequest) {
        
        logRequest("createSmsMessage", request.getMaskedPhoneNumber());
        
        try {
            // Extract IP address from request if not provided
            String ipAddress = request.ipAddress();
            if (ipAddress == null || ipAddress.isBlank()) {
                ipAddress = extractClientIpAddress(httpRequest);
            }
            
            // Validate phone number format
            validatePhoneNumberFormat(request.phoneNumber());
            
            // Check rate limiting
            if (!smsOtpAuthService.canSendOtp(request.phoneNumber())) {
                throw new StrategizException(AuthErrors.OTP_RATE_LIMITED, 
                        "Too many SMS requests. Please wait before requesting another OTP.");
            }
            
            // Send SMS OTP for authentication
            boolean success = smsOtpAuthService.sendOtp(request.phoneNumber(), ipAddress, request.countryCode());
            
            if (success) {
                SmsOtpSendResponse response = SmsOtpSendResponse.success(
                    request.phoneNumber(),
                    request.getMaskedPhoneNumber(),
                    generateOtpId(request.phoneNumber()),
                    300 // 5 minutes in seconds
                );
                
                logRequestSuccess("createSmsMessage", request.getMaskedPhoneNumber(), "SMS message created successfully");
                return createCleanResponse(response);
            } else {
                throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send SMS OTP");
            }
            
        } catch (StrategizException e) {
            log.warn("SMS message creation failed for {}: {}", request.getMaskedPhoneNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating SMS message for {}: {}", request.getMaskedPhoneNumber(), e.getMessage(), e);
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to create SMS message");
        }
    }
    
    /**
     * Create verification resource - Verify SMS OTP code for the specified phone number
     * 
     * POST /auth/smsotp/verifications
     * 
     * @param request SMS OTP verification request containing phone number and OTP code
     * @return Response with verification result and details
     */
    @PostMapping("/verifications")
    public ResponseEntity<SmsOtpVerifyResponse> verifySmsOtp(
            @Valid @RequestBody SmsOtpVerifyRequest request) {
        
        logRequest("createSmsVerification", maskPhoneNumber(request.phoneNumber()));
        
        try {
            // Validate phone number format
            validatePhoneNumberFormat(request.phoneNumber());
            
            // Validate OTP code format
            validateOtpCodeFormat(request.otpCode());
            
            // Verify OTP for authentication
            boolean isValid = smsOtpAuthService.verifyOtp(request.phoneNumber(), request.otpCode());
            
            if (isValid) {
                SmsOtpVerifyResponse response = SmsOtpVerifyResponse.success(request.phoneNumber());
                logRequestSuccess("createSmsVerification", maskPhoneNumber(request.phoneNumber()), "SMS verification created successfully");
                return createCleanResponse(response);
            } else {
                SmsOtpVerifyResponse response = SmsOtpVerifyResponse.failure(
                    request.phoneNumber(), 
                    "Invalid or expired SMS OTP code"
                );
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (StrategizException e) {
            log.warn("SMS verification creation failed for {}: {}", maskPhoneNumber(request.phoneNumber()), e.getMessage());
            
            // Map specific errors to appropriate HTTP status codes
            if (e.getErrorCode().equals(AuthErrors.OTP_EXPIRED.name())) {
                SmsOtpVerifyResponse response = SmsOtpVerifyResponse.failure(request.phoneNumber(), e.getMessage());
                return ResponseEntity.badRequest().body(response);
            } else if (e.getErrorCode().equals(AuthErrors.OTP_MAX_ATTEMPTS_EXCEEDED.name())) {
                SmsOtpVerifyResponse response = SmsOtpVerifyResponse.failure(request.phoneNumber(), e.getMessage());
                return ResponseEntity.status(429).body(response); // Too Many Requests
            } else if (e.getErrorCode().equals(AuthErrors.OTP_NOT_FOUND.name())) {
                SmsOtpVerifyResponse response = SmsOtpVerifyResponse.failure(request.phoneNumber(), e.getMessage());
                return ResponseEntity.status(404).body(response); // Not Found
            }
            
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating SMS verification for {}: {}", maskPhoneNumber(request.phoneNumber()), e.getMessage(), e);
            throw new StrategizException(AuthErrors.VERIFICATION_FAILED, "Failed to create SMS verification");
        }
    }
    
    /**
     * Get SMS message status by message ID
     * 
     * GET /auth/smsotp/messages/{messageId}
     * 
     * @param messageId The message ID returned when creating the SMS message
     * @return Response containing current message status and remaining attempts
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<SmsOtpStatusResponse> getMessageStatus(@PathVariable String messageId) {
        
        logRequest("getSmsMessageStatus", messageId);
        
        try {
            // Extract phone number from messageId (assuming messageId contains phone hash)
            String phoneNumber = extractPhoneNumberFromMessageId(messageId);
            
            if (phoneNumber == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Validate phone number format
            validatePhoneNumberFormat(phoneNumber);
            
            // Get OTP status for authentication
            String status = smsOtpAuthService.getOtpStatus(phoneNumber);
            int remainingAttempts = smsOtpAuthService.getRemainingAttempts(phoneNumber);
            
            // Calculate expiration time (approximate)
            String expiresAt = null;
            if ("PENDING".equals(status)) {
                expiresAt = java.time.LocalDateTime.now().plusMinutes(5)
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            SmsOtpStatusResponse response = new SmsOtpStatusResponse(
                phoneNumber,
                status,
                expiresAt,
                remainingAttempts
            );
            
            logRequestSuccess("getSmsMessageStatus", messageId, status);
            return createCleanResponse(response);
            
        } catch (StrategizException e) {
            if (e.getErrorCode().equals(AuthErrors.OTP_NOT_FOUND.name())) {
                log.debug("No message found for ID: {}", messageId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error checking SMS message status for {}: {}", messageId, e.getMessage(), e);
            throw new StrategizException(AuthErrors.SMS_SERVICE_UNAVAILABLE, "Failed to check SMS message status");
        }
    }
    
    /**
     * Validate phone number format (E.164)
     */
    private void validatePhoneNumberFormat(String phoneNumber) {
        if (phoneNumber == null || !phoneNumber.matches("^\\+[1-9]\\d{1,14}$")) {
            throw new StrategizException(AuthErrors.INVALID_PHONE_NUMBER, 
                    "Phone number must be in E.164 format (e.g., +1234567890)");
        }
    }
    
    /**
     * Validate OTP code format (6 digits)
     */
    private void validateOtpCodeFormat(String otpCode) {
        if (otpCode == null || !otpCode.matches("^\\d{6}$")) {
            throw new StrategizException(AuthErrors.INVALID_OTP_FORMAT, 
                    "OTP code must be exactly 6 digits");
        }
    }
    
    /**
     * Extract client IP address from HTTP request
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get the first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Mask phone number for security in logs
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-***-****";
        }
        
        String countryCode = phoneNumber.substring(0, phoneNumber.length() - 7);
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return countryCode + "***" + lastFour;
    }
    
    /**
     * Generate a unique OTP ID for tracking purposes
     */
    private String generateOtpId(String phoneNumber) {
        long timestamp = System.currentTimeMillis();
        int hash = Math.abs(phoneNumber.hashCode());
        return String.format("sms_%d_%d", timestamp, hash);
    }
    
    /**
     * Extract phone number from message ID (reverse of generateOtpId)
     * This is a simplified implementation - in production you'd use a proper lookup table
     */
    private String extractPhoneNumberFromMessageId(String messageId) {
        // For now, this is a placeholder since we don't have a reverse mapping
        // In production, you'd store messageId -> phoneNumber mappings in a cache/database
        // For this demo, we'll return null to indicate not found
        return null;
    }
}