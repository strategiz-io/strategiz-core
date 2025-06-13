package io.strategiz.data.base;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class FirestoreOperations {

    private static final Logger log = LoggerFactory.getLogger(FirestoreOperations.class);
    private final Firestore firestore;

    public FirestoreOperations(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Sets the data in a Firestore document. This is a blocking operation.
     *
     * @param documentReference The reference to the document.
     * @param data              The data to set (can be a POJO).
     * @param <T>               The type of the data object.
     */
    public <T> void set(DocumentReference documentReference, T data) {
        try {
            ApiFuture<WriteResult> future = documentReference.set(data);
            WriteResult result = future.get(); // Block and wait for the operation to complete
            log.debug("Data set for document {} at update time: {}", documentReference.getId(), result.getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error setting data for document {}: {}", documentReference.getId(), e.getMessage(), e);
            // Consider your application's error handling strategy.
            // Rethrowing as a runtime exception is a common approach.
            Thread.currentThread().interrupt(); // Preserve interrupt status
            throw new RuntimeException("Failed to set Firestore document: " + documentReference.getId(), e);
        }
    }

    // You can add other common Firestore operations here as needed, for example:
    // public <T> Optional<T> get(DocumentReference documentReference, Class<T> clazz) { ... }
    // public void update(DocumentReference documentReference, Map<String, Object> updates) { ... }

    /**
     * Deletes a Firestore document. This is a blocking operation.
     *
     * @param documentReference The reference to the document to delete.
     */
    public void delete(DocumentReference documentReference) {
        try {
            ApiFuture<WriteResult> future = documentReference.delete();
            WriteResult result = future.get(); // Block and wait for the operation to complete
            log.debug("Document {} deleted at update time: {}", documentReference.getId(), result.getUpdateTime());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting document {}: {}", documentReference.getId(), e.getMessage(), e);
            // Consider your application's error handling strategy.
            Thread.currentThread().interrupt(); // Preserve interrupt status
            throw new RuntimeException("Failed to delete Firestore document: " + documentReference.getId(), e);
        }
    }
}
