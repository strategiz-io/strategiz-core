package io.strategiz.service.portfolio;

import io.strategiz.client.kraken.KrakenClient;
import io.strategiz.client.kraken.model.KrakenAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Kraken implementation of the BrokerageService interface
 * Provides Kraken portfolio data via the Kraken API client
 */
public class KrakenBrokerageService implements BrokerageService {

    private static final String PROVIDER_NAME = "kraken";
    private static final Logger log = LoggerFactory.getLogger(KrakenBrokerageService.class);
    private final KrakenClient krakenClient;

    @Autowired
    public KrakenBrokerageService(KrakenClient krakenClient) {
        this.krakenClient = krakenClient;
        log.info("KrakenBrokerageService initialized");
    }

    @Override
    public Map<String, Object> getPortfolioData(Map<String, String> credentials) {
        try {
            log.info("Getting Kraken portfolio data");
            
            // Extract API credentials
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("privateKey");
            
            if (apiKey == null || secretKey == null) {
                log.error("Missing Kraken API credentials");
                return Map.of("error", "Missing Kraken API credentials");
            }
            
            // Get account data from Kraken API
            KrakenAccount account = krakenClient.getAccount(apiKey, secretKey);
            
            // Check for API errors
            if (account.getError() != null && account.getError().length > 0) {
                log.error("Kraken API error: {}", String.join(", ", account.getError()));
                return Map.of("error", "Kraken API error: " + String.join(", ", account.getError()));
            }
            
            // Process and format the data
            return processKrakenAccountData(account);
        } catch (Exception e) {
            log.error("Error getting Kraken portfolio data", e);
            return Map.of("error", "Error getting Kraken portfolio data: " + e.getMessage());
        }
    }

    @Override
    public Object getRawAccountData(Map<String, String> credentials) {
        try {
            log.info("Getting raw Kraken account data");
            
            // Extract API credentials
            String apiKey = credentials.get("apiKey");
            String secretKey = credentials.get("privateKey");
            
            if (apiKey == null || secretKey == null) {
                log.error("Missing Kraken API credentials");
                return Map.of("error", "Missing Kraken API credentials");
            }
            
            // Get account data from Kraken API and return it unmodified
            return krakenClient.getAccount(apiKey, secretKey);
        } catch (Exception e) {
            log.error("Error getting raw Kraken account data", e);
            return Map.of("error", "Error getting raw Kraken account data: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
    
    /**
     * Process Kraken account data into a standardized portfolio format
     *
     * @param account Kraken account data
     * @return Standardized portfolio data
     */
    private Map<String, Object> processKrakenAccountData(KrakenAccount account) {
        Map<String, Object> portfolioData = new HashMap<>();
        Map<String, Object> assets = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        
        if (account.getResult() != null) {
            // Process each asset in the account
            for (Map.Entry<String, Object> entry : account.getResult().entrySet()) {
                try {
                    String assetKey = entry.getKey();
                    String balance = entry.getValue().toString();
                    BigDecimal amount = new BigDecimal(balance);
                    
                    // Skip assets with zero balance
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    // Create asset data
                    Map<String, Object> assetData = new HashMap<>();
                    assetData.put("symbol", getCleanAssetName(assetKey));
                    assetData.put("originalSymbol", assetKey); // Keep the original symbol for reference
                    assetData.put("amount", amount);
                    
                    // Add asset to the collection
                    assets.put(assetKey, assetData);
                } catch (Exception e) {
                    log.warn("Error processing Kraken asset: {}", entry.getKey(), e);
                }
            }
        }
        
        // Add assets to portfolio data
        portfolioData.put("assets", assets);
        portfolioData.put("totalValue", totalValue);
        portfolioData.put("provider", PROVIDER_NAME);
        
        return portfolioData;
    }
    
    /**
     * Clean up Kraken asset names
     * Kraken uses special prefixes (X, Z) for some assets
     *
     * @param assetName Original Kraken asset name
     * @return Cleaned asset name
     */
    private String getCleanAssetName(String assetName) {
        // Handle special cases first
        if (assetName.equals("XXBT")) return "BTC";
        if (assetName.equals("XETH")) return "ETH";
        if (assetName.equals("XXDG")) return "DOGE";
        
        // Handle futures contracts
        if (assetName.endsWith(".F")) {
            String baseName = assetName.substring(0, assetName.length() - 2);
            return getCleanAssetName(baseName) + ".F";
        }
        
        // Handle general X/Z prefixed assets
        if (assetName.length() > 1 && (assetName.startsWith("X") || assetName.startsWith("Z"))) {
            return assetName.substring(1);
        }
        
        // Return as-is for other assets
        return assetName;
    }
}
