package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for revoking a session Contains the session ID and user ID for revocation
 */
public record SessionRevocationRequest(@NotBlank(message = "Session ID is required") String sessionId,

		@NotBlank(message = "User ID is required") String userId) {
}