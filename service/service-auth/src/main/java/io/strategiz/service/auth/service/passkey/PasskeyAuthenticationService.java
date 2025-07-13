package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.service.passkey.util.PasskeySignatureVerifier;
import io.strategiz.service.base.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service handling passkey authentication flows
 */
@Service
public class PasskeyAuthenticationService extends BaseService {
    
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
        
        // Ensure we're using real passkey authentication, not mock data
        ensureRealApiData("PasskeyAuthenticationService");
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
        log.info("Beginning passkey authentication");
        
        // Validate real API connection
        if (!validateRealApiConnection("PasskeyAuthenticationService")) {
            throw new IllegalStateException("Real API connection validation failed");
        }
        
        // Generate challenge
        String challenge = challengeService.createChallenge(null, PasskeyChallengeType.AUTHENTICATION);
        
        // Create authentication options
        Object publicKeyCredentialRequestOptions = createAuthenticationOptions(challenge);
        
        return new AuthenticationChallenge(publicKeyCredentialRequestOptions);
    }
    
    /**
     * Create authentication options for WebAuthn
     */
    private Object createAuthenticationOptions(String challenge) {
        // Create basic authentication options
        return Map.of(
            "challenge", challenge,
            "timeout", challengeTimeoutMs,
            "rpId", rpId,
            "allowCredentials", List.of(),
            "userVerification", "preferred"
        );
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
        
        log.info("Completing passkey authentication for credential: {}", credentialId);
        
        // Validate real API connection
        if (!validateRealApiConnection("PasskeyAuthenticationService")) {
            return new AuthenticationResult(false, null, null, "Real API connection validation failed");
        }
        
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
            
            // Generate authentication tokens AND session using new unified approach
            // Extract context from challenge (we should have user email etc.)
            SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
                userId,
                null, // userEmail - could be extracted from user lookup
                List.of("passkeys"), // Authentication method used
                false, // Not partial auth - full authentication completed
                deviceId,
                deviceId, // Use deviceId as fingerprint for now
                ipAddress,
                "Passkey Client" // userAgent placeholder
            );
            
            SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
            
            String accessToken = authResult.accessToken();
            String refreshToken = authResult.refreshToken();
            
            // Log session creation status
            if (authResult.hasSession()) {
                log.info("Created session {} along with tokens for passkey auth", authResult.getSessionId());
            } else {
                log.info("Created tokens without session (session management disabled)");
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
