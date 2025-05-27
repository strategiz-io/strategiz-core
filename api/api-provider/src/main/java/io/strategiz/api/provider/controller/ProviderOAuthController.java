package io.strategiz.api.provider.controller;

import io.americanexpress.synapse.service.rest.controller.BaseController;
import io.strategiz.service.provider.ProviderOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling OAuth flows with exchanges and brokerages.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/provider")
public class ProviderOAuthController extends BaseController {

    private final ProviderOAuthService providerOAuthService;
    
    @Value("${application.frontend-url}")
    private String frontendUrl;

    @Autowired
    public ProviderOAuthController(ProviderOAuthService providerOAuthService) {
        this.providerOAuthService = providerOAuthService;
    }

    /**
     * Initiates the OAuth flow for a provider.
     *
     * @param providerId The provider ID (kraken, binanceus, charlesschwab)
     * @param accountType The account type (paper or real)
     * @param principal The authenticated user
     * @return The authorization URL
     */
    @GetMapping("/auth/{providerId}")
    public ResponseEntity<Map<String, String>> initiateOAuthFlow(
            @PathVariable String providerId,
            @RequestParam(defaultValue = "paper") String accountType,
            Principal principal) {
        
        String userId = principal.getName();
        String state = UUID.randomUUID().toString();
        
        // In a real implementation, you would store this state to validate the callback
        
        String authUrl = providerOAuthService.generateAuthorizationUrl(
                providerId, userId, accountType, state);
        
        Map<String, String> response = new HashMap<>();
        response.put("authUrl", authUrl);
        response.put("state", state);
        
        return ResponseEntity.ok(response);
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
        
        // In a real implementation, you would validate the state parameter
        
        if (error != null) {
            log.error("OAuth error from provider {}: {}", providerId, error);
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
        
        boolean success = providerOAuthService.handleOAuthCallback(
                providerId, userId, code, accountType);
        
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
    @DeleteMapping("/{providerId}")
    public ResponseEntity<Map<String, String>> disconnectProvider(
            @PathVariable String providerId,
            Principal principal) {
        
        String userId = principal.getName();
        boolean success = providerOAuthService.disconnectProvider(userId, providerId);
        
        Map<String, String> response = new HashMap<>();
        
        if (success) {
            response.put("status", "success");
            response.put("message", "Successfully disconnected provider " + providerId);
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to disconnect provider " + providerId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Gets the connection status of all providers for the user.
     *
     * @param principal The authenticated user
     * @return List of connected providers
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProviderStatus(Principal principal) {
        // This would normally query the user repository to get actual provider statuses
        // For now, we're just returning a mock response
        
        String userId = principal.getName();
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> providers = new HashMap<>();
        
        Map<String, Object> kraken = new HashMap<>();
        kraken.put("connected", false);
        kraken.put("accountType", "paper");
        
        Map<String, Object> binanceus = new HashMap<>();
        binanceus.put("connected", false);
        binanceus.put("accountType", "paper");
        
        Map<String, Object> charlesschwab = new HashMap<>();
        charlesschwab.put("connected", false);
        charlesschwab.put("accountType", "paper");
        
        providers.put("kraken", kraken);
        providers.put("binanceus", binanceus);
        providers.put("charlesschwab", charlesschwab);
        
        response.put("providers", providers);
        response.put("accountMode", "paper");
        
        return ResponseEntity.ok(response);
    }
}
