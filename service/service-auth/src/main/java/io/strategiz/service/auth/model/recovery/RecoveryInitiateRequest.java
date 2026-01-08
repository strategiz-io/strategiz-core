package io.strategiz.service.auth.model.recovery;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to initiate account recovery.
 *
 * @param email the email address associated with the account
 */
public record RecoveryInitiateRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {
}
