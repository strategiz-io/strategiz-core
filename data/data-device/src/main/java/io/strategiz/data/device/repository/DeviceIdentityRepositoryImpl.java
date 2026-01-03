package io.strategiz.data.device.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.device.model.DeviceIdentity;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Firestore implementation of DeviceIdentityRepository
 */
@Repository
public class DeviceIdentityRepositoryImpl extends BaseRepository<DeviceIdentity> implements DeviceIdentityRepository {

    public DeviceIdentityRepositoryImpl(Firestore firestore) {
        super(firestore, DeviceIdentity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-device";
    }

    @Override
    public List<DeviceIdentity> findByDeviceId(String deviceId) {
        return findByField("device_id", deviceId);
    }

    @Override
    public List<DeviceIdentity> findByUserId(String userId) {
        return findByField("user_id", userId);
    }

    @Override
    public List<DeviceIdentity> findByUserIdAndTrustedTrue(String userId) {
        return findByUserId(userId).stream()
                .filter(DeviceIdentity::isTrusted)
                .collect(Collectors.toList());
    }
    
    @Override
    public DeviceIdentity saveAuthenticatedDevice(DeviceIdentity entity, String userId) {
        // For now, save to main collection and mark it as authenticated
        entity.setUserId(userId);
        return save(entity, userId);
    }
    
    @Override
    public DeviceIdentity saveAnonymousDevice(DeviceIdentity entity) {
        // Save to root devices collection as anonymous
        entity.setUserId("anonymous");
        return save(entity, "anonymous");
    }
    
    @Override
    public Optional<DeviceIdentity> findAuthenticatedDevice(String userId, String deviceId) {
        // Find by device ID and verify user ID matches
        Optional<DeviceIdentity> device = findById(deviceId);
        if (device.isPresent() && userId.equals(device.get().getUserId())) {
            return device;
        }
        return Optional.empty();
    }
    
    @Override
    public Optional<DeviceIdentity> findAnonymousDevice(String deviceId) {
        // Find by device ID and verify it's anonymous
        Optional<DeviceIdentity> device = findById(deviceId);
        if (device.isPresent() && "anonymous".equals(device.get().getUserId())) {
            return device;
        }
        return Optional.empty();
    }
    
    @Override
    public boolean deleteAuthenticatedDevice(String userId, String deviceId) {
        // Verify device belongs to user before deleting
        Optional<DeviceIdentity> device = findById(deviceId);
        if (device.isPresent() && userId.equals(device.get().getUserId())) {
            return delete(deviceId, userId);
        }
        return false;
    }
    
    @Override
    public boolean deleteAnonymousDevice(String deviceId) {
        // Verify device is anonymous before deleting
        Optional<DeviceIdentity> device = findById(deviceId);
        if (device.isPresent() && "anonymous".equals(device.get().getUserId())) {
            return delete(deviceId, "system");
        }
        return false;
    }
    
    @Override
    public List<DeviceIdentity> findAllAnonymousDevices() {
        // Return all devices from root collection
        return findAll();
    }
    
    // Legacy method implementations
    @Override
    public List<DeviceIdentity> findAll() {
        return super.findAll();
    }
    
    @Override
    public DeviceIdentity save(DeviceIdentity entity) {
        // For legacy compatibility - use anonymous user if no userId is set
        String userId = entity.getUserId() != null ? entity.getUserId() : "anonymous";
        return save(entity, userId);
    }
    
    @Override
    public boolean existsById(String id) {
        return exists(id);
    }
    
    @Override
    public void deleteById(String id) {
        delete(id, "system");
    }
    
    @Override
    public void delete(DeviceIdentity entity) {
        if (entity != null && entity.getId() != null) {
            deleteById(entity.getId());
        }
    }
}