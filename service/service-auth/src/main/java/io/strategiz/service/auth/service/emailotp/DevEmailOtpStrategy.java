package io.strategiz.service.auth.service.emailotp;

import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.strategiz.data.user.model.User;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;

/**
 * Development implementation of EmailOTP authentication strategy.
 * This implementation logs OTP codes instead of sending actual emails,
 * making it easier to test in development environments.
 * Active in any profile other than "prod".
 */
@Component
@Profile("!prod") // Active in any profile except prod (including dev)
public class DevEmailOtpStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(DevEmailOtpStrategy.class);
    private static final Random random = new Random();
    
    @Override
    public Object setupAuthentication(User user) {
        // Generate a mock 6-digit code for development testing
        String mockCode = String.format("%06d", random.nextInt(1000000));
        
        String email = user.getProfile().getEmail();
        logger.info("[DEV MODE] Would send signup verification OTP to email: {}", email);
        logger.info("DEV MODE: Mock verification code for testing: {}", mockCode);
        
        // Return mock data with the code visible in logs for easy testing
        return Map.of(
            "success", true,
            "message", "Email verification skipped in development",
            "mockCode", mockCode,
            "requiresVerification", true,
            "verificationType", "emailotp"
        );
    }
    
    @Override
    public String getAuthMethodName() {
        return "emailotp";
    }
}
