package io.strategiz.service.device;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.DeviceIdentityRepository;
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
 * Service layer for device identity management
 * Coordinates between repositories and handles business logic
 */
@Service
public class DeviceIdentityService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceIdentityService.class);
    
    private final DeviceIdentityRepository deviceRepository;
    
    @Autowired
    public DeviceIdentityService(DeviceIdentityRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    
    /**
     * Register an anonymous device (not associated with a user)
     * Used when a user first visits the site
     * 
     * @param deviceInfo Map of device information from fingerprinting
     * @param userAgent The raw user agent string
     * @return The created device identity
     */
    public DeviceIdentity registerAnonymousDevice(Map<String, Object> deviceInfo, String userAgent) {
        // Generate a secure device ID
        String deviceId = generateDeviceId();
        
        // Create a device identity with basic info
        DeviceIdentity device = new DeviceIdentity();
        device.setDeviceId(deviceId);
        device.setUserAgent(userAgent);
        device.setTrusted(false);
        
        // Extract platform information from device info if available
        if (deviceInfo != null) {
            if (deviceInfo.containsKey("platformOs")) {
                device.setPlatformOs((String) deviceInfo.get("platformOs"));
            }
            if (deviceInfo.containsKey("platformType")) {
                device.setPlatformType((String) deviceInfo.get("platformType"));
            }
            if (deviceInfo.containsKey("platformBrand")) {
                device.setPlatformBrand((String) deviceInfo.get("platformBrand"));
            }
            if (deviceInfo.containsKey("platformModel")) {
                device.setPlatformModel((String) deviceInfo.get("platformModel"));
            }
            if (deviceInfo.containsKey("platformVersion")) {
                device.setPlatformVersion((String) deviceInfo.get("platformVersion"));
            }
        }
        
        return deviceRepository.saveAnonymousDevice(device);
    }
    
    /**
     * Register an authenticated device (associated with a user)
     * Used after user signs in or signs up
     * 
     * @param deviceId The device ID (can be null for new devices)
     * @param userId The user ID to associate with
     * @param deviceName A user-friendly name for the device
     * @param deviceInfo Map of device information from fingerprinting
     * @param userAgent The raw user agent string
     * @return The registered device identity
     */
    public DeviceIdentity registerAuthenticatedDevice(
            String deviceId,
            String userId,
            String deviceName,
            Map<String, Object> deviceInfo,
            String userAgent) {
        
        // For authenticated devices, we always create in the user's subcollection
        // If an anonymous device exists with this ID, it will remain in the anonymous collection
        // This allows tracking device history across authentication states
        
        // If no device ID provided or device not found, create a new one
        deviceId = deviceId != null && !deviceId.isEmpty() ? deviceId : generateDeviceId();
        
        // Create a new device identity
        DeviceIdentity device = new DeviceIdentity();
        device.setDeviceId(deviceId);
        device.setUserId(userId);
        device.setDeviceName(deviceName);
        device.setUserAgent(userAgent);
        device.setTrusted(true);
        
        // Extract platform information from device info if available
        if (deviceInfo != null) {
            if (deviceInfo.containsKey("platformOs")) {
                device.setPlatformOs((String) deviceInfo.get("platformOs"));
            }
            if (deviceInfo.containsKey("platformType")) {
                device.setPlatformType((String) deviceInfo.get("platformType"));
            }
            if (deviceInfo.containsKey("platformBrand")) {
                device.setPlatformBrand((String) deviceInfo.get("platformBrand"));
            }
            if (deviceInfo.containsKey("platformModel")) {
                device.setPlatformModel((String) deviceInfo.get("platformModel"));
            }
            if (deviceInfo.containsKey("platformVersion")) {
                device.setPlatformVersion((String) deviceInfo.get("platformVersion"));
            }
        }
        
        return deviceRepository.saveAuthenticatedDevice(device, userId);
    }
    
    /**
     * Find a device by its device ID
     * 
     * @param deviceId The device ID to search for
     * @return Optional containing the device if found
     */
    public Optional<DeviceIdentity> findDeviceById(String deviceId) {
        // First try to find in anonymous devices
        return deviceRepository.findAnonymousDevice(deviceId);
    }
    
    /**
     * Find a device by ID for a specific user
     * 
     * @param userId The user ID
     * @param deviceId The device ID to search for
     * @return Optional containing the device if found
     */
    public Optional<DeviceIdentity> findUserDevice(String userId, String deviceId) {
        return deviceRepository.findAuthenticatedDevice(userId, deviceId);
    }
    
    /**
     * Get all devices for a user
     * 
     * @param userId The user ID to search for
     * @return List of devices associated with the user
     */
    public List<DeviceIdentity> getUserDevices(String userId) {
        return deviceRepository.findByUserId(userId);
    }
    
    /**
     * Get all trusted devices for a user
     * 
     * @param userId The user ID to search for
     * @return List of trusted devices associated with the user
     */
    public List<DeviceIdentity> getTrustedUserDevices(String userId) {
        return deviceRepository.findByUserIdAndTrustedTrue(userId);
    }
    
    /**
     * Update device platform information
     * 
     * @param deviceId The device ID to update
     * @param deviceInfo Map of device information from fingerprinting
     * @return Optional containing the updated device if found and updated
     */
    public Optional<DeviceIdentity> updateDeviceInfo(String deviceId, Map<String, Object> deviceInfo) {
        // This method would need userId to properly update authenticated devices
        // For now, we'll only update anonymous devices
        Optional<DeviceIdentity> deviceOpt = deviceRepository.findAnonymousDevice(deviceId);
        
        if (deviceOpt.isPresent() && deviceInfo != null) {
            DeviceIdentity device = deviceOpt.get();
            
            // Update platform information
            if (deviceInfo.containsKey("platformOs")) {
                device.setPlatformOs((String) deviceInfo.get("platformOs"));
            }
            if (deviceInfo.containsKey("platformType")) {
                device.setPlatformType((String) deviceInfo.get("platformType"));
            }
            if (deviceInfo.containsKey("platformBrand")) {
                device.setPlatformBrand((String) deviceInfo.get("platformBrand"));
            }
            if (deviceInfo.containsKey("platformModel")) {
                device.setPlatformModel((String) deviceInfo.get("platformModel"));
            }
            if (deviceInfo.containsKey("platformVersion")) {
                device.setPlatformVersion((String) deviceInfo.get("platformVersion"));
            }
            
            return Optional.of(deviceRepository.saveAnonymousDevice(device));
        }
        
        return Optional.empty();
    }
    
    /**
     * Update device trusted status
     * 
     * @param deviceId The device ID to update
     * @param trusted The trusted status to set
     * @return Optional containing the updated device if found and updated
     */
    /**
     * Update device trusted status for a user's device
     * 
     * @param userId The user ID
     * @param deviceId The device ID to update
     * @param trusted The trusted status to set
     * @return Optional containing the updated device if found and updated
     */
    public Optional<DeviceIdentity> updateDeviceTrustedStatus(String userId, String deviceId, boolean trusted) {
        Optional<DeviceIdentity> deviceOpt = deviceRepository.findAuthenticatedDevice(userId, deviceId);
        if (deviceOpt.isPresent()) {
            DeviceIdentity device = deviceOpt.get();
            device.setTrusted(trusted);
            return Optional.of(deviceRepository.saveAuthenticatedDevice(device, userId));
        }
        return Optional.empty();
    }
    
    /**
     * Delete a device
     * 
     * @param deviceId The device ID to delete
     * @return true if deleted, false if not found
     */
    /**
     * Delete an anonymous device
     * 
     * @param deviceId The device ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteAnonymousDevice(String deviceId) {
        return deviceRepository.deleteAnonymousDevice(deviceId);
    }
    
    /**
     * Delete a user's device
     * 
     * @param userId The user ID
     * @param deviceId The device ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteUserDevice(String userId, String deviceId) {
        return deviceRepository.deleteAuthenticatedDevice(userId, deviceId);
    }
    
    /**
     * Delete all devices for a user
     * 
     * @param userId The user ID whose devices should be deleted
     * @return The number of devices deleted
     */
    public int deleteUserDevices(String userId) {
        List<DeviceIdentity> devices = deviceRepository.findByUserId(userId);
        int count = 0;
        for (DeviceIdentity device : devices) {
            if (deviceRepository.deleteAuthenticatedDevice(userId, device.getDeviceId())) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Generate a secure device ID
     * 
     * @return A secure random device ID
     */
    private String generateDeviceId() {
        return UUID.randomUUID().toString();
    }
}
