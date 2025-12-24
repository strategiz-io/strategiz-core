package io.strategiz.service.auth.controller.smsotp;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.smsotp.SmsOtpAuthenticationService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
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
 * Controller for SMS OTP authentication (sign-in)
 * 
 * Handles SMS OTP authentication for users with verified phone numbers.
 * This is used for passwordless sign-in via SMS.
 */
@RestController
@RequestMapping("/v1/auth/sms-otp")
public class SmsOtpAuthenticationController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(SmsOtpAuthenticationController.class);
    
    @Autowired
    private SmsOtpAuthenticationService smsOtpAuthenticationService;
    
    @Autowired
    private CookieUtil cookieUtil;
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }
    
    /**
     * Request authentication OTP for sign-in
     * 
     * POST /auth/sms-otp/authentications
     * 
     * @param request Request with phone number
     * @return Response with session ID for OTP verification
     */
    @PostMapping("/authentications")
    public ResponseEntity<Map<String, Object>> requestAuthenticationOtp(
            @RequestBody @Valid AuthenticationOtpRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        
        logRequest("requestAuthenticationOtp", request.phoneNumber);
        
        // Send authentication OTP - requires userId
        String sessionId = smsOtpAuthenticationService.sendAuthenticationOtp(
            request.userId,
            request.phoneNumber,
            request.countryCode != null ? request.countryCode : "US"
        );
        
        Map<String, Object> response = Map.of(
            "success", true,
            "sessionId", sessionId,
            "phoneNumber", maskPhoneNumber(request.phoneNumber),
            "message", "Authentication code sent to your phone"
        );
        
        logRequestSuccess("requestAuthenticationOtp", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Request authentication OTP by phone number only (for sign-in page)
     * This endpoint allows users to enter just their phone number
     * 
     * POST /v1/auth/sms-otp/authentications/send
     * 
     * @param request Request with phone number only
     * @return Response with session ID for OTP verification
     */
    @PostMapping("/authentications/send")
    public ResponseEntity<Map<String, Object>> requestAuthenticationByPhone(
            @RequestBody @Valid PhoneAuthenticationRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ipAddress) {
        
        logRequest("requestAuthenticationByPhone", request.phoneNumber);
        
        // Send authentication OTP by phone lookup
        String sessionId = smsOtpAuthenticationService.sendAuthenticationOtpByPhone(
            request.phoneNumber,
            request.countryCode != null ? request.countryCode : "US"
        );
        
        Map<String, Object> response = Map.of(
            "success", true,
            "sessionId", sessionId,
            "phoneNumber", maskPhoneNumber(request.phoneNumber),
            "message", "Verification code sent to your phone"
        );
        
        logRequestSuccess("requestAuthenticationByPhone", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Verify authentication OTP and sign in (RESTful version)
     * 
     * PUT /v1/auth/sms-otp/authentications/{sessionId}
     * 
     * @param sessionId The OTP session ID
     * @param request Verification request with OTP code
     * @param httpResponse HTTP response to set cookies
     * @return Response with authentication tokens
     */
    @PutMapping("/authentications/{sessionId}")
    public ResponseEntity<Map<String, Object>> verifyAuthenticationOtp(
            @PathVariable String sessionId,
            @RequestBody @Valid AuthenticationVerifyRequest request,
            HttpServletResponse httpResponse) {
        
        logRequest("verifyAuthenticationOtp", request.phoneNumber);
        
        // Authenticate with OTP
        Map<String, Object> authResult = smsOtpAuthenticationService.authenticateWithOtp(
            request.phoneNumber,
            request.otpCode,
            sessionId
        );
        
        if (authResult == null) {
            log.warn("SMS OTP authentication failed for phone: {}", maskPhoneNumber(request.phoneNumber));
            throw new StrategizException(AuthErrors.OTP_EXPIRED, "Invalid or expired OTP code");
        }
        
        // Create success response with authentication tokens
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
        response.put("message", "Authentication successful");
        
        // Add authentication tokens from the business layer and set cookies
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
        logRequestSuccess("verifyAuthenticationOtp", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Request DTO for authentication OTP
     */
    public record AuthenticationOtpRequest(
        @NotBlank(message = "User ID is required")
        String userId,
        
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        
        String countryCode
    ) {}
    
    /**
     * Request DTO for phone-only authentication (sign-in page)
     */
    public record PhoneAuthenticationRequest(
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        
        String countryCode
    ) {}
    
    /**
     * Verify authentication OTP without session ID (for backwards compatibility)
     * 
     * POST /v1/auth/sms-otp/authentications/verify
     * 
     * @param request Verification request with phone and OTP code
     * @return Response with authentication tokens
     */
    @PostMapping("/authentications/verify")
    public ResponseEntity<Map<String, Object>> verifyAuthenticationOtpWithoutSession(
            @RequestBody @Valid AuthenticationVerifyRequest request) {
        
        logRequest("verifyAuthenticationOtpWithoutSession", request.phoneNumber);
        
        // Authenticate with OTP (sessionId will be looked up by phone number)
        Map<String, Object> authResult = smsOtpAuthenticationService.authenticateWithOtp(
            request.phoneNumber,
            request.otpCode,
            null // No sessionId provided, will lookup by phone
        );
        
        if (authResult == null) {
            log.warn("SMS OTP authentication failed for phone: {}", maskPhoneNumber(request.phoneNumber));
            throw new StrategizException(AuthErrors.OTP_EXPIRED, "Invalid or expired OTP code");
        }
        
        // Create success response with authentication tokens
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("phoneNumber", maskPhoneNumber(request.phoneNumber));
        response.put("message", "Authentication successful");
        
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
        if (authResult.containsKey("userId")) {
            response.put("userId", authResult.get("userId"));
        }
        
        logRequestSuccess("verifyAuthenticationOtpWithoutSession", request.phoneNumber, response);
        return createCleanResponse(response);
    }
    
    /**
     * Request DTO for OTP verification
     */
    public record AuthenticationVerifyRequest(
        @NotBlank(message = "Phone number is required")
        String phoneNumber,
        
        @NotBlank(message = "OTP code is required")
        String otpCode
    ) {}
    
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}