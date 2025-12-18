package io.strategiz.service.provider.controller;

import io.strategiz.service.provider.service.ProviderCallbackService;
import io.strategiz.service.provider.model.response.ProviderCallbackResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.auth.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.Map;

/**
 * Controller for handling OAuth callbacks from provider integrations.
 * Processes authorization codes and completes OAuth flows for various providers.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@RestController
@RequestMapping("/v1/providers/callback")
public class ProviderCallbackController extends BaseController {
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.PROVIDER_MODULE;
    }
    
    private final ProviderCallbackService providerCallbackService;
    private final CookieUtil cookieUtil;

    @Autowired
    public ProviderCallbackController(ProviderCallbackService providerCallbackService,
                                      CookieUtil cookieUtil) {
        this.providerCallbackService = providerCallbackService;
        this.cookieUtil = cookieUtil;
    }
    
    /**
     * Handle OAuth callback from provider (GET request from OAuth provider).
     * Sets HTTP-only session cookie for authentication before redirecting to frontend.
     * Creates a database-backed session with full audit trail (IP, User-Agent).
     *
     * @param provider The provider name (coinbase, kraken, etc.)
     * @param code The authorization code from OAuth provider
     * @param state The state parameter for security validation
     * @param error Optional error parameter if OAuth failed
     * @param errorDescription Optional error description
     * @param servletRequest The HTTP request for extracting client info
     * @param servletResponse The HTTP response for setting cookies
     * @return Redirect to frontend with success or error
     */
    @GetMapping("/{provider}")
    public RedirectView handleOAuthCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        // Map "cb" to "coinbase" to avoid Coinbase's redirect URI restriction
        if ("cb".equals(provider)) {
            provider = "coinbase";
        }

        // Extract client info for enterprise-grade audit trail
        String clientIp = getClientIpAddress(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        log.info("=== OAuth Callback Received ===");
        log.info("Provider: {}, State: {}, Has code: {}, Error: {}, ClientIP: {}",
                provider, state, code != null ? "yes" : "no", error, clientIp);

        try {
            // Handle OAuth errors from provider
            if (error != null) {
                log.error("OAuth error from {}: {} - {}", provider, error, errorDescription);
                return new RedirectView(providerCallbackService.getErrorRedirectUrl(provider, error, errorDescription));
            }

            // Validate required parameters
            if (code == null || code.isEmpty()) {
                log.error("Missing authorization code from {} callback", provider);
                return new RedirectView(providerCallbackService.getErrorRedirectUrl(provider, "missing_code", "Authorization code is required"));
            }

            if (state == null || state.isEmpty()) {
                log.error("Missing state parameter from {} callback", provider);
                return new RedirectView(providerCallbackService.getErrorRedirectUrl(provider, "missing_state", "State parameter is required"));
            }

            // Process the OAuth callback with client info for audit trail
            log.info("Processing OAuth callback for {} - starting token exchange", provider);
            ProviderCallbackResponse response = providerCallbackService.processOAuthCallback(
                provider, code, state, clientIp, userAgent);
            log.info("OAuth callback successful for {} - status: {}", provider, response.getStatus());

            // Set HTTP-only session cookie for authentication
            // This ensures the frontend can validate the session after redirect
            String accessToken = response.getAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                cookieUtil.setAccessTokenCookie(servletResponse, accessToken);
                log.info("Set HTTP-only access token cookie for provider OAuth callback");
            } else {
                log.warn("No access token available to set cookie - user may need to re-authenticate");
            }

            // Redirect to frontend with success
            String redirectUrl = response.getRedirectUrl();
            log.info("Redirecting to: {}", redirectUrl);
            return new RedirectView(redirectUrl);

        } catch (Exception e) {
            log.error("Error processing OAuth callback for provider: {}", provider, e);
            return new RedirectView(providerCallbackService.getErrorRedirectUrl(provider, "processing_error", e.getMessage()));
        }
    }
    
    /**
     * Handle OAuth callback from provider (POST request from frontend).
     * Used when frontend captures the callback and sends it to backend.
     * 
     * @param provider The provider name
     * @param callbackData The callback data containing code and state
     * @param principal The authenticated user
     * @return Response with connection status
     */
    @PostMapping("/{provider}")
    public ResponseEntity<ProviderCallbackResponse> handleOAuthCallbackPost(
            @PathVariable String provider,
            @RequestBody Map<String, String> callbackData,
            Principal principal) {
        
        // Map "cb" to "coinbase" to avoid Coinbase's redirect URI restriction
        if ("cb".equals(provider)) {
            provider = "coinbase";
        }
        
        String code = callbackData.get("code");
        String state = callbackData.get("state");
        String error = callbackData.get("error");
        String errorDescription = callbackData.get("error_description");
        
        log.info("Received OAuth callback POST from provider: {}, state: {}, user: {}", 
                provider, state, principal != null ? principal.getName() : "anonymous");
        
        try {
            // Handle OAuth errors
            if (error != null) {
                log.error("OAuth error from {}: {} - {}", provider, error, errorDescription);
                throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                    principal != null ? principal.getName() : "unknown", provider, error);
            }
            
            // Validate required parameters
            if (code == null || code.isEmpty()) {
                throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "code");
            }
            
            if (state == null || state.isEmpty()) {
                throw new StrategizException(ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "state");
            }
            
            // Process the OAuth callback (POST doesn't have request context, use defaults)
            ProviderCallbackResponse response = providerCallbackService.processOAuthCallback(
                provider, code, state, "0.0.0.0", "Frontend POST Callback");

            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing OAuth callback POST for provider: {}", provider, e);
            throw new StrategizException(ServiceProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                principal != null ? principal.getName() : "unknown", provider, e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for the callback service.
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ProviderCallbackController is healthy");
    }

    /**
     * Extract the real client IP address from the request.
     * Handles proxies, load balancers, and CDNs by checking standard headers.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check X-Forwarded-For header (used by most proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, the first one is the client
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header (used by Nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // Check CF-Connecting-IP header (used by Cloudflare)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty() && !"unknown".equalsIgnoreCase(cfConnectingIp)) {
            return cfConnectingIp;
        }

        // Check True-Client-IP header (used by Akamai and some CDNs)
        String trueClientIp = request.getHeader("True-Client-IP");
        if (trueClientIp != null && !trueClientIp.isEmpty() && !"unknown".equalsIgnoreCase(trueClientIp)) {
            return trueClientIp;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}