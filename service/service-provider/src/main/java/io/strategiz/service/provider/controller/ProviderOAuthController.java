package io.strategiz.service.provider.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.strategiz.service.provider.ProviderOAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;

/**
 * Controller for OAuth provider connections (exchanges, wallets, etc.).
 * Handles connecting and disconnecting OAuth-based providers.
 * Uses clean architecture - returns resources directly, no wrappers.
 */
@RestController
@RequestMapping("/api/provider/oauth")
public class ProviderOAuthController {

    private static final Logger log = LoggerFactory.getLogger(ProviderOAuthController.class);

    private final ProviderOAuthService providerOAuthService;

    public ProviderOAuthController(ProviderOAuthService providerOAuthService) {
        this.providerOAuthService = providerOAuthService;
    }

    /**
     * Initiate OAuth connection with a provider
     * 
     * @param providerId The provider ID (e.g., "coinbase", "binance")
     * @param principal The authenticated user
     * @return Clean OAuth initialization response with authorization URL - no wrapper, let GlobalExceptionHandler handle errors
     */
    @PostMapping("/{providerId}/connect")
    public ResponseEntity<Map<String, String>> initiateOAuthFlow(
            @PathVariable String providerId,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Initiating OAuth flow for provider: {} and user: {}", providerId, userId);
        
        // Get authorization URL - let exceptions bubble up
        Map<String, String> authorizationData = providerOAuthService.initiateOAuth(providerId, userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(authorizationData);
    }

    /**
     * Disconnect a provider from user's account
     * 
     * @param providerId The provider ID to disconnect
     * @param principal The authenticated user
     * @return Clean disconnection response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @DeleteMapping("/{providerId}/disconnect")
    public ResponseEntity<Map<String, String>> disconnectProvider(
            @PathVariable String providerId,
            Principal principal) {
        
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Disconnecting provider: {} for user: {}", providerId, userId);
        
        // Disconnect provider - let exceptions bubble up
        Map<String, String> disconnectionResult = providerOAuthService.disconnectProvider(providerId, userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(disconnectionResult);
    }

    /**
     * Get the status of provider connections for the user
     * 
     * @param principal The authenticated user
     * @return Clean provider status response - no wrapper, let GlobalExceptionHandler handle errors
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProviderStatus(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Authentication required");
        }
        
        String userId = principal.getName();
        log.info("Getting provider status for user: {}", userId);
        
        // Get provider status - let exceptions bubble up
        Map<String, Object> providerStatus = providerOAuthService.getProviderStatus(userId);
        
        // Return clean response - headers added by StandardHeadersInterceptor
        return ResponseEntity.ok(providerStatus);
    }
}
