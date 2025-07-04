package io.strategiz.service.auth.service.signup;

import org.springframework.stereotype.Component;

import java.util.List;

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
     * Creates a token pair for a user with specified authentication method
     * 
     * @param userId The ID of the user
     * @param authMethod The authentication method used (determines ACR level)
     * @return A TokenPair containing access and refresh tokens
     */
    public TokenPair createTokens(String userId, String authMethod) {
        // Determine ACR level based on authentication method
        String acr;
        List<String> authMethods;
        
        switch (authMethod.toLowerCase()) {
            case "passkeys":
                acr = "2.3"; // High assurance
                authMethods = List.of("passkeys");
                break;
            case "totp":
                acr = "2.2"; // Substantial assurance (multi-factor)
                authMethods = List.of("totp");
                break;
            case "sms_otp":
                acr = "2.2"; // Substantial assurance (multi-factor)
                authMethods = List.of("sms_otp");
                break;
            case "email_otp":
                acr = "2.2"; // Substantial assurance (multi-factor)
                authMethods = List.of("email_otp");
                break;
            case "password":
            default:
                acr = "2.1"; // Basic assurance (password only)
                authMethods = List.of("password");
                break;
        }
        
        // Create authentication token pair with proper claims
        return sessionAuthBusiness.createAuthenticationTokenPair(
            userId,
            authMethods,
            acr,
            null, // No device ID for signup
            "signup-service"
        );
    }
    
    /**
     * Creates a token pair for a user (backwards compatibility)
     * 
     * @param userId The ID of the user
     * @return A TokenPair containing access and refresh tokens
     */
    @Deprecated
    public TokenPair createTokens(String userId) {
        // Default to password authentication for backwards compatibility
        return createTokens(userId, "password");
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
