package io.strategiz.service.provider.controller;

import io.strategiz.service.base.controller.BaseApiController;
import io.strategiz.service.base.model.ApiResponseWrapper;
import io.strategiz.service.provider.ProviderOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling OAuth flows with exchanges and brokerages.
 * Extends BaseApiController for standardized API response handling.
 */
@RestController
@RequestMapping("/api/v1/provider")
public class ProviderOAuthController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(ProviderOAuthController.class);
    
    @Value("${application.frontend-url}")
    private String frontendUrl;
    
    private final ProviderOAuthService providerOAuthService;

    public ProviderOAuthController(ProviderOAuthService providerOAuthService) {
        this.providerOAuthService = providerOAuthService;
    }

    /**
     * Initiates the OAuth flow for a provider.
     *
     * @param providerId The provider ID (kraken, binanceus)
     * @param accountType The account type (paper or real)
     * @param principal The authenticated user
     * @return The authorization URL
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/auth/{providerId}")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> initiateOAuthFlow(
            @PathVariable String providerId,
            @RequestParam(defaultValue = "paper") String accountType,
            Principal principal) {
        
        String userId = principal.getName();
        String state = UUID.randomUUID().toString();
        
        try {
            String authorizationUrl = providerOAuthService.generateAuthorizationUrl(
                    providerId, userId, accountType, state);
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("authUrl", authorizationUrl);
            responseData.put("state", state);
            
            return success(responseData);
        } catch (IllegalArgumentException e) {
            log.error("Error generating authorization URL for provider {}: {}", providerId, e.getMessage());
            return badRequest("Unsupported provider: " + providerId);
        } catch (Exception e) {
            log.error("Unexpected error initiating OAuth flow for provider {}: {}", providerId, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH_FLOW_ERROR", 
                    "An error occurred initiating OAuth flow");
        }
    }

    /**
     * Handles the OAuth callback from providers.
     *
     * @param providerId The provider ID
     * @param code The authorization code
     * @param state The state parameter for security
     * @param error Any error that occurred
     * @return A redirect to the frontend with success or error status
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/callback/{providerId}")
    public RedirectView handleOAuthCallback(
            @PathVariable String providerId,
            @RequestParam(required = false) String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String accountType,
            Principal principal) {
        
        String userId = principal.getName();
        String redirectUrl = frontendUrl + "/account/providers";
        
        if (error != null) {
            log.info("OAuth callback for provider: {}, code: {}, state: {}, error: {}", 
                    providerId, code, state, error);
            return new RedirectView(redirectUrl + "?status=error&provider=" + providerId);
        }
        
        if (code == null) {
            log.error("No authorization code received from provider {}", providerId);
            return new RedirectView(redirectUrl + "?status=error&provider=" + providerId);
        }
        
        // Default to paper trading if not specified
        if (accountType == null) {
            accountType = "paper";
        }
        
        boolean success = false;
        try {
            success = providerOAuthService.handleOAuthCallback(providerId, userId, code, accountType);
        } catch (Exception e) {
            log.error("Error handling OAuth callback for provider {}: {}", providerId, e.getMessage());
        }
        
        if (success) {
            return new RedirectView(redirectUrl + "?status=success&provider=" + providerId);
        } else {
            return new RedirectView(redirectUrl + "?status=error&provider=" + providerId);
        }
    }

    /**
     * Disconnects a provider.
     *
     * @param providerId The provider ID
     * @param principal The authenticated user
     * @return Success or error message
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{providerId}")
    public ResponseEntity<ApiResponseWrapper<Map<String, String>>> disconnectProvider(
            @PathVariable String providerId,
            Principal principal) {
        
        String userId = principal.getName();
        Map<String, String> responseData = new HashMap<>();
        
        try {
            boolean success = providerOAuthService.disconnectProvider(userId, providerId);
            
            if (success) {
                responseData.put("status", "success");
                responseData.put("message", "Successfully disconnected provider " + providerId);
                return success(responseData);
            } else {
                return error(HttpStatus.INTERNAL_SERVER_ERROR, "DISCONNECT_FAILED", 
                        "Failed to disconnect provider " + providerId);
            }
        } catch (Exception e) {
            log.error("Error disconnecting provider {}: {}", providerId, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "DISCONNECT_ERROR", 
                    "An error occurred disconnecting the provider");
        }
    }

    /**
     * Gets the connection status of all providers for the user.
     *
     * @param principal The authenticated user
     * @return List of connected providers
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/status")
    public ResponseEntity<ApiResponseWrapper<Map<String, Object>>> getProviderStatus(Principal principal) {
        String userId = principal.getName();
        Map<String, Object> response = new HashMap<>();
        
        // This would normally query the user repository to get actual provider statuses
        // For now, we're just returning a mock response
        Map<String, Object> providers = new HashMap<>();
        
        Map<String, Object> kraken = new HashMap<>();
        kraken.put("connected", false);
        kraken.put("accountType", "paper");
        
        Map<String, Object> binanceus = new HashMap<>();
        binanceus.put("connected", false);
        binanceus.put("accountType", "paper");
        
        providers.put("kraken", kraken);
        providers.put("binanceus", binanceus);
        
        response.put("providers", providers);
        response.put("accountMode", "paper");
        
        return success(response);
    }
}
