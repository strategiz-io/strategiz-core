package io.strategiz.data.auth;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import io.strategiz.data.document.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Firestore implementation of the DeviceIdentityRepository interface
 */
@Repository
public class FirestoreDeviceIdentityRepository implements DeviceIdentityRepository {

    private static final Logger log = LoggerFactory.getLogger(FirestoreDeviceIdentityRepository.class);
    private static final String COLLECTION_PATH = "device_identities";

    private final DocumentStorageService documentStorage;

    @Autowired
    public FirestoreDeviceIdentityRepository(DocumentStorageService documentStorage) {
        this.documentStorage = documentStorage;
    }

    @Override
    public Optional<DeviceIdentity> findById(String deviceIdentityId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentSnapshot document = firestore.collection(COLLECTION_PATH)
                .document(deviceIdentityId)
                .get()
                .get();
            
            if (!document.exists()) {
                return Optional.empty();
            }
            
            DeviceIdentity deviceIdentity = document.toObject(DeviceIdentity.class);
            if (deviceIdentity == null) {
                return Optional.empty();
            }
            
            deviceIdentity.setId(document.getId());
            return Optional.of(deviceIdentity);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding device identity by id: {}", deviceIdentityId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Optional<DeviceIdentity> findByDeviceId(String deviceId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("deviceId", deviceId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            DeviceIdentity deviceIdentity = document.toObject(DeviceIdentity.class);
            if (deviceIdentity == null) {
                return Optional.empty();
            }
            
            deviceIdentity.setId(document.getId());
            return Optional.of(deviceIdentity);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding device identity by device ID: {}", deviceId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public Optional<DeviceIdentity> findByDeviceIdAndUserId(String deviceId, String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("deviceId", deviceId)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            DeviceIdentity deviceIdentity = document.toObject(DeviceIdentity.class);
            if (deviceIdentity == null) {
                return Optional.empty();
            }
            
            deviceIdentity.setId(document.getId());
            return Optional.of(deviceIdentity);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding device identity by device ID and user ID: {}, {}", deviceId, userId, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public List<DeviceIdentity> findAllByUserId(String userId) {
        try {
            List<DeviceIdentity> deviceIdentities = new ArrayList<>();
            Firestore firestore = documentStorage.getDocumentDb();
            
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                DeviceIdentity deviceIdentity = document.toObject(DeviceIdentity.class);
                if (deviceIdentity != null) {
                    deviceIdentity.setId(document.getId());
                    deviceIdentities.add(deviceIdentity);
                }
            }
            
            return deviceIdentities;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error finding device identities for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public DeviceIdentity save(DeviceIdentity deviceIdentity) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            DocumentReference docRef;
            
            if (deviceIdentity.getId() != null && !deviceIdentity.getId().isEmpty()) {
                // Update existing device identity
                docRef = firestore.collection(COLLECTION_PATH)
                    .document(deviceIdentity.getId());
            } else {
                // Create new device identity
                docRef = firestore.collection(COLLECTION_PATH)
                    .document();
                deviceIdentity.setId(docRef.getId());
            }
            
            // Save to Firestore
            ApiFuture<WriteResult> result = docRef.set(deviceIdentity);
            result.get(); // Wait for write to complete
            
            return deviceIdentity;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving device identity: {}", deviceIdentity.getId(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save device identity", e);
        }
    }

    @Override
    public boolean deleteById(String deviceIdentityId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            ApiFuture<WriteResult> result = firestore.collection(COLLECTION_PATH)
                .document(deviceIdentityId)
                .delete();
            
            result.get(); // Wait for delete to complete
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting device identity: {}", deviceIdentityId, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public int deleteAllByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Get all device identities for the user
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PATH)
                .whereEqualTo("userId", userId)
                .get()
                .get();
            
            int count = querySnapshot.size();
            
            // Delete each device identity
            WriteBatch batch = firestore.batch();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                batch.delete(document.getReference());
            }
            
            // Commit the batch
            batch.commit().get();
            
            return count;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting device identities for user: {}", userId, e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
