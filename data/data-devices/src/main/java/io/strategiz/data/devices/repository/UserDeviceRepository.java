package io.strategiz.data.devices.repository;

import io.strategiz.data.devices.entity.UserDevice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for users/{userId}/devices subcollection
 */
public interface UserDeviceRepository {
    
    /**
     * Register device for user
     */
    UserDevice registerDevice(String userId, UserDevice device);
    
    /**
     * Get all devices for user
     */
    List<UserDevice> findByUserId(String userId);
    
    /**
     * Get device by ID
     */
    Optional<UserDevice> findByUserIdAndDeviceId(String userId, String deviceId);
    
    /**
     * Find device by agent ID
     */
    Optional<UserDevice> findByUserIdAndAgentId(String userId, String agentId);
    
    /**
     * Update device
     */
    UserDevice updateDevice(String userId, String deviceId, UserDevice device);
    
    /**
     * Update last login
     */
    void updateLastLogin(String userId, String deviceId, Instant lastLoginAt);
    
    /**
     * Update device location and IP
     */
    void updateLocationAndIp(String userId, String deviceId, String location, String ipAddress);
    
    /**
     * Mark device as trusted
     */
    void markAsTrusted(String userId, String deviceId);
    
    /**
     * Mark device as untrusted
     */
    void markAsUntrusted(String userId, String deviceId);
    
    /**
     * Update push token
     */
    void updatePushToken(String userId, String deviceId, String pushToken);
    
    /**
     * Remove device
     */
    void removeDevice(String userId, String deviceId);
    
    /**
     * Get trusted devices
     */
    List<UserDevice> findTrustedDevices(String userId);
    
    /**
     * Get devices by platform type
     */
    List<UserDevice> findByUserIdAndPlatformType(String userId, String platformType);
    
    /**
     * Count devices for user
     */
    long countByUserId(String userId);
    
    /**
     * Get recent devices (last 30 days)
     */
    List<UserDevice> findRecentDevices(String userId, Instant since);
}