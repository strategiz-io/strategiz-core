package io.strategiz.service.device.controller.authenticated;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.service.authenticated.DeleteAuthenticatedDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Controller exclusively for deleting authenticated devices
 * Handles all delete operations for user devices
 * Single Responsibility: Device deletion for authenticated users
 */
@RestController
@RequestMapping("/v1/users/devices")
public class DeleteAuthenticatedDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteAuthenticatedDeviceController.class);
    
    private final DeleteAuthenticatedDeviceService deleteService;
    
    @Autowired
    public DeleteAuthenticatedDeviceController(DeleteAuthenticatedDeviceService deleteService) {
        this.deleteService = deleteService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Delete a specific device
     * Endpoint: DELETE /v1/users/devices/{deviceId}
     * 
     * @param deviceId The device ID to delete
     * @return Success response with deletion details
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable String deviceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Deleting device {} for user {}", deviceId, userId);
        
        try {
            boolean deleted = deleteService.deleteDevice(userId, deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("deviceId", deviceId);
            response.put("message", deleted ? "Device deleted successfully" : "Device not found");
            
            if (deleted) {
                log.info("Successfully deleted device {} for user {}", deviceId, userId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Device {} not found for user {}", deviceId, userId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting device {} for user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete all devices for the authenticated user
     * Endpoint: DELETE /v1/users/devices
     * 
     * @param keepCurrent Optional parameter to keep the current device
     * @return Success response with deletion count
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllDevices(
            @RequestParam(defaultValue = "false") boolean keepCurrent,
            @RequestParam(required = false) String currentVisitorId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Deleting all devices for user {} (keepCurrent: {})", userId, keepCurrent);
        
        try {
            int deletedCount;
            if (keepCurrent && currentVisitorId != null) {
                deletedCount = deleteService.deleteAllExceptCurrent(userId, currentVisitorId);
            } else {
                deletedCount = deleteService.deleteAllDevices(userId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", String.format("Deleted %d device(s)", deletedCount));
            
            log.info("Deleted {} devices for user {}", deletedCount, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting all devices for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete multiple devices by IDs
     * Endpoint: DELETE /v1/users/devices/batch
     * 
     * @param deviceIds List of device IDs to delete
     * @return Success response with deletion details
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteMultipleDevices(
            @RequestBody List<String> deviceIds) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Batch deleting {} devices for user {}", deviceIds.size(), userId);
        
        try {
            Map<String, Boolean> results = deleteService.deleteMultipleDevices(userId, deviceIds);
            
            long successCount = results.values().stream().filter(Boolean::booleanValue).count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRequested", deviceIds.size());
            response.put("deletedCount", successCount);
            response.put("results", results);
            response.put("message", String.format("Deleted %d of %d device(s)", 
                successCount, deviceIds.size()));
            
            log.info("Deleted {} of {} devices for user {}", 
                successCount, deviceIds.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error batch deleting devices for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete untrusted devices
     * Endpoint: DELETE /v1/users/devices/untrusted
     * 
     * @return Success response with deletion count
     */
    @DeleteMapping("/untrusted")
    public ResponseEntity<Map<String, Object>> deleteUntrustedDevices() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Deleting untrusted devices for user {}", userId);
        
        try {
            int deletedCount = deleteService.deleteUntrustedDevices(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("message", String.format("Deleted %d untrusted device(s)", deletedCount));
            
            log.info("Deleted {} untrusted devices for user {}", deletedCount, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting untrusted devices for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete old/inactive devices
     * Endpoint: DELETE /v1/users/devices/inactive
     * 
     * @param daysInactive Number of days of inactivity (default 90)
     * @return Success response with deletion count
     */
    @DeleteMapping("/inactive")
    public ResponseEntity<Map<String, Object>> deleteInactiveDevices(
            @RequestParam(defaultValue = "90") int daysInactive) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.info("Deleting devices inactive for {} days for user {}", daysInactive, userId);
        
        try {
            int deletedCount = deleteService.deleteInactiveDevices(userId, daysInactive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deletedCount", deletedCount);
            response.put("daysInactive", daysInactive);
            response.put("message", String.format("Deleted %d inactive device(s)", deletedCount));
            
            log.info("Deleted {} inactive devices for user {}", deletedCount, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting inactive devices for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}