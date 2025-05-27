package io.strategiz.service.dashboard;

import io.americanexpress.synapse.service.rest.service.BaseService;
import io.strategiz.api.dashboard.model.PortfolioSummaryResponse;
import io.strategiz.api.dashboard.model.WatchlistResponse;
import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.client.alphavantage.model.StockData;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.service.dashboard.model.DashboardData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.strategiz.service.dashboard.constant.DashboardConstants;

/**
 * Service for dashboard operations.
 * This service coordinates data retrieval and processing for the dashboard.
 * Implements Synapse BaseService pattern.
 */
@Slf4j
@Service
public class DashboardService extends BaseService {

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
     * Gets all data needed for the dashboard using Synapse patterns
     * 
     * @param userId The user ID to fetch dashboard data for
     * @return Dashboard data as a structured object
     */
    public DashboardData getDashboardData(String userId) {
        log.info("Getting dashboard data for user: {}", userId);
        
        try {
            // Create the dashboard data object
            DashboardData dashboardData = new DashboardData();
            dashboardData.setUserId(userId);
            
            // Use the business-portfolio module to get portfolio data
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            
            // Convert data to structured objects
            dashboardData.setPortfolio(convertToPortfolioSummary(portfolioData));
            dashboardData.setMetrics(portfolioManager.calculatePortfolioMetrics(portfolioData));
            dashboardData.setMarket(getMarketData());
            dashboardData.setWatchlist(getWatchlistData(userId));
            
            return dashboardData;
        } catch (Exception e) {
            log.error("Error getting dashboard data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve dashboard data", e);
        }
    }
    
    /**
     * Gets portfolio summary for the authenticated user
     * 
     * @param userId The user ID to fetch portfolio data for
     * @return Portfolio summary response
     */
    public PortfolioSummaryResponse getPortfolioSummary(String userId) {
        log.info("Getting portfolio summary for user: {}", userId);
        
        try {
            // Get portfolio data from the business layer
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            
            // Convert to API response model
            PortfolioSummaryResponse response = new PortfolioSummaryResponse();
            response.setTotalValue(portfolioData.getTotalValue() != null ? 
                    portfolioData.getTotalValue() : BigDecimal.ZERO);
            response.setDailyChange(portfolioData.getDailyChange() != null ? 
                    portfolioData.getDailyChange() : BigDecimal.ZERO);
            response.setDailyChangePercent(portfolioData.getDailyChangePercent() != null ? 
                    portfolioData.getDailyChangePercent() : BigDecimal.ZERO);
            
            // Check if there are any exchange connections
            boolean hasExchanges = portfolioData.getExchanges() != null && !portfolioData.getExchanges().isEmpty();
            response.setHasExchangeConnections(hasExchanges);
            
            // Set the message shown in the UI when no exchanges are found
            if (!hasExchanges) {
                response.setStatusMessage("No exchange connections found. Please configure your API keys in the settings.");
                response.setNeedsApiKeyConfiguration(true);
            }
            
            // Convert exchanges
            Map<String, PortfolioSummaryResponse.ExchangeData> exchanges = new HashMap<>();
            if (hasExchanges) {
                portfolioData.getExchanges().forEach((key, exchangeData) -> {
                    PortfolioSummaryResponse.ExchangeData exchange = new PortfolioSummaryResponse.ExchangeData();
                    exchange.setName(exchangeData.getName());
                    exchange.setValue(exchangeData.getValue());
                    
                    // Convert assets
                    Map<String, PortfolioSummaryResponse.AssetData> assets = new HashMap<>();
                    if (exchangeData.getAssets() != null) {
                        exchangeData.getAssets().forEach((assetKey, assetData) -> {
                            PortfolioSummaryResponse.AssetData asset = new PortfolioSummaryResponse.AssetData();
                            asset.setSymbol(assetData.getSymbol());
                            asset.setName(assetData.getName());
                            asset.setQuantity(assetData.getQuantity());
                            asset.setPrice(assetData.getPrice());
                            asset.setValue(assetData.getValue());
                            asset.setAllocationPercent(assetData.getAllocationPercent());
                            assets.put(assetKey, asset);
                        });
                    }
                    exchange.setAssets(assets);
                    exchanges.put(key, exchange);
                });
            }
            response.setExchanges(exchanges);
            
            return response;
        } catch (Exception e) {
            log.error("Error getting portfolio summary for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve portfolio summary", e);
        }
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
            
            // Fetch crypto market data
            if (!cryptoAssets.isEmpty()) {
                Map<String, MarketData> cryptoMarketData = fetchCryptoMarketData(cryptoAssets);
                
                for (WatchlistAsset asset : cryptoAssets) {
                    MarketData marketData = cryptoMarketData.getOrDefault(asset.getId(), 
                            new MarketData(asset.getId(), asset.getSymbol(), asset.getName(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    
                    WatchlistResponse.WatchlistItem item = new WatchlistResponse.WatchlistItem();
                    item.setId(asset.getId());
                    item.setSymbol(asset.getSymbol());
                    item.setName(asset.getName());
                    item.setCategory("Crypto");
                    item.setPrice(marketData.getPrice());
                    item.setChange(marketData.getChange());
                    item.setChangePercent(marketData.getChangePercent());
                    item.setPositiveChange(marketData.isPositiveChange());
                    item.setChartDataUrl("/api/chart/" + asset.getSymbol());
                    
                    watchlistItems.add(item);
                }
            }
            
            // Fetch stock market data
            if (!stockAssets.isEmpty()) {
                Map<String, MarketData> stockMarketData = fetchStockMarketData(stockAssets);
                
                for (WatchlistAsset asset : stockAssets) {
                    MarketData marketData = stockMarketData.getOrDefault(asset.getId(), 
                            new MarketData(asset.getId(), asset.getSymbol(), asset.getName(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
                    
                    WatchlistResponse.WatchlistItem item = new WatchlistResponse.WatchlistItem();
                    item.setId(asset.getId());
                    item.setSymbol(asset.getSymbol());
                    item.setName(asset.getName());
                    item.setCategory("Stocks");
                    item.setPrice(marketData.getPrice());
                    item.setChange(marketData.getChange());
                    item.setChangePercent(marketData.getChangePercent());
                    item.setPositiveChange(marketData.isPositiveChange());
                    item.setChartDataUrl("/api/chart/" + asset.getSymbol());
                    
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
     * Fetch crypto market data from CoinGecko API
     * 
     * @param cryptoAssets List of crypto assets
     * @return Map of asset ID to market data
     */
    private Map<String, MarketData> fetchCryptoMarketData(List<WatchlistAsset> cryptoAssets) {
        log.info("Fetching crypto market data for {} assets", cryptoAssets.size());
        Map<String, MarketData> marketData = new HashMap<>();
        
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
                
                marketData.put(crypto.getId(), new MarketData(
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
     * Provide fallback data for critical crypto assets if API call fails
     * 
     * @param cryptoAssets List of crypto assets
     * @param marketData Map to populate with fallback data
     */
    private void provideFallbackCryptoData(List<WatchlistAsset> cryptoAssets, Map<String, MarketData> marketData) {
        log.warn("Using fallback crypto market data due to API failure");
        
        if (cryptoAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_CRYPTO_BTC.equals(asset.getId()))) {
            marketData.put("bitcoin", new MarketData(
                "bitcoin", "BTC", "Bitcoin", 
                new BigDecimal("109673.00"), 
                new BigDecimal("1962.70"), 
                new BigDecimal("1.79"), 
                true
            ));
        }
        
        if (cryptoAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_CRYPTO_ETH.equals(asset.getId()))) {
            marketData.put("ethereum", new MarketData(
                "ethereum", "ETH", "Ethereum", 
                new BigDecimal("2561.84"), 
                new BigDecimal("65.01"), 
                new BigDecimal("2.54"), 
                true
            ));
        }
    }
    
    /**
     * Fetch stock market data from AlphaVantage API
     * 
     * @param stockAssets List of stock assets
     * @return Map of asset ID to market data
     */
    private Map<String, MarketData> fetchStockMarketData(List<WatchlistAsset> stockAssets) {
        log.info("Fetching stock market data for {} assets", stockAssets.size());
        Map<String, MarketData> marketData = new HashMap<>();
        
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
                
                marketData.put(entry.getKey(), new MarketData(
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
     * Provide fallback data for critical stocks if API call fails
     * 
     * @param stockAssets List of stock assets
     * @param marketData Map to populate with fallback data
     */
    private void provideFallbackStockData(List<WatchlistAsset> stockAssets, Map<String, MarketData> marketData) {
        log.warn("Using fallback stock market data due to API failure");
        
        if (stockAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_STOCK_MSFT.equals(asset.getId()))) {
            marketData.put("MSFT", new MarketData(
                "MSFT", "MSFT", "Microsoft", 
                new BigDecimal("450.18"), 
                new BigDecimal("-4.68"), 
                new BigDecimal("-1.03"), 
                false
            ));
        }
        
        if (stockAssets.stream().anyMatch(asset -> DashboardConstants.DEFAULT_STOCK_AAPL.equals(asset.getId()))) {
            marketData.put("AAPL", new MarketData(
                "AAPL", "AAPL", "Apple Inc.", 
                new BigDecimal("213.07"), 
                new BigDecimal("1.52"), 
                new BigDecimal("0.72"), 
                true
            ));
        }
    }
    
    /**
     * Asset data class for watchlist
     */
    @Data
    private static class WatchlistAsset {
        private final String id;
        private final String symbol;
        private final String name;
        private final String category;
        
        public WatchlistAsset(String id, String symbol, String name, String category) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.category = category;
        }
    }
    
    /**
     * Market data class for assets
     */
    @Data
    private static class MarketData {
        private final String id;
        private final String symbol;
        private final String name;
        private final BigDecimal price;
        private final BigDecimal change;
        private final BigDecimal changePercent;
        private final boolean positiveChange;
        
        public MarketData(String id, String symbol, String name, BigDecimal price, 
                          BigDecimal change, BigDecimal changePercent, boolean positiveChange) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
            this.positiveChange = positiveChange;
        }
    }
    
    /**
     * Converts portfolio data to a structured PortfolioSummary object
     * 
     * @param portfolioData Portfolio data from PortfolioManager
     * @return Structured PortfolioSummary object
     */
    private DashboardData.PortfolioSummary convertToPortfolioSummary(PortfolioData portfolioData) {
        DashboardData.PortfolioSummary portfolioSummary = new DashboardData.PortfolioSummary();
        
        // Set values from the portfolio data
        portfolioSummary.setTotalValue(portfolioData.getTotalValue());
        portfolioSummary.setDailyChange(portfolioData.getDailyChange());
        portfolioSummary.setDailyChangePercent(portfolioData.getDailyChangePercent());
        
        // Convert exchanges
        Map<String, DashboardData.ExchangeData> exchanges = new HashMap<>();
        if (portfolioData.getExchanges() != null) {
            portfolioData.getExchanges().forEach((key, exchangeData) -> {
                DashboardData.ExchangeData exchange = new DashboardData.ExchangeData();
                exchange.setName(exchangeData.getName());
                exchange.setValue(exchangeData.getValue());
                
                // Convert assets
                Map<String, DashboardData.AssetData> assets = new HashMap<>();
                if (exchangeData.getAssets() != null) {
                    exchangeData.getAssets().forEach((assetKey, assetData) -> {
                        DashboardData.AssetData asset = new DashboardData.AssetData();
                        asset.setSymbol(assetData.getSymbol());
                        asset.setName(assetData.getName());
                        asset.setQuantity(assetData.getQuantity());
                        asset.setPrice(assetData.getPrice());
                        asset.setValue(assetData.getValue());
                        asset.setAllocationPercent(assetData.getAllocationPercent());
                        assets.put(assetKey, asset);
                    });
                }
                exchange.setAssets(assets);
                exchanges.put(key, exchange);
            });
        }
        portfolioSummary.setExchanges(exchanges);
        
        return portfolioSummary;
    }
    
    /**
     * Gets market data for the dashboard following Synapse patterns
     * 
     * @return Market data as a structured object
     */
    private DashboardData.MarketData getMarketData() {
        log.debug("Retrieving market data");
        
        try {
            // Create market data object
            DashboardData.MarketData marketData = new DashboardData.MarketData();
            
            // Add market indexes
            Map<String, BigDecimal> indexes = new HashMap<>();
            indexes.put("S&P500", new BigDecimal("4500.0"));
            indexes.put("NASDAQ", new BigDecimal("14000.0"));
            indexes.put("DOW", new BigDecimal("36000.0"));
            marketData.setIndexes(indexes);
            
            // Add market trends
            Map<String, BigDecimal> trends = new HashMap<>();
            marketData.setTrends(trends);
            
            return marketData;
        } catch (Exception e) {
            log.error("Error retrieving market data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve market data", e);
        }
    }
    
    /**
     * Gets watchlist data for the dashboard following Synapse patterns
     * 
     * @param userId The user ID to fetch watchlist data for
     * @return Watchlist data as a list of structured objects
     */
    private List<DashboardData.WatchlistItem> getWatchlistData(String userId) {
        log.debug("Retrieving watchlist data for user: {}", userId);
        
        try {
            // Create example watchlist items
            List<DashboardData.WatchlistItem> watchlist = new ArrayList<>();
            
            // Example item 1
            DashboardData.WatchlistItem item1 = new DashboardData.WatchlistItem();
            item1.setId("BTC");
            item1.setSymbol("BTC-USD");
            item1.setName("Bitcoin");
            item1.setType("CRYPTO");
            item1.setPrice(new BigDecimal("50000.00"));
            item1.setChange(new BigDecimal("1500.00"));
            item1.setChangePercent(new BigDecimal("3.00"));
            watchlist.add(item1);
            
            // Example item 2
            DashboardData.WatchlistItem item2 = new DashboardData.WatchlistItem();
            item2.setId("AAPL");
            item2.setSymbol("AAPL");
            item2.setName("Apple Inc.");
            item2.setType("STOCK");
            item2.setPrice(new BigDecimal("175.50"));
            item2.setChange(new BigDecimal("-0.50"));
            item2.setChangePercent(new BigDecimal("-0.28"));
            watchlist.add(item2);
            
            return watchlist;
        } catch (Exception e) {
            log.error("Error retrieving watchlist data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve watchlist data", e);
        }
    }
}
