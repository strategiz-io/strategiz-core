package io.strategiz.data.device.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.device.model.DeviceIdentity;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;

import java.util.List;
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