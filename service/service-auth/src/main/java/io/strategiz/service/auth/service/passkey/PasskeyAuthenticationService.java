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
     * Challenge data for authentication
     */
    public record AuthenticationChallenge(String rpId, String challenge, int timeout) {}
    
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
        
        // Create authentication options
        return new AuthenticationChallenge(rpId, challenge, challengeTimeoutMs);
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
            
            // Generate auth tokens via business-token-auth using reflection to avoid direct dependencies
            Object tokenPair = sessionAuthBusiness.createTokenPair(userId, deviceId, ipAddress);
            
            // Extract token values using reflection
            String accessToken = null;
            String refreshToken = null;
            try {
                var method1 = tokenPair.getClass().getMethod("accessToken");
                var method2 = tokenPair.getClass().getMethod("refreshToken");
                accessToken = (String)method1.invoke(tokenPair);
                refreshToken = (String)method2.invoke(tokenPair);
            } catch (Exception e) {
                log.error("Error extracting token values", e);
                return new AuthenticationResult(false, null, null, "Error generating auth tokens");
            }
            
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
