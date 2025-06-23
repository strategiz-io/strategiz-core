package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository implementation for updating device identity entities
 * Following Single Responsibility Principle - focused only on update operations
 */
@Repository
public class UpdateDeviceIdentityRepository {
    
    private static final Logger log = LoggerFactory.getLogger(UpdateDeviceIdentityRepository.class);
    
    private final DeviceIdentityRepository repository;
    
    public UpdateDeviceIdentityRepository(DeviceIdentityRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Update an existing device identity
     * 
     * @param deviceIdentity The device identity with updated fields
     * @return The updated device identity
     */
    public DeviceIdentity update(DeviceIdentity deviceIdentity) {
        try {
            // Always update the last seen timestamp
            deviceIdentity.setLastSeen(Instant.now());
            
            return repository.save(deviceIdentity);
        } catch (Exception e) {
            log.error("Error updating device identity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update device identity", e);
        }
    }
    
    /**
     * Update a device's trusted status
     * 
     * @param deviceId The device ID
     * @param trusted The new trusted status
     * @return The updated device identity, or empty if not found
     */
    public Optional<DeviceIdentity> updateTrustedStatus(String deviceId, boolean trusted) {
        try {
            Optional<DeviceIdentity> deviceOpt = repository.findById(deviceId);
            
            if (deviceOpt.isPresent()) {
                DeviceIdentity device = deviceOpt.get();
                device.setTrusted(trusted);
                device.setLastSeen(Instant.now());
                return Optional.of(repository.save(device));
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error updating trusted status for device {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to update device trusted status", e);
        }
    }
    
    /**
     * Associate a device with a user ID
     * 
     * @param deviceId The device ID
     * @param userId The user ID to associate
     * @return The updated device identity, or empty if not found
     */
    public Optional<DeviceIdentity> associateWithUser(String deviceId, String userId) {
        try {
            Optional<DeviceIdentity> deviceOpt = repository.findById(deviceId);
            
            if (deviceOpt.isPresent()) {
                DeviceIdentity device = deviceOpt.get();
                device.setUserId(userId);
                device.setTrusted(true); // Trust device when associating with a user
                device.setLastSeen(Instant.now());
                return Optional.of(repository.save(device));
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error associating device {} with user {}: {}", deviceId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to associate device with user", e);
        }
    }
    
    /**
     * Update device platform information
     * 
     * @param deviceId The device ID
     * @param platformOs OS name
     * @param platformType Device type
     * @param platformBrand Device brand
     * @param platformModel Device model
     * @param platformVersion OS version
     * @return The updated device identity, or empty if not found
     */
    public Optional<DeviceIdentity> updatePlatformInfo(
            String deviceId, 
            String platformOs,
            String platformType, 
            String platformBrand,
            String platformModel,
            String platformVersion) {
        
        try {
            Optional<DeviceIdentity> deviceOpt = repository.findById(deviceId);
            
            if (deviceOpt.isPresent()) {
                DeviceIdentity device = deviceOpt.get();
                device.setPlatformOs(platformOs);
                device.setPlatformType(platformType);
                device.setPlatformBrand(platformBrand);
                device.setPlatformModel(platformModel);
                device.setPlatformVersion(platformVersion);
                device.setLastSeen(Instant.now());
                return Optional.of(repository.save(device));
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error updating platform info for device {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to update device platform information", e);
        }
    }
}
