package io.strategiz.auth.service;

import io.strategiz.auth.model.DeviceIdentity;
import io.strategiz.auth.model.Session;
import io.strategiz.auth.repository.DeviceIdentityRepository;
import io.strategiz.auth.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing device identity authentication
 */
@Service
public class DeviceIdentityService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceIdentityService.class);
    private static final long SESSION_EXPIRY_SECONDS = 30 * 24 * 60 * 60; // 30 days for device sessions

    private final DeviceIdentityRepository deviceIdentityRepository;
    private final SessionRepository sessionRepository;

    @Autowired
    public DeviceIdentityService(DeviceIdentityRepository deviceIdentityRepository, SessionRepository sessionRepository) {
        this.deviceIdentityRepository = deviceIdentityRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Registers a new device for a user
     * @param userId The user ID
     * @param deviceId The device ID
     * @param publicKey The public key for the device
     * @param name The device name
     * @param deviceInfo Additional device information for security monitoring
     * @return The registered device identity
     * @throws ExecutionException If there's an error registering the device
     * @throws InterruptedException If there's an error registering the device
     */
    public DeviceIdentity registerDevice(
            String userId,
            String deviceId,
            String publicKey,
            String name,
            Map<String, Object> deviceInfo) throws ExecutionException, InterruptedException {
        
        logger.info("Registering device for user: {} with deviceId: {}", userId, deviceId);
        
        // Check if device already exists for this user
        Optional<DeviceIdentity> existingDevice = 
                deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId).get();
        
        if (existingDevice.isPresent()) {
            logger.info("Device already registered, updating: {}", deviceId);
            DeviceIdentity device = existingDevice.get();
            device.setPublicKey(publicKey);
            device.setName(name);
            device.setDeviceInfo(deviceInfo);
            device.setLastUsedAt(Instant.now().getEpochSecond());
            
            deviceIdentityRepository.save(device).get();
            return device;
        }
        
        // Create new device identity
        DeviceIdentity device = DeviceIdentity.builder()
                .userId(userId)
                .deviceId(deviceId)
                .publicKey(publicKey)
                .name(name)
                .deviceInfo(deviceInfo)
                .createdAt(Instant.now().getEpochSecond())
                .lastUsedAt(Instant.now().getEpochSecond())
                .trusted(false) // New devices are not trusted by default
                .build();
        
        deviceIdentityRepository.save(device).get();
        
        logger.info("Device registered successfully for user: {} with deviceId: {}", userId, deviceId);
        
        return device;
    }

    /**
     * Creates a session for device web crypto authentication
     * @param userId The user ID
     * @param email The user's email
     * @param deviceId The device ID
     * @param signature The signature from the device
     * @param deviceInfo Additional device information
     * @return The session ID
     * @throws ExecutionException If there's an error creating the session
     * @throws InterruptedException If there's an error creating the session
     */
    public String createDeviceSession(
            String userId,
            String email,
            String deviceId,
            String signature,
            Map<String, Object> deviceInfo) throws ExecutionException, InterruptedException {
        
        logger.info("Creating device session for user: {} with deviceId: {}", userId, deviceId);
        
        // Verify the device exists and is registered to this user
        Optional<DeviceIdentity> deviceOpt = 
                deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId).get();
        
        if (deviceOpt.isEmpty()) {
            logger.warn("Device not registered for user: {} with deviceId: {}", userId, deviceId);
            throw new IllegalArgumentException("Device not registered for this user");
        }
        
        DeviceIdentity device = deviceOpt.get();
        
        // TODO: Verify the signature using the device's public key
        // This would be a cryptographic verification to ensure the request is legitimate
        
        // Update device info and last used time
        device.setDeviceInfo(deviceInfo);
        device.updateLastUsedTime();
        deviceIdentityRepository.save(device).get();
        
        // Create session
        String sessionId = UUID.randomUUID().toString();
        Session session = Session.builder()
                .id(sessionId)
                .userId(userId)
                .token("device-auth:" + deviceId) // Special token for device auth with device ID
                .createdAt(Instant.now().getEpochSecond())
                .expiresAt(Instant.now().getEpochSecond() + SESSION_EXPIRY_SECONDS)
                .lastAccessedAt(Instant.now().getEpochSecond())
                .build();
        
        sessionRepository.save(session).get(); // Wait for completion
        logger.info("Device session created successfully: {}", sessionId);
        
        return sessionId;
    }

    /**
     * Gets all registered devices for a user
     * @param userId The user ID
     * @return The list of registered devices
     * @throws ExecutionException If there's an error getting the devices
     * @throws InterruptedException If there's an error getting the devices
     */
    public List<DeviceIdentity> getDevices(String userId) throws ExecutionException, InterruptedException {
        logger.info("Getting devices for user: {}", userId);
        return deviceIdentityRepository.findByUserId(userId).get();
    }

    /**
     * Sets a device's trusted status
     * @param deviceId The device ID
     * @param userId The user ID
     * @param trusted Whether the device should be trusted
     * @return The updated device identity
     * @throws ExecutionException If there's an error updating the device
     * @throws InterruptedException If there's an error updating the device
     */
    public DeviceIdentity setDeviceTrusted(String deviceId, String userId, boolean trusted) 
            throws ExecutionException, InterruptedException {
        
        logger.info("Setting device trusted status to {} for deviceId: {} and userId: {}", 
                trusted, deviceId, userId);
        
        Optional<DeviceIdentity> deviceOpt = 
                deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId).get();
        
        if (deviceOpt.isEmpty()) {
            logger.warn("Device not found for deviceId: {} and userId: {}", deviceId, userId);
            throw new IllegalArgumentException("Device not found");
        }
        
        DeviceIdentity device = deviceOpt.get();
        device.setTrusted(trusted);
        deviceIdentityRepository.save(device).get();
        
        return device;
    }

    /**
     * Deletes a device
     * @param deviceId The device ID
     * @param userId The user ID
     * @throws ExecutionException If there's an error deleting the device
     * @throws InterruptedException If there's an error deleting the device
     */
    public void deleteDevice(String deviceId, String userId) 
            throws ExecutionException, InterruptedException {
        
        logger.info("Deleting device for deviceId: {} and userId: {}", deviceId, userId);
        
        Optional<DeviceIdentity> deviceOpt = 
                deviceIdentityRepository.findByDeviceIdAndUserId(deviceId, userId).get();
        
        if (deviceOpt.isPresent()) {
            deviceIdentityRepository.deleteById(deviceOpt.get().getId()).get();
            logger.info("Device deleted successfully");
        } else {
            logger.warn("Device not found for deletion");
        }
    }
}
