package io.strategiz.service.device.controller.authenticated;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.base.constants.ModuleConstants;
import io.strategiz.service.device.model.authenticated.CreateAuthenticatedDeviceRequest;
import io.strategiz.service.device.model.authenticated.CreateAuthenticatedDeviceResponse;
import io.strategiz.service.device.service.authenticated.CreateAuthenticatedDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller exclusively for creating/registering authenticated devices
 * Handles comprehensive device fingerprinting during sign-in and sign-up flows
 * Single Responsibility: Device creation for authenticated users
 */
@RestController
@RequestMapping("/v1/users/devices")
public class CreateAuthenticatedDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(CreateAuthenticatedDeviceController.class);
    
    private final CreateAuthenticatedDeviceService createService;
    
    @Autowired
    public CreateAuthenticatedDeviceController(CreateAuthenticatedDeviceService createService) {
        this.createService = createService;
    }
    
    @Override
    protected String getModuleName() {
        return ModuleConstants.DEVICE_MODULE;
    }
    
    /**
     * Register a new authenticated device with comprehensive fingerprinting
     * Endpoint: POST /v1/users/devices/registrations
     * 
     * This endpoint receives comprehensive device fingerprinting data including:
     * - FingerprintJS visitor ID and confidence score
     * - Browser information (name, version, vendor, languages)
     * - Operating system details
     * - Hardware capabilities (screen, memory, CPU cores)
     * - Rendering fingerprints (Canvas, WebGL, fonts)
     * - Network information (timezone, offset)
     * - Media capabilities (audio/video codecs, devices)
     * - Browser features detection
     * - Trust indicators (incognito, ad block, VPN detection)
     * - Web Crypto public key for device authentication
     * 
     * @param request HTTP request for extracting IP and server-side data
     * @param deviceRequest Comprehensive device fingerprint data from client
     * @return Response with registered device details
     */
    @PostMapping("/registrations")
    public ResponseEntity<CreateAuthenticatedDeviceResponse> registerDevice(
            HttpServletRequest request,
            @RequestBody CreateAuthenticatedDeviceRequest deviceRequest) {
        
        // Get authenticated user ID from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.error("Attempt to register device without authentication");
            CreateAuthenticatedDeviceResponse errorResponse = new CreateAuthenticatedDeviceResponse();
            errorResponse.setError("Authentication required");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String userId = auth.getName();
        log.info("Registering comprehensive device fingerprint for user: {}", userId);
        
        try {
            // Extract server-side information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            // Add server-side information to the request
            deviceRequest.setUserAgent(userAgent);
            
            // Delegate to service layer for device registration
            CreateAuthenticatedDeviceResponse response = createService.createAuthenticatedDevice(
                userId,
                deviceRequest
            );
            
            if (response.isSuccess()) {
                log.info("Successfully registered device {} for user {}", 
                    response.getDeviceId(), userId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to register device for user {}: {}", 
                    userId, response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error registering device for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateDeviceResponse.error("Internal error registering device")
            );
        }
    }
    
    /**
     * Bulk register multiple devices (for migration or import scenarios)
     * Endpoint: POST /v1/users/devices/bulk
     * 
     * @param request HTTP request
     * @param deviceRequests Array of device fingerprints to register
     * @return Response with registration results
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkRegisterDevices(
            HttpServletRequest request,
            @RequestBody DeviceRequest[] deviceRequests) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        
        String userId = auth.getName();
        log.info("Bulk registering {} devices for user: {}", deviceRequests.length, userId);
        
        // This would be implemented if needed for bulk operations
        return ResponseEntity.status(501).body("Bulk registration not yet implemented");
    }
    
    /**
     * Extract client IP address from request, handling proxy headers
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        // Check for proxy headers in order of preference
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated list of IPs (first is original client)
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }
}