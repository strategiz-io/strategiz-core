package io.strategiz.service.auth.service.oauth;

import io.strategiz.client.google.client.GoogleClient;
import io.strategiz.client.google.model.GoogleTokenResponse;
import io.strategiz.client.google.model.GoogleUserInfo;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.service.auth.config.AuthOAuthConfig;
import io.strategiz.service.auth.model.config.AuthOAuthSettings;
import io.strategiz.service.auth.manager.OAuthAuthenticationManager;
import io.strategiz.service.auth.manager.OAuthUserManager;
import io.strategiz.service.auth.model.ApiTokenResponse;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
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
 * Service for handling Google OAuth flows
 */
@Service
public class GoogleOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);

    private final GoogleClient googleClient;
    private final OAuthUserManager oauthUserManager;
    private final OAuthAuthenticationManager oauthAuthenticationManager;
    private final SessionAuthBusiness sessionAuthBusiness;
    private final AuthOAuthConfig oauthConfig;

    public GoogleOAuthService(
            GoogleClient googleClient,
            OAuthUserManager oauthUserManager,
            OAuthAuthenticationManager oauthAuthenticationManager,
            SessionAuthBusiness sessionAuthBusiness,
            AuthOAuthConfig oauthConfig) {
        this.googleClient = googleClient;
        this.oauthUserManager = oauthUserManager;
        this.oauthAuthenticationManager = oauthAuthenticationManager;
        this.sessionAuthBusiness = sessionAuthBusiness;
        this.oauthConfig = oauthConfig;
    }

    /**
     * Get the authorization URL for Google OAuth
     *
     * @param isSignup Whether this is for signup flow
     * @return The authorization URL and state
     */
    public Map<String, String> getAuthorizationUrl(boolean isSignup) {
        String state = UUID.randomUUID().toString();
        if (isSignup) {
            state = "signup:" + state;
        }

        AuthOAuthSettings googleConfig = oauthConfig.getGoogle();
        if (googleConfig == null) {
            logger.error("Google OAuth configuration is not available. Check application.properties for oauth.providers.google settings.");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Google OAuth is not configured");
        }

        // Use signup redirect URI for signup flow, otherwise use signin redirect URI
        String redirectUri = isSignup && googleConfig.getSignupRedirectUri() != null
                ? googleConfig.getSignupRedirectUri()
                : googleConfig.getRedirectUri();

        String authUrl = UriComponentsBuilder.fromUriString(googleConfig.getAuthUrl())
                .queryParam("client_id", googleConfig.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
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
     * Handle OAuth callback from Google
     *
     * @param code Authorization code from Google
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Clean response object based on the operation result
     */
    public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
        // Determine signup from state for backwards compatibility
        boolean isSignup = oauthUserManager.isSignupFlow(state);
        return handleOAuthCallback(code, state, deviceId, isSignup);
    }

    /**
     * Handle OAuth callback from Google with explicit signup flag
     *
     * @param code Authorization code from Google
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @param isSignup Whether this is a signup flow (determined by calling controller)
     * @return Clean response object based on the operation result
     */
    public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId, boolean isSignup) {
        if (!isValidAuthorizationCode(code)) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Missing authorization code");
        }

        GoogleTokenResponse tokenResponse = exchangeCodeForToken(code, state);
        if (tokenResponse == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to exchange authorization code for token");
        }

        GoogleUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());
        if (userInfo == null) {
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Failed to get user info");
        }

        Object result = processUserAuthentication(userInfo, isSignup);
        
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
        } else if (result instanceof LoginFlowResponse) {
            LoginFlowResponse loginResponse = (LoginFlowResponse) result;
            response.put("type", "login");
            response.put("userId", loginResponse.userId());
            response.put("email", loginResponse.email());
            response.put("name", loginResponse.name());
            response.put("accessToken", loginResponse.tokens().accessToken());
            response.put("refreshToken", loginResponse.tokens().refreshToken());
            response.put("tokenType", loginResponse.tokens().tokenType());
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

    private GoogleTokenResponse exchangeCodeForToken(String code, String state) {
        AuthOAuthSettings googleConfig = oauthConfig.getGoogle();
        if (googleConfig == null) {
            logger.error("Google OAuth configuration is not available during token exchange");
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "Google OAuth is not configured");
        }

        // Use signup redirect URI if this is a signup flow, otherwise use signin redirect URI
        boolean isSignup = oauthUserManager.isSignupFlow(state);
        String redirectUri = isSignup && googleConfig.getSignupRedirectUri() != null
                ? googleConfig.getSignupRedirectUri()
                : googleConfig.getRedirectUri();

        return googleClient.exchangeCodeForToken(
            code,
            googleConfig.getClientId(),
            googleConfig.getClientSecret(),
            redirectUri
        ).orElse(null);
    }

    private GoogleUserInfo getUserInfo(String accessToken) {
        return googleClient.getUserInfo(accessToken).orElse(null);
    }

    private Object processUserAuthentication(GoogleUserInfo userInfo, boolean isSignup) {
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

    /**
     * Response wrapper for login flow that includes user data
     */
    public record LoginFlowResponse(
        ApiTokenResponse tokens,
        String userId,
        String email,
        String name
    ) {}

    private OAuthSignupResponse handleSignupFlow(GoogleUserInfo userInfo) {
        return oauthUserManager.createOAuthUser(
            userInfo.getEmail(), 
            userInfo.getName(), 
            userInfo.getPictureUrl(), 
            "google", 
            userInfo.getGoogleId(),
            null, // deviceId not available
            null  // ipAddress not available
        );
    }

    private LoginFlowResponse handleLoginFlow(UserEntity user, GoogleUserInfo userInfo) {
        oauthAuthenticationManager.ensureOAuthMethod(
            user,
            "google",
            userInfo.getGoogleId(),
            userInfo.getEmail()
        );

        // Generate authentication tokens using unified approach
        SessionAuthBusiness.AuthRequest authRequest = new SessionAuthBusiness.AuthRequest(
            user.getUserId(),
            userInfo.getEmail(),
            List.of("google"), // Authentication method used
            false, // Not partial auth - OAuth provides full authentication
            null, // Device ID not available in OAuth flow
            null, // Device fingerprint not available
            null, // IP address not available in OAuth flow
            "Google OAuth",
            false // demoMode
        );

        SessionAuthBusiness.AuthResult authResult = sessionAuthBusiness.createAuthentication(authRequest);

        ApiTokenResponse tokens = new ApiTokenResponse(
            authResult.accessToken(),
            authResult.refreshToken(),
            "bearer"
        );

        // Get name from user profile, fallback to Google userInfo
        String userName = userInfo.getName();
        if (user.getProfile() != null && user.getProfile().getName() != null) {
            userName = user.getProfile().getName();
        }

        return new LoginFlowResponse(
            tokens,
            user.getUserId(),
            userInfo.getEmail(),
            userName
        );
    }
} 