package io.strategiz.service.auth.service.passkey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationChallenge;
import io.strategiz.service.auth.service.passkey.PasskeyRegistrationService.RegistrationRequest;

/**
 * Implementation of AuthMethodStrategy for Passkey-based authentication.
 */
@Component
public class PasskeyAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(PasskeyAuthStrategy.class);
    private final PasskeyRegistrationService passkeyService;
    
    public PasskeyAuthStrategy(PasskeyRegistrationService passkeyService) {
        this.passkeyService = passkeyService;
    }
    
    @Override
    public Object setupAuthentication(UserEntity user) {
        logger.info("Setting up passkey authentication for user: {}", user.getUserId());
        String email = user.getProfile().getEmail();
        
        // Create registration request object with user details
        RegistrationRequest registrationRequest = new RegistrationRequest(user.getUserId(), email);
        
        // Begin passkey registration process
        RegistrationChallenge challenge = passkeyService.beginRegistration(registrationRequest);
        return challenge;
    }
    
    @Override
    public String getAuthMethodName() {
        return "passkey";
    }
}
