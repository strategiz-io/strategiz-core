package io.strategiz.data.auth.repository.passkey.credential;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import io.strategiz.data.base.document.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutionException;

/**
 * Repository for deleting passkey credentials
 */
@Repository
public class DeletePasskeyCredentialRepository {

    private static final Logger log = LoggerFactory.getLogger(DeletePasskeyCredentialRepository.class);
    
    private static final String COLLECTION_PASSKEY_CREDENTIALS = "passkey_credentials";
    private static final String FIELD_CREDENTIAL_ID = "credentialId";
    private static final String FIELD_USER_ID = "userId";
    
    private final DocumentStorage documentStorage;
    
    public DeletePasskeyCredentialRepository(DocumentStorage documentStorage) {
        this.documentStorage = documentStorage;
    }
    
    /**
     * Delete a passkey credential by its credential ID
     * 
     * @param credentialId Credential ID
     * @return true if deleted, false if not found
     */
    public boolean deleteByCredentialId(String credentialId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .whereEqualTo(FIELD_CREDENTIAL_ID, credentialId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                log.debug("No credential found with ID: {}", credentialId);
                return false;
            }
            
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            WriteResult result = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .document(document.getId())
                .delete()
                .get();
                
            log.info("Deleted passkey credential with ID: {} at {}", document.getId(), result.getUpdateTime());
            return true;
        } catch (Exception e) {
            log.error("Error deleting passkey credential by credential ID {}: {}", credentialId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Delete all passkey credentials for a user
     * 
     * @param userId User ID
     * @return Number of credentials deleted
     */
    public int deleteAllByUserId(String userId) {
        try {
            Firestore firestore = documentStorage.getDocumentDb();
            QuerySnapshot querySnapshot = firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .get()
                .get();
            
            if (querySnapshot.isEmpty()) {
                return 0;
            }
            
            int deleteCount = 0;
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                firestore.collection(COLLECTION_PASSKEY_CREDENTIALS)
                    .document(document.getId())
                    .delete()
                    .get();
                log.debug("Deleted passkey credential: {}", document.getId());
                deleteCount++;
            }
            
            log.info("Deleted {} passkey credentials for user ID: {}", deleteCount, userId);
            return deleteCount;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting passkey credentials for user {}: {}", userId, e.getMessage(), e);
            Thread.currentThread().interrupt();
            return 0;
        }
    }
}
