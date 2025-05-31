package io.strategiz.data.base.document;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for document-oriented storage operations
 * This centralizes all document database access to abstract away the specific implementation (Firestore)
 */
@Service
public class DocumentStorageService implements DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

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
