package io.strategiz.service.dashboard.constant;

/**
 * Constants for Dashboard Service
 */
public final class DashboardConstants {
    
    private DashboardConstants() {
        // Private constructor to prevent instantiation
    }
    
    // Cache Configuration
    public static final String CACHE_NAME_PORTFOLIO = "dashboard-portfolio-summary";
    public static final String CACHE_NAME_WATCHLIST = "dashboard-watchlist";
    public static final String CACHE_NAME_MARKET_DATA = "dashboard-market-data";
    
    // Default Values
    public static final String DEFAULT_CURRENCY = "USD";
    public static final int DEFAULT_WATCHLIST_SIZE = 10;
    public static final int DEFAULT_PORTFOLIO_CHART_DAYS = 30;
    
    // Asset Types
    public static final String ASSET_TYPE_CRYPTO = "CRYPTO";
    public static final String ASSET_TYPE_STOCK = "STOCK";
    public static final String ASSET_TYPE_ETF = "ETF";
    public static final String ASSET_TYPE_COMMODITY = "COMMODITY";
    
    // Default Assets
    public static final String DEFAULT_CRYPTO_BTC = "bitcoin";
    public static final String DEFAULT_CRYPTO_ETH = "ethereum";
    public static final String DEFAULT_STOCK_MSFT = "MSFT";
    public static final String DEFAULT_STOCK_AAPL = "AAPL";
    
    // Error Messages
    public static final String ERROR_PORTFOLIO_DATA_FETCH = "Error fetching portfolio data: ";
    public static final String ERROR_WATCHLIST_DATA_FETCH = "Error fetching watchlist data: ";
    public static final String ERROR_MARKET_DATA_FETCH = "Error fetching market data: ";
    public static final String ERROR_INVALID_ASSET_TYPE = "Invalid asset type: ";
    
    // Response Messages
    public static final String SUCCESS_PORTFOLIO_FETCH = "Portfolio summary data retrieved successfully";
    public static final String SUCCESS_WATCHLIST_FETCH = "Watchlist data retrieved successfully";
}
