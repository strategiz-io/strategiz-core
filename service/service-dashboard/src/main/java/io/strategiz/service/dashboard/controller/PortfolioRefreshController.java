package io.strategiz.service.dashboard.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.dashboard.service.PortfolioRefreshService;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.business.tokenauth.PasetoTokenProvider;
import io.strategiz.framework.exception.StrategizException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

/**
 * Controller for refreshing portfolio data from connected exchange/brokerage providers.
 *
 * Endpoints:
 * - POST /v1/portfolio/refresh/on-signin - Refresh after user authentication
 * - POST /v1/portfolio/refresh - Manual refresh (button click or auto-refresh)
 * - GET /v1/portfolio/refresh/status - Get last refresh times
 *
 * Also maintains backward compatibility with old endpoints:
 * - POST /v1/dashboard/sync/on-signin
 * - POST /v1/dashboard/sync/refresh
 * - GET /v1/dashboard/sync/status
 */
@RestController
@CrossOrigin(origins = "*")
public class PortfolioRefreshController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRefreshController.class);
    private static final String MODULE_NAME = "service-dashboard";

    @Autowired
    private PortfolioRefreshService refreshService;

    @Autowired
    private PasetoTokenProvider tokenProvider;

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Refresh portfolio on user sign-in.
     * Called immediately after authentication to load initial data.
     *
     * New endpoint: POST /v1/portfolio/refresh/on-signin
     * Legacy endpoint: POST /v1/dashboard/sync/on-signin (backward compatible)
     */
    @PostMapping({"/v1/portfolio/refresh/on-signin", "/v1/dashboard/sync/on-signin"})
    public ResponseEntity<Map<String, Object>> onUserSignIn(HttpServletRequest request) {
        String userId = null;

        try {
            // Extract and validate auth token
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid or missing authorization header");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "No valid authentication token",
                    "timestamp", Instant.now().toString()
                ));
            }

            String token = authHeader.substring(7);
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");
            Boolean demoMode = (Boolean) claims.get("demoMode");

            log.info("Portfolio refresh on sign-in for user: {}, demoMode: {}", userId, demoMode);

            Map<String, Object> result = refreshService.refreshOnSignIn(userId, demoMode);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "userId", userId,
                "demoMode", demoMode != null ? demoMode : false,
                "refreshResult", result,
                "timestamp", Instant.now().toString()
            ));

        } catch (StrategizException e) {
            log.error("Business error during sign-in portfolio refresh for user: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during sign-in portfolio refresh for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_INITIALIZATION_FAILED,
                MODULE_NAME,
                userId != null ? userId : "unknown",
                e.getMessage()
            );
        }
    }

    /**
     * Manual portfolio refresh.
     * Called when user clicks refresh button or auto-refresh interval triggers.
     *
     * New endpoint: POST /v1/portfolio/refresh
     * Legacy endpoint: POST /v1/dashboard/sync/refresh (backward compatible)
     */
    @PostMapping({"/v1/portfolio/refresh", "/v1/dashboard/sync/refresh"})
    public ResponseEntity<Map<String, Object>> refreshPortfolio(HttpServletRequest request) {
        String userId = null;

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "No valid authentication token"
                ));
            }

            String token = authHeader.substring(7);
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");
            Boolean demoMode = (Boolean) claims.get("demoMode");

            log.info("Manual portfolio refresh requested for user: {}", userId);

            // Skip refresh if in demo mode
            if (Boolean.TRUE.equals(demoMode)) {
                return ResponseEntity.ok(Map.of(
                    "status", "skipped",
                    "reason", "Demo mode active",
                    "timestamp", Instant.now().toString()
                ));
            }

            Map<String, Object> result = refreshService.refreshPortfolio(userId);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "refreshResult", result,
                "timestamp", Instant.now().toString()
            ));

        } catch (StrategizException e) {
            log.error("Business error during portfolio refresh for user: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during portfolio refresh for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                MODULE_NAME,
                "refresh",
                e.getMessage()
            );
        }
    }

    /**
     * Get refresh status for user.
     *
     * New endpoint: GET /v1/portfolio/refresh/status
     * Legacy endpoint: GET /v1/dashboard/sync/status (backward compatible)
     */
    @GetMapping({"/v1/portfolio/refresh/status", "/v1/dashboard/sync/status"})
    public ResponseEntity<Map<String, Object>> getRefreshStatus(HttpServletRequest request) {
        String userId = null;

        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "No valid authentication token"
                ));
            }

            String token = authHeader.substring(7);
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");

            Map<String, Object> status = refreshService.getRefreshStatus(userId);

            return ResponseEntity.ok(status);

        } catch (StrategizException e) {
            log.error("Business error getting refresh status for user: {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting refresh status for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                MODULE_NAME,
                "get-refresh-status",
                e.getMessage()
            );
        }
    }
}
