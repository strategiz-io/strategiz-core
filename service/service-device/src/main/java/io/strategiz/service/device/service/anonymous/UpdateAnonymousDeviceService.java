package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.ReadDeviceRepository;
import io.strategiz.data.device.repository.UpdateDeviceRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.exception.DeviceErrorDetails;
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
            Optional<DeviceIdentity> existingDevice = readRepository.findAnonymousDevice(deviceId);
            if (existingDevice.isEmpty()) {
                throw new StrategizException(DeviceErrorDetails.DEVICE_NOT_FOUND, "service-device", deviceId);
            }
            
            DeviceIdentity device = existingDevice.get();
            
            // Update fields based on UpdateAnonymousDeviceRequest
            if (request.getVisitorId() != null) {
                device.setVisitorId(request.getVisitorId());
            }
            if (request.getVisitorId() != null) {
                device.setVisitorId(request.getVisitorId());
            }
            if (request.getPlatform() != null) {
                device.setPlatform(request.getPlatform());
            }
            if (request.getBrowserName() != null) {
                device.setBrowserName(request.getBrowserName());
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
            // Update comprehensive fingerprint fields
            if (request.getConfidence() != null) {
                device.setFingerprintConfidence(request.getConfidence());
            }
            if (request.getCanvasFingerprint() != null) {
                device.setCanvasFingerprint(request.getCanvasFingerprint());
            }
            if (request.getPublicKey() != null) {
                device.setPublicKey(request.getPublicKey());
            }
            
            device.setLastSeen(Instant.now());
            // BaseEntity handles updated date automatically
            
            // Save updates
            Optional<DeviceIdentity> updatedDevice = updateRepository.updateAnonymousDevice(device);

            if (updatedDevice.isEmpty()) {
                throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", deviceId);
            }
            
            // Create response
            UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
            response.setDeviceId(updatedDevice.get().getDeviceId());
            response.setVisitorId(updatedDevice.get().getVisitorId());
            response.setTrustScore((double) updatedDevice.get().getTrustScore());
            response.setTrustLevel(updatedDevice.get().getTrustLevel());
            response.setUpdatedAt(Instant.now());
            
            log.info("Successfully updated anonymous device: {}", deviceId);
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating anonymous device {}: {}", deviceId, e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", e, deviceId);
        }
    }
    
    public UpdateAnonymousDeviceResponse updateTrustLevel(String deviceId, String trustLevel) {
        log.debug("Updating trust level for device {} to {}", deviceId, trustLevel);

        Optional<DeviceIdentity> updated = updateRepository.updateAnonymousDeviceTrustLevel(
            deviceId, trustLevel);

        if (updated.isEmpty()) {
            throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", deviceId);
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setTrustLevel(updated.get().getTrustLevel());
        response.setUpdatedAt(Instant.now());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse markSuspicious(String deviceId, String reason) {
        log.debug("Marking device {} as suspicious: {}", deviceId, reason);

        Optional<DeviceIdentity> updated = updateRepository.markAnonymousDeviceSuspicious(
            deviceId, reason);

        if (updated.isEmpty()) {
            throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", deviceId);
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setTrustLevel("LOW");
        response.setUpdatedAt(Instant.now());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse blockDevice(String deviceId, String reason) {
        log.debug("Blocking device {}: {}", deviceId, reason);

        Optional<DeviceIdentity> updated = updateRepository.blockAnonymousDevice(deviceId, reason);

        if (updated.isEmpty()) {
            throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", deviceId);
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setBlocked(true);
        response.setUpdatedAt(Instant.now());
        
        return response;
    }
    
    public UpdateAnonymousDeviceResponse unblockDevice(String deviceId) {
        log.debug("Unblocking device {}", deviceId);

        Optional<DeviceIdentity> updated = updateRepository.unblockAnonymousDevice(deviceId);

        if (updated.isEmpty()) {
            throw new StrategizException(DeviceErrorDetails.DEVICE_UPDATE_FAILED, "service-device", deviceId);
        }
        
        UpdateAnonymousDeviceResponse response = new UpdateAnonymousDeviceResponse();
        response.setDeviceId(updated.get().getDeviceId());
        response.setBlocked(false);
        response.setUpdatedAt(Instant.now());
        
        return response;
    }
    
    private String determineTrustLevel(int trustScore) {
        if (trustScore >= 80) return "HIGH";
        if (trustScore >= 50) return "MEDIUM";
        if (trustScore >= 30) return "LOW";
        return "VERY_LOW";
    }
}