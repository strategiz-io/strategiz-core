package io.strategiz.data.auth.repository.passkey.credential;

import com.google.cloud.firestore.Firestore;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Repository for updating passkey credentials
 */
@Repository
public class UpdatePasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(UpdatePasskeyCredentialRepository.class);
    
    private static final String COLLECTION_PASSKEY_CREDENTIALS = "passkey_credentials";
    
    private final DocumentStorage documentStorage;
    
    public UpdatePasskeyCredentialRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Update an existing passkey credential
     * 
     * @param credential Passkey credential to update
     * @return Updated passkey credential
     * @throws IllegalArgumentException if credential ID is null or empty
     */
    public PasskeyCredential update(PasskeyCredential credential) {
        if (credential.getId() == null || credential.getId().isEmpty()) {
            throw new IllegalArgumentException("Cannot update credential without an ID");
        }
        
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            
            // Update existing credential
            firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .document(credential.getId())
                .set(credential)
                .get();
            
            log.debug("Updated passkey credential with ID: {}", credential.getId());
            return credential;
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error updating passkey credential: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Failed to update passkey credential", e);
        }
    }
}
