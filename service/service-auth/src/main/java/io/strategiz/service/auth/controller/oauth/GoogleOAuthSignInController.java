package io.strategiz.service.auth.controller.oauth;

import io.strategiz.service.auth.service.oauth.GoogleOAuthService;
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
 * Controller for handling Google OAuth sign-in flow
 * Specifically for existing users logging in via Google
 */
@RestController
@RequestMapping("/v1/auth/oauth/google/signin")
public class GoogleOAuthSignInController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthSignInController.class);
    
    private final GoogleOAuthService googleOAuthService;
    private final CookieUtil cookieUtil;

    public GoogleOAuthSignInController(GoogleOAuthService googleOAuthService, CookieUtil cookieUtil) {
        this.googleOAuthService = googleOAuthService;
        this.cookieUtil = cookieUtil;
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
     * @param httpResponse HTTP response for setting cookies
     * @return JSON response with user data and success status
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallbackJson(
            @RequestBody Map<String, String> callbackRequest,
            HttpServletResponse httpResponse) {

        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");

        logger.info("Received OAuth sign-in callback JSON with state: {}", state);

        try {
            Map<String, Object> result = googleOAuthService.handleOAuthCallback(code, state, null, false);

            // Set HTTP-only cookies for session management
            String accessToken = (String) result.get("accessToken");
            String refreshToken = (String) result.get("refreshToken");
            if (accessToken != null) {
                cookieUtil.setAccessTokenCookie(httpResponse, accessToken);
            }
            if (refreshToken != null) {
                cookieUtil.setRefreshTokenCookie(httpResponse, refreshToken);
            }
            logger.info("Authentication cookies set for Google OAuth sign-in");

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
     * Handle OAuth callback from Google for sign-in (redirect to frontend)
     *
     * This endpoint receives the authorization code from Google and redirects
     * to the frontend with the code. The frontend will then call the POST
     * callback endpoint to exchange the code for tokens.
     *
     * IMPORTANT: Do NOT process the code here - OAuth codes can only be used once!
     *
     * @param code Authorization code from Google
     * @param state State parameter for verification
     * @param error Optional error parameter from Google
     * @param errorDescription Optional error description from Google
     * @return Redirect to frontend with code or error
     */
    @GetMapping("/callback")
    public RedirectView handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        logger.info("Received OAuth sign-in callback with state: {}", state);

        // Handle OAuth errors from Google
        if (error != null) {
            logger.error("OAuth error from Google: {} - {}", error, errorDescription);
            String errorMsg = errorDescription != null ? errorDescription : error;
            return new RedirectView(String.format("%s/auth/oauth/google/callback?error=%s",
                    googleOAuthService.getFrontendUrl(),
                    java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8)));
        }

        // Validate code is present
        if (code == null || code.isEmpty()) {
            logger.error("No authorization code received from Google");
            return new RedirectView(String.format("%s/auth/oauth/google/callback?error=%s",
                    googleOAuthService.getFrontendUrl(),
                    java.net.URLEncoder.encode("No authorization code received", java.nio.charset.StandardCharsets.UTF_8)));
        }

        // Redirect to frontend with the code - frontend will call POST callback to exchange it
        logger.info("Redirecting to frontend with authorization code for sign-in");
        return new RedirectView(String.format("%s/auth/oauth/google/callback?code=%s&state=%s",
                googleOAuthService.getFrontendUrl(),
                java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8),
                state != null ? java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8) : ""));
    }
}