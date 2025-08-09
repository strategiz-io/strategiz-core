package io.strategiz.data.provider.constants;

/**
 * Constants for data-provider module
 */
public final class DataProviderConstants {
    
    private DataProviderConstants() {
        // Prevent instantiation
    }
    
    // Collection names
    public static final String COLLECTION_PROVIDER_INTEGRATIONS = "provider_integrations";
    
    // Provider types
    public static final String PROVIDER_TYPE_EXCHANGE = "exchange";
    public static final String PROVIDER_TYPE_OAUTH = "oauth";
    public static final String PROVIDER_TYPE_TRADING = "trading";
    public static final String PROVIDER_TYPE_CRYPTO = "crypto";
    public static final String PROVIDER_TYPE_BANK = "bank";
    
    // Connection types
    public static final String CONNECTION_TYPE_API_KEY = "api_key";
    public static final String CONNECTION_TYPE_OAUTH = "oauth";
    public static final String CONNECTION_TYPE_OAUTH2 = "oauth2";
    public static final String CONNECTION_TYPE_PLAID = "plaid";
    
    // Status values
    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PENDING_OAUTH = "pending_oauth";
    
    // Capabilities
    public static final String CAPABILITY_READ = "READ";
    public static final String CAPABILITY_TRADE = "TRADE";
    public static final String CAPABILITY_PORTFOLIO = "PORTFOLIO";
    public static final String CAPABILITY_TRANSFER = "TRANSFER";
    
    // Provider IDs
    public static final String PROVIDER_ID_COINBASE = "coinbase";
    public static final String PROVIDER_ID_KRAKEN = "kraken";
    public static final String PROVIDER_ID_BINANCEUS = "binanceus";
    public static final String PROVIDER_ID_ALPACA = "alpaca";
    
    // Error codes
    public static final String ERROR_PROVIDER_NOT_FOUND = "PROVIDER_NOT_FOUND";
    public static final String ERROR_PROVIDER_ALREADY_EXISTS = "PROVIDER_ALREADY_EXISTS";
    public static final String ERROR_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ERROR_CONNECTION_FAILED = "CONNECTION_FAILED";
    public static final String ERROR_OAUTH_FAILED = "OAUTH_FAILED";
}