package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.GoogleOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Google OAuth sign-in flow
 * Specifically for existing users logging in via Google
 */
@RestController
@RequestMapping("/auth/oauth/google/signin")
public class GoogleOAuthSignInController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthSignInController.class);
    
    private final GoogleOAuthService googleOAuthService;
    
    public GoogleOAuthSignInController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }
    
    /**
     * Get Google OAuth authorization URL for sign-in
     * @return JSON response with authorization URL
     */
    @GetMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        Map<String, String> authInfo = googleOAuthService.getAuthorizationUrl(false); // isSignup = false
        logger.info("Providing Google OAuth sign-in authorization URL: {}", authInfo.get("url"));
        return ResponseEntity.ok(authInfo);
    }

    /**
     * Initiates the Google OAuth sign-in flow with direct redirect
     * @return Redirect to Google's authorization URL
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth() {
        Map<String, String> authInfo = googleOAuthService.getAuthorizationUrl(false); // isSignup = false
        logger.info("Redirecting to Google OAuth sign-in: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Google for sign-in
     * 
     * @param callbackRequest JSON request with code and state
     * @return JSON response with user data and success status
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallbackJson(
            @RequestBody Map<String, String> callbackRequest) {
        
        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");
        
        logger.info("Received OAuth sign-in callback JSON with state: {}", state);
        
        try {
            Map<String, Object> result = googleOAuthService.handleOAuthCallback(code, state, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in Google OAuth sign-in callback", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Google OAuth sign-in callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle OAuth callback from Google for sign-in (legacy redirect)
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
        
        logger.info("Received OAuth sign-in callback with state: {} and deviceId: {}", state, deviceId);
        
        try {
            Map<String, Object> result = googleOAuthService.handleOAuthCallback(code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth/oauth/google/callback?error=%s", 
                        googleOAuthService.getFrontendUrl(), error));
            }
            
            return new RedirectView(String.format("%s/auth/oauth/google/callback?code=%s&state=%s", 
                    googleOAuthService.getFrontendUrl(), code, state));
        } catch (Exception e) {
            logger.error("Error in Google OAuth sign-in callback", e);
            return new RedirectView(String.format("%s/auth/oauth/google/callback?error=%s", 
                    googleOAuthService.getFrontendUrl(), e.getMessage()));
        }
    }
}