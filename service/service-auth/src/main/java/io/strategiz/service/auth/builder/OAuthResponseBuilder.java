package io.strategiz.service.auth.builder;

import io.strategiz.business.tokenauth.PasetoTokenProvider;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.model.User;
import io.strategiz.service.auth.model.signup.SignupResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for building OAuth response objects
 */
@Component
public class OAuthResponseBuilder {

    private final PasetoTokenProvider tokenProvider;
    private final SessionAuthBusiness sessionAuthBusiness;

    public OAuthResponseBuilder(PasetoTokenProvider tokenProvider, SessionAuthBusiness sessionAuthBusiness) {
        this.tokenProvider = tokenProvider;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Build success response for new user signup
     */
    public Map<String, Object> buildSignupSuccessResponse(SignupResponse signupResponse, String email, String name) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userMap = buildUserMap(signupResponse.getUserId(), email, name);
        
        result.put("success", true);
        result.put("isNewUser", true);
        result.put("user", userMap);
        result.put("accessToken", signupResponse.getAccessToken());
        result.put("refreshToken", signupResponse.getRefreshToken());
        
        return result;
    }

    /**
     * Build success response for existing user login
     */
    public Map<String, Object> buildLoginSuccessResponse(User user) {
        // Create authentication tokens for OAuth login
        // Using ACR "2.2" (substantial assurance) for OAuth providers
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
            user.getUserId(),
            List.of("password"), // OAuth providers typically provide password-equivalent authentication
            "2.2", // ACR "2.2" - Substantial assurance for OAuth
            null, // No device ID for OAuth responses
            "oauth-login" // IP address placeholder
        );
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> userMap = buildUserMap(
            user.getUserId(), 
            user.getProfile().getEmail(), 
            user.getProfile().getName()
        );
        
        result.put("success", true);
        result.put("isNewUser", false);
        result.put("user", userMap);
        result.put("accessToken", tokenPair.accessToken());
        result.put("refreshToken", tokenPair.refreshToken());
        
        return result;
    }

    /**
     * Build error response
     */
    public Map<String, Object> buildErrorResponse(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        return result;
    }

    private Map<String, Object> buildUserMap(String userId, String email, String name) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("email", email);
        userMap.put("name", name);
        return userMap;
    }
} 