package io.strategiz.data.device.repository;

import io.strategiz.data.base.repository.BaseRepository;
import io.strategiz.data.device.model.DeviceIdentity;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Base repository for device operations extending BaseRepository
 * Handles direct Firestore operations for device entities
 * 
 * Collection Paths:
 * - Anonymous: /devices (root collection)
 * - Authenticated: /users/{userId}/devices (subcollection)
 */
@Repository
public class DeviceBaseRepository extends BaseRepository<DeviceIdentity> {
    
    @Autowired
    public DeviceBaseRepository(Firestore firestore) {
        super(firestore, DeviceIdentity.class);
    }

    @Override
    protected String getModuleName() {
        return "data-device";
    }
    
    // BaseRepository doesn't have getCollectionPath as abstract method
    // We handle collection paths directly in the implementation methods
    
    /**
     * Get collection path for authenticated devices
     */
    protected String getUserDeviceCollectionPath(String userId) {
        return "users/" + userId + "/devices";
    }
}