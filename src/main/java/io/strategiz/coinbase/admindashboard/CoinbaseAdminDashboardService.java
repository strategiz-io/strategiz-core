package io.strategiz.coinbase.admindashboard;

import io.strategiz.coinbase.service.firestore.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CoinbaseAdminDashboardService {
    @Autowired
    private FirestoreService firestoreService;
    @Autowired
    private CoinbaseAdminDashboardClient coinbaseClient;

    public ResponseEntity<String> getRawAccountData(String email) {
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        String apiKey = credentials.get("apiKey");
        String privateKey = credentials.get("privateKey");
        return coinbaseClient.getAccounts(apiKey, privateKey);
    }

    // Placeholder for portfolio data logic
    public ResponseEntity<String> getPortfolioData(String email) {
        // TODO: Implement portfolio summary logic
        return ResponseEntity.ok("Portfolio data endpoint not yet implemented");
    }

    // Credential check logic
    public ResponseEntity<String> checkCredentials(String email) {
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        return ResponseEntity.ok("Credentials found");
    }
}
