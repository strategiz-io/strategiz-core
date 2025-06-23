package io.strategiz.service.auth.passkey.model;

/**
 * Allowed credential for authentication
 */
public record AllowedCredential(
        String id, 
        String type
) {}
