package io.strategiz.auth.service;

import io.strategiz.auth.model.PasskeyCredential;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.repository.PasskeyCredentialRepository;
import io.strategiz.auth.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing passkey authentication
 */
@Service
public class PasskeyService {
    private static final Logger logger = LoggerFactory.getLogger(PasskeyService.class);
    private static final long SESSION_EXPIRY_SECONDS = 24 * 60 * 60; // 24 hours

    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final SessionRepository sessionRepository;

    @Autowired
    public PasskeyService(PasskeyCredentialRepository passkeyCredentialRepository, SessionRepository sessionRepository) {
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Registers a new passkey credential for a user
     * @param userId The user ID
     * @param credentialId The credential ID from the WebAuthn API
     * @param publicKey The public key from the WebAuthn API
     * @param attestationObject The attestation object from the WebAuthn API
     * @param clientDataJSON The client data JSON from the WebAuthn API
     * @param userAgent The user agent string
     * @param deviceName The device name
     * @return The registered passkey credential
     * @throws ExecutionException If there's an error registering the credential
     * @throws InterruptedException If there's an error registering the credential
     */
    public PasskeyCredential registerPasskey(
            String userId, 
            String credentialId, 
            String publicKey,
            String attestationObject,
            String clientDataJSON,
            String userAgent,
            String deviceName) throws ExecutionException, InterruptedException {
        
        logger.info("Registering passkey for user: {}", userId);
        
        PasskeyCredential credential = PasskeyCredential.builder()
                .userId(userId)
                .credentialId(credentialId)
                .publicKey(publicKey)
                .attestationObject(attestationObject)
                .clientDataJSON(clientDataJSON)
                .createdAt(Instant.now().getEpochSecond())
                .lastUsedAt(Instant.now().getEpochSecond())
                .userAgent(userAgent)
                .deviceName(deviceName)
                .build();
        
        passkeyCredentialRepository.save(credential).get();
        
        logger.info("Passkey registered successfully for user: {}", userId);
        
        return credential;
    }

    /**
     * Creates a session for passkey authentication
     * @param userId The user ID
     * @param email The user's email
     * @param timestamp The timestamp of the authentication
     * @param credentialId The credential ID used for authentication
     * @return The session ID
     * @throws ExecutionException If there's an error creating the session
     * @throws InterruptedException If there's an error creating the session
     */
    public String createPasskeySession(String userId, String email, String timestamp, String credentialId) 
            throws ExecutionException, InterruptedException {
        logger.info("Creating passkey session for user: {}", userId);
        
        // Update the last used time for the credential
        updateCredentialLastUsed(userId, credentialId);
        
        String sessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .id(sessionId)
                .userId(userId)
                .token("passkey-auth:" + credentialId) // Special token for passkey auth with credential ID
                .createdAt(Instant.now().getEpochSecond())
                .expiresAt(Instant.now().getEpochSecond() + SESSION_EXPIRY_SECONDS)
                .lastAccessedAt(Instant.now().getEpochSecond())
                .build();
        
        sessionRepository.save(session).get(); // Wait for completion
        logger.info("Passkey session created successfully: {}", sessionId);
        
        return sessionId;
    }

    /**
     * Updates the last used time for a credential
     * @param userId The user ID
     * @param credentialId The credential ID
     * @throws ExecutionException If there's an error updating the credential
     * @throws InterruptedException If there's an error updating the credential
     */
    private void updateCredentialLastUsed(String userId, String credentialId) 
            throws ExecutionException, InterruptedException {
        // Find the credential by user ID and credential ID
        CompletableFuture<List<PasskeyCredential>> credentialsFuture = passkeyCredentialRepository.findByUserId(userId);
        List<PasskeyCredential> credentials = credentialsFuture.get();
        
        for (PasskeyCredential credential : credentials) {
            if (credential.getCredentialId().equals(credentialId)) {
                credential.updateLastUsedTime();
                passkeyCredentialRepository.save(credential).get();
                logger.info("Updated last used time for credential: {}", credentialId);
                break;
            }
        }
    }

    /**
     * Gets all passkey credentials for a user
     * @param userId The user ID
     * @return The list of passkey credentials
     * @throws ExecutionException If there's an error getting the credentials
     * @throws InterruptedException If there's an error getting the credentials
     */
    public List<PasskeyCredential> getPasskeyCredentials(String userId) 
            throws ExecutionException, InterruptedException {
        logger.info("Getting passkey credentials for user: {}", userId);
        return passkeyCredentialRepository.findByUserId(userId).get();
    }

    /**
     * Deletes a passkey credential
     * @param id The credential ID
     * @throws ExecutionException If there's an error deleting the credential
     * @throws InterruptedException If there's an error deleting the credential
     */
    public void deletePasskeyCredential(String id) throws ExecutionException, InterruptedException {
        logger.info("Deleting passkey credential: {}", id);
        passkeyCredentialRepository.deleteById(id).get();
    }
}
