package io.strategiz.service.exchange.coinbase.advanced;

import io.strategiz.service.exchange.coinbase.FirestoreService;
import io.strategiz.client.coinbase.advanced.CoinbaseAdvancedTradeClient;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for Coinbase Advanced Trade API
 * Provides methods for interacting with the Coinbase Advanced Trade API
 */
@Service
public class CoinbaseAdvancedTradeService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-exchange";
    }

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
