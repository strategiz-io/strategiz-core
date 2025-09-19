package io.strategiz.service.dashboard.controller;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.dashboard.service.SignInPortfolioSyncService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling portfolio synchronization on user sign-in
 * This gets triggered immediately after user authentication to sync all provider data
 */
@RestController
@RequestMapping("/v1/dashboard/sync")
@CrossOrigin(origins = "*")
public class SignInPortfolioSyncController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(SignInPortfolioSyncController.class);
    private static final String MODULE_NAME = "service-dashboard";

    @Autowired
    private SignInPortfolioSyncService syncService;

    @Autowired
    private PasetoTokenProvider tokenProvider;

    @Override
    protected String getModuleName() {
        return ModuleConstants.DASHBOARD_MODULE;
    }

    /**
     * Initialize portfolio sync on user sign-in
     * Creates Firestore collections and triggers data sync for all connected providers
     * 
     * @param request HTTP request containing auth token
     * @return Response with sync status and provider information
     */
    @PostMapping("/on-signin")
    public ResponseEntity<Map<String, Object>> onUserSignIn(HttpServletRequest request) {
        
        // Extract and validate auth token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid or missing authorization header");
            // Note: Should use a proper AUTH error from service-auth module
            // For now, returning simple 401 as auth errors should be in auth module
            return ResponseEntity.status(401).body(Map.of(
                "error", "No valid authentication token",
                "timestamp", Instant.now().toString()
            ));
        }
        
        String token = authHeader.substring(7);
        String userId = null;
        
        try {
            // Validate token and extract user information
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");
            Boolean demoMode = (Boolean) claims.get("demoMode");
            
            log.info("Initiating portfolio sync for user: {}, demoMode: {}", userId, demoMode);
            
            // Call service to perform sync
            Map<String, Object> syncResult = syncService.performInitialSync(userId, demoMode);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("userId", userId);
            response.put("demoMode", demoMode);
            response.put("syncResult", syncResult);
            response.put("timestamp", Instant.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (StrategizException e) {
            // Business exceptions are already properly formatted
            log.error("Business error during sign-in portfolio sync for user: {}", userId, e);
            throw e; // Re-throw to be handled by global exception handler
            
        } catch (Exception e) {
            log.error("Unexpected error during sign-in portfolio sync for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.SYNC_INITIALIZATION_FAILED,
                MODULE_NAME,
                userId != null ? userId : "unknown",
                e.getMessage()
            );
        }
    }

    /**
     * Manual sync endpoint to refresh portfolio data
     * 
     * @param request HTTP request containing auth token
     * @return Response with sync status
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshPortfolio(HttpServletRequest request) {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "No valid authentication token"
            ));
        }
        
        String token = authHeader.substring(7);
        String userId = null;
        
        try {
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");
            Boolean demoMode = (Boolean) claims.get("demoMode");
            
            log.info("Manual portfolio refresh requested for user: {}", userId);
            
            // Skip sync if in demo mode
            if (Boolean.TRUE.equals(demoMode)) {
                return ResponseEntity.ok(Map.of(
                    "status", "skipped",
                    "reason", "Demo mode active",
                    "timestamp", Instant.now().toString()
                ));
            }
            
            Map<String, Object> syncResult = syncService.refreshPortfolio(userId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "syncResult", syncResult,
                "timestamp", Instant.now().toString()
            ));
            
        } catch (StrategizException e) {
            log.error("Business error during portfolio refresh for user: {}", userId, e);
            throw e; // Re-throw to be handled by global exception handler
            
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
     * Get sync status for the user
     * 
     * @param request HTTP request containing auth token
     * @return Current sync status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus(HttpServletRequest request) {
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "No valid authentication token"
            ));
        }
        
        String token = authHeader.substring(7);
        String userId = null;
        
        try {
            Map<String, Object> claims = tokenProvider.parseToken(token);
            userId = (String) claims.get("sub");
            
            Map<String, Object> status = syncService.getSyncStatus(userId);
            
            return ResponseEntity.ok(status);
            
        } catch (StrategizException e) {
            log.error("Business error getting sync status for user: {}", userId, e);
            throw e; // Re-throw to be handled by global exception handler
            
        } catch (Exception e) {
            log.error("Unexpected error getting sync status for user: {}", userId, e);
            throw new StrategizException(
                ServiceDashboardErrorDetails.DASHBOARD_ERROR,
                MODULE_NAME,
                "get-sync-status",
                e.getMessage()
            );
        }
    }
}