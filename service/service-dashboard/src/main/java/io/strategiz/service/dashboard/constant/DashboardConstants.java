package io.strategiz.service.dashboard.constant;

/**
 * Constants for Dashboard Service
 */
public final class DashboardConstants {

	private DashboardConstants() {
		// Private constructor to prevent instantiation
	}

	// API Endpoints
	public static final String API_COINBASE = "/api/exchange/coinbase";

	public static final String API_KRAKEN = "/api/exchange/kraken";

	public static final String API_BINANCE = "/api/exchange/binance";

	public static final String API_ROBINHOOD = "/api/exchange/robinhood";

	// Cache Configuration
	public static final String CACHE_NAME_PORTFOLIO = "dashboard-portfolio-summary";

	public static final String CACHE_NAME_WATCHLIST = "dashboard-watchlist";

	public static final String CACHE_NAME_MARKET_DATA = "dashboard-market-data";

	// Default Values
	public static final String DEFAULT_CURRENCY = "USD";

	public static final int DEFAULT_WATCHLIST_SIZE = 10;

	public static final int DEFAULT_PORTFOLIO_CHART_DAYS = 30;

	// Asset Types & Categories
	public static final String CATEGORY_ALL = "All";

	public static final String CATEGORY_CRYPTO = "Crypto";

	public static final String CATEGORY_STOCKS = "Stocks";

	public static final String TYPE_CRYPTO = "crypto";

	public static final String TYPE_STOCK = "stock";

	public static final String TYPE_ETF = "etf";

	public static final String TYPE_FOREX = "forex";

	public static final String ASSET_TYPE_COMMODITY = "COMMODITY";

	// Market Indexes
	public static final String INDEX_BITCOIN = "BTCUSD";

	public static final String INDEX_ETHEREUM = "ETHUSD";

	public static final String INDEX_SP500 = "SPX";

	public static final String INDEX_NASDAQ = "IXIC";

	public static final String INDEX_DOW = "DJI";

	// Time Periods
	public static final String PERIOD_DAY = "1d";

	public static final String PERIOD_WEEK = "1w";

	public static final String PERIOD_MONTH = "1m";

	public static final String PERIOD_YEAR = "1y";

	// Default Assets
	public static final String DEFAULT_CRYPTO_BTC = "bitcoin";

	public static final String DEFAULT_CRYPTO_ETH = "ethereum";

	public static final String DEFAULT_STOCK_MSFT = "MSFT";

	public static final String DEFAULT_STOCK_AAPL = "AAPL";

	// Status & Messages
	public static final String STATUS_NO_CONNECTIONS = "No exchange connections found. Please connect your accounts.";

	public static final String SUCCESS_WATCHLIST_FETCH = "Successfully fetched watchlist data";

	public static final String SUCCESS_PORTFOLIO_FETCH = "Portfolio summary data retrieved successfully";

	// Log Messages
	public static final String LOG_PORTFOLIO_FETCH_SUCCESS = "Successfully fetched portfolio data for user: {}";

	public static final String LOG_PORTFOLIO_FETCH_ERROR = "Error fetching portfolio data for user: {}";

	public static final String LOG_MARKET_DATA_FETCH_SUCCESS = "Successfully fetched market data";

	public static final String LOG_MARKET_DATA_FETCH_ERROR = "Error fetching market data: {}";

	public static final String LOG_WATCHLIST_FETCH_SUCCESS = "Successfully fetched watchlist data for user: {}";

	public static final String LOG_WATCHLIST_FETCH_ERROR = "Error fetching watchlist data for user: {}";

	// Error Messages
	public static final String ERROR_PORTFOLIO_DATA_FETCH = "Error fetching portfolio data: ";

	public static final String ERROR_WATCHLIST_DATA_FETCH = "Error fetching watchlist data: ";

	public static final String ERROR_MARKET_DATA_FETCH = "Error fetching market data: ";

	public static final String ERROR_INVALID_ASSET_TYPE = "Invalid asset type: ";

}
