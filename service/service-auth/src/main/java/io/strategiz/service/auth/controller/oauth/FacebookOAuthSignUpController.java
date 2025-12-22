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
            Map<String, Object> result = facebookOAuthService.handleOAuthCallback(code, state, null, true);

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
     * Handle OAuth callback from Facebook for sign-up (redirect to frontend)
     *
     * This endpoint receives the authorization code from Facebook and redirects
     * to the frontend with the code. The frontend will then call the POST
     * callback endpoint to exchange the code for tokens.
     *
     * IMPORTANT: Do NOT process the code here - OAuth codes can only be used once!
     *
     * @param code Authorization code from Facebook
     * @param state State parameter for verification
     * @param error Optional error parameter from Facebook
     * @param errorDescription Optional error description from Facebook
     * @return Redirect to frontend with code or error
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        logger.info("Received OAuth sign-up callback with state: {}", state);

        // Handle OAuth errors from Facebook
        if (error != null) {
            logger.error("OAuth error from Facebook: {} - {}", error, errorDescription);
            String errorMsg = errorDescription != null ? errorDescription : error;
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s",
                    facebookOAuthService.getFrontendUrl(),
                    java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8)));
        }

        // Validate code is present
        if (code == null || code.isEmpty()) {
            logger.error("No authorization code received from Facebook");
            return new RedirectView(String.format("%s/auth/oauth/facebook/callback?error=%s",
                    facebookOAuthService.getFrontendUrl(),
                    java.net.URLEncoder.encode("No authorization code received", java.nio.charset.StandardCharsets.UTF_8)));
        }

        // Redirect to frontend with the code - frontend will call POST callback to exchange it
        logger.info("Redirecting to frontend with authorization code for sign-up");
        return new RedirectView(String.format("%s/auth/oauth/facebook/callback?code=%s&state=%s",
                facebookOAuthService.getFrontendUrl(),
                java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8),
                state != null ? java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8) : ""));
    }
}