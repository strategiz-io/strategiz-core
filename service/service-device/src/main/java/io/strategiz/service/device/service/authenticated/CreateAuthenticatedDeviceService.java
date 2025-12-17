package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.exception.DeviceErrorDetails;
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
    
    /**
     * Create an authenticated device for a user during sign-up step 2 or sign-in
     * Stores in users/{userId}/devices collection
     */
    public CreateAuthenticatedDeviceResponse createAuthenticatedDevice(
            String userId, CreateAuthenticatedDeviceRequest request) {
        
        log.debug("Creating authenticated device for user: {}", userId);
        
        try {
            // Create device entity with FingerprintJS and Web Crypto data
            DeviceIdentity device = new DeviceIdentity();
            device.setDeviceId(UUID.randomUUID().toString());
            device.setUserId(userId);
            device.setDeviceName(request.getDeviceName());
            
            // FingerprintJS data
            device.setVisitorId(request.getVisitorId());
            device.setFingerprintConfidence(request.getConfidence());
            device.setCanvasFingerprint(request.getCanvasFingerprint());
            
            // Web Crypto API public key
            device.setPublicKey(request.getPublicKey());
            
            // Browser info
            device.setBrowserName(request.getBrowserName());
            device.setBrowserVersion(request.getBrowserVersion());
            device.setUserAgent(request.getUserAgent());
            
            // OS info
            device.setOsName(request.getOsName());
            device.setOsVersion(request.getOsVersion());
            device.setPlatform(request.getPlatform());
            
            // Hardware info
            device.setScreenResolution(request.getScreenResolution());
            device.setHardwareConcurrency(request.getHardwareConcurrency());
            
            // Network info
            device.setTimezone(request.getTimezone());
            device.setLanguage(request.getLanguage());
            device.setLanguages(request.getLanguages());
            
            // Trust indicators
            device.setIncognitoMode(request.getIncognito());
            device.setTrusted(request.getTrusted() != null ? request.getTrusted() : false);
            device.setTrustLevel("pending");
            
            // Timestamps
            device.setFirstSeen(Instant.now());
            device.setLastSeen(Instant.now());
            
            // Save to repository (pass device first, then userId)
            DeviceIdentity savedDevice = createRepository.createAuthenticatedDevice(device, userId);
            
            // Create response
            CreateAuthenticatedDeviceResponse response = new CreateAuthenticatedDeviceResponse();
            response.setDeviceId(savedDevice.getDeviceId());
            response.setUserId(savedDevice.getUserId());
            response.setDeviceName(savedDevice.getDeviceName());
            response.setVisitorId(savedDevice.getVisitorId());
            response.setTrusted(savedDevice.isTrusted());
            response.setTrustScore(savedDevice.getTrustScore());
            response.setTrustLevel(savedDevice.getTrustLevel());
            response.setCreatedAt(savedDevice.getCreatedAt());
            
            log.info("Successfully created authenticated device {} for user {}", 
                savedDevice.getDeviceId(), userId);
            return response;
            
        } catch (Exception e) {
            log.error("Error creating authenticated device for user {}: {}",
                userId, e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_REGISTRATION_FAILED, "service-device", e, userId);
        }
    }
}
