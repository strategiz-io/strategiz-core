package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for reading device identity data
 * Following Single Responsibility Principle - focused only on read operations
 */
@Repository
public class ReadDeviceIdentityRepository {
    
    private static final Logger log = LoggerFactory.getLogger(ReadDeviceIdentityRepository.class);
    
    private final DeviceIdentityRepository repository;
    
    public ReadDeviceIdentityRepository(DeviceIdentityRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Find a device by its unique device ID
     * 
     * @param deviceId The device ID to search for
     * @return Optional containing the device if found
     */
    public Optional<DeviceIdentity> findByDeviceId(String deviceId) {
        try {
            List<DeviceIdentity> devices = repository.findByDeviceId(deviceId);
            return devices.isEmpty() ? Optional.empty() : Optional.of(devices.get(0));
        } catch (Exception e) {
            log.error("Error finding device by deviceId {}: {}", deviceId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Find all devices for a given user
     * 
     * @param userId The user ID to search for
     * @return List of devices associated with the user
     */
    public List<DeviceIdentity> findByUserId(String userId) {
        try {
            return repository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Error finding devices for userId {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Find a device identity by its ID
     * 
     * @param id The ID of the device identity
     * @return Optional containing the device identity if found
     */
    public Optional<DeviceIdentity> findById(String id) {
        try {
            return repository.findById(id);
        } catch (Exception e) {
            log.error("Error finding device by id {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Find all trusted devices for a specific user
     * 
     * @param userId The user ID
     * @return List of trusted devices for the user
     */
    public List<DeviceIdentity> findTrustedDevicesByUserId(String userId) {
        try {
            return repository.findByUserIdAndTrustedTrue(userId);
        } catch (Exception e) {
            log.error("Error finding trusted devices for userId {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Find all device identities
     * 
     * @return List of all device identities
     */
    public List<DeviceIdentity> findAll() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error finding all devices: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
