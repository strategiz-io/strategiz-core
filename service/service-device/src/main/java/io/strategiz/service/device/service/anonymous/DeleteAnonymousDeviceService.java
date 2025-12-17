package io.strategiz.service.device.service.anonymous;

import io.strategiz.data.device.repository.DeleteDeviceRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.exception.DeviceErrorDetails;
import io.strategiz.service.device.model.anonymous.DeleteAnonymousDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeleteAnonymousDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteAnonymousDeviceService.class);
    
    private final DeleteDeviceRepository deleteRepository;
    
    @Autowired
    public DeleteAnonymousDeviceService(DeleteDeviceRepository deleteRepository) {
        this.deleteRepository = deleteRepository;
    }
    
    public DeleteAnonymousDeviceResponse deleteAnonymousDevice(String deviceId) {
        log.debug("Deleting anonymous device: {}", deviceId);
        
        try {
            boolean deleted = deleteRepository.deleteAnonymousDevice(deviceId);
            
            DeleteAnonymousDeviceResponse response = new DeleteAnonymousDeviceResponse();
            response.setDeviceId(deviceId);
            response.setDeleted(deleted);
            response.setDeletedAt(deleted ? Instant.now() : null);
            
            if (deleted) {
                log.info("Successfully deleted anonymous device: {}", deviceId);
            } else {
                log.warn("Failed to delete anonymous device: {}", deviceId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error deleting anonymous device {}: {}", deviceId, e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_DELETE_FAILED, "service-device", e, deviceId);
        }
    }
    
    public int deleteInactiveDevices(int daysInactive) {
        log.debug("Deleting anonymous devices inactive for {} days", daysInactive);
        
        try {
            Instant cutoff = Instant.now().minusSeconds(daysInactive * 24L * 60 * 60);
            int count = deleteRepository.deleteInactiveAnonymousDevices(cutoff);
            
            log.info("Deleted {} inactive anonymous devices", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting inactive devices: {}", e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_DELETE_FAILED, "service-device", e);
        }
    }
    
    public int deleteSuspiciousDevices() {
        log.debug("Deleting suspicious anonymous devices");
        
        try {
            int count = deleteRepository.deleteSuspiciousAnonymousDevices();
            log.info("Deleted {} suspicious anonymous devices", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting suspicious devices: {}", e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_DELETE_FAILED, "service-device", e);
        }
    }
    
    public int deleteBlockedDevices() {
        log.debug("Deleting blocked anonymous devices");
        
        try {
            int count = deleteRepository.deleteBlockedAnonymousDevices();
            log.info("Deleted {} blocked anonymous devices", count);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting blocked devices: {}", e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_DELETE_FAILED, "service-device", e);
        }
    }
    
    public DeleteAnonymousDeviceResponse purgeAllAnonymousDevices() {
        log.warn("Purging all anonymous devices");
        
        try {
            int count = deleteRepository.deleteAllAnonymousDevices();
            
            DeleteAnonymousDeviceResponse response = new DeleteAnonymousDeviceResponse();
            response.setDeleted(count > 0);
            response.setDeletedAt(Instant.now());
            response.setMessage("Purged " + count + " anonymous devices");
            
            log.info("Purged {} anonymous devices", count);
            return response;
            
        } catch (Exception e) {
            log.error("Error purging anonymous devices: {}", e.getMessage(), e);
            throw new StrategizException(DeviceErrorDetails.DEVICE_DELETE_FAILED, "service-device", e);
        }
    }
}