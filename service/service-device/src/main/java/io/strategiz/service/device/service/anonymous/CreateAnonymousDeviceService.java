package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceRequest;
import io.strategiz.service.device.model.anonymous.CreateAnonymousDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAnonymousDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateAnonymousDeviceService.class);
    
    private final CreateDeviceRepository createRepository;
    
    @Autowired
    public CreateAnonymousDeviceService(CreateDeviceRepository createRepository) {
        this.createRepository = createRepository;
    }
    
    public CreateAnonymousDeviceResponse createAnonymousDevice(CreateAnonymousDeviceRequest request) {
        log.debug("Creating anonymous device with fingerprint: {}", request.getFingerprint());
        
        try {
            // Create device entity
            DeviceIdentity device = new DeviceIdentity();
            device.setDeviceId(UUID.randomUUID().toString());
            device.setCanvasFingerprint(request.getFingerprint());
            device.setVisitorId(request.getVisitorId());
            device.setPlatform(request.getPlatform());
            device.setBrowserName(request.getBrowser());
            device.setUserAgent(request.getUserAgent());
            device.setIpAddress(request.getIpAddress());
            device.setScreenResolution(request.getScreenResolution());
            device.setTimezone(request.getTimezone());
            device.setLanguage(request.getLanguage());
            device.setOsName(request.getOsName());
            device.setOsVersion(request.getOsVersion());
            // Anonymous devices don't have userId
            device.setUserId(null);
            device.setTrusted(false);
            device.setFingerprintConfidence(calculateInitialTrustScore(request));
            device.setTrustLevel(determineTrustLevel(device.getFingerprintConfidence()));
            device.setFirstSeen(Instant.now());
            device.setLastSeen(Instant.now());
            
            // Save to repository
            DeviceIdentity savedDevice = createRepository.createAnonymousDevice(device);
            
            // Create response
            CreateAnonymousDeviceResponse response = new CreateAnonymousDeviceResponse();
            response.setDeviceId(savedDevice.getDeviceId());
            response.setFingerprint(savedDevice.getCanvasFingerprint());
            response.setTrustScore(savedDevice.getFingerprintConfidence());
            response.setTrustLevel(savedDevice.getTrustLevel());
            response.setCreatedAt(savedDevice.getFirstSeen());
            
            log.info("Successfully created anonymous device: {}", savedDevice.getDeviceId());
            return response;
            
        } catch (Exception e) {
            log.error("Error creating anonymous device: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create anonymous device", e);
        }
    }
    
    private Double calculateInitialTrustScore(CreateAnonymousDeviceRequest request) {
        double score = 0.5; // Base score
        
        // Adjust based on available information
        if (request.getFingerprint() != null && !request.getFingerprint().isEmpty()) {
            score += 0.1;
        }
        if (request.getVisitorId() != null && !request.getVisitorId().isEmpty()) {
            score += 0.1;
        }
        if (request.getUserAgent() != null && !request.getUserAgent().isEmpty()) {
            score += 0.05;
        }
        if (request.getScreenResolution() != null && !request.getScreenResolution().isEmpty()) {
            score += 0.05;
        }
        
        // Cap at 1.0
        return Math.min(score, 1.0);
    }
    
    private String determineTrustLevel(Double trustScore) {
        if (trustScore == null) return "UNKNOWN";
        if (trustScore >= 0.8) return "HIGH";
        if (trustScore >= 0.5) return "MEDIUM";
        if (trustScore >= 0.3) return "LOW";
        return "VERY_LOW";
    }
}