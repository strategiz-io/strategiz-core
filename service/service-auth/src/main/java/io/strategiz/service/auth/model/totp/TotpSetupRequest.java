package io.strategiz.service.auth.model.totp;

/**
 * Request to initiate TOTP setup for a user
 */
public record TotpSetupRequest(
        String userId,
        String email,
        String authenticatorName,
        String deviceId
) {
}
