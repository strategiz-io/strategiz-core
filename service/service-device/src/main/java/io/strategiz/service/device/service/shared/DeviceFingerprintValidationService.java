package io.strategiz.service.device.service.shared;

import io.strategiz.service.device.model.DeviceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Shared service for validating device fingerprint data
 * Ensures data integrity and detects anomalies
 */
@Service
public class DeviceFingerprintValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintValidationService.class);
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );
    
    private static final Pattern CANVAS_FINGERPRINT_PATTERN = Pattern.compile(
        "^[a-f0-9]{64}$" // SHA-256 hash
    );
    
    /**
     * Validate comprehensive device fingerprint
     * 
     * @param request The device request to validate
     * @return true if valid, false otherwise
     */
    public boolean validateFingerprint(DeviceRequest request) {
        if (request == null) {
            log.warn("Null device request");
            return false;
        }
        
        // Validate required fields
        if (!validateRequiredFields(request)) {
            return false;
        }
        
        // Validate fingerprint consistency
        if (!validateFingerprintConsistency(request)) {
            return false;
        }
        
        // Validate data ranges
        if (!validateDataRanges(request)) {
            return false;
        }
        
        // Check for suspicious patterns
        if (detectSuspiciousPatterns(request)) {
            log.warn("Suspicious patterns detected in fingerprint");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate required fields are present
     */
    private boolean validateRequiredFields(DeviceRequest request) {
        // Device ID or visitor ID should be present
        if (request.getDeviceId() == null && 
            (request.getFingerprint() == null || 
             request.getFingerprint().getVisitorId() == null)) {
            log.warn("Missing device ID and visitor ID");
            return false;
        }
        
        // Browser info should be present
        if (request.getBrowser() == null) {
            log.warn("Missing browser information");
            return false;
        }
        
        // OS info should be present
        if (request.getOs() == null) {
            log.warn("Missing OS information");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate fingerprint data consistency
     */
    private boolean validateFingerprintConsistency(DeviceRequest request) {
        // Check browser consistency
        if (request.getBrowser() != null) {
            String browserName = request.getBrowser().getName();
            String userAgent = request.getBrowser().getUserAgent();
            
            if (browserName != null && userAgent != null) {
                // Validate browser name matches user agent
                if (browserName.equalsIgnoreCase("Chrome") && 
                    !userAgent.contains("Chrome")) {
                    log.warn("Browser name doesn't match user agent");
                    return false;
                }
                
                if (browserName.equalsIgnoreCase("Firefox") && 
                    !userAgent.contains("Firefox")) {
                    log.warn("Browser name doesn't match user agent");
                    return false;
                }
            }
        }
        
        // Check OS consistency
        if (request.getOs() != null && request.getBrowser() != null) {
            String osName = request.getOs().getName();
            String userAgent = request.getBrowser().getUserAgent();
            
            if (osName != null && userAgent != null) {
                if (osName.equalsIgnoreCase("Windows") && 
                    !userAgent.contains("Windows")) {
                    log.warn("OS name doesn't match user agent");
                    return false;
                }
                
                if (osName.equalsIgnoreCase("macOS") && 
                    !userAgent.contains("Mac OS")) {
                    log.warn("OS name doesn't match user agent");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Validate data ranges
     */
    private boolean validateDataRanges(DeviceRequest request) {
        // Validate hardware ranges
        if (request.getHardware() != null) {
            // Screen resolution
            if (request.getHardware().getScreenResolution() != null) {
                if (!validateScreenResolution(request.getHardware().getScreenResolution())) {
                    log.warn("Invalid screen resolution: {}", 
                        request.getHardware().getScreenResolution());
                    return false;
                }
            }
            
            // Color depth (typically 8, 16, 24, or 32)
            Integer colorDepth = request.getHardware().getColorDepth();
            if (colorDepth != null && 
                (colorDepth < 1 || colorDepth > 48)) {
                log.warn("Invalid color depth: {}", colorDepth);
                return false;
            }
            
            // Hardware concurrency (CPU cores)
            Integer cores = request.getHardware().getHardwareConcurrency();
            if (cores != null && (cores < 1 || cores > 256)) {
                log.warn("Invalid hardware concurrency: {}", cores);
                return false;
            }
            
            // Device memory (in GB)
            Integer memory = request.getHardware().getDeviceMemory();
            if (memory != null && (memory < 0 || memory > 1024)) {
                log.warn("Invalid device memory: {}", memory);
                return false;
            }
            
            // Max touch points
            Integer touchPoints = request.getHardware().getMaxTouchPoints();
            if (touchPoints != null && (touchPoints < 0 || touchPoints > 20)) {
                log.warn("Invalid max touch points: {}", touchPoints);
                return false;
            }
        }
        
        // Validate fingerprint confidence
        if (request.getFingerprint() != null && 
            request.getFingerprint().getConfidence() != null) {
            Double confidence = request.getFingerprint().getConfidence();
            if (confidence < 0.0 || confidence > 1.0) {
                log.warn("Invalid fingerprint confidence: {}", confidence);
                return false;
            }
        }
        
        // Validate timezone offset (-720 to 840 minutes)
        if (request.getNetwork() != null && 
            request.getNetwork().getTimezoneOffset() != null) {
            Integer offset = request.getNetwork().getTimezoneOffset();
            if (offset < -720 || offset > 840) {
                log.warn("Invalid timezone offset: {}", offset);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validate screen resolution format
     */
    private boolean validateScreenResolution(String resolution) {
        if (resolution == null) return false;
        
        // Format should be WIDTHxHEIGHT
        Pattern resPattern = Pattern.compile("^\\d{3,5}x\\d{3,5}$");
        if (!resPattern.matcher(resolution).matches()) {
            return false;
        }
        
        String[] parts = resolution.split("x");
        try {
            int width = Integer.parseInt(parts[0]);
            int height = Integer.parseInt(parts[1]);
            
            // Reasonable bounds for screen resolution
            return width >= 320 && width <= 10000 && 
                   height >= 240 && height <= 10000;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Detect suspicious patterns in fingerprint
     */
    private boolean detectSuspiciousPatterns(DeviceRequest request) {
        // Check for all nulls (minimal fingerprint)
        int nullCount = 0;
        int totalFields = 0;
        
        if (request.getBrowser() != null) {
            totalFields += 8;
            if (request.getBrowser().getName() == null) nullCount++;
            if (request.getBrowser().getVersion() == null) nullCount++;
            if (request.getBrowser().getVendor() == null) nullCount++;
            if (request.getBrowser().getUserAgent() == null) nullCount++;
            if (request.getBrowser().getCookiesEnabled() == null) nullCount++;
            if (request.getBrowser().getDoNotTrack() == null) nullCount++;
            if (request.getBrowser().getLanguage() == null) nullCount++;
            if (request.getBrowser().getLanguages() == null) nullCount++;
        }
        
        if (request.getHardware() != null) {
            totalFields += 7;
            if (request.getHardware().getScreenResolution() == null) nullCount++;
            if (request.getHardware().getColorDepth() == null) nullCount++;
            if (request.getHardware().getPixelRatio() == null) nullCount++;
            if (request.getHardware().getHardwareConcurrency() == null) nullCount++;
            if (request.getHardware().getDeviceMemory() == null) nullCount++;
            if (request.getHardware().getMaxTouchPoints() == null) nullCount++;
        }
        
        // If more than 70% of fields are null, it's suspicious
        if (totalFields > 0 && (double)nullCount / totalFields > 0.7) {
            log.warn("Too many null fields in fingerprint: {}/{}", nullCount, totalFields);
            return true;
        }
        
        // Check for default/generic values
        if (request.getBrowser() != null && 
            "Unknown".equals(request.getBrowser().getName()) &&
            "Unknown".equals(request.getBrowser().getVersion())) {
            log.warn("Generic browser values detected");
            return true;
        }
        
        // Check canvas fingerprint format
        if (request.getRendering() != null && 
            request.getRendering().getCanvas() != null) {
            String canvas = request.getRendering().getCanvas();
            if (!CANVAS_FINGERPRINT_PATTERN.matcher(canvas).matches() &&
                !canvas.equals("canvas-not-supported") &&
                !canvas.equals("canvas-error")) {
                log.warn("Invalid canvas fingerprint format: {}", canvas);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validate IP address format
     */
    public boolean validateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        
        // Check IPv4 format
        if (IP_PATTERN.matcher(ipAddress).matches()) {
            return true;
        }
        
        // Check IPv6 (simplified)
        if (ipAddress.contains(":")) {
            return true; // Basic IPv6 check
        }
        
        return false;
    }
    
    /**
     * Check if fingerprint data appears to be spoofed
     */
    public boolean isLikelySpoofed(DeviceRequest request) {
        if (request.getTrust() == null) {
            return false;
        }
        
        // Check lie detection flags
        boolean hasLies = Boolean.TRUE.equals(request.getTrust().getHasLiedLanguages()) ||
                         Boolean.TRUE.equals(request.getTrust().getHasLiedResolution()) ||
                         Boolean.TRUE.equals(request.getTrust().getHasLiedOs()) ||
                         Boolean.TRUE.equals(request.getTrust().getHasLiedBrowser());
        
        if (hasLies) {
            log.warn("Fingerprint spoofing detected");
            return true;
        }
        
        // Check for tampering
        if (Boolean.TRUE.equals(request.getTrust().getTampering())) {
            log.warn("Fingerprint tampering detected");
            return true;
        }
        
        return false;
    }
}