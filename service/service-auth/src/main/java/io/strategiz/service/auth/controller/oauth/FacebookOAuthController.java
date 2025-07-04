package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.FacebookOAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Facebook OAuth authentication flow
 * Uses clean architecture - returns resources directly, no wrappers
 */
@RestController
@RequestMapping("/auth/oauth/facebook")
public class FacebookOAuthController {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthController.class);
    
    private final FacebookOAuthService facebookOAuthService;
    
    public FacebookOAuthController(FacebookOAuthService facebookOAuthService) {
        this.facebookOAuthService = facebookOAuthService;
    }
    
    /**
     * Get Facebook OAuth authorization URL as JSON (for frontend)
     * @param isSignup Whether this is part of signup flow or just login
     * @return JSON response with authorization URL
     */
    @GetMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl(
            @RequestParam(defaultValue = "false") boolean isSignup) {
        
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(isSignup);
        logger.info("Providing Facebook OAuth authorization URL: {}", authInfo.get("url"));
        return ResponseEntity.ok(authInfo);
    }

    /**
     * Initiates the Facebook OAuth flow with direct redirect (legacy)
     * @param isSignup Whether this is part of signup flow or just login
     * @return Redirect to Facebook's authorization URL
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth(
            @RequestParam(defaultValue = "false") boolean isSignup) {
        
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(isSignup);
        logger.info("Redirecting to Facebook OAuth: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Facebook (JSON API for new frontend)
     * 
     * @param callbackRequest JSON request with code and state
     * @return OAuth callback result - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallbackJson(
            @RequestBody Map<String, String> callbackRequest) {
        
        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");
        
        logger.info("Received OAuth callback JSON with state: {}", state);
        
        // Clean architecture - let exceptions bubble up to GlobalExceptionHandler
        Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, null);
        return ResponseEntity.ok(result);
    }

    /**
     * Handle OAuth callback from Facebook (legacy redirect)
     * 
     * @param code Authorization code from Facebook
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
            // Note: For redirects, we still need try-catch since we can't return StandardErrorResponse
            // GlobalExceptionHandler doesn't handle RedirectView returns
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s", 
                        facebookOAuthService.getFrontendUrl(), error));
            }
            
            // Extract data for redirect to our frontend callback handler
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?code=%s&state=%s", 
                    facebookOAuthService.getFrontendUrl(), code, state));
        } catch (Exception e) {
            logger.error("Error in Facebook OAuth callback", e);
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s", 
                    facebookOAuthService.getFrontendUrl(), e.getMessage()));
        }
    }
}
