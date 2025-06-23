package io.strategiz.data.auth.repository.passkey.credential;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Repository for creating passkey credentials
 */
@Repository
public class CreatePasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(CreatePasskeyCredentialRepository.class);
    
    private static final String COLLECTION_PASSKEY_CREDENTIALS = "passkey_credentials";
    
    private final DocumentStorage documentStorage;
    
    public CreatePasskeyCredentialRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Create a new passkey credential
     * 
     * @param credential Passkey credential to save
     * @return Saved passkey credential with generated ID
     */
    public PasskeyCredential create(PasskeyCredential credential) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Generate a new document ID if none exists
            var docRef = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS).document();
            credential.setId(docRef.getId());
            
            // Write the document
            docRef.set(credential).get();
            log.debug("Created new passkey credential with ID: {}", credential.getId());
            
            return credential;
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error creating passkey credential: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to create passkey credential", e);
        }
    }
}
