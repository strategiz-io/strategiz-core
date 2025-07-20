package io.strategiz.service.auth.controller.oauth;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.service.oauth.OAuthProviderService;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

/**
 * Generic OAuth provider controller that delegates to specific OAuth services.
 * This controller provides a unified interface for all OAuth providers.
 */
@RestController
@RequestMapping("/v1/auth/oauth/{provider}")
public class OAuthProviderController extends BaseController {

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    private static final Logger logger = LoggerFactory.getLogger(OAuthProviderController.class);
    
    private final OAuthProviderService oauthProviderService;
    
    public OAuthProviderController(OAuthProviderService oauthProviderService) {
        this.oauthProviderService = oauthProviderService;
    }
    
    /**
     * Get OAuth authorization URL for sign-up
     * @param provider OAuth provider name (google, facebook, coinbase, etc.)
     * @return JSON response with authorization URL
     */
    @GetMapping("/signup/authorization-url")
    public ResponseEntity<Map<String, String>> getSignupAuthorizationUrl(@PathVariable String provider) {
        logger.info("Getting {} OAuth sign-up authorization URL", provider);
        try {
            Map<String, String> authInfo = oauthProviderService.getAuthorizationUrl(provider, true);
            return ResponseEntity.ok(authInfo);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting {} OAuth sign-up authorization URL", provider, e);
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "OAuth provider not supported: " + provider);
        }
    }
    
    /**
     * Get OAuth authorization URL for sign-in
     * @param provider OAuth provider name (google, facebook, coinbase, etc.)
     * @return JSON response with authorization URL
     */
    @GetMapping("/signin/authorization-url")
    public ResponseEntity<Map<String, String>> getSigninAuthorizationUrl(@PathVariable String provider) {
        logger.info("Getting {} OAuth sign-in authorization URL", provider);
        try {
            Map<String, String> authInfo = oauthProviderService.getAuthorizationUrl(provider, false);
            return ResponseEntity.ok(authInfo);
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting {} OAuth sign-in authorization URL", provider, e);
            throw new StrategizException(AuthErrors.INVALID_TOKEN, "OAuth provider not supported: " + provider);
        }
    }

    /**
     * Initiate OAuth sign-up flow with direct redirect
     * @param provider OAuth provider name
     * @return Redirect to provider's authorization URL
     */
    @GetMapping("/signup/auth")
    public RedirectView initiateSignupOAuth(@PathVariable String provider) {
        logger.info("Initiating {} OAuth sign-up flow", provider);
        Map<String, String> authInfo = oauthProviderService.getAuthorizationUrl(provider, true);
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Initiate OAuth sign-in flow with direct redirect
     * @param provider OAuth provider name
     * @return Redirect to provider's authorization URL
     */
    @GetMapping("/signin/auth")
    public RedirectView initiateSigninOAuth(@PathVariable String provider) {
        logger.info("Initiating {} OAuth sign-in flow", provider);
        Map<String, String> authInfo = oauthProviderService.getAuthorizationUrl(provider, false);
        return new RedirectView(authInfo.get("url"));
    }
    
    /**
     * Handle OAuth callback for sign-up (POST)
     * @param provider OAuth provider name
     * @param callbackRequest JSON request with code and state
     * @return JSON response with user data and success status
     */
    @PostMapping("/signup/callback")
    public ResponseEntity<Map<String, Object>> handleSignupCallbackJson(
            @PathVariable String provider,
            @RequestBody Map<String, String> callbackRequest) {
        
        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");
        String deviceId = callbackRequest.get("deviceId");
        
        logger.info("Received {} OAuth sign-up callback JSON with state: {}", provider, state);
        
        try {
            Map<String, Object> result = oauthProviderService.handleOAuthCallback(provider, code, state, deviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in {} OAuth sign-up callback", provider, e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", provider + " OAuth sign-up callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Handle OAuth callback for sign-in (POST)
     * @param provider OAuth provider name
     * @param callbackRequest JSON request with code and state
     * @return JSON response with user data and success status
     */
    @PostMapping("/signin/callback")
    public ResponseEntity<Map<String, Object>> handleSigninCallbackJson(
            @PathVariable String provider,
            @RequestBody Map<String, String> callbackRequest) {
        
        String code = callbackRequest.get("code");
        String state = callbackRequest.get("state");
        String deviceId = callbackRequest.get("deviceId");
        
        logger.info("Received {} OAuth sign-in callback JSON with state: {}", provider, state);
        
        try {
            Map<String, Object> result = oauthProviderService.handleOAuthCallback(provider, code, state, deviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in {} OAuth sign-in callback", provider, e);
            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", provider + " OAuth sign-in callback failed"
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Handle OAuth callback for sign-up (GET - legacy redirect)
     * @param provider OAuth provider name
     * @param code Authorization code from provider
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Redirect to frontend with tokens
     */
    @GetMapping("/signup/callback")
    public RedirectView handleSignupCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String deviceId) {
        
        logger.info("Received {} OAuth sign-up callback with state: {} and deviceId: {}", provider, state, deviceId);
        
        try {
            Map<String, Object> result = oauthProviderService.handleOAuthCallback(provider, code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            String frontendUrl = oauthProviderService.getFrontendUrl();
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth/oauth/%s/callback?error=%s", 
                        frontendUrl, provider, error));
            }
            
            return new RedirectView(String.format("%s/auth/oauth/%s/callback?code=%s&state=%s", 
                    frontendUrl, provider, code, state));
        } catch (Exception e) {
            logger.error("Error in {} OAuth sign-up callback", provider, e);
            String frontendUrl = oauthProviderService.getFrontendUrl();
            return new RedirectView(String.format("%s/auth/oauth/%s/callback?error=%s", 
                    frontendUrl, provider, e.getMessage()));
        }
    }
    
    /**
     * Handle OAuth callback for sign-in (GET - legacy redirect)
     * @param provider OAuth provider name
     * @param code Authorization code from provider
     * @param state State parameter for verification
     * @param deviceId Optional device ID for fingerprinting
     * @return Redirect to frontend with tokens
     */
    @GetMapping("/signin/callback")
    public RedirectView handleSigninCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String deviceId) {
        
        logger.info("Received {} OAuth sign-in callback with state: {} and deviceId: {}", provider, state, deviceId);
        
        try {
            Map<String, Object> result = oauthProviderService.handleOAuthCallback(provider, code, state, deviceId);
            
            boolean success = (Boolean) result.getOrDefault("success", false);
            String frontendUrl = oauthProviderService.getFrontendUrl();
            
            if (!success) {
                String error = (String) result.getOrDefault("error", "Unknown error");
                return new RedirectView(String.format("%s/auth/oauth/%s/callback?error=%s", 
                        frontendUrl, provider, error));
            }
            
            return new RedirectView(String.format("%s/auth/oauth/%s/callback?code=%s&state=%s", 
                    frontendUrl, provider, code, state));
        } catch (Exception e) {
            logger.error("Error in {} OAuth sign-in callback", provider, e);
            String frontendUrl = oauthProviderService.getFrontendUrl();
            return new RedirectView(String.format("%s/auth/oauth/%s/callback?error=%s", 
                    frontendUrl, provider, e.getMessage()));
        }
    }
}