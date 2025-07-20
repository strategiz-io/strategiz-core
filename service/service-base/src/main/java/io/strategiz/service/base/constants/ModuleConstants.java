package io.strategiz.service.base.constants;

/**
 * Centralized module names for consistent error handling and logging.
 * Each service module should use these constants when throwing exceptions.
 * 
 * This ensures:
 * - Consistent module identification in error messages
 * - Easy refactoring if module names change
 * - Clear module ownership for errors
 * - Better error tracking and debugging
 */
public final class ModuleConstants {
    
    // Prevent instantiation
    private ModuleConstants() {
        throw new UnsupportedOperationException("Constants class");
    }
    
    // Service modules
    public static final String AUTH_MODULE = "Authentication Service";
    public static final String PROFILE_MODULE = "Profile Service";
    public static final String DASHBOARD_MODULE = "Dashboard Service";
    public static final String PORTFOLIO_MODULE = "Portfolio Service";
    public static final String EXCHANGE_MODULE = "Exchange Service";
    public static final String STRATEGY_MODULE = "Strategy Service";
    public static final String MARKETPLACE_MODULE = "Marketplace Service";
    public static final String PROVIDER_MODULE = "Provider Service";
    public static final String DEVICE_MODULE = "Device Service";
    public static final String WALLET_MODULE = "Wallet Address Service";
    public static final String MONITORING_MODULE = "Monitoring Service";
    public static final String MARKETING_MODULE = "Marketing Service";
    
    // Framework modules
    public static final String EXCEPTION_MODULE = "Exception Framework";
    public static final String SECRETS_MODULE = "Secrets Framework";
    public static final String AI_MODULE = "AI Framework";
    public static final String FIREBASE_MODULE = "Firebase Framework";
    
    // Business modules
    public static final String TOKEN_AUTH_MODULE = "Token Authentication Business";
    public static final String PORTFOLIO_BUSINESS_MODULE = "Portfolio Business";
    public static final String STRATEGY_BUSINESS_MODULE = "Strategy Business";
    
    // Client modules
    public static final String FIREBASE_CLIENT_MODULE = "Firebase Client";
    public static final String VAULT_CLIENT_MODULE = "Vault Client";
    public static final String GOOGLE_CLIENT_MODULE = "Google OAuth Client";
    public static final String FACEBOOK_CLIENT_MODULE = "Facebook OAuth Client";
    public static final String BINANCE_CLIENT_MODULE = "Binance Client";
    public static final String COINBASE_CLIENT_MODULE = "Coinbase Client";
    public static final String KRAKEN_CLIENT_MODULE = "Kraken Client";
    public static final String ALPHAVANTAGE_CLIENT_MODULE = "Alpha Vantage Client";
    public static final String COINGECKO_CLIENT_MODULE = "CoinGecko Client";
    public static final String YAHOO_CLIENT_MODULE = "Yahoo Finance Client";
    
    // Data modules
    public static final String USER_DATA_MODULE = "User Data";
    public static final String AUTH_DATA_MODULE = "Authentication Data";
    public static final String SESSION_DATA_MODULE = "Session Data";
    public static final String PORTFOLIO_DATA_MODULE = "Portfolio Data";
    public static final String STRATEGY_DATA_MODULE = "Strategy Data";
    
    // Default for unknown modules
    public static final String UNKNOWN_MODULE = "Unknown Module";
    
    // System user constants for non-user-driven operations
    public static final String SYSTEM_USER_ID = "SYSTEM";
    public static final String SYSTEM_USER_EMAIL = "system@strategiz.io";
    public static final String SYSTEM_USER_NAME = "System Process";
}