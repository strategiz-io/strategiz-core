package io.strategiz.service.auth.model.session;

/**
 * Response model for sign out operation Contains the result of the sign out attempt
 */
public record SignOutResponse(boolean success, String message, int sessionsRevoked) {
}