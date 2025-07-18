package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository implementation for creating device identity entities
 * Following Single Responsibility Principle - focused only on create operations
 */
@Repository
public class CreateDeviceIdentityRepository {
    
    private static final Logger log = LoggerFactory.getLogger(CreateDeviceIdentityRepository.class);
    
    private final DeviceIdentityRepository repository;
    
    public CreateDeviceIdentityRepository(DeviceIdentityRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Create a new device identity
     * 
     * @param deviceIdentity The device identity to create
     * @return The created device identity
     */
    public DeviceIdentity create(DeviceIdentity deviceIdentity) {
        try {
            // Generate UUID if not provided
            if (deviceIdentity.getId() == null || deviceIdentity.getId().isEmpty()) {
                deviceIdentity.setId(UUID.randomUUID().toString());
            }
            
            // Ensure timestamps are set
            if (deviceIdentity.getFirstSeen() == null) {
                deviceIdentity.setFirstSeen(Instant.now());
            }
            
            deviceIdentity.setLastSeen(Instant.now());
            
            return repository.save(deviceIdentity, deviceIdentity.getUserId() != null ? deviceIdentity.getUserId() : "anonymous");
        } catch (Exception e) {
            log.error("Error creating device identity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create device identity", e);
        }
    }
    
    /**
     * Create a new anonymous device
     * 
     * @param deviceId The device ID
     * @return The created device identity
     */
    public DeviceIdentity createAnonymous(String deviceId) {
        DeviceIdentity device = new DeviceIdentity();
        device.setDeviceId(deviceId);
        device.setFirstSeen(Instant.now());
        device.setLastSeen(Instant.now());
        device.setTrusted(false);
        return create(device);
    }
    
    /**
     * Create a new authenticated device
     * 
     * @param deviceId The device ID
     * @param userId The user ID
     * @param deviceName Optional device name
     * @return The created device identity
     */
    public DeviceIdentity createAuthenticated(String deviceId, String userId, String deviceName) {
        DeviceIdentity device = new DeviceIdentity();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setDeviceName(deviceName);
        device.setFirstSeen(Instant.now());
        device.setLastSeen(Instant.now());
        device.setTrusted(true);
        return create(device);
    }
}
