package io.strategiz.service.auth.model.totp;

/**
 * Request to verify a TOTP code during setup
 */
public record TotpVerifyRequest(
        String userId,
        String secret,
        String code,
        String deviceId
) {
}
