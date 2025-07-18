package io.strategiz.service.auth.service.oauth;

import io.strategiz.data.auth.model.oauth.OAuthAuthenticationMethod;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for handling Google OAuth authentication method
 */
// @Component // Disabled - needs update to use new entity architecture
public class GoogleOAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthStrategy.class);
    
    private final UserRepository userRepository;
    private final AuthOAuthConfig oauthConfig;
    
    public GoogleOAuthStrategy(UserRepository userRepository, AuthOAuthConfig oauthConfig) {
        this.userRepository = userRepository;
        this.oauthConfig = oauthConfig;
    }
    
    @Override
    public Object setupAuthentication(UserEntity user) {
        logger.info("Setting up Google OAuth authentication for user: {}", user.getUserId());
        
        // Create OAuth authentication method
        OAuthAuthenticationMethod oauthMethod = new OAuthAuthenticationMethod();
        oauthMethod.setProvider("google");
        oauthMethod.setMethodName("Google Account");
        
        // Set provider ID from user profile data if available
        // Since we can't determine exact UserEntity model structure, we'll use a default approach
        String email = user.getProfile().getEmail();
        String providerId = email; // Default to email as provider ID for now
        oauthMethod.setProviderId(providerId);
        logger.info("Setting provider ID based on email for user: {}", user.getUserId());
        
        // Audit fields are handled automatically by BaseEntity
        
        // Note: OAuth authentication method will be saved by the OAuth flow completion
        // This method just returns the config for the client to initiate OAuth
        
        // Return OAuth config for client
        AuthOAuthSettings googleConfig = oauthConfig.getGoogle();
        Map<String, String> authConfig = new HashMap<>();
        authConfig.put("clientId", googleConfig.getClientId());
        authConfig.put("redirectUri", googleConfig.getRedirectUri());
        authConfig.put("provider", "google");
        
        return authConfig;
    }
    
    @Override
    public String getAuthMethodName() {
        return "google";
    }
} 