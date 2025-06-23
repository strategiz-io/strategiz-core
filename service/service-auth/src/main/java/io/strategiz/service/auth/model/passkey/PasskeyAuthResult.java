package io.strategiz.service.auth.passkey.model;

/**
 * Result of passkey authentication or registration
 */
public record PasskeyAuthResult(
        boolean success,
        String accessToken,
        String refreshToken
) {}
