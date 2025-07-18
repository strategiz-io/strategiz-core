package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeviceIdentity entities
 * Implementation provided by client-firebase module
 */
public interface DeviceIdentityRepository {
    
    /**
     * Save device identity (create or update)
     * @param entity The device identity to save
     * @param userId Who is saving it
     * @return The saved device identity
     */
    DeviceIdentity save(DeviceIdentity entity, String userId);
    
    /**
     * Find device identity by ID
     * @param id Device identity ID
     * @return Optional device identity
     */
    Optional<DeviceIdentity> findById(String id);
    
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
     * Delete device identity
     * @param id Device identity ID to delete
     * @param userId Who is deleting it
     * @return True if device was found and deleted
     */
    boolean delete(String id, String userId);
    
    /**
     * Find all device identities
     * @return List of all device identities
     */
    List<DeviceIdentity> findAll();
    
    /**
     * Save a device identity without userId (for legacy compatibility)
     * @param entity The device identity to save
     * @return The saved device identity
     */
    DeviceIdentity save(DeviceIdentity entity);
    
    /**
     * Check if a device identity exists by ID
     * @param id The device identity ID
     * @return True if exists
     */
    boolean existsById(String id);
    
    /**
     * Delete a device identity by ID
     * @param id The device identity ID
     */
    void deleteById(String id);
    
    /**
     * Delete a device identity entity
     * @param entity The device identity to delete
     */
    void delete(DeviceIdentity entity);
}
