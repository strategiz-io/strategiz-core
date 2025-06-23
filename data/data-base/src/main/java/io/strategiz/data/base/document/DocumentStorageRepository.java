package io.strategiz.data.base.document;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Repository for document-oriented storage operations
 * This centralizes all document database access using Firestore as the implementation
 */
@Repository
public class DocumentStorageRepository implements DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageRepository.class);

    /**
     * Get the document database instance
     * Currently implemented with Firestore but could be replaced with another document database
     *
     * @return Document database instance
     */
    public Firestore getDocumentDb() {
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Document storage functionality will not be available.");
            throw new IllegalStateException("Document storage provider not initialized");
        }
        return FirestoreClient.getFirestore();
    }
    
    /**
     * Check if the document storage provider is initialized and ready
     *
     * @return true if ready, false otherwise
     */
    public boolean isReady() {
        return !com.google.firebase.FirebaseApp.getApps().isEmpty();
    }
}
