package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.GoogleOAuthService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Google OAuth sign-up flow
 * Specifically for new users creating accounts via Google
 */
@RestController
@RequestMapping("/v1/auth/oauth/google/signup")
public class GoogleOAuthSignUpController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthSignUpController.class);
    
    private final GoogleOAuthService googleOAuthService;
    
    public GoogleOAuthSignUpController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }
    
    /**
     * Get Google OAuth authorization URL for sign-up
     * @return JSON response with authorization URL
     */
    @GetMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        Map<String, String> authInfo = googleOAuthService.getAuthorizationUrl(true); // isSignup = true
        logger.info("Providing Google OAuth sign-up authorization URL: {}", authInfo.get("url"));
        return ResponseEntity.ok(authInfo);
    }

    /**
     * Initiates the Google OAuth sign-up flow with direct redirect
     * @return Redirect to Google's authorization URL
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth() {
        Map<String, String> authInfo = googleOAuthService.getAuthorizationUrl(true); // isSignup = true
        logger.info("Redirecting to Google OAuth sign-up: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Google for sign-up
     * 
     * @param callbackRequest JSON request with code and state
     * @return JSON response with user data and success status
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallbackJson(
            @RequestBody Map<String, String> callbackRequest) {
        
        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");
        
        logger.info("Received OAuth sign-up callback JSON with state: {}", state);
        
        try {
            Map<String, Object> result = googleOAuthService.handleOAuthCallback(code, state, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in Google OAuth sign-up callback", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Google OAuth sign-up callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle OAuth callback from Google for sign-up (legacy redirect)
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
        
        logger.info("Received OAuth sign-up callback with state: {} and deviceId: {}", state, deviceId);
        
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
            logger.error("Error in Google OAuth sign-up callback", e);
            return new RedirectView(String.format("%s/auth/oauth/google/callback?error=%s", 
                    googleOAuthService.getFrontendUrl(), e.getMessage()));
        }
    }
}