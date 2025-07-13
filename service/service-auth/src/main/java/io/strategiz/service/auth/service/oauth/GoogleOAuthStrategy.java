package io.strategiz.service.auth.service.oauth;

import io.strategiz.data.user.model.OAuthAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for handling Google OAuth authentication method
 */
@Component
public class GoogleOAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthStrategy.class);
    
    private final UserRepository userRepository;
    private final AuthOAuthConfig oauthConfig;
    
    public GoogleOAuthStrategy(UserRepository userRepository, AuthOAuthConfig oauthConfig) {
        this.userRepository = userRepository;
        this.oauthConfig = oauthConfig;
    }
    
    @Override
    public Object setupAuthentication(User user) {
        logger.info("Setting up Google OAuth authentication for user: {}", user.getUserId());
        
        // Create OAuth authentication method
        OAuthAuthenticationMethod oauthMethod = new OAuthAuthenticationMethod();
        oauthMethod.setProvider("google");
        oauthMethod.setType("OAUTH_GOOGLE");
        oauthMethod.setName("Google Account");
        
        // Set provider ID from user profile data if available
        // Since we can't determine exact User model structure, we'll use a default approach
        String email = user.getProfile().getEmail();
        String providerId = email; // Default to email as provider ID for now
        oauthMethod.setProviderId(providerId);
        logger.info("Setting provider ID based on email for user: {}", user.getUserId());
        
        // Audit fields are handled automatically by BaseEntity
        
        // Add authentication method to user
        user.addAuthenticationMethod(oauthMethod);
        userRepository.updateUser(user);
        
        // Return OAuth config for client
        AuthOAuthConfig.AuthOAuthSettings googleConfig = oauthConfig.getGoogle();
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