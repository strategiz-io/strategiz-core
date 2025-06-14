package io.strategiz.service.exchange.coinbase.advanced;

import io.strategiz.service.exchange.coinbase.FirestoreService;
import io.strategiz.client.coinbase.advanced.CoinbaseAdvancedTradeClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for Coinbase Advanced Trade API
 * Provides methods for interacting with the Coinbase Advanced Trade API
 */
@Service
public class CoinbaseAdvancedTradeService {
    private static final Logger log = LoggerFactory.getLogger(CoinbaseAdvancedTradeService.class);

    @Autowired
    private FirestoreService firestoreService;
    
    @Autowired
    private CoinbaseAdvancedTradeClient coinbaseClient;

    /**
     * Get raw account data from Coinbase Advanced Trade API
     * 
     * @param email User email
     * @return Raw account data
     */
    public ResponseEntity<String> getRawAccountData(String email) {
        log.info("Getting raw account data from Coinbase Advanced Trade API for user: {}", email);
        
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
}
