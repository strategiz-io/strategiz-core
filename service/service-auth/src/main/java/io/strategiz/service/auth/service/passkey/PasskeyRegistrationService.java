package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service handling passkey registration flows
 */
@Service
public class PasskeyRegistrationService {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationService.class);
    
    @Value("${passkey.rpId:localhost}")
    private String rpId;
    
    @Value("${passkey.rpName:Strategiz}")
    private String rpName;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private int challengeTimeoutMs;
    
    private final PasskeyChallengeService challengeService;
    private final PasskeyCredentialRepository credentialRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public PasskeyRegistrationService(
            PasskeyChallengeService challengeService,
            PasskeyCredentialRepository credentialRepository,
            SessionAuthBusiness sessionAuthBusiness) {
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Registration challenge data
     */
    public record RegistrationChallenge(
            String rpId,
            String rpName,
            String username,
            String userId,
            String challenge,
            int timeout,
            AuthenticatorSelectionCriteria authenticatorSelection,
            String attestation,
            boolean excludeCredentials) {}
            
    /**
     * Authenticator selection criteria for registration
     */
    public record AuthenticatorSelectionCriteria(
            String authenticatorAttachment,  // "platform", "cross-platform", or null for both
            String residentKey,              // "required", "preferred", or "discouraged"
            boolean requireResidentKey,       // Legacy property, use residentKey instead
            String userVerification)         // "required", "preferred", or "discouraged"
    {}
    
    /**
     * Registration request data
     */
    public record RegistrationRequest(String userId, String username) {}
    
    /**
     * Registration completion data
     */
    public record RegistrationCompletion(
            String userId,
            String credentialId,
            String publicKey,  // Base64 encoded public key
            String attestationObject,
            String clientDataJSON,
            String userAgent,
            String deviceName) {}
    
    /**
     * Authentication tokens
     */
    public record AuthTokens(String accessToken, String refreshToken) {}
    

    
    /**
     * Registration result data
     */
    public record RegistrationResult(boolean success, String credentialId, Object result) {
        // Constructor to handle error message
        public static RegistrationResult error(String errorMessage) {
            return new RegistrationResult(false, null, errorMessage);
        }
        
        // Constructor to handle success with tokens
        public static RegistrationResult success(String credentialId, AuthTokens tokens) {
            return new RegistrationResult(true, credentialId, tokens);
        }
    }
    
    /**
     * Begin passkey registration process
     * 
     * @param request Registration request with user information
     * @return Registration challenge for the client
     */
    public RegistrationChallenge beginRegistration(RegistrationRequest request) {
        String userId = request.userId();
        String username = request.username();
        
        // Generate a challenge for this registration
        String challenge = challengeService.createChallenge(userId, PasskeyChallengeType.REGISTRATION);
        
        // Configure authenticator selection criteria to support both platform and cross-platform authenticators
        AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(
            null,              // null = support both platform and cross-platform authenticators
            "preferred",       // Prefer resident keys (discoverable credentials)
            false,            // Legacy property, using residentKey instead
            "preferred"       // Prefer user verification
        );
        
        // Create registration options
        return new RegistrationChallenge(
            rpId,
            rpName,
            username,
            userId,
            challenge,
            challengeTimeoutMs,
            authenticatorSelection,
            "none",           // Attestation conveyance preference - "none", "indirect", "direct", or "enterprise"
            false             // Whether to exclude existing credentials
        );
    }
    
    /**
     * Complete passkey registration by verifying and storing the credential
     * 
     * @param completion Registration completion data
     * @return Registration result
     */
    @Transactional
    public RegistrationResult completeRegistration(RegistrationCompletion completion) {
        String userId = completion.userId();
        String credentialId = completion.credentialId();
        String publicKey = completion.publicKey();
        String attestationObject = completion.attestationObject();
        String clientDataJSON = completion.clientDataJSON();
        String userAgent = completion.userAgent();
        String deviceName = completion.deviceName();
        
        // Extract and verify challenge
        String challenge = challengeService.extractChallengeFromClientData(clientDataJSON);
        boolean challengeValid = challengeService.verifyChallenge(challenge, userId, PasskeyChallengeType.REGISTRATION);
        
        if (!challengeValid) {
            log.warn("Invalid challenge for passkey registration: {}", challenge);
            return new RegistrationResult(false, null, "Invalid challenge");
        }
        
        try {
            // Check if credential already exists
            if (credentialRepository.existsByCredentialId(credentialId)) {
                log.warn("Credential already exists: {}", credentialId);
                return new RegistrationResult(false, null, "Credential already registered");
            }
            
            // Create and store the new credential
            PasskeyCredential credential = new PasskeyCredential();
            credential.setId(UUID.randomUUID().toString());
            credential.setCredentialId(credentialId);
            credential.setUserId(userId);
            credential.setPublicKeyBase64(publicKey);  // Already Base64 encoded
            credential.setDeviceName(deviceName);
            credential.setUserAgent(userAgent);
            credential.setRegistrationTime(Instant.now());
            credential.setLastUsedTime(Instant.now());
            credential.setTrusted(true);  // This credential is trusted
            
            // Extract AAGUID from attestation if available
            String aaguid = extractAaguidFromAttestation(attestationObject);
            if (aaguid != null) {
                credential.setAaguid(aaguid);
                credential.setAuthenticatorName(lookupAuthenticatorName(aaguid));
            }
            
            // Save the credential
            PasskeyCredential savedCredential = credentialRepository.save(credential);
            
            log.info("Successfully registered passkey for user {}, credential ID: {}", userId, credentialId);
            
            // Use the sessionAuthBusiness to generate tokens upon successful registration
            // This allows immediate authentication after passkey registration
            Object tokenPair = sessionAuthBusiness.createTokenPair(userId, deviceName, userAgent);
            
            // Extract token values using reflection to avoid direct dependency
            String accessToken = null;
            String refreshToken = null;
            try {
                var method1 = tokenPair.getClass().getMethod("accessToken");
                var method2 = tokenPair.getClass().getMethod("refreshToken");
                accessToken = (String)method1.invoke(tokenPair);
                refreshToken = (String)method2.invoke(tokenPair);
            } catch (Exception e) {
                log.error("Error extracting token values", e);
                // Fallback to null tokens
            }
            
            AuthTokens authTokens = new AuthTokens(accessToken, refreshToken);
            return RegistrationResult.success(savedCredential.getId(), authTokens);
            
        } catch (Exception e) {
            log.error("Error registering passkey credential", e);
            return RegistrationResult.error(e.getMessage());
        }
    }
    
    /**
     * Extract AAGUID from attestation object if available
     */
    private String extractAaguidFromAttestation(String attestationObject) {
        // In a real implementation, this would parse the CBOR attestation object
        // and extract the AAGUID. For simplicity, we're returning null.
        return null;
    }
    
    /**
     * Look up authenticator name from AAGUID using FIDO Metadata Service
     */
    private String lookupAuthenticatorName(String aaguid) {
        // In a real implementation, this would look up the authenticator name
        // in the FIDO Metadata Service or a local cache.
        return "Unknown Authenticator";
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
