package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import java.time.Instant;
import java.util.Map;

/**
 * Repository interface exclusively for device deletion operations
 * Single Responsibility: Device removal/deletion
 */
public interface DeleteDeviceRepository {
    
    // ========== Authenticated Device Deletes ==========
    
    /**
     * Delete a specific authenticated device
     * 
     * @param userId The user ID
     * @param deviceId The device ID to delete
     * @return true if device was deleted, false if not found
     */
    boolean deleteAuthenticatedDevice(String userId, String deviceId);
    
    /**
     * Delete all devices for a user
     * 
     * @param userId The user ID
     * @return Number of devices deleted
     */
    int deleteAllUserDevices(String userId);
    
    /**
     * Delete all devices except the specified one
     * 
     * @param userId The user ID
     * @param keepDeviceId The device ID to keep
     * @return Number of devices deleted
     */
    int deleteAllUserDevicesExcept(String userId, String keepDeviceId);
    
    /**
     * Delete multiple authenticated devices
     * 
     * @param userId The user ID
     * @param deviceIds Array of device IDs to delete
     * @return Map of device ID to deletion success
     */
    Map<String, Boolean> deleteMultipleAuthenticatedDevices(String userId, String[] deviceIds);
    
    /**
     * Delete untrusted devices for a user
     * 
     * @param userId The user ID
     * @return Number of devices deleted
     */
    int deleteUntrustedUserDevices(String userId);
    
    /**
     * Delete inactive devices for a user
     * 
     * @param userId The user ID
     * @param lastSeenBefore Devices not seen since this time
     * @return Number of devices deleted
     */
    int deleteInactiveUserDevices(String userId, Instant lastSeenBefore);
    
    /**
     * Get an authenticated device before deletion
     * 
     * @param deviceId The device ID
     * @param userId The user ID
     * @return The device or null if not found
     */
    DeviceIdentity getAuthenticatedDevice(String deviceId, String userId);
    
    // ========== Anonymous Device Deletes ==========
    
    /**
     * Delete a specific anonymous device
     * 
     * @param deviceId The device ID to delete
     * @return true if device was deleted, false if not found
     */
    boolean deleteAnonymousDevice(String deviceId);
    
    /**
     * Delete multiple anonymous devices
     * 
     * @param deviceIds Array of device IDs to delete
     * @return Map of device ID to deletion success
     */
    Map<String, Boolean> deleteMultipleAnonymousDevices(String[] deviceIds);
    
    /**
     * Delete inactive anonymous devices
     * 
     * @param lastSeenBefore Devices not seen since this time
     * @return Number of devices deleted
     */
    int deleteInactiveAnonymousDevices(Instant lastSeenBefore);
    
    /**
     * Delete suspicious anonymous devices
     * 
     * @param trustThreshold Devices with trust score below this value
     * @return Number of devices deleted
     */
    int deleteSuspiciousAnonymousDevices(int trustThreshold);
    
    /**
     * Delete blocked anonymous devices
     * 
     * @return Number of devices deleted
     */
    int deleteBlockedAnonymousDevices();
    
    /**
     * Delete anonymous devices by IP address
     * 
     * @param ipAddress The IP address
     * @return Number of devices deleted
     */
    int deleteAnonymousDevicesByIpAddress(String ipAddress);
    
    /**
     * Get an anonymous device before deletion
     * 
     * @param deviceId The device ID
     * @return The device or null if not found
     */
    DeviceIdentity getAnonymousDevice(String deviceId);
    
    /**
     * Purge all anonymous devices
     * WARNING: This deletes ALL anonymous devices
     * 
     * @return Number of devices deleted
     */
    int purgeAllAnonymousDevices();
    
    // ========== Conditional Deletes ==========
    
    /**
     * Delete authenticated devices matching criteria
     * 
     * @param userId The user ID
     * @param criteria Map of field names to values
     * @return Number of devices deleted
     */
    int deleteAuthenticatedDevicesWhere(String userId, Map<String, Object> criteria);
    
    /**
     * Delete anonymous devices matching criteria
     * 
     * @param criteria Map of field names to values
     * @return Number of devices deleted
     */
    int deleteAnonymousDevicesWhere(Map<String, Object> criteria);
    
    /**
     * Delete devices older than specified age
     * 
     * @param userId Optional user ID (null for anonymous)
     * @param olderThan Devices created before this time
     * @return Number of devices deleted
     */
    int deleteDevicesOlderThan(String userId, Instant olderThan);
    
    /**
     * Delete devices with low fingerprint confidence
     * 
     * @param userId Optional user ID (null for anonymous)
     * @param confidenceThreshold Minimum confidence score
     * @return Number of devices deleted
     */
    int deleteDevicesWithLowConfidence(String userId, double confidenceThreshold);
}