package io.strategiz.service.device.controller.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.model.DeviceRequest;
import io.strategiz.service.device.service.anonymous.UpdateAnonymousDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Controller exclusively for updating anonymous devices
 * Handles all update operations for anonymous devices
 * Single Responsibility: Device updates for anonymous visitors
 */
@RestController
@RequestMapping("/v1/devices/anonymous")
public class UpdateAnonymousDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(UpdateAnonymousDeviceController.class);
    
    private final UpdateAnonymousDeviceService updateService;
    
    @Autowired
    public UpdateAnonymousDeviceController(UpdateAnonymousDeviceService updateService) {
        this.updateService = updateService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Update anonymous device fingerprint
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/fingerprint
     * 
     * Updates the device with new comprehensive fingerprint data
     * 
     * @param deviceId The device ID to update
     * @param request HTTP request for server-side data
     * @param deviceRequest Updated fingerprint data
     * @return Updated device
     */
    @PutMapping("/{deviceId}/fingerprint")
    public ResponseEntity<DeviceIdentity> updateFingerprint(
            @PathVariable String deviceId,
            HttpServletRequest request,
            @RequestBody DeviceRequest deviceRequest) {
        
        log.info("Updating fingerprint for anonymous device: {}", deviceId);
        
        try {
            // Extract server-side information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            return updateService.updateDeviceFingerprint(
                    deviceId, deviceRequest, ipAddress, userAgent)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating fingerprint for device {}: {}", 
                deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device trust level based on risk assessment
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/trust-level
     * 
     * @param deviceId The device ID to update
     * @param trustLevel The trust level (high, medium, low, blocked)
     * @return Updated device
     */
    @PutMapping("/{deviceId}/trust-level")
    public ResponseEntity<DeviceIdentity> updateTrustLevel(
            @PathVariable String deviceId,
            @RequestParam String trustLevel) {
        
        log.info("Updating trust level to '{}' for anonymous device: {}", trustLevel, deviceId);
        
        try {
            return updateService.updateTrustLevel(deviceId, trustLevel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating trust level for device {}: {}", 
                deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Mark device as suspicious
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/suspicious
     * 
     * @param deviceId The device ID to mark
     * @param reason Reason for marking as suspicious
     * @return Updated device
     */
    @PutMapping("/{deviceId}/suspicious")
    public ResponseEntity<DeviceIdentity> markSuspicious(
            @PathVariable String deviceId,
            @RequestParam String reason) {
        
        log.warn("Marking anonymous device {} as suspicious: {}", deviceId, reason);
        
        try {
            return updateService.markAsSuspicious(deviceId, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error marking device {} as suspicious: {}", 
                deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device last seen timestamp
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/touch
     * 
     * @param deviceId The device ID to update
     * @return Updated device
     */
    @PutMapping("/{deviceId}/touch")
    public ResponseEntity<DeviceIdentity> touchDevice(@PathVariable String deviceId) {
        log.debug("Updating last seen for anonymous device: {}", deviceId);
        
        try {
            return updateService.updateLastSeen(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating last seen for device {}: {}", 
                deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update device location information
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/location
     * 
     * @param deviceId The device ID to update
     * @param request HTTP request for IP extraction
     * @return Updated device
     */
    @PutMapping("/{deviceId}/location")
    public ResponseEntity<DeviceIdentity> updateLocation(
            @PathVariable String deviceId,
            HttpServletRequest request) {
        
        log.info("Updating location for anonymous device: {}", deviceId);
        
        try {
            String ipAddress = extractClientIpAddress(request);
            
            return updateService.updateLocation(deviceId, ipAddress)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error updating location for device {}: {}", 
                deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Block an anonymous device
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/block
     * 
     * @param deviceId The device ID to block
     * @param reason Reason for blocking
     * @return Updated device
     */
    @PutMapping("/{deviceId}/block")
    public ResponseEntity<DeviceIdentity> blockDevice(
            @PathVariable String deviceId,
            @RequestParam String reason) {
        
        log.warn("Blocking anonymous device {}: {}", deviceId, reason);
        
        try {
            return updateService.blockDevice(deviceId, reason)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error blocking device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Unblock an anonymous device
     * Endpoint: PUT /v1/devices/anonymous/{deviceId}/unblock
     * 
     * @param deviceId The device ID to unblock
     * @return Updated device
     */
    @PutMapping("/{deviceId}/unblock")
    public ResponseEntity<DeviceIdentity> unblockDevice(@PathVariable String deviceId) {
        log.info("Unblocking anonymous device: {}", deviceId);
        
        try {
            return updateService.unblockDevice(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error unblocking device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Batch update multiple device attributes
     * Endpoint: PATCH /v1/devices/anonymous/{deviceId}
     * 
     * @param deviceId The device ID to update
     * @param updates Map of field names to new values
     * @return Updated device
     */
    @PatchMapping("/{deviceId}")
    public ResponseEntity<DeviceIdentity> patchDevice(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> updates) {
        
        log.info("Patching anonymous device {} with {} updates", deviceId, updates.size());
        
        try {
            return updateService.patchDevice(deviceId, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error patching device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Extract client IP address from request
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR",
            "CF-Connecting-IP", "True-Client-IP"
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