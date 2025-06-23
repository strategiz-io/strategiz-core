package io.strategiz.service.device;

import io.strategiz.data.device.model.DeviceIdentity;
import io.strategiz.data.device.repository.CreateDeviceIdentityRepository;
import io.strategiz.data.device.repository.DeleteDeviceIdentityRepository;
import io.strategiz.data.device.repository.ReadDeviceIdentityRepository;
import io.strategiz.data.device.repository.UpdateDeviceIdentityRepository;
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
    
    private final CreateDeviceIdentityRepository createRepo;
    private final ReadDeviceIdentityRepository readRepo;
    private final UpdateDeviceIdentityRepository updateRepo;
    private final DeleteDeviceIdentityRepository deleteRepo;
    
    @Autowired
    public DeviceIdentityService(
            CreateDeviceIdentityRepository createRepo,
            ReadDeviceIdentityRepository readRepo,
            UpdateDeviceIdentityRepository updateRepo,
            DeleteDeviceIdentityRepository deleteRepo) {
        this.createRepo = createRepo;
        this.readRepo = readRepo;
        this.updateRepo = updateRepo;
        this.deleteRepo = deleteRepo;
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
        
        return createRepo.create(device);
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
        
        // Check if device already exists
        if (deviceId != null && !deviceId.isEmpty()) {
            Optional<DeviceIdentity> existingDevice = readRepo.findByDeviceId(deviceId);
            if (existingDevice.isPresent()) {
                // Update existing device with user ID
                DeviceIdentity device = existingDevice.get();
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
                
                return updateRepo.update(device);
            }
        }
        
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
        
        return createRepo.create(device);
    }
    
    /**
     * Find a device by its device ID
     * 
     * @param deviceId The device ID to search for
     * @return Optional containing the device if found
     */
    public Optional<DeviceIdentity> findDeviceById(String deviceId) {
        return readRepo.findByDeviceId(deviceId);
    }
    
    /**
     * Get all devices for a user
     * 
     * @param userId The user ID to search for
     * @return List of devices associated with the user
     */
    public List<DeviceIdentity> getUserDevices(String userId) {
        return readRepo.findByUserId(userId);
    }
    
    /**
     * Get all trusted devices for a user
     * 
     * @param userId The user ID to search for
     * @return List of trusted devices associated with the user
     */
    public List<DeviceIdentity> getTrustedUserDevices(String userId) {
        return readRepo.findTrustedDevicesByUserId(userId);
    }
    
    /**
     * Update device platform information
     * 
     * @param deviceId The device ID to update
     * @param deviceInfo Map of device information from fingerprinting
     * @return Optional containing the updated device if found and updated
     */
    public Optional<DeviceIdentity> updateDeviceInfo(String deviceId, Map<String, Object> deviceInfo) {
        Optional<DeviceIdentity> deviceOpt = readRepo.findByDeviceId(deviceId);
        
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
            
            return Optional.of(updateRepo.update(device));
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
    public Optional<DeviceIdentity> updateDeviceTrustedStatus(String deviceId, boolean trusted) {
        return updateRepo.updateTrustedStatus(deviceId, trusted);
    }
    
    /**
     * Delete a device
     * 
     * @param deviceId The device ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteDevice(String deviceId) {
        return deleteRepo.deleteByDeviceId(deviceId);
    }
    
    /**
     * Delete all devices for a user
     * 
     * @param userId The user ID whose devices should be deleted
     * @return The number of devices deleted
     */
    public int deleteUserDevices(String userId) {
        return deleteRepo.deleteAllByUserId(userId);
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
