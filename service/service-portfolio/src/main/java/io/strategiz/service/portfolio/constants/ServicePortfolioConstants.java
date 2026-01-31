package io.strategiz.service.portfolio.constants;

/**
 * Constants for the Portfolio Service module. Single Responsibility: Only holds constants
 * for this module.
 */
public final class ServicePortfolioConstants {

	private ServicePortfolioConstants() {
		// Prevent instantiation
	}

	// API Paths
	public static final String BASE_PATH = "/v1/portfolio";

	public static final String SUMMARY_PATH = "/summary";

	public static final String OVERVIEW_PATH = "/overview";

	public static final String PROVIDERS_PATH = "/providers";

	public static final String REFRESH_PATH = "/refresh";

	// Module name for logging
	public static final String MODULE_NAME = "service-portfolio";

	// Default values
	public static final int DEFAULT_TOP_HOLDINGS_LIMIT = 10;

	public static final int DEFAULT_MAX_PROVIDERS = 10;

	public static final int DEFAULT_CACHE_TIMEOUT_MINUTES = 5;

	// Provider types
	public static final String PROVIDER_KRAKEN = "kraken";

	public static final String PROVIDER_COINBASE = "coinbase";

	public static final String PROVIDER_BINANCE = "binanceus";

	public static final String PROVIDER_ALPACA = "alpaca";

	public static final String PROVIDER_SCHWAB = "schwab";

	public static final String PROVIDER_ROBINHOOD = "robinhood";

	// Asset types
	public static final String ASSET_TYPE_CRYPTO = "crypto";

	public static final String ASSET_TYPE_STOCK = "stock";

	public static final String ASSET_TYPE_FOREX = "forex";

	public static final String ASSET_TYPE_COMMODITY = "commodity";

	// Sync status
	public static final String SYNC_STATUS_SUCCESS = "success";

	public static final String SYNC_STATUS_ERROR = "error";

	public static final String SYNC_STATUS_SYNCING = "syncing";

	// Error messages
	public static final String ERROR_NO_AUTH = "Authentication required";

	public static final String ERROR_PROVIDER_NOT_FOUND = "Provider not found";

	public static final String ERROR_PORTFOLIO_FETCH_FAILED = "Failed to fetch portfolio data";

}