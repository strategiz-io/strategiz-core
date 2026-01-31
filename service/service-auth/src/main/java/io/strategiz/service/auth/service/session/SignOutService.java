package io.strategiz.service.auth.service.session;

import io.strategiz.service.auth.model.session.SignOutResponse;
import io.strategiz.service.base.BaseService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Service for handling user sign out operations Manages session cleanup and token
 * revocation
 */
@Service
public class SignOutService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-auth";
	}

	private final SessionService sessionService;

	public SignOutService(SessionService sessionService) {
		this.sessionService = sessionService;
	}

	/**
	 * Sign out a user and clean up their sessions
	 * @param userId The user ID to sign out
	 * @param sessionId The current session ID (token) to revoke
	 * @param deviceId The device ID (optional)
	 * @param revokeAllSessions Whether to revoke all sessions or just the current one
	 * @param request HTTP request for session context
	 * @param response HTTP response for clearing cookies
	 * @return SignOutResponse with operation result
	 */
	public SignOutResponse signOut(String userId, String sessionId, String deviceId, boolean revokeAllSessions,
			HttpServletRequest request, HttpServletResponse response) {
		log.info("Processing sign out for user: {} (revokeAll: {})", userId, revokeAllSessions);

		try {
			int sessionsRevoked = 0;

			// ALWAYS revoke ALL sessions for the user to ensure complete logout
			// This prevents auto-login issues and ensures security
			log.info("Revoking ALL sessions for user: {} (force logout from all devices)", userId);
			int allSessionsRevoked = sessionService.terminateAllUserSessions(userId,
					"User sign out - force logout from all devices");
			sessionsRevoked = allSessionsRevoked;

			// Terminate the current session and clear cookies
			boolean currentSessionTerminated = sessionService.terminateSession(request, response);
			if (currentSessionTerminated) {
				log.info("Current session and cookies cleared for user: {}", userId);
			}
			else {
				log.warn("Current session termination failed for user: {}, but continuing with logout", userId);
			}

			// Log the total sessions revoked
			log.info("Successfully revoked {} total sessions for user: {}", sessionsRevoked, userId);

			return new SignOutResponse(true, "User signed out successfully", sessionsRevoked);

		}
		catch (Exception e) {
			log.error("Error during sign out for user {}: {}", userId, e.getMessage());
			return new SignOutResponse(false, "Sign out failed: " + e.getMessage(), 0);
		}
	}

}