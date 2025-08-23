package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import io.strategiz.data.device.repository.UpdateDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.anonymous.UpdateAnonymousDeviceRequest;
import io.strategiz.service.device.model.anonymous.UpdateAnonymousDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UpdateAnonymousDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(UpdateAnonymousDeviceService.class);
    
    private final UpdateDeviceRepository updateRepository;
    private final ReadDeviceRepository readRepository;
    
    @Autowired
    public UpdateAnonymousDeviceService(
            UpdateDeviceRepository updateRepository,
            ReadDeviceRepository readRepository) {
        this.updateRepository = updateRepository;
        this.readRepository = readRepository;
    }
    
    public UpdateAnonymousDeviceResponse updateAnonymousDevice(
            String deviceId, UpdateAnonymousDeviceRequest request) {
        
        log.debug("Updating anonymous device: {}", deviceId);
        
        try {
            // Get existing device
            Optional<DeviceIdentity> existingDevice = readRepository.getAnonymousDevice(deviceId);
            if (existingDevice.isEmpty()) {
                throw new RuntimeException("Device not found: " + deviceId);
            }
            
            DeviceIdentity device = existingDevice.get();
            
            // Update fields
            if (request.getFingerprint() != null) {
                device.setFingerprint(request.getFingerprint());
            }
            if (request.getVisitorId() != null) {
                device.setVisitorId(request.getVisitorId());
            }
            if (request.getPlatform() != null) {
                device.setPlatform(request.getPlatform());
            }
            if (request.getBrowser() != null) {
                device.setBrowserName(request.getBrowser());
            }
            if (request.getUserAgent() != null) {
                device.setUserAgent(request.getUserAgent());
            }
            if (request.getIpAddress() != null) {
                device.setIpAddress(request.getIpAddress());
            }
            if (request.getScreenResolution() != null) {
                device.setScreenResolution(request.getScreenResolution());
            }
            if (request.getTimezone() != null) {
                device.setTimezone(request.getTimezone());
            }
            if (request.getLanguage() != null) {
                device.setLanguage(request.getLanguage());
            }
            if (request.getOsName() != null) {
                device.setOsName(request.getOsName());
            }
            if (request.getOsVersion() != null) {
                device.setOsVersion(request.getOsVersion());
            }
            if (request.getTrustScore() != null) {
                device.setTrustScore(request.getTrustScore());
                device.setTrustLevel(determineTrustLevel(request.getTrustScore()));
            }
            
            device.setLastSeen(Instant.now());
            device.setUpdatedAt(Instant.now());
            
            // Save updates
            Optional<DeviceIdentity> updatedDevice = updateRepository.updateAnonymousDevice(device);
            
            if (updatedDevice.isEmpty()) {
                throw new RuntimeException("Failed to update device");
            }
            
            // Create response
            UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
            response.setDeviceId(updatedDevice.get().getDeviceId());
            response.setFingerprint(updatedDevice.get().getFingerprint());
            response.setTrustScore(updatedDevice.get().getTrustScore());
            response.setTrustLevel(updatedDevice.get().getTrustLevel());
            response.setUpdatedAt(updatedDevice.get().getUpdatedAt());
            
            log.info("Successfully updated anonymous device: {}", deviceId);
            return response;
            
        } catch (Exception e) {
            log.error("Error updating anonymous device {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to update anonymous device", e);
        }
    }
    
    public UpdateAnonymousDeviceResponse updateTrustLevel(String deviceId, String trustLevel) {
        log.debug("Updating trust level for device {} to {}", deviceId, trustLevel);
        
        Optional<DeviceIdentity> updated = updateRepository.updateAnonymousDeviceTrustLevel(
            deviceId, trustLevel);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to update trust level");
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setTrustLevel(updated.get().getTrustLevel());
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse markSuspicious(String deviceId, String reason) {
        log.debug("Marking device {} as suspicious: {}", deviceId, reason);
        
        Optional<DeviceIdentity> updated = updateRepository.markAnonymousDeviceSuspicious(
            deviceId, reason);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to mark device as suspicious");
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setTrustLevel("LOW");
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse blockDevice(String deviceId, String reason) {
        log.debug("Blocking device {}: {}", deviceId, reason);
        
        Optional<DeviceIdentity> updated = updateRepository.blockAnonymousDevice(deviceId, reason);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to block device");
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setBlocked(true);
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse unblockDevice(String deviceId) {
        log.debug("Unblocking device {}", deviceId);
        
        Optional<DeviceIdentity> updated = updateRepository.unblockAnonymousDevice(deviceId);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to unblock device");
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setBlocked(false);
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    private String determineTrustLevel(Double trustScore) {
        if (trustScore == null) return "UNKNOWN";
        if (trustScore >= 0.8) return "HIGH";
        if (trustScore >= 0.5) return "MEDIUM";
        if (trustScore >= 0.3) return "LOW";
        return "VERY_LOW";
    }
}