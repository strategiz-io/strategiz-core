package io.strategiz.service.device.controller.anonymous;

import io.strategiz.framework.token.issuer.PasetoTokenIssuer;
import io.strategiz.service.auth.util.CookieUtil;
import io.strategiz.service.base.controller.BaseController;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceRequest;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceResponse;
import io.strategiz.service.device.service.anonymous.CreateAnonymousDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

    private final PasetoTokenIssuer pasetoTokenIssuer;

    private final CookieUtil cookieUtil;

    @Autowired
    public CreateAnonymousDeviceController(CreateAnonymousDeviceService createService,
            PasetoTokenIssuer pasetoTokenIssuer, CookieUtil cookieUtil) {
        this.createService = createService;
        this.pasetoTokenIssuer = pasetoTokenIssuer;
        this.cookieUtil = cookieUtil;
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
    public ResponseEntity<CreateAnonymousDeviceResponse> registerAnonymousDevice(
            HttpServletRequest request,
            HttpServletResponse httpResponse,
            @RequestBody CreateAnonymousDeviceRequest deviceRequest) {

        log.info("Registering anonymous device with comprehensive fingerprint");

        try {
            // Extract server-side information and enrich the request
            String ipAddress = extractClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            // Enrich request with server-side data if not already set
            if (deviceRequest.getIpAddress() == null) {
                deviceRequest.setIpAddress(ipAddress);
            }
            if (deviceRequest.getUserAgent() == null) {
                deviceRequest.setUserAgent(userAgent);
            }

            // Delegate to service layer for device registration
            CreateAnonymousDeviceResponse response = createService.createAnonymousDevice(deviceRequest);

            if (response.isSuccess()) {
                log.info("Successfully registered anonymous device: {}",
                    response.getDeviceId());

                // Issue PASETO device token and set as HttpOnly cookie
                try {
                    String fingerprint = deviceRequest.getVisitorId();
                    String deviceToken = pasetoTokenIssuer.issueDeviceToken(
                            response.getDeviceId(), fingerprint);
                    cookieUtil.setDeviceTokenCookie(httpResponse, deviceToken);
                    log.info("Set device token cookie for device: {}", response.getDeviceId());
                }
                catch (Exception e) {
                    log.warn("Failed to issue device token cookie: {}", e.getMessage());
                }

                return ResponseEntity.ok(response);
            } else {
                log.warn("Failed to register anonymous device: {}",
                    response.getError());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error registering anonymous device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateAnonymousDeviceResponse.error("Internal error registering device")
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
    public ResponseEntity<CreateAnonymousDeviceResponse> quickRegister(
            HttpServletRequest request,
            @RequestParam(required = false) String visitorId) {

        log.info("Quick registration for anonymous device");

        try {
            // Create minimal request
            CreateAnonymousDeviceRequest deviceRequest = new CreateAnonymousDeviceRequest();
            deviceRequest.setVisitorId(visitorId);
            deviceRequest.setIpAddress(extractClientIpAddress(request));
            deviceRequest.setUserAgent(request.getHeader("User-Agent"));

            CreateAnonymousDeviceResponse response = createService.createAnonymousDevice(deviceRequest);

            if (response.isSuccess()) {
                log.info("Quick registered anonymous device: {}", response.getDeviceId());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error in quick registration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateAnonymousDeviceResponse.error("Quick registration failed")
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
    public ResponseEntity<CreateAnonymousDeviceResponse> preregisterDevice(
            HttpServletRequest request) {

        log.debug("Pre-registering anonymous device for analytics");

        try {
            // Create minimal request
            CreateAnonymousDeviceRequest deviceRequest = new CreateAnonymousDeviceRequest();
            deviceRequest.setIpAddress(extractClientIpAddress(request));
            deviceRequest.setUserAgent(request.getHeader("User-Agent"));

            CreateAnonymousDeviceResponse response = createService.createAnonymousDevice(deviceRequest);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error pre-registering device: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                CreateAnonymousDeviceResponse.error("Pre-registration failed")
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
