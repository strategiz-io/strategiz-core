package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.converter.PasskeyCredentialConverter;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.auth.exception.AuthErrorDetails;
import io.strategiz.framework.exception.StrategizException;

// Import WebAuthn4J libraries for proper attestation parsing
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.converter.AuthenticatorDataConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service handling passkey registration flows
 */
@Service
public class PasskeyRegistrationService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyRegistrationService.class);
    
    @Value("${passkey.rpId:localhost}")
    private String rpId;
    
    @Value("${passkey.rpName:Strategiz}")
    private String rpName;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private int challengeTimeoutMs;
    
    private final PasskeyChallengeService challengeService;
    private final PasskeyCredentialRepository credentialRepository;
    private final PasskeyCredentialConverter credentialConverter;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public PasskeyRegistrationService(
            PasskeyChallengeService challengeService,
            PasskeyCredentialRepository credentialRepository,
            PasskeyCredentialConverter credentialConverter,
            SessionAuthBusiness sessionAuthBusiness) {
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.credentialConverter = credentialConverter;
        this.sessionAuthBusiness = sessionAuthBusiness;
        
        // Ensure we're using real passkey registration, not mock data
        ensureRealApiData("PasskeyRegistrationService");
    }
    
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
            String attestationObject,
            String clientDataJSON,
            String deviceId,
            String deviceName,
            String userAgent) {}
    
    /**
     * Authenticator selection criteria
     */
    public record AuthenticatorSelectionCriteria(
            String authenticatorAttachment,
            String residentKey,
            boolean requireResidentKey,
            String userVerification) {}
    
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
     * Registration result
     */
    public record RegistrationResult(boolean success, String credentialId, Object result) {
        public AuthTokens tokens() {
            return (AuthTokens) result;
        }
    }
    
    /**
     * Authentication tokens
     */
    public record AuthTokens(String accessToken, String refreshToken) {}
    
    /**
     * Begin passkey registration process by generating a challenge
     * 
     * @param request Registration request with user details
     * @return Registration challenge for the client
     */
    public RegistrationChallenge beginRegistration(RegistrationRequest request) {
        String userId = request.userId();
        String username = request.username();
        
        log.info("Beginning passkey registration for user: {}", userId);
        
        // Validate real API connection
        if (!validateRealApiConnection("PasskeyRegistrationService")) {
            throw new StrategizException(AuthErrorDetails.EXTERNAL_SERVICE_ERROR, "service-auth", "PasskeyRegistrationService", "validateRealApiConnection");
        }
        
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
        String attestationObject = completion.attestationObject();
        String clientDataJSON = completion.clientDataJSON();
        String deviceId = completion.deviceId();
        String deviceName = completion.deviceName();
        String userAgent = completion.userAgent();
        
        log.info("Completing passkey registration for user: {}", userId);
        
        // Validate real API connection
        if (!validateRealApiConnection("PasskeyRegistrationService")) {
            return new RegistrationResult(false, credentialId, "Real API connection validation failed");
        }
        
        try {
            // Extract and verify challenge
            String challenge = challengeService.extractChallengeFromClientData(clientDataJSON);
            boolean challengeValid = challengeService.verifyChallenge(challenge, userId, PasskeyChallengeType.REGISTRATION);
            
            if (!challengeValid) {
                log.warn("Invalid challenge for passkey registration: {}", challenge);
                return new RegistrationResult(false, credentialId, "Invalid challenge");
            }
            
            // Check if credential already exists
            if (credentialRepository.findByCredentialId(credentialId).isPresent()) {
                log.warn("Credential already exists: {}", credentialId);
                return new RegistrationResult(false, credentialId, "Credential already exists");
            }
            
            // Parse attestation to extract public key
            byte[] publicKeyBytes = extractPublicKeyFromAttestation(attestationObject);
            if (publicKeyBytes == null) {
                log.warn("Could not extract public key from attestation");
                return new RegistrationResult(false, credentialId, "Could not extract public key");
            }
            
            // Create and save credential
            PasskeyCredential credential = new PasskeyCredential();
            // Don't set ID manually - let JPA generate it to avoid optimistic locking issues
            credential.setCredentialId(credentialId);
            credential.setUserId(userId);
            credential.setPublicKey(publicKeyBytes);
            credential.setAuthenticatorName(deviceName != null ? deviceName : "Unknown Device");
            credential.setRegistrationTime(Instant.now());
            credential.setLastUsedTime(Instant.now());
            credential.setAaguid(""); // Could be extracted from attestation
            credential.setDeviceName(deviceName);
            credential.setUserAgent(userAgent);
            credential.setTrusted(true);
            
            credentialRepository.save(credentialConverter.toEntity(credential));
            
            // Generate authentication tokens
            SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
                userId,
                List.of("passkeys"), // Authentication method used
                "2.2", // ACR "2.2" - Strong assurance for passkeys
                deviceId,
                null // IP address not available in registration
            );
            
            AuthTokens tokens = new AuthTokens(tokenPair.accessToken(), tokenPair.refreshToken());
            
            log.info("Successfully registered passkey for user: {}", userId);
            return new RegistrationResult(true, credentialId, tokens);
            
        } catch (Exception e) {
            log.error("Error completing passkey registration", e);
            return new RegistrationResult(false, credentialId, e.getMessage());
        }
    }
    
    /**
     * Extract public key from attestation object
     */
    private byte[] extractPublicKeyFromAttestation(String attestationObject) {
        try {
            // Use WebAuthn4J to parse attestation
            ObjectConverter objectConverter = new ObjectConverter();
            AuthenticatorDataConverter converter = new AuthenticatorDataConverter(objectConverter);
            
            // Decode base64url attestation object
            byte[] attestationBytes = java.util.Base64.getUrlDecoder().decode(attestationObject);
            
            // Parse CBOR attestation object
            Map<String, Object> attestationMap = objectConverter.getCborConverter().readValue(attestationBytes, Map.class);
            
            // Extract authData
            byte[] authDataBytes = (byte[]) attestationMap.get("authData");
            
            // Parse authenticator data
            AuthenticatorData authData = converter.convert(authDataBytes);
            
            // Extract attested credential data
            AttestedCredentialData attestedCredentialData = authData.getAttestedCredentialData();
            if (attestedCredentialData == null) {
                return null;
            }
            
            // Extract COSE key
            COSEKey coseKey = attestedCredentialData.getCOSEKey();
            if (coseKey == null) {
                return null;
            }
            
            // Convert to bytes
            return objectConverter.getCborConverter().writeValueAsBytes(coseKey);
            
        } catch (Exception e) {
            log.warn("Error extracting public key from attestation", e);
            return null;
        }
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

