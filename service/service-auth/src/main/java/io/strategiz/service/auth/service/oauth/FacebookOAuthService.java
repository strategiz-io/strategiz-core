package io.strategiz.service.auth.service.oauth;

import io.strategiz.client.facebook.FacebookClient;
import io.strategiz.client.facebook.model.FacebookTokenResponse;
import io.strategiz.client.facebook.model.FacebookUserInfo;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import io.strategiz.service.auth.manager.OAuthAuthenticationManager;
import io.strategiz.service.auth.manager.OAuthUserManager;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.business.tokenauth.SessionAuthBusiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        AuthOAuthSettings facebookConfig = oauthConfig.getFacebook();
        if (facebookConfig == null) {
            logger.error("Facebook OAuth configuration is not available. Check application.properties for oauth.providers.facebook settings.");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Facebook OAuth is not configured");
        }

        // Use signup redirect URI for signup flow, otherwise use signin redirect URI
        String redirectUri = isSignup && facebookConfig.getSignupRedirectUri() != null
                ? facebookConfig.getSignupRedirectUri()
                : facebookConfig.getRedirectUri();

        String authUrl = UriComponentsBuilder.fromUriString(facebookConfig.getAuthUrl())
                .queryParam("client_id", facebookConfig.getClientId())
                .queryParam("redirect_uri", redirectUri)
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
     * Get the frontend URL for redirects
     */
    public String getFrontendUrl() {
        return oauthConfig.getFrontendUrl();
    }
    
    /**
     * Handle OAuth callback from Facebook
     * 
     * @param code Authorization code from Facebook
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Clean response object based on the operation result
     */
    public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
        if (!isValidAuthorizationCode(code)) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Missing authorization code");
        }
        
        FacebookTokenResponse tokenResponse = exchangeCodeForToken(code, state);
        if (tokenResponse == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to exchange authorization code for token");
        }
        
        FacebookUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to get user info");
        }
        
        Object result = processUserAuthentication(userInfo, state);
        
        // Convert result to Map<String, Object> for consistent JSON response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        if (result instanceof OAuthSignupResponse) {
            OAuthSignupResponse signupResponse = (OAuthSignupResponse) result;
            response.put("type", "signup");
            response.put("userId", signupResponse.getUserId());
            response.put("email", signupResponse.getEmail());
            response.put("name", signupResponse.getName());
            response.put("accessToken", signupResponse.getAccessToken());
            response.put("refreshToken", signupResponse.getRefreshToken());
        } else if (result instanceof ApiTokenResponse) {
            ApiTokenResponse loginTokenResponse = (ApiTokenResponse) result;
            response.put("type", "login");
            response.put("accessToken", loginTokenResponse.accessToken());
            response.put("refreshToken", loginTokenResponse.refreshToken());
            response.put("tokenType", loginTokenResponse.tokenType());
        }
        
        return response;
    }

    private boolean isValidAuthorizationCode(String code) {
        if (code == null) {
            logger.error("Missing authorization code");
            return false;
        }
        return true;
    }

    private FacebookTokenResponse exchangeCodeForToken(String code, String state) {
        AuthOAuthSettings facebookConfig = oauthConfig.getFacebook();
        if (facebookConfig == null) {
            logger.error("Facebook OAuth configuration is not available during token exchange");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Facebook OAuth is not configured");
        }

        // Use signup redirect URI if this is a signup flow, otherwise use signin redirect URI
        boolean isSignup = oauthUserManager.isSignupFlow(state);
        String redirectUri = isSignup && facebookConfig.getSignupRedirectUri() != null
                ? facebookConfig.getSignupRedirectUri()
                : facebookConfig.getRedirectUri();

        return facebookClient.exchangeCodeForToken(
            code,
            facebookConfig.getClientId(),
            facebookConfig.getClientSecret(),
            redirectUri
        ).orElse(null);
    }

    private FacebookUserInfo getUserInfo(String accessToken) {
        return facebookClient.getUserInfo(accessToken).orElse(null);
    }

    private Object processUserAuthentication(FacebookUserInfo userInfo, String state) {
        boolean isSignup = oauthUserManager.isSignupFlow(state);
        Optional<UserEntity> existingUser = oauthUserManager.findUserByEmail(userInfo.getEmail());
        
        if (isSignup && existingUser.isPresent()) {
            throw new StrategizException(AuthErrors.INVALID_CREDENTIALS, "email_already_exists");
        }
        
        if (isSignup || !existingUser.isPresent()) {
            return handleSignupFlow(userInfo);
        } else {
            return handleLoginFlow(existingUser.get(), userInfo);
        }
    }

    private OAuthSignupResponse handleSignupFlow(FacebookUserInfo userInfo) {
        return oauthUserManager.createOAuthUser(
            userInfo.getEmail(), 
            userInfo.getName(), 
            userInfo.getPictureUrl(), 
            "facebook", 
            userInfo.getFacebookId(),
            null, // deviceId not available
            null  // ipAddress not available
        );
    }

    private ApiTokenResponse handleLoginFlow(UserEntity user, FacebookUserInfo userInfo) {
        oauthAuthenticationManager.ensureOAuthMethod(
            user, 
            "facebook", 
            userInfo.getFacebookId(), 
            userInfo.getEmail()
        );
        
        // Generate authentication tokens using unified approach
        SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
            user.getUserId(),
            userInfo.getEmail(),
            List.of("facebook"), // Authentication method used
            false, // Not partial auth - OAuth provides full authentication
            null, // Device ID not available in OAuth flow
            null, // Device fingerprint not available
            null, // IP address not available in OAuth flow
            "Facebook OAuth",
            false // demoMode
        );
        
        SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);
        
        return new ApiTokenResponse(
            authResult.accessToken(),
            authResult.refreshToken(),
            user.getUserId()
        );
    }
}
