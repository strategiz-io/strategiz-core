package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.FacebookOAuthService;
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
 * Controller for handling Facebook OAuth authentication flow
 */
@RestController
@RequestMapping("/auth/oauth/facebook")
public class FacebookOAuthController extends BaseApiController {

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthController.class);
    
    private final FacebookOAuthService facebookOAuthService;
    
    public FacebookOAuthController(FacebookOAuthService facebookOAuthService) {
        this.facebookOAuthService = facebookOAuthService;
    }
    
    /**
     * Initiates the Facebook OAuth flow
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
     * Handle OAuth callback from Facebook
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
            // Delegate to service to handle the OAuth callback
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth?error=%s", 
                        facebookOAuthService.getFrontendUrl(), error));
            }
            
            // Extract data for redirect
            String userId = ((Map<?, ?>)result.get("user")).get("userId").toString();
            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");
            boolean isNewUser = (Boolean) result.getOrDefault("isNewUser", false);
            
            // Build the redirect URL
            String redirectUrl = String.format("%s/auth/callback?accessToken=%s&refreshToken=%s&userId=%s&isNewUser=%s", 
                    facebookOAuthService.getFrontendUrl(), accessToken, refreshToken, userId, isNewUser);
            
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            logger.error("Error in Facebook OAuth callback", e);
            return new RedirectView(facebookOAuthService.getFrontendUrl() + "/auth?error=" + e.getMessage());
        }
    }
}
