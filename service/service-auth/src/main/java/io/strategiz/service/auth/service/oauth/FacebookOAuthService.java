package io.strategiz.service.auth.service.oauth;

import io.strategiz.client.facebook.FacebookClient;
import io.strategiz.client.facebook.model.FacebookTokenResponse;
import io.strategiz.client.facebook.model.FacebookUserInfo;
import io.strategiz.data.user.model.User;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.manager.OAuthAuthenticationManager;
import io.strategiz.service.auth.manager.OAuthUserManager;
import io.strategiz.service.auth.model.signup.SignupResponse;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling Facebook OAuth flows
 */
@Service
public class FacebookOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthService.class);

    private final FacebookClient facebookClient;
    private final OAuthUserManager oauthUserManager;
    private final OAuthAuthenticationManager oauthAuthenticationManager;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final AuthOAuthConfig oauthConfig;

    @Value("${application.frontend-url}")
    private String frontendUrl;

    public FacebookOAuthService(
            FacebookClient facebookClient,
            OAuthUserManager oauthUserManager,
            OAuthAuthenticationManager oauthAuthenticationManager,
            SessionAuthBusiness sessionAuthBusiness,
            AuthOAuthConfig oauthConfig) {
        this.facebookClient = facebookClient;
        this.oauthUserManager = oauthUserManager;
        this.oauthAuthenticationManager = oauthAuthenticationManager;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.oauthConfig = oauthConfig;
    }

    /**
     * Get the authorization URL for Facebook OAuth
     *
     * @param isSignup Whether this is for signup flow
     * @return The authorization URL and state
     */
    public Map<String, String> getAuthorizationUrl(boolean isSignup) {
        String state = UUID.randomUUID().toString();
        if (isSignup) {
            state = "signup:" + state;
        }
        
        AuthOAuthConfig.AuthOAuthSettings facebookConfig = oauthConfig.getFacebook();
        if (facebookConfig == null) {
            logger.error("Facebook OAuth configuration is not available. Check application.properties for oauth.providers.facebook settings.");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Facebook OAuth is not configured");
        }
        
        String authUrl = UriComponentsBuilder.fromUriString(facebookConfig.getAuthUrl())
                .queryParam("client_id", facebookConfig.getClientId())
                .queryParam("redirect_uri", facebookConfig.getRedirectUri())
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .queryParam("scope", "email,public_profile")
                .toUriString();
        
        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        response.put("state", state);
        
        return response;
    }
    
    /**
     * Get frontend URL
     * @return The frontend URL
     */
    public String getFrontendUrl() {
        return frontendUrl;
    }
    
    /**
     * Handle OAuth callback from Facebook
     * 
     * @param code Authorization code from Facebook
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Clean response object based on the operation result
     */
    public Object handleOAuthCallback(String code, String state, String deviceId) {
        if (!isValidAuthorizationCode(code)) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Missing authorization code");
        }
        
        FacebookTokenResponse tokenResponse = exchangeCodeForToken(code);
        if (tokenResponse == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to exchange authorization code for token");
        }
        
        FacebookUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to get user info");
        }
        
        return processUserAuthentication(userInfo, state);
    }

    private boolean isValidAuthorizationCode(String code) {
        if (code == null) {
            logger.error("Missing authorization code");
            return false;
        }
        return true;
    }

    private FacebookTokenResponse exchangeCodeForToken(String code) {
        AuthOAuthConfig.AuthOAuthSettings facebookConfig = oauthConfig.getFacebook();
        if (facebookConfig == null) {
            logger.error("Facebook OAuth configuration is not available during token exchange");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Facebook OAuth is not configured");
        }
        
        return facebookClient.exchangeCodeForToken(
            code, 
            facebookConfig.getClientId(), 
            facebookConfig.getClientSecret(), 
            facebookConfig.getRedirectUri()
        ).orElse(null);
    }

    private FacebookUserInfo getUserInfo(String accessToken) {
        return facebookClient.getUserInfo(accessToken).orElse(null);
    }

    private Object processUserAuthentication(FacebookUserInfo userInfo, String state) {
        boolean isSignup = oauthUserManager.isSignupFlow(state);
        Optional<User> existingUser = oauthUserManager.findUserByEmail(userInfo.getEmail());
        
        if (isSignup && existingUser.isPresent()) {
            throw new StrategizException(AuthErrors.INVALID_CREDENTIALS, "email_already_exists");
        }
        
        if (isSignup || !existingUser.isPresent()) {
            return handleSignupFlow(userInfo);
        } else {
            return handleLoginFlow(existingUser.get(), userInfo);
        }
    }

    private SignupResponse handleSignupFlow(FacebookUserInfo userInfo) {
        return oauthUserManager.createOAuthUser(
            userInfo.getEmail(), 
            userInfo.getName(), 
            userInfo.getPictureUrl(), 
            "facebook", 
            userInfo.getFacebookId()
        );
    }

    private ApiTokenResponse handleLoginFlow(User user, FacebookUserInfo userInfo) {
        oauthAuthenticationManager.ensureOAuthMethod(
            user, 
            "facebook", 
            userInfo.getFacebookId(), 
            userInfo.getEmail()
        );
        
        // Generate authentication tokens
        SessionAuthBusiness.TokenPair tokenPair = sessionAuthBusiness.createAuthenticationTokenPair(
            user.getUserId(),
            List.of("facebook"), // Authentication method used
            "2.1", // ACR "2.1" - Basic assurance for OAuth
            null, // Device ID not available
            null  // IP address not available
        );
        
        return new ApiTokenResponse(
            tokenPair.accessToken(),
            tokenPair.refreshToken(),
            user.getUserId()
        );
    }
}
