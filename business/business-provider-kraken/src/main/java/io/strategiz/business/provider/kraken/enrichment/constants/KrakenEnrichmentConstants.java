package io.strategiz.business.provider.kraken.enrichment.constants;

import java.util.Set;

/**
 * Constants used for Kraken data enrichment.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
public class KrakenEnrichmentConstants {
    
    /**
     * Set of Kraken asset codes that represent cash/fiat currencies.
     */
    public static final Set<String> CASH_ASSETS = Set.of(
        "ZUSD", "USD", "ZEUR", "EUR", "ZGBP", "GBP", 
        "ZCAD", "CAD", "ZAUD", "AUD", "ZJPY", "JPY",
        "ZCHF", "CHF", "ZNZD", "NZD"
    );
    
    /**
     * Suffixes used by Kraken for staked assets.
     */
    public static final String STAKING_SUFFIX = ".S";
    
    /**
     * Suffixes used by Kraken for futures.
     */
    public static final String FUTURES_SUFFIX = ".F";
    
    private KrakenEnrichmentConstants() {
        // Private constructor to prevent instantiation
    }
}