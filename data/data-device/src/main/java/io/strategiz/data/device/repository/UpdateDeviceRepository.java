package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import java.util.Optional;
import java.util.Map;
import java.time.Instant;

/**
 * Repository interface exclusively for device update operations
 * Single Responsibility: Device modification/updates
 */
public interface UpdateDeviceRepository {
    
    // ========== Authenticated Device Updates ==========
    
    /**
     * Update an authenticated device
     * 
     * @param userId The user ID
     * @param device The device entity with updates
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateAuthenticatedDevice(String userId, DeviceIdentity device);
    
    /**
     * Update an authenticated device (returns non-optional)
     * 
     * @param device The device entity with updates
     * @param userId The user ID
     * @return The updated device
     */
    default DeviceIdentity updateAuthenticatedDevice(DeviceIdentity device, String userId) {
        return updateAuthenticatedDevice(userId, device).orElse(device);
    }
    
    /**
     * Update device trust status
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @param trusted The new trust status
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateDeviceTrust(String userId, String deviceId, boolean trusted);
    
    /**
     * Update device name
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @param deviceName The new device name
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateDeviceName(String userId, String deviceId, String deviceName);
    
    /**
     * Update device trust level
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @param trustLevel The new trust level
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateDeviceTrustLevel(String userId, String deviceId, String trustLevel);
    
    /**
     * Update device last seen timestamp
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @param lastSeen The new last seen timestamp
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateDeviceLastSeen(String userId, String deviceId, Instant lastSeen);
    
    /**
     * Partial update with specific fields
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @param updates Map of field names to new values
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> partialUpdateAuthenticatedDevice(
        String userId, String deviceId, Map<String, Object> updates);
    
    // ========== Anonymous Device Updates ==========
    
    /**
     * Update an anonymous device
     * 
     * @param device The device entity with updates
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateAnonymousDevice(DeviceIdentity device);
    
    /**
     * Update anonymous device trust level
     * 
     * @param deviceId The device ID
     * @param trustLevel The new trust level
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateAnonymousDeviceTrustLevel(String deviceId, String trustLevel);
    
    /**
     * Update anonymous device last seen
     * 
     * @param deviceId The device ID
     * @param lastSeen The new last seen timestamp
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> updateAnonymousDeviceLastSeen(String deviceId, Instant lastSeen);
    
    /**
     * Mark anonymous device as suspicious
     * 
     * @param deviceId The device ID
     * @param reason The reason for marking suspicious
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> markAnonymousDeviceSuspicious(String deviceId, String reason);
    
    /**
     * Block an anonymous device
     * 
     * @param deviceId The device ID
     * @param reason The reason for blocking
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> blockAnonymousDevice(String deviceId, String reason);
    
    /**
     * Unblock an anonymous device
     * 
     * @param deviceId The device ID
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> unblockAnonymousDevice(String deviceId);
    
    /**
     * Partial update anonymous device with specific fields
     * 
     * @param deviceId The device ID
     * @param updates Map of field names to new values
     * @return Optional containing updated device if successful
     */
    Optional<DeviceIdentity> partialUpdateAnonymousDevice(
        String deviceId, Map<String, Object> updates);
    
    // ========== Bulk Updates ==========
    
    /**
     * Bulk update authenticated devices
     * 
     * @param userId The user ID
     * @param deviceIds Array of device IDs to update
     * @param updates Map of field names to new values
     * @return Number of devices updated
     */
    int bulkUpdateAuthenticatedDevices(
        String userId, String[] deviceIds, Map<String, Object> updates);
    
    /**
     * Bulk update anonymous devices
     * 
     * @param deviceIds Array of device IDs to update
     * @param updates Map of field names to new values
     * @return Number of devices updated
     */
    int bulkUpdateAnonymousDevices(String[] deviceIds, Map<String, Object> updates);
    
    /**
     * Update all user devices with specific criteria
     * 
     * @param userId The user ID
     * @param criteria Criteria for selecting devices
     * @param updates Map of field names to new values
     * @return Number of devices updated
     */
    int updateAuthenticatedDevicesWhere(
        String userId, Map<String, Object> criteria, Map<String, Object> updates);
    
    /**
     * Update all anonymous devices with specific criteria
     * 
     * @param criteria Criteria for selecting devices
     * @param updates Map of field names to new values
     * @return Number of devices updated
     */
    int updateAnonymousDevicesWhere(
        Map<String, Object> criteria, Map<String, Object> updates);
}