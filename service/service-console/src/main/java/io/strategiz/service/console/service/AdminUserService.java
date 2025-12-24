package io.strategiz.service.console.service;

import io.strategiz.data.session.entity.SessionEntity;
import io.strategiz.data.session.repository.SessionRepository;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.console.exception.ServiceConsoleErrorDetails;
import io.strategiz.service.console.model.response.AdminUserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Autowired
    public AdminUserService(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    public List<AdminUserResponse> listUsers(int page, int pageSize) {
        logger.info("Listing users: page={}, pageSize={}", page, pageSize);

        // TODO: Implement pagination at the repository level
        List<UserEntity> allUsers = userRepository.findAll();

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
        logger.info("Getting user details: userId={}", userId);

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
        }

        return convertToResponse(userOpt.get());
    }

    public AdminUserResponse disableUser(String userId, String adminUserId) {
        logger.info("Disabling user: userId={}, by adminUserId={}", userId, adminUserId);

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

        logger.info("User {} has been disabled", userId);
        return convertToResponse(user);
    }

    public AdminUserResponse enableUser(String userId) {
        logger.info("Enabling user: userId={}", userId);

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
        }

        UserEntity user = userOpt.get();
        user.setIsActive(true);
        userRepository.save(user);

        logger.info("User {} has been enabled", userId);
        return convertToResponse(user);
    }

    public List<SessionEntity> getUserSessions(String userId) {
        logger.info("Getting sessions for user: userId={}", userId);
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }

    public void terminateSession(String userId, String sessionId) {
        logger.info("Terminating session: userId={}, sessionId={}", userId, sessionId);

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

        logger.info("Session {} has been terminated", sessionId);
    }

    public void deleteUser(String userId, String adminUserId) {
        logger.info("Deleting user: userId={}, by adminUserId={}", userId, adminUserId);

        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new StrategizException(ServiceConsoleErrorDetails.USER_NOT_FOUND, "service-console", userId);
        }

        // Prevent admin from deleting themselves
        if (userId.equals(adminUserId)) {
            throw new StrategizException(ServiceConsoleErrorDetails.CANNOT_MODIFY_OWN_ACCOUNT, "service-console",
                    "Cannot delete your own account");
        }

        // Terminate all sessions before deleting user
        sessionRepository.deleteByUserId(userId);

        // Delete the user
        userRepository.deleteUser(userId);

        logger.warn("User {} has been permanently deleted by admin {}", userId, adminUserId);
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

        // Count active sessions
        List<SessionEntity> sessions = sessionRepository.findByUserIdAndRevokedFalse(user.getId());
        response.setActiveSessions(sessions.size());

        // Get last login from most recent session
        if (!sessions.isEmpty()) {
            sessions.stream()
                .filter(s -> s.getIssuedAt() != null)
                .map(SessionEntity::getIssuedAt)
                .max(Instant::compareTo)
                .ifPresent(response::setLastLoginAt);
        }

        return response;
    }
}
