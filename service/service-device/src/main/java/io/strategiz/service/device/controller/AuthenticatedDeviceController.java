package io.strategiz.service.device.controller;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.service.device.DeviceIdentityService;
import io.strategiz.service.device.model.CreateDeviceRequest;
import io.strategiz.service.device.model.CreateDeviceResponse;
import io.strategiz.service.device.util.DeviceFingerprintUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for authenticated device operations
 * Used for sign-in and sign-up flows
 */
@RestController
@RequestMapping("/v1/device/authenticated")
public class AuthenticatedDeviceController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthenticatedDeviceController.class);
    
    private final DeviceIdentityService deviceService;
    private final DeviceFingerprintUtil fingerprintUtil;
    
    @Autowired
    public AuthenticatedDeviceController(
            DeviceIdentityService deviceService,
            DeviceFingerprintUtil fingerprintUtil) {
        this.deviceService = deviceService;
        this.fingerprintUtil = fingerprintUtil;
    }
    
    /**
     * Register an authenticated device during sign-in or sign-up
     * This links the device to the authenticated user
     * 
     * @param request HTTP request
     * @param registrationRequest Device registration data
     * @return Response with registered device details
     */
    @PostMapping("/register")
    public ResponseEntity<CreateDeviceResponse> registerAuthenticatedDevice(
            HttpServletRequest request,
            @RequestBody CreateDeviceRequest registrationRequest) {
        
        // Get the authenticated user ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        
        log.info("Registering authenticated device for user: {}", userId);
        
        try {
            // Extract device information from request
            Map<String, Object> deviceInfo = fingerprintUtil.extractDeviceInfo(request);
            
            // Add any additional client-side fingerprinting data
            if (registrationRequest.getDeviceInfo() != null) {
                deviceInfo.putAll(registrationRequest.getDeviceInfo());
            }
            
            // Get user agent
            String userAgent = request.getHeader("User-Agent");
            
            // Custom device name (optional)
            String deviceName = deviceInfo.containsKey("deviceName") ? 
                    (String) deviceInfo.get("deviceName") : 
                    generateDeviceName(deviceInfo);
            
            // Register the device
            DeviceIdentity device = deviceService.registerAuthenticatedDevice(
                    registrationRequest.getDeviceId(),
                    userId,
                    deviceName,
                    deviceInfo,
                    userAgent);
            
            // Prepare response
            CreateDeviceResponse response = new CreateDeviceResponse();
            response.setDeviceId(device.getDeviceId());
            response.setRegistrationTime(device.getFirstSeen());
            response.setSuccess(true);
            
            log.info("Authenticated device registered: {} for user: {}", device.getDeviceId(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to register authenticated device: {}", e.getMessage(), e);
            
            CreateDeviceResponse response = new CreateDeviceResponse();
            response.setSuccess(false);
            response.setError("Failed to register device: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get all devices for the authenticated user
     * 
     * @return List of user devices
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getUserDevices() {
        // Get the authenticated user ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        
        log.info("Retrieving devices for user: {}", userId);
        
        List<DeviceIdentity> devices = deviceService.getUserDevices(userId);
        
        // Transform to simplified maps for the response
        List<Map<String, Object>> response = devices.stream()
                .map(this::deviceToMap)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a specific device
     * 
     * @param deviceId The device ID to delete
     * @return Success/failure response
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> deleteDevice(
            @PathVariable("deviceId") String deviceId) {
        
        // Get the authenticated user ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        
        log.info("Deleting device: {} for user: {}", deviceId, userId);
        
        // Verify the device belongs to the user
        Optional<DeviceIdentity> deviceOpt = deviceService.findDeviceById(deviceId);
        if (deviceOpt.isEmpty() || !userId.equals(deviceOpt.get().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        
        boolean deleted = deviceService.deleteDevice(deviceId);
        
        Map<String, Object> response = Map.of(
                "success", deleted,
                "deviceId", deviceId,
                "message", deleted ? "Device deleted successfully" : "Failed to delete device"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update device trusted status
     * 
     * @param deviceId The device ID to update
     * @param trusted The trusted status to set
     * @return Success/failure response
     */
    @PutMapping("/{deviceId}/trusted/{trusted}")
    public ResponseEntity<Map<String, Object>> updateDeviceTrustedStatus(
            @PathVariable("deviceId") String deviceId,
            @PathVariable("trusted") boolean trusted) {
        
        // Get the authenticated user ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        
        log.info("Updating trusted status to {} for device: {} by user: {}", trusted, deviceId, userId);
        
        // Verify the device belongs to the user
        Optional<DeviceIdentity> deviceOpt = deviceService.findDeviceById(deviceId);
        if (deviceOpt.isEmpty() || !userId.equals(deviceOpt.get().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        
        Optional<DeviceIdentity> updatedDevice = deviceService.updateDeviceTrustedStatus(deviceId, trusted);
        
        if (updatedDevice.isPresent()) {
            Map<String, Object> response = Map.of(
                    "success", true,
                    "deviceId", deviceId,
                    "trusted", updatedDevice.get().isTrusted(),
                    "message", "Device trusted status updated successfully"
            );
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = Map.of(
                    "success", false,
                    "deviceId", deviceId,
                    "message", "Failed to update device trusted status"
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Transform device entity to simplified map for API responses
     * 
     * @param device The device entity
     * @return Map representation of the device
     */
    private Map<String, Object> deviceToMap(DeviceIdentity device) {
        return Map.of(
                "id", device.getId(),
                "deviceId", device.getDeviceId(),
                "deviceName", device.getDeviceName() != null ? device.getDeviceName() : "Unknown Device",
                "firstSeen", device.getFirstSeen(),
                "lastSeen", device.getLastSeen(),
                "trusted", device.isTrusted(),
                "platform", Map.of(
                        "os", device.getPlatformOs() != null ? device.getPlatformOs() : "Unknown",
                        "type", device.getPlatformType() != null ? device.getPlatformType() : "Unknown",
                        "brand", device.getPlatformBrand() != null ? device.getPlatformBrand() : "Unknown",
                        "model", device.getPlatformModel() != null ? device.getPlatformModel() : "Unknown",
                        "version", device.getPlatformVersion() != null ? device.getPlatformVersion() : "Unknown"
                )
        );
    }
    
    /**
     * Generate a friendly device name based on device information
     * 
     * @param deviceInfo The device information
     * @return A user-friendly device name
     */
    private String generateDeviceName(Map<String, Object> deviceInfo) {
        String platformType = (String) deviceInfo.getOrDefault("platformType", "Unknown");
        String platformBrand = (String) deviceInfo.getOrDefault("platformBrand", "");
        String platformModel = (String) deviceInfo.getOrDefault("platformModel", "");
        
        StringBuilder deviceName = new StringBuilder();
        
        if ("Unknown".equals(platformType)) {
            return "Unknown Device";
        }
        
        // Add brand if available
        if (platformBrand != null && !platformBrand.isEmpty() && !"Unknown".equals(platformBrand)) {
            deviceName.append(platformBrand).append(" ");
        }
        
        // Add model if available
        if (platformModel != null && !platformModel.isEmpty() && !"Unknown".equals(platformModel)) {
            deviceName.append(platformModel);
        }
        
        // Fallback to platform type if no brand or model
        if (deviceName.length() == 0) {
            deviceName.append(platformType);
        }
        
        return deviceName.toString().trim();
    }
}
