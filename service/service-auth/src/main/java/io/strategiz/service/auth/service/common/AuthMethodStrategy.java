package io.strategiz.service.auth.service.common;

import io.strategiz.data.user.entity.UserEntity;

/**
 * Strategy interface for handling different authentication methods.
 * This follows the Strategy Pattern and Open/Closed Principle, allowing
 * new authentication methods to be added without modifying existing code.
 */
public interface AuthMethodStrategy {
    
    /**
     * Sets up the authentication method for a user
     * 
     * @param user The user for whom to set up authentication
     * @return Auth method specific data to include in the response
     */
    Object setupAuthentication(UserEntity user);
    
    /**
     * Gets the name of this authentication method
     * 
     * @return The name of this authentication method (e.g., "passkey", "totp")
     */
    String getAuthMethodName();
}
