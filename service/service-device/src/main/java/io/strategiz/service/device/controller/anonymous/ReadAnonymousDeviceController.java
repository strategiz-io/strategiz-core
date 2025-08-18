package io.strategiz.service.device.controller.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.service.anonymous.ReadAnonymousDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller exclusively for reading/querying anonymous devices
 * Handles all read operations for anonymous devices
 * Single Responsibility: Device retrieval for analytics and monitoring
 */
@RestController
@RequestMapping("/v1/devices/anonymous")
public class ReadAnonymousDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(ReadAnonymousDeviceController.class);
    
    private final ReadAnonymousDeviceService readService;
    
    @Autowired
    public ReadAnonymousDeviceController(ReadAnonymousDeviceService readService) {
        this.readService = readService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Get an anonymous device by ID
     * Endpoint: GET /v1/devices/anonymous/{deviceId}
     * 
     * Used for tracking and analytics purposes
     * 
     * @param deviceId The device ID to retrieve
     * @return The device details if found
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceIdentity> getAnonymousDevice(@PathVariable String deviceId) {
        log.debug("Retrieving anonymous device: {}", deviceId);
        
        try {
            return readService.getAnonymousDevice(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving anonymous device {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Check if an anonymous device exists
     * Endpoint: GET /v1/devices/anonymous/{deviceId}/exists
     * 
     * @param deviceId The device ID to check
     * @return Boolean indicating existence
     */
    @GetMapping("/{deviceId}/exists")
    public ResponseEntity<Map<String, Object>> checkDeviceExists(@PathVariable String deviceId) {
        log.debug("Checking existence of anonymous device: {}", deviceId);
        
        try {
            boolean exists = readService.deviceExists(deviceId);
            return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "exists", exists
            ));
        } catch (Exception e) {
            log.error("Error checking device existence {}: {}", deviceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Find device by visitor ID (FingerprintJS)
     * Endpoint: GET /v1/devices/anonymous/by-visitor/{visitorId}
     * 
     * @param visitorId The FingerprintJS visitor ID
     * @return The matching device if found
     */
    @GetMapping("/by-visitor/{visitorId}")
    public ResponseEntity<DeviceIdentity> findByVisitorId(@PathVariable String visitorId) {
        log.debug("Finding anonymous device by visitor ID: {}", visitorId);
        
        try {
            return readService.findByVisitorId(visitorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error finding device by visitor ID {}: {}", visitorId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all anonymous devices (admin endpoint)
     * Endpoint: GET /v1/devices/anonymous
     * 
     * Should be restricted to admin users in production
     * 
     * @param limit Maximum number of results (default 100)
     * @param offset Offset for pagination (default 0)
     * @return List of anonymous devices
     */
    @GetMapping
    public ResponseEntity<List<DeviceIdentity>> getAllAnonymousDevices(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        log.debug("Retrieving anonymous devices with limit {} and offset {}", limit, offset);
        
        try {
            List<DeviceIdentity> devices = readService.getAllAnonymousDevices(limit, offset);
            log.info("Retrieved {} anonymous devices", devices.size());
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error retrieving anonymous devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get anonymous device statistics
     * Endpoint: GET /v1/devices/anonymous/stats
     * 
     * @return Statistics about anonymous devices
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAnonymousDeviceStats() {
        log.debug("Retrieving anonymous device statistics");
        
        try {
            Map<String, Object> stats = readService.getAnonymousDeviceStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving anonymous device statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search anonymous devices by criteria
     * Endpoint: GET /v1/devices/anonymous/search
     * 
     * @param ipAddress Optional IP address filter
     * @param browserName Optional browser name filter
     * @param osName Optional OS name filter
     * @param trustLevel Optional trust level filter
     * @return Filtered list of devices
     */
    @GetMapping("/search")
    public ResponseEntity<List<DeviceIdentity>> searchAnonymousDevices(
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) String browserName,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) String trustLevel) {
        
        log.debug("Searching anonymous devices with filters: ip={}, browser={}, os={}, trust={}", 
            ipAddress, browserName, osName, trustLevel);
        
        try {
            Map<String, Object> filters = Map.of(
                "ipAddress", ipAddress != null ? ipAddress : "",
                "browserName", browserName != null ? browserName : "",
                "osName", osName != null ? osName : "",
                "trustLevel", trustLevel != null ? trustLevel : ""
            );
            
            List<DeviceIdentity> devices = readService.searchAnonymousDevices(filters);
            log.info("Found {} anonymous devices matching filters", devices.size());
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error searching anonymous devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get suspicious anonymous devices
     * Endpoint: GET /v1/devices/anonymous/suspicious
     * 
     * Returns devices with low trust scores or suspicious indicators
     * 
     * @return List of suspicious devices
     */
    @GetMapping("/suspicious")
    public ResponseEntity<List<DeviceIdentity>> getSuspiciousDevices() {
        log.debug("Retrieving suspicious anonymous devices");
        
        try {
            List<DeviceIdentity> devices = readService.getSuspiciousDevices();
            log.info("Found {} suspicious anonymous devices", devices.size());
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error retrieving suspicious devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get recent anonymous devices
     * Endpoint: GET /v1/devices/anonymous/recent
     * 
     * @param hours Number of hours to look back (default 24)
     * @return List of recent devices
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DeviceIdentity>> getRecentDevices(
            @RequestParam(defaultValue = "24") int hours) {
        
        log.debug("Retrieving anonymous devices from last {} hours", hours);
        
        try {
            List<DeviceIdentity> devices = readService.getRecentDevices(hours);
            log.info("Found {} recent anonymous devices", devices.size());
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error retrieving recent devices: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}