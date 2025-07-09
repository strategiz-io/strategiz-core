package io.strategiz.service.auth.model.passkey;

/**
 * Result of passkey authentication or registration
 */
public record PasskeyAuthResult(
        boolean success,
        String accessToken,
        String refreshToken
) {}
