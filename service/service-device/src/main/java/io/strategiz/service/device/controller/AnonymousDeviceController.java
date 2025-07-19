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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Controller for anonymous device operations (non-authenticated flows)
 * Used primarily for landing page and initial device registration
 */
@RestController
@RequestMapping("/v1/device/anonymous")
public class AnonymousDeviceController {
    
    private static final Logger log = LoggerFactory.getLogger(AnonymousDeviceController.class);
    
    private final DeviceIdentityService deviceService;
    private final DeviceFingerprintUtil fingerprintUtil;
    
    @Autowired
    public AnonymousDeviceController(
            DeviceIdentityService deviceService,
            DeviceFingerprintUtil fingerprintUtil) {
        this.deviceService = deviceService;
        this.fingerprintUtil = fingerprintUtil;
    }
    
    /**
     * Register a new anonymous device
     * Called when a user first visits the site without authentication
     * 
     * @param request The HTTP request
     * @param registrationRequest Client-side device information
     * @return Response with device ID
     */
    @PostMapping("/register")
    public ResponseEntity<CreateDeviceResponse> registerAnonymousDevice(
            HttpServletRequest request,
            @RequestBody(required = false) CreateDeviceRequest registrationRequest) {
        
        log.info("Registering anonymous device");
        
        try {
            // Extract device information from request and headers
            Map<String, Object> deviceInfo = fingerprintUtil.extractDeviceInfo(request);
            
            // Add any additional client-side fingerprinting data
            if (registrationRequest != null && registrationRequest.getDeviceInfo() != null) {
                deviceInfo.putAll(registrationRequest.getDeviceInfo());
            }
            
            // Get the user agent
            String userAgent = request.getHeader("User-Agent");
            
            // Register the device
            DeviceIdentity device = deviceService.registerAnonymousDevice(deviceInfo, userAgent);
            
            // Prepare the response
            CreateDeviceResponse response = new CreateDeviceResponse();
            response.setDeviceId(device.getDeviceId());
            response.setRegistrationTime(device.getFirstSeen());
            response.setSuccess(true);
            
            log.info("Anonymous device registered: {}", device.getDeviceId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to register anonymous device: {}", e.getMessage(), e);
            
            CreateDeviceResponse response = new CreateDeviceResponse();
            response.setSuccess(false);
            response.setError("Failed to register device: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
