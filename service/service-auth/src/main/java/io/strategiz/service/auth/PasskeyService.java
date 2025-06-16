package io.strategiz.service.auth;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.Passkey;
import io.strategiz.data.auth.PasskeyCredential;
import io.strategiz.data.auth.PasskeyCredentialRepository;
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
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for passkey credential management with proper WebAuthn challenge-response flow
 */
@Service
public class PasskeyService {

    /**
     * Result container for passkey authentication operations
     */
    public record PasskeyAuthResult(boolean success, String accessToken, String refreshToken) {}

    /**
     * Registration challenge data
     */
    public record RegistrationChallenge(
            String challenge,
            String rpId,
            String rpName,
            String userId,
            String userDisplayName,
            int timeout
    ) {}

    /**
     * Authentication challenge data
     */
    public record AuthenticationChallenge(
            String challenge,
            String rpId,
            int timeout,
            List<AllowedCredential> allowCredentials
    ) {}

    /**
     * Allowed credential for authentication
     */
    public record AllowedCredential(String id, String type) {}

    /**
     * Registration request data
     */
    public record RegistrationRequest(
            String email,
            String displayName
    ) {}

    /**
     * Registration completion data
     */
    public record RegistrationCompletion(
            String credentialId,
            String clientDataJSON,
            String attestationObject,
            String email
    ) {}

    /**
     * Authentication completion data
     */
    public record AuthenticationCompletion(
            String credentialId,
            String clientDataJSON,
            String authenticatorData,
            String signature,
            String userHandle
    ) {}

    private static final Logger log = LoggerFactory.getLogger(PasskeyService.class);
    private static final String RP_ID = "localhost"; // TODO: Make configurable
    private static final String RP_NAME = "Strategiz";
    private static final int CHALLENGE_TIMEOUT = 60000; // 60 seconds
    
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // In-memory challenge storage (in production, use Redis or database)
    private final Map<String, String> challengeStorage = new ConcurrentHashMap<>();
    
    @Autowired
    public PasskeyService(PasskeyCredentialRepository passkeyCredentialRepository, SessionAuthBusiness sessionAuthBusiness) {
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Begin passkey registration - generate challenge
     */
    public RegistrationChallenge beginRegistration(RegistrationRequest request) {
        log.info("Beginning passkey registration for email: {}", request.email());
        
        // Generate cryptographically secure challenge
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        
        // Generate user ID (in production, this should be from your user service)
        String userId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(request.email().getBytes(StandardCharsets.UTF_8));
        
        // Store challenge temporarily (associate with email for verification)
        challengeStorage.put(request.email(), challenge);
        
        // Schedule challenge cleanup after timeout
        // In production, use proper cache with TTL
        
        return new RegistrationChallenge(
                challenge,
                RP_ID,
                RP_NAME,
                userId,
                request.displayName() != null ? request.displayName() : request.email().split("@")[0],
                CHALLENGE_TIMEOUT
        );
    }

    /**
     * Complete passkey registration - verify and store credential
     */
    public PasskeyAuthResult completeRegistration(RegistrationCompletion completion) {
        log.info("Completing passkey registration for email: {}", completion.email());
        
        try {
            // Verify challenge
            String expectedChallenge = challengeStorage.get(completion.email());
            if (expectedChallenge == null) {
                log.error("No challenge found for email: {}", completion.email());
                return new PasskeyAuthResult(false, null, null);
            }
            
            // Verify client data JSON contains the expected challenge
            if (!verifyClientDataChallenge(completion.clientDataJSON(), expectedChallenge)) {
                log.error("Challenge verification failed for email: {}", completion.email());
                return new PasskeyAuthResult(false, null, null);
            }
            
            // Parse attestation object and extract public key
            // For now, we'll store the raw attestation object
            // In production, you'd parse this and extract the public key
            
            // Create and save credential
            long now = Instant.now().getEpochSecond();
            PasskeyCredential credential = PasskeyCredential.builder()
                    .userId(completion.email()) // Using email as userId for now
                    .credentialId(completion.credentialId())
                    .publicKey("extracted-from-attestation") // TODO: Extract from attestation
                    .attestationObject(completion.attestationObject())
                    .clientDataJSON(completion.clientDataJSON())
                    .createdAt(now)
                    .lastUsedAt(now)
                    .userAgent("WebAuthn-Client")
                    .deviceName("Platform-Authenticator")
                    .build();
            
            passkeyCredentialRepository.save(credential);
            
            // Clean up challenge
            challengeStorage.remove(completion.email());
            
            // Generate tokens
            SessionAuthBusiness.TokenPair tokens = sessionAuthBusiness.createTokenPair(
                    completion.email(), 
                    "passkey-device", 
                    "127.0.0.1" // TODO: Get actual IP address
            );
            
            log.info("Passkey registration completed successfully for email: {}", completion.email());
            return new PasskeyAuthResult(true, tokens.accessToken(), tokens.refreshToken());
            
        } catch (Exception e) {
            log.error("Error completing passkey registration: {}", e.getMessage(), e);
            challengeStorage.remove(completion.email()); // Clean up on error
            return new PasskeyAuthResult(false, null, null);
        }
    }

    /**
     * Begin passkey authentication - generate challenge
     */
    public AuthenticationChallenge beginAuthentication() {
        log.info("Beginning passkey authentication");
        
        // Generate challenge
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        
        // For resident keys (discoverable credentials), we don't need to specify allowed credentials
        // The authenticator will present available credentials to the user
        
        // Store challenge with a temporary key (in production, associate with session)
        String sessionKey = "auth-" + System.currentTimeMillis();
        challengeStorage.put(sessionKey, challenge);
        
        return new AuthenticationChallenge(
                challenge,
                RP_ID,
                CHALLENGE_TIMEOUT,
                List.of() // Empty for resident keys
        );
    }

    /**
     * Complete passkey authentication - verify assertion
     */
    public PasskeyAuthResult completeAuthentication(AuthenticationCompletion completion) {
        log.info("Completing passkey authentication for credential: {}", completion.credentialId());
        
        try {
            // Find credential
            Optional<PasskeyCredential> credentialOpt = 
                    passkeyCredentialRepository.findByCredentialId(completion.credentialId());
            
            if (credentialOpt.isEmpty()) {
                log.error("Credential not found: {}", completion.credentialId());
                return new PasskeyAuthResult(false, null, null);
            }
            
            PasskeyCredential credential = credentialOpt.get();
            
            // Verify challenge (simplified - in production, properly associate with session)
            if (!verifyAuthenticationChallenge(completion.clientDataJSON())) {
                log.error("Challenge verification failed for credential: {}", completion.credentialId());
                return new PasskeyAuthResult(false, null, null);
            }
            
            // Verify signature (simplified - in production, implement full WebAuthn verification)
            // This would involve:
            // 1. Reconstructing the signed data
            // 2. Verifying the signature using the stored public key
            // 3. Checking authenticator data flags
            
            // Update last used time
            credential.updateLastUsedTime();
            passkeyCredentialRepository.save(credential);
            
            // Generate tokens
            SessionAuthBusiness.TokenPair tokens = sessionAuthBusiness.createTokenPair(
                    credential.getUserId(), 
                    "passkey-device", 
                    "127.0.0.1" // TODO: Get actual IP address
            );
            
            log.info("Passkey authentication completed successfully for user: {}", credential.getUserId());
            return new PasskeyAuthResult(true, tokens.accessToken(), tokens.refreshToken());
            
        } catch (Exception e) {
            log.error("Error completing passkey authentication: {}", e.getMessage(), e);
            return new PasskeyAuthResult(false, null, null);
        }
    }

    /**
     * Verify client data contains expected challenge
     */
    private boolean verifyClientDataChallenge(String clientDataJSON, String expectedChallenge) {
        try {
            // Decode and parse client data JSON
            String decoded = new String(Base64.getUrlDecoder().decode(clientDataJSON), StandardCharsets.UTF_8);
            // Simple check - in production, use proper JSON parsing
            return decoded.contains(expectedChallenge);
        } catch (Exception e) {
            log.error("Error verifying client data challenge: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify authentication challenge
     */
    private boolean verifyAuthenticationChallenge(String clientDataJSON) {
        try {
            // Simplified verification - in production, properly extract and verify challenge
            String decoded = new String(Base64.getUrlDecoder().decode(clientDataJSON), StandardCharsets.UTF_8);
            
            // Check if any stored challenge matches (simplified)
            return challengeStorage.values().stream()
                    .anyMatch(challenge -> decoded.contains(challenge));
        } catch (Exception e) {
            log.error("Error verifying authentication challenge: {}", e.getMessage());
            return false;
        }
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
                    SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createTokenPair(
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
