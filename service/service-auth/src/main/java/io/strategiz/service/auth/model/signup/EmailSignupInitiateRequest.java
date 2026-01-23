package io.strategiz.service.auth.model.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for initiating email-based signup.
 * This starts the signup process by sending an OTP to verify email ownership.
 *
 * Note: No password required - this is a passwordless signup flow.
 * Users will authenticate via Passkey, TOTP, or SMS set up in Step 2.
 */
public record EmailSignupInitiateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    String recaptchaToken
) {}
