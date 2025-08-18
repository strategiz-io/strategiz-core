package io.strategiz.service.device.service.shared;

import io.strategiz.data.device.model.DeviceIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Shared service for calculating device trust scores
 * Used to assess risk and determine security requirements
 */
@Service
public class DeviceTrustCalculationService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceTrustCalculationService.class);
    
    // Trust score thresholds
    private static final int HIGH_TRUST_THRESHOLD = 80;
    private static final int MEDIUM_TRUST_THRESHOLD = 50;
    private static final int LOW_TRUST_THRESHOLD = 30;
    
    /**
     * Calculate comprehensive trust score for a device
     * 
     * @param device The device to evaluate
     * @return Trust score from 0 to 100
     */
    public int calculateTrustScore(DeviceIdentity device) {
        if (device == null) {
            return 0;
        }
        
        int score = 100; // Start with perfect score
        
        // Privacy mode indicators (-10 to -20 points)
        if (Boolean.TRUE.equals(device.getIncognitoMode())) {
            score -= 10;
            log.debug("Incognito mode detected: -10 points");
        }
        
        if (Boolean.TRUE.equals(device.getAdBlockEnabled())) {
            score -= 5; // Less severe, common extension
            log.debug("Ad blocker detected: -5 points");
        }
        
        // Network indicators (-20 to -30 points each)
        if (Boolean.TRUE.equals(device.getVpnDetected())) {
            score -= 20;
            log.debug("VPN detected: -20 points");
        }
        
        if (Boolean.TRUE.equals(device.getProxyDetected())) {
            score -= 20;
            log.debug("Proxy detected: -20 points");
        }
        
        // Bot indicators (-40 points)
        if (Boolean.TRUE.equals(device.getBotDetected())) {
            score -= 40;
            log.debug("Bot detected: -40 points");
        }
        
        // Tampering indicators (-30 points)
        if (Boolean.TRUE.equals(device.getTamperingDetected())) {
            score -= 30;
            log.debug("Tampering detected: -30 points");
        }
        
        // Lie detection (-15 points each)
        if (Boolean.TRUE.equals(device.getHasLiedLanguages())) {
            score -= 15;
            log.debug("Language lie detected: -15 points");
        }
        
        if (Boolean.TRUE.equals(device.getHasLiedResolution())) {
            score -= 15;
            log.debug("Resolution lie detected: -15 points");
        }
        
        if (Boolean.TRUE.equals(device.getHasLiedOs())) {
            score -= 15;
            log.debug("OS lie detected: -15 points");
        }
        
        if (Boolean.TRUE.equals(device.getHasLiedBrowser())) {
            score -= 15;
            log.debug("Browser lie detected: -15 points");
        }
        
        // Positive indicators (add points)
        
        // High fingerprint confidence (+5 to +10 points)
        if (device.getFingerprintConfidence() != null) {
            if (device.getFingerprintConfidence() > 0.95) {
                score += 10;
                log.debug("High fingerprint confidence: +10 points");
            } else if (device.getFingerprintConfidence() > 0.85) {
                score += 5;
                log.debug("Good fingerprint confidence: +5 points");
            }
        }
        
        // Has public key for cryptographic verification (+5 points)
        if (device.getPublicKey() != null && !device.getPublicKey().isEmpty()) {
            score += 5;
            log.debug("Has public key: +5 points");
        }
        
        // Complete device profile (+5 points)
        if (hasCompleteProfile(device)) {
            score += 5;
            log.debug("Complete device profile: +5 points");
        }
        
        // Standard browser features (+3 points each)
        if (Boolean.TRUE.equals(device.getHasLocalStorage())) {
            score += 3;
        }
        if (Boolean.TRUE.equals(device.getHasSessionStorage())) {
            score += 3;
        }
        if (Boolean.TRUE.equals(device.getCookiesEnabled())) {
            score += 3;
        }
        
        // Device history bonus (for returning devices)
        if (device.getFirstSeen() != null && device.getLastSeen() != null) {
            long daysSinceFirstSeen = calculateDaysSinceFirstSeen(device);
            if (daysSinceFirstSeen > 30) {
                score += 10; // Established device
                log.debug("Established device (>30 days): +10 points");
            } else if (daysSinceFirstSeen > 7) {
                score += 5; // Recently established
                log.debug("Recently established device (>7 days): +5 points");
            }
        }
        
        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));
        
        log.info("Calculated trust score: {} for device: {}", score, device.getDeviceId());
        return score;
    }
    
    /**
     * Get trust level based on score
     * 
     * @param trustScore The calculated trust score
     * @return Trust level string
     */
    public String getTrustLevel(int trustScore) {
        if (trustScore >= HIGH_TRUST_THRESHOLD) {
            return "high";
        } else if (trustScore >= MEDIUM_TRUST_THRESHOLD) {
            return "medium";
        } else if (trustScore >= LOW_TRUST_THRESHOLD) {
            return "low";
        } else {
            return "blocked";
        }
    }
    
    /**
     * Determine if device requires additional verification
     * 
     * @param trustScore The calculated trust score
     * @return true if verification required
     */
    public boolean requiresVerification(int trustScore) {
        return trustScore < MEDIUM_TRUST_THRESHOLD;
    }
    
    /**
     * Calculate risk level (inverse of trust)
     * 
     * @param trustScore The calculated trust score
     * @return Risk level from 0 (no risk) to 100 (maximum risk)
     */
    public int calculateRiskLevel(int trustScore) {
        return 100 - trustScore;
    }
    
    /**
     * Check if device has a complete profile
     */
    private boolean hasCompleteProfile(DeviceIdentity device) {
        return device.getBrowserName() != null &&
               device.getBrowserVersion() != null &&
               device.getOsName() != null &&
               device.getOsVersion() != null &&
               device.getScreenResolution() != null &&
               device.getTimezone() != null &&
               device.getLanguage() != null &&
               device.getCanvasFingerprint() != null;
    }
    
    /**
     * Calculate days since device was first seen
     */
    private long calculateDaysSinceFirstSeen(DeviceIdentity device) {
        if (device.getFirstSeen() == null) {
            return 0;
        }
        
        long millisSinceFirstSeen = System.currentTimeMillis() - 
            device.getFirstSeen().toEpochMilli();
        return millisSinceFirstSeen / (1000 * 60 * 60 * 24); // Convert to days
    }
    
    /**
     * Get security recommendations based on trust score
     * 
     * @param trustScore The calculated trust score
     * @return Array of recommended security measures
     */
    public String[] getSecurityRecommendations(int trustScore) {
        if (trustScore >= HIGH_TRUST_THRESHOLD) {
            return new String[] {
                "Standard authentication sufficient",
                "Monitor for unusual activity"
            };
        } else if (trustScore >= MEDIUM_TRUST_THRESHOLD) {
            return new String[] {
                "Consider two-factor authentication",
                "Monitor closely for suspicious activity",
                "Limit sensitive operations"
            };
        } else if (trustScore >= LOW_TRUST_THRESHOLD) {
            return new String[] {
                "Require two-factor authentication",
                "Restrict access to sensitive features",
                "Enable enhanced monitoring",
                "Consider CAPTCHA verification"
            };
        } else {
            return new String[] {
                "Block access to sensitive operations",
                "Require manual verification",
                "Enable maximum security measures",
                "Consider blocking device entirely"
            };
        }
    }
    
    /**
     * Check if device should be auto-blocked
     * 
     * @param device The device to evaluate
     * @return true if device should be blocked
     */
    public boolean shouldAutoBlock(DeviceIdentity device) {
        // Auto-block if multiple severe indicators
        int severeIndicators = 0;
        
        if (Boolean.TRUE.equals(device.getBotDetected())) severeIndicators++;
        if (Boolean.TRUE.equals(device.getTamperingDetected())) severeIndicators++;
        if (Boolean.TRUE.equals(device.getVpnDetected()) && 
            Boolean.TRUE.equals(device.getProxyDetected())) severeIndicators++;
        
        // Check for multiple lies
        int lieCount = 0;
        if (Boolean.TRUE.equals(device.getHasLiedLanguages())) lieCount++;
        if (Boolean.TRUE.equals(device.getHasLiedResolution())) lieCount++;
        if (Boolean.TRUE.equals(device.getHasLiedOs())) lieCount++;
        if (Boolean.TRUE.equals(device.getHasLiedBrowser())) lieCount++;
        
        if (lieCount >= 3) severeIndicators++;
        
        boolean shouldBlock = severeIndicators >= 2;
        
        if (shouldBlock) {
            log.warn("Device {} should be auto-blocked due to {} severe indicators", 
                device.getDeviceId(), severeIndicators);
        }
        
        return shouldBlock;
    }
}