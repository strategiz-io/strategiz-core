package io.strategiz.service.auth.passkey.model;

/**
 * Challenge data for passkey registration
 */
public record RegistrationChallenge(
        String challenge,
        String rpId,
        String rpName,
        String userId,
        String userName,
        int timeout
) {}
