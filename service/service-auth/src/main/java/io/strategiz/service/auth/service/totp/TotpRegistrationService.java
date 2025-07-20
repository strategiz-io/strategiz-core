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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling TOTP registration operations
 * Manages the generation of TOTP secrets, QR codes, and TOTP setup/disabling
 */
@Service
public class TotpRegistrationService extends BaseTotpService {
    
    private final AuthenticationMethodRepository authMethodRepository;
    
    public TotpRegistrationService(UserRepository userRepository, 
                                   AuthenticationMethodRepository authMethodRepository) {
        super(userRepository, authMethodRepository);
        this.authMethodRepository = authMethodRepository;
    }
    
    /**
     * Generate a new TOTP secret for a user
     * @param username the username to generate the secret for
     * @return the generated QR code as a data URI
     */
    public String generateTotpSecret(String username) {
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
        UserEntity user = userOpt.get();
        
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
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.ACCOUNT_NAME, username);
        totpAuth.setEnabled(false); // Not enabled until verified
        
        authMethodRepository.saveForUser(user.getId(), totpAuth);
        
        // Generate the QR code
        return generateQrCodeUri(username, secret);
    }
    
    /**
     * Enable TOTP for a user's session
     * @param username the username
     * @param sessionToken the session token
     * @param code the TOTP code to verify
     * @return true if TOTP was successfully enabled, false otherwise
     */
    public boolean enableTotp(String username, String sessionToken, String code) {
        // Verify the code first
        if (!verifyCode(username, code)) {
            return false;
        }
        
        // Get the user and update the TOTP authentication method
        Optional<UserEntity> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return false;
        }
        
        UserEntity user = userOpt.get();
        List<AuthenticationMethodEntity> totpMethods = authMethodRepository.findByUserIdAndType(user.getId(), AuthenticationMethodType.TOTP);
        
        if (totpMethods.isEmpty()) {
            log.warn("TOTP auth method not found for user: {}", username);
            return false;
        }
        
        AuthenticationMethodEntity totpAuth = totpMethods.get(0);
        
        // Mark the TOTP as verified and enabled
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFIED, true);
        totpAuth.putMetadata(AuthenticationMethodMetadata.TotpMetadata.VERIFICATION_TIME, Instant.now().toString());
        totpAuth.setEnabled(true);
        totpAuth.markAsUsed();
        authMethodRepository.saveForUser(user.getId(), totpAuth);
        
        log.info("TOTP enabled for user {} with session {}", username, sessionToken);
        return true;
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
