package io.strategiz.service.auth.model.session;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for signing out a user
 * Contains user session information needed for clean logout
 */
public record SignOutRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    String sessionId,
    String deviceId,
    boolean revokeAllSessions
) {
} 