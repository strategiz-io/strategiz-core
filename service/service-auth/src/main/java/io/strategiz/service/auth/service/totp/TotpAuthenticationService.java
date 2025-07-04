package io.strategiz.service.auth.service.totp;

import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.DomainService;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * Service for handling TOTP authentication operations
 * Manages TOTP code verification and authentication
 */
@Service
@DomainService(domain = "auth")
public class TotpAuthenticationService extends BaseTotpService {
    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationService.class);
    
    public TotpAuthenticationService(UserRepository userRepository) {
        super(userRepository);
    }
    
    /**
     * Override to make public for authentication purposes
     * @throws StrategizException if the code verification fails
     */
    @Override
    public boolean verifyCode(String username, String code) {
        boolean isVerified = super.verifyCode(username, code);
        if (!isVerified) {
            // Get remaining attempts - in a real system this would be stored and tracked
            int attemptsRemaining = 3; // Example value
            log.debug("TOTP verification failed for user: {}, attempts remaining: {}", username, attemptsRemaining);
            throw new StrategizException(AuthErrors.TOTP_INVALID_CODE, username);
        }
        return true;
    }
    
    /**
     * Authenticate a user using TOTP
     * @param username the username
     * @param code the TOTP code
     * @throws StrategizException if the user does not exist or TOTP verification fails
     */
    public void authenticateWithTotp(String username, String code) {
        // verifyCode will throw an exception if code verification fails
        verifyCode(username, code);
        
        // Update the last verification time
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isEmpty()) {
            throw new StrategizException(AuthErrors.USER_NOT_FOUND, username);
        }
            
        User user = userOpt.get();
        TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
        if (totpAuth == null) {
            log.error("User {} has no TOTP authentication method configured", username);
            throw new StrategizException(AuthErrors.VALIDATION_FAILED, "TOTP is not enabled for this user");
        }
        
        totpAuth.setLastVerifiedAt(new Date());
        userRepository.save(user);
        
        log.info("TOTP authentication successful for user: {}", username);
    }
    

}
