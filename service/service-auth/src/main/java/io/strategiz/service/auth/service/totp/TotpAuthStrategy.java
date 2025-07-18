package io.strategiz.service.auth.service.totp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;

/**
 * Implementation of AuthMethodStrategy for TOTP-based authentication.
 */
@Component
public class TotpAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(TotpAuthStrategy.class);
    private final TotpAuthenticationService totpAuthenticationService;
    private final TotpRegistrationService totpRegistrationService;
    
    public TotpAuthStrategy(TotpAuthenticationService totpAuthenticationService, TotpRegistrationService totpRegistrationService) {
        this.totpAuthenticationService = totpAuthenticationService;
        this.totpRegistrationService = totpRegistrationService;
    }
    
    @Override
    public Object setupAuthentication(UserEntity user) {
        logger.info("Setting up TOTP authentication for user: {}", user.getUserId());
        
        // Generate TOTP secret for the user
        Object totpSetupData = totpRegistrationService.generateTotpSecret(user.getProfile().getEmail());
        return totpSetupData;
    }
    
    @Override
    public String getAuthMethodName() {
        return "totp";
    }
}
