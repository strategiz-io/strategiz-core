package io.strategiz.auth.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.auth.model.DeviceIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore implementation of the DeviceIdentityRepository
 */
@Repository
public class FirestoreDeviceIdentityRepository implements DeviceIdentityRepository {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreDeviceIdentityRepository.class);
    private static final String COLLECTION_NAME = "device_identities";

    private final Firestore firestore;

    public FirestoreDeviceIdentityRepository() {
        this.firestore = FirestoreClient.getFirestore();
    }

    @Override
    public CompletableFuture<Void> save(DeviceIdentity deviceIdentity) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef;
            if (deviceIdentity.getId() != null && !deviceIdentity.getId().isEmpty()) {
                docRef = firestore.collection(COLLECTION_NAME).document(deviceIdentity.getId());
            } else {
                docRef = firestore.collection(COLLECTION_NAME).document();
                deviceIdentity.setId(docRef.getId());
            }
            
            ApiFuture<WriteResult> result = docRef.set(deviceIdentity);
            
            result.addListener(() -> {
                try {
                    result.get();
                    logger.info("Saved device identity: {}", deviceIdentity.getId());
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to save device identity: {}", deviceIdentity.getId(), e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error saving device identity", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Optional<DeviceIdentity>> findById(String id) {
        CompletableFuture<Optional<DeviceIdentity>> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<DocumentSnapshot> documentSnapshot = docRef.get();
            
            documentSnapshot.addListener(() -> {
                try {
                    DocumentSnapshot snapshot = documentSnapshot.get();
                    if (snapshot.exists()) {
                        DeviceIdentity deviceIdentity = snapshot.toObject(DeviceIdentity.class);
                        logger.info("Found device identity: {}", id);
                        future.complete(Optional.ofNullable(deviceIdentity));
                    } else {
                        logger.info("Device identity not found: {}", id);
                        future.complete(Optional.empty());
                    }
                } catch (Exception e) {
                    logger.error("Failed to get device identity: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding device identity by ID", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Optional<DeviceIdentity>> findByDeviceIdAndUserId(String deviceId, String userId) {
        CompletableFuture<Optional<DeviceIdentity>> future = new CompletableFuture<>();
        
        try {
            Query query = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("deviceId", deviceId)
                    .whereEqualTo("userId", userId);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            
            querySnapshot.addListener(() -> {
                try {
                    QuerySnapshot snapshot = querySnapshot.get();
                    if (!snapshot.isEmpty()) {
                        DeviceIdentity deviceIdentity = snapshot.getDocuments().get(0).toObject(DeviceIdentity.class);
                        logger.info("Found device identity for deviceId: {} and userId: {}", deviceId, userId);
                        future.complete(Optional.ofNullable(deviceIdentity));
                    } else {
                        logger.info("Device identity not found for deviceId: {} and userId: {}", deviceId, userId);
                        future.complete(Optional.empty());
                    }
                } catch (Exception e) {
                    logger.error("Failed to get device identity for deviceId: {} and userId: {}", deviceId, userId, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding device identity by deviceId and userId", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<List<DeviceIdentity>> findByUserId(String userId) {
        CompletableFuture<List<DeviceIdentity>> future = new CompletableFuture<>();
        
        try {
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("userId", userId);
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            
            querySnapshot.addListener(() -> {
                try {
                    List<DeviceIdentity> deviceIdentities = new ArrayList<>();
                    QuerySnapshot snapshot = querySnapshot.get();
                    
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        DeviceIdentity deviceIdentity = document.toObject(DeviceIdentity.class);
                        if (deviceIdentity != null) {
                            deviceIdentities.add(deviceIdentity);
                        }
                    }
                    
                    logger.info("Found {} device identities for user: {}", deviceIdentities.size(), userId);
                    future.complete(deviceIdentities);
                } catch (Exception e) {
                    logger.error("Failed to get device identities for user: {}", userId, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error finding device identities by user ID", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    @Override
    public CompletableFuture<Void> deleteById(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
            ApiFuture<WriteResult> result = docRef.delete();
            
            result.addListener(() -> {
                try {
                    result.get();
                    logger.info("Deleted device identity: {}", id);
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("Failed to delete device identity: {}", id, e);
                    future.completeExceptionally(e);
                }
            }, Runnable::run);
        } catch (Exception e) {
            logger.error("Error deleting device identity", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
}
