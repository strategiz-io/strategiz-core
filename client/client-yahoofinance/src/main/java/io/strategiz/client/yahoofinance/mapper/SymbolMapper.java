package io.strategiz.client.yahoofinance.mapper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps various symbol formats to Yahoo Finance ticker symbols.
 * Single Responsibility: Symbol format conversion.
 */
@Component
public class SymbolMapper {
    
    private final Map<String, String> cryptoSymbolMap;
    
    public SymbolMapper() {
        this.cryptoSymbolMap = initializeCryptoSymbolMap();
    }
    
    /**
     * Convert a symbol to Yahoo Finance format.
     * 
     * @param symbol Input symbol (e.g., "BTC", "XXBT", "BTC.S")
     * @return Yahoo Finance formatted symbol (e.g., "BTC-USD")
     */
    public String toYahooSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return symbol;
        }
        
        // Clean the symbol (remove staking suffixes)
        String cleanSymbol = cleanSymbol(symbol);
        
        // Check if we have a direct mapping
        String yahooSymbol = cryptoSymbolMap.get(cleanSymbol.toUpperCase());
        if (yahooSymbol != null) {
            return yahooSymbol;
        }
        
        // If no mapping exists and doesn't contain hyphen, assume crypto and add -USD
        if (!cleanSymbol.contains("-")) {
            return cleanSymbol.toUpperCase() + "-USD";
        }
        
        // Return as-is if already formatted
        return cleanSymbol.toUpperCase();
    }
    
    /**
     * Clean symbol by removing known suffixes and prefixes.
     */
    private String cleanSymbol(String symbol) {
        return symbol
            .replace(".S", "")
            .replace("_STAKED", "")
            .trim();
    }
    
    /**
     * Initialize the crypto symbol mapping.
     */
    private Map<String, String> initializeCryptoSymbolMap() {
        Map<String, String> map = new HashMap<>();
        
        // Major cryptocurrencies
        map.put("BTC", "BTC-USD");
        map.put("ETH", "ETH-USD");
        map.put("BNB", "BNB-USD");
        map.put("XRP", "XRP-USD");
        map.put("ADA", "ADA-USD");
        map.put("SOL", "SOL-USD");
        map.put("DOGE", "DOGE-USD");
        map.put("DOT", "DOT-USD");
        map.put("AVAX", "AVAX-USD");
        map.put("MATIC", "MATIC-USD");
        map.put("POL", "POL1-USD");
        map.put("LINK", "LINK-USD");
        map.put("LTC", "LTC-USD");
        map.put("UNI", "UNI-USD");
        map.put("ATOM", "ATOM-USD");
        map.put("XLM", "XLM-USD");
        map.put("ETC", "ETC-USD");
        map.put("XMR", "XMR-USD");
        map.put("BCH", "BCH-USD");
        map.put("ALGO", "ALGO-USD");
        map.put("TRX", "TRX-USD");
        map.put("NEAR", "NEAR-USD");
        map.put("FIL", "FIL-USD");
        map.put("ICP", "ICP-USD");
        map.put("APT", "APT-USD");
        map.put("ARB", "ARB-USD");
        map.put("OP", "OP-USD");
        
        // DeFi tokens
        map.put("AAVE", "AAVE-USD");
        map.put("MKR", "MKR-USD");
        map.put("COMP", "COMP-USD");
        map.put("SNX", "SNX-USD");
        map.put("CRV", "CRV-USD");
        map.put("SUSHI", "SUSHI-USD");
        map.put("YFI", "YFI-USD");
        map.put("BAL", "BAL-USD");
        map.put("1INCH", "1INCH-USD");
        map.put("INCH", "1INCH-USD");
        
        // Gaming & Metaverse
        map.put("AXS", "AXS-USD");
        map.put("SAND", "SAND-USD");
        map.put("MANA", "MANA-USD");
        map.put("ENJ", "ENJ-USD");
        map.put("GALA", "GALA-USD");
        map.put("IMX", "IMX-USD");
        map.put("APE", "APE-USD");
        map.put("CHZ", "CHZ-USD");
        
        // Layer 2 & Infrastructure
        map.put("LDO", "LDO-USD");
        map.put("RPL", "RPL-USD");
        map.put("STX", "STX-USD");
        map.put("FLOW", "FLOW-USD");
        map.put("GRT", "GRT-USD");
        map.put("OCEAN", "OCEAN-USD");
        map.put("RENDER", "RNDR-USD");
        map.put("RNDR", "RNDR-USD");
        map.put("AKT", "AKT3-USD");
        
        // Meme coins
        map.put("SHIB", "SHIB-USD");
        map.put("PEPE", "PEPE24478-USD");
        
        // Stablecoins
        map.put("USDT", "USDT-USD");
        map.put("USDC", "USDC-USD");
        map.put("DAI", "DAI-USD");
        map.put("BUSD", "BUSD-USD");
        
        // Exchange-specific symbols (Kraken)
        map.put("XXBT", "BTC-USD");
        map.put("XETH", "ETH-USD");
        map.put("XXRP", "XRP-USD");
        map.put("XLTC", "LTC-USD");
        map.put("XXLM", "XLM-USD");
        map.put("XZEC", "ZEC-USD");
        map.put("XXMR", "XMR-USD");
        map.put("XDOGE", "DOGE-USD");
        
        // Additional tokens
        map.put("ZEC", "ZEC-USD");
        map.put("ZRX", "ZRX-USD");
        map.put("BAT", "BAT-USD");
        map.put("BAND", "BAND-USD");
        map.put("DASH", "DASH-USD");
        map.put("EOS", "EOS-USD");
        map.put("WAVES", "WAVES-USD");
        map.put("KNC", "KNC-USD");
        map.put("KSM", "KSM-USD");
        map.put("LRC", "LRC-USD");
        map.put("OMG", "OMG-USD");
        map.put("ONT", "ONT-USD");
        map.put("QTUM", "QTUM-USD");
        map.put("REN", "REN-USD");
        map.put("STORJ", "STORJ-USD");
        map.put("UMA", "UMA-USD");
        map.put("XTZ", "XTZ-USD");
        
        // Cosmos ecosystem
        map.put("TIA", "TIA-USD");
        map.put("OSMO", "OSMO-USD");
        map.put("JUNO", "JUNO-USD");
        map.put("SCRT", "SCRT-USD");
        map.put("KAVA", "KAVA-USD");
        map.put("INJ", "INJ-USD");
        map.put("SEI", "SEI-USD");
        
        // Other DeFi
        map.put("BLUR", "BLUR-USD");
        map.put("CVX", "CVX-USD");
        map.put("FXS", "FXS-USD");
        map.put("GMX", "GMX-USD");
        map.put("LPT", "LPT-USD");
        map.put("PERP", "PERP-USD");
        map.put("SUI", "SUI-USD");
        
        return map;
    }
}