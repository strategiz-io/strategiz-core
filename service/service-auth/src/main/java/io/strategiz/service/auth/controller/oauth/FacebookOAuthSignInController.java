package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.FacebookOAuthService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Facebook OAuth sign-in flow
 * Specifically for existing users logging in via Facebook
 */
@RestController
@RequestMapping("/v1/auth/oauth/facebook/signin")
public class FacebookOAuthSignInController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthSignInController.class);
    
    private final FacebookOAuthService facebookOAuthService;
    
    public FacebookOAuthSignInController(FacebookOAuthService facebookOAuthService) {
        this.facebookOAuthService = facebookOAuthService;
    }
    
    /**
     * Get Facebook OAuth authorization URL for sign-in
     * @return JSON response with authorization URL
     */
    @GetMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(false); // isSignup = false
        logger.info("Providing Facebook OAuth sign-in authorization URL: {}", authInfo.get("url"));
        return ResponseEntity.ok(authInfo);
    }

    /**
     * Initiates the Facebook OAuth sign-in flow with direct redirect
     * @return Redirect to Facebook's authorization URL
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth() {
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(false); // isSignup = false
        logger.info("Redirecting to Facebook OAuth sign-in: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Facebook for sign-in
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
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in Facebook OAuth sign-in callback", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Facebook OAuth sign-in callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle OAuth callback from Facebook for sign-in (legacy redirect)
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
        
        logger.info("Received OAuth sign-in callback with state: {} and deviceId: {}", state, deviceId);
        
        try {
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s", 
                        facebookOAuthService.getFrontendUrl(), error));
            }
            
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?code=%s&state=%s", 
                    facebookOAuthService.getFrontendUrl(), code, state));
        } catch (Exception e) {
            logger.error("Error in Facebook OAuth sign-in callback", e);
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s", 
                    facebookOAuthService.getFrontendUrl(), e.getMessage()));
        }
    }
}