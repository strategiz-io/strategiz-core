package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for DeviceIdentity entities
 * Extends Spring Data JPA's JpaRepository with custom query methods
 */
@Repository
public interface DeviceIdentityRepository extends JpaRepository<DeviceIdentity, String> {
    
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
}
