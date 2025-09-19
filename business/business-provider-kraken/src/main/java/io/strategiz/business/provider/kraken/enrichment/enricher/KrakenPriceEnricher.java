package io.strategiz.business.provider.kraken.enrichment.enricher;

import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Enricher responsible for adding pricing information and calculating portfolio values.
 * Handles current prices, 24h changes, and total value calculations.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenPriceEnricher {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenPriceEnricher.class);
    
    /**
     * Enrich data with pricing information and calculate values.
     * 
     * @param enrichedData The data to enrich with prices
     * @param currentPrices Map of current market prices (may be incomplete)
     */
    public void enrichWithPrices(EnrichedKrakenData enrichedData, Map<String, BigDecimal> currentPrices) {
        log.debug("Enriching with price data for {} assets", enrichedData.getAssetInfo().size());
        
        for (Map.Entry<String, EnrichedKrakenData.AssetInfo> entry : enrichedData.getAssetInfo().entrySet()) {
            String symbol = entry.getKey();
            EnrichedKrakenData.AssetInfo info = entry.getValue();
            
            // Get or derive the price
            BigDecimal price = getPriceForAsset(info, currentPrices);
            info.setCurrentPrice(price);
            
            // Calculate current value
            BigDecimal value = info.getQuantity().multiply(price).setScale(2, RoundingMode.HALF_UP);
            info.setCurrentValue(value);
            
            // Add 24h price change (would normally come from market data API)
            BigDecimal priceChange = get24hPriceChange(info.getNormalizedSymbol());
            info.setPriceChange24h(priceChange);
            
            log.debug("Priced {}: {} @ ${} = ${}", 
                     info.getNormalizedSymbol(), 
                     info.getQuantity(), 
                     price, 
                     value);
        }
    }
    
    /**
     * Get price for an asset, using various sources and fallbacks.
     */
    private BigDecimal getPriceForAsset(EnrichedKrakenData.AssetInfo info, Map<String, BigDecimal> providedPrices) {
        String symbol = info.getNormalizedSymbol();
        
        // Remove any suffix for price lookup
        String baseSymbol = symbol.replace("_STAKED", "");
        
        // First check provided prices (from Kraken API or previous fetch)
        if (providedPrices != null) {
            // Try original symbol from Kraken
            if (info.getOriginalSymbol() != null && providedPrices.containsKey(info.getOriginalSymbol())) {
                return providedPrices.get(info.getOriginalSymbol());
            }
            
            // Try normalized symbol
            if (providedPrices.containsKey(baseSymbol)) {
                return providedPrices.get(baseSymbol);
            }
        }
        
        // For fiat currencies, the price is always 1 USD equivalent
        if (info.isCash()) {
            return getFiatToUsdRate(baseSymbol);
        }
        
        // Use fallback static prices when no real price is available
        BigDecimal fallbackPrice = getFallbackPrice(baseSymbol);
        if (fallbackPrice.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Using fallback price for {}: ${}", baseSymbol, fallbackPrice);
            return fallbackPrice;
        }
        
        log.warn("No price available for {} - will show as $0 value", baseSymbol);
        return BigDecimal.ZERO;
    }
    
    /**
     * Get fiat to USD conversion rate.
     */
    private BigDecimal getFiatToUsdRate(String currency) {
        Map<String, BigDecimal> fiatRates = Map.of(
            "USD", BigDecimal.ONE,
            "EUR", new BigDecimal("1.08"),
            "GBP", new BigDecimal("1.27"),
            "CAD", new BigDecimal("0.73"),
            "AUD", new BigDecimal("0.65"),
            "JPY", new BigDecimal("0.0067"),
            "CHF", new BigDecimal("1.12"),
            "NZD", new BigDecimal("0.60")
        );
        
        return fiatRates.getOrDefault(currency, BigDecimal.ONE);
    }
    
    /**
     * Get price for unknown assets - returns zero to indicate no price available.
     * We don't use fake prices when not in demo mode.
     */
    private BigDecimal getHardcodedPrice(String symbol) {
        log.warn("No real price available for {} - value will be shown as $0", symbol);
        // Return zero instead of fake price - better to show incomplete data than fake data
        return BigDecimal.ZERO;
    }
    
    /**
     * Get fallback price for an asset when real-time data isn't available.
     */
    private BigDecimal getFallbackPrice(String symbol) {
        // Static price mappings based on recent market data
        Map<String, BigDecimal> fallbackPrices = Map.ofEntries(
            Map.entry("BTC", new BigDecimal("110851.07")),
            Map.entry("ETH", new BigDecimal("4292.10")),
            Map.entry("ADA", new BigDecimal("0.8333")),
            Map.entry("SOL", new BigDecimal("206.16")),
            Map.entry("DOT", new BigDecimal("7.50")),
            Map.entry("MATIC", new BigDecimal("0.85")),
            Map.entry("POL", new BigDecimal("0.276")),
            Map.entry("LINK", new BigDecimal("15.00")),
            Map.entry("UNI", new BigDecimal("6.50")),
            Map.entry("ATOM", new BigDecimal("8.20")),
            Map.entry("XRP", new BigDecimal("2.87")),
            Map.entry("XLM", new BigDecimal("0.12")),
            Map.entry("LTC", new BigDecimal("75.00")),
            Map.entry("DOGE", new BigDecimal("0.2276")),
            Map.entry("AVAX", new BigDecimal("24.65")),
            Map.entry("TRX", new BigDecimal("0.3303")),
            Map.entry("XMR", new BigDecimal("160.00")),
            Map.entry("ZEC", new BigDecimal("35.00")),
            Map.entry("ALGO", new BigDecimal("0.20")),
            Map.entry("FLOW", new BigDecimal("0.80")),
            Map.entry("NEAR", new BigDecimal("2.50")),
            Map.entry("FIL", new BigDecimal("4.50")),
            Map.entry("GRT", new BigDecimal("0.18")),
            Map.entry("OCEAN", new BigDecimal("0.65")),
            Map.entry("STORJ", new BigDecimal("0.55")),
            Map.entry("SAND", new BigDecimal("0.40")),
            Map.entry("MANA", new BigDecimal("0.55")),
            Map.entry("GALA", new BigDecimal("0.01625")),
            Map.entry("AKT", new BigDecimal("1.09")),
            Map.entry("SEI", new BigDecimal("0.2945")),
            Map.entry("INJ", new BigDecimal("12.98")),
            Map.entry("RENDER", new BigDecimal("5.50")),
            Map.entry("SHIB", new BigDecimal("0.00001")),
            Map.entry("PEPE", new BigDecimal("0.00001")),
            Map.entry("APE", new BigDecimal("1.50")),
            Map.entry("AAVE", new BigDecimal("85.00")),
            Map.entry("CRV", new BigDecimal("0.45")),
            Map.entry("MKR", new BigDecimal("1450.00")),
            Map.entry("COMP", new BigDecimal("45.00")),
            Map.entry("SNX", new BigDecimal("2.20")),
            Map.entry("YFI", new BigDecimal("5500.00")),
            Map.entry("SUSHI", new BigDecimal("0.95")),
            Map.entry("BAL", new BigDecimal("2.15")),
            Map.entry("1INCH", new BigDecimal("0.35")),
            Map.entry("ENJ", new BigDecimal("0.28")),
            Map.entry("CHZ", new BigDecimal("0.07")),
            Map.entry("BLUR", new BigDecimal("0.25")),
            Map.entry("IMX", new BigDecimal("1.40")),
            Map.entry("LDO", new BigDecimal("1.85")),
            Map.entry("RPL", new BigDecimal("25.00")),
            Map.entry("KAVA", new BigDecimal("0.65")),
            Map.entry("SCRT", new BigDecimal("0.35")),
            Map.entry("TIA", new BigDecimal("5.20")),
            Map.entry("OSMO", new BigDecimal("0.55")),
            Map.entry("JUNO", new BigDecimal("0.35")),
            Map.entry("BAND", new BigDecimal("1.35")),
            Map.entry("KSM", new BigDecimal("25.00"))
        );
        
        return fallbackPrices.getOrDefault(symbol, BigDecimal.ZERO);
    }
    
    /**
     * Get 24h price change percentage.
     * Currently returns zero as we don't have real 24h change data yet.
     */
    private BigDecimal get24hPriceChange(String symbol) {
        // TODO: This should come from Kraken ticker API which includes 24h change data
        // For now, return zero rather than fake data
        return BigDecimal.ZERO;
    }
}