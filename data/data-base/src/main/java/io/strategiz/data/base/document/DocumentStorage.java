package io.strategiz.data.base.document;

import com.google.cloud.firestore.Firestore;

/**
 * Interface for document storage operations
 * This provides an abstraction layer for Firestore operations
 */
public interface DocumentStorage {
    
    /**
     * Gets the Firestore document database instance
     * @return Firestore instance
     */
    Firestore getDocumentDb();
}
