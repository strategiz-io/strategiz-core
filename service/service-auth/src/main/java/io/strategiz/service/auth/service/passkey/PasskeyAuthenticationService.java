package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.service.passkey.util.PasskeySignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service handling passkey authentication flows
 */
@Service
public class PasskeyAuthenticationService {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyAuthenticationService.class);
    
    @Value("${passkey.rpId:localhost}")
    private String rpId;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private int challengeTimeoutMs;
    
    private final PasskeyChallengeService challengeService;
    private final PasskeyCredentialRepository credentialRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public PasskeyAuthenticationService(
            PasskeyChallengeService challengeService,
            PasskeyCredentialRepository credentialRepository,
            SessionAuthBusiness sessionAuthBusiness) {
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    
    /**
     * Authentication result with tokens or error information
     */
    public record AuthenticationResult(boolean success, String accessToken, String refreshToken, String errorMessage) {}
    
    /**
     * Challenge data for authentication with full WebAuthn options
     */
    public record AuthenticationChallenge(Object publicKeyCredentialRequestOptions) {}
    
    /**
     * Authentication completion request data
     */
    public record AuthenticationCompletion(
            String credentialId,
            String authenticatorData,
            String clientDataJSON,
            String signature,
            String userHandle,
            String ipAddress,
            String deviceId) {}
    
    /**
     * Begin passkey authentication process by generating a challenge
     * 
     * @return Authentication challenge for the client
     */
    public AuthenticationChallenge beginAuthentication() {
        // Generate a challenge for this authentication
        // No user ID at this stage since we don't know which user is authenticating yet
        // Using "id" as a placeholder to satisfy database NOT NULL constraint
        String challenge = challengeService.createChallenge("id", PasskeyChallengeType.AUTHENTICATION);
        
        // For discoverable credentials, we need to return all registered credentials
        // so the browser can show available passkeys to the user
        java.util.List<java.util.Map<String, Object>> allowCredentials;
        try {
            // Get all stored passkey credentials
            java.util.List<PasskeyCredential> storedCredentials = credentialRepository.findAll();
            log.info("Found {} stored passkey credentials for authentication", storedCredentials.size());
            
            // Convert to WebAuthn format
            allowCredentials = storedCredentials.stream()
                .map(credential -> java.util.Map.<String, Object>of(
                    "id", credential.getCredentialId(),
                    "type", "public-key"
                ))
                .collect(java.util.stream.Collectors.toList());
                
            log.debug("Converted {} credentials to allowCredentials format", allowCredentials.size());
        } catch (Exception e) {
            log.warn("Error retrieving stored credentials, falling back to empty allowCredentials", e);
            allowCredentials = java.util.Collections.emptyList();
        }
        
        // Create authentication options in proper WebAuthn format
        var options = java.util.Map.of(
            "challenge", challenge,
            "timeout", challengeTimeoutMs,
            "rpId", rpId,
            "userVerification", "preferred",
            "allowCredentials", allowCredentials // Return stored credentials for discovery
        );
        
        return new AuthenticationChallenge(options);
    }
    
    /**
     * Complete passkey authentication by verifying the assertion
     * 
     * @param completion Authentication completion data
     * @return Authentication result with tokens if successful
     */
    @Transactional
    public AuthenticationResult completeAuthentication(AuthenticationCompletion completion) {
        String credentialId = completion.credentialId();
        String signature = completion.signature();
        String authenticatorData = completion.authenticatorData();
        String clientDataJSON = completion.clientDataJSON();
        String ipAddress = completion.ipAddress();
        String deviceId = completion.deviceId();
        
        // Extract and verify challenge
        String challenge = challengeService.extractChallengeFromClientData(clientDataJSON);
        boolean challengeValid = challengeService.verifyChallenge(challenge, null, PasskeyChallengeType.AUTHENTICATION);
        
        if (!challengeValid) {
            log.warn("Invalid challenge for passkey authentication: {}", challenge);
            return new AuthenticationResult(false, null, null, "Invalid challenge");
        }
        
        // Find credential by ID
        Optional<PasskeyCredential> credentialOpt = credentialRepository.findByCredentialId(credentialId);
        if (credentialOpt.isEmpty()) {
            return new AuthenticationResult(false, null, null, "Credential not found");
        }
        
        PasskeyCredential credential = credentialOpt.get();
        String userId = credential.getUserId();
        
        try {
            // Verify signature using the utility
            boolean signatureValid = PasskeySignatureVerifier.verifySignature(
                    credential.getPublicKeyBase64(),
                    authenticatorData,
                    clientDataJSON,
                    signature
            );
            
            if (!signatureValid) {
                log.warn("Invalid signature for credential ID: {}", credentialId);
                return new AuthenticationResult(false, null, null, "Invalid signature");
            }
            
            // Update last used time
            credential.setLastUsedTime(Instant.now());
            credentialRepository.save(credential);
            
            // Generate authentication tokens with proper ACR/AAL separation
            // ACR "2" = Fully authenticated, AAL "3" = Hardware crypto (passkeys)
            SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
                userId,
                List.of("passkeys"), // Authentication method used
                false, // Not partial auth - full authentication completed
                deviceId,
                ipAddress
            );
            
            String accessToken = tokenPair.accessToken();
            String refreshToken = tokenPair.refreshToken();
            
            log.info("Successfully authenticated user {} with passkey", userId);
            return new AuthenticationResult(true, accessToken, refreshToken, null);
            
        } catch (Exception e) {
            log.error("Error verifying passkey assertion", e);
            return new AuthenticationResult(false, null, null, e.getMessage());
        }
    }
    
    /**
     * Find a passkey credential by its credentialId
     */
    public Optional<Passkey> getCredentialByCredentialId(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId)
                .map(this::convertToPasskey);
    }
    
    /**
     * Check if a credential exists for the given credentialId and userId
     */
    public boolean credentialExistsForUser(String credentialId, String userId) {
        // Simple implementation that doesn't require a special repository method
        return credentialRepository.findByCredentialId(credentialId)
                .map(credential -> userId.equals(credential.getUserId()))
                .orElse(false);
    }
    
    /**
     * Convert entity to model
     */
    private Passkey convertToPasskey(PasskeyCredential entity) {
        if (entity == null) {
            return null;
        }
        
        Passkey model = new Passkey();
        model.setId(entity.getId());
        model.setCredentialId(entity.getCredentialId());
        model.setUserId(entity.getUserId());
        model.setAuthenticatorName(entity.getAuthenticatorName());
        model.setRegistrationTime(entity.getRegistrationTime());
        model.setLastUsedTime(entity.getLastUsedTime());
        model.setAaguid(entity.getAaguid());
        model.setPublicKey(entity.getPublicKey());
        model.setDeviceName(entity.getDeviceName());
        model.setUserAgent(entity.getUserAgent());
        model.setTrusted(entity.isTrusted());
        
        return model;
    }
}
