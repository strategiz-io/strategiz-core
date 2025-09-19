package io.strategiz.service.device.service.authenticated;

import io.strategiz.data.device.repository.DeleteDeviceRepository;
import io.strategiz.service.base.service.BaseService;
import io.strategiz.service.device.model.authenticated.DeleteAuthenticatedDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeleteAuthenticatedDeviceService extends BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteAuthenticatedDeviceService.class);
    
    private final DeleteDeviceRepository deleteRepository;
    
    @Autowired
    public DeleteAuthenticatedDeviceService(DeleteDeviceRepository deleteRepository) {
        this.deleteRepository = deleteRepository;
    }
    
    /**
     * Delete a single device
     */
    public boolean deleteDevice(String userId, String deviceId) {
        log.debug("Deleting device {} for user {}", deviceId, userId);
        try {
            return deleteRepository.deleteAuthenticatedDevice(userId, deviceId);
        } catch (Exception e) {
            log.error("Error deleting device {} for user {}: {}", deviceId, userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Delete all devices for a user
     */
    public int deleteAllDevices(String userId) {
        log.debug("Deleting all devices for user {}", userId);
        try {
            return deleteRepository.deleteAllUserDevices(userId);
        } catch (Exception e) {
            log.error("Error deleting all devices for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Delete all devices except the current one
     */
    public int deleteAllExceptCurrent(String userId, String currentVisitorId) {
        log.debug("Deleting all devices except current for user {}", userId);
        try {
            return deleteRepository.deleteAllUserDevicesExcept(userId, currentVisitorId);
        } catch (Exception e) {
            log.error("Error deleting devices except current for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Delete multiple devices by IDs
     */
    public Map<String, Boolean> deleteMultipleDevices(String userId, List<String> deviceIds) {
        log.debug("Deleting {} devices for user {}", deviceIds.size(), userId);
        Map<String, Boolean> results = new HashMap<>();
        for (String deviceId : deviceIds) {
            try {
                boolean deleted = deleteRepository.deleteAuthenticatedDevice(userId, deviceId);
                results.put(deviceId, deleted);
            } catch (Exception e) {
                log.error("Error deleting device {} for user {}: {}", deviceId, userId, e.getMessage());
                results.put(deviceId, false);
            }
        }
        return results;
    }
    
    /**
     * Delete untrusted devices
     */
    public int deleteUntrustedDevices(String userId) {
        log.debug("Deleting untrusted devices for user {}", userId);
        try {
            return deleteRepository.deleteUntrustedUserDevices(userId);
        } catch (Exception e) {
            log.error("Error deleting untrusted devices for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Delete inactive devices
     */
    public int deleteInactiveDevices(String userId, int daysInactive) {
        log.debug("Deleting inactive devices for user {} (inactive > {} days)", userId, daysInactive);
        try {
            Instant cutoff = Instant.now().minus(daysInactive, ChronoUnit.DAYS);
            return deleteRepository.deleteInactiveUserDevices(userId, cutoff);
        } catch (Exception e) {
            log.error("Error deleting inactive devices for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
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
    
    
    public DeleteAuthenticatedDeviceResponse revokeDevice(String userId, String deviceId) {
        log.debug("Revoking device {} for user {}", deviceId, userId);
        
        // Revoke is essentially the same as delete for now
        // Could be extended to mark as revoked instead of deleting
        return deleteAuthenticatedDevice(userId, deviceId);
    }
}