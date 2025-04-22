package io.strategiz.coinbase.controller;

import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.model.TickerPrice;
import io.strategiz.coinbase.service.CoinbaseCloudService;
import io.strategiz.coinbase.service.CoinbaseService;
import io.strategiz.coinbase.service.firestore.FirestoreService;
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
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
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
                
                String asset = account.getBalance().getCurrency();
                String name = account.getName();
                
                // Create position data
                Map<String, Object> position = new HashMap<>();
                position.put("asset", asset);
                position.put("balance", account.getBalance().getAmount());
                position.put("name", name);
                
                // For Coinbase Cloud, market value is included in account data
                double marketValue = account.getUsdValue();
                if (marketValue > 0) {
                    totalValue += marketValue;
                    
                    // Use market value to calculate estimated price
                    double price = quantity > 0 ? marketValue / quantity : 0;
                    position.put("price", price);
                    position.put("marketValue", marketValue);
                } else {
                    // Fallback to old method as backup
                    try {
                        TickerPrice tickerPrice = coinbaseService.getTickerPrice(asset, "USD");
                        if (tickerPrice != null) {
                            double price = Double.parseDouble(tickerPrice.getAmount());
                            marketValue = quantity * price;
                            totalValue += marketValue;
                            
                            position.put("price", price);
                            position.put("marketValue", marketValue);
                        } else {
                            logger.warn("Could not get price for asset {}", asset);
                            position.put("price", 0);
                            position.put("marketValue", 0);
                        }
                    } catch (Exception e) {
                        logger.warn("Could not get price for asset {}: {}", asset, e.getMessage());
                        position.put("price", 0);
                        position.put("marketValue", 0);
                    }
                }
                
                positions.add(position);
            }
        }
        
        // Sort positions by market value (descending)
        positions.sort((p1, p2) -> {
            double mv1 = (double) p1.getOrDefault("marketValue", 0.0);
            double mv2 = (double) p2.getOrDefault("marketValue", 0.0);
            return Double.compare(mv2, mv1);
        });
        
        result.put("connected", true);
        result.put("totalValue", totalValue);
        result.put("positions", positions);
        
        return result;
    }
    
    /**
     * Create an error response
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("connected", false);
        response.put("error", errorMessage);
        response.put("totalValue", 0.0);
        response.put("positions", new ArrayList<>());
        return response;
    }
    
    /**
     * Health check endpoint
     * Verifies if user has valid Coinbase credentials
     */
    @GetMapping("/health")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Map<String, Object>> healthCheck(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user's API credentials from Firestore
            Map<String, String> credentials = firestoreService.getCoinbaseCredentials(email);
            
            boolean credentialsFound = credentials != null && !credentials.isEmpty() &&
                credentials.containsKey("apiKey") && credentials.containsKey("privateKey");
            
            response.put("credentialsFound", credentialsFound);
            
            if (credentialsFound) {
                // Test API connection
                String apiKey = credentials != null ? credentials.get("apiKey") : null;
                String privateKey = credentials != null ? credentials.get("privateKey") : null;
                
                if (apiKey == null || privateKey == null) {
                    response.put("connectionSuccessful", false);
                    return ResponseEntity.ok(response);
                }
                
                boolean connectionSuccessful = coinbaseCloudService.testConnection(apiKey, privateKey);
                
                response.put("connectionSuccessful", connectionSuccessful);
            } else {
                response.put("connectionSuccessful", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking Coinbase health", e);
            response.put("credentialsFound", false);
            response.put("connectionSuccessful", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
