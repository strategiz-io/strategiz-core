package io.strategiz.service.auth.service.totp;

import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.AuthenticationMethod;
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
    public TotpRegistrationService(UserRepository userRepository) {
        super(userRepository);
    }
    
    /**
     * Generate a new TOTP secret for a user
     * @param username the username to generate the secret for
     * @return the generated QR code as a data URI
     */
    public String generateTotpSecret(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
        User user = userOpt.get();
        
        // Generate a new TOTP secret
        String secret = secretGenerator.generate();
        
        // Find existing TOTP method or create a new one
        TotpAuthenticationMethod totpAuth = null;
        if (user.getAuthenticationMethods() != null) {
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType())) {
                    totpAuth = (TotpAuthenticationMethod) method;
                    break;
                }
            }
        }
        
        if (totpAuth == null) {
            totpAuth = new TotpAuthenticationMethod();
            totpAuth.setType("TOTP");
            totpAuth.setName("Authenticator App");
            totpAuth.setCreatedBy(username);
            totpAuth.setModifiedBy(username);
            totpAuth.setCreatedAt(new Date());
            totpAuth.setModifiedAt(new Date());
            totpAuth.setIsActive(true);
            totpAuth.setVersion(1);
            
            if (user.getAuthenticationMethods() == null) {
                user.setAuthenticationMethods(new ArrayList<>());
            }
            user.addAuthenticationMethod(totpAuth);
        }
        
        // Mark as not verified yet
        totpAuth.setSecret(secret);
        totpAuth.setLastVerifiedAt(null); // Not verified yet
        userRepository.save(user);
        
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
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return false;
        }
        
        User user = userOpt.get();
        TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
        
        if (totpAuth == null) {
            log.warn("TOTP auth method not found for user: {}", username);
            return false;
        }
        
        // Mark the TOTP as enabled by setting lastVerifiedAt
        totpAuth.setLastVerifiedAt(new Date());
        totpAuth.setModifiedAt(new Date());
        totpAuth.setModifiedBy(username);
        userRepository.save(user);
        
        log.info("TOTP enabled for user {} with session {}", username, sessionToken);
        return true;
    }
    
    /**
     * Check if TOTP is set up for a user
     * @param username the username
     * @return true if TOTP is set up, false otherwise
     */

    
    /**
     * Check if TOTP is set up for a user
     * @param username the username
     * @return true if TOTP is set up, false otherwise
     */
    public boolean isTotpSetUp(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        
        // Find TOTP authentication method
        TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
        
        // Consider verified if last verification time exists
        return totpAuth != null && totpAuth.getLastVerifiedAt() != null;
    }
    
    /**
     * Disable TOTP for a user
     * @param username the username
     */
    public void disableTotp(String username) {
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", username);
            return;
        }
        User user = userOpt.get();
        
        // Remove TOTP authentication
        if (user.getAuthenticationMethods() != null) {
            List<AuthenticationMethod> updatedMethods = new ArrayList<>();
            for (AuthenticationMethod method : user.getAuthenticationMethods()) {
                if (!(method instanceof TotpAuthenticationMethod && "TOTP".equals(method.getType()))) {
                    updatedMethods.add(method);  // Keep all non-TOTP methods
                }
            }
            user.setAuthenticationMethods(updatedMethods);
            userRepository.save(user);
            log.info("TOTP disabled for user {}", username);
        }
    }
}
