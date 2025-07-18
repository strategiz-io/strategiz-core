package io.strategiz.service.auth.service.totp;

import java.time.Instant;
import io.strategiz.data.auth.entity.totp.TotpAuthenticationMethodEntity;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
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
        List<AuthenticationMethodEntity> authMethods = authMethodRepository.findByUserId(user.getId());
        TotpAuthenticationMethodEntity totpAuth = null;
        if (authMethods != null) {
            for (AuthenticationMethodEntity method : authMethods) {
                if (method instanceof TotpAuthenticationMethodEntity && "TOTP".equals(method.getAuthenticationMethodType())) {
                    totpAuth = (TotpAuthenticationMethodEntity) method;
                    break;
                }
            }
        }
        
        if (totpAuth == null) {
            totpAuth = new TotpAuthenticationMethodEntity();
            totpAuth.setUserId(user.getId());
            totpAuth.setName("Authenticator App");
        }
        
        // Mark as not verified yet
        totpAuth.setSecret(secret);
        totpAuth.setVerified(false);
        authMethodRepository.save(totpAuth);
        
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
        TotpAuthenticationMethodEntity totpAuth = findTotpAuthMethod(user);
        
        if (totpAuth == null) {
            log.warn("TOTP auth method not found for user: {}", username);
            return false;
        }
        
        // Mark the TOTP as verified and enabled
        totpAuth.markAsVerified();
        authMethodRepository.save(totpAuth);
        
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
        TotpAuthenticationMethodEntity totpAuth = findTotpAuthMethod(user);
        
        // Check if TOTP is configured and verified
        return totpAuth != null && totpAuth.isVerified();
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
        
        // Remove TOTP authentication
        List<AuthenticationMethodEntity> authMethods = authMethodRepository.findByUserId(user.getId());
        if (authMethods != null) {
            for (AuthenticationMethodEntity method : authMethods) {
                if (method instanceof TotpAuthenticationMethodEntity && "TOTP".equals(method.getAuthenticationMethodType())) {
                    authMethodRepository.delete(method);
                    break;
                }
            }
            log.info("TOTP disabled for user {}", username);
        }
    }
}
