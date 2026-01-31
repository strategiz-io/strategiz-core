package io.strategiz.client.coingecko.constant;

/**
 * Constants for CoinGecko API client
 */
public final class CoinGeckoConstants {

	private CoinGeckoConstants() {
		// Private constructor to prevent instantiation
	}

	// API Endpoints
	public static final String MARKETS_ENDPOINT = "/api/v3/coins/markets";

	public static final String COIN_INFO_ENDPOINT = "/api/v3/coins/{id}";

	public static final String SEARCH_ENDPOINT = "/api/v3/search";

	// Query Parameters
	public static final String PARAM_VS_CURRENCY = "vs_currency";

	public static final String PARAM_IDS = "ids";

	public static final String PARAM_PRICE_CHANGE = "price_change_percentage";

	public static final String PARAM_ORDER = "order";

	public static final String PARAM_PER_PAGE = "per_page";

	public static final String PARAM_PAGE = "page";

	public static final String PARAM_QUERY = "query";

	// Default Values
	public static final String DEFAULT_CURRENCY = "usd";

	public static final String DEFAULT_ORDER = "market_cap_desc";

	public static final int DEFAULT_PER_PAGE = 100;

	public static final String DEFAULT_PRICE_CHANGE = "24h,7d,30d";

	// Cache Configuration
	public static final String CACHE_NAME_MARKETS = "coingecko-markets";

	public static final String CACHE_NAME_COIN_INFO = "coingecko-coin-info";

	public static final String CACHE_NAME_SEARCH = "coingecko-search";

	// Error Messages
	public static final String ERROR_API_CALL_FAILED = "CoinGecko API call failed: ";

	public static final String ERROR_COIN_NOT_FOUND = "Cryptocurrency not found with ID: ";

	public static final String ERROR_INVALID_RESPONSE = "Invalid response from CoinGecko API";

}
