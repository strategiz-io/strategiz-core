package io.strategiz.service.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.strategiz.client.coinbase.CoinbaseDataClient;
import io.strategiz.client.coinbase.CoinbaseClient;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.portfolio.exception.ServicePortfolioErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing Coinbase portfolio data
 */
@Service
public class CoinbasePortfolioService {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbasePortfolioService.class);
    
    private final CoinbaseDataClient coinbaseDataClient;
    private final ReadProviderIntegrationRepository providerRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public CoinbasePortfolioService(CoinbaseDataClient coinbaseDataClient,
                                   ReadProviderIntegrationRepository providerRepository) {
        this.coinbaseDataClient = coinbaseDataClient;
        this.providerRepository = providerRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Validate Coinbase connection
     */
    public Map<String, Object> validateConnection(String userId) {
        try {
            // Find Coinbase provider for user - try multiple provider types
            List<ProviderIntegrationEntity> providers = new ArrayList<>();
            
            // Try CRYPTO type first
            providers.addAll(providerRepository.findByUserIdAndProviderType(userId, "CRYPTO"));
            log.debug("Found {} CRYPTO providers for user {}", providers.size(), userId);
            
            // Also try EXCHANGE type
            if (providers.isEmpty()) {
                providers.addAll(providerRepository.findByUserIdAndProviderType(userId, "EXCHANGE"));
                log.debug("Found {} EXCHANGE providers for user {}", providers.size(), userId);
            }
            
            // Also try finding by provider ID directly
            if (providers.isEmpty()) {
                Optional<ProviderIntegrationEntity> directFind = providerRepository.findByUserIdAndProviderId(userId, "coinbase");
                directFind.ifPresent(providers::add);
                log.debug("Direct search for coinbase provider: {}", directFind.isPresent());
            }
            
            // Log all providers found
            for (ProviderIntegrationEntity provider : providers) {
                log.info("Found provider: name={}, id={}, type={}, status={}", 
                    provider.getProviderName(), provider.getProviderId(), 
                    provider.getProviderType(), provider.getStatus());
            }
            
            Optional<ProviderIntegrationEntity> coinbaseProvider = providers.stream()
                .filter(p -> "coinbase".equalsIgnoreCase(p.getProviderName()) || "coinbase".equalsIgnoreCase(p.getProviderId()))
                .findFirst();
            
            if (coinbaseProvider.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "No Coinbase integration found. Please connect your Coinbase account first."
                );
            }
            
            ProviderIntegrationEntity provider = coinbaseProvider.get();
            Map<String, Object> metadata = provider.getMetadata();
            
            if (metadata == null || !metadata.containsKey("access_token")) {
                log.error("Provider found but no access token in metadata. Provider: {}, Metadata: {}", 
                    provider.getProviderId(), metadata);
                return Map.of(
                    "success", false,
                    "message", "Access token not found. Please reconnect your Coinbase account."
                );
            }
            
            String accessToken = (String) metadata.get("access_token");
            log.info("Retrieved access token for validation, user: {} (token length: {})", userId, accessToken.length());
            
            // Validate token by making a test API call
            try {
                Map<String, Object> userInfo = getCurrentUser(accessToken);
                
                return Map.of(
                    "success", true,
                    "message", "Connection is valid",
                    "userInfo", userInfo,
                    "provider", Map.of(
                        "id", provider.getProviderId(),
                        "status", provider.getStatus(),
                        "lastSync", provider.getLastSyncAt(),
                        "capabilities", provider.getCapabilities()
                    )
                );
                
            } catch (Exception e) {
                log.error("Token validation failed", e);
                return Map.of(
                    "success", false,
                    "message", "Token is invalid or expired. Please reconnect your Coinbase account.",
                    "error", e.getMessage()
                );
            }
            
        } catch (Exception e) {
            log.error("Error validating Coinbase connection", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR, 
                "Failed to validate connection: " + e.getMessage());
        }
    }
    
    /**
     * Get current user info using OAuth token
     */
    private Map<String, Object> getCurrentUser(String accessToken) {
        try {
            return coinbaseDataClient.getCurrentUser(accessToken);
        } catch (Exception e) {
            log.error("Failed to fetch user info from Coinbase", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR,
                "Failed to fetch user info: " + e.getMessage());
        }
    }
    
    /**
     * Get complete portfolio data
     */
    public Map<String, Object> getPortfolioData(String userId) {
        try {
            // Find Coinbase provider for user - try multiple provider types
            List<ProviderIntegrationEntity> providers = new ArrayList<>();
            
            // Try CRYPTO type first
            providers.addAll(providerRepository.findByUserIdAndProviderType(userId, "CRYPTO"));
            log.debug("Found {} CRYPTO providers for user {}", providers.size(), userId);
            
            // Also try EXCHANGE type
            if (providers.isEmpty()) {
                providers.addAll(providerRepository.findByUserIdAndProviderType(userId, "EXCHANGE"));
                log.debug("Found {} EXCHANGE providers for user {}", providers.size(), userId);
            }
            
            // Also try finding by provider ID directly
            if (providers.isEmpty()) {
                Optional<ProviderIntegrationEntity> directFind = providerRepository.findByUserIdAndProviderId(userId, "coinbase");
                directFind.ifPresent(providers::add);
                log.debug("Direct search for coinbase provider: {}", directFind.isPresent());
            }
            
            // Log all providers found
            for (ProviderIntegrationEntity provider : providers) {
                log.info("Found provider: name={}, id={}, type={}, status={}", 
                    provider.getProviderName(), provider.getProviderId(), 
                    provider.getProviderType(), provider.getStatus());
            }
            
            Optional<ProviderIntegrationEntity> coinbaseProvider = providers.stream()
                .filter(p -> "coinbase".equalsIgnoreCase(p.getProviderName()) || "coinbase".equalsIgnoreCase(p.getProviderId()))
                .findFirst();
            
            if (coinbaseProvider.isEmpty()) {
                throw new StrategizException(ServicePortfolioErrorDetails.PROVIDER_NOT_FOUND, 
                    "Coinbase integration not found. Please connect your Coinbase account first.");
            }
            
            ProviderIntegrationEntity provider = coinbaseProvider.get();
            Map<String, Object> metadata = provider.getMetadata();
            
            if (metadata == null || !metadata.containsKey("access_token")) {
                log.error("Provider found but no access token in metadata. Provider: {}, Metadata: {}", 
                    provider.getProviderId(), metadata);
                throw new StrategizException(ServicePortfolioErrorDetails.INVALID_CREDENTIALS, 
                    "Access token not found. Please reconnect your Coinbase account.");
            }
            
            String accessToken = (String) metadata.get("access_token");
            log.info("Retrieved access token for user: {} (token length: {})", userId, accessToken.length());
            
            // Always fetch real accounts from Coinbase
            List<Map<String, Object>> accounts = getAllAccounts(accessToken);
            Map<String, Object> summary = calculatePortfolioSummary(accounts);
            
            // Update last sync time
            provider.setLastSyncAt(Instant.now());
            // Note: We should save this update through UpdateProviderRepository
            
            return Map.of(
                "success", true,
                "accounts", accounts,
                "summary", summary,
                "lastSync", Instant.now(),
                "provider", provider.getProviderName()
            );
            
        } catch (Exception e) {
            log.error("Error fetching portfolio data", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR, 
                "Failed to fetch portfolio data: " + e.getMessage());
        }
    }
    
    /**
     * Get all accounts using OAuth token
     */
    private List<Map<String, Object>> getAllAccounts(String accessToken) {
        try {
            return coinbaseDataClient.getAccounts(accessToken);
        } catch (Exception e) {
            log.error("Failed to fetch accounts from Coinbase", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR,
                "Failed to fetch accounts: " + e.getMessage());
        }
    }
    
    /**
     * Calculate portfolio summary
     */
    private Map<String, Object> calculatePortfolioSummary(List<Map<String, Object>> accounts) {
        BigDecimal totalValue = BigDecimal.ZERO;
        Map<String, BigDecimal> currencyBreakdown = new HashMap<>();
        
        for (Map<String, Object> account : accounts) {
            Map<String, Object> nativeBalance = (Map<String, Object>) account.get("native_balance");
            if (nativeBalance != null) {
                String amountStr = (String) nativeBalance.get("amount");
                BigDecimal amount = new BigDecimal(amountStr);
                totalValue = totalValue.add(amount);
                
                String currency = (String) account.get("currency");
                currencyBreakdown.put(currency, amount);
            }
        }
        
        return Map.of(
            "totalValue", totalValue,
            "totalValueFormatted", "$" + totalValue.setScale(2, RoundingMode.HALF_UP),
            "accountCount", accounts.size(),
            "currencyBreakdown", currencyBreakdown
        );
    }
    
    /**
     * Get holdings with profit/loss calculations
     */
    public Map<String, Object> getHoldingsWithProfits(String userId) {
        try {
            Map<String, Object> portfolioData = getPortfolioData(userId);
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) portfolioData.get("accounts");
            
            List<Map<String, Object>> holdings = new ArrayList<>();
            BigDecimal totalCost = new BigDecimal("45000"); // Sample cost basis
            BigDecimal totalValue = BigDecimal.ZERO;
            
            for (Map<String, Object> account : accounts) {
                Map<String, Object> holding = new HashMap<>(account);
                
                Map<String, Object> nativeBalance = (Map<String, Object>) account.get("native_balance");
                if (nativeBalance != null) {
                    BigDecimal currentValue = new BigDecimal((String) nativeBalance.get("amount"));
                    totalValue = totalValue.add(currentValue);
                    
                    // Calculate sample P&L (in production, fetch actual cost basis)
                    BigDecimal costBasis = currentValue.multiply(new BigDecimal("0.85")); // Sample: 15% profit
                    BigDecimal profitLoss = currentValue.subtract(costBasis);
                    BigDecimal profitLossPercent = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    
                    holding.put("currentValue", currentValue);
                    holding.put("costBasis", costBasis);
                    holding.put("profitLoss", profitLoss);
                    holding.put("profitLossPercent", profitLossPercent);
                    
                    holdings.add(holding);
                }
            }
            
            BigDecimal totalProfitLoss = totalValue.subtract(totalCost);
            BigDecimal totalProfitLossPercent = totalProfitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            
            return Map.of(
                "success", true,
                "holdings", holdings,
                "summary", Map.of(
                    "totalValue", totalValue,
                    "totalCost", totalCost,
                    "totalProfitLoss", totalProfitLoss,
                    "totalProfitLossPercent", totalProfitLossPercent
                ),
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            log.error("Error fetching holdings", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR, 
                "Failed to fetch holdings: " + e.getMessage());
        }
    }
    
    /**
     * Get recent transactions
     */
    public Map<String, Object> getRecentTransactions(String userId, int limit) {
        try {
            // TODO: Implement actual Coinbase API call to fetch transactions
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR,
                "Transaction fetching not yet implemented");
            
        } catch (Exception e) {
            log.error("Error fetching transactions", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR, 
                "Failed to fetch transactions: " + e.getMessage());
        }
    }
    
    /**
     * Get current prices
     */
    public Map<String, Object> getCurrentPrices(String symbols) {
        try {
            // Sample price data for testing
            Map<String, BigDecimal> prices = new HashMap<>();
            prices.put("BTC", new BigDecimal("96000.00"));
            prices.put("ETH", new BigDecimal("3640.00"));
            prices.put("SOL", new BigDecimal("185.00"));
            
            return Map.of(
                "success", true,
                "prices", prices,
                "timestamp", Instant.now()
            );
            
        } catch (Exception e) {
            log.error("Error fetching prices", e);
            throw new StrategizException(ServicePortfolioErrorDetails.PORTFOLIO_DATA_FETCH_ERROR, 
                "Failed to fetch prices: " + e.getMessage());
        }
    }
    
    /**
     * Get mock portfolio data for demo purposes
     */
    private Map<String, Object> getMockPortfolioData() {
        List<Map<String, Object>> accounts = new ArrayList<>();
        
        // Mock Bitcoin holding
        accounts.add(Map.of(
            "id", "btc-wallet-demo",
            "name", "Bitcoin Wallet",
            "currency", "BTC",
            "balance", "1.2534",
            "native_balance", Map.of(
                "amount", "120325.44",
                "currency", "USD"
            ),
            "type", "wallet"
        ));
        
        // Mock Ethereum holding
        accounts.add(Map.of(
            "id", "eth-wallet-demo",
            "name", "Ethereum Wallet",
            "currency", "ETH",
            "balance", "15.7892",
            "native_balance", Map.of(
                "amount", "57472.13",
                "currency", "USD"
            ),
            "type", "wallet"
        ));
        
        // Mock Solana holding
        accounts.add(Map.of(
            "id", "sol-wallet-demo",
            "name", "Solana Wallet",
            "currency", "SOL",
            "balance", "234.567",
            "native_balance", Map.of(
                "amount", "43394.90",
                "currency", "USD"
            ),
            "type", "wallet"
        ));
        
        // Mock USDC holding
        accounts.add(Map.of(
            "id", "usdc-wallet-demo",
            "name", "USDC Wallet",
            "currency", "USDC",
            "balance", "5000.00",
            "native_balance", Map.of(
                "amount", "5000.00",
                "currency", "USD"
            ),
            "type", "fiat"
        ));
        
        Map<String, Object> summary = calculatePortfolioSummary(accounts);
        
        return Map.of(
            "success", true,
            "accounts", accounts,
            "summary", summary,
            "lastSync", Instant.now(),
            "provider", "coinbase-demo"
        );
    }
}