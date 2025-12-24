package io.strategiz.service.device.controller.anonymous;

import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.device.model.DeviceRequest;
import io.strategiz.service.device.model.CreateDeviceResponse;
import io.strategiz.service.device.service.anonymous.CreateAnonymousDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller exclusively for creating/registering anonymous devices
 * Handles device fingerprinting for non-authenticated users
 * Single Responsibility: Device creation for anonymous visitors
 */
@RestController
@RequestMapping("/v1/devices/anonymous")
public class CreateAnonymousDeviceController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(CreateAnonymousDeviceController.class);
    
    private final CreateAnonymousDeviceService createService;
    
    @Autowired
    public CreateAnonymousDeviceController(CreateAnonymousDeviceService createService) {
        this.createService = createService;
    }
    
    @Override
    protected String getModuleName() {
        return "service-device";
    }
    
    /**
     * Register a new anonymous device with comprehensive fingerprinting
     * Endpoint: POST /v1/devices/anonymous/registrations
     * 
     * Called when a user first visits the site without authentication.
     * Collects comprehensive device fingerprint for:
     * - Fraud detection
     * - Analytics
     * - Security monitoring
     * - Future device recognition when user signs up
     * 
     * @param request HTTP request for extracting IP and server-side data
     * @param deviceRequest Comprehensive device fingerprint data from client
     * @return Response with anonymous device ID for tracking
     */
    @PostMapping("/registrations")
    public ResponseEntity<CreateDeviceResponse> registerAnonymousDevice(
            HttpServletRequest request,
            @RequestBody DeviceRequest deviceRequest) {
        
        log.info("Registering anonymous device with comprehensive fingerprint");
        
        try {
            // Extract server-side information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");
            String acceptLanguage = request.getHeader("Accept-Language");
            
            // Delegate to service layer for device registration with enrichment
            CreateDeviceResponse response = createService.registerAnonymousDevice(
                deviceRequest,
                ipAddress,
                userAgent,
                referer,
                acceptLanguage
            );
            
            if (response.isSuccess()) {
                log.info("Successfully registered anonymous device: {}", 
                    response.getDeviceId());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to register anonymous device: {}", 
                    response.getError());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error registering anonymous device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateDeviceResponse.error("Internal error registering device")
            );
        }
    }
    
    /**
     * Quick registration with minimal fingerprinting
     * Endpoint: POST /v1/devices/anonymous/quick
     * 
     * For scenarios where full fingerprinting might impact performance
     * 
     * @param request HTTP request
     * @param visitorId Optional FingerprintJS visitor ID
     * @return Response with device ID
     */
    @PostMapping("/quick")
    public ResponseEntity<CreateDeviceResponse> quickRegister(
            HttpServletRequest request,
            @RequestParam(required = false) String visitorId) {
        
        log.info("Quick registration for anonymous device");
        
        try {
            // Extract minimal information
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            CreateDeviceResponse response = createService.quickRegisterDevice(
                visitorId,
                ipAddress,
                userAgent
            );
            
            if (response.isSuccess()) {
                log.info("Quick registered anonymous device: {}", response.getDeviceId());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error in quick registration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateDeviceResponse.error("Quick registration failed")
            );
        }
    }
    
    /**
     * Pre-register device for analytics before user interaction
     * Endpoint: POST /v1/devices/anonymous/preregister
     * 
     * Used for early tracking and analytics
     * 
     * @param request HTTP request
     * @return Response with provisional device ID
     */
    @PostMapping("/preregister")
    public ResponseEntity<CreateDeviceResponse> preregisterDevice(
            HttpServletRequest request) {
        
        log.debug("Pre-registering anonymous device for analytics");
        
        try {
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            CreateDeviceResponse response = createService.preregisterDevice(
                ipAddress,
                userAgent
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error pre-registering device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateDeviceResponse.error("Pre-registration failed")
            );
        }
    }
    
    /**
     * Extract client IP address from request, handling proxy headers
     */
    private String extractClientIpAddress(HttpServletRequest request) {
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
            "HTTP_FORWARDED",
            "CF-Connecting-IP",  // Cloudflare
            "True-Client-IP"      // Cloudflare Enterprise
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