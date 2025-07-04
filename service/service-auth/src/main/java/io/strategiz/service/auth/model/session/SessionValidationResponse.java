package io.strategiz.service.auth.model.session;

/**
 * Response model for session validation operation
 * Contains validation result and user information if valid
 */
public record SessionValidationResponse(
    boolean valid,
    String userId,
    long expiresAt
) {
} 