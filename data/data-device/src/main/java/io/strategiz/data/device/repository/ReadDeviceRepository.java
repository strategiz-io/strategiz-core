package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Repository interface for all device read operations
 * Groups both authenticated and anonymous device queries
 * Single Responsibility: Device retrieval/querying
 * 
 * Collection Paths:
 * - Anonymous: /devices (root collection)
 * - Authenticated: /users/{userId}/devices (subcollection)
 */
public interface ReadDeviceRepository {
    
    // ========== Authenticated Device Reads ==========
    
    /**
     * Find an authenticated device by ID
     * Reads from: /users/{userId}/devices/{deviceId}
     * 
     * @param userId The user ID
     * @param deviceId The device ID
     * @return Optional containing the device if found
     */
    Optional<DeviceIdentity> findAuthenticatedDevice(String userId, String deviceId);
    
    /**
     * Find all devices for a user
     * 
     * @param userId The user ID
     * @return List of user's devices
     */
    List<DeviceIdentity> findAllByUserId(String userId);
    
    /**
     * Find trusted devices for a user
     * 
     * @param userId The user ID
     * @return List of trusted devices
     */
    List<DeviceIdentity> findTrustedByUserId(String userId);
    
    /**
     * Find device by visitor ID for a user
     * 
     * @param userId The user ID
     * @param visitorId The FingerprintJS visitor ID
     * @return Optional containing the device if found
     */
    Optional<DeviceIdentity> findByUserIdAndVisitorId(String userId, String visitorId);
    
    /**
     * Search user devices with filters
     * 
     * @param userId The user ID
     * @param filters Map of field names to filter values
     * @return List of matching devices
     */
    List<DeviceIdentity> searchUserDevices(String userId, Map<String, Object> filters);
    
    /**
     * Get an authenticated device (convenience method)
     * 
     * @param deviceId The device ID
     * @param userId The user ID  
     * @return The device or null if not found
     */
    default DeviceIdentity getAuthenticatedDevice(String deviceId, String userId) {
        return findAuthenticatedDevice(userId, deviceId).orElse(null);
    }
    
    // ========== Anonymous Device Reads ==========
    
    /**
     * Find an anonymous device by ID
     * 
     * @param deviceId The device ID
     * @return Optional containing the device if found
     */
    Optional<DeviceIdentity> findAnonymousDevice(String deviceId);
    
    /**
     * Find anonymous device by visitor ID
     * 
     * @param visitorId The FingerprintJS visitor ID
     * @return Optional containing the device if found
     */
    Optional<DeviceIdentity> findAnonymousByVisitorId(String visitorId);
    
    /**
     * Get all anonymous devices with pagination
     * 
     * @param limit Maximum number of results
     * @param offset Offset for pagination
     * @return List of anonymous devices
     */
    List<DeviceIdentity> findAllAnonymousDevices(int limit, int offset);
    
    /**
     * Search anonymous devices with filters
     * 
     * @param filters Map of field names to filter values
     * @return List of matching devices
     */
    List<DeviceIdentity> searchAnonymousDevices(Map<String, Object> filters);
    
    /**
     * Find anonymous devices by IP address
     * 
     * @param ipAddress The IP address
     * @return List of devices from this IP
     */
    List<DeviceIdentity> findAnonymousByIpAddress(String ipAddress);
    
    /**
     * Find suspicious anonymous devices
     * 
     * @param trustThreshold Devices below this trust score
     * @return List of suspicious devices
     */
    List<DeviceIdentity> findSuspiciousAnonymousDevices(int trustThreshold);
    
    /**
     * Find recent anonymous devices
     * 
     * @param hoursAgo Number of hours to look back
     * @return List of recent devices
     */
    List<DeviceIdentity> findRecentAnonymousDevices(int hoursAgo);
    
    /**
     * Get an anonymous device (convenience method)
     * 
     * @param deviceId The device ID
     * @return The device or null if not found
     */
    default DeviceIdentity getAnonymousDevice(String deviceId) {
        return findAnonymousDevice(deviceId).orElse(null);
    }
    
    // ========== Cross-collection Reads ==========
    
    /**
     * Check if device exists (in either collection)
     * 
     * @param deviceId The device ID
     * @return true if exists in any collection
     */
    boolean deviceExists(String deviceId);
    
    /**
     * Count all devices for a user
     * 
     * @param userId The user ID
     * @return Device count
     */
    long countUserDevices(String userId);
    
    /**
     * Count all anonymous devices
     * 
     * @return Anonymous device count
     */
    long countAnonymousDevices();
    
    /**
     * Get device statistics for a user
     * 
     * @param userId The user ID
     * @return Map of statistics (total, trusted, untrusted, etc.)
     */
    Map<String, Object> getUserDeviceStatistics(String userId);
    
    /**
     * Get anonymous device statistics
     * 
     * @return Map of statistics
     */
    Map<String, Object> getAnonymousDeviceStatistics();
}