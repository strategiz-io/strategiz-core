package io.strategiz.service.auth.service.oauth;

import io.strategiz.data.user.model.OAuthAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.AuthenticationMethodRepository;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.model.signup.SignupRequest;
import io.strategiz.service.auth.model.signup.SignupResponse;
import io.strategiz.service.auth.service.signup.SignupService;
import io.strategiz.business.tokenauth.PasetoTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
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
    
    private final SignupService signupService;
    private final UserRepository userRepository;
    private final AuthenticationMethodRepository authenticationMethodRepository;
    private final PasetoTokenProvider tokenProvider;
    private final RestTemplate restTemplate;
    
    @Value("${auth.google.client-id}")
    private String clientId;
    
    @Value("${auth.google.client-secret}")
    private String clientSecret;
    
    @Value("${auth.google.redirect-uri}")
    private String redirectUri;
    
    @Value("${application.frontend-url}")
    private String frontendUrl;
    
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    
    public GoogleOAuthService(
            SignupService signupService, 
            UserRepository userRepository,
            AuthenticationMethodRepository authenticationMethodRepository, 
            PasetoTokenProvider tokenProvider, 
            RestTemplate restTemplate) {
        this.signupService = signupService;
        this.userRepository = userRepository;
        this.authenticationMethodRepository = authenticationMethodRepository;
        this.tokenProvider = tokenProvider;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get the frontend URL from configuration
     * 
     * @return The frontend URL
     */
    public String getFrontendUrl() {
        return frontendUrl;
    }
    
    /**
     * Get the authorization URL for Google OAuth
     * 
     * @param isSignup Whether this is for signup flow
     * @return Map containing the authorization URL and state
     */
    public Map<String, String> getAuthorizationUrl(boolean isSignup) {
        String state = UUID.randomUUID().toString();
        if (isSignup) {
            state = "signup:" + state;
        }
        
        String scope = "email profile openid";
        
        String authUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .toUriString();
        
        logger.info("Generated Google OAuth URL: {}", authUrl);
        
        Map<String, String> response = new HashMap<>();
        response.put("url", authUrl);
        response.put("state", state);
        
        return response;
    }
    
    /**
     * Handle OAuth callback from Google
     * 
     * @param code Authorization code from Google
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Map containing tokens and user info
     */
    public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
        // Check if this is a signup flow
        boolean isSignup = state != null && state.startsWith("signup:");
        
        // Exchange code for token
        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            logger.error("Failed to exchange code for token");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get access token from Google");
            return errorResponse;
        }
        
        String accessToken = (String) tokenResponse.get("access_token");
        
        // Get user info from Google
        Map<String, Object> userInfo = getUserInfo(accessToken);
        
        if (userInfo == null || !userInfo.containsKey("sub")) {
            logger.error("Failed to get user info from Google");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get user info from Google");
            return errorResponse;
        }
        
        String googleId = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        
        // Check if this OAuth method already exists
        List<OAuthAuthenticationMethod> existingOAuthMethods = authenticationMethodRepository.findByProviderAndUid("google", googleId);
        
        if (!existingOAuthMethods.isEmpty()) {
            // User exists - handle login flow
            OAuthAuthenticationMethod existingMethod = existingOAuthMethods.get(0);
            String userId = existingMethod.getUserId();
            
            // Get the user
            Optional<User> userOptional = userRepository.getUserById(userId);
            
            if (!userOptional.isPresent()) {
                logger.error("User not found for OAuth method: {}", userId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "User account not found");
                return errorResponse;
            }
            
            User user = userOptional.get();
            
            // Generate tokens
            String appAccessToken = tokenProvider.createAccessToken(userId);
            String refreshToken = tokenProvider.createRefreshToken(userId);
            
            // Return success response with tokens
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("isNewUser", false);
            successResponse.put("accessToken", appAccessToken);
            successResponse.put("refreshToken", refreshToken);
            successResponse.put("user", user);
            
            // Link the device ID if provided
            if (deviceId != null && !deviceId.isEmpty()) {
                // TODO: Link device ID to user account
                logger.info("Device ID provided for existing user: {}", deviceId);
            }
            
            return successResponse;
        } else if (isSignup) {
            // New user - handle signup flow
            if (email == null || email.isEmpty()) {
                logger.error("Email not provided by Google");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Email not provided by Google");
                return errorResponse;
            }
            
            // Check if user already exists with this email
            Optional<User> existingUserOptional = userRepository.getUserByEmail(email);
            
            if (existingUserOptional.isPresent()) {
                // User exists but not linked to Google
                User existingUser = existingUserOptional.get();
                
                // Create and save OAuth method
                OAuthAuthenticationMethod oauthMethod = new OAuthAuthenticationMethod();
                oauthMethod.setUserId(existingUser.getUserId());
                oauthMethod.setProvider("google");
                oauthMethod.setProviderId(googleId);
                oauthMethod.setEmail(email);
                oauthMethod.setCreatedBy(existingUser.getUserId());
                oauthMethod.setCreatedAt(new Date());
                oauthMethod.setModifiedBy(existingUser.getUserId());
                oauthMethod.setModifiedAt(new Date());
                oauthMethod.setType("OAUTH_GOOGLE");
                oauthMethod.setName("Google Account");
                
                authenticationMethodRepository.save(oauthMethod);
                
                // Generate tokens
                String appAccessToken = tokenProvider.createAccessToken(existingUser.getUserId());
                String refreshToken = tokenProvider.createRefreshToken(existingUser.getUserId());
                
                // Return success response
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("isNewUser", false);
                successResponse.put("isAccountLinked", true);
                successResponse.put("accessToken", appAccessToken);
                successResponse.put("refreshToken", refreshToken);
                successResponse.put("user", existingUser);
                
                // Link device ID if provided
                if (deviceId != null && !deviceId.isEmpty()) {
                    // TODO: Link device ID to user account
                    logger.info("Device ID provided for linked account: {}", deviceId);
                }
                
                return successResponse;
            }
            
            // Create new user
            SignupRequest signupRequest = new SignupRequest();
            signupRequest.setEmail(email);
            // Handle name fields based on their availability in userInfo
            String fullName = (String) userInfo.getOrDefault("name", "");
            String givenName = (String) userInfo.getOrDefault("given_name", "");
            String familyName = (String) userInfo.getOrDefault("family_name", "");
            
            // If available, set full name or construct from parts
            if (fullName != null && !fullName.isEmpty()) {
                signupRequest.setName(fullName);
            } else if (givenName != null || familyName != null) {
                String constructedName = ((givenName != null ? givenName : "") + " " + 
                                      (familyName != null ? familyName : "")).trim();
                signupRequest.setName(constructedName);
            }
            
            // Add OAuth data
            Map<String, String> authData = new HashMap<>();
            authData.put("provider", "google");
            authData.put("providerId", googleId);
            authData.put("email", email);
            
            // Add picture if available
            if (userInfo.containsKey("picture")) {
                authData.put("picture", (String) userInfo.get("picture"));
            }
            
            signupRequest.setAuthData(authData);
            
            // Add device ID if provided
            if (deviceId != null && !deviceId.isEmpty()) {
                // Add device fingerprint to auth data map instead
                authData.put("deviceId", deviceId);
                logger.info("Device ID added to signup request: {}", deviceId);
            }
            
            // Process signup
            SignupResponse signupResponse = signupService.processSignup(signupRequest);
            
            if (signupResponse == null || signupResponse.getUserId() == null) {
                logger.error("Signup failed");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Failed to create account");
                return errorResponse;
            }
            
            // Get the newly created user
            String newUserId = signupResponse.getUserId();
            Optional<User> newUserOpt = userRepository.getUserById(newUserId);
            
            if (!newUserOpt.isPresent()) {
                logger.error("Failed to retrieve newly created user: {}", newUserId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Failed to retrieve user account");
                return errorResponse;
            }
            
            User newUser = newUserOpt.get();
            
            // Generate tokens
            String appAccessToken = tokenProvider.createAccessToken(newUser.getUserId());
            String refreshToken = tokenProvider.createRefreshToken(newUser.getUserId());
            
            // Return success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("isNewUser", true);
            successResponse.put("accessToken", appAccessToken);
            successResponse.put("refreshToken", refreshToken);
            successResponse.put("user", newUser);
            
            return successResponse;
        } else {
            // User doesn't exist but trying to login
            logger.error("User doesn't exist with Google ID: {}", googleId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "No account found linked to this Google account");
            return errorResponse;
        }
    }
    
    /**
     * Exchange authorization code for access token
     * 
     * @param code Authorization code
     * @return Token response from Google
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("client_id", clientId);
            requestBody.put("client_secret", clientSecret);
            requestBody.put("code", code);
            requestBody.put("redirect_uri", redirectUri);
            requestBody.put("grant_type", "authorization_code");
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GOOGLE_TOKEN_URL,
                    HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(requestBody, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error exchanging code for token", e);
            return null;
        }
    }
    
    /**
     * Get user info from Google
     * 
     * @param accessToken Access token
     * @return User info from Google
     */
    private Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error getting user info", e);
            return null;
        }
    }
}
