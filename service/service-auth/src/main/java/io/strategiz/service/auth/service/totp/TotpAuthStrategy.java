package io.strategiz.service.auth.service.totp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.strategiz.data.user.model.User;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;

/**
 * Implementation of AuthMethodStrategy for TOTP-based authentication.
 */
@Component
public class TotpAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(TotpAuthStrategy.class);
    private final TotpService totpService;
    
    public TotpAuthStrategy(TotpService totpService) {
        this.totpService = totpService;
    }
    
    @Override
    public Object setupAuthentication(User user) {
        logger.info("Setting up TOTP authentication for user: {}", user.getUserId());
        
        // Generate TOTP secret for the user
        Object totpSetupData = totpService.generateTotpSecret(user.getProfile().getEmail());
        return totpSetupData;
    }
    
    @Override
    public String getAuthMethodName() {
        return "totp";
    }
}
