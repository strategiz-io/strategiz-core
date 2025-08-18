package io.strategiz.service.device.controller.anonymous;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.service.anonymous.DeleteAnonymousDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Controller exclusively for deleting anonymous devices
 * Handles all delete operations for anonymous devices
 * Single Responsibility: Device deletion for anonymous visitors/cleanup
 */
@RestController
@RequestMapping("/v1/devices/anonymous")
public class DeleteAnonymousDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteAnonymousDeviceController.class);
    
    private final DeleteAnonymousDeviceService deleteService;
    
    @Autowired
    public DeleteAnonymousDeviceController(DeleteAnonymousDeviceService deleteService) {
        this.deleteService = deleteService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Delete a specific anonymous device
     * Endpoint: DELETE /v1/devices/anonymous/{deviceId}
     * 
     * @param deviceId The device ID to delete
     * @return Success response with deletion details
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable String deviceId) {
        log.info("Deleting anonymous device: {}", deviceId);
        
        try {
            boolean deleted = deleteService.deleteDevice(deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("deviceId", deviceId);
            response.put("message", deleted ? "Device deleted successfully" : "Device not found");
            
            if (deleted) {
                log.info("Successfully deleted anonymous device: {}", deviceId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Anonymous device not found: {}", deviceId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting anonymous device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete multiple anonymous devices by IDs
     * Endpoint: DELETE /v1/devices/anonymous/batch
     * 
     * @param deviceIds List of device IDs to delete
     * @return Success response with deletion details
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteMultipleDevices(
            @RequestBody List<String> deviceIds) {
        
        log.info("Batch deleting {} anonymous devices", deviceIds.size());
        
        try {
            Map<String, Boolean> results = deleteService.deleteMultipleDevices(deviceIds);
            
            long successCount = results.values().stream().filter(Boolean::booleanValue).count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRequested", deviceIds.size());
            response.put("deletedCount", successCount);
            response.put("results", results);
            response.put("message", String.format("Deleted %d of %d device(s)", 
                successCount, deviceIds.size()));
            
            log.info("Deleted {} of {} anonymous devices", successCount, deviceIds.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error batch deleting anonymous devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete old/inactive anonymous devices
     * Endpoint: DELETE /v1/devices/anonymous/inactive
     * 
     * Used for cleanup of stale anonymous devices
     * 
     * @param daysInactive Number of days of inactivity (default 30)
     * @return Success response with deletion count
     */
    @DeleteMapping("/inactive")
    public ResponseEntity<Map<String, Object>> deleteInactiveDevices(
            @RequestParam(defaultValue = "30") int daysInactive) {
        
        log.info("Deleting anonymous devices inactive for {} days", daysInactive);
        
        try {
            int deletedCount = deleteService.deleteInactiveDevices(daysInactive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("daysInactive", daysInactive);
            response.put("message", String.format("Deleted %d inactive anonymous device(s)", deletedCount));
            
            log.info("Deleted {} inactive anonymous devices", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting inactive anonymous devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete suspicious anonymous devices
     * Endpoint: DELETE /v1/devices/anonymous/suspicious
     * 
     * Removes devices marked as suspicious or with low trust scores
     * 
     * @param trustThreshold Minimum trust score (devices below this are deleted)
     * @return Success response with deletion count
     */
    @DeleteMapping("/suspicious")
    public ResponseEntity<Map<String, Object>> deleteSuspiciousDevices(
            @RequestParam(defaultValue = "30") int trustThreshold) {
        
        log.info("Deleting suspicious anonymous devices with trust score below {}", trustThreshold);
        
        try {
            int deletedCount = deleteService.deleteSuspiciousDevices(trustThreshold);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("trustThreshold", trustThreshold);
            response.put("message", String.format("Deleted %d suspicious device(s)", deletedCount));
            
            log.info("Deleted {} suspicious anonymous devices", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting suspicious devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete blocked anonymous devices
     * Endpoint: DELETE /v1/devices/anonymous/blocked
     * 
     * @return Success response with deletion count
     */
    @DeleteMapping("/blocked")
    public ResponseEntity<Map<String, Object>> deleteBlockedDevices() {
        log.info("Deleting blocked anonymous devices");
        
        try {
            int deletedCount = deleteService.deleteBlockedDevices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", String.format("Deleted %d blocked device(s)", deletedCount));
            
            log.info("Deleted {} blocked anonymous devices", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting blocked devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Purge all anonymous devices (admin only)
     * Endpoint: DELETE /v1/devices/anonymous/purge
     * 
     * Should be heavily restricted in production
     * 
     * @param confirmationCode Required confirmation code to prevent accidental deletion
     * @return Success response with deletion count
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Object>> purgeAllDevices(
            @RequestParam String confirmationCode) {
        
        // Require specific confirmation code
        if (!"PURGE_ANONYMOUS_DEVICES".equals(confirmationCode)) {
            log.warn("Invalid confirmation code for purge operation");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid confirmation code"
            ));
        }
        
        log.warn("PURGING all anonymous devices");
        
        try {
            int deletedCount = deleteService.purgeAllDevices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", String.format("Purged %d anonymous device(s)", deletedCount));
            
            log.warn("PURGED {} anonymous devices", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error purging anonymous devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete devices by IP address
     * Endpoint: DELETE /v1/devices/anonymous/by-ip
     * 
     * @param ipAddress IP address to match
     * @return Success response with deletion count
     */
    @DeleteMapping("/by-ip")
    public ResponseEntity<Map<String, Object>> deleteByIpAddress(
            @RequestParam String ipAddress) {
        
        log.info("Deleting anonymous devices with IP: {}", ipAddress);
        
        try {
            int deletedCount = deleteService.deleteByIpAddress(ipAddress);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("ipAddress", ipAddress);
            response.put("message", String.format("Deleted %d device(s) with IP %s", 
                deletedCount, ipAddress));
            
            log.info("Deleted {} anonymous devices with IP {}", deletedCount, ipAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting devices by IP {}: {}", ipAddress, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}