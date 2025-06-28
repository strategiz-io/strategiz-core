package io.strategiz.service.auth.service.totp;

import io.strategiz.data.user.model.TotpAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

/**
 * Service for handling TOTP authentication operations
 * Manages TOTP code verification and authentication
 */
@Service
public class TotpAuthenticationService extends BaseTotpService {
    public TotpAuthenticationService(UserRepository userRepository) {
        super(userRepository);
    }
    
    /**
     * Override to make public for authentication purposes
     */
    @Override
    public boolean verifyCode(String username, String code) {
        return super.verifyCode(username, code);
    }
    
    /**
     * Authenticate a user using TOTP
     * @param username the username
     * @param code the TOTP code
     * @return true if authentication was successful, false otherwise
     */
    public boolean authenticateWithTotp(String username, String code) {
        if (!verifyCode(username, code)) {
            return false;
        }
        
        // Update the last verification time
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            TotpAuthenticationMethod totpAuth = findTotpAuthMethod(user);
            
            if (totpAuth != null) {
                totpAuth.setLastVerifiedAt(new Date());
                userRepository.save(user);
            }
        }
        
        return true;
    }
    

}
