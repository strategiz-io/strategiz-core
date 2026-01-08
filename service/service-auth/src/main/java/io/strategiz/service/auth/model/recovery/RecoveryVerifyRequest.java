package io.strategiz.service.auth.model.recovery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to verify a recovery code (email or SMS).
 *
 * @param code the verification code
 */
public record RecoveryVerifyRequest(
        @NotBlank(message = "Code is required")
        @Size(min = 6, max = 6, message = "Code must be 6 digits")
        String code
) {
}
