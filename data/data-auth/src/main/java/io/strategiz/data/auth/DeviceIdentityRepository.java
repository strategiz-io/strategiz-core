package io.strategiz.data.auth;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DeviceIdentity data access operations
 */
public interface DeviceIdentityRepository {

    /**
     * Find a device identity by ID
     *
     * @param deviceIdentityId the device identity ID
     * @return optional device identity
     */
    Optional<DeviceIdentity> findById(String deviceIdentityId);
    
    /**
     * Find a device identity by device ID
     *
     * @param deviceId the device ID
     * @return optional device identity
     */
    Optional<DeviceIdentity> findByDeviceId(String deviceId);
    
    /**
     * Find a device identity by device ID and user ID
     *
     * @param deviceId the device ID
     * @param userId the user ID
     * @return optional device identity
     */
    Optional<DeviceIdentity> findByDeviceIdAndUserId(String deviceId, String userId);
    
    /**
     * Find all device identities for a user
     *
     * @param userId the user ID
     * @return list of device identities
     */
    List<DeviceIdentity> findAllByUserId(String userId);
    
    /**
     * Save a device identity
     *
     * @param deviceIdentity the device identity to save
     * @return the saved device identity with ID
     */
    DeviceIdentity save(DeviceIdentity deviceIdentity);
    
    /**
     * Delete a device identity by ID
     *
     * @param deviceIdentityId the device identity ID
     * @return true if deleted, false otherwise
     */
    boolean deleteById(String deviceIdentityId);
    
    /**
     * Delete all device identities for a user
     *
     * @param userId the user ID
     * @return number of deleted device identities
     */
    int deleteAllByUserId(String userId);
}
