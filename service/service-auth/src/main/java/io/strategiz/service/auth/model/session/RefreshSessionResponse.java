package io.strategiz.service.auth.model.session;

/**
 * Response model for session refresh operation
 * Contains the new access token after successful refresh
 */
public record RefreshSessionResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {
} 