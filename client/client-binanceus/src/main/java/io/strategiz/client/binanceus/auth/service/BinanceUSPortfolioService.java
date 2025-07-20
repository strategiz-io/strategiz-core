package io.strategiz.client.binanceus.auth.service;

import io.strategiz.client.binanceus.auth.BinanceUSApiAuthClient;
import io.strategiz.client.binanceus.auth.model.BinanceUSApiCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching portfolio data from Binance US
 */
@Service
public class BinanceUSPortfolioService {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSPortfolioService.class);
    
    private final BinanceUSApiAuthClient apiAuthClient;
    private final BinanceUSCredentialService credentialService;
    
    public BinanceUSPortfolioService(
            BinanceUSApiAuthClient apiAuthClient,
            BinanceUSCredentialService credentialService) {
        this.apiAuthClient = apiAuthClient;
        this.credentialService = credentialService;
    }
    
    /**
     * Get portfolio data for a user
     * 
     * @param userId User ID
     * @return Portfolio data including balances
     */
    public Map<String, Object> getPortfolio(String userId) {
        try {
            // Get credentials from Vault
            BinanceUSApiCredentials credentials = credentialService.getCredentials(userId);
            if (credentials == null) {
                log.warn("No Binance US credentials found for user: {}", userId);
                return null;
            }
            
            // Fetch account data
            Map<String, Object> accountData = apiAuthClient.getAccountInfo(
                credentials.getApiKey(), 
                credentials.getApiSecret()
            ).block();
            
            if (accountData == null) {
                log.warn("No account data returned for user: {}", userId);
                return null;
            }
            
            // Process and return portfolio data
            return processPortfolioData(accountData);
            
        } catch (Exception e) {
            log.error("Error fetching portfolio for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get account balances for a user
     * 
     * @param userId User ID
     * @return Account balances
     */
    public List<Map<String, Object>> getBalances(String userId) {
        try {
            // Get credentials from Vault
            BinanceUSApiCredentials credentials = credentialService.getCredentials(userId);
            if (credentials == null) {
                log.warn("No Binance US credentials found for user: {}", userId);
                return null;
            }
            
            // Fetch balances
            Map<String, Object> balanceData = apiAuthClient.getAccountBalances(
                credentials.getApiKey(), 
                credentials.getApiSecret()
            ).block();
            
            if (balanceData == null || !balanceData.containsKey("balances")) {
                log.warn("No balance data returned for user: {}", userId);
                return null;
            }
            
            // Process balances
            return processBalances((List<Map<String, Object>>) balanceData.get("balances"));
            
        } catch (Exception e) {
            log.error("Error fetching balances for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get open orders for a user
     * 
     * @param userId User ID
     * @param symbol Optional symbol filter
     * @return Open orders
     */
    public List<Map<String, Object>> getOpenOrders(String userId, String symbol) {
        try {
            // Get credentials from Vault
            BinanceUSApiCredentials credentials = credentialService.getCredentials(userId);
            if (credentials == null) {
                log.warn("No Binance US credentials found for user: {}", userId);
                return null;
            }
            
            // Fetch open orders
            Map<String, Object> orderData = apiAuthClient.getOpenOrders(
                credentials.getApiKey(), 
                credentials.getApiSecret(),
                symbol
            ).block();
            
            if (orderData == null) {
                log.warn("No order data returned for user: {}", userId);
                return new ArrayList<>();
            }
            
            // If response is a list, return it; otherwise wrap in list
            if (orderData instanceof List) {
                return (List<Map<String, Object>>) orderData;
            }
            
            return List.of(orderData);
            
        } catch (Exception e) {
            log.error("Error fetching open orders for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Get trade history for a user
     * 
     * @param userId User ID
     * @param symbol Trading symbol
     * @param limit Number of trades to return
     * @return Trade history
     */
    public List<Map<String, Object>> getTradeHistory(String userId, String symbol, Integer limit) {
        try {
            // Get credentials from Vault
            BinanceUSApiCredentials credentials = credentialService.getCredentials(userId);
            if (credentials == null) {
                log.warn("No Binance US credentials found for user: {}", userId);
                return null;
            }
            
            // Fetch trade history
            Map<String, Object> tradeData = apiAuthClient.getTradeHistory(
                credentials.getApiKey(), 
                credentials.getApiSecret(),
                symbol,
                limit
            ).block();
            
            if (tradeData == null) {
                log.warn("No trade data returned for user: {}", userId);
                return new ArrayList<>();
            }
            
            // If response is a list, return it; otherwise wrap in list
            if (tradeData instanceof List) {
                return (List<Map<String, Object>>) tradeData;
            }
            
            return List.of(tradeData);
            
        } catch (Exception e) {
            log.error("Error fetching trade history for user: {}", userId, e);
            return null;
        }
    }
    
    /**
     * Process raw portfolio data from Binance US API
     * 
     * @param accountData Raw account data
     * @return Processed portfolio data
     */
    private Map<String, Object> processPortfolioData(Map<String, Object> accountData) {
        Map<String, Object> portfolio = new HashMap<>();
        
        // Extract account type and permissions
        portfolio.put("accountType", accountData.get("accountType"));
        portfolio.put("canTrade", accountData.get("canTrade"));
        portfolio.put("canWithdraw", accountData.get("canWithdraw"));
        portfolio.put("canDeposit", accountData.get("canDeposit"));
        
        // Process balances
        if (accountData.containsKey("balances")) {
            List<Map<String, Object>> balances = processBalances(
                (List<Map<String, Object>>) accountData.get("balances")
            );
            portfolio.put("balances", balances);
            
            // Calculate total value (in USD if possible)
            BigDecimal totalUsdValue = calculateTotalValue(balances);
            portfolio.put("totalUsdValue", totalUsdValue);
        }
        
        // Add update time
        portfolio.put("updateTime", accountData.get("updateTime"));
        
        return portfolio;
    }
    
    /**
     * Process raw balance data
     * 
     * @param rawBalances Raw balance list from API
     * @return Processed balance list (only non-zero balances)
     */
    private List<Map<String, Object>> processBalances(List<Map<String, Object>> rawBalances) {
        List<Map<String, Object>> processedBalances = new ArrayList<>();
        
        for (Map<String, Object> balance : rawBalances) {
            String freeStr = (String) balance.get("free");
            String lockedStr = (String) balance.get("locked");
            
            BigDecimal free = new BigDecimal(freeStr);
            BigDecimal locked = new BigDecimal(lockedStr);
            BigDecimal total = free.add(locked);
            
            // Only include non-zero balances
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> processedBalance = new HashMap<>();
                processedBalance.put("asset", balance.get("asset"));
                processedBalance.put("free", free);
                processedBalance.put("locked", locked);
                processedBalance.put("total", total);
                
                processedBalances.add(processedBalance);
            }
        }
        
        return processedBalances;
    }
    
    /**
     * Calculate total portfolio value in USD
     * Note: This is a simplified calculation - in production you'd need
     * current prices from market data
     * 
     * @param balances List of balances
     * @return Total value (simplified)
     */
    private BigDecimal calculateTotalValue(List<Map<String, Object>> balances) {
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Map<String, Object> balance : balances) {
            String asset = (String) balance.get("asset");
            BigDecimal total = (BigDecimal) balance.get("total");
            
            // For USD-based assets, add directly
            if ("USD".equals(asset) || "USDT".equals(asset) || "USDC".equals(asset)) {
                totalValue = totalValue.add(total);
            }
            // For other assets, you'd need to fetch current prices
            // This is simplified for now
        }
        
        return totalValue;
    }
}