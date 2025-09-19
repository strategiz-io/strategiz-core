package io.strategiz.business.provider.kraken.constants;

import java.util.Map;
import java.util.Set;

/**
 * Constants for Kraken provider integration
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
public final class KrakenConstants {
    
    // Provider identification
    public static final String PROVIDER_ID = "kraken";
    public static final String PROVIDER_NAME = "Kraken";
    public static final String PROVIDER_TYPE = "crypto";
    
    // API endpoints (relative paths for private API)
    public static final String BALANCE_ENDPOINT = "/private/Balance";
    public static final String TRADES_ENDPOINT = "/private/TradesHistory";
    public static final String OPEN_ORDERS_ENDPOINT = "/private/OpenOrders";
    public static final String CLOSED_ORDERS_ENDPOINT = "/private/ClosedOrders";
    public static final String TICKER_ENDPOINT = "/public/Ticker";
    public static final String ASSET_PAIRS_ENDPOINT = "/public/AssetPairs";
    
    // Connection configuration
    public static final String CONNECTION_TYPE = "api_key";
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    
    // Asset mappings - Kraken uses prefixes for some assets
    public static final Map<String, String> ASSET_MAPPING = Map.ofEntries(
        Map.entry("XXBT", "BTC"),     // Bitcoin
        Map.entry("XBT", "BTC"),      // Alternative Bitcoin symbol
        Map.entry("XETH", "ETH"),     // Ethereum
        Map.entry("ETH2", "ETH"),     // Staked Ethereum
        Map.entry("ETH2.S", "ETH"),   // Staked Ethereum
        Map.entry("XXRP", "XRP"),     // Ripple
        Map.entry("XLTC", "LTC"),     // Litecoin
        Map.entry("XXDG", "DOGE"),    // Dogecoin
        Map.entry("XDG", "DOGE"),     // Alternative Dogecoin symbol
        Map.entry("TRX.F", "TRX"),    // Tron (futures)
        Map.entry("ETH.F", "ETH"),    // Ethereum futures
        Map.entry("POL", "POL"),      // Polygon (POL is the new ticker)
        Map.entry("POL.S", "POL"),    // Polygon Staked
        Map.entry("POL.F", "POL"),    // Polygon futures
        Map.entry("MATIC", "POL"),    // Old Polygon ticker
        Map.entry("DOT.F", "DOT"),    // Polkadot
        Map.entry("ADA.F", "ADA"),    // Cardano
        Map.entry("ATOM.F", "ATOM"),  // Cosmos
        Map.entry("KSM.F", "KSM"),    // Kusama
        Map.entry("SOL.F", "SOL"),    // Solana
        Map.entry("INJ.F", "INJ"),    // Injective
        Map.entry("SUI.F", "SUI"),    // Sui
        Map.entry("SEI.F", "SEI"),    // Sei
        Map.entry("TIA", "TIA"),      // Celestia (spot)
        Map.entry("TIA.S", "TIA"),    // Celestia staked
        Map.entry("TIA.F", "TIA"),    // Celestia futures
        Map.entry("DYM.F", "DYM"),    // Dymension
        Map.entry("BABY", "BABY"),    // BabyDoge
        Map.entry("RENDER", "RNDR"),  // Render Token
        Map.entry("RNDR", "RNDR"),    // Render Token alternative
        Map.entry("GALA", "GALA"),    // Gala
        Map.entry("GAL", "GALA"),     // Alternative Gala symbol
        Map.entry("PEPE", "PEPE"),    // Pepe
        Map.entry("ZUSD", "USD"),     // US Dollar
        Map.entry("ZEUR", "EUR"),     // Euro
        Map.entry("ZGBP", "GBP"),     // British Pound
        Map.entry("ZCAD", "CAD"),     // Canadian Dollar
        Map.entry("ZJPY", "JPY")      // Japanese Yen
    );
    
    // Cash/Fiat assets
    public static final Set<String> CASH_ASSETS = Set.of(
        "ZUSD", "USD",
        "ZEUR", "EUR",
        "ZGBP", "GBP",
        "ZCAD", "CAD",
        "ZJPY", "JPY",
        "ZAUD", "AUD",
        "ZCHF", "CHF"
    );
    
    // Rate limiting
    public static final int API_CALL_TIER_STARTER = 10;  // calls per second
    public static final int API_CALL_TIER_INTERMEDIATE = 20;
    public static final int API_CALL_TIER_PRO = 30;
    
    // Validation
    public static final int MIN_API_KEY_LENGTH = 20;
    public static final int MIN_API_SECRET_LENGTH = 20;
    
    // Error codes from Kraken API
    public static final String ERROR_INVALID_KEY = "EAPI:Invalid key";
    public static final String ERROR_INVALID_SIGNATURE = "EAPI:Invalid signature";
    public static final String ERROR_INVALID_NONCE = "EAPI:Invalid nonce";
    public static final String ERROR_PERMISSION_DENIED = "EAPI:Permission denied";
    public static final String ERROR_RATE_LIMIT = "EAPI:Rate limit exceeded";
    
    // Prevent instantiation
    private KrakenConstants() {
        throw new AssertionError("KrakenConstants class should not be instantiated");
    }
}