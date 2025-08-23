package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.repository.DeleteDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.authenticated.DeleteAuthenticatedDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DeleteAuthenticatedDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteAuthenticatedDeviceService.class);
    
    private final DeleteDeviceRepository deleteRepository;
    
    @Autowired
    public DeleteAuthenticatedDeviceService(DeleteDeviceRepository deleteRepository) {
        this.deleteRepository = deleteRepository;
    }
    
    public DeleteAuthenticatedDeviceResponse deleteAuthenticatedDevice(
            String userId, String deviceId) {
        
        log.debug("Deleting authenticated device {} for user {}", deviceId, userId);
        
        try {
            boolean deleted = deleteRepository.deleteAuthenticatedDevice(userId, deviceId);
            
            DeleteAuthenticatedDeviceResponse response = new DeleteAuthenticatedDeviceResponse();
            response.setDeviceId(deviceId);
            response.setUserId(userId);
            response.setDeleted(deleted);
            response.setDeletedAt(deleted ? Instant.now() : null);
            
            if (deleted) {
                log.info("Successfully deleted authenticated device {} for user {}", 
                    deviceId, userId);
            } else {
                log.warn("Failed to delete authenticated device {} for user {}", 
                    deviceId, userId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error deleting authenticated device {} for user {}: {}", 
                deviceId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete authenticated device", e);
        }
    }
    
    public int deleteAllUserDevices(String userId) {
        log.debug("Deleting all devices for user {}", userId);
        
        try {
            int count = deleteRepository.deleteAllUserDevices(userId);
            log.info("Deleted {} devices for user {}", count, userId);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting all devices for user {}: {}", 
                userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user devices", e);
        }
    }
    
    public int deleteUntrustedDevices(String userId) {
        log.debug("Deleting untrusted devices for user {}", userId);
        
        try {
            int count = deleteRepository.deleteUntrustedDevices(userId);
            log.info("Deleted {} untrusted devices for user {}", count, userId);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting untrusted devices for user {}: {}", 
                userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete untrusted devices", e);
        }
    }
    
    public int deleteInactiveDevices(String userId, int daysInactive) {
        log.debug("Deleting devices inactive for {} days for user {}", daysInactive, userId);
        
        try {
            Instant cutoff = Instant.now().minusSeconds(daysInactive * 24L * 60 * 60);
            int count = deleteRepository.deleteInactiveAuthenticatedDevices(userId, cutoff);
            
            log.info("Deleted {} inactive devices for user {}", count, userId);
            return count;
            
        } catch (Exception e) {
            log.error("Error deleting inactive devices for user {}: {}", 
                userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete inactive devices", e);
        }
    }
    
    public int bulkDeleteDevices(String userId, String[] deviceIds) {
        log.debug("Bulk deleting {} devices for user {}", deviceIds.length, userId);
        
        try {
            int count = deleteRepository.bulkDeleteAuthenticatedDevices(userId, deviceIds);
            log.info("Deleted {} devices for user {}", count, userId);
            return count;
            
        } catch (Exception e) {
            log.error("Error bulk deleting devices for user {}: {}", 
                userId, e.getMessage(), e);
            throw new RuntimeException("Failed to bulk delete devices", e);
        }
    }
    
    public DeleteAuthenticatedDeviceResponse revokeDevice(String userId, String deviceId) {
        log.debug("Revoking device {} for user {}", deviceId, userId);
        
        // Revoke is essentially the same as delete for now
        // Could be extended to mark as revoked instead of deleting
        return deleteAuthenticatedDevice(userId, deviceId);
    }
}