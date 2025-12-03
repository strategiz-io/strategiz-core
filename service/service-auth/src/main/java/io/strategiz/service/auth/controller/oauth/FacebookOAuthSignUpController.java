package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.FacebookOAuthService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Controller for handling Facebook OAuth sign-up flow
 * Specifically for new users creating accounts via Facebook
 */
@RestController
@RequestMapping("/v1/auth/oauth/facebook/signup")
public class FacebookOAuthSignUpController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger logger = LoggerFactory.getLogger(FacebookOAuthSignUpController.class);
    
    private final FacebookOAuthService facebookOAuthService;
    private final CookieUtil cookieUtil;

    public FacebookOAuthSignUpController(FacebookOAuthService facebookOAuthService, CookieUtil cookieUtil) {
        this.facebookOAuthService = facebookOAuthService;
        this.cookieUtil = cookieUtil;
    }
    
    /**
     * Get Facebook OAuth authorization URL for sign-up
     * @return JSON response with authorization URL
     */
    @GetMapping("/authorization-url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(true); // isSignup = true
        logger.info("Providing Facebook OAuth sign-up authorization URL: {}", authInfo.get("url"));
        return ResponseEntity.ok(authInfo);
    }

    /**
     * Initiates the Facebook OAuth sign-up flow with direct redirect
     * @return Redirect to Facebook's authorization URL
     */
    @GetMapping("/auth")
    public RedirectView initiateOAuth() {
        Map<String, String> authInfo = facebookOAuthService.getAuthorizationUrl(true); // isSignup = true
        logger.info("Redirecting to Facebook OAuth sign-up: {}", authInfo.get("url"));
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback from Facebook for sign-up
     *
     * @param callbackRequest JSON request with code and state
     * @param httpResponse HTTP response for setting cookies
     * @return JSON response with user data and success status
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallbackJson(
            @RequestBody Map<String, String> callbackRequest,
            HttpServletResponse httpResponse) {

        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");

        logger.info("Received OAuth sign-up callback JSON with state: {}", state);

        try {
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, null);

            // Set HTTP-only cookies for session management
            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");
            if (accessToken != null) {
                cookieUtil.setAccessTokenCookie(httpResponse, accessToken);
            }
            if (refreshToken != null) {
                cookieUtil.setRefreshTokenCookie(httpResponse, refreshToken);
            }
            logger.info("Authentication cookies set for Facebook OAuth sign-up");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in Facebook OAuth sign-up callback", e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "Facebook OAuth sign-up callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle OAuth callback from Facebook for sign-up (legacy redirect)
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
        
        logger.info("Received OAuth sign-up callback with state: {} and deviceId: {}", state, deviceId);
        
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
            logger.error("Error in Facebook OAuth sign-up callback", e);
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s", 
                    facebookOAuthService.getFrontendUrl(), e.getMessage()));
        }
    }
}