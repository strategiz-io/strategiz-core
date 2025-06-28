package io.strategiz.data.user.repository;

import java.util.List;
import java.util.Optional;

import io.strategiz.data.user.model.OAuthAuthenticationMethod;

/**
 * Repository interface for AuthenticationMethod entities
 */
public interface AuthenticationMethodRepository {
    
    /**
     * Find OAuth authentication methods by provider and user ID
     * 
     * @param provider OAuth provider (e.g., "google", "facebook")
     * @param uid Provider-specific user ID
     * @return List of matching OAuth authentication methods
     */
    List<OAuthAuthenticationMethod> findByProviderAndUid(String provider, String uid);
    
    /**
     * Save an authentication method
     * 
     * @param oAuthMethod The authentication method to save
     * @return The saved authentication method
     */
    OAuthAuthenticationMethod save(OAuthAuthenticationMethod oAuthMethod);
    
    /**
     * Find OAuth authentication methods by user ID, provider, and provider ID
     * 
     * @param userId The user ID in the system
     * @param provider OAuth provider (e.g., "google", "facebook")
     * @param providerId Provider-specific user ID
     * @return List of matching OAuth authentication methods
     */
    List<OAuthAuthenticationMethod> findByUserIdAndProviderAndProviderId(String userId, String provider, String providerId);
}
