package io.strategiz.service.auth.service.oauth;

import io.strategiz.data.user.model.OAuthAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for handling Google OAuth authentication method
 */
@Component
public class GoogleOAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthStrategy.class);
    
    private final UserRepository userRepository;
    
    @Value("${auth.google.client-id}")
    private String googleClientId;
    
    @Value("${auth.google.client-secret}")
    private String googleClientSecret;
    
    @Value("${auth.google.redirect-uri}")
    private String redirectUri;
    
    public GoogleOAuthStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public Object setupAuthentication(User user) {
        logger.info("Setting up Google OAuth authentication for user: {}", user.getUserId());
        
        // Create OAuth authentication method
        OAuthAuthenticationMethod oauthMethod = new OAuthAuthenticationMethod();
        oauthMethod.setProvider("google");
        oauthMethod.setType("OAUTH_GOOGLE");
        oauthMethod.setName("Google Account");
        oauthMethod.setIsActive(true);
        
        // Set audit fields
        Date now = new Date();
        oauthMethod.setCreatedBy("google_oauth_strategy");
        oauthMethod.setCreatedAt(now);
        oauthMethod.setModifiedBy("google_oauth_strategy");
        oauthMethod.setModifiedAt(now);
        
        // Add authentication method to user
        user.addAuthenticationMethod(oauthMethod);
        userRepository.updateUser(user);
        
        // Return OAuth config for client
        Map<String, String> authConfig = new HashMap<>();
        authConfig.put("clientId", googleClientId);
        authConfig.put("redirectUri", redirectUri);
        authConfig.put("provider", "google");
        
        return authConfig;
    }
    
    @Override
    public String getAuthMethodName() {
        return "google";
    }
}
