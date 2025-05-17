package io.strategiz.robinhood.controller;

import io.strategiz.robinhood.service.RobinhoodService;
import io.strategiz.robinhood.service.FirestoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Admin Controller for Robinhood API
 * Handles all admin-specific functionality for the Robinhood dashboard
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/robinhood")
public class RobinhoodAdminController {

    private final RobinhoodService robinhoodService;
    private final FirestoreService firestoreService;

    @Autowired
    public RobinhoodAdminController(RobinhoodService robinhoodService, FirestoreService firestoreService) {
        this.robinhoodService = robinhoodService;
        this.firestoreService = firestoreService;
    }

    /**
     * Get raw account data from Robinhood API
     * This endpoint returns the completely unmodified raw data from Robinhood API
     * @param request Request containing credentials
     * @return Raw account data from Robinhood API
     */
    @PostMapping("/raw-data")
    @CrossOrigin(origins = {"http://localhost:3000", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawAccountData(@RequestBody Map<String, String> request) {
        try {
            log.info("Received request for raw Robinhood data");
            String username = request.get("username");
            String password = request.get("password");
            if (username == null || password == null) {
                log.warn("Missing username or password in request");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Username and password are required"
                ));
            }
            Object data = robinhoodService.getRawAccountData(username, password);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error fetching Robinhood raw data", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint for Robinhood integration
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    /**
     * Get Robinhood API credentials for a given user (admin only)
     * @param userId The user ID (email) whose credentials to fetch
     * @return Map with username and password, or error if not found
     */
    @GetMapping("/api-keys")
    public ResponseEntity<Object> getRobinhoodApiKeys(@RequestParam String userId) {
        try {
            Map<String, String> creds = firestoreService.getRobinhoodCredentials(userId);
            if (creds == null || !creds.containsKey("username") || !creds.containsKey("password")) {
                return ResponseEntity.status(404).body(Map.of(
                    "status", "error",
                    "message", "No Robinhood credentials found for user: " + userId
                ));
            }
            return ResponseEntity.ok(Map.of(
                "username", creds.get("username"),
                "password", creds.get("password")
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}

