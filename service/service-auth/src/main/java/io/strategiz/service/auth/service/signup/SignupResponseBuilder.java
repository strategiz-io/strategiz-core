package io.strategiz.service.auth.service.signup;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import org.springframework.stereotype.Component;

/**
 * Builder for creating SignupResponse objects for OAuth signup
 */
@Component
public class SignupResponseBuilder {

    private final SessionAuthBusiness sessionAuthBusiness;

    public SignupResponseBuilder(SessionAuthBusiness sessionAuthBusiness) {
        this.sessionAuthBusiness = sessionAuthBusiness;
    }

    /**
     * Build successful signup response with tokens
     * 
     * @param user The created user entity
     * @param message Success message
     * @param authMethods List of authentication methods
     * @param deviceId Device ID
     * @param ipAddress IP address
     * @return OAuthSignupResponse with user data and tokens
     */
    public OAuthSignupResponse buildSuccessResponse(UserEntity user, String message, 
                                             java.util.List<String> authMethods, 
                                             String deviceId, String ipAddress) {
        // Generate authentication tokens
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
            user.getUserId(),
            authMethods,
            "1", // Authentication Context Class Reference
            deviceId,
            ipAddress
        );

        return new OAuthSignupResponse(
            true,
            message,
            user.getUserId(),
            user.getProfile().getEmail(),
            user.getProfile().getName(),
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            user.getProfile().getPhotoURL()
        );
    }

    /**
     * Build failure signup response
     * 
     * @param message Error message
     * @return OAuthSignupResponse indicating failure
     */
    public OAuthSignupResponse buildFailureResponse(String message) {
        return new OAuthSignupResponse(false, message);
    }
}