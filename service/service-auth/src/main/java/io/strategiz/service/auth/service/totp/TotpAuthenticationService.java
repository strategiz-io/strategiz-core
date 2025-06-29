package io.strategiz.service.auth.service.totp;

import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.DomainService;
import io.strategiz.framework.exception.ErrorFactory;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Service for handling TOTP authentication operations
 * Manages TOTP code verification and authentication
 */
@Service
@DomainService(domain = "auth")
public class TotpAuthenticationService extends BaseTotpService {
    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationService.class);
    private final ErrorFactory errorFactory;
    
    public TotpAuthenticationService(UserRepository userRepository, ErrorFactory errorFactory) {
        super(userRepository);
        this.errorFactory = errorFactory;
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
            throw errorFactory.totpVerificationFailed(username, attemptsRemaining);
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
        User user = userRepository.findById(username)
            .orElseThrow(() -> errorFactory.userNotFound(username));
            
        TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
        if (totpAuth == null) {
            log.error("User {} has no TOTP authentication method configured", username);
            throw errorFactory.validationFailed("TOTP is not enabled for this user");
        }
        
        totpAuth.setLastVerifiedAt(new Date());
        userRepository.save(user);
        
        log.info("TOTP authentication successful for user: {}", username);
    }
    

}
