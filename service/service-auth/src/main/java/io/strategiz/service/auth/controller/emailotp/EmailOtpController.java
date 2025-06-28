package io.strategiz.service.auth.controller.emailotp;

import io.strategiz.service.auth.model.ApiResponse;
import io.strategiz.service.auth.model.emailotp.EmailOtpRequest;
import io.strategiz.service.auth.model.emailotp.EmailOtpVerificationRequest;
import io.strategiz.service.auth.service.emailotp.EmailOtpService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for email OTP operations
 * Provides endpoints for sending and verifying email one-time passwords
 */
@RestController
@RequestMapping("/auth/emailotp")
public class EmailOtpController {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpController.class);
    
    @Autowired
    private EmailOtpService emailOtpService;
    
    /**
     * Send a one-time password to an email address
     * 
     * @param request Email and purpose information
     * @return API response indicating success or failure
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody EmailOtpRequest request) {
        
        logger.info("Received request to send email OTP for purpose: {}", 
                request.purpose());
        
        boolean sent = emailOtpService.sendOtp(
                request.email(), request.purpose());
        
        if (sent) {
            return ResponseEntity.ok(
                ApiResponse.<Void>success("Email OTP sent successfully", null));
        } else {
            return ResponseEntity.status(500).body(
                ApiResponse.error("Failed to send email OTP"));
        }
    }
    
    /**
     * Verify an OTP code submitted by a user
     * 
     * @param request Email, purpose, and code information
     * @return API response indicating if verification was successful, includes identity token for signup
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(
            @Valid @RequestBody EmailOtpVerificationRequest request) {
        
        logger.info("Received request to verify email OTP for purpose: {}", 
                request.purpose());
        
        boolean verified = emailOtpService.verifyOtp(
                request.email(), request.purpose(), request.code());
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("verified", verified);
        
        if (verified) {
            // For signup purpose, the email verification is sufficient
            // Client can proceed with the verified email
            if ("signup".equals(request.purpose())) {
                logger.info("Email verification for signup successful: {}", request.email());
            }
            
            return ResponseEntity.ok(
                ApiResponse.success("Email OTP verification successful", responseData));
        } else {
            return ResponseEntity.ok(
                ApiResponse.success("Invalid or expired email OTP", responseData));
        }
    }
    
    /**
     * Check if an email has a pending OTP verification
     * 
     * @param email Email address to check
     * @param purpose Purpose of verification
     * @return API response indicating if a pending verification exists
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkOtpStatus(
            @RequestParam("email") String email,
            @RequestParam("purpose") String purpose) {
        
        logger.info("Checking email OTP status for email and purpose: {}", purpose);
        
        boolean hasPending = emailOtpService.hasPendingOtp(email, purpose);
        
        return ResponseEntity.ok(
            ApiResponse.success(
                hasPending ? "Email OTP pending" : "No pending email OTP", 
                hasPending));
    }
}
