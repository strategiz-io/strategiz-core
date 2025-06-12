package io.strategiz.service.auth;

import io.strategiz.data.auth.Passkey;
import io.strategiz.data.auth.PasskeyCredential;
import io.strategiz.data.auth.PasskeyCredentialRepository;
import io.strategiz.service.auth.token.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

// No WebAuthn imports needed for this simplified implementation

/**
 * Service for passkey credential management
 */
@Service
public class PasskeyService {

    /**
     * Result container for passkey authentication operations
     */
    public record PasskeyAuthResult(boolean success, String accessToken, String refreshToken) {}

    private static final Logger log = LoggerFactory.getLogger(PasskeyService.class);
    
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final TokenService tokenService;
    
    @Autowired
    public PasskeyService(PasskeyCredentialRepository passkeyCredentialRepository, TokenService tokenService) {
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.tokenService = tokenService;
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
     * @param ipAddress the IP address of the client making the request
     * @param deviceId optional device identifier
     * @return PasskeyAuthResult containing success status and tokens if successful
     */
    public PasskeyAuthResult verifyAssertion(
            String credentialId,
            String userId,
            String signature,
            String authenticatorData,
            String clientDataJSON,
            String ipAddress,
            String deviceId) {
        
        log.info("Verifying passkey assertion for user: {}, credential ID: {}", userId, credentialId);
        
        // Get the credential
        Optional<PasskeyCredential> credentialOpt = 
            passkeyCredentialRepository.findByCredentialIdAndUserId(credentialId, userId);
        
        if (credentialOpt.isEmpty()) {
            log.warn("Credential not found for user: {}, credential ID: {}", userId, credentialId);
            return new PasskeyAuthResult(false, null, null);
        }
        
        PasskeyCredential credential = credentialOpt.get();
        
        try {
            // Convert base64url encoded strings to byte arrays
            byte[] publicKeyBytes = Base64.getUrlDecoder().decode(credential.getPublicKey());
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signature);
            byte[] authenticatorDataBytes = Base64.getUrlDecoder().decode(authenticatorData);
            byte[] clientDataJSONBytes = clientDataJSON.getBytes(StandardCharsets.UTF_8);
            
            // Calculate client data hash
            byte[] clientDataHash = MessageDigest.getInstance("SHA-256").digest(clientDataJSONBytes);
            
            // Create assertion data (authenticator data + client data hash)
            byte[] signedData = ByteBuffer.allocate(authenticatorDataBytes.length + clientDataHash.length)
                    .put(authenticatorDataBytes)
                    .put(clientDataHash)
                    .array();
            
            // Parse the public key
            PublicKey publicKey = null;
            try {
                // Use Java's built-in EC key spec parser
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                publicKey = keyFactory.generatePublic(keySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                log.error("Invalid public key format: {}", e.getMessage());
                return new PasskeyAuthResult(false, null, null);
            }
            
            // Verify signature
            try {
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initVerify(publicKey);
                sig.update(signedData);
                boolean verificationSuccessful = sig.verify(signatureBytes);
                
                if (verificationSuccessful) {
                    // Update last used time if verification is successful
                    credential.updateLastUsedTime();
                    passkeyCredentialRepository.save(credential);
                    log.info("Passkey verification successful for user: {}", userId);
                    
                    // Generate token pair for successful authentication
                    TokenService.TokenPair tokenPair = tokenService.createTokenPair(
                            userId, 
                            deviceId, 
                            ipAddress, 
                            "user");
                    
                    return new PasskeyAuthResult(true, tokenPair.accessToken(), tokenPair.refreshToken());
                } else {
                    log.warn("Passkey verification failed for user: {}", userId);
                    return new PasskeyAuthResult(false, null, null);
                }
                
            } catch (Exception e) {
                log.error("Error verifying passkey signature: {}", e.getMessage());
                return new PasskeyAuthResult(false, null, null);
            }
        } catch (Exception e) {
            log.error("Error in passkey verification process: {}", e.getMessage());
            return new PasskeyAuthResult(false, null, null);
        }
    }
    
    /**
     * Convert PasskeyCredential to Passkey
     * 
     * @param credential PasskeyCredential to convert
     * @return Passkey object
     */
    private Passkey convertToPasskey(PasskeyCredential credential) {
        if (credential == null) {
            return null;
        }
        
        return Passkey.builder()
                .id(credential.getId())
                .userId(credential.getUserId())
                .credentialId(credential.getCredentialId())
                .publicKey(credential.getPublicKey())
                .name(credential.getDeviceName())
                .createdAt(credential.getCreatedAt())
                .lastUsedAt(credential.getLastUsedAt())
                .trusted(true)
                .build();
    }
    
    /**
     * Register a new passkey
     *
     * @param userId User ID
     * @param credentialId Credential ID
     * @param publicKey Public key
     * @return The registered passkey
     */
    public Passkey registerPasskey(String userId, String credentialId, String publicKey) {
        log.info("Registering new passkey for user: {}, credential ID: {}", userId, credentialId);
        
        // Use the existing registerCredential method with minimal required parameters
        PasskeyCredential credential = registerCredential(
            userId,
            credentialId,
            publicKey,
            "", // attestationObject
            "", // clientDataJSON
            "API", // userAgent
            "Default Device" // deviceName
        );
        
        return convertToPasskey(credential);
    }
    
    /**
     * Get all passkeys for a user
     *
     * @param userId User ID
     * @return List of passkeys
     */
    public List<Passkey> getUserPasskeys(String userId) {
        log.info("Getting passkeys for user: {}", userId);
        List<PasskeyCredential> credentials = getUserCredentials(userId);
        
        return credentials.stream()
                .map(this::convertToPasskey)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Delete a passkey by credential ID
     *
     * @param credentialId Credential ID
     * @return true if deleted, false otherwise
     */
    public boolean deletePasskey(String credentialId) {
        log.info("Deleting passkey with credential ID: {}", credentialId);
        
        Optional<PasskeyCredential> credential = getCredentialByCredentialId(credentialId);
        if (credential.isPresent()) {
            return deleteCredential(credential.get().getId());
        }
        
        return false;
    }
}
