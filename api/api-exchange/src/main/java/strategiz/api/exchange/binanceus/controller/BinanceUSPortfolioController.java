package strategiz.api.exchange.binanceus.controller;

import strategiz.data.exchange.binanceus.model.Account;
import strategiz.data.exchange.binanceus.model.Balance;
import strategiz.data.exchange.binanceus.model.TickerPrice;
import strategiz.service.exchange.binanceus.BinanceUSService;
import strategiz.service.exchange.binanceus.FirestoreService;
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
            // Process each balance entry
            for (Balance balance : balances) {
                double free = Double.parseDouble(balance.getFree());
                double locked = Double.parseDouble(balance.getLocked());
                double total = free + locked;
                
                // Skip zero balances
                if (total <= 0.0) {
                    continue;
                }
                
                String asset = balance.getAsset();
                
                // Calculate USD value for this asset
                double usdValue = 0.0;
                
                // For USD stablecoins, use 1:1 value
                if (asset.equals("USD") || asset.equals("USDT") || asset.equals("USDC") || asset.equals("BUSD") || asset.equals("DAI")) {
                    usdValue = total;
                } else {
                    // For other assets, look up the price
                    String symbol = asset + "USD";
                    
                    // Check if we have a price for this symbol
                    if (priceMap.containsKey(symbol)) {
                        double price = priceMap.get(symbol);
                        usdValue = total * price;
                    } else {
                        // Try alternative symbols like BTCUSDT
                        symbol = asset + "USDT";
                        if (priceMap.containsKey(symbol)) {
                            double price = priceMap.get(symbol);
                            usdValue = total * price;
                        }
                    }
                }
                
                // Add to total portfolio value
                totalValue += usdValue;
                
                // Create position entry
                Map<String, Object> position = new HashMap<>();
                position.put("asset", asset);
                position.put("free", free);
                position.put("locked", locked);
                position.put("total", total);
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
