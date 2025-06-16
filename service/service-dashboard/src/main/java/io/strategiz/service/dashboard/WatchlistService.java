package io.strategiz.service.dashboard;

import io.strategiz.client.alphavantage.AlphaVantageClient;
import io.strategiz.client.alphavantage.model.StockData;
import io.strategiz.client.coingecko.CoinGeckoClient;
import io.strategiz.service.dashboard.model.watchlist.WatchlistItem;
import java.util.Collections;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import io.strategiz.service.dashboard.model.watchlist.WatchlistAsset;
import io.strategiz.service.dashboard.model.watchlist.WatchlistResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service for watchlist operations.
 * This service provides data for user watchlists.
 */
@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    private final CoinGeckoClient coinGeckoClient;
    private final AlphaVantageClient alphaVantageClient;

    @Autowired
    public WatchlistService(CoinGeckoClient coinGeckoClient,
                          AlphaVantageClient alphaVantageClient) {
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
            List<WatchlistItem> watchlistItems = new ArrayList<>();
            
            for (WatchlistAsset asset : userWatchlist) {
                WatchlistItem item = new WatchlistItem();
                item.setSymbol(asset.getSymbol());
                item.setName(asset.getName());
                item.setType(asset.getType());
                
                // Get real-time data based on asset type
                if ("crypto".equalsIgnoreCase(asset.getType())) {
                    try {
                        List<CryptoCurrency> cryptoList = coinGeckoClient.getCryptocurrencyMarketData(Collections.singletonList(asset.getSymbol()), "usd");
                        if (cryptoList != null && !cryptoList.isEmpty()) {
                            CryptoCurrency cryptoData = cryptoList.get(0);
                            item.setCurrentPrice(cryptoData.getCurrentPrice());
                            item.setPriceChangePercentage24h(cryptoData.getPriceChangePercentage24h());
                        } else {
                            log.warn("No crypto data returned for symbol: {}", asset.getSymbol());
                            item.setCurrentPrice(BigDecimal.ZERO);
                            item.setPriceChangePercentage24h(BigDecimal.ZERO);
                        }
                    } catch (Exception e) {
                        log.warn("Error fetching crypto data for {}: {}", asset.getSymbol(), e.getMessage());
                        item.setCurrentPrice(BigDecimal.ZERO);
                        item.setPriceChangePercentage24h(BigDecimal.ZERO);
                    }
                } else if ("stock".equalsIgnoreCase(asset.getType())) {
                    try {
                        StockData stockData = alphaVantageClient.getStockQuote(asset.getSymbol());
                        if (stockData != null) {
                            item.setCurrentPrice(stockData.getPrice());
                            item.setPriceChangePercentage24h(stockData.getChangePercent());
                        } else {
                            log.warn("No stock data returned for symbol: {}", asset.getSymbol());
                            item.setCurrentPrice(BigDecimal.ZERO);
                            item.setPriceChangePercentage24h(BigDecimal.ZERO);
                        }
                    } catch (Exception e) {
                        log.warn("Error fetching stock data for {}: {}", asset.getSymbol(), e.getMessage());
                        item.setCurrentPrice(BigDecimal.ZERO);
                        item.setPriceChangePercentage24h(BigDecimal.ZERO);
                    }
                }
                
                watchlistItems.add(item);
            }
            
            response.setWatchlistItems(watchlistItems);
            return response;
        } catch (Exception e) {
            log.error("Error getting watchlist for user: " + userId, e);
            throw new RuntimeException("Failed to get watchlist", e);
        }
    }
    
    /**
     * Gets user watchlist from database or user service
     * 
     * @param userId The user ID to get watchlist for
     * @return List of watchlist assets
     */
    private List<WatchlistAsset> getUserWatchlist(String userId) {
        // In a real implementation, this would fetch from a database or user service
        // For now, return an empty list to trigger the default watchlist
        return new ArrayList<>();
    }
    
    /**
     * Gets default watchlist assets
     * 
     * @return List of default watchlist assets
     */
    private List<WatchlistAsset> getDefaultWatchlist() {
        List<WatchlistAsset> defaultWatchlist = new ArrayList<>();
        
        // Add some default crypto assets
        WatchlistAsset bitcoin = new WatchlistAsset();
        bitcoin.setSymbol("BTC");
        bitcoin.setName("Bitcoin");
        bitcoin.setType("crypto");
        defaultWatchlist.add(bitcoin);
        
        WatchlistAsset ethereum = new WatchlistAsset();
        ethereum.setSymbol("ETH");
        ethereum.setName("Ethereum");
        ethereum.setType("crypto");
        defaultWatchlist.add(ethereum);
        
        // Add some default stock assets
        WatchlistAsset apple = new WatchlistAsset();
        apple.setSymbol("AAPL");
        apple.setName("Apple Inc.");
        apple.setType("stock");
        defaultWatchlist.add(apple);
        
        WatchlistAsset microsoft = new WatchlistAsset();
        microsoft.setSymbol("MSFT");
        microsoft.setName("Microsoft Corporation");
        microsoft.setType("stock");
        defaultWatchlist.add(microsoft);
        
        return defaultWatchlist;
    }
}
