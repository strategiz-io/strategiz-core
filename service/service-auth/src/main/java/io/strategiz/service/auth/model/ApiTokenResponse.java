package io.strategiz.service.auth.model;

/**
 * Response containing authentication tokens
 */
public record ApiTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType
) {}
