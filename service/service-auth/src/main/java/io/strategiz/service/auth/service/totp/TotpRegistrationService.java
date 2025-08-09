package io.strategiz.service.auth.service.totp;

import java.time.Instant;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling TOTP registration operations
 * Manages the generation of TOTP secrets, QR codes, and TOTP setup/disabling
 */
@Service
public class TotpRegistrationService extends BaseTotpService {
    
    private final AuthenticationMethodRepository authMethodRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public TotpRegistrationService(UserRepository userRepository, 
                                   AuthenticationMethodRepository authMethodRepository,
                                   SessionAuthBusiness sessionAuthBusiness) {
        super(userRepository, authMethodRepository);
        this.authMethodRepository = authMethodRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Result holder for TOTP secret generation
     */
    public static class TotpGenerationResult {
        private final String secret;
        private final String qrCodeUri;
        
        public TotpGenerationResult(String secret, String qrCodeUri) {
            this.secret = secret;
            this.qrCodeUri = qrCodeUri;
        }
        
        public String getSecret() { return secret; }
        public String getQrCodeUri() { return qrCodeUri; }
    }
    
    /**
     * Generate a new TOTP secret for a user (legacy method)
     * @param username the username to generate the secret for
     * @return the generated QR code as a data URI
     * @deprecated Use generateTotpSecretWithDetails for new implementations
     */
    @Deprecated
    public String generateTotpSecret(String username) {
        return generateTotpSecretWithDetails(username).getQrCodeUri();
    }
    
    /**
     * Generate a new TOTP secret for a user with both secret and QR code
     * @param username the username to generate the secret for
     * @return TotpGenerationResult containing both secret and QR code
     */
    public TotpGenerationResult generateTotpSecretWithDetails(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
        UserEntity user = userOpt.get();
        
        // Get the user's email for the TOTP label
        String userEmail = user.getProfile().getEmail();
        
        // Generate a new TOTP secret
        String secret = secretGenerator.generate();
        
        // Find existing TOTP method or create a new one
        List<AuthenticationMethodEntity> existingMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        AuthenticationMethodEntity totpAuth = null;
        
        if (!existingMethods.isEmpty()) {
            totpAuth = existingMethods.get(0); // Use first TOTP method
        }
        
        if (totpAuth == null) {
            totpAuth = new AuthenticationMethodEntity(AuthenticationMethodType.TOTP, "Authenticator App");
        }
        
        // Store TOTP-specific data in metadata using constants
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY, secret);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, false);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.QR_CODE_GENERATED, true);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.BACKUP_CODES, new ArrayList<String>()); // Empty backup codes initially
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.BACKUP_CODES_USED, new ArrayList<String>());
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ALGORITHM, "SHA1"); // Default TOTP algorithm
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.DIGITS, 6); // Standard 6-digit codes
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.PERIOD, 30); // 30-second time step
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ISSUER, "Strategiz");
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ACCOUNT_NAME, userEmail); // Store email for user-friendly display
        totpAuth.setEnabled(false); // Not enabled until verified
        
        log.info("Saving TOTP auth method for user ID: {} (email: {}) with secret: {}", user.getId(), userEmail, secret.substring(0, 4) + "...");
        authMethodRepository.saveForUser(user.getId(), totpAuth);
        log.info("Successfully saved TOTP auth method for user ID: {}", user.getId());
        
        // Verify the save was successful by immediately retrieving it
        List<AuthenticationMethodEntity> savedMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        if (savedMethods.isEmpty()) {
            log.error("Failed to verify TOTP save for user ID: {} - method not found after save", user.getId());
            throw new RuntimeException("Failed to save TOTP authentication method");
        }
        log.info("Verified TOTP auth method save - found {} TOTP methods for user ID: {}", savedMethods.size(), user.getId());
        
        // Generate the QR code using the email for display
        String qrCodeUri = generateQrCodeUri(userEmail, secret);
        return new TotpGenerationResult(secret, qrCodeUri);
    }
    
    /**
     * Verify TOTP code during registration completion
     * This method allows verification of codes against unconfigured TOTP methods
     * @param username the username
     * @param code the TOTP code to verify
     * @return true if the code is valid for the pending TOTP setup, false otherwise
     */
    protected boolean verifyRegistrationCode(String username, String code) {
        log.info("Starting TOTP registration verification for user: {}", username);
        
        // First try to find user by ID (which is what we're using as username)
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found by ID, trying by email: {}", username);
            // Try finding by email as fallback
            userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                log.warn("User not found for TOTP registration verification: {}", username);
                return false;
            }
        }
        
        UserEntity user = userOpt.get();
        log.info("Found user with ID: {} for username: {}", user.getId(), username);
        
        // Log more details about the search
        log.info("Searching for TOTP methods with userId: {} and type: {}", user.getId(), AuthenticationMethodType.TOTP);
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        log.info("Found {} TOTP methods for user ID: {}", totpMethods.size(), user.getId());
        
        // Also try to list all auth methods for debugging
        if (totpMethods.isEmpty()) {
            log.warn("No TOTP methods found. Listing all auth methods for user ID: {}", user.getId());
            List<AuthenticationMethodEntity> allMethods = authMethodRepository.findByUserId(user.getId());
            log.warn("Total auth methods for user: {}, types: {}", 
                allMethods.size(), 
                allMethods.stream().map(m -> m.getType().toString()).collect(java.util.stream.Collectors.joining(", "))
            );
        }
        
        if (totpMethods.isEmpty()) {
            log.warn("TOTP auth method not found for registration verification: {}", username);
            return false;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        String secret = totpAuth.getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY);
        
        if (secret == null) {
            log.warn("TOTP secret not found for registration verification: {}", username);
            return false;
        }
        
        // Verify the code against the secret, regardless of configured status
        boolean isValid = isCodeValid(code, secret);
        if (isValid) {
            log.info("TOTP registration code verified successfully for user: {}", username);
        } else {
            log.warn("Invalid TOTP registration code for user: {}", username);
        }
        
        return isValid;
    }
    
    /**
     * Enable TOTP for a user's session and return updated auth tokens
     * @param username the username
     * @param sessionToken the session token
     * @param code the TOTP code to verify
     * @return Map containing success status and updated tokens, or null if failed
     */
    public Map<String, Object> enableTotpWithTokenUpdate(String username, String sessionToken, String code) {
        // Verify the code using registration-specific verification
        if (!verifyRegistrationCode(username, code)) {
            return null;
        }
        
        // Get the user and update the TOTP authentication method
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return null;
        }
        
        UserEntity user = userOpt.get();
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        
        if (totpMethods.isEmpty()) {
            log.warn("TOTP auth method not found for user: {}", username);
            return null;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        
        // Mark the TOTP as verified and enabled
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, true);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFICATION_TIME, Instant.now().toString());
        totpAuth.setEnabled(true);
        totpAuth.markAsUsed();
        authMethodRepository.saveForUser(user.getId(), totpAuth);
        
        // Update the session with TOTP as an authenticated method
        // This should generate a new PASETO token with ACR=2, AAL=2, and TOTP in auth methods
        Map<String, Object> authResult = sessionAuthBusiness.addAuthenticationMethod(
            sessionToken,
            "totp",
            2 // ACR level 2 for TOTP
        );
        
        log.info("TOTP enabled for user {} with updated session", username);
        return authResult;
    }
    
    /**
     * Enable TOTP for a user's session (backward compatibility)
     * @param username the username
     * @param sessionToken the session token
     * @param code the TOTP code to verify
     * @return true if TOTP was successfully enabled, false otherwise
     */
    public boolean enableTotp(String username, String sessionToken, String code) {
        Map<String, Object> result = enableTotpWithTokenUpdate(username, sessionToken, code);
        return result != null;
    }
    
    /**
     * Check if TOTP is set up for a user
     * @param username the username
     * @return true if TOTP is set up, false otherwise
     */
    public boolean isTotpSetUp(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        UserEntity user = userOpt.get();
        
        // Find TOTP authentication method
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndTypeAndIsEnabled(user.getId(), AuthenticationMethodType.TOTP, true);
        
        if (totpMethods.isEmpty()) {
            return false;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        // Check if TOTP is configured and verified
        return Boolean.TRUE.equals(totpAuth.getMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED));
    }
    
    /**
     * Disable TOTP for a user
     * @param username the username
     */
    public void disableTotp(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return;
        }
        UserEntity user = userOpt.get();
        
        // Remove TOTP authentication methods
        authMethodRepository.deleteByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        log.info("TOTP disabled for user {}", username);
    }
}
