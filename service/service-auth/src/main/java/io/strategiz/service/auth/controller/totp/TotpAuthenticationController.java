package io.strategiz.service.auth.controller.totp;

import io.strategiz.service.auth.service.totp.TotpService;
import io.strategiz.service.auth.model.totp.*;
import io.strategiz.service.auth.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for TOTP authentication operations
 * Handles validation of TOTP codes during login
 */
@RestController
@RequestMapping("/auth/totp/authentication")
public class TotpAuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(TotpAuthenticationController.class);

    @Autowired
    private TotpService totpService;
    
    /**
     * Authenticate using TOTP
     * Verifies the provided TOTP code and returns authentication tokens if valid
     * 
     * @param request Authentication request with user ID and code
     * @return Response with authentication tokens if successful
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TotpAuthenticationResponse>> authenticateWithTotp(
            @RequestBody TotpAuthenticationRequest request, HttpServletRequest httpRequest) {
        log.info("Authenticating with TOTP for user ID: {}", request.userId());
        
        try {
            String ipAddress = getClientIp(httpRequest);
            log.debug("Authentication attempt from IP: {}", ipAddress);
            
            // Verify the TOTP code
            boolean isAuthenticated = totpService.verifyCode(request.userId(), request.code());
            
            if (!isAuthenticated) {
                log.warn("TOTP authentication failed for user: {}", request.userId());
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Authentication failed"));
            }
            
            // TODO: Implement proper token service integration
            // This is a placeholder for TokenPair logic that would be called:
            // Optional<TokenService.TokenPair> tokenPairOpt = tokenService.generateTokenPair(request.userId());
            
            // Placeholder - replace with actual token generation
            String accessToken = "sample-access-token-" + System.currentTimeMillis();
            String refreshToken = "sample-refresh-token-" + System.currentTimeMillis();
            
            TotpAuthenticationResponse response = TotpAuthenticationResponse.success(accessToken, refreshToken);
            log.info("TOTP authentication successful for user: {}", request.userId());
            return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
            
        } catch (Exception e) {
            log.error("Error during TOTP authentication: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Authentication error: " + e.getMessage()));
        }
    }
    
    /**
     * Get the client IP address from the request
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
