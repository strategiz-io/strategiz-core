package io.strategiz.data.auth.repository.passkey.credential;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for reading passkey credentials
 */
@Repository
public class ReadPasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(ReadPasskeyCredentialRepository.class);
    
    private static final String COLLECTION_PASSKEY_CREDENTIALS = "passkey_credentials";
    private static final String FIELD_CREDENTIAL_ID = "credentialId";
    private static final String FIELD_USER_ID = "userId";
    
    private final DocumentStorage documentStorage;
    
    public ReadPasskeyCredentialRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Find a passkey credential by its credential ID
     * 
     * @param credentialId Credential ID
     * @return Optional containing the passkey credential if found
     */
    public Optional<PasskeyCredential> findByCredentialId(String credentialId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .whereEqualTo(FIELD_CREDENTIAL_ID, credentialId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Optional.empty();
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            PasskeyCredential credential = document.toObject(PasskeyCredential.class);
            if (credential != null) {
                credential.setId(document.getId());
                return Optional.of(credential);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error finding passkey credential by credential ID {}: {}", credentialId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Find all passkey credentials for a user
     * 
     * @param userId User ID
     * @return List of passkey credentials for the user
     */
    public List<PasskeyCredential> findByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<PasskeyCredential> results = new ArrayList<>();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                PasskeyCredential credential = document.toObject(PasskeyCredential.class);
                if (credential != null) {
                    credential.setId(document.getId());
                    results.add(credential);
                }
            }
            
            return results;
        } catch (Exception e) {
            log.error("Error finding passkey credentials by user ID {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
