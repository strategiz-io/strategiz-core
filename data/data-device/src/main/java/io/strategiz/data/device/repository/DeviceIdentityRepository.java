package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeviceIdentity entities
 * Implementation provided by client-firebase module
 * 
 * Supports both authenticated devices (stored as subcollection under users)
 * and anonymous devices (stored in root collection)
 */
public interface DeviceIdentityRepository {
    
    /**
     * Save authenticated device identity as subcollection under user
     * Stores at: /users/{userId}/devices/{deviceId}
     * @param entity The device identity to save
     * @param userId The user ID (required for authenticated devices)
     * @return The saved device identity
     */
    DeviceIdentity saveAuthenticatedDevice(DeviceIdentity entity, String userId);
    
    /**
     * Save anonymous device identity at root collection
     * Stores at: /devices/{deviceId}
     * @param entity The device identity to save
     * @return The saved device identity
     */
    DeviceIdentity saveAnonymousDevice(DeviceIdentity entity);
    
    /**
     * Find authenticated device by ID under a specific user
     * @param userId The user ID
     * @param deviceId Device identity ID
     * @return Optional device identity
     */
    Optional<DeviceIdentity> findAuthenticatedDevice(String userId, String deviceId);
    
    /**
     * Find anonymous device by ID from root collection
     * @param deviceId Device identity ID
     * @return Optional device identity
     */
    Optional<DeviceIdentity> findAnonymousDevice(String deviceId);
    
    /**
     * Find devices by their unique device ID
     * 
     * @param deviceId The device ID to search for
     * @return List of matching devices
     */
    List<DeviceIdentity> findByDeviceId(String deviceId);
    
    /**
     * Find all devices associated with a user
     * 
     * @param userId The user ID to search for
     * @return List of devices associated with the user
     */
    List<DeviceIdentity> findByUserId(String userId);
    
    /**
     * Find trusted devices associated with a user
     * 
     * @param userId The user ID to search for
     * @return List of trusted devices associated with the user
     */
    List<DeviceIdentity> findByUserIdAndTrustedTrue(String userId);
    
    /**
     * Delete authenticated device from user's subcollection
     * @param userId The user ID
     * @param deviceId Device identity ID to delete
     * @return True if device was found and deleted
     */
    boolean deleteAuthenticatedDevice(String userId, String deviceId);
    
    /**
     * Delete anonymous device from root collection
     * @param deviceId Device identity ID to delete
     * @return True if device was found and deleted
     */
    boolean deleteAnonymousDevice(String deviceId);
    
    /**
     * Find all anonymous devices (from root collection)
     * @return List of all anonymous device identities
     */
    List<DeviceIdentity> findAllAnonymousDevices();
    
    // Legacy compatibility methods (will use anonymous device operations)
    
    /**
     * @deprecated Use saveAnonymousDevice or saveAuthenticatedDevice instead
     */
    @Deprecated
    default DeviceIdentity save(DeviceIdentity entity, String userId) {
        if (userId != null && !userId.isEmpty() && !"anonymous".equals(userId)) {
            return saveAuthenticatedDevice(entity, userId);
        }
        return saveAnonymousDevice(entity);
    }
    
    /**
     * @deprecated Use saveAnonymousDevice instead
     */
    @Deprecated
    default DeviceIdentity save(DeviceIdentity entity) {
        return saveAnonymousDevice(entity);
    }
    
    /**
     * @deprecated Use findAnonymousDevice or findAuthenticatedDevice instead
     */
    @Deprecated
    default Optional<DeviceIdentity> findById(String id) {
        return findAnonymousDevice(id);
    }
    
    /**
     * @deprecated Use deleteAnonymousDevice or deleteAuthenticatedDevice instead
     */
    @Deprecated
    default boolean delete(String id, String userId) {
        if (userId != null && !userId.isEmpty() && !"anonymous".equals(userId)) {
            return deleteAuthenticatedDevice(userId, id);
        }
        return deleteAnonymousDevice(id);
    }
    
    /**
     * @deprecated Use deleteAnonymousDevice instead
     */
    @Deprecated
    default void deleteById(String id) {
        deleteAnonymousDevice(id);
    }
    
    /**
     * @deprecated Use deleteAnonymousDevice instead
     */
    @Deprecated
    default void delete(DeviceIdentity entity) {
        if (entity != null && entity.getId() != null) {
            deleteAnonymousDevice(entity.getId());
        }
    }
    
    /**
     * @deprecated Use findAnonymousDevice instead
     */
    @Deprecated
    default boolean existsById(String id) {
        return findAnonymousDevice(id).isPresent();
    }
    
    /**
     * @deprecated Use findAllAnonymousDevices instead
     */
    @Deprecated
    default List<DeviceIdentity> findAll() {
        return findAllAnonymousDevices();
    }
}
