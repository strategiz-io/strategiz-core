package io.strategiz.service.device.service.shared;

import io.strategiz.data.device.model.DeviceIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Shared service for enriching device data with server-side information
 * Used by both anonymous and authenticated device services
 */
@Service
public class DeviceEnrichmentService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceEnrichmentService.class);
    
    // Common VPN/Proxy IP ranges (simplified example)
    private static final List<String> VPN_IP_PATTERNS = Arrays.asList(
        "10\\..*",           // Private network
        "172\\.(1[6-9]|2[0-9]|3[01])\\..*", // Private network
        "192\\.168\\..*"     // Private network
    );
    
    // Known datacenter IP ranges (simplified)
    private static final List<String> DATACENTER_PATTERNS = Arrays.asList(
        "104\\..*",          // Common cloud providers
        "35\\..*",           // Google Cloud
        "52\\..*",           // AWS
        "13\\..*"            // AWS
    );
    
    /**
     * Enrich device data with server-side information
     * 
     * @param device The device entity to enrich
     * @param ipAddress The client IP address
     */
    public void enrichDeviceData(DeviceIdentity device, String ipAddress) {
        log.debug("Enriching device data for IP: {}", ipAddress);
        
        try {
            // Set IP address
            device.setIpAddress(ipAddress);
            
            // Detect VPN/Proxy
            boolean vpnDetected = detectVpn(ipAddress);
            device.setVpnDetected(vpnDetected);
            
            // Detect proxy
            boolean proxyDetected = detectProxy(ipAddress);
            device.setProxyDetected(proxyDetected);
            
            // Detect if from datacenter
            boolean datacenter = isDatacenterIp(ipAddress);
            if (datacenter) {
                log.warn("Device from datacenter IP: {}", ipAddress);
            }
            
            // Get IP location (mock implementation)
            String location = getIpLocation(ipAddress);
            device.setIpLocation(location);
            
            // Detect bot patterns
            boolean botDetected = detectBot(device.getUserAgent());
            device.setBotDetected(botDetected);
            
            // Check for tampering indicators
            boolean tamperingDetected = detectTampering(device);
            device.setTamperingDetected(tamperingDetected);
            
            log.debug("Enrichment complete - VPN: {}, Proxy: {}, Bot: {}, Tampering: {}", 
                vpnDetected, proxyDetected, botDetected, tamperingDetected);
            
        } catch (Exception e) {
            log.error("Error enriching device data: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Detect if IP is from a VPN
     */
    private boolean detectVpn(String ipAddress) {
        if (ipAddress == null) return false;
        
        // Check against known VPN patterns
        for (String pattern : VPN_IP_PATTERNS) {
            if (Pattern.matches(pattern, ipAddress)) {
                log.debug("VPN detected for IP: {}", ipAddress);
                return true;
            }
        }
        
        // In production, would use IP intelligence services
        return false;
    }
    
    /**
     * Detect if IP is from a proxy
     */
    private boolean detectProxy(String ipAddress) {
        if (ipAddress == null) return false;
        
        // Check for proxy headers patterns
        // In production, would check against proxy databases
        return ipAddress.startsWith("192.168.") || 
               ipAddress.startsWith("10.") ||
               ipAddress.equals("127.0.0.1");
    }
    
    /**
     * Check if IP is from a datacenter
     */
    private boolean isDatacenterIp(String ipAddress) {
        if (ipAddress == null) return false;
        
        for (String pattern : DATACENTER_PATTERNS) {
            if (Pattern.matches(pattern, ipAddress)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get location from IP address
     * In production, would use GeoIP service
     */
    private String getIpLocation(String ipAddress) {
        if (ipAddress == null) return "Unknown";
        
        // Mock implementation
        if (ipAddress.startsWith("192.168.")) {
            return "Local Network";
        } else if (ipAddress.startsWith("10.")) {
            return "Private Network";
        }
        
        // In production, would use MaxMind GeoIP or similar
        return "Unknown";
    }
    
    /**
     * Detect bot patterns in user agent
     */
    private boolean detectBot(String userAgent) {
        if (userAgent == null) return false;
        
        String ua = userAgent.toLowerCase();
        
        // Check for known bot patterns
        List<String> botPatterns = Arrays.asList(
            "bot", "crawler", "spider", "scraper",
            "wget", "curl", "python", "java",
            "headless", "phantomjs", "selenium"
        );
        
        for (String pattern : botPatterns) {
            if (ua.contains(pattern)) {
                log.debug("Bot detected in user agent: {}", pattern);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Detect tampering indicators
     */
    private boolean detectTampering(DeviceIdentity device) {
        // Check for inconsistencies
        
        // Example: Chrome user agent but no Chrome-specific features
        if (device.getUserAgent() != null && 
            device.getUserAgent().contains("Chrome") &&
            device.getBrowserName() != null &&
            !device.getBrowserName().equalsIgnoreCase("Chrome")) {
            log.warn("User agent tampering detected");
            return true;
        }
        
        // Check for impossible combinations
        if (device.getMaxTouchPoints() != null && 
            device.getMaxTouchPoints() > 0 &&
            device.getOsName() != null &&
            (device.getOsName().equalsIgnoreCase("Windows") || 
             device.getOsName().equalsIgnoreCase("Linux"))) {
            // Desktop OS with touch points might indicate tampering
            log.warn("Suspicious touch points on desktop OS");
            return true;
        }
        
        return false;
    }
    
    /**
     * Validate and normalize timezone
     */
    public String normalizeTimezone(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return "UTC";
        }
        
        // Validate timezone format
        try {
            // In production, would validate against known timezones
            return timezone;
        } catch (Exception e) {
            log.warn("Invalid timezone: {}", timezone);
            return "UTC";
        }
    }
    
    /**
     * Calculate geographic distance between IP locations
     * Used for detecting suspicious location changes
     */
    public double calculateLocationDistance(String ip1, String ip2) {
        // In production, would use actual geo coordinates
        // This is a placeholder implementation
        String loc1 = getIpLocation(ip1);
        String loc2 = getIpLocation(ip2);
        
        if (loc1.equals(loc2)) {
            return 0.0;
        }
        
        // Return arbitrary distance for different locations
        return 1000.0; // km
    }
}