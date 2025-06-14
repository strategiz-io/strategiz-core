package io.strategiz.service.dashboard;

import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.client.alphavantage.model.StockData;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.service.dashboard.constants.DashboardConstants;
import io.strategiz.service.dashboard.model.dashboard.AssetMarketData;
import io.strategiz.service.dashboard.model.watchlist.WatchlistAsset;
import io.strategiz.service.dashboard.model.watchlist.WatchlistResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for dashboard operations.
 * This service coordinates data retrieval and processing for the dashboard.
 * Implements Synapse BaseService pattern.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final PortfolioManager portfolioManager;
    private final CoinGeckoClient coinGeckoClient;
    private final AlphaVantageClient alphaVantageClient;

    @Autowired
    public DashboardService(PortfolioManager portfolioManager, 
                          CoinGeckoClient coinGeckoClient,
                          AlphaVantageClient alphaVantageClient) {
        this.portfolioManager = portfolioManager;
        this.coinGeckoClient = coinGeckoClient;
        this.alphaVantageClient = alphaVantageClient;
    }

    /**
     * Gets watchlist data for the authenticated user
     * 
     * @param userId The user ID to fetch watchlist data for
     * @return Watchlist response
     */
    public WatchlistResponse getWatchlist(String userId) {
        log.info("Getting watchlist data for user: {}", userId);
        
        try {
            // Create response
            WatchlistResponse response = new WatchlistResponse();
            
            // Set available categories
            response.setAvailableCategories(Arrays.asList("All", "Crypto", "Stocks"));
            
            // Get user's watchlist preferences from user service or database
            List<WatchlistAsset> userWatchlist = getUserWatchlist(userId);
            
            // If user has no watchlist, provide default assets
            if (userWatchlist.isEmpty()) {
                userWatchlist = getDefaultWatchlist();
            }
            
            // Process and transform watchlist data
            List<WatchlistResponse.WatchlistItem> watchlistItems = new ArrayList<>();
            
            // Group assets by type for batch processing
            List<WatchlistAsset> cryptoAssets = userWatchlist.stream()
                .filter(asset -> "Crypto".equalsIgnoreCase(asset.getCategory()))
                .collect(Collectors.toList());
            
            List<WatchlistAsset> stockAssets = userWatchlist.stream()
                .filter(asset -> "Stocks".equalsIgnoreCase(asset.getCategory()))
                .collect(Collectors.toList());
            
            // Fetch crypto market data in batch
            if (!cryptoAssets.isEmpty()) {
                Map<String, AssetMarketData> cryptoMarketData = fetchCryptoMarketData(cryptoAssets);
                
                for (WatchlistAsset asset : cryptoAssets) {
                    AssetMarketData marketData = cryptoMarketData.getOrDefault(asset.getId(), 
                            new AssetMarketData(asset.getId(), asset.getSymbol(), asset.getName(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    
                    WatchlistResponse.WatchlistItem item = new WatchlistResponse.WatchlistItem();
                    item.setId(asset.getId());
                    item.setSymbol(asset.getSymbol());
                    item.setName(asset.getName());
                    item.setCategory(asset.getCategory());
                    item.setPrice(marketData.getPrice());
                    item.setChange(marketData.getChange());
                    item.setChangePercent(marketData.getChangePercent());
                    item.setPositiveChange(marketData.isPositiveChange());
                    watchlistItems.add(item);
                }
            }
            
            // Fetch stock market data
            if (!stockAssets.isEmpty()) {
                Map<String, AssetMarketData> stockMarketData = fetchStockMarketData(stockAssets);
                
                for (WatchlistAsset asset : stockAssets) {
                    AssetMarketData marketData = stockMarketData.getOrDefault(asset.getId(), 
                            new AssetMarketData(asset.getId(), asset.getSymbol(), asset.getName(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    
                    WatchlistResponse.WatchlistItem item = new WatchlistResponse.WatchlistItem();
                    item.setId(asset.getId());
                    item.setSymbol(asset.getSymbol());
                    item.setName(asset.getName());
                    item.setCategory(asset.getCategory());
                    item.setPrice(marketData.getPrice());
                    item.setChange(marketData.getChange());
                    item.setChangePercent(marketData.getChangePercent());
                    item.setPositiveChange(marketData.isPositiveChange());
                    watchlistItems.add(item);
                }
            }
            
            response.setWatchlistItems(watchlistItems);
            
            return response;
        } catch (Exception e) {
            log.error("Error getting watchlist for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve watchlist", e);
        }
    }
    
    // ... rest of the code ...

    /**
     * Fetch crypto market data from CoinGecko API
     * 
     * @param cryptoAssets List of crypto assets
     * @return Map of asset ID to market data
     */
    private Map<String, AssetMarketData> fetchCryptoMarketData(List<WatchlistAsset> cryptoAssets) {
        log.info("Fetching crypto market data for {} assets", cryptoAssets.size());
        Map<String, AssetMarketData> marketData = new HashMap<>();
        
        try {
            // Extract crypto IDs for the API call
            List<String> cryptoIds = cryptoAssets.stream()
                .map(WatchlistAsset::getId)
                .collect(Collectors.toList());
            
            if (cryptoIds.isEmpty()) {
                return marketData;
            }
            
            // Call CoinGecko API to get real-time market data
            List<CryptoCurrency> cryptoCurrencies = coinGeckoClient.getCryptocurrencyMarketData(cryptoIds, "usd");
            
            // Convert API response to our internal model
            for (CryptoCurrency crypto : cryptoCurrencies) {
                BigDecimal price = crypto.getCurrentPrice() != null ? crypto.getCurrentPrice() : BigDecimal.ZERO;
                BigDecimal priceChange = crypto.getPriceChange24h() != null ? crypto.getPriceChange24h() : BigDecimal.ZERO;
                BigDecimal priceChangePercent = crypto.getPriceChangePercentage24h() != null ? 
                    crypto.getPriceChangePercentage24h() : BigDecimal.ZERO;
                boolean isPositive = priceChange.compareTo(BigDecimal.ZERO) >= 0;
                
                marketData.put(crypto.getId(), new AssetMarketData(
                    crypto.getId(),
                    crypto.getSymbol().toUpperCase(),
                    crypto.getName(),
                    price,
                    priceChange,
                    priceChangePercent,
                    isPositive
                ));
            }
            
            log.info("Successfully fetched market data for {} cryptocurrencies", cryptoCurrencies.size());
        } catch (Exception e) {
            log.error("Error fetching crypto market data: {}", e.getMessage(), e);
            // Provide fallback data for critical assets if API fails
            provideFallbackCryptoData(cryptoAssets, marketData);
        }
        
        return marketData;
    }
    
    /**
     * Fetch stock market data from AlphaVantage API
     * 
     * @param stockAssets List of stock assets
     * @return Map of asset ID to market data
     */
    private Map<String, AssetMarketData> fetchStockMarketData(List<WatchlistAsset> stockAssets) {
        log.info("Fetching stock market data for {} assets", stockAssets.size());
        Map<String, AssetMarketData> marketData = new HashMap<>();
        
        try {
            // Extract stock symbols for the API call
            List<String> symbols = stockAssets.stream()
                .map(WatchlistAsset::getId)
                .collect(Collectors.toList());
            
            if (symbols.isEmpty()) {
                return marketData;
            }
            
            // Call AlphaVantage API to get real-time stock data
            Map<String, StockData> stockDataMap = alphaVantageClient.getBatchStockQuotes(symbols);
            
            // Convert API response to our internal model
            for (Map.Entry<String, StockData> entry : stockDataMap.entrySet()) {
                StockData stock = entry.getValue();
                
                BigDecimal price = stock.getPrice() != null ? stock.getPrice() : BigDecimal.ZERO;
                BigDecimal change = stock.getChange() != null ? stock.getChange() : BigDecimal.ZERO;
                BigDecimal changePercent = stock.getChangePercent() != null ? 
                    stock.getChangePercent() : BigDecimal.ZERO;
                boolean isPositive = change.compareTo(BigDecimal.ZERO) >= 0;
                
                marketData.put(entry.getKey(), new AssetMarketData(
                    entry.getKey(),
                    stock.getSymbol(),
                    stock.getName(),
                    price,
                    change,
                    changePercent,
                    isPositive
                ));
            }
            
            log.info("Successfully fetched market data for {} stocks", stockDataMap.size());
        } catch (Exception e) {
            log.error("Error fetching stock market data: {}", e.getMessage(), e);
            // Provide fallback data for critical stocks if API fails
            provideFallbackStockData(stockAssets, marketData);
        }
        
        return marketData;
    }
    
    /**
     * Get user's watchlist preferences from user service or database
     * 
     * @param userId User ID
     * @return List of watchlist assets
     */
    private List<WatchlistAsset> getUserWatchlist(String userId) {
        log.info(DashboardConstants.SUCCESS_WATCHLIST_FETCH);
        // TODO: Implement real data fetch from database or user service
        // This would connect to the same Firebase collection that the UI uses
        // For now returning an empty list to use defaults
        return new ArrayList<>();
    }
    
    /**
     * Get default watchlist assets
     * 
     * @return List of default watchlist assets
     */
    private List<WatchlistAsset> getDefaultWatchlist() {
        List<WatchlistAsset> defaultAssets = new ArrayList<>();
        
        // Add default crypto assets
        defaultAssets.add(new WatchlistAsset(DashboardConstants.DEFAULT_CRYPTO_BTC, "BTC", "Bitcoin", "Crypto"));
        defaultAssets.add(new WatchlistAsset(DashboardConstants.DEFAULT_CRYPTO_ETH, "ETH", "Ethereum", "Crypto"));
        
        // Add default stock assets
        defaultAssets.add(new WatchlistAsset(DashboardConstants.DEFAULT_STOCK_MSFT, "MSFT", "Microsoft", "Stocks"));
        
        return defaultAssets;
    }
    
    /**
     * Provide fallback data for critical crypto assets if API call fails
     * 
     * @param cryptoAssets List of crypto assets
     * @param marketData Map to populate with fallback data
     */
    private void provideFallbackCryptoData(List<WatchlistAsset> cryptoAssets, Map<String, AssetMarketData> marketData) {
        log.warn("Using fallback crypto market data due to API failure");
        
        if (cryptoAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_CRYPTO_BTC.equals(asset.getId()))) {
            marketData.put("bitcoin", new AssetMarketData(
                "bitcoin", "BTC", "Bitcoin", 
                new BigDecimal("109673.00"), 
                new BigDecimal("1962.70"), 
                new BigDecimal("1.79"), 
                true
            ));
        }
        
        if (cryptoAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_CRYPTO_ETH.equals(asset.getId()))) {
            marketData.put("ethereum", new AssetMarketData(
                "ethereum", "ETH", "Ethereum", 
                new BigDecimal("2561.84"), 
                new BigDecimal("65.01"), 
                new BigDecimal("2.54"), 
                true
            ));
        }
    }
    
    /**
     * Provide fallback data for critical stocks if API call fails
     * 
     * @param stockAssets List of stock assets
     * @param marketData Map to populate with fallback data
     */
    private void provideFallbackStockData(List<WatchlistAsset> stockAssets, Map<String, AssetMarketData> marketData) {
        log.warn("Using fallback stock market data due to API failure");
        
        if (stockAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_STOCK_MSFT.equals(asset.getId()))) {
            marketData.put("MSFT", new AssetMarketData(
                "MSFT", "MSFT", "Microsoft", 
                new BigDecimal("450.18"), 
                new BigDecimal("-4.68"), 
                new BigDecimal("-1.03"), 
                false
            ));
        }
        
        if (stockAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_STOCK_AAPL.equals(asset.getId()))) {
            marketData.put("AAPL", new AssetMarketData(
                "AAPL", "AAPL", "Apple Inc.", 
                new BigDecimal("213.07"), 
                new BigDecimal("1.52"), 
                new BigDecimal("0.72"), 
                true
            ));
        }
    }
}
