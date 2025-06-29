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
import org.springframework.http.HttpEntity;
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
 * Service for handling Facebook OAuth flows
 */
@Service
public class FacebookOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthService.class);

    private final SignupService signupService;
    private final UserRepository userRepository;
    private final AuthenticationMethodRepository authenticationMethodRepository;
    private final PasetoTokenProvider tokenProvider;
    private final RestTemplate restTemplate;

    @Value("${auth.facebook.client-id}")
    private String clientId;

    @Value("${auth.facebook.client-secret}")
    private String clientSecret;

    @Value("${auth.facebook.redirect-uri}")
    private String redirectUri;

    @Value("${application.frontend-url}")
    private String frontendUrl;
    
    @Value("${auth.facebook.auth-url:https://www.facebook.com/v12.0/dialog/oauth}")
    private String facebookAuthUrl;
    
    @Value("${auth.facebook.token-url:https://graph.facebook.com/v12.0/oauth/access_token}")
    private String facebookTokenUrl;
    
    @Value("${auth.facebook.user-info-url:https://graph.facebook.com/me}")
    private String facebookUserInfoUrl;

    public FacebookOAuthService(
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
        
        String authUrl = UriComponentsBuilder.fromUriString(facebookAuthUrl)
                .queryParam("client_id", clientId)
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
     * @return Map containing user, tokens, and success status
     */
    public Map<String, Object> handleOAuthCallback(String code, String state, String deviceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (code == null) {
                logger.error("Missing authorization code");
                result.put("success", false);
                result.put("error", "Missing authorization code");
                return result;
            }
            
            // Exchange code for access token
            String tokenUrl = UriComponentsBuilder.fromUriString(facebookTokenUrl)
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("code", code)
                    .queryParam("redirect_uri", redirectUri)
                    .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> tokenResponse = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>(){}
            );
            
            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                logger.error("Failed to get access token from Facebook");
                result.put("success", false);
                result.put("error", "Failed to exchange authorization code for token");
                return result;
            }
            
            String accessToken = (String) tokenResponse.getBody().get("access_token");
            
            // Get user info
            String userInfoUrl = UriComponentsBuilder.fromUriString(facebookUserInfoUrl)
                    .queryParam("fields", "id,name,email")
                    .queryParam("access_token", accessToken)
                    .toUriString();
            
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> requestEntity = new HttpEntity<>(userInfoHeaders);
            
            ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> userData = userResponse.getBody();
            if (userData == null || !userData.containsKey("id")) {
                logger.error("Failed to get user data from Facebook: {}", userData);
                result.put("success", false);
                result.put("error", "Failed to get user info");
                return result;
            }
            
            String facebookId = (String) userData.get("id");
            String email = (String) userData.get("email");
            String name = (String) userData.get("name");
            String pictureUrl = null;
            
            // Check if we need to register a new user or login an existing user
            boolean isSignup = state != null && state.startsWith("signup:");
            Optional<User> existingUser = userRepository.getUserByEmail(email);
            
            if (isSignup && existingUser.isPresent()) {
                // User already exists but trying to signup
                result.put("success", false);
                result.put("error", "email_already_exists");
                return result;
            }
            
            if (isSignup || !existingUser.isPresent()) {
                // Create new user - signup flow
                SignupRequest signupRequest = new SignupRequest();
                signupRequest.setEmail(email);
                signupRequest.setName(name);
                signupRequest.setPhotoURL(pictureUrl);
                signupRequest.setAuthMethod("facebook");
                
                // Store provider ID in auth data
                Map<String, String> authData = new HashMap<>();
                authData.put("providerId", facebookId);
                authData.put("email", email);
                signupRequest.setAuthData(authData);
                
                try {
                    SignupResponse signupResponse = signupService.processSignup(signupRequest);
                    
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", signupResponse.getUserId());
                    userMap.put("email", email);
                    userMap.put("name", name);
                    
                    result.put("success", true);
                    result.put("isNewUser", true);
                    result.put("user", userMap);
                    result.put("accessToken", signupResponse.getAccessToken());
                    result.put("refreshToken", signupResponse.getRefreshToken());
                    
                    return result;
                } catch (Exception e) {
                    logger.error("Error during signup process", e);
                    result.put("success", false);
                    result.put("error", "Signup failed: " + e.getMessage());
                    return result;
                }
            } else {
                // Login flow - user exists
                User user = existingUser.get();
                
                // Check if user has Facebook auth method or create one
                List<OAuthAuthenticationMethod> existingMethods = authenticationMethodRepository
                        .findByUserIdAndProviderAndProviderId(user.getUserId(), "facebook", facebookId);
                
                if (existingMethods.isEmpty()) {
                    // Link Facebook account to existing user
                    OAuthAuthenticationMethod oAuthMethod = new OAuthAuthenticationMethod();
                    oAuthMethod.setUserId(user.getUserId());
                    oAuthMethod.setProvider("facebook");
                    oAuthMethod.setProviderId(facebookId);
                    oAuthMethod.setEmail(email);
                    oAuthMethod.setCreatedAt(new Date());
                    oAuthMethod.setCreatedBy(user.getUserId());
                    oAuthMethod.setModifiedAt(new Date());
                    oAuthMethod.setModifiedBy(user.getUserId());
                    oAuthMethod.setIsActive(true);
                    authenticationMethodRepository.save(oAuthMethod);
                } else {
                    // Reactivate if needed
                    OAuthAuthenticationMethod oAuthMethod = existingMethods.get(0);
                    if (!oAuthMethod.isActive()) {
                        oAuthMethod.setIsActive(true);
                        oAuthMethod.setModifiedBy(user.getUserId());
                        oAuthMethod.setModifiedAt(new Date());
                        authenticationMethodRepository.save(oAuthMethod);
                    }
                }
                
                // Generate authentication tokens
                String userAccessToken = tokenProvider.createAccessToken(user.getUserId());
                String refreshToken = tokenProvider.createRefreshToken(user.getUserId());
                
                // Build result with user and tokens
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getUserId());
                userMap.put("email", user.getProfile().getEmail());
                userMap.put("name", user.getProfile().getName());
                
                result.put("success", true);
                result.put("isNewUser", false);
                result.put("user", userMap);
                result.put("accessToken", userAccessToken);
                result.put("refreshToken", refreshToken);
                
                return result;
            }
        } catch (Exception e) {
            logger.error("Unexpected error in Facebook OAuth callback", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
