package io.strategiz.data.device.repository;

import io.strategiz.data.device.model.DeviceIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of CreateDeviceRepository that directly connects to Firestore
 * THIS is where the database connection happens - IN the data module!
 */
@Repository
public class CreateDeviceRepositoryImpl implements CreateDeviceRepository {
    
    private final Firestore firestore;
    private final DeviceBaseRepository baseRepository;
    
    @Autowired
    public CreateDeviceRepositoryImpl(Firestore firestore, DeviceBaseRepository baseRepository) {
        this.firestore = firestore;
        this.baseRepository = baseRepository;
    }
    
    @Override
    public DeviceIdentity createAuthenticatedDevice(DeviceIdentity device, String userId) {
        // Timestamps are handled by BaseEntity/BaseRepository
        
        // Generate device ID if not set
        if (device.getDeviceId() == null) {
            device.setDeviceId(UUID.randomUUID().toString());
        }
        
        // Save to user's device subcollection: /users/{userId}/devices
        try {
            DocumentReference docRef = firestore
                .collection("users")
                .document(userId)
                .collection("devices")
                .document(device.getDeviceId());
            
            docRef.set(device).get();
            return device;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create authenticated device", e);
        }
    }
    
    @Override
    public DeviceIdentity createAnonymousDevice(DeviceIdentity device) {
        // Timestamps are handled by BaseEntity/BaseRepository
        
        // Generate device ID if not set
        if (device.getDeviceId() == null) {
            device.setDeviceId(UUID.randomUUID().toString());
        }
        
        // Save to root devices collection: /devices
        return baseRepository.save(device, "anonymous");
    }
    
    @Override
    public DeviceIdentity[] bulkCreateAuthenticatedDevices(DeviceIdentity[] devices, String userId) {
        DeviceIdentity[] savedDevices = new DeviceIdentity[devices.length];
        for (int i = 0; i < devices.length; i++) {
            savedDevices[i] = createAuthenticatedDevice(devices[i], userId);
        }
        return savedDevices;
    }
    
    @Override
    public DeviceIdentity[] bulkCreateAnonymousDevices(DeviceIdentity[] devices) {
        DeviceIdentity[] savedDevices = new DeviceIdentity[devices.length];
        for (int i = 0; i < devices.length; i++) {
            savedDevices[i] = createAnonymousDevice(devices[i]);
        }
        return savedDevices;
    }
}