package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.GoogleOAuthService;
import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Google OAuth authentication flow
 */
@RestController
@RequestMapping("/auth/oauth/google")
public class GoogleOAuthController extends BaseApiController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthController.class);
    
    private final GoogleOAuthService googleOAuthService;
    
    public GoogleOAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }
    
    /**
     * Initiates the Google OAuth flow with direct redirect
     * @param isSignup Whether this is part of signup flow or just login
     * @return Redirect to Google's authorization page
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth(
            @RequestParam(defaultValue = "false") boolean isSignup) {
        
        Map<String, String> authInfo = googleOAuthService.getAuthorizationUrl(isSignup);
        logger.info("Redirecting to Google OAuth: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Google
     * 
     * @param code Authorization code from Google
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Redirect to frontend with tokens
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String deviceId) {
        
        logger.info("Received OAuth callback with state: {} and deviceId: {}", state, deviceId);
        
        try {
            // Delegate to service to handle the OAuth callback
            Map<String, Object> result = googleOAuthService.handleOAuthCallback(code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth?error=%s", 
                        googleOAuthService.getFrontendUrl(), error));
            }
            
            // Extract data for redirect
            String userId = ((Map<?, ?>)result.get("user")).get("userId").toString();
            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");
            boolean isNewUser = (Boolean) result.getOrDefault("isNewUser", false);
            
            // Build the redirect URL
            String redirectUrl = String.format("%s/auth/callback?accessToken=%s&refreshToken=%s&userId=%s&isNewUser=%s", 
                    googleOAuthService.getFrontendUrl(), accessToken, refreshToken, userId, isNewUser);
            
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            logger.error("Error in Google OAuth callback", e);
            return new RedirectView(googleOAuthService.getFrontendUrl() + "/auth?error=" + e.getMessage());
        }
    }
}
