package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.authenticated.CreateAuthenticatedDeviceRequest;
import io.strategiz.service.device.model.authenticated.CreateAuthenticatedDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAuthenticatedDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateAuthenticatedDeviceService.class);
    
    private final CreateDeviceRepository createRepository;
    
    @Autowired
    public CreateAuthenticatedDeviceService(CreateDeviceRepository createRepository) {
        this.createRepository = createRepository;
    }
    
    public CreateAuthenticatedDeviceResponse createAuthenticatedDevice(
            String userId, CreateAuthenticatedDeviceRequest request) {
        
        log.debug("Creating authenticated device for user: {}", userId);
        
        try {
            // Create device entity
            DeviceIdentity device = new DeviceIdentity();
            device.setDeviceId(UUID.randomUUID().toString());
            device.setUserId(userId);
            device.setDeviceName(request.getDeviceName());
            device.setFingerprint(request.getFingerprint());
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
            device.setAnonymous(false);
            device.setTrusted(request.isTrusted());
            device.setTrustScore(calculateInitialTrustScore(request));
            device.setTrustLevel(determineTrustLevel(device.getTrustScore()));
            device.setCreatedAt(Instant.now());
            device.setLastSeen(Instant.now());
            
            // Save to repository
            DeviceIdentity savedDevice = createRepository.createAuthenticatedDevice(userId, device);
            
            // Create response
            CreateAuthenticatedDeviceResponse response = new CreateAuthenticatedDeviceResponse();
            response.setDeviceId(savedDevice.getDeviceId());
            response.setUserId(savedDevice.getUserId());
            response.setDeviceName(savedDevice.getDeviceName());
            response.setFingerprint(savedDevice.getFingerprint());
            response.setTrusted(savedDevice.getTrusted());
            response.setTrustScore(savedDevice.getTrustScore());
            response.setTrustLevel(savedDevice.getTrustLevel());
            response.setCreatedAt(savedDevice.getCreatedAt());
            
            log.info("Successfully created authenticated device {} for user {}", 
                savedDevice.getDeviceId(), userId);
            return response;
            
        } catch (Exception e) {
            log.error("Error creating authenticated device for user {}: {}", 
                userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create authenticated device", e);
        }
    }
    
    public CreateAuthenticatedDeviceResponse registerDevice(
            String userId, String deviceName, String fingerprint) {
        
        log.debug("Registering device {} for user {}", deviceName, userId);
        
        CreateAuthenticatedDeviceRequest request = new CreateAuthenticatedDeviceRequest();
        request.setDeviceName(deviceName);
        request.setFingerprint(fingerprint);
        request.setTrusted(false); // New devices start as untrusted
        
        return createAuthenticatedDevice(userId, request);
    }
    
    public CreateAuthenticatedDeviceResponse linkAnonymousDevice(
            String userId, String anonymousDeviceId, String deviceName) {
        
        log.debug("Linking anonymous device {} to user {}", anonymousDeviceId, userId);
        
        try {
            DeviceIdentity linkedDevice = createRepository.linkAnonymousDeviceToUser(
                anonymousDeviceId, userId, deviceName);
            
            CreateAuthenticatedDeviceResponse response = new CreateAuthenticatedDeviceResponse();
            response.setDeviceId(linkedDevice.getDeviceId());
            response.setUserId(linkedDevice.getUserId());
            response.setDeviceName(linkedDevice.getDeviceName());
            response.setFingerprint(linkedDevice.getFingerprint());
            response.setTrusted(linkedDevice.getTrusted());
            response.setTrustScore(linkedDevice.getTrustScore());
            response.setTrustLevel(linkedDevice.getTrustLevel());
            response.setCreatedAt(linkedDevice.getCreatedAt());
            
            log.info("Successfully linked anonymous device {} to user {}", 
                anonymousDeviceId, userId);
            return response;
            
        } catch (Exception e) {
            log.error("Error linking anonymous device {} to user {}: {}", 
                anonymousDeviceId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to link anonymous device", e);
        }
    }
    
    private Double calculateInitialTrustScore(CreateAuthenticatedDeviceRequest request) {
        double score = 0.6; // Base score for authenticated devices
        
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
        if (request.isTrusted()) {
            score += 0.2;
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