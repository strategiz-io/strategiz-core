package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Repository implementation for deleting device identity entities
 * Following Single Responsibility Principle - focused only on delete operations
 */
@Repository
public class DeleteDeviceIdentityRepository {
    
    private static final Logger log = LoggerFactory.getLogger(DeleteDeviceIdentityRepository.class);
    
    private final DeviceIdentityRepository repository;
    
    public DeleteDeviceIdentityRepository(DeviceIdentityRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Delete a device identity by ID
     * 
     * @param id The ID of the device identity to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteById(String id) {
        try {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting device identity with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete device identity", e);
        }
    }
    
    /**
     * Delete a device identity entity
     * 
     * @param deviceIdentity The device identity to delete
     */
    public void delete(DeviceIdentity deviceIdentity) {
        try {
            repository.delete(deviceIdentity);
        } catch (Exception e) {
            log.error("Error deleting device identity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete device identity", e);
        }
    }
    
    /**
     * Delete a device by its device ID
     * 
     * @param deviceId The device ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteByDeviceId(String deviceId) {
        try {
            Optional<DeviceIdentity> deviceOpt = repository.findByDeviceId(deviceId)
                .stream()
                .findFirst();
            
            if (deviceOpt.isPresent()) {
                repository.delete(deviceOpt.get());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error deleting device with device ID {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete device by device ID", e);
        }
    }
    
    /**
     * Delete all devices for a specific user
     * 
     * @param userId The user ID whose devices should be deleted
     * @return The number of devices deleted
     */
    public int deleteAllByUserId(String userId) {
        try {
            int count = 0;
            for (DeviceIdentity device : repository.findByUserId(userId)) {
                repository.delete(device);
                count++;
            }
            return count;
        } catch (Exception e) {
            log.error("Error deleting devices for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete devices for user", e);
        }
    }
}
