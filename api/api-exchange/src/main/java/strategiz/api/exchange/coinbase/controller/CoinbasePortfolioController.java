package strategiz.api.exchange.coinbase.controller;

import strategiz.data.exchange.coinbase.model.Account;
import strategiz.data.exchange.coinbase.model.TickerPrice;
import strategiz.service.exchange.coinbase.CoinbaseCloudService;
import strategiz.service.exchange.coinbase.CoinbaseService;
import strategiz.service.exchange.coinbase.FirestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for Coinbase portfolio-specific endpoints
 * Handles all data processing for the Coinbase portfolio page
 */
@RestController
@RequestMapping("/api/coinbase/portfolio")
public class CoinbasePortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(CoinbasePortfolioController.class);

    @Autowired
    private CoinbaseService coinbaseService;
    
    @Autowired
    private CoinbaseCloudService coinbaseCloudService;

    @Autowired
    private FirestoreService firestoreService;

    /**
     * Get portfolio data for the Coinbase portfolio page
     * This endpoint:
     * 1. Fetches raw data from Coinbase API
     * 2. Calculates metrics like total value
     * 3. Returns processed data ready for display
     *
     * @param email User email for retrieving API credentials
     * @return Processed portfolio data with all necessary metrics
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@RequestParam String email) {
        logger.info("Getting Coinbase portfolio data for user: {}", email);
        
        try {
            // Get user's API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(email);
            
            if (credentials == null || credentials.isEmpty() || 
                !credentials.containsKey("apiKey") || !credentials.containsKey("privateKey")) {
                logger.warn("No valid Coinbase credentials found for user: {}", email);
                return ResponseEntity.ok(createErrorResponse("No valid Coinbase API credentials found"));
            }
            
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            // Configure Coinbase Cloud service with the user's credentials
            coinbaseCloudService.configure(apiKey, privateKey);
            
            // Get accounts from Coinbase Cloud API
            List<Account> accounts = coinbaseCloudService.getAccountBalances(apiKey, privateKey);
            
            if (accounts == null) {
                logger.error("Error getting Coinbase account data");
                return ResponseEntity.ok(createErrorResponse("Error retrieving Coinbase account data"));
            }
            
            // Process the data and calculate metrics
            Map<String, Object> processedData = processPortfolioData(accounts);
            
            // Include the raw accounts data for transparency
            processedData.put("rawData", accounts);
            
            return ResponseEntity.ok(processedData);
            
        } catch (Exception e) {
            logger.error("Error getting Coinbase portfolio data", e);
            return ResponseEntity.ok(createErrorResponse("Error retrieving Coinbase portfolio data: " + e.getMessage()));
        }
    }
    
    /**
     * Process the accounts data and calculate portfolio metrics
     */
    private Map<String, Object> processPortfolioData(List<Account> accounts) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> positions = new ArrayList<>();
        double totalValue = 0.0;
        
        if (accounts != null) {
            for (Account account : accounts) {
                // Skip accounts with zero balance
                double quantity = Double.parseDouble(account.getBalance().getAmount());
                if (quantity <= 0) {
                    continue;
                }
                
                String currency = account.getCurrency();
                double usdValue = account.getUsdValue();
                
                // Add to total portfolio value
                totalValue += usdValue;
                
                // Create position entry
                Map<String, Object> position = new HashMap<>();
                position.put("id", account.getId());
                position.put("name", account.getName());
                position.put("currency", currency);
                position.put("quantity", quantity);
                position.put("usdValue", usdValue);
                
                positions.add(position);
            }
            
            // Sort positions by USD value (descending)
            positions.sort((p1, p2) -> {
                double v1 = (double) p1.get("usdValue");
                double v2 = (double) p2.get("usdValue");
                return Double.compare(v2, v1);
            });
        }
        
        // Populate result
        result.put("status", "success");
        result.put("positions", positions);
        result.put("totalValue", totalValue);
        result.put("assetCount", positions.size());
        
        return result;
    }
    
    /**
     * Create an error response with the given message
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }
}
