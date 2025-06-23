package io.strategiz.service.device.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for extracting device fingerprint information from requests
 */
@Component
public class DeviceFingerprintUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintUtil.class);
    
    /**
     * Extract device information from the user agent and headers
     * 
     * @param request HTTP request
     * @return Map containing device fingerprint information
     */
    public Map<String, Object> extractDeviceInfo(HttpServletRequest request) {
        Map<String, Object> deviceInfo = new HashMap<>();
        
        try {
            String userAgent = request.getHeader("User-Agent");
            deviceInfo.put("userAgent", userAgent);
            
            // Parse user agent for basic platform info
            Map<String, String> platformInfo = parseUserAgent(userAgent);
            deviceInfo.putAll(platformInfo);
            
            // Add IP address
            deviceInfo.put("ipAddress", getClientIpAddress(request));
            
            // Add any additional device info from client-side fingerprinting
            // (These would be sent as headers or parameters from client-side JavaScript)
            if (request.getHeader("X-Screen-Resolution") != null) {
                deviceInfo.put("screenResolution", request.getHeader("X-Screen-Resolution"));
            }
            
            if (request.getHeader("X-Color-Depth") != null) {
                deviceInfo.put("colorDepth", request.getHeader("X-Color-Depth"));
            }
            
            if (request.getHeader("X-Timezone-Offset") != null) {
                deviceInfo.put("timezoneOffset", request.getHeader("X-Timezone-Offset"));
            }
            
            if (request.getHeader("X-Language") != null) {
                deviceInfo.put("language", request.getHeader("X-Language"));
            }
            
            if (request.getHeader("X-Device-Memory") != null) {
                deviceInfo.put("deviceMemory", request.getHeader("X-Device-Memory"));
            }
            
            if (request.getHeader("X-Hardware-Concurrency") != null) {
                deviceInfo.put("hardwareConcurrency", request.getHeader("X-Hardware-Concurrency"));
            }
            
            if (request.getHeader("X-Device-Fingerprint") != null) {
                deviceInfo.put("fingerprint", request.getHeader("X-Device-Fingerprint"));
            }
        } catch (Exception e) {
            log.error("Error extracting device information: {}", e.getMessage(), e);
        }
        
        return deviceInfo;
    }
    
    /**
     * Extract basic platform information from the user agent string
     * 
     * @param userAgent The user agent string
     * @return Map containing platform information
     */
    private Map<String, String> parseUserAgent(String userAgent) {
        Map<String, String> info = new HashMap<>();
        
        if (userAgent == null) {
            return info;
        }
        
        // Default values
        String platformOs = "Unknown";
        String platformType = "Unknown";
        String platformBrand = "Unknown";
        String platformModel = "Unknown";
        String platformVersion = "Unknown";
        
        userAgent = userAgent.toLowerCase();
        
        // Detect OS
        if (userAgent.contains("windows")) {
            platformOs = "Windows";
            if (userAgent.contains("windows nt 10")) {
                platformVersion = "10";
            } else if (userAgent.contains("windows nt 6.3")) {
                platformVersion = "8.1";
            } else if (userAgent.contains("windows nt 6.2")) {
                platformVersion = "8";
            } else if (userAgent.contains("windows nt 6.1")) {
                platformVersion = "7";
            } else if (userAgent.contains("windows nt 6.0")) {
                platformVersion = "Vista";
            } else if (userAgent.contains("windows nt 5.1")) {
                platformVersion = "XP";
            }
            platformType = "Desktop";
        } else if (userAgent.contains("macintosh") || userAgent.contains("mac os x")) {
            platformOs = "macOS";
            platformType = "Desktop";
            // Try to extract version
            int startIndex = userAgent.indexOf("mac os x ");
            if (startIndex > -1) {
                startIndex += 9; // Length of "mac os x "
                StringBuilder version = new StringBuilder();
                for (int i = startIndex; i < userAgent.length(); i++) {
                    char c = userAgent.charAt(i);
                    if (c == '.' || Character.isDigit(c) || c == '_') {
                        version.append(c == '_' ? '.' : c);
                    } else {
                        break;
                    }
                }
                if (version.length() > 0) {
                    platformVersion = version.toString();
                }
            }
        } else if (userAgent.contains("linux")) {
            platformOs = "Linux";
            platformType = "Desktop";
            if (userAgent.contains("ubuntu")) {
                platformBrand = "Ubuntu";
            } else if (userAgent.contains("fedora")) {
                platformBrand = "Fedora";
            } else if (userAgent.contains("debian")) {
                platformBrand = "Debian";
            }
        } else if (userAgent.contains("android")) {
            platformOs = "Android";
            platformType = userAgent.contains("mobile") ? "Mobile" : "Tablet";
            // Try to extract version
            int startIndex = userAgent.indexOf("android ");
            if (startIndex > -1) {
                startIndex += 8; // Length of "android "
                StringBuilder version = new StringBuilder();
                for (int i = startIndex; i < userAgent.length(); i++) {
                    char c = userAgent.charAt(i);
                    if (c == '.' || Character.isDigit(c)) {
                        version.append(c);
                    } else {
                        break;
                    }
                }
                if (version.length() > 0) {
                    platformVersion = version.toString();
                }
            }
            
            // Try to extract manufacturer and model
            if (userAgent.contains("samsung")) {
                platformBrand = "Samsung";
                if (userAgent.contains("sm-")) {
                    int modelStartIndex = userAgent.indexOf("sm-");
                    if (modelStartIndex > -1) {
                        StringBuilder model = new StringBuilder();
                        for (int i = modelStartIndex; i < userAgent.length() && i < modelStartIndex + 10; i++) {
                            char c = userAgent.charAt(i);
                            if (c == ' ' || c == ';') {
                                break;
                            }
                            model.append(c);
                        }
                        if (model.length() > 0) {
                            platformModel = model.toString().toUpperCase();
                        }
                    }
                }
            } else if (userAgent.contains("lg")) {
                platformBrand = "LG";
            } else if (userAgent.contains("motorola")) {
                platformBrand = "Motorola";
            } else if (userAgent.contains("huawei")) {
                platformBrand = "Huawei";
            } else if (userAgent.contains("xiaomi")) {
                platformBrand = "Xiaomi";
            } else if (userAgent.contains("google")) {
                platformBrand = "Google";
                if (userAgent.contains("pixel")) {
                    platformModel = "Pixel";
                }
            }
        } else if (userAgent.contains("iphone")) {
            platformOs = "iOS";
            platformType = "Mobile";
            platformBrand = "Apple";
            platformModel = "iPhone";
            // Try to extract version
            int startIndex = userAgent.indexOf("cpu iphone os ");
            if (startIndex > -1) {
                startIndex += 14; // Length of "cpu iphone os "
                StringBuilder version = new StringBuilder();
                for (int i = startIndex; i < userAgent.length(); i++) {
                    char c = userAgent.charAt(i);
                    if (c == '.' || Character.isDigit(c) || c == '_') {
                        version.append(c == '_' ? '.' : c);
                    } else {
                        break;
                    }
                }
                if (version.length() > 0) {
                    platformVersion = version.toString();
                }
            }
        } else if (userAgent.contains("ipad")) {
            platformOs = "iOS";
            platformType = "Tablet";
            platformBrand = "Apple";
            platformModel = "iPad";
            // Try to extract version
            int startIndex = userAgent.indexOf("cpu os ");
            if (startIndex > -1) {
                startIndex += 7; // Length of "cpu os "
                StringBuilder version = new StringBuilder();
                for (int i = startIndex; i < userAgent.length(); i++) {
                    char c = userAgent.charAt(i);
                    if (c == '.' || Character.isDigit(c) || c == '_') {
                        version.append(c == '_' ? '.' : c);
                    } else {
                        break;
                    }
                }
                if (version.length() > 0) {
                    platformVersion = version.toString();
                }
            }
        }
        
        info.put("platformOs", platformOs);
        info.put("platformType", platformType);
        info.put("platformBrand", platformBrand);
        info.put("platformModel", platformModel);
        info.put("platformVersion", platformVersion);
        
        return info;
    }
    
    /**
     * Get the client's IP address accounting for proxies
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = null;
        
        // Check proxied IP headers first
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };
        
        for (String header : ipHeaders) {
            String value = request.getHeader(header);
            if (value != null && value.length() > 0 && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For may contain multiple IPs, get the first one
                ipAddress = value.split(",")[0];
                break;
            }
        }
        
        // If no proxy was used, get the remote address
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        
        return ipAddress;
    }
}
