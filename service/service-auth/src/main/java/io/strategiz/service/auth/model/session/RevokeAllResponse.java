package io.strategiz.service.auth.model.session;

/**
 * Response model for revoking all sessions operation
 * Contains the count of sessions that were revoked
 */
public record RevokeAllResponse(
    int count,
    String message
) {
} 