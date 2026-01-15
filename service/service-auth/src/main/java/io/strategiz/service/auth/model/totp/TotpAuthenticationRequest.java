package io.strategiz.service.auth.model.totp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request for TOTP authentication (login)
 */
public record TotpAuthenticationRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "TOTP code is required")
        @Pattern(regexp = "\\d{6}", message = "TOTP code must be exactly 6 digits")
        String code,

        String deviceId,

        String recaptchaToken
) {
}
