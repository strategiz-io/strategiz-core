package io.strategiz.service.auth.model;

/**
 * Enhanced authentication response containing both tokens and user information
 */
public record AuthenticationResponse(String accessToken, String refreshToken, String tokenType, UserInfo user,
		String redirectUrl // Optional: redirect URL with one-time token for cross-app SSO
) {
	/**
	 * Nested record for user information
	 */
	public record UserInfo(String id, String email, String name, Boolean verified) {
	}

	/**
	 * Create authentication response with default token type (no redirect)
	 */
	public static AuthenticationResponse create(String accessToken, String refreshToken, UserInfo user) {
		return new AuthenticationResponse(accessToken, refreshToken, "Bearer", user, null);
	}

	/**
	 * Create authentication response with redirect URL for cross-app SSO
	 */
	public static AuthenticationResponse createWithRedirect(String accessToken, String refreshToken, UserInfo user,
			String redirectUrl) {
		return new AuthenticationResponse(accessToken, refreshToken, "Bearer", user, redirectUrl);
	}
}