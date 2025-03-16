package io.strategiz.auth.controller;

import io.strategiz.auth.model.PasskeyCredential;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.service.PasskeyService;
import io.strategiz.auth.service.SessionService;
import io.strategiz.common.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling passkey authentication endpoints
 */
@RestController
@RequestMapping("/api/auth/passkey")
public class PasskeyController {
    private static final Logger logger = LoggerFactory.getLogger(PasskeyController.class);

    private final PasskeyService passkeyService;
    private final SessionService sessionService;

    @Autowired
    public PasskeyController(PasskeyService passkeyService, SessionService sessionService) {
        this.passkeyService = passkeyService;
        this.sessionService = sessionService;
    }

    /**
     * Creates a session using passkey authentication
     * @param requestBody The request body containing user ID, email, timestamp, and credential ID
     * @return The session ID
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPasskeySession(@RequestBody Map<String, String> requestBody) {
        try {
            String userId = requestBody.get("userId");
            String email = requestBody.get("email");
            String timestamp = requestBody.get("timestamp");
            String credentialId = requestBody.get("credentialId");

            if (userId == null || email == null || timestamp == null || credentialId == null) {
                logger.error("Missing required fields for passkey session creation");
                return ResponseEntity.badRequest().body(
                        ApiResponse.<Map<String, String>>builder()
                                .success(false)
                                .message("Missing required fields")
                                .build()
                );
            }

            String sessionId = passkeyService.createPasskeySession(userId, email, timestamp, credentialId);

            Map<String, String> response = Map.of("sessionId", sessionId);
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>builder()
                            .success(true)
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error creating passkey session", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Error creating passkey session: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Validates a passkey session
     * @param sessionId The session ID to validate
     * @return The user ID if the session is valid
     */
    @GetMapping("/session/{sessionId}/validate")
    public ResponseEntity<ApiResponse<Map<String, String>>> validatePasskeySession(@PathVariable String sessionId) {
        try {
            Optional<Session> sessionOpt = sessionService.validateSession(sessionId);
            
            if (sessionOpt.isEmpty()) {
                logger.warn("Invalid or expired passkey session: {}", sessionId);
                return ResponseEntity.ok(
                        ApiResponse.<Map<String, String>>builder()
                                .success(false)
                                .message("Invalid or expired session")
                                .build()
                );
            }
            
            Session session = sessionOpt.get();
            Map<String, String> response = Map.of("userId", session.getUserId());
            
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>builder()
                            .success(true)
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error validating passkey session", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Error validating passkey session: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Refreshes a passkey session
     * @param sessionId The session ID to refresh
     * @return Success or failure
     */
    @PostMapping("/session/{sessionId}/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshPasskeySession(@PathVariable String sessionId) {
        try {
            Session refreshedSession = sessionService.refreshSession(sessionId);
            
            if (refreshedSession == null) {
                logger.warn("Failed to refresh passkey session: {}", sessionId);
                return ResponseEntity.ok(
                        ApiResponse.<Map<String, String>>builder()
                                .success(false)
                                .message("Failed to refresh session")
                                .build()
                );
            }
            
            Map<String, String> response = Map.of(
                    "sessionId", refreshedSession.getId(),
                    "userId", refreshedSession.getUserId()
            );
            
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, String>>builder()
                            .success(true)
                            .data(response)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error refreshing passkey session", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Error refreshing passkey session: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Gets all passkey credentials for a user
     * @param userId The user ID
     * @return The list of passkey credentials
     */
    @GetMapping("/credentials/{userId}")
    public ResponseEntity<ApiResponse<List<PasskeyCredential>>> getPasskeyCredentials(@PathVariable String userId) {
        try {
            List<PasskeyCredential> credentials = passkeyService.getPasskeyCredentials(userId);
            
            return ResponseEntity.ok(
                    ApiResponse.<List<PasskeyCredential>>builder()
                            .success(true)
                            .data(credentials)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error getting passkey credentials", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<List<PasskeyCredential>>builder()
                            .success(false)
                            .message("Error getting passkey credentials: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Registers a new passkey credential
     * @param requestBody The request body containing credential details
     * @return The registered credential
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PasskeyCredential>> registerPasskey(@RequestBody Map<String, Object> requestBody) {
        try {
            String userId = (String) requestBody.get("userId");
            String credentialId = (String) requestBody.get("credentialId");
            String publicKey = (String) requestBody.get("publicKey");
            String attestationObject = (String) requestBody.get("attestationObject");
            String clientDataJSON = (String) requestBody.get("clientDataJSON");
            String userAgent = (String) requestBody.get("userAgent");
            String deviceName = (String) requestBody.get("deviceName");
            
            if (userId == null || credentialId == null || publicKey == null) {
                logger.error("Missing required fields for passkey registration");
                return ResponseEntity.badRequest().body(
                        ApiResponse.<PasskeyCredential>builder()
                                .success(false)
                                .message("Missing required fields")
                                .build()
                );
            }
            
            PasskeyCredential credential = passkeyService.registerPasskey(
                    userId, credentialId, publicKey, attestationObject, 
                    clientDataJSON, userAgent, deviceName);
            
            return ResponseEntity.ok(
                    ApiResponse.<PasskeyCredential>builder()
                            .success(true)
                            .data(credential)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error registering passkey", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<PasskeyCredential>builder()
                            .success(false)
                            .message("Error registering passkey: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Deletes a passkey credential
     * @param id The credential ID
     * @return Success or failure
     */
    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePasskeyCredential(@PathVariable String id) {
        try {
            passkeyService.deletePasskeyCredential(id);
            
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Passkey credential deleted successfully")
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error deleting passkey credential", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Error deleting passkey credential: " + e.getMessage())
                            .build()
            );
        }
    }
}
