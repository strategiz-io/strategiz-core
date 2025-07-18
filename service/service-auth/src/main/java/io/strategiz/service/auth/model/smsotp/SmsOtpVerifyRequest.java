package io.strategiz.service.auth.model.smsotp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for verifying SMS OTP
 * 
 * @param phoneNumber The phone number that received the OTP (E.164 format)
 * @param otpCode The OTP code to verify (6 digits)
 */
public record SmsOtpVerifyRequest(
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +1234567890)")
    String phoneNumber,
    
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP code must be exactly 6 digits")
    String otpCode
) {}