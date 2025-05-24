package io.strategiz.api.auth;

import io.strategiz.data.auth.DeviceIdentity;
import io.strategiz.data.auth.Session;
import io.strategiz.service.auth.DeviceIdentityService;
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
 * Controller for handling device identity authentication endpoints
 */
@RestController
@RequestMapping("/auth/device")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081", "https://strategiz.io"})
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
    public ResponseEntity<ApiResponse> createDeviceSession(
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("Creating device session");
            
            // Extract user information from the request
            String userId = (String) request.get("userId");
            String email = (String) request.get("email");
            String deviceId = (String) request.get("deviceId");
            String signature = (String) request.get("signature");
            
            if (userId == null || deviceId == null || signature == null) {
                logger.warn("Missing required fields in request");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing required fields"));
            }
            
            // Verify the device exists and belongs to the user
            Optional<DeviceIdentity> deviceOpt = deviceIdentityService.getDeviceByDeviceIdAndUserId(deviceId, userId);
            
            if (deviceOpt.isEmpty()) {
                logger.warn("Device not found for user: {}, device ID: {}", userId, deviceId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Device not registered for this user"));
            }
            
            DeviceIdentity device = deviceOpt.get();
            
            // TODO: Verify the signature with the device's public key
            // This is a placeholder for actual signature verification
            boolean signatureValid = true;
            
            if (!signatureValid) {
                logger.warn("Invalid signature for device: {}", deviceId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid signature"));
            }
            
            // Create a new session
            Session session = sessionService.createSession(userId);
            
            // Update device last used time
            device.updateLastUsedTime();
            deviceIdentityService.updateDevice(device);
            
            logger.info("Device session created for user: {}", userId);
            
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", session.getId());
            sessionData.put("token", session.getToken());
            sessionData.put("expiresAt", session.getExpiresAt());
            
            return ResponseEntity.ok(ApiResponse.success("Session created", sessionData));
        } catch (Exception e) {
            logger.error("Error creating device session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error creating session: " + e.getMessage()));
        }
    }
    
    /**
     * Registers a new device for a user
     * @param request The request containing userId, deviceId, publicKey, name, and deviceInfo
     * @return A response with the registered device
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerDevice(
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("Registering new device");
            
            // Extract device information from the request
            String userId = (String) request.get("userId");
            String deviceId = (String) request.get("deviceId");
            String publicKey = (String) request.get("publicKey");
            String name = (String) request.get("name");
            Map<String, Object> deviceInfo = (Map<String, Object>) request.get("deviceInfo");
            
            if (userId == null || deviceId == null || publicKey == null) {
                logger.warn("Missing required fields in request");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing required fields"));
            }
            
            // Set default name if not provided
            if (name == null || name.isEmpty()) {
                name = "Unknown Device";
            }
            
            // Set empty device info if not provided
            if (deviceInfo == null) {
                deviceInfo = new HashMap<>();
            }
            
            // Register the device
            DeviceIdentity device = deviceIdentityService.registerDevice(
                    userId, deviceId, publicKey, name, deviceInfo, false);
            
            logger.info("Device registered for user: {}, device ID: {}", userId, deviceId);
            
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("id", device.getId());
            deviceData.put("deviceId", device.getDeviceId());
            deviceData.put("name", device.getName());
            deviceData.put("trusted", device.isTrusted());
            
            return ResponseEntity.ok(ApiResponse.success("Device registered", deviceData));
        } catch (Exception e) {
            logger.error("Error registering device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error registering device: " + e.getMessage()));
        }
    }
    
    /**
     * Gets all devices for a user
     * @param userId The user ID
     * @return A response with the user's devices
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse> getUserDevices(
            @PathVariable String userId) {
        try {
            logger.info("Getting devices for user: {}", userId);
            
            List<DeviceIdentity> devices = deviceIdentityService.getUserDevices(userId);
            
            logger.info("Found {} devices for user: {}", devices.size(), userId);
            
            Map<String, Object> devicesData = new HashMap<>();
            devicesData.put("count", devices.size());
            devicesData.put("devices", devices);
            
            return ResponseEntity.ok(ApiResponse.success("User devices retrieved", devicesData));
        } catch (Exception e) {
            logger.error("Error getting user devices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error getting user devices: " + e.getMessage()));
        }
    }
    
    /**
     * Gets a device by ID
     * @param deviceId The device ID
     * @return A response with the device
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse> getDevice(
            @PathVariable String deviceId) {
        try {
            logger.info("Getting device by ID: {}", deviceId);
            
            Optional<DeviceIdentity> deviceOpt = deviceIdentityService.getDeviceById(deviceId);
            
            if (deviceOpt.isEmpty()) {
                logger.warn("Device not found: {}", deviceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Device not found"));
            }
            
            DeviceIdentity device = deviceOpt.get();
            
            logger.info("Device found: {}", deviceId);
            
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("device", device);
            
            return ResponseEntity.ok(ApiResponse.success("Device retrieved", deviceData));
        } catch (Exception e) {
            logger.error("Error getting device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error getting device: " + e.getMessage()));
        }
    }
    
    /**
     * Updates a device's trusted status
     * @param deviceId The device ID
     * @param request The request containing the trusted status
     * @return A response indicating success or failure
     */
    @PutMapping("/{deviceId}/trust")
    public ResponseEntity<ApiResponse> updateDeviceTrust(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> request) {
        try {
            logger.info("Updating device trust status: {}", deviceId);
            
            Boolean trusted = (Boolean) request.get("trusted");
            
            if (trusted == null) {
                logger.warn("Missing trusted field in request");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing trusted field"));
            }
            
            Optional<DeviceIdentity> deviceOpt = deviceIdentityService.getDeviceById(deviceId);
            
            if (deviceOpt.isEmpty()) {
                logger.warn("Device not found: {}", deviceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Device not found"));
            }
            
            DeviceIdentity device = deviceOpt.get();
            device.setTrusted(trusted);
            
            deviceIdentityService.updateDevice(device);
            
            logger.info("Device trust status updated: {}, trusted: {}", deviceId, trusted);
            
            return ResponseEntity.ok(ApiResponse.success("Device trust status updated"));
        } catch (Exception e) {
            logger.error("Error updating device trust status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error updating device trust status: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a device
     * @param deviceId The device ID
     * @return A response indicating success or failure
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse> deleteDevice(
            @PathVariable String deviceId) {
        try {
            logger.info("Deleting device: {}", deviceId);
            
            boolean deleted = deviceIdentityService.deleteDevice(deviceId);
            
            if (deleted) {
                logger.info("Device deleted: {}", deviceId);
                return ResponseEntity.ok(ApiResponse.success("Device deleted"));
            } else {
                logger.warn("Device not found or could not be deleted: {}", deviceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Device not found or could not be deleted"));
            }
        } catch (Exception e) {
            logger.error("Error deleting device", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error deleting device: " + e.getMessage()));
        }
    }
}
