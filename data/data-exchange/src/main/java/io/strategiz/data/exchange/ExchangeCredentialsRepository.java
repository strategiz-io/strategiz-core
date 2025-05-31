package io.strategiz.data.exchange;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import io.strategiz.data.base.document.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository for managing exchange API credentials
 * This abstracts away the specific storage implementation (Firestore)
 */
@Repository
public class ExchangeCredentialsRepository {

    private static final Logger log = LoggerFactory.getLogger(ExchangeCredentialsRepository.class);
    
    private final DocumentStorageService documentStorage;
    
    @Autowired
    public ExchangeCredentialsRepository(DocumentStorageService documentStorage) {
        this.documentStorage = documentStorage;
    }

    /**
     * Get exchange credentials for a user
     * 
     * @param userId User ID
     * @param provider Exchange provider (e.g., "kraken", "binance")
     * @return Map of credentials (apiKey, secretKey)
     */
    public Map<String, String> getExchangeCredentials(String userId, String provider) {
        if (!documentStorage.isReady()) {
            log.warn("Document storage not initialized. Cannot retrieve {} credentials.", provider);
            return null;
        }
        
        try {
            log.info("Getting {} credentials for user: {}", provider, userId);
            
            // Check 1: Look in the new api_credentials subcollection (new structure)
            log.info("Checking api_credentials subcollection for {} credentials", provider);
            QuerySnapshot querySnapshot = documentStorage.getDocumentDb()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", provider)
                .get()
                .get();
            
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                
                if (document.exists()) {
                    Map<String, String> credentials = new HashMap<>();
                    credentials.put("apiKey", document.getString("apiKey"));
                    credentials.put("secretKey", document.getString("secretKey"));
                    
                    log.info("Found {} credentials in api_credentials subcollection for user: {}", provider, userId);
                    if (credentials.get("apiKey") != null) {
                        String apiKeyPreview = credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "...";
                        log.info("API key found (first 5 chars): {}", apiKeyPreview);
                    }
                    if (credentials.get("secretKey") != null) {
                        log.info("Secret key found (length): {} chars", credentials.get("secretKey").length());
                    }
                    
                    return credentials;
                }
            } else {
                log.info("No {} credentials found in api_credentials subcollection", provider);
            }
            
            // Check 2: Look in the legacy credentials subcollection
            log.info("Checking legacy credentials subcollection for {} credentials", provider);
            DocumentSnapshot legacyDocument = documentStorage.getDocumentDb()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document(provider)
                .get()
                .get();
            
            if (legacyDocument.exists()) {
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", legacyDocument.getString("apiKey"));
                credentials.put("secretKey", legacyDocument.getString("secretKey"));
                
                log.info("Found {} credentials in legacy credentials subcollection for user: {}", provider, userId);
                if (credentials.get("apiKey") != null) {
                    String apiKeyPreview = credentials.get("apiKey").substring(0, Math.min(credentials.get("apiKey").length(), 5)) + "...";
                    log.info("API key found (first 5 chars): {}", apiKeyPreview);
                }
                if (credentials.get("secretKey") != null) {
                    log.info("Secret key found (length): {} chars", credentials.get("secretKey").length());
                }
                
                // Migrate to new structure
                migrateToNewStructure(userId, provider, legacyDocument.getString("apiKey"), legacyDocument.getString("secretKey"));
                
                return credentials;
            } else {
                log.info("No {} credentials found in legacy credentials subcollection", provider);
            }
            
            log.info("No {} credentials found for user: {}", provider, userId);
            return null;
        } catch (Exception e) {
            log.error("Error retrieving {} credentials for user: {}", provider, userId, e);
            return null;
        }
    }
    
    /**
     * Save exchange credentials for a user
     * 
     * @param userId User ID
     * @param provider Exchange provider (e.g., "kraken", "binance")
     * @param apiKey API key
     * @param secretKey Secret key
     * @return true if successful, false otherwise
     */
    public boolean saveExchangeCredentials(String userId, String provider, String apiKey, String secretKey) {
        if (!documentStorage.isReady()) {
            log.warn("Document storage not initialized. Cannot save {} credentials.", provider);
            return false;
        }
        
        try {
            log.info("Saving {} credentials for user: {}", provider, userId);
            
            // Create credentials map
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", provider);
            credentials.put("apiKey", apiKey);
            credentials.put("secretKey", secretKey);
            credentials.put("createdAt", java.time.Instant.now().toString());
            credentials.put("updatedAt", java.time.Instant.now().toString());
            
            // Save to the new api_credentials subcollection
            documentStorage.getDocumentDb()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", provider)
                .get()
                .get()
                .getDocuments()
                .forEach(doc -> doc.getReference().delete());
            
            documentStorage.getDocumentDb()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .document()
                .set(credentials)
                .get();
            
            log.info("Successfully saved {} credentials for user: {}", provider, userId);
            return true;
        } catch (Exception e) {
            log.error("Error saving {} credentials for user: {}", provider, userId, e);
            return false;
        }
    }
    
    /**
     * Migrate credentials from the legacy structure to the new structure
     * 
     * @param userId User ID
     * @param provider Exchange provider
     * @param apiKey API key
     * @param secretKey Secret key
     */
    private void migrateToNewStructure(String userId, String provider, String apiKey, String secretKey) {
        try {
            log.info("Migrating {} credentials from legacy structure to new structure for user: {}", provider, userId);
            
            // Save to the new structure
            saveExchangeCredentials(userId, provider, apiKey, secretKey);
            
            // Optionally, remove from the old structure
            // This is commented out to avoid data loss in case of migration issues
            /*
            documentStorage.getDocumentDb()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document(provider)
                .delete();
            */
            
            log.info("Successfully migrated {} credentials for user: {}", provider, userId);
        } catch (Exception e) {
            log.error("Error migrating {} credentials for user: {}", provider, userId, e);
        }
    }
}
