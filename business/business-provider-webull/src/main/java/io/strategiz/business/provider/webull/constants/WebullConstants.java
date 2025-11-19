package io.strategiz.business.provider.webull.constants;

import java.util.Set;

/**
 * Constants for Webull provider integration
 *
 * @author Strategiz Platform
 * @since 1.0
 */
public final class WebullConstants {

    // Provider identification
    public static final String PROVIDER_ID = "webull";
    public static final String PROVIDER_NAME = "Webull";
    public static final String PROVIDER_TYPE = "brokerage";

    // API endpoints
    public static final String BASE_URL = "https://api.webull.com";
    public static final String ACCOUNT_LIST_ENDPOINT = "/api/trade/account/list";
    public static final String ACCOUNT_POSITIONS_ENDPOINT = "/api/trade/account/positions";
    public static final String ACCOUNT_BALANCE_ENDPOINT = "/api/trade/account/balance";
    public static final String ORDERS_ENDPOINT = "/api/trade/orders";
    public static final String ORDER_PLACE_ENDPOINT = "/api/trade/order/place";

    // Connection configuration
    public static final String CONNECTION_TYPE = "api_key";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;

    // Signature configuration
    public static final String SIGNATURE_ALGORITHM = "HmacSHA1";
    public static final String SIGNATURE_VERSION = "1.0";

    // Rate limiting (10 calls per 30 seconds per App ID)
    public static final int RATE_LIMIT_CALLS = 10;
    public static final int RATE_LIMIT_WINDOW_SECONDS = 30;

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // Validation
    public static final int MIN_APP_KEY_LENGTH = 10;
    public static final int MIN_APP_SECRET_LENGTH = 10;

    // Supported instrument types
    public static final Set<String> SUPPORTED_INSTRUMENT_TYPES = Set.of(
            "STOCK",
            "ETF",
            "OPTION"
    );

    // Supported markets
    public static final Set<String> SUPPORTED_MARKETS = Set.of(
            "US"  // Currently only US stocks/ETFs
    );

    // Trading hours (Eastern Time)
    public static final String PRE_MARKET_START = "07:00";
    public static final String MARKET_OPEN = "09:30";
    public static final String MARKET_CLOSE = "16:00";
    public static final String AFTER_HOURS_END = "20:00";

    // Error codes from Webull API
    public static final String ERROR_INVALID_SIGNATURE = "INVALID_SIGNATURE";
    public static final String ERROR_INVALID_APP_KEY = "INVALID_APP_KEY";
    public static final String ERROR_RATE_LIMIT = "RATE_LIMIT_EXCEEDED";
    public static final String ERROR_INVALID_ACCOUNT = "INVALID_ACCOUNT";
    public static final String ERROR_PERMISSION_DENIED = "PERMISSION_DENIED";

    // Prevent instantiation
    private WebullConstants() {
        throw new AssertionError("WebullConstants class should not be instantiated");
    }
}
