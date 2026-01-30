package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.smsotp.SmsOtpRegistrationService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for SMS OTP registration (phone number setup)
 * 
 * Handles phone number registration and verification for new users or
 * users adding SMS OTP as an authentication method.
 */
@RestController
@RequestMapping("/v1/auth/sms-otp")
public class SmsOtpRegistrationController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(SmsOtpRegistrationController.class);

    @Autowired
    private SmsOtpRegistrationService smsOtpRegistrationService;

    @Autowired
    private CookieUtil cookieUtil;
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }
    
    /**
     * Register a phone number and send verification OTP
     * 
     * POST /auth/sms-otp/registrations
     * 
     * @param request Registration request with phone number and user details
     * @return Response with registration status
     */
    @PostMapping("/registrations")
    public ResponseEntity<Map<String, Object>> registerPhoneNumber(
            @RequestBody @Valid RegistrationRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        
        logRequest("registerPhoneNumber", request.phoneNumber);
        
        // Default IP if not provided
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "127.0.0.1";
        }
        
        // Register phone number and send OTP
        boolean sent = smsOtpRegistrationService.registerPhoneNumber(
            request.userId,
            request.phoneNumber,
            ipAddress,
            request.countryCode != null ? request.countryCode : "US"
        );
        
        if (!sent) {
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to send verification SMS");
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "phoneNumber", maskPhoneNumber(request.phoneNumber),
            "userId", request.userId,
            "registrationId", "sms-" + System.currentTimeMillis(),
            "message", "Verification code sent to your phone"
        );
        
        logRequestSuccess("registerPhoneNumber", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Verify phone number with OTP code
     * 
     * PUT /auth/sms-otp/registrations/{registrationId}
     * 
     * @param registrationId The registration ID (can be any value, kept for consistency)
     * @param request Verification request with OTP code
     * @return Response with verification status and updated tokens
     */
    @PutMapping("/registrations/{registrationId}")
    public ResponseEntity<Map<String, Object>> verifyPhoneNumber(
            @PathVariable String registrationId,
            @RequestBody @Valid VerificationRequest request,
            HttpServletRequest httpRequest) {

        logRequest("verifyPhoneNumber", request.phoneNumber);

        // Resolve access token: prefer request body, fall back to cookie
        String accessToken = request.accessToken;
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = extractCookieValue(httpRequest, "strategiz-access-token");
        }

        // Verify OTP and get updated tokens
        Map<String, Object> authResult = smsOtpRegistrationService.verifySmsOtpWithTokenUpdate(
            request.userId,
            accessToken,
            request.phoneNumber,
            request.otpCode
        );
        
        if (authResult == null) {
            log.warn("SMS OTP verification failed for user: {}", request.userId);
            throw new StrategizException(AuthErrors.OTP_EXPIRED, "Invalid or expired OTP code");
        }
        
        // Create success response with updated tokens
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("registrationId", registrationId);
        response.put("userId", request.userId);
        response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
        response.put("message", "Phone number verified successfully");
        
        // Add authentication tokens from the business layer
        if (authResult.containsKey("accessToken")) {
            response.put("accessToken", authResult.get("accessToken"));
        }
        if (authResult.containsKey("refreshToken")) {
            response.put("refreshToken", authResult.get("refreshToken"));
        }
        if (authResult.containsKey("identityToken")) {
            response.put("identityToken", authResult.get("identityToken"));
        }
        
        logRequestSuccess("verifyPhoneNumber", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Resend verification OTP
     * 
     * POST /auth/sms-otp/registrations/{registrationId}/resend
     * 
     * @param registrationId The registration ID
     * @param request Request with phone number details
     * @return Response with resend status
     */
    @PostMapping("/registrations/{registrationId}/resend")
    public ResponseEntity<Map<String, Object>> resendVerificationOtp(
            @PathVariable String registrationId,
            @RequestBody @Valid RegistrationRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        
        logRequest("resendVerificationOtp", request.phoneNumber);
        
        // Default IP if not provided
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = "127.0.0.1";
        }
        
        // Resend OTP
        boolean sent = smsOtpRegistrationService.resendVerificationOtp(
            request.userId,
            request.phoneNumber,
            ipAddress,
            request.countryCode != null ? request.countryCode : "US"
        );
        
        if (!sent) {
            throw new StrategizException(AuthErrors.SMS_SEND_FAILED, "Failed to resend verification SMS");
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "registrationId", registrationId,
            "phoneNumber", maskPhoneNumber(request.phoneNumber),
            "message", "Verification code resent to your phone"
        );
        
        logRequestSuccess("resendVerificationOtp", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Get SMS OTP registration status
     * 
     * GET /auth/sms-otp/registrations/{userId}
     * 
     * @param userId The user ID to check
     * @return Status response
     */
    @GetMapping("/registrations/{userId}")
    public ResponseEntity<Map<String, Object>> getRegistrationStatus(@PathVariable String userId) {
        logRequest("getSmsOtpRegistrationStatus", userId);
        
        // Check if user has verified SMS OTP
        boolean hasVerified = smsOtpRegistrationService.hasVerifiedSmsOtp(userId);
        
        Map<String, Object> result = Map.of(
            "enabled", hasVerified,
            "userId", userId,
            "registrationType", "sms_otp"
        );
        
        logRequestSuccess("getSmsOtpRegistrationStatus", userId, result);
        return createCleanResponse(result);
    }
    
    /**
     * Remove phone number registration
     * 
     * DELETE /auth/sms-otp/registrations/{userId}
     * 
     * @param userId The user ID
     * @param phoneNumber The phone number to remove (query param)
     * @return Removal status
     */
    @DeleteMapping("/registrations/{userId}")
    public ResponseEntity<Map<String, Object>> removePhoneNumber(
            @PathVariable String userId,
            @RequestParam String phoneNumber) {
        
        logRequest("removePhoneNumber", userId);
        
        boolean removed = smsOtpRegistrationService.removePhoneNumber(userId, phoneNumber);
        
        Map<String, Object> result = Map.of(
            "success", removed,
            "userId", userId,
            "phoneNumber", maskPhoneNumber(phoneNumber),
            "message", removed ? "Phone number removed successfully" : "Phone number not found"
        );
        
        logRequestSuccess("removePhoneNumber", userId, result);
        return createCleanResponse(result);
    }
    
    /**
     * Verify Firebase phone authentication token
     * 
     * POST /auth/sms-otp/firebase/verify
     * 
     * This endpoint validates Firebase ID tokens from client-side phone authentication
     * and completes the SMS OTP registration/authentication process.
     * 
     * @param request Firebase token verification request
     * @return Response with verification status and session tokens
     */
    @PostMapping("/firebase/verify")
    public ResponseEntity<Map<String, Object>> verifyFirebaseToken(
            @RequestBody @Valid FirebaseTokenRequest request,
            HttpServletResponse httpResponse) {

        logRequest("verifyFirebaseToken", request.phoneNumber);

        // Verify Firebase token and complete registration/authentication
        Map<String, Object> authResult = smsOtpRegistrationService.verifyFirebaseTokenAndComplete(
            request.firebaseIdToken,
            request.userId,
            request.phoneNumber,
            request.isRegistration
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("verified", true);
        response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
        response.put("message", request.isRegistration ?
            "Phone number verified and registered successfully" :
            "Phone authentication successful");

        // Set authentication cookies and include tokens in response
        if (authResult.containsKey("accessToken")) {
            String accessToken = (String) authResult.get("accessToken");
            response.put("accessToken", accessToken);
            cookieUtil.setAccessTokenCookie(httpResponse, accessToken);
        }
        if (authResult.containsKey("refreshToken")) {
            String refreshToken = (String) authResult.get("refreshToken");
            response.put("refreshToken", refreshToken);
            cookieUtil.setRefreshTokenCookie(httpResponse, refreshToken);
        }
        if (authResult.containsKey("identityToken")) {
            response.put("identityToken", authResult.get("identityToken"));
        }
        if (authResult.containsKey("userId")) {
            response.put("userId", authResult.get("userId"));
        }

        log.info("Authentication cookies set for phone: {}", maskPhoneNumber(request.phoneNumber));
        logRequestSuccess("verifyFirebaseToken", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Request DTO for phone registration
     */
    public record RegistrationRequest(
        @NotBlank(message = "User ID is required")
        String userId,
        
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        
        String countryCode
    ) {}
    
    /**
     * Request DTO for OTP verification
     */
    public record VerificationRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        String accessToken,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "OTP code is required")
        String otpCode
    ) {}
    
    /**
     * Request DTO for Firebase token verification
     */
    public record FirebaseTokenRequest(
        @NotBlank(message = "Firebase ID token is required")
        String firebaseIdToken,
        
        String userId,  // Optional for registration, required for authentication
        
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        
        boolean isRegistration  // true for signup, false for signin
    ) {}
    
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}