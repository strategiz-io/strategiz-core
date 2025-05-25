package io.strategiz.service.auth;

import io.strategiz.data.auth.DeviceIdentity;
import io.strategiz.data.auth.DeviceIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for device identity management
 */
@Service
public class DeviceIdentityService {

    private static final Logger log = LoggerFactory.getLogger(DeviceIdentityService.class);
    
    private final DeviceIdentityRepository deviceIdentityRepository;
    
    @Autowired
    public DeviceIdentityService(DeviceIdentityRepository deviceIdentityRepository) {
        this.deviceIdentityRepository = deviceIdentityRepository;
    }
    
    /**
     * Register a new device identity
     *
     * @param userId User ID
     * @param deviceId Device ID
     * @param deviceName User-friendly device name
     * @return The registered device identity
     */
    public DeviceIdentity registerDevice(String userId, String deviceId, String deviceName) {
        // Call the full method with default values
        return registerDevice(userId, deviceId, "", deviceName, null, true);
    }
    
    /**
     * Register a new device identity
     *
     * @param userId User ID
     * @param deviceId Device ID
     * @param publicKey Public key for the device
     * @param name User-friendly device name
     * @param deviceInfo Device information
     * @param trusted Whether the device is trusted
     * @return The registered device identity
     */
    public DeviceIdentity registerDevice(
            String userId, 
            String deviceId, 
            String publicKey, 
            String name, 
            Map<String, Object> deviceInfo, 
            boolean trusted) {
        
        log.info("Registering new device for user: {}, device ID: {}", userId, deviceId);
        
        // Check if device already exists for this user
        Optional<DeviceIdentity> existingDevice = deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId);
        
        if (existingDevice.isPresent()) {
            log.info("Device already registered for user: {}, device ID: {}", userId, deviceId);
            DeviceIdentity device = existingDevice.get();
            
            // Update existing device
            device.setPublicKey(publicKey);
            device.setName(name);
            device.setDeviceInfo(deviceInfo);
            device.setTrusted(trusted);
            device.updateLastUsedTime();
            
            return deviceIdentityRepository.save(device);
        }
        
        // Create new device
        long now = Instant.now().getEpochSecond();
        DeviceIdentity device = DeviceIdentity.builder()
                .userId(userId)
                .deviceId(deviceId)
                .publicKey(publicKey)
                .name(name)
                .deviceInfo(deviceInfo)
                .createdAt(now)
                .lastUsedAt(now)
                .trusted(trusted)
                .build();
        
        return deviceIdentityRepository.save(device);
    }
    
    /**
     * Get a device identity by ID
     *
     * @param deviceIdentityId Device identity ID
     * @return Optional containing the device identity if found
     */
    public Optional<DeviceIdentity> getDeviceById(String deviceIdentityId) {
        log.debug("Getting device by ID: {}", deviceIdentityId);
        return deviceIdentityRepository.findById(deviceIdentityId);
    }
    
    /**
     * Get a device identity by device ID
     *
     * @param deviceId Device ID
     * @return Optional containing the device identity if found
     */
    public Optional<DeviceIdentity> getDeviceByDeviceId(String deviceId) {
        log.debug("Getting device by device ID: {}", deviceId);
        return deviceIdentityRepository.findByDeviceId(deviceId);
    }
    
    /**
     * Get a device identity by device ID and user ID
     *
     * @param deviceId Device ID
     * @param userId User ID
     * @return Optional containing the device identity if found
     */
    public Optional<DeviceIdentity> getDeviceByDeviceIdAndUserId(String deviceId, String userId) {
        log.debug("Getting device by device ID and user ID: {}, {}", deviceId, userId);
        return deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId);
    }
    
    /**
     * Get all devices for a user
     *
     * @param userId User ID
     * @return List of device identities
     */
    public List<DeviceIdentity> getUserDevices(String userId) {
        log.info("Getting devices for user: {}", userId);
        return deviceIdentityRepository.findAllByUserId(userId);
    }
    
    /**
     * Update a device
     *
     * @param deviceIdentity Device identity to update
     * @return Updated device identity
     */
    public DeviceIdentity updateDevice(DeviceIdentity deviceIdentity) {
        log.info("Updating device: {}", deviceIdentity.getId());
        deviceIdentity.updateLastUsedTime();
        return deviceIdentityRepository.save(deviceIdentity);
    }
    
    /**
     * Delete a device
     *
     * @param deviceIdentityId Device identity ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteDevice(String deviceIdentityId) {
        log.info("Deleting device: {}", deviceIdentityId);
        return deviceIdentityRepository.deleteById(deviceIdentityId);
    }
    
    /**
     * Delete all devices for a user
     *
     * @param userId User ID
     * @return Number of deleted devices
     */
    public int deleteUserDevices(String userId) {
        log.info("Deleting all devices for user: {}", userId);
        return deviceIdentityRepository.deleteAllByUserId(userId);
    }
    
    /**
     * Generate a new device ID
     *
     * @return Generated device ID
     */
    public String generateDeviceId() {
        return UUID.randomUUID().toString();
    }
}
