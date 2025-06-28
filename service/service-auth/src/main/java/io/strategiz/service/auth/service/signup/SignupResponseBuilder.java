package io.strategiz.service.auth.service.signup;

import org.springframework.stereotype.Component;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.business.tokenauth.SessionAuthBusiness.TokenPair;
import io.strategiz.data.user.model.User;
import io.strategiz.service.auth.model.signup.SignupResponse;

/**
 * Builder class responsible for creating the SignupResponse.
 * This follows the Single Responsibility Principle by separating
 * response creation from other business logic.
 */
@Component
public class SignupResponseBuilder {
    
    private final SessionAuthBusiness sessionAuthBusiness;
    
    public SignupResponseBuilder(SessionAuthBusiness sessionAuthBusiness) {
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Creates a token pair for a user
     * 
     * @param userId The ID of the user
     * @return A TokenPair containing access and refresh tokens
     */
    public TokenPair createTokens(String userId) {
        // Create token pair with signup-service as source and no device ID
        return sessionAuthBusiness.createTokenPair(userId, null, "signup-service");
    }
    
    /**
     * Builds a complete SignupResponse with all necessary data
     * 
     * @param user The created user
     * @param tokenPair The token pair for authentication
     * @param authMethodData Authentication method specific data
     * @return A fully populated SignupResponse
     */
    public SignupResponse buildResponse(User user, TokenPair tokenPair, Object authMethodData) {
        String accessToken = tokenPair.accessToken();
        String refreshToken = tokenPair.refreshToken();
        
        // Default expiration is usually 24 hours (86400 seconds)
        long expiresIn = 86400;
        
        // Create the response with the available constructor
        SignupResponse response = new SignupResponse(
            user.getUserId(),
            user.getProfile().getName(),
            user.getProfile().getEmail(),
            accessToken,
            refreshToken,
            expiresIn
        );
        
        // Set authentication method specific data
        response.setAuthMethodData(authMethodData);
        
        return response;
    }
}
