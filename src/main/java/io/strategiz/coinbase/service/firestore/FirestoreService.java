package io.strategiz.coinbase.service.firestore;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Service for interacting with Firestore database for Coinbase credentials
 */
@Service("coinbaseFirestoreService")
@Slf4j
public class FirestoreService {

    /**
     * Get Coinbase credentials for a user
     * 
     * @param userId User ID
     * @return Map of credentials (apiKey, privateKey, passphrase)
     */
    public Map<String, String> getCoinbaseCredentials(String userId) {
        log.info("Getting Coinbase credentials for user: {}", userId);
        
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot retrieve Coinbase credentials.");
            return null;
        }
        
        try {
            // First try the provider-based query in api_credentials subcollection (like BinanceUS)
            log.info("Checking api_credentials subcollection with provider=coinbase query");
            DocumentSnapshot credentialDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .whereEqualTo("provider", "coinbase")
                .limit(1)
                .get()
                .get()
                .getDocuments()
                .stream()
                .findFirst()
                .orElse(null);
                
            if (credentialDoc != null && credentialDoc.exists()) {
                log.info("Found Coinbase credentials in api_credentials via provider query");
                Map<String, Object> data = credentialDoc.getData();
                Map<String, String> result = new HashMap<>();
                if (data != null) {
                    String apiKey = (String) data.get("apiKey");
                    String privateKey = (String) data.get("privateKey");
                    String passphrase = (String) data.get("passphrase");
                    
                    // Check if credentials are valid
                    if (apiKey == null || apiKey.isEmpty()) {
                        log.warn("API key from Firestore is null or empty for user: {}", userId);
                    } else {
                        log.info("Retrieved valid API key from Firestore for user: {} (length: {})", userId, apiKey.length());
                    }
                    
                    if (privateKey == null || privateKey.isEmpty()) {
                        log.warn("Private key from Firestore is null or empty for user: {}", userId);
                    } else {
                        log.info("Retrieved valid private key from Firestore for user: {} (length: {})", userId, privateKey.length());
                        // Log some details about the private key format to help diagnose issues
                        boolean containsDashes = privateKey.contains("-");
                        boolean containsUnderscores = privateKey.contains("_");
                        boolean containsEquals = privateKey.contains("=");
                        log.debug("Private key format - contains dashes: {}, contains underscores: {}, contains equals: {}",
                                 containsDashes, containsUnderscores, containsEquals);
                    }
                    
                    if (passphrase == null || passphrase.isEmpty()) {
                        log.warn("Passphrase from Firestore is null or empty for user: {}", userId);
                    } else {
                        log.info("Retrieved valid passphrase from Firestore for user: {}", userId);
                    }
                    
                    result.put("apiKey", apiKey);
                    result.put("privateKey", privateKey);
                    result.put("passphrase", passphrase);
                    return result;
                }
            }
            
            // Try direct document 'coinbase' in api_credentials subcollection
            log.info("Checking api_credentials/coinbase document");
            DocumentSnapshot document = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .document("coinbase")
                .get()
                .get();
            
            if (document.exists()) {
                log.info("Found document in api_credentials/coinbase");
                
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", document.getString("apiKey"));
                credentials.put("privateKey", document.getString("privateKey"));
                credentials.put("passphrase", document.getString("passphrase"));
                
                // If privateKey is not found, fall back to secretKey for backward compatibility
                if (credentials.get("privateKey") == null) {
                    log.info("privateKey not found, falling back to secretKey");
                    credentials.put("privateKey", document.getString("secretKey"));
                }
                
                return credentials;
            }
            
            // Try provider-based query in legacy credentials subcollection
            log.info("Checking legacy credentials subcollection with provider=coinbase query");
            QuerySnapshot legacyQuerySnapshot = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .whereEqualTo("provider", "coinbase")
                .get()
                .get();
            
            if (legacyQuerySnapshot != null && !legacyQuerySnapshot.isEmpty()) {
                DocumentSnapshot legacyDoc = legacyQuerySnapshot.getDocuments().get(0);
                log.info("Found Coinbase credentials in legacy credentials via provider query");
                
                Map<String, String> credentials = new HashMap<>();
                credentials.put("apiKey", legacyDoc.getString("apiKey"));
                credentials.put("passphrase", legacyDoc.getString("passphrase"));
                
                // Try privateKey first, then secretKey for backward compatibility
                String privateKey = legacyDoc.getString("privateKey");
                if (privateKey == null) {
                    log.info("privateKey not found in legacy collection, using secretKey");
                    privateKey = legacyDoc.getString("secretKey");
                }
                credentials.put("privateKey", privateKey);
                
                return credentials;
            }
            
            // Try direct document 'coinbase' in legacy credentials subcollection
            log.info("Checking credentials/coinbase document");
            DocumentSnapshot legacyDoc = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("credentials")
                .document("coinbase")
                .get()
                .get();
            
            if (!legacyDoc.exists()) {
                log.warn("Coinbase credentials not found in any location for user: {}", userId);
                return null;
            }
            
            log.info("Found Coinbase credentials in credentials/coinbase");
            
            Map<String, String> credentials = new HashMap<>();
            credentials.put("apiKey", legacyDoc.getString("apiKey"));
            
            // Try privateKey first, then secretKey for backward compatibility
            String privateKey = legacyDoc.getString("privateKey");
            if (privateKey == null) {
                log.info("privateKey not found in legacy collection, using secretKey");
                privateKey = legacyDoc.getString("secretKey");
            }
            credentials.put("privateKey", privateKey);
            
            return credentials;
        } catch (Exception e) {
            log.error("Error retrieving Coinbase credentials: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Save Coinbase API credentials to Firestore
     * 
     * @param userId User ID
     * @param apiKey API key
     * @param privateKey Private key (can also be referred to as secretKey in other contexts)
     * @param passphrase Passphrase for the Coinbase API
     * @return true if successful, false otherwise
     */
    public boolean saveCoinbaseCredentials(String userId, String apiKey, String privateKey, String passphrase) {
        log.info("Saving Coinbase credentials for user: {}", userId);
        
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Cannot save Coinbase credentials.");
            return false;
        }
        
        try {
            // Create a map of the credentials with provider field for consistency
            Map<String, Object> credentials = new HashMap<>();
            credentials.put("provider", "coinbase");
            credentials.put("apiKey", apiKey);
            credentials.put("privateKey", privateKey);
            credentials.put("passphrase", passphrase);
            // For backward compatibility, also store as secretKey
            credentials.put("secretKey", privateKey);
            credentials.put("updatedAt", System.currentTimeMillis());
            
            // Save to the api_credentials subcollection (preferred location)
            WriteResult result = FirestoreClient.getFirestore()
                .collection("users")
                .document(userId)
                .collection("api_credentials")
                .document("coinbase")
                .set(credentials)
                .get();
            
            log.info("Coinbase credentials saved successfully at time: {}", result.getUpdateTime());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving Coinbase credentials: {}", e.getMessage(), e);
            return false;
        }
    }
}
