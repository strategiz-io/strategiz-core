package io.strategiz.service.auth.model.totp;

/**
 * Request to authenticate using a TOTP code
 */
public record TotpAuthRequest(
        String userId,
        String code,
        String deviceId
) {
}
