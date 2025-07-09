package io.strategiz.service.auth.model.passkey;

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
