package io.strategiz.service.auth;

import io.strategiz.data.auth.PasskeyCredential;
import io.strategiz.data.auth.PasskeyCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for passkey credential management
 */
@Service
public class PasskeyService {

    private static final Logger log = LoggerFactory.getLogger(PasskeyService.class);
    
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    
    @Autowired
    public PasskeyService(PasskeyCredentialRepository passkeyCredentialRepository) {
        this.passkeyCredentialRepository = passkeyCredentialRepository;
    }
    
    /**
     * Register a new passkey credential
     *
     * @param userId User ID
     * @param credentialId Credential ID
     * @param publicKey Public key for the credential
     * @param attestationObject Attestation object
     * @param clientDataJSON Client data JSON
     * @param userAgent User agent
     * @param deviceName Device name
     * @return The registered passkey credential
     */
    public PasskeyCredential registerCredential(
            String userId,
            String credentialId,
            String publicKey,
            String attestationObject,
            String clientDataJSON,
            String userAgent,
            String deviceName) {
        
        log.info("Registering new passkey credential for user: {}, credential ID: {}", userId, credentialId);
        
        // Check if credential already exists for this user
        Optional<PasskeyCredential> existingCredential = 
            passkeyCredentialRepository.findByCredentialIdAndUserId(credentialId, userId);
        
        if (existingCredential.isPresent()) {
            log.info("Credential already registered for user: {}, credential ID: {}", userId, credentialId);
            PasskeyCredential credential = existingCredential.get();
            
            // Update existing credential
            credential.setPublicKey(publicKey);
            credential.setAttestationObject(attestationObject);
            credential.setClientDataJSON(clientDataJSON);
            credential.setUserAgent(userAgent);
            credential.setDeviceName(deviceName);
            credential.updateLastUsedTime();
            
            return passkeyCredentialRepository.save(credential);
        }
        
        // Create new credential
        long now = Instant.now().getEpochSecond();
        PasskeyCredential credential = PasskeyCredential.builder()
                .userId(userId)
                .credentialId(credentialId)
                .publicKey(publicKey)
                .attestationObject(attestationObject)
                .clientDataJSON(clientDataJSON)
                .createdAt(now)
                .lastUsedAt(now)
                .userAgent(userAgent)
                .deviceName(deviceName)
                .build();
        
        return passkeyCredentialRepository.save(credential);
    }
    
    /**
     * Get a passkey credential by ID
     *
     * @param id Passkey credential ID
     * @return Optional containing the passkey credential if found
     */
    public Optional<PasskeyCredential> getCredentialById(String id) {
        log.debug("Getting passkey credential by ID: {}", id);
        return passkeyCredentialRepository.findById(id);
    }
    
    /**
     * Get a passkey credential by credential ID
     *
     * @param credentialId Credential ID
     * @return Optional containing the passkey credential if found
     */
    public Optional<PasskeyCredential> getCredentialByCredentialId(String credentialId) {
        log.debug("Getting passkey credential by credential ID: {}", credentialId);
        return passkeyCredentialRepository.findByCredentialId(credentialId);
    }
    
    /**
     * Get a passkey credential by credential ID and user ID
     *
     * @param credentialId Credential ID
     * @param userId User ID
     * @return Optional containing the passkey credential if found
     */
    public Optional<PasskeyCredential> getCredentialByCredentialIdAndUserId(String credentialId, String userId) {
        log.debug("Getting passkey credential by credential ID and user ID: {}, {}", credentialId, userId);
        return passkeyCredentialRepository.findByCredentialIdAndUserId(credentialId, userId);
    }
    
    /**
     * Get all passkey credentials for a user
     *
     * @param userId User ID
     * @return List of passkey credentials
     */
    public List<PasskeyCredential> getUserCredentials(String userId) {
        log.info("Getting passkey credentials for user: {}", userId);
        return passkeyCredentialRepository.findAllByUserId(userId);
    }
    
    /**
     * Update a passkey credential
     *
     * @param credential Passkey credential to update
     * @return Updated passkey credential
     */
    public PasskeyCredential updateCredential(PasskeyCredential credential) {
        log.info("Updating passkey credential: {}", credential.getId());
        credential.updateLastUsedTime();
        return passkeyCredentialRepository.save(credential);
    }
    
    /**
     * Delete a passkey credential
     *
     * @param id Passkey credential ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteCredential(String id) {
        log.info("Deleting passkey credential: {}", id);
        return passkeyCredentialRepository.deleteById(id);
    }
    
    /**
     * Delete all passkey credentials for a user
     *
     * @param userId User ID
     * @return Number of deleted passkey credentials
     */
    public int deleteUserCredentials(String userId) {
        log.info("Deleting all passkey credentials for user: {}", userId);
        return passkeyCredentialRepository.deleteAllByUserId(userId);
    }
    
    /**
     * Verify a passkey assertion
     * 
     * @param credentialId Credential ID
     * @param userId User ID
     * @param signature Signature
     * @param authenticatorData Authenticator data
     * @param clientDataJSON Client data JSON
     * @return true if verification is successful, false otherwise
     */
    public boolean verifyAssertion(
            String credentialId,
            String userId,
            String signature,
            String authenticatorData,
            String clientDataJSON) {
        
        log.info("Verifying passkey assertion for user: {}, credential ID: {}", userId, credentialId);
        
        // Get the credential
        Optional<PasskeyCredential> credentialOpt = 
            passkeyCredentialRepository.findByCredentialIdAndUserId(credentialId, userId);
        
        if (credentialOpt.isEmpty()) {
            log.warn("Credential not found for user: {}, credential ID: {}", userId, credentialId);
            return false;
        }
        
        PasskeyCredential credential = credentialOpt.get();
        
        // TODO: Implement actual WebAuthn verification logic
        // This is a placeholder for actual signature verification
        boolean verificationSuccessful = true;
        
        if (verificationSuccessful) {
            // Update last used time
            credential.updateLastUsedTime();
            passkeyCredentialRepository.save(credential);
            
            log.info("Passkey assertion verification successful for user: {}", userId);
            return true;
        } else {
            log.warn("Passkey assertion verification failed for user: {}", userId);
            return false;
        }
    }
}
