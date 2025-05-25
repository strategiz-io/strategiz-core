package io.strategiz.api.auth;

import io.strategiz.data.auth.PasskeyCredential;
import io.strategiz.data.auth.Session;
import io.strategiz.service.auth.PasskeyService;
import io.strategiz.service.auth.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling passkey authentication endpoints
 */
@RestController
@RequestMapping("/auth/passkey")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080", "https://strategiz.io"})
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
    public ResponseEntity<ApiResponse> createPasskeySession(@RequestBody Map<String, String> requestBody) {
        try {
            String userId = requestBody.get("userId");
            String email = requestBody.get("email");
            String timestamp = requestBody.get("timestamp");
            String credentialId = requestBody.get("credentialId");
            String signature = requestBody.get("signature");
            String authenticatorData = requestBody.get("authenticatorData");
            String clientDataJSON = requestBody.get("clientDataJSON");

            if (userId == null || credentialId == null || signature == null) {
                logger.error("Missing required fields for passkey session creation");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing required fields for passkey session creation"));
            }

            // Verify the passkey assertion
            boolean verified = passkeyService.verifyAssertion(
                    credentialId, userId, signature, authenticatorData, clientDataJSON);

            if (!verified) {
                logger.error("Passkey verification failed for user: {}", userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Passkey verification failed"));
            }

            // Create a new session
            Session session = sessionService.createSession(userId);

            logger.info("Passkey session created for user: {}", userId);

            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", session.getId());
            sessionData.put("token", session.getToken());
            sessionData.put("expiresAt", session.getExpiresAt());

            return ResponseEntity.ok(ApiResponse.success("Session created", sessionData));
        } catch (Exception e) {
            logger.error("Error creating passkey session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating passkey session: " + e.getMessage()));
        }
    }

    /**
     * Registers a new passkey credential
     * @param requestBody The request body containing credential data
     * @return Response indicating success or failure
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerPasskeyCredential(@RequestBody Map<String, Object> requestBody) {
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
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing required fields for passkey registration"));
            }

            // Set default values for optional fields
            if (userAgent == null) userAgent = "Unknown";
            if (deviceName == null) deviceName = "Unknown Device";

            // Register the passkey credential
            PasskeyCredential credential = passkeyService.registerCredential(
                    userId, credentialId, publicKey, attestationObject, clientDataJSON, userAgent, deviceName);

            logger.info("Passkey credential registered for user: {}", userId);

            Map<String, Object> credentialData = new HashMap<>();
            credentialData.put("id", credential.getId());
            credentialData.put("credentialId", credential.getCredentialId());
            credentialData.put("deviceName", credential.getDeviceName());

            return ResponseEntity.ok(ApiResponse.success("Passkey credential registered", credentialData));
        } catch (Exception e) {
            logger.error("Error registering passkey credential", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error registering passkey credential: " + e.getMessage()));
        }
    }

    /**
     * Gets all passkey credentials for a user
     * @param userId The user ID
     * @return A list of the user's passkey credentials
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserPasskeyCredentials(@PathVariable String userId) {
        try {
            logger.info("Getting passkey credentials for user: {}", userId);

            List<PasskeyCredential> credentials = passkeyService.getUserCredentials(userId);

            logger.info("Found {} passkey credentials for user: {}", credentials.size(), userId);

            Map<String, Object> credentialsData = new HashMap<>();
            credentialsData.put("count", credentials.size());
            credentialsData.put("credentials", credentials);

            return ResponseEntity.ok(ApiResponse.success("User passkey credentials retrieved", credentialsData));
        } catch (Exception e) {
            logger.error("Error getting user passkey credentials", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error getting user passkey credentials: " + e.getMessage()));
        }
    }

    /**
     * Gets a passkey credential by ID
     * @param id The passkey credential ID
     * @return The passkey credential
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getPasskeyCredential(@PathVariable String id) {
        try {
            logger.info("Getting passkey credential: {}", id);

            Optional<PasskeyCredential> credentialOpt = passkeyService.getCredentialById(id);

            if (credentialOpt.isEmpty()) {
                logger.warn("Passkey credential not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Passkey credential not found"));
            }

            PasskeyCredential credential = credentialOpt.get();

            logger.info("Passkey credential found: {}", id);

            Map<String, Object> credentialData = new HashMap<>();
            credentialData.put("credential", credential);

            return ResponseEntity.ok(ApiResponse.success("Passkey credential retrieved", credentialData));
        } catch (Exception e) {
            logger.error("Error getting passkey credential", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error getting passkey credential: " + e.getMessage()));
        }
    }

    /**
     * Deletes a passkey credential
     * @param id The passkey credential ID
     * @return Response indicating success or failure
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deletePasskeyCredential(@PathVariable String id) {
        try {
            logger.info("Deleting passkey credential: {}", id);

            boolean deleted = passkeyService.deleteCredential(id);

            if (deleted) {
                logger.info("Passkey credential deleted: {}", id);
                return ResponseEntity.ok(ApiResponse.success("Passkey credential deleted"));
            } else {
                logger.warn("Passkey credential not found or could not be deleted: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Passkey credential not found or could not be deleted"));
            }
        } catch (Exception e) {
            logger.error("Error deleting passkey credential", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting passkey credential: " + e.getMessage()));
        }
    }
}
