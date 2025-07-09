package io.strategiz.service.auth.model.passkey;

/**
 * Allowed credential for authentication
 */
public record AllowedCredential(
        String id, 
        String type
) {}
