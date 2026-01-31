package io.strategiz.service.auth.model.passkey;

/**
 * Request to initiate passkey registration
 */
public record RegistrationRequest(String email, String displayName) {
}
