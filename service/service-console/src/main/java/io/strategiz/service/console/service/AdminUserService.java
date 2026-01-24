package io.strategiz.service.console.service;

import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.data.preferences.entity.AlertNotificationPreferences;
import io.strategiz.data.preferences.repository.AlertNotificationPreferencesRepository;
import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.data.session.repository.SessionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.UpdateUserRequest;
import io.strategiz.service.console.model.response.AdminUserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for admin user management operations.
 */
@Service
public class AdminUserService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-console";
	}

	private final UserRepository userRepository;

	private final SessionRepository sessionRepository;

	private final AlertNotificationPreferencesRepository alertPreferencesRepository;

	private final PasskeyCredentialRepository passkeyCredentialRepository;

	@Autowired
	public AdminUserService(UserRepository userRepository, SessionRepository sessionRepository,
			AlertNotificationPreferencesRepository alertPreferencesRepository,
			PasskeyCredentialRepository passkeyCredentialRepository) {
		this.userRepository = userRepository;
		this.sessionRepository = sessionRepository;
		this.alertPreferencesRepository = alertPreferencesRepository;
		this.passkeyCredentialRepository = passkeyCredentialRepository;
	}

	public List<AdminUserResponse> listUsers(int page, int pageSize) {
		log.info("Listing users: page={}, pageSize={}", page, pageSize);

		// Get ALL users including inactive ones (admin sees everything)
		List<UserEntity> allUsers = userRepository.findAllIncludingInactive();

		// Simple in-memory pagination for now
		int start = page * pageSize;
		int end = Math.min(start + pageSize, allUsers.size());

		if (start >= allUsers.size()) {
			return List.of();
		}

		List<AdminUserResponse> responses = new ArrayList<>();
		for (int i = start; i < end; i++) {
			UserEntity user = allUsers.get(i);
			responses.add(convertToResponse(user));
		}

		return responses;
	}

	public AdminUserResponse getUser(String userId) {
		log.info("Getting user details: userId={}", userId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
		}

		return convertToResponse(userOpt.get());
	}

	public AdminUserResponse disableUser(String userId, String adminUserId) {
		log.info("Disabling user: userId={}, by adminUserId={}", userId, adminUserId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
		}

		UserEntity user = userOpt.get();

		// Prevent admin from disabling themselves
		if (userId.equals(adminUserId)) {
			throw new StrategizException(ServiceConsoleErrorDetails.CANNOT_MODIFY_OWN_ACCOUNT, "service-console",
					"Cannot disable your own account");
		}

		// Set user as inactive
		user.setIsActive(false);
		userRepository.save(user);

		// Invalidate all sessions
		List<SessionEntity> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
		for (SessionEntity session : sessions) {
			session.setRevoked(true);
			sessionRepository.save(session);
		}

		log.info("User {} has been disabled", userId);
		return convertToResponse(user);
	}

	public AdminUserResponse enableUser(String userId) {
		log.info("Enabling user: userId={}", userId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
		}

		UserEntity user = userOpt.get();
		user.setIsActive(true);
		userRepository.save(user);

		log.info("User {} has been enabled", userId);
		return convertToResponse(user);
	}

	public List<SessionEntity> getUserSessions(String userId) {
		log.info("Getting sessions for user: userId={}", userId);
		return sessionRepository.findByUserIdAndRevokedFalse(userId);
	}

	public void terminateSession(String userId, String sessionId) {
		log.info("Terminating session: userId={}, sessionId={}", userId, sessionId);

		Optional<SessionEntity> sessionOpt = sessionRepository.findById(sessionId);
		if (sessionOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.SESSION_NOT_FOUND, "service-console", sessionId);
		}

		SessionEntity session = sessionOpt.get();
		if (!session.getUserId().equals(userId)) {
			throw new StrategizException(ServiceConsoleErrorDetails.SESSION_USER_MISMATCH, "service-console",
					"Session " + sessionId + " does not belong to user " + userId);
		}

		session.setRevoked(true);
		sessionRepository.save(session);

		log.info("Session {} has been terminated", sessionId);
	}

	/**
	 * Delete a user and return their passkey credential IDs for GPM signaling.
	 * @param userId User ID to delete
	 * @param adminUserId Admin performing the deletion
	 * @return List of passkey credential IDs that were deleted (for WebAuthn Signal API)
	 */
	public List<String> deleteUser(String userId, String adminUserId) {
		log.info("Hard deleting user: userId={}, by adminUserId={}", userId, adminUserId);

		// Prevent admin from deleting themselves
		if (userId.equals(adminUserId)) {
			throw new StrategizException(ServiceConsoleErrorDetails.CANNOT_MODIFY_OWN_ACCOUNT, "service-console",
					"Cannot delete your own account");
		}

		// Delete all sessions for this user
		sessionRepository.deleteByUserId(userId);

		// Get passkey credential IDs before deletion (for GPM signaling)
		List<String> deletedCredentialIds = passkeyCredentialRepository.findByUserId(userId)
			.stream()
			.map(cred -> cred.getCredentialId())
			.toList();

		// Delete all passkey credentials for this user
		passkeyCredentialRepository.deleteByUserId(userId);

		// Hard delete the user and all subcollections
		// Note: hardDeleteUser checks if document exists directly in Firestore (regardless of isActive)
		userRepository.hardDeleteUser(userId, adminUserId);

		log.warn("User {} has been permanently deleted (hard delete) by admin {}. Deleted {} passkey credentials.",
				userId, adminUserId, deletedCredentialIds.size());

		return deletedCredentialIds;
	}

	public AdminUserResponse updateUserRole(String userId, String newRole, String adminUserId) {
		log.info("Updating user role: userId={}, newRole={}, by adminUserId={}", userId, newRole, adminUserId);

		// Validate role
		if (newRole == null || (!newRole.equals("USER") && !newRole.equals("ADMIN"))) {
			throw new StrategizException(ServiceConsoleErrorDetails.INVALID_ROLE, "service-console",
					"Role must be USER or ADMIN");
		}

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
		}

		UserEntity user = userOpt.get();

		// Prevent admin from demoting themselves
		if (userId.equals(adminUserId) && newRole.equals("USER")) {
			throw new StrategizException(ServiceConsoleErrorDetails.CANNOT_MODIFY_OWN_ACCOUNT, "service-console",
					"Cannot demote your own account");
		}

		// Update role
		UserProfileEntity profile = user.getProfile();
		if (profile != null) {
			profile.setRole(newRole);
			userRepository.save(user);
		}

		log.info("User {} role updated to {} by admin {}", userId, newRole, adminUserId);
		return convertToResponse(user);
	}

	public AdminUserResponse updateUser(String userId, UpdateUserRequest request, String adminUserId) {
		log.info("Updating user: userId={}, by adminUserId={}", userId, adminUserId);

		Optional<UserEntity> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
		}

		UserEntity user = userOpt.get();
		UserProfileEntity profile = user.getProfile();

		if (profile == null) {
			profile = new UserProfileEntity();
			user.setProfile(profile);
		}

		// Update fields if provided
		if (request.getName() != null) {
			profile.setName(request.getName());
		}
		if (request.getEmail() != null) {
			profile.setEmail(request.getEmail());
		}
		if (request.getSubscriptionTier() != null) {
			profile.setSubscriptionTier(request.getSubscriptionTier());
		}
		if (request.getDemoMode() != null) {
			profile.setDemoMode(request.getDemoMode());
		}

		userRepository.save(user);

		// Update phone number in alert preferences if provided
		if (request.getPhoneNumber() != null) {
			try {
				alertPreferencesRepository.updatePhoneNumber(userId, request.getPhoneNumber());
			}
			catch (Exception e) {
				log.warn("Could not update phone number for user {}: {}", userId, e.getMessage());
			}
		}

		log.info("User {} updated by admin {}", userId, adminUserId);
		return convertToResponse(user);
	}

	private AdminUserResponse convertToResponse(UserEntity user) {
		AdminUserResponse response = new AdminUserResponse();
		response.setId(user.getId());

		UserProfileEntity profile = user.getProfile();
		if (profile != null) {
			response.setEmail(profile.getEmail());
			response.setName(profile.getName());
			response.setRole(profile.getRole());
			response.setSubscriptionTier(profile.getSubscriptionTier());
			response.setIsEmailVerified(profile.getIsEmailVerified());
			response.setDemoMode(profile.getDemoMode());
		}

		response.setStatus(Boolean.TRUE.equals(user.getIsActive()) ? "ACTIVE" : "DISABLED");

		if (user.getCreatedDate() != null) {
			response.setCreatedAt(user.getCreatedDate().toDate().toInstant());
		}

		// Count active sessions (ACCESS tokens only, exclude expired)
		Instant now = Instant.now();
		List<SessionEntity> sessions = sessionRepository.findByUserIdAndTokenTypeAndRevokedFalse(user.getId(),
				"ACCESS");
		List<SessionEntity> activeSessions = sessions.stream()
			.filter(s -> s.getExpiresAt() != null && s.getExpiresAt().isAfter(now))
			.toList();
		response.setActiveSessions(activeSessions.size());

		// Get last login from most recent session activity (use lastAccessedAt, not
		// issuedAt)
		if (!sessions.isEmpty()) {
			sessions.stream()
				.filter(s -> s.getLastAccessedAt() != null)
				.map(SessionEntity::getLastAccessedAt)
				.max(Instant::compareTo)
				.ifPresent(response::setLastLoginAt);
		}

		// Get phone number from alert notification preferences
		try {
			AlertNotificationPreferences alertPrefs = alertPreferencesRepository.getByUserId(user.getId());
			if (alertPrefs != null && alertPrefs.getPhoneNumber() != null) {
				response.setPhoneNumber(alertPrefs.getPhoneNumber());
			}
		}
		catch (Exception e) {
			log.debug("Could not fetch phone number for user {}: {}", user.getId(), e.getMessage());
		}

		return response;
	}

}
