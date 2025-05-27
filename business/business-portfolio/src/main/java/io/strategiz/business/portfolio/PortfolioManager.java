package io.strategiz.business.portfolio;

import io.americanexpress.synapse.framework.manager.BaseManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import io.strategiz.client.binanceus.BinanceUSClient;
import io.strategiz.client.kraken.KrakenClient;
import io.strategiz.client.kraken.model.KrakenAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/**
 * Core business logic for portfolio operations.
 * This class contains domain logic that can be reused across different services.
 * Implements Synapse BaseManager pattern.
 */
@Slf4j
@Component
public class PortfolioManager extends BaseManager {

    private final KrakenClient krakenClient;
    private final BinanceUSClient binanceUSClient;

    @Autowired
    public PortfolioManager(KrakenClient krakenClient, BinanceUSClient binanceUSClient) {
        this.krakenClient = krakenClient;
        this.binanceUSClient = binanceUSClient;
        log.info("PortfolioManager initialized with exchange clients");
    }

    /**
     * Aggregates portfolio data from multiple exchanges using Synapse patterns
     * 
     * @param userId The user ID to fetch portfolio data for
     * @return Structured portfolio data
     */
    public PortfolioData getAggregatedPortfolioData(String userId) {
        log.info("Getting aggregated portfolio data for user: {}", userId);
        
        try {
            // Create portfolio data object
            PortfolioData portfolioData = new PortfolioData();
            portfolioData.setUserId(userId);
            portfolioData.setTotalValue(BigDecimal.ZERO);
            portfolioData.setDailyChange(BigDecimal.ZERO);
            portfolioData.setDailyChangePercent(BigDecimal.ZERO);
            
            // Create exchanges collection
            Map<String, PortfolioData.ExchangeData> exchanges = new HashMap<>();
            
            // Get credentials for each exchange (in a real implementation, these would be retrieved from a secure store)
            Map<String, Map<String, String>> credentials = getExchangeCredentials(userId);
            
            // Process Kraken data if credentials are available
            if (credentials.containsKey("kraken")) {
                PortfolioData.ExchangeData krakenData = getKrakenPortfolioData(credentials.get("kraken"));
                if (krakenData != null) {
                    exchanges.put("kraken", krakenData);
                    portfolioData.setTotalValue(portfolioData.getTotalValue().add(krakenData.getValue()));
                }
            }
            
            // Process Binance US data if credentials are available
            if (credentials.containsKey("binanceus")) {
                PortfolioData.ExchangeData binanceData = getBinanceUSPortfolioData(credentials.get("binanceus"));
                if (binanceData != null) {
                    exchanges.put("binanceus", binanceData);
                    portfolioData.setTotalValue(portfolioData.getTotalValue().add(binanceData.getValue()));
                }
            }
            
            // Set exchanges collection in portfolio data
            portfolioData.setExchanges(exchanges);
            
            return portfolioData;
        } catch (Exception e) {
            log.error("Error getting portfolio data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve portfolio data", e);
        }
    }
    
    /**
     * Gets exchange credentials for the user
     * In a real implementation, this would retrieve credentials from a secure storage solution
     * 
     * @param userId User ID
     * @return Map of exchange credentials
     */
    private Map<String, Map<String, String>> getExchangeCredentials(String userId) {
        // In a real implementation, these would be retrieved from a secure store like Firebase Firestore
        // As mentioned in the memories: "The new structure will place all API keys under a subcollection
        // called 'api_credentials' directly under each user document"
        
        Map<String, Map<String, String>> credentials = new HashMap<>();
        
        try {
            // For now, return sample credentials for testing
            // In production, these would be retrieved from the user's api_credentials collection
            
            // Kraken credentials
            Map<String, String> krakenCreds = new HashMap<>();
            krakenCreds.put("apiKey", "sample_kraken_api_key");
            krakenCreds.put("privateKey", "sample_kraken_private_key");
            credentials.put("kraken", krakenCreds);
            
            // Binance US credentials
            Map<String, String> binanceCreds = new HashMap<>();
            binanceCreds.put("apiKey", "sample_binance_api_key");
            binanceCreds.put("privateKey", "sample_binance_private_key");
            credentials.put("binanceus", binanceCreds);
            
            return credentials;
        } catch (Exception e) {
            log.error("Error retrieving exchange credentials for user {}: {}", userId, e.getMessage(), e);
            return credentials; // Return empty credentials on error
        }
    }
    
    /**
     * Gets portfolio data from Kraken exchange
     * 
     * @param credentials Kraken API credentials
     * @return Structured exchange data
     */
    private PortfolioData.ExchangeData getKrakenPortfolioData(Map<String, String> credentials) {
        log.info("Getting Kraken portfolio data");
        
        try {
            // Extract API credentials
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            if (apiKey == null || privateKey == null) {
                log.error("Missing Kraken API credentials");
                return null;
            }
            
            // Create exchange data object
            PortfolioData.ExchangeData krakenData = new PortfolioData.ExchangeData();
            krakenData.setId("kraken");
            krakenData.setName("Kraken");
            krakenData.setValue(BigDecimal.ZERO);
            krakenData.setAssets(new HashMap<>());
            
            // Get account data from Kraken API
            KrakenAccount account = krakenClient.getAccount(apiKey, privateKey);
            
            // Check for API errors
            if (account.getError() != null && account.getError().length > 0) {
                log.error("Kraken API error: {}", String.join(", ", account.getError()));
                return null;
            }
            
            // Process and format the data
            BigDecimal totalValue = BigDecimal.ZERO;
            Map<String, PortfolioData.AssetData> assets = new HashMap<>();
            
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
                        PortfolioData.AssetData assetData = new PortfolioData.AssetData();
                        assetData.setSymbol(getCleanAssetName(assetKey));
                        assetData.setName(getAssetFullName(assetData.getSymbol()));
                        assetData.setAmount(amount);
                        assetData.setValue(BigDecimal.ZERO); // Would be calculated based on current price
                        
                        // Add asset to the collection
                        assets.put(assetKey, assetData);
                    } catch (Exception e) {
                        log.warn("Error processing Kraken asset: {}", entry.getKey(), e);
                    }
                }
            }
            
            // Set assets and total value
            krakenData.setAssets(assets);
            krakenData.setValue(totalValue);
            
            return krakenData;
        } catch (Exception e) {
            log.error("Error getting Kraken portfolio data: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets portfolio data from Binance US exchange
     * 
     * @param credentials Binance US API credentials
     * @return Structured exchange data
     */
    private PortfolioData.ExchangeData getBinanceUSPortfolioData(Map<String, String> credentials) {
        log.info("Getting Binance US portfolio data");
        
        try {
            // Extract API credentials
            String apiKey = credentials.get("apiKey");
            String privateKey = credentials.get("privateKey");
            
            if (apiKey == null || privateKey == null) {
                log.error("Missing Binance US API credentials");
                return null;
            }
            
            // Create exchange data object
            PortfolioData.ExchangeData binanceData = new PortfolioData.ExchangeData();
            binanceData.setId("binanceus");
            binanceData.setName("Binance US");
            binanceData.setValue(BigDecimal.ZERO);
            binanceData.setAssets(new HashMap<>());
            
            // Get account info from Binance US API
            String accountInfo = binanceUSClient.getAccountInfo(apiKey, privateKey);
            
            // Check if response is valid
            if (accountInfo == null || accountInfo.isEmpty()) {
                log.error("Failed to retrieve Binance US account data");
                return null;
            }
            
            // Parse JSON response
            JSONObject accountJson = new JSONObject(accountInfo);
            
            // Process and format the data
            BigDecimal totalValue = BigDecimal.ZERO;
            Map<String, PortfolioData.AssetData> assets = new HashMap<>();
            
            // Extract balances array
            if (accountJson.has("balances")) {
                for (int i = 0; i < accountJson.getJSONArray("balances").length(); i++) {
                    try {
                        JSONObject balance = accountJson.getJSONArray("balances").getJSONObject(i);
                        String asset = balance.getString("asset");
                        BigDecimal free = new BigDecimal(balance.getString("free"));
                        BigDecimal locked = new BigDecimal(balance.getString("locked"));
                        BigDecimal total = free.add(locked);
                        
                        // Skip assets with zero balance
                        if (total.compareTo(BigDecimal.ZERO) <= 0) {
                            continue;
                        }
                        
                        // Create asset data
                        PortfolioData.AssetData assetData = new PortfolioData.AssetData();
                        assetData.setSymbol(asset);
                        assetData.setName(getAssetFullName(asset));
                        assetData.setAmount(total);
                        assetData.setValue(BigDecimal.ZERO); // Would be calculated based on current price
                        
                        // Add asset to the collection
                        assets.put(asset, assetData);
                    } catch (Exception e) {
                        log.warn("Error processing Binance US asset: {}", e.getMessage(), e);
                    }
                }
            }
            
            // Set assets and total value
            binanceData.setAssets(assets);
            binanceData.setValue(totalValue);
            
            return binanceData;
        } catch (Exception e) {
            log.error("Error getting Binance US portfolio data: {}", e.getMessage(), e);
            return null;
        }
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
    
    /**
     * Get full asset name from symbol
     *
     * @param symbol Asset symbol
     * @return Full asset name
     */
    private String getAssetFullName(String symbol) {
        // Map common symbols to full names
        Map<String, String> assetNames = new HashMap<>();
        assetNames.put("BTC", "Bitcoin");
        assetNames.put("ETH", "Ethereum");
        assetNames.put("SOL", "Solana");
        assetNames.put("ADA", "Cardano");
        assetNames.put("DOT", "Polkadot");
        assetNames.put("DOGE", "Dogecoin");
        assetNames.put("XRP", "Ripple");
        assetNames.put("LTC", "Litecoin");
        
        // Return full name if available, otherwise return the symbol
        return assetNames.getOrDefault(symbol, symbol);
    }
    
    
    /**
     * Calculates portfolio statistics and metrics using Synapse patterns
     * 
     * @param portfolioData The structured portfolio data
     * @return Structured portfolio metrics
     */
    public PortfolioMetrics calculatePortfolioMetrics(PortfolioData portfolioData) {
        log.info("Calculating portfolio metrics for user: {}", portfolioData.getUserId());
        
        try {
            // Create portfolio metrics object
            PortfolioMetrics metrics = new PortfolioMetrics();
            metrics.setUserId(portfolioData.getUserId());
            metrics.setTotalValue(portfolioData.getTotalValue());
            
            // Set performance metrics
            Map<String, BigDecimal> performance = new HashMap<>();
            performance.put("daily", new BigDecimal("0.0"));
            performance.put("weekly", new BigDecimal("0.0"));
            performance.put("monthly", new BigDecimal("0.0"));
            performance.put("yearly", new BigDecimal("0.0"));
            metrics.setPerformance(performance);
            
            // Set allocation metrics
            Map<String, BigDecimal> allocation = new HashMap<>();
            metrics.setAllocation(allocation);
            
            // Set risk metrics
            Map<String, BigDecimal> risk = new HashMap<>();
            risk.put("volatility", new BigDecimal("0.0"));
            risk.put("sharpeRatio", new BigDecimal("0.0"));
            metrics.setRisk(risk);
            
            return metrics;
        } catch (Exception e) {
            log.error("Error calculating metrics for portfolio: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to calculate portfolio metrics", e);
        }
    }
    
    /**
     * Overloaded method to support the legacy Map<String, Object> interface
     * This is a temporary bridge method that will be removed once all services are migrated to Synapse
     * 
     * @param portfolioData Raw portfolio data as a Map
     * @return Portfolio metrics as a Map
     */
    public Map<String, Object> calculatePortfolioMetrics(Map<String, Object> portfolioData) {
        log.info("Using legacy interface for calculating portfolio metrics");
        
        // Convert the Map to a PortfolioData object
        PortfolioData data = new PortfolioData();
        data.setUserId((String) portfolioData.getOrDefault("userId", "unknown"));
        
        // For simplicity, just set the total value
        if (portfolioData.containsKey("totalValue")) {
            Object value = portfolioData.get("totalValue");
            if (value instanceof Number) {
                data.setTotalValue(new BigDecimal(value.toString()));
            } else {
                data.setTotalValue(BigDecimal.ZERO);
            }
        } else {
            data.setTotalValue(BigDecimal.ZERO);
        }
        
        // Calculate metrics using the structured object
        PortfolioMetrics metrics = calculatePortfolioMetrics(data);
        
        // Convert back to a Map for legacy support
        Map<String, Object> result = new HashMap<>();
        result.put("totalValue", metrics.getTotalValue());
        result.put("performance", metrics.getPerformance());
        result.put("allocation", metrics.getAllocation());
        result.put("risk", metrics.getRisk());
        
        return result;
    }
}
