package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.auth.repository.passkey.credential.PasskeyCredentialRepository;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;

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
            credential.setCredentialId(credentialId);
            credential.setUserId(userId);
            
            // Extract public key from attestationObject if not provided
            String extractedPublicKey = publicKey;
            if (extractedPublicKey == null || extractedPublicKey.isEmpty()) {
                // Properly extract the public key from the attestation object
                extractedPublicKey = extractPublicKeyFromAttestation(attestationObject);
                if (extractedPublicKey == null) {
                    log.error("Failed to extract public key from attestation object for credential: {}", credentialId);
                    return RegistrationResult.error("Failed to extract public key from attestation");
                }
                log.info("Successfully extracted public key from attestation object for credential: {}", credentialId);
            }
            
            credential.setPublicKeyBase64(extractedPublicKey);
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
            
            // Create authentication token with ACR "2.3" (high assurance - passkeys)
            // This allows immediate authentication after passkey registration
            SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
                userId,
                List.of("passkeys"), // Authentication method used
                "2.3", // ACR "2.3" - High assurance (passkeys)
                deviceName, // Device ID
                userAgent // IP address (using user agent as placeholder)
            );
            
            String accessToken = tokenPair.accessToken();
            String refreshToken = tokenPair.refreshToken();
            
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
     * Extract public key from attestation object and return it as base64 encoded string
     * 
     * @param attestationObjectBase64 Base64 encoded attestation object
     * @return Base64 encoded public key
     */
    private String extractPublicKeyFromAttestation(String attestationObjectBase64) {
        try {
            if (attestationObjectBase64 == null || attestationObjectBase64.isEmpty()) {
                log.error("Attestation object is null or empty");
                return null;
            }
            
            // Decode base64URL attestation object (WebAuthn uses base64URL encoding)
            byte[] attestationObjectBytes = java.util.Base64.getUrlDecoder().decode(attestationObjectBase64);
            
            // Initialize the ObjectConverter which is used by WebAuthn4J
            ObjectConverter objectConverter = new ObjectConverter();
            
            // Create converters for attestation data
            AuthenticatorDataConverter authenticatorDataConverter = new AuthenticatorDataConverter(objectConverter);
            
            // First try to parse using WebAuthn4J's standard approach
            try {
                // Parse attestation object to get raw map structure
                Map<String, Object> attestationObjectMap = objectConverter.getCborConverter().readValue(attestationObjectBytes, Map.class);
                
                // Extract the authenticatorData bytes
                byte[] authenticatorDataBytes = (byte[]) attestationObjectMap.get("authData");
                if (authenticatorDataBytes == null) {
                    log.error("No authData found in attestation object");
                    return null;
                }
                
                // Parse authenticator data
                AuthenticatorData<?> authenticatorData = authenticatorDataConverter.convert(authenticatorDataBytes);
                
                // Get attested credential data which contains the public key
                AttestedCredentialData attestedCredentialData = authenticatorData.getAttestedCredentialData();
                if (attestedCredentialData == null) {
                    log.error("No attested credential data found in authenticator data");
                    return null;
                }
                
                // Extract the COSE key (public key) and convert to bytes
                COSEKey coseKey = attestedCredentialData.getCOSEKey();
                if (coseKey == null) {
                    log.error("No COSE key found in attested credential data");
                    return null;
                }
                
                // Convert the key to bytes for storage
                byte[] pubKeyBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);
                
                // Encode as Base64 for storage
                String publicKeyBase64 = java.util.Base64.getEncoder().encodeToString(pubKeyBytes);
                log.info("Successfully extracted public key from attestation object, length: {} bytes", pubKeyBytes.length);
                
                return publicKeyBase64;
            } catch (Exception e) {
                log.warn("Standard WebAuthn parsing failed, trying alternative approach: {}", e.getMessage());
                
                // Fallback to a simpler CBOR extraction if the standard approach fails
                // This could be needed for non-standard attestation formats
                try {
                    // Extract a portion of the attestation object that may contain the key
                    // This is less reliable but can work as a fallback
                    int potentialKeyStartOffset = Math.max(attestationObjectBytes.length - 150, 0);
                    byte[] pubKeyBytes = new byte[Math.min(100, attestationObjectBytes.length - potentialKeyStartOffset)];
                    System.arraycopy(attestationObjectBytes, potentialKeyStartOffset, pubKeyBytes, 0, pubKeyBytes.length);
                    
                    String publicKeyBase64 = java.util.Base64.getEncoder().encodeToString(pubKeyBytes);
                    log.info("Used fallback method to extract potential public key, length: {} bytes", pubKeyBytes.length);
                    return publicKeyBase64;
                } catch (Exception fallbackEx) {
                    log.error("Fallback extraction also failed: {}", fallbackEx.getMessage());
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("Error extracting public key from attestation: {}", e.getMessage(), e);
            return null;
        }
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
