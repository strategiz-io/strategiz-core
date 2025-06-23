package io.strategiz.service.auth.passkey.model;

/**
 * Request to initiate passkey registration
 */
public record RegistrationRequest(
        String email,
        String displayName
) {}
