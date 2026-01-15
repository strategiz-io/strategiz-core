package io.strategiz.service.auth.model.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request model for verifying email OTP and completing signup.
 */
public record EmailSignupVerifyRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "\\d{6}", message = "OTP code must be exactly 6 digits")
    String otpCode,

    @NotBlank(message = "Session ID is required")
    String sessionId
) {}
