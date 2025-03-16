package io.strategiz.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing sessions
 */
@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final long SESSION_EXPIRY_SECONDS = 24 * 60 * 60; // 24 hours

    private final SessionRepository sessionRepository;
    private final PasskeyService passkeyService;
    private final DeviceIdentityService deviceIdentityService;

    @Autowired
    public SessionService(
            SessionRepository sessionRepository,
            PasskeyService passkeyService,
            DeviceIdentityService deviceIdentityService) {
        this.sessionRepository = sessionRepository;
        this.passkeyService = passkeyService;
        this.deviceIdentityService = deviceIdentityService;
    }

    /**
     * Creates a session for a user
     * @param token The Firebase token
     * @return The session ID
     * @throws ExecutionException If there's an error creating the session
     * @throws InterruptedException If there's an error creating the session
     * @throws FirebaseAuthException If there's an error verifying the token
     */
    public String createSession(String token) throws ExecutionException, InterruptedException, FirebaseAuthException {
        logger.info("Creating session with token");
        
        // Verify the token
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        String userId = decodedToken.getUid();
        
        // Create a session
        String sessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .id(sessionId)
                .userId(userId)
                .token(token)
                .createdAt(Instant.now().getEpochSecond())
                .expiresAt(Instant.now().getEpochSecond() + SESSION_EXPIRY_SECONDS)
                .lastAccessedAt(Instant.now().getEpochSecond())
                .build();
        
        sessionRepository.save(session).get(); // Wait for completion
        logger.info("Session created: {}", sessionId);
        
        return sessionId;
    }

    /**
     * Creates a session for passkey authentication
     * @param userId The user ID
     * @param email The user's email
     * @param timestamp The timestamp of the authentication
     * @return The session ID
     * @throws ExecutionException If there's an error creating the session
     * @throws InterruptedException If there's an error creating the session
     */
    public String createPasskeySession(String userId, String email, String timestamp) 
            throws ExecutionException, InterruptedException {
        logger.info("Delegating passkey session creation to PasskeyService");
        // This method is kept for backward compatibility
        // In the future, clients should call PasskeyService directly
        return passkeyService.createPasskeySession(userId, email, timestamp, null);
    }

    /**
     * Creates a session for device web crypto authentication
     * @param userId The user ID
     * @param email The user's email
     * @param deviceId The device ID
     * @param signature The signature from the device
     * @param deviceInfo Additional device information
     * @return The session ID
     * @throws ExecutionException If there's an error creating the session
     * @throws InterruptedException If there's an error creating the session
     */
    public String createDeviceSession(String userId, String email, String deviceId, String signature, 
                                     java.util.Map<String, Object> deviceInfo) 
            throws ExecutionException, InterruptedException {
        logger.info("Delegating device session creation to DeviceIdentityService");
        // This method is kept for backward compatibility
        // In the future, clients should call DeviceIdentityService directly
        return deviceIdentityService.createDeviceSession(userId, email, deviceId, signature, deviceInfo);
    }

    /**
     * Validates a session
     * @param sessionId The session ID
     * @return An Optional containing the session if valid, or empty if invalid
     * @throws ExecutionException If there's an error validating the session
     * @throws InterruptedException If there's an error validating the session
     */
    public Optional<Session> validateSession(String sessionId) throws ExecutionException, InterruptedException {
        logger.info("Validating session: {}", sessionId);
        
        // Get the session
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId).get();
        
        if (sessionOpt.isEmpty()) {
            logger.warn("Session not found: {}", sessionId);
            return Optional.empty();
        }
        
        Session session = sessionOpt.get();
        
        // Check if the session has expired
        if (session.getExpiresAt() < Instant.now().getEpochSecond()) {
            logger.warn("Session expired: {}", sessionId);
            return Optional.empty();
        }
        
        // Update the last accessed time
        session.setLastAccessedAt(Instant.now().getEpochSecond());
        sessionRepository.save(session).get();
        
        logger.info("Session validated: {}", sessionId);
        return Optional.of(session);
    }

    /**
     * Refreshes a session
     * @param sessionId The session ID
     * @return The refreshed session, or null if the session is invalid
     * @throws ExecutionException If there's an error refreshing the session
     * @throws InterruptedException If there's an error refreshing the session
     */
    public Session refreshSession(String sessionId) throws ExecutionException, InterruptedException {
        logger.info("Refreshing session: {}", sessionId);
        
        // Validate the session first
        Optional<Session> sessionOpt = validateSession(sessionId);
        
        if (sessionOpt.isEmpty()) {
            logger.warn("Cannot refresh invalid session: {}", sessionId);
            return null;
        }
        
        Session session = sessionOpt.get();
        
        // Update the expiry time
        session.setExpiresAt(Instant.now().getEpochSecond() + SESSION_EXPIRY_SECONDS);
        sessionRepository.save(session).get();
        
        logger.info("Session refreshed: {}", sessionId);
        return session;
    }
}
