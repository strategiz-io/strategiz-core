package io.strategiz.service.device.controller.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.service.authenticated.ReadAuthenticatedDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller exclusively for reading/querying authenticated devices
 * Handles all read operations for user devices
 * Single Responsibility: Device retrieval for authenticated users
 */
@RestController
@RequestMapping("/v1/users/devices")
public class ReadAuthenticatedDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(ReadAuthenticatedDeviceController.class);
    
    private final ReadAuthenticatedDeviceService readService;
    
    @Autowired
    public ReadAuthenticatedDeviceController(ReadAuthenticatedDeviceService readService) {
        this.readService = readService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Get all devices for the authenticated user
     * Endpoint: GET /v1/users/devices
     * 
     * @return List of user's devices with comprehensive fingerprint data
     */
    @GetMapping
    public ResponseEntity<List<DeviceIdentity>> getAllUserDevices() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Retrieving all devices for user: {}", userId);
        
        try {
            List<DeviceIdentity> devices = readService.getAllUserDevices(userId);
            log.info("Found {} devices for user: {}", devices.size(), userId);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error retrieving devices for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific device by ID
     * Endpoint: GET /v1/users/devices/{deviceId}
     * 
     * @param deviceId The device ID to retrieve
     * @return The device details if found and owned by user
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceIdentity> getDevice(@PathVariable String deviceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Retrieving device {} for user: {}", deviceId, userId);
        
        try {
            return readService.getUserDevice(userId, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving device {} for user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get only trusted devices for the user
     * Endpoint: GET /v1/users/devices/trusted
     * 
     * @return List of trusted devices
     */
    @GetMapping("/trusted")
    public ResponseEntity<List<DeviceIdentity>> getTrustedDevices() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Retrieving trusted devices for user: {}", userId);
        
        try {
            List<DeviceIdentity> devices = readService.getTrustedUserDevices(userId);
            log.info("Found {} trusted devices for user: {}", devices.size(), userId);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error retrieving trusted devices for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get device statistics for the user
     * Endpoint: GET /v1/users/devices/stats
     * 
     * @return Statistics about user's devices
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDeviceStatistics() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Retrieving device statistics for user: {}", userId);
        
        try {
            Map<String, Object> stats = readService.getUserDeviceStatistics(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving device statistics for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Search devices by fingerprint attributes
     * Endpoint: GET /v1/users/devices/search
     * 
     * @param browserName Optional browser name filter
     * @param osName Optional OS name filter
     * @param trusted Optional trusted status filter
     * @return Filtered list of devices
     */
    @GetMapping("/search")
    public ResponseEntity<List<DeviceIdentity>> searchDevices(
            @RequestParam(required = false) String browserName,
            @RequestParam(required = false) String osName,
            @RequestParam(required = false) Boolean trusted) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Searching devices for user {} with filters: browser={}, os={}, trusted={}", 
            userId, browserName, osName, trusted);
        
        try {
            Map<String, Object> filters = new HashMap<>();
            if (browserName != null) filters.put("browserName", browserName);
            if (osName != null) filters.put("osName", osName);
            if (trusted != null) filters.put("trusted", trusted);
            
            List<DeviceIdentity> devices = readService.searchUserDevices(userId, filters);
            log.info("Found {} devices matching filters for user: {}", devices.size(), userId);
            return ResponseEntity.ok(devices);
        } catch (Exception e) {
            log.error("Error searching devices for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get the current device based on fingerprint matching
     * Endpoint: GET /v1/users/devices/current
     * 
     * @param visitorId FingerprintJS visitor ID from client
     * @return The matching device if found
     */
    @GetMapping("/current")
    public ResponseEntity<DeviceIdentity> getCurrentDevice(
            @RequestParam String visitorId) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String userId = auth.getName();
        log.debug("Finding current device for user {} with visitorId: {}", userId, visitorId);
        
        try {
            return readService.findDeviceByVisitorId(userId, visitorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error finding current device for user {}: {}", 
                userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}