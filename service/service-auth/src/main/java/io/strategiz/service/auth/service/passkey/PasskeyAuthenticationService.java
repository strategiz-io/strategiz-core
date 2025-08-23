package io.strategiz.service.auth.service.passkey;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.service.auth.model.passkey.Passkey;
import io.strategiz.service.auth.model.passkey.PasskeyChallengeType;
import io.strategiz.service.auth.service.passkey.util.PasskeySignatureVerifier;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.data.user.repository.UserRepository;
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
    
    @Override
    protected String getModuleName() {
        return "service-auth";
    }
    
    private static final Logger log = LoggerFactory.getLogger(PasskeyAuthenticationService.class);
    
    @Value("${passkey.rpId:localhost}")
    private String rpId;
    
    @Value("${passkey.challengeTimeoutMs:60000}")
    private int challengeTimeoutMs;
    
    private final PasskeyChallengeService challengeService;
    private final AuthenticationMethodRepository authMethodRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final UserRepository userRepository;
    
    public PasskeyAuthenticationService(
            PasskeyChallengeService challengeService,
            AuthenticationMethodRepository authMethodRepository,
            SessionAuthBusiness sessionAuthBusiness,
            UserRepository userRepository) {
        this.challengeService = challengeService;
        this.authMethodRepository = authMethodRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.userRepository = userRepository;
        
        // Ensure we're using real passkey authentication, not mock data
        ensureRealApiData("PasskeyAuthenticationService");
    }
    
    
    /**
     * Authentication result with tokens or error information
     */
    public record AuthenticationResult(boolean success, String accessToken, String refreshToken, String userId, String errorMessage) {
        // Convenience constructor for failure
        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, null, null, null, errorMessage);
        }
        
        // Convenience constructor for success
        public static AuthenticationResult success(String accessToken, String refreshToken, String userId) {
            return new AuthenticationResult(true, accessToken, refreshToken, userId, null);
        }
    }
    
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
        
        // Generate challenge using SYSTEM user for authentication flows
        String challenge = challengeService.createChallenge(ModuleConstants.SYSTEM_USER_ID, PasskeyChallengeType.AUTHENTICATION);
        
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
            return AuthenticationResult.failure("Real API connection validation failed");
        }
        
        // Extract and verify challenge
        String challenge = challengeService.extractChallengeFromClientData(clientDataJSON);
        boolean challengeValid = challengeService.verifyChallenge(challenge, null, PasskeyChallengeType.AUTHENTICATION);
        
        if (!challengeValid) {
            log.warn("Invalid challenge for passkey authentication: {}", challenge);
            return AuthenticationResult.failure("Invalid challenge");
        }
        
        // Find credential by ID using collection group query
        Optional<AuthenticationMethodEntity> authMethodOpt = authMethodRepository.findByPasskeyCredentialId(credentialId);
        if (authMethodOpt.isEmpty()) {
            return AuthenticationResult.failure("Credential not found");
        }
        
        AuthenticationMethodEntity authMethod = authMethodOpt.get();
        String publicKeyBase64 = authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.PUBLIC_KEY_BASE64);
        
        // Extract userId from the authentication method's path
        // The authentication method repository should have set this, but we can also extract from path
        String userId = extractUserIdFromAuthMethod(authMethod);
        
        try {
            // Verify signature using the utility
            boolean signatureValid = PasskeySignatureVerifier.verifySignature(
                    publicKeyBase64,
                    authenticatorData,
                    clientDataJSON,
                    signature
            );
            
            if (!signatureValid) {
                log.warn("Invalid signature for credential ID: {}", credentialId);
                return AuthenticationResult.failure("Invalid signature");
            }
            
            // Update last used time in authentication method
            authMethod.markAsUsed();
            authMethodRepository.saveForUser(userId, authMethod);
            
            // Get user's trading mode from profile
            String tradingMode = userRepository.findById(userId)
                .map(user -> user.getProfile() != null ? user.getProfile().getTradingMode() : "demo")
                .orElse("demo");
            
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
                "Passkey Client", // userAgent placeholder
                tradingMode
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
            return AuthenticationResult.success(accessToken, refreshToken, userId);
            
        } catch (Exception e) {
            log.error("Error verifying passkey assertion", e);
            return AuthenticationResult.failure(e.getMessage());
        }
    }
    
    /**
     * Find a passkey credential by its credentialId
     */
    public Optional<Passkey> getCredentialByCredentialId(String credentialId) {
        return authMethodRepository.findByPasskeyCredentialId(credentialId)
                .map(this::convertAuthMethodToPasskey);
    }
    
    /**
     * Check if a credential exists for the given credentialId and userId
     */
    public boolean credentialExistsForUser(String credentialId, String userId) {
        // Use collection group query to find authentication method
        return authMethodRepository.findByPasskeyCredentialId(credentialId)
                .map(authMethod -> {
                    String methodUserId = extractUserIdFromAuthMethod(authMethod);
                    return userId.equals(methodUserId);
                })
                .orElse(false);
    }
    
    /**
     * Convert entity to model
     */
    /**
     * Extract userId from authentication method
     * The method should contain userId info or we extract from document path
     */
    private String extractUserIdFromAuthMethod(AuthenticationMethodEntity authMethod) {
        // First check if we have it in metadata (some implementations might store it)
        String userId = authMethod.getMetadataAsString("userId");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        
        // Otherwise, we need to extract from the repository implementation
        // The repository should handle this in the findByPasskeyCredentialId method
        // For now, throw exception as this should be handled by repository
        throw new IllegalStateException("Unable to extract userId from authentication method. Repository implementation should handle this.");
    }
    
    /**
     * Convert AuthenticationMethodEntity to Passkey model
     */
    private Passkey convertAuthMethodToPasskey(AuthenticationMethodEntity authMethod) {
        if (authMethod == null) {
            return null;
        }
        
        Passkey model = new Passkey();
        model.setId(authMethod.getId());
        model.setCredentialId(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID));
        model.setUserId(extractUserIdFromAuthMethod(authMethod));
        model.setAuthenticatorName(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.AUTHENTICATOR_NAME));
        model.setRegistrationTime(Instant.parse(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.REGISTRATION_TIME)));
        model.setLastUsedTime(authMethod.getLastUsedAt());
        model.setAaguid(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.AAGUID));
        
        // Decode public key from base64
        String publicKeyBase64 = authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.PUBLIC_KEY_BASE64);
        if (publicKeyBase64 != null) {
            model.setPublicKey(java.util.Base64.getDecoder().decode(publicKeyBase64));
        }
        
        model.setDeviceName(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.DEVICE_NAME));
        model.setUserAgent(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.USER_AGENT));
        model.setTrusted(Boolean.parseBoolean(authMethod.getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.TRUSTED)));
        
        return model;
    }
}
