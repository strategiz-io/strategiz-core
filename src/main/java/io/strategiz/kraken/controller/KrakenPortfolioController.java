package io.strategiz.kraken.controller;

import io.strategiz.kraken.model.KrakenAccount;
import io.strategiz.kraken.service.FirestoreService;
import io.strategiz.kraken.service.KrakenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for Kraken portfolio-specific endpoints
 * Handles all data processing for the Kraken portfolio page
 */
@RestController
@RequestMapping("/api/kraken/portfolio")
public class KrakenPortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(KrakenPortfolioController.class);

    @Autowired
    private KrakenService krakenService;

    @Autowired
    @Qualifier("krakenFirestoreService")
    private FirestoreService firestoreService;

    /**
     * Get portfolio data for the Kraken portfolio page
     * This endpoint:
     * 1. Fetches raw data from Kraken API
     * 2. Calculates profit/loss and other metrics
     * 3. Returns processed data ready for display
     *
     * @param email User email for retrieving API credentials
     * @return Processed portfolio data with all necessary metrics
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@RequestParam String email) {
        logger.info("Getting Kraken portfolio data for user: {}", email);
        
        try {
            // Get user's API credentials from Firestore
            Map<String, String> credentials = firestoreService.getKrakenCredentials(email);
            
            if (credentials == null || credentials.isEmpty() || 
                !credentials.containsKey("apiKey") || !credentials.containsKey("secretKey")) {
                logger.warn("No valid Kraken credentials found for user: {}", email);
                return ResponseEntity.ok(createErrorResponse("No valid Kraken API credentials found"));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Get account balance from Kraken API
            KrakenAccount accountData = krakenService.getAccount(apiKey, secretKey);
            
            if (accountData == null || accountData.getError() != null && accountData.getError().length > 0) {
                String errorMsg = accountData != null && accountData.getError() != null ? 
                    String.join(", ", accountData.getError()) : "Unknown error";
                logger.error("Error getting Kraken account data: {}", errorMsg);
                return ResponseEntity.ok(createErrorResponse("Error retrieving Kraken account data: " + errorMsg));
            }
            
            // Process the data and calculate metrics
            Map<String, Object> processedData = processPortfolioData(accountData);
            
            // Include the raw data for transparency
            processedData.put("rawData", accountData);
            
            return ResponseEntity.ok(processedData);
            
        } catch (Exception e) {
            logger.error("Error getting Kraken portfolio data", e);
            return ResponseEntity.ok(createErrorResponse("Error retrieving Kraken portfolio data: " + e.getMessage()));
        }
    }
    
    /**
     * Process the raw balance data and calculate portfolio metrics
     */
    private Map<String, Object> processPortfolioData(KrakenAccount accountData) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> positions = new ArrayList<>();
        double totalValue = 0.0;
        
        // Extract balance data
        Map<String, Object> balances = accountData.getResult();
        
        if (balances != null) {
            for (Map.Entry<String, Object> entry : balances.entrySet()) {
                String asset = entry.getKey();
                double quantity = Double.parseDouble(entry.getValue().toString());
                
                // Skip zero balances
                if (quantity <= 0) {
                    continue;
                }
                
                // Create position data
                Map<String, Object> position = new HashMap<>();
                position.put("asset", asset);
                position.put("quantity", quantity);
                
                // For now, we'll use placeholder values for price and market value
                // In a real implementation, you would fetch current prices from Kraken
                double price = estimateAssetPrice(asset);
                double marketValue = quantity * price;
                totalValue += marketValue;
                
                position.put("price", price);
                position.put("marketValue", marketValue);
                
                // Calculate profit/loss (this would require historical purchase data)
                // For demonstration, we'll use a placeholder calculation
                double estimatedProfit = calculateEstimatedProfit(asset, quantity, price);
                position.put("profit", estimatedProfit);
                position.put("profitPercentage", estimatedProfit > 0 ? (estimatedProfit / marketValue) * 100 : 0);
                
                positions.add(position);
            }
        }
        
        // Sort positions by market value (descending)
        positions.sort((p1, p2) -> {
            double mv1 = (double) p1.get("marketValue");
            double mv2 = (double) p2.get("marketValue");
            return Double.compare(mv2, mv1);
        });
        
        result.put("connected", true);
        result.put("totalValue", totalValue);
        result.put("positions", positions);
        
        return result;
    }
    
    /**
     * Estimate asset price (placeholder implementation)
     * In a real implementation, you would fetch current prices from Kraken
     */
    private double estimateAssetPrice(String asset) {
        // Handle USD specially
        if (asset.equals("ZUSD")) {
            return 1.0;
        }
        
        // For demonstration purposes, use placeholder prices
        Map<String, Double> placeholderPrices = new HashMap<>();
        placeholderPrices.put("XXBT", 60000.0);  // Bitcoin
        placeholderPrices.put("XETH", 3000.0);   // Ethereum
        placeholderPrices.put("XXRP", 0.5);      // Ripple
        placeholderPrices.put("XLTC", 80.0);     // Litecoin
        placeholderPrices.put("XXLM", 0.1);      // Stellar
        placeholderPrices.put("XDOT", 15.0);     // Polkadot
        placeholderPrices.put("XADA", 0.4);      // Cardano
        
        return placeholderPrices.getOrDefault(asset, 1.0);
    }
    
    /**
     * Calculate estimated profit (placeholder implementation)
     * In a real implementation, you would fetch cost basis from a database
     */
    private double calculateEstimatedProfit(String asset, double quantity, double currentPrice) {
        // This is a placeholder - in a real implementation, you would:
        // 1. Fetch historical purchase data for this asset
        // 2. Calculate cost basis
        // 3. Compare current value to cost basis
        
        // For demonstration, we'll use a random profit percentage between -20% and +50%
        Random random = new Random();
        double profitPercentage = (random.nextDouble() * 70) - 20; // -20% to +50%
        double costBasis = (currentPrice * quantity) / (1 + (profitPercentage / 100));
        
        return (currentPrice * quantity) - costBasis;
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
}
