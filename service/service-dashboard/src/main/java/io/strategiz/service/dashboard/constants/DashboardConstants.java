package io.strategiz.service.dashboard.constants;

/**
 * Constants used by the dashboard service.
 */
public class DashboardConstants {
    
    // API endpoints
    public static final String API_COINBASE = "/api/exchange/coinbase";
    public static final String API_KRAKEN = "/api/exchange/kraken";
    public static final String API_BINANCE = "/api/exchange/binance";
    public static final String API_ROBINHOOD = "/api/exchange/robinhood";
    
    // Watchlist categories
    public static final String CATEGORY_ALL = "All";
    public static final String CATEGORY_CRYPTO = "Crypto";
    public static final String CATEGORY_STOCKS = "Stocks";
    
    // Asset types
    public static final String TYPE_CRYPTO = "crypto";
    public static final String TYPE_STOCK = "stock";
    public static final String TYPE_ETF = "etf";
    public static final String TYPE_FOREX = "forex";
    
    // Market indices
    public static final String INDEX_BITCOIN = "BTCUSD";
    public static final String INDEX_ETHEREUM = "ETHUSD";
    public static final String INDEX_SP500 = "SPX";
    public static final String INDEX_NASDAQ = "IXIC";
    public static final String INDEX_DOW = "DJI";
    
    // Time periods
    public static final String PERIOD_DAY = "1d";
    public static final String PERIOD_WEEK = "1w";
    public static final String PERIOD_MONTH = "1m";
    public static final String PERIOD_YEAR = "1y";
    
    // Status messages
    public static final String STATUS_NO_CONNECTIONS = "No exchange connections found. Please connect your accounts.";
    public static final String SUCCESS_WATCHLIST_FETCH = "Successfully fetched watchlist data";
    
    // Default assets for watchlists
    public static final String DEFAULT_CRYPTO_BTC = "bitcoin";
    public static final String DEFAULT_CRYPTO_ETH = "ethereum";
    public static final String DEFAULT_STOCK_MSFT = "MSFT";
    public static final String DEFAULT_STOCK_AAPL = "AAPL";
    
    // Log messages
    public static final String LOG_PORTFOLIO_FETCH_SUCCESS = "Successfully fetched portfolio data for user: {}";
    public static final String LOG_PORTFOLIO_FETCH_ERROR = "Error fetching portfolio data for user: {}";
    public static final String LOG_MARKET_DATA_FETCH_SUCCESS = "Successfully fetched market data";
    public static final String LOG_MARKET_DATA_FETCH_ERROR = "Error fetching market data: {}";
    public static final String LOG_WATCHLIST_FETCH_SUCCESS = "Successfully fetched watchlist data for user: {}";
    public static final String LOG_WATCHLIST_FETCH_ERROR = "Error fetching watchlist data for user: {}";
    
    private DashboardConstants() {
        // Private constructor to prevent instantiation
    }
}
