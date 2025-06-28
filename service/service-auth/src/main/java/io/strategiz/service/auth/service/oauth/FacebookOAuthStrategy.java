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
 * Strategy for handling Facebook OAuth authentication method
 */
@Component
public class FacebookOAuthStrategy implements AuthMethodStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthStrategy.class);
    
    private final UserRepository userRepository;
    
    @Value("${auth.facebook.client-id}")
    private String facebookClientId;
    
    @Value("${auth.facebook.client-secret}")
    private String facebookClientSecret;
    
    @Value("${auth.facebook.redirect-uri}")
    private String redirectUri;
    
    public FacebookOAuthStrategy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public Object setupAuthentication(User user) {
        logger.info("Setting up Facebook OAuth authentication for user: {}", user.getUserId());
        
        // Create OAuth authentication method
        OAuthAuthenticationMethod oauthMethod = new OAuthAuthenticationMethod();
        oauthMethod.setProvider("facebook");
        oauthMethod.setType("OAUTH_FACEBOOK");
        oauthMethod.setName("Facebook Account");
        oauthMethod.setIsActive(true);
        
        // Extract Facebook provider ID from auth data if present
        Map<String, String> authData = user.getAuthData();
        if (authData != null && authData.containsKey("providerId")) {
            oauthMethod.setProviderId(authData.get("providerId"));
        }
        
        // Set audit fields
        Date now = new Date();
        oauthMethod.setCreatedBy("facebook_oauth_strategy");
        oauthMethod.setCreatedAt(now);
        oauthMethod.setModifiedBy("facebook_oauth_strategy");
        oauthMethod.setModifiedAt(now);
        
        // Add authentication method to user
        user.addAuthenticationMethod(oauthMethod);
        userRepository.updateUser(user);
        
        // Return OAuth config for client
        Map<String, String> authConfig = new HashMap<>();
        authConfig.put("clientId", facebookClientId);
        authConfig.put("redirectUri", redirectUri);
        authConfig.put("provider", "facebook");
        
        return authConfig;
    }
    
    @Override
    public String getAuthMethodName() {
        return "facebook";
    }
}
