package strategiz.service.walletaddress.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import io.strategiz.client.walletaddress.model.WalletAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Service for managing wallet address operations using Firestore.
 */
@Service("walletAddressFirestoreService")
public class WalletAddressFirestoreService {

    private static final Logger log = LoggerFactory.getLogger(WalletAddressFirestoreService.class);

    private static final String USERS_COLLECTION = "users";
    private static final String API_CREDENTIALS_COLLECTION = "api_credentials";
    private static final String WALLET_ADDRESSES_DOC = "wallet_addresses";

    /**
     * Save wallet addresses for a user in Firestore under api_credentials
     */
    public void saveWalletAddresses(String userId, List<WalletAddress> wallets) {
        if (FirestoreClient.getFirestore() == null) {
            log.warn("Firebase not initialized. Cannot save wallet addresses.");
            return;
        }
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("wallets", wallets);
            FirestoreClient.getFirestore()
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(API_CREDENTIALS_COLLECTION)
                .document(WALLET_ADDRESSES_DOC)
                .set(data)
                .get();
            log.info("Saved wallet addresses for user: {}", userId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving wallet addresses: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieve wallet addresses for a user from Firestore under api_credentials
     */
    public List<WalletAddress> getWalletAddresses(String userId) {
        List<WalletAddress> wallets = new ArrayList<>();
        if (FirestoreClient.getFirestore() == null) {
            log.warn("Firebase not initialized. Cannot retrieve wallet addresses.");
            return wallets;
        }
        try {
            DocumentSnapshot doc = FirestoreClient.getFirestore()
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(API_CREDENTIALS_COLLECTION)
                .document(WALLET_ADDRESSES_DOC)
                .get()
                .get();
            if (doc.exists() && doc.contains("wallets")) {
                List<Map<String, Object>> walletMaps = (List<Map<String, Object>>) doc.get("wallets");
                for (Map<String, Object> map : walletMaps) {
                    String blockchain = (String) map.get("blockchain");
                    String address = (String) map.get("address");
                    wallets.add(new WalletAddress(blockchain, address));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving wallet addresses: {}", e.getMessage(), e);
        }
        return wallets;
    }
}
