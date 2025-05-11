package io.strategiz.coinbase.advanced;

import io.strategiz.coinbase.service.firestore.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CoinbaseAdvancedTradeService {
    @Autowired
    private FirestoreService firestoreService;
    @Autowired
    private CoinbaseAdvancedTradeClient coinbaseClient;

    public ResponseEntity<String> getRawAccountData(String email) {
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        String apiKey = credentials.get("apiKey");
        String privateKey = credentials.get("privateKey");
        return coinbaseClient.getAccounts(apiKey, privateKey);
    }
}
