package io.strategiz.service.auth.model;

/**
 * Enhanced authentication response containing both tokens and user information
 */
public record AuthenticationResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UserInfo user
) {
    /**
     * Nested record for user information
     */
    public record UserInfo(
        String id,
        String email,
        String name,
        Boolean verified
    ) {}
    
    /**
     * Create authentication response with default token type
     */
    public static AuthenticationResponse create(String accessToken, String refreshToken, UserInfo user) {
        return new AuthenticationResponse(accessToken, refreshToken, "Bearer", user);
    }
}