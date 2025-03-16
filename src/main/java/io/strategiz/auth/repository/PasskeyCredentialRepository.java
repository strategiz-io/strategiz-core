package io.strategiz.auth.repository;

import io.strategiz.auth.model.PasskeyCredential;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for managing passkey credentials
 */
@Repository
public interface PasskeyCredentialRepository {
    /**
     * Saves a passkey credential
     * @param credential The credential to save
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> save(PasskeyCredential credential);

    /**
     * Finds a passkey credential by its ID
     * @param id The credential ID
     * @return A CompletableFuture that completes with the optional credential
     */
    CompletableFuture<Optional<PasskeyCredential>> findById(String id);

    /**
     * Finds all passkey credentials for a user
     * @param userId The user ID
     * @return A CompletableFuture that completes with the list of credentials
     */
    CompletableFuture<List<PasskeyCredential>> findByUserId(String userId);

    /**
     * Deletes a passkey credential by its ID
     * @param id The credential ID
     * @return A CompletableFuture that completes when the operation is done
     */
    CompletableFuture<Void> deleteById(String id);
}
