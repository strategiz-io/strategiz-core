package io.strategiz.auth.controller;

import io.strategiz.auth.model.DeviceIdentity;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.service.DeviceIdentityService;
import io.strategiz.auth.service.SessionService;
import io.strategiz.common.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling device identity authentication endpoints
 */
@RestController
@RequestMapping("/auth/device")
public class DeviceIdentityController {
    private static final Logger logger = LoggerFactory.getLogger(DeviceIdentityController.class);

    private final DeviceIdentityService deviceIdentityService;
    private final SessionService sessionService;

    @Autowired
    public DeviceIdentityController(DeviceIdentityService deviceIdentityService, SessionService sessionService) {
        this.deviceIdentityService = deviceIdentityService;
        this.sessionService = sessionService;
    }

    /**
     * Creates a session for device web crypto authentication
     * @param request The request containing userId, email, deviceId, signature, and deviceInfo
     * @return A response with the session ID
     */
    @PostMapping("/session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDeviceSession(
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("Creating device session");
            
            // Extract user information from the request
            String userId = (String) request.get("userId");
            String email = (String) request.get("email");
            String deviceId = (String) request.get("deviceId");
            String signature = (String) request.get("signature");
            
            // Extract device information for security monitoring
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceInfo = (Map<String, Object>) request.get("deviceInfo");
            
            if (userId == null || deviceId == null || signature == null) {
                logger.warn("Missing required fields in device session request");
                return ResponseEntity.badRequest().body(
                        ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("Missing required fields: userId, deviceId, and signature are required")
                                .build()
                );
            }
            
            // Create a new device session
            String sessionId = deviceIdentityService.createDeviceSession(userId, email, deviceId, signature, deviceInfo);
            
            // Prepare response
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("sessionId", sessionId);
            responseData.put("userId", userId);
            responseData.put("deviceId", deviceId);
            
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .data(responseData)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to create device session", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to create device session: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Validates a device session
     * @param sessionId The session ID to validate
     * @return A response with the session details
     */
    @GetMapping("/validate/{sessionId}")
    public ResponseEntity<ApiResponse<Session>> validateDeviceSession(@PathVariable("sessionId") String sessionId) {
        try {
            logger.info("Validating device session: {}", sessionId);
            Optional<Session> optionalSession = sessionService.validateSession(sessionId);
            
            if (optionalSession.isPresent()) {
                Session session = optionalSession.get();
                return ResponseEntity.ok(
                        ApiResponse.<Session>builder()
                                .success(true)
                                .data(session)
                                .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<Session>builder()
                                .success(false)
                                .message("Invalid device session")
                                .build()
                );
            }
        } catch (Exception e) {
            logger.error("Failed to validate device session: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Session>builder()
                            .success(false)
                            .message("Failed to validate device session: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Refreshes a device session
     * @param sessionId The session ID to refresh
     * @return A response with the refreshed session details
     */
    @PostMapping("/refresh/{sessionId}")
    public ResponseEntity<ApiResponse<Session>> refreshDeviceSession(@PathVariable("sessionId") String sessionId) {
        try {
            logger.info("Refreshing device session: {}", sessionId);
            Session session = sessionService.refreshSession(sessionId);
            
            if (session != null) {
                return ResponseEntity.ok(
                        ApiResponse.<Session>builder()
                                .success(true)
                                .data(session)
                                .build()
                );
            } else {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<Session>builder()
                                .success(false)
                                .message("Invalid device session")
                                .build()
                );
            }
        } catch (Exception e) {
            logger.error("Failed to refresh device session: {}", sessionId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Session>builder()
                            .success(false)
                            .message("Failed to refresh device session: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Registers a new device for a user
     * @param request The request containing device information
     * @return A response indicating success or failure
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceIdentity>> registerDevice(
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("Registering new device");
            
            // Extract user and device information
            String userId = (String) request.get("userId");
            String deviceId = (String) request.get("deviceId");
            String publicKey = (String) request.get("publicKey");
            String name = (String) request.get("name");
            
            // Extract device information for security monitoring
            @SuppressWarnings("unchecked")
            Map<String, Object> deviceInfo = (Map<String, Object>) request.get("deviceInfo");
            
            if (userId == null || deviceId == null || publicKey == null || name == null) {
                logger.warn("Missing required fields in device registration request");
                return ResponseEntity.badRequest().body(
                        ApiResponse.<DeviceIdentity>builder()
                                .success(false)
                                .message("Missing required fields: userId, deviceId, publicKey, and name are required")
                                .build()
                );
            }
            
            // Register a new device
            DeviceIdentity device = deviceIdentityService.registerDevice(
                    userId, deviceId, publicKey, name, deviceInfo);
            
            return ResponseEntity.ok(
                    ApiResponse.<DeviceIdentity>builder()
                            .success(true)
                            .data(device)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to register device", e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<DeviceIdentity>builder()
                            .success(false)
                            .message("Failed to register device: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * Lists all registered devices for a user
     * @param userId The user ID
     * @return A response with the list of devices
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listDevices(@PathVariable String userId) {
        try {
            logger.info("Listing devices for user: {}", userId);
            
            // Get all registered devices for the user
            List<DeviceIdentity> devices = deviceIdentityService.getDevices(userId);
            
            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .data(Map.of("devices", devices))
                            .build()
            );
        } catch (Exception e) {
            logger.error("Failed to list devices for user: {}", userId, e);
            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Failed to list devices: " + e.getMessage())
                            .build()
            );
        }
    }
}
