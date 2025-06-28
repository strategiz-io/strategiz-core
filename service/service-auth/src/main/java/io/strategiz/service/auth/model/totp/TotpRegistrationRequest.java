package io.strategiz.service.auth.model.totp;

/**
 * Request for TOTP registration (setup)
 */
public record TotpRegistrationRequest(
        String userId
) {
}
