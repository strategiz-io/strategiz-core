package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for refreshing a session Contains the refresh token to be used for
 * generating new access token
 */
public record RefreshSessionRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {
}