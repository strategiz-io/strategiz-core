package io.strategiz.service.auth.passkey.model;

/**
 * Data for completing passkey authentication
 */
public record AuthenticationCompletion(
        String credentialId,
        String clientDataJSON,
        String authenticatorData,
        String signature,
        String userHandle
) {}
