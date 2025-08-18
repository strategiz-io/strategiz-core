package io.strategiz.service.device.controller.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.model.DeviceRequest;
import io.strategiz.service.device.service.authenticated.UpdateAuthenticatedDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller exclusively for updating authenticated devices
 * Handles all update operations for user devices
 * Single Responsibility: Device updates for authenticated users
 */
@RestController
@RequestMapping("/v1/users/devices")
public class UpdateAuthenticatedDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(UpdateAuthenticatedDeviceController.class);
    
    private final UpdateAuthenticatedDeviceService updateService;
    
    @Autowired
    public UpdateAuthenticatedDeviceController(UpdateAuthenticatedDeviceService updateService) {
        this.updateService = updateService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Update device trusted status
     * Endpoint: PUT /v1/users/devices/{deviceId}/trust
     * 
     * @param deviceId The device ID to update
     * @param trusted The trusted status to set
     * @return Updated device
     */
    @PutMapping("/{deviceId}/trust")
    public ResponseEntity<DeviceIdentity> updateDeviceTrust(
            @PathVariable String deviceId,
            @RequestParam boolean trusted) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Updating trust status to {} for device {} by user {}", 
            trusted, deviceId, userId);
        
        try {
            return updateService.updateDeviceTrust(userId, deviceId, trusted)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating trust for device {} by user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device name
     * Endpoint: PUT /v1/users/devices/{deviceId}/name
     * 
     * @param deviceId The device ID to update
     * @param deviceName The new device name
     * @return Updated device
     */
    @PutMapping("/{deviceId}/name")
    public ResponseEntity<DeviceIdentity> updateDeviceName(
            @PathVariable String deviceId,
            @RequestParam String deviceName) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Updating device name to '{}' for device {} by user {}", 
            deviceName, deviceId, userId);
        
        try {
            return updateService.updateDeviceName(userId, deviceId, deviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating name for device {} by user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device fingerprint data (refresh comprehensive fingerprint)
     * Endpoint: PUT /v1/users/devices/{deviceId}/fingerprint
     * 
     * @param deviceId The device ID to update
     * @param request HTTP request for server-side data
     * @param deviceRequest Updated comprehensive fingerprint data
     * @return Updated device
     */
    @PutMapping("/{deviceId}/fingerprint")
    public ResponseEntity<DeviceIdentity> updateDeviceFingerprint(
            @PathVariable String deviceId,
            HttpServletRequest request,
            @RequestBody DeviceRequest deviceRequest) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Updating fingerprint for device {} by user {}", deviceId, userId);
        
        try {
            // Extract server-side information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            return updateService.updateDeviceFingerprint(
                    userId, deviceId, deviceRequest, ipAddress, userAgent)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating fingerprint for device {} by user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device trust level based on risk assessment
     * Endpoint: PUT /v1/users/devices/{deviceId}/trust-level
     * 
     * @param deviceId The device ID to update
     * @param trustLevel The trust level (high, medium, low, blocked)
     * @return Updated device
     */
    @PutMapping("/{deviceId}/trust-level")
    public ResponseEntity<DeviceIdentity> updateDeviceTrustLevel(
            @PathVariable String deviceId,
            @RequestParam String trustLevel) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Updating trust level to '{}' for device {} by user {}", 
            trustLevel, deviceId, userId);
        
        try {
            return updateService.updateDeviceTrustLevel(userId, deviceId, trustLevel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating trust level for device {} by user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Batch update multiple device attributes
     * Endpoint: PATCH /v1/users/devices/{deviceId}
     * 
     * @param deviceId The device ID to update
     * @param updates Map of field names to new values
     * @return Updated device
     */
    @PatchMapping("/{deviceId}")
    public ResponseEntity<DeviceIdentity> patchDevice(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> updates) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Patching device {} with {} updates by user {}", 
            deviceId, updates.size(), userId);
        
        try {
            return updateService.patchDevice(userId, deviceId, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error patching device {} by user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Mark device as verified (e.g., after 2FA verification)
     * Endpoint: PUT /v1/users/devices/{deviceId}/verify
     * 
     * @param deviceId The device ID to verify
     * @return Updated device
     */
    @PutMapping("/{deviceId}/verify")
    public ResponseEntity<DeviceIdentity> verifyDevice(@PathVariable String deviceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Verifying device {} for user {}", deviceId, userId);
        
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("trusted", true);
            updates.put("trustLevel", "high");
            
            return updateService.patchDevice(userId, deviceId, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error verifying device {} for user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Extract client IP address from request
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
}