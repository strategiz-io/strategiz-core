package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import io.strategiz.data.device.repository.UpdateDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.authenticated.UpdateAuthenticatedDeviceRequest;
import io.strategiz.service.device.model.authenticated.UpdateAuthenticatedDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UpdateAuthenticatedDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(UpdateAuthenticatedDeviceService.class);
    
    private final UpdateDeviceRepository updateRepository;
    private final ReadDeviceRepository readRepository;
    
    @Autowired
    public UpdateAuthenticatedDeviceService(
            UpdateDeviceRepository updateRepository,
            ReadDeviceRepository readRepository) {
        this.updateRepository = updateRepository;
        this.readRepository = readRepository;
    }
    
    public UpdateAuthenticatedDeviceResponse updateAuthenticatedDevice(
            String userId, String deviceId, UpdateAuthenticatedDeviceRequest request) {
        
        log.debug("Updating authenticated device {} for user {}", deviceId, userId);
        
        try {
            // Get existing device
            Optional<DeviceIdentity> existingDevice = readRepository.getAuthenticatedDevice(
                userId, deviceId);
            
            if (existingDevice.isEmpty()) {
                throw new RuntimeException("Device not found: " + deviceId);
            }
            
            DeviceIdentity device = existingDevice.get();
            
            // Update fields
            if (request.getDeviceName() != null) {
                device.setDeviceName(request.getDeviceName());
            }
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
            if (request.getTrusted() != null) {
                device.setTrusted(request.getTrusted());
            }
            if (request.getTrustScore() != null) {
                device.setTrustScore(request.getTrustScore());
                device.setTrustLevel(determineTrustLevel(request.getTrustScore()));
            }
            
            device.setLastSeen(Instant.now());
            device.setUpdatedAt(Instant.now());
            
            // Save updates
            Optional<DeviceIdentity> updatedDevice = updateRepository.updateAuthenticatedDevice(
                userId, device);
            
            if (updatedDevice.isEmpty()) {
                throw new RuntimeException("Failed to update device");
            }
            
            // Create response
            UpdateAuthenticatedDeviceResponse response = new UpdateAuthenticatedDeviceResponse();
            response.setDeviceId(updatedDevice.get().getDeviceId());
            response.setUserId(updatedDevice.get().getUserId());
            response.setDeviceName(updatedDevice.get().getDeviceName());
            response.setFingerprint(updatedDevice.get().getFingerprint());
            response.setTrusted(updatedDevice.get().getTrusted());
            response.setTrustScore(updatedDevice.get().getTrustScore());
            response.setTrustLevel(updatedDevice.get().getTrustLevel());
            response.setUpdatedAt(updatedDevice.get().getUpdatedAt());
            
            log.info("Successfully updated authenticated device {} for user {}", deviceId, userId);
            return response;
            
        } catch (Exception e) {
            log.error("Error updating authenticated device {} for user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update authenticated device", e);
        }
    }
    
    public UpdateAuthenticatedDeviceResponse updateDeviceTrust(
            String userId, String deviceId, boolean trusted) {
        
        log.debug("Updating trust status for device {} to {}", deviceId, trusted);
        
        Optional<DeviceIdentity> updated = updateRepository.updateDeviceTrust(
            userId, deviceId, trusted);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to update device trust");
        }
        
        UpdateAuthenticatedDeviceResponse response = new UpdateAuthenticatedDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setUserId(updated.get().getUserId());
        response.setTrusted(updated.get().getTrusted());
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAuthenticatedDeviceResponse updateDeviceName(
            String userId, String deviceId, String deviceName) {
        
        log.debug("Updating name for device {} to {}", deviceId, deviceName);
        
        Optional<DeviceIdentity> updated = updateRepository.updateDeviceName(
            userId, deviceId, deviceName);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to update device name");
        }
        
        UpdateAuthenticatedDeviceResponse response = new UpdateAuthenticatedDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setUserId(updated.get().getUserId());
        response.setDeviceName(updated.get().getDeviceName());
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAuthenticatedDeviceResponse updateTrustLevel(
            String userId, String deviceId, String trustLevel) {
        
        log.debug("Updating trust level for device {} to {}", deviceId, trustLevel);
        
        Optional<DeviceIdentity> updated = updateRepository.updateDeviceTrustLevel(
            userId, deviceId, trustLevel);
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to update trust level");
        }
        
        UpdateAuthenticatedDeviceResponse response = new UpdateAuthenticatedDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setUserId(updated.get().getUserId());
        response.setTrustLevel(updated.get().getTrustLevel());
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public UpdateAuthenticatedDeviceResponse updateLastSeen(
            String userId, String deviceId) {
        
        log.debug("Updating last seen for device {}", deviceId);
        
        Optional<DeviceIdentity> updated = updateRepository.updateDeviceLastSeen(
            userId, deviceId, Instant.now());
        
        if (updated.isEmpty()) {
            throw new RuntimeException("Failed to update last seen");
        }
        
        UpdateAuthenticatedDeviceResponse response = new UpdateAuthenticatedDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setUserId(updated.get().getUserId());
        response.setUpdatedAt(updated.get().getUpdatedAt());
        
        return response;
    }
    
    public int bulkUpdateDevices(String userId, String[] deviceIds, Map<String, Object> updates) {
        log.debug("Bulk updating {} devices for user {}", deviceIds.length, userId);
        
        return updateRepository.bulkUpdateAuthenticatedDevices(userId, deviceIds, updates);
    }
    
    public int updateAllUserDevices(String userId, Map<String, Object> updates) {
        log.debug("Updating all devices for user {}", userId);
        
        // Update all devices without specific criteria
        Map<String, Object> criteria = new HashMap<>();
        return updateRepository.updateAuthenticatedDevicesWhere(userId, criteria, updates);
    }
    
    private String determineTrustLevel(Double trustScore) {
        if (trustScore == null) return "UNKNOWN";
        if (trustScore >= 0.8) return "HIGH";
        if (trustScore >= 0.5) return "MEDIUM";
        if (trustScore >= 0.3) return "LOW";
        return "VERY_LOW";
    }
}