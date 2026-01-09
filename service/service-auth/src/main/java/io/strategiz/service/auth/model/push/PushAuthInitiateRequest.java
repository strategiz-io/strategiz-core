package io.strategiz.service.auth.model.push;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to initiate push authentication.
 *
 * @param userId the user ID to authenticate (required for signin flow)
 * @param purpose the purpose: "signin", "mfa", or "recovery"
 */
public record PushAuthInitiateRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        String purpose
) {
    public String getPurposeOrDefault() {
        return purpose != null ? purpose : "signin";
    }
}
