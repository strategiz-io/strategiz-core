package io.strategiz.service.exchange.coinbase.admin;

import io.strategiz.service.exchange.coinbase.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for Coinbase Admin Dashboard
 * Provides methods for retrieving and processing Coinbase account data for admin purposes
 */
@Service
@Slf4j
public class CoinbaseAdminDashboardService {
    @Autowired
    private FirestoreService firestoreService;
    
    @Autowired
    private CoinbaseAdminDashboardClient coinbaseClient;

    /**
     * Get raw account data from Coinbase
     * 
     * @param email User email
     * @return Raw account data
     */
    public ResponseEntity<String> getRawAccountData(String email) {
        log.info("Getting raw account data for user: {}", email);
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            log.warn("Missing API credentials for user: {}", email);
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        String apiKey = credentials.get("apiKey");
        String privateKey = credentials.get("privateKey");
        
        // Important: Always use real API data, never mock responses
        return coinbaseClient.getAccounts(apiKey, privateKey);
    }

    /**
     * Get portfolio data from Coinbase
     * 
     * @param email User email
     * @return Portfolio data
     */
    public ResponseEntity<String> getPortfolioData(String email) {
        log.info("Getting portfolio data for user: {}", email);
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            log.warn("Missing API credentials for user: {}", email);
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        
        // TODO: Implement portfolio summary logic with real API data
        return ResponseEntity.ok("Portfolio data endpoint not yet implemented");
    }

    /**
     * Check if Coinbase credentials are valid
     * 
     * @param email User email
     * @return Credential status
     */
    public ResponseEntity<String> checkCredentials(String email) {
        log.info("Checking credentials for user: {}", email);
        var credentials = firestoreService.getCoinbaseCredentials(email);
        if (credentials == null || !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
            log.warn("Missing API credentials for user: {}", email);
            return ResponseEntity.status(404).body("Missing API credentials");
        }
        log.info("Credentials found for user: {}", email);
        return ResponseEntity.ok("Credentials found");
    }
}
