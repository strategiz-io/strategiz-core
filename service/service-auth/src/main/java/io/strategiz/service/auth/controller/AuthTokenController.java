package io.strategiz.service.auth.controller;

import io.strategiz.framework.authorization.annotation.AuthUser;
import io.strategiz.framework.authorization.annotation.RequireAuth;
import io.strategiz.framework.authorization.context.AuthenticatedUser;
import io.strategiz.service.auth.service.AuthTokenService;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for one-time authentication tokens.
 * Used for cross-app SSO (token relay pattern).
 *
 * Endpoints:
 * - POST /v1/auth/token/generate - Generate a one-time token (requires auth)
 * - POST /v1/auth/token/exchange - Exchange token for session (no auth required)
 */
@RestController
@RequestMapping("/v1/auth/token")
@Tag(name = "Auth Token", description = "Cross-app SSO token management")
public class AuthTokenController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenController.class);

    private final AuthTokenService authTokenService;
    private final CookieUtil cookieUtil;

    public AuthTokenController(AuthTokenService authTokenService, CookieUtil cookieUtil) {
        this.authTokenService = authTokenService;
        this.cookieUtil = cookieUtil;
    }

    @Override
    protected String getModuleName() {
        return ModuleConstants.AUTH_MODULE;
    }

    /**
     * Generate a one-time auth token for cross-app SSO.
     * Requires authentication.
     *
     * @param user        The authenticated user
     * @param redirectUrl The URL to redirect to after token exchange
     * @param request     HTTP request for IP address
     * @return The generated token and redirect URL
     */
    @PostMapping("/generate")
    @RequireAuth
    @Operation(summary = "Generate one-time auth token", description = "Generate a token for cross-app SSO redirect")
    public ResponseEntity<Map<String, Object>> generateToken(
            @AuthUser AuthenticatedUser user,
            @RequestParam("redirect") String redirectUrl,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        logger.info("Token generation requested for user {} to {}", user.getUserId(), redirectUrl);

        try {
            String token = authTokenService.generateToken(user.getUserId(), redirectUrl, clientIp);

            // Build redirect URL with token
            String separator = redirectUrl.contains("?") ? "&" : "?";
            String fullRedirectUrl = redirectUrl + separator + "auth_token=" + token;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token,
                    "redirectUrl", fullRedirectUrl
            ));
        } catch (Exception e) {
            logger.error("Token generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Exchange a one-time token for a session.
     * Does not require authentication (token is the auth).
     *
     * @param request  HTTP request containing the token
     * @param response HTTP response for setting cookies
     * @return Session info and user details
     */
    @PostMapping("/exchange")
    @Operation(summary = "Exchange auth token for session", description = "Exchange one-time token for session cookies")
    public ResponseEntity<Map<String, Object>> exchangeToken(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = body.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Missing token"
            ));
        }

        String clientIp = getClientIp(request);
        logger.info("Token exchange requested from IP {}", clientIp);

        try {
            AuthTokenService.ExchangeResult result = authTokenService.exchangeToken(token, clientIp);

            // Set session cookies
            cookieUtil.setAccessTokenCookie(response, result.accessToken());
            cookieUtil.setRefreshTokenCookie(response, result.refreshToken());

            logger.info("Token exchange successful for user {}", result.userId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", result.userId(),
                    "email", result.email() != null ? result.email() : "",
                    "name", result.name() != null ? result.name() : "",
                    "role", result.role() != null ? result.role() : "",
                    "accessToken", result.accessToken(),
                    "refreshToken", result.refreshToken()
            ));
        } catch (Exception e) {
            logger.error("Token exchange failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
