package io.strategiz.service.auth.model.totp;

/**
 * Request for TOTP authentication (login)
 */
public record TotpAuthenticationRequest(
        String userId,
        String code
) {
}
