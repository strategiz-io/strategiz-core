package io.strategiz.client.alphavantage.constant;

/**
 * Constants for AlphaVantage API client
 */
public final class AlphaVantageConstants {

	private AlphaVantageConstants() {
		// Private constructor to prevent instantiation
	}

	// API Endpoints and Functions
	public static final String ENDPOINT_QUERY = "/query";

	public static final String FUNCTION_QUOTE = "GLOBAL_QUOTE";

	public static final String FUNCTION_SEARCH = "SYMBOL_SEARCH";

	public static final String FUNCTION_OVERVIEW = "OVERVIEW";

	public static final String FUNCTION_TIME_SERIES_DAILY = "TIME_SERIES_DAILY";

	public static final String FUNCTION_TIME_SERIES_WEEKLY = "TIME_SERIES_WEEKLY";

	// Query Parameters
	public static final String PARAM_FUNCTION = "function";

	public static final String PARAM_SYMBOL = "symbol";

	public static final String PARAM_KEYWORDS = "keywords";

	public static final String PARAM_APIKEY = "apikey";

	public static final String PARAM_DATATYPE = "datatype";

	public static final String PARAM_OUTPUT_SIZE = "outputsize";

	// JSON Response Keys
	public static final String KEY_GLOBAL_QUOTE = "Global Quote";

	public static final String KEY_SYMBOL = "01. symbol";

	public static final String KEY_PRICE = "05. price";

	public static final String KEY_CHANGE = "09. change";

	public static final String KEY_CHANGE_PERCENT = "10. change percent";

	public static final String KEY_SEARCH_MATCHES = "bestMatches";

	// Default Values
	public static final String DEFAULT_DATATYPE = "json";

	public static final String DEFAULT_OUTPUT_SIZE = "compact";

	// Cache Configuration
	public static final String CACHE_NAME_QUOTE = "alphavantage-quote";

	public static final String CACHE_NAME_SEARCH = "alphavantage-search";

	public static final String CACHE_NAME_OVERVIEW = "alphavantage-overview";

	public static final String CACHE_NAME_TIME_SERIES = "alphavantage-time-series";

	// Error Messages
	public static final String ERROR_API_CALL_FAILED = "AlphaVantage API call failed: ";

	public static final String ERROR_STOCK_NOT_FOUND = "Stock not found with symbol: ";

	public static final String ERROR_INVALID_RESPONSE = "Invalid response from AlphaVantage API";

	public static final String ERROR_API_LIMIT_EXCEEDED = "AlphaVantage API call limit exceeded";

}
