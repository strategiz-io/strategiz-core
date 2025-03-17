package io.strategiz.binanceus.controller;

import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import io.strategiz.binanceus.model.TickerPrice;
import io.strategiz.binanceus.service.BinanceUSService;
import io.strategiz.binanceus.service.FirestoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for Binance US portfolio-specific endpoints
 * Handles all data processing for the Binance US portfolio page
 */
@RestController
@RequestMapping("/api/binanceus/portfolio")
public class BinanceUSPortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(BinanceUSPortfolioController.class);

    @Autowired
    private BinanceUSService binanceUSService;

    @Autowired
    private FirestoreService firestoreService;

    /**
     * Get portfolio data for the Binance US portfolio page
     * This endpoint:
     * 1. Fetches raw data from Binance US API
     * 2. Calculates profit/loss and other metrics
     * 3. Returns processed data ready for display
     *
     * @param email User email for retrieving API credentials
     * @return Processed portfolio data with all necessary metrics
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getPortfolioData(@RequestParam String email) {
        logger.info("Getting Binance US portfolio data for user: {}", email);
        
        try {
            // Get user's API credentials from Firestore
            Map<String, String> credentials = firestoreService.getBinanceUSCredentials(email);
            
            if (credentials == null || credentials.isEmpty() || 
                !credentials.containsKey("apiKey") || !credentials.containsKey("secretKey")) {
                logger.warn("No valid Binance US credentials found for user: {}", email);
                return ResponseEntity.ok(createErrorResponse("No valid Binance US API credentials found"));
            }
            
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("secretKey");
            
            // Get account data from Binance US API
            Account accountData = binanceUSService.getAccount(apiKey, secretKey);
            
            if (accountData == null || accountData.getBalances() == null) {
                logger.error("Error getting Binance US account data");
                return ResponseEntity.ok(createErrorResponse("Error retrieving Binance US account data"));
            }
            
            // Get ticker prices for all symbols
            List<TickerPrice> tickerPrices = binanceUSService.getTickerPrices();
            
            // Process the data and calculate metrics
            Map<String, Object> processedData = processPortfolioData(accountData, tickerPrices);
            
            // Include the raw data for transparency - this is the completely unmodified object from the API
            processedData.put("rawData", accountData);
            
            return ResponseEntity.ok(processedData);
            
        } catch (Exception e) {
            logger.error("Error getting Binance US portfolio data", e);
            return ResponseEntity.ok(createErrorResponse("Error retrieving Binance US portfolio data: " + e.getMessage()));
        }
    }
    
    /**
     * Process the raw balance data and calculate portfolio metrics
     */
    private Map<String, Object> processPortfolioData(Account accountData, List<TickerPrice> tickerPrices) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> positions = new ArrayList<>();
        double totalValue = 0.0;
        
        // Create a map of symbol to price for easier lookup
        Map<String, Double> priceMap = new HashMap<>();
        for (TickerPrice ticker : tickerPrices) {
            priceMap.put(ticker.getSymbol(), Double.parseDouble(ticker.getPrice()));
        }
        
        // Extract balance data
        List<Balance> balances = accountData.getBalances();
        
        if (balances != null) {
            for (Balance balance : balances) {
                // Skip zero balances
                double free = Double.parseDouble(balance.getFree());
                double locked = Double.parseDouble(balance.getLocked());
                double quantity = free + locked;
                
                if (quantity <= 0) {
                    continue;
                }
                
                String asset = balance.getAsset();
                
                // Create position data
                Map<String, Object> position = new HashMap<>();
                position.put("asset", asset);
                position.put("quantity", quantity);
                position.put("free", free);
                position.put("locked", locked);
                
                // Calculate USD value
                double price = getAssetPrice(asset, priceMap);
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
     * Get asset price from ticker info
     */
    private double getAssetPrice(String asset, Map<String, Double> priceMap) {
        // Handle USD specially
        if (asset.equals("USD")) {
            return 1.0;
        }
        
        // For other assets, look up in price map
        String symbol = asset + "USD";
        Double price = priceMap.get(symbol);
        
        if (price != null) {
            return price;
        }
        
        // For assets without a direct USD pair, try to find BTC pair and convert
        symbol = asset + "BTC";
        Double btcPrice = priceMap.get(symbol);
        Double btcUsdPrice = priceMap.get("BTCUSD");
        
        if (btcPrice != null && btcUsdPrice != null) {
            return btcPrice * btcUsdPrice;
        }
        
        // Default to 0 if price not found
        return 0.0;
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
