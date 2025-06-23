package io.strategiz.service.auth.model.passkey;

/**
 * Request for beginning passkey registration
 */
public record PasskeyRegistrationRequest(
    String email,
    String displayName
) {}
