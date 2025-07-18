package io.strategiz.data.devices.repository;

import io.strategiz.data.devices.entity.UserDeviceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for users/{userId}/devices subcollection
 */
@Repository
public interface UserDeviceRepository extends CrudRepository<UserDeviceEntity, String> {
    
    // ===============================
    // Spring Data Query Methods
    // ===============================
    
    /**
     * Find devices by agent ID
     */
    Optional<UserDeviceEntity> findByAgentId(String agentId);
    
    /**
     * Find devices by name
     */
    List<UserDeviceEntity> findByDeviceName(String deviceName);
    
    /**
     * Find devices by name (case insensitive)
     */
    List<UserDeviceEntity> findByDeviceNameIgnoreCase(String deviceName);
    
    /**
     * Find trusted devices
     */
    List<UserDeviceEntity> findByTrustedTrue();
    
    /**
     * Find untrusted devices
     */
    List<UserDeviceEntity> findByTrustedFalse();
    
    /**
     * Find devices by platform type
     */
    List<UserDeviceEntity> findByPlatformType(String platformType);
    
    /**
     * Find devices by platform OS
     */
    List<UserDeviceEntity> findByPlatformOs(String os);
    
    /**
     * Find devices with push tokens
     */
    List<UserDeviceEntity> findByPushTokenIsNotNull();
    
    /**
     * Find devices without push tokens
     */
    List<UserDeviceEntity> findByPushTokenIsNull();
    
    /**
     * Find device by push token
     */
    Optional<UserDeviceEntity> findByPushToken(String pushToken);
    
    /**
     * Check if agent ID exists
     */
    boolean existsByAgentId(String agentId);
    
    /**
     * Count trusted devices
     */
    long countByTrustedTrue();
    
    /**
     * Count devices by platform type
     */
    long countByPlatformType(String platformType);
    
    /**
     * Find devices ordered by last login
     */
    List<UserDeviceEntity> findAllByOrderByLastLoginAtDesc();
    
    /**
     * Find devices by last login after date
     */
    List<UserDeviceEntity> findByLastLoginAtAfter(Instant since);
}