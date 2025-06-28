package io.strategiz.service.auth.model.emailotp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for sending email OTP
 * Contains the email to send OTP to and the purpose
 */
public record EmailOtpRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Purpose is required")
    @Pattern(
        regexp = "^(signup|reset-password|change-email|account-recovery)$", 
        message = "Purpose must be one of: signup, reset-password, change-email, account-recovery"
    )
    String purpose
) {
}
