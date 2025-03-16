package io.strategiz.auth.repository;

import io.strategiz.auth.model.DeviceIdentity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for managing device identities
 */
@Repository
public interface DeviceIdentityRepository {
    /**
     * Saves a device identity
     * @param deviceIdentity The device identity to save
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> save(DeviceIdentity deviceIdentity);

    /**
     * Finds a device identity by its ID
     * @param id The device identity ID
     * @return A CompletableFuture that completes with the optional device identity
     */
    CompletableFuture<Optional<DeviceIdentity>> findById(String id);

    /**
     * Finds a device identity by device ID and user ID
     * @param deviceId The device ID
     * @param userId The user ID
     * @return A CompletableFuture that completes with the optional device identity
     */
    CompletableFuture<Optional<DeviceIdentity>> findByDeviceIdAndUserId(String deviceId, String userId);

    /**
     * Finds all device identities for a user
     * @param userId The user ID
     * @return A CompletableFuture that completes with the list of device identities
     */
    CompletableFuture<List<DeviceIdentity>> findByUserId(String userId);

    /**
     * Deletes a device identity by its ID
     * @param id The device identity ID
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> deleteById(String id);
}
