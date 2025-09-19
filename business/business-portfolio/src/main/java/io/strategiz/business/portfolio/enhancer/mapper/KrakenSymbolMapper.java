package io.strategiz.business.portfolio.enhancer.mapper;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Kraken-specific symbols to standard symbols and provides metadata.
 * Handles Kraken's unique naming conventions and staking suffixes.
 */
@Component
public class KrakenSymbolMapper {
    
    private static final Map<String, String> KRAKEN_TO_STANDARD;
    private static final Map<String, String> STAKING_APR_MAP;
    
    static {
        // Initialize Kraken symbol mappings
        KRAKEN_TO_STANDARD = new HashMap<>();
        
        // Crypto mappings
        KRAKEN_TO_STANDARD.put("XXBT", "BTC");      // Bitcoin
        KRAKEN_TO_STANDARD.put("XBT", "BTC");        // Bitcoin (alternative)
        KRAKEN_TO_STANDARD.put("XETH", "ETH");       // Ethereum
        KRAKEN_TO_STANDARD.put("ETH", "ETH");        // Ethereum (direct)
        KRAKEN_TO_STANDARD.put("XXRP", "XRP");       // Ripple
        KRAKEN_TO_STANDARD.put("XRP", "XRP");        // Ripple (direct)
        KRAKEN_TO_STANDARD.put("XXLM", "XLM");       // Stellar  
        KRAKEN_TO_STANDARD.put("XLTC", "LTC");       // Litecoin
        KRAKEN_TO_STANDARD.put("XXMR", "XMR");       // Monero
        KRAKEN_TO_STANDARD.put("XZEC", "ZEC");       // Zcash
        KRAKEN_TO_STANDARD.put("XXDG", "DOGE");      // Dogecoin
        KRAKEN_TO_STANDARD.put("XDG", "DOGE");       // Dogecoin (alternative)
        KRAKEN_TO_STANDARD.put("DOGE", "DOGE");      // Dogecoin (direct)
        
        // Fiat mappings
        KRAKEN_TO_STANDARD.put("ZUSD", "USD");       // US Dollar
        KRAKEN_TO_STANDARD.put("ZEUR", "EUR");       // Euro
        KRAKEN_TO_STANDARD.put("ZGBP", "GBP");       // British Pound
        KRAKEN_TO_STANDARD.put("ZCAD", "CAD");       // Canadian Dollar
        KRAKEN_TO_STANDARD.put("ZJPY", "JPY");       // Japanese Yen
        KRAKEN_TO_STANDARD.put("ZAUD", "AUD");       // Australian Dollar
        KRAKEN_TO_STANDARD.put("ZCHF", "CHF");       // Swiss Franc
        
        // Direct mappings (already standard)
        KRAKEN_TO_STANDARD.put("ADA", "ADA");        // Cardano
        KRAKEN_TO_STANDARD.put("ALGO", "ALGO");      // Algorand
        KRAKEN_TO_STANDARD.put("ATOM", "ATOM");      // Cosmos
        KRAKEN_TO_STANDARD.put("AVAX", "AVAX");      // Avalanche
        KRAKEN_TO_STANDARD.put("BAT", "BAT");        // Basic Attention Token
        KRAKEN_TO_STANDARD.put("BCH", "BCH");        // Bitcoin Cash
        KRAKEN_TO_STANDARD.put("COMP", "COMP");      // Compound
        KRAKEN_TO_STANDARD.put("CRV", "CRV");        // Curve
        KRAKEN_TO_STANDARD.put("DAI", "DAI");        // DAI Stablecoin
        KRAKEN_TO_STANDARD.put("DOT", "DOT");        // Polkadot
        KRAKEN_TO_STANDARD.put("EOS", "EOS");        // EOS
        KRAKEN_TO_STANDARD.put("FIL", "FIL");        // Filecoin
        KRAKEN_TO_STANDARD.put("FLOW", "FLOW");      // Flow
        KRAKEN_TO_STANDARD.put("GRT", "GRT");        // The Graph
        KRAKEN_TO_STANDARD.put("ICX", "ICX");        // ICON
        KRAKEN_TO_STANDARD.put("INJ", "INJ");        // Injective
        KRAKEN_TO_STANDARD.put("KAVA", "KAVA");      // Kava
        KRAKEN_TO_STANDARD.put("KSM", "KSM");        // Kusama
        KRAKEN_TO_STANDARD.put("LINK", "LINK");      // Chainlink
        KRAKEN_TO_STANDARD.put("LSK", "LSK");        // Lisk
        KRAKEN_TO_STANDARD.put("MANA", "MANA");      // Decentraland
        KRAKEN_TO_STANDARD.put("MATIC", "MATIC");    // Polygon
        KRAKEN_TO_STANDARD.put("MKR", "MKR");        // Maker
        KRAKEN_TO_STANDARD.put("OCEAN", "OCEAN");    // Ocean Protocol
        KRAKEN_TO_STANDARD.put("OMG", "OMG");        // OMG Network
        KRAKEN_TO_STANDARD.put("OXT", "OXT");        // Orchid
        KRAKEN_TO_STANDARD.put("PAXG", "PAXG");      // PAX Gold
        KRAKEN_TO_STANDARD.put("QTUM", "QTUM");      // Qtum
        KRAKEN_TO_STANDARD.put("RENDER", "RENDER");  // Render
        KRAKEN_TO_STANDARD.put("REP", "REP");        // Augur
        KRAKEN_TO_STANDARD.put("SAND", "SAND");      // The Sandbox
        KRAKEN_TO_STANDARD.put("SC", "SC");          // Siacoin
        KRAKEN_TO_STANDARD.put("SNX", "SNX");        // Synthetix
        KRAKEN_TO_STANDARD.put("SOL", "SOL");        // Solana
        KRAKEN_TO_STANDARD.put("STORJ", "STORJ");    // Storj
        KRAKEN_TO_STANDARD.put("SUSHI", "SUSHI");    // SushiSwap
        KRAKEN_TO_STANDARD.put("TRX", "TRX");        // TRON
        KRAKEN_TO_STANDARD.put("UNI", "UNI");        // Uniswap
        KRAKEN_TO_STANDARD.put("USDC", "USDC");      // USD Coin
        KRAKEN_TO_STANDARD.put("USDT", "USDT");      // Tether
        KRAKEN_TO_STANDARD.put("WAVES", "WAVES");    // Waves
        KRAKEN_TO_STANDARD.put("WBTC", "WBTC");      // Wrapped Bitcoin
        KRAKEN_TO_STANDARD.put("XTZ", "XTZ");        // Tezos
        KRAKEN_TO_STANDARD.put("YFI", "YFI");        // Yearn Finance
        KRAKEN_TO_STANDARD.put("ZRX", "ZRX");        // 0x
        
        // Additional from user's portfolio
        KRAKEN_TO_STANDARD.put("AKT", "AKT");        // Akash
        KRAKEN_TO_STANDARD.put("SEI", "SEI");        // Sei
        KRAKEN_TO_STANDARD.put("GALA", "GALA");      // Gala Games
        KRAKEN_TO_STANDARD.put("BABY", "BABY");      // BabyDoge
        KRAKEN_TO_STANDARD.put("POL", "POL");        // Polygon (new ticker)
        KRAKEN_TO_STANDARD.put("PEPE", "PEPE");      // Pepe
        KRAKEN_TO_STANDARD.put("BTC", "BTC");        // Bitcoin (direct)
        KRAKEN_TO_STANDARD.put("INJ", "INJ");        // Injective (direct mapping already exists above)
        
        // Initialize staking APR map
        STAKING_APR_MAP = new HashMap<>();
        STAKING_APR_MAP.put("ETH", "3-5% APR");
        STAKING_APR_MAP.put("ADA", "4-6% APR");
        STAKING_APR_MAP.put("DOT", "12-14% APR");
        STAKING_APR_MAP.put("SOL", "5-7% APR");
        STAKING_APR_MAP.put("ATOM", "7-9% APR");
        STAKING_APR_MAP.put("TRX", "3-4% APR");
        STAKING_APR_MAP.put("MATIC", "3-4% APR");
        STAKING_APR_MAP.put("POL", "3-4% APR");
        STAKING_APR_MAP.put("AVAX", "5-9% APR");
        STAKING_APR_MAP.put("AKT", "4-5% APR");
        STAKING_APR_MAP.put("INJ", "7-8% APR");
        STAKING_APR_MAP.put("SEI", "4-5% APR");
    }
    
    /**
     * Convert Kraken symbol to standard symbol
     * @param krakenSymbol The Kraken-specific symbol
     * @return Standard symbol and staking info
     */
    public SymbolMapping mapSymbol(String krakenSymbol) {
        if (krakenSymbol == null || krakenSymbol.isEmpty()) {
            return null;
        }
        
        SymbolMapping mapping = new SymbolMapping();
        mapping.setOriginalSymbol(krakenSymbol);
        
        // Check for staking suffixes
        boolean isStaked = false;
        String baseSymbol = krakenSymbol;
        
        if (krakenSymbol.endsWith(".S")) {
            isStaked = true;
            baseSymbol = krakenSymbol.substring(0, krakenSymbol.length() - 2);
        } else if (krakenSymbol.endsWith(".F")) {
            isStaked = true;
            baseSymbol = krakenSymbol.substring(0, krakenSymbol.length() - 2);
        } else if (krakenSymbol.equals("ETH2") || krakenSymbol.equals("ETH2.S")) {
            isStaked = true;
            baseSymbol = "XETH";
        }
        
        // Map to standard symbol
        String standardSymbol = KRAKEN_TO_STANDARD.get(baseSymbol);
        if (standardSymbol == null) {
            // If not in map, use the base symbol as-is
            standardSymbol = baseSymbol;
        }
        
        mapping.setStandardSymbol(standardSymbol);
        mapping.setStaked(isStaked);
        
        // Add staking APR if applicable
        if (isStaked && STAKING_APR_MAP.containsKey(standardSymbol)) {
            mapping.setStakingAPR(STAKING_APR_MAP.get(standardSymbol));
        }
        
        // Determine asset type
        if (isFiatSymbol(standardSymbol)) {
            mapping.setAssetType("fiat");
        } else {
            mapping.setAssetType("crypto");
        }
        
        return mapping;
    }
    
    /**
     * Check if a symbol represents fiat currency
     */
    private boolean isFiatSymbol(String symbol) {
        return symbol.equals("USD") || symbol.equals("EUR") || symbol.equals("GBP") ||
               symbol.equals("CAD") || symbol.equals("JPY") || symbol.equals("AUD") ||
               symbol.equals("CHF") || symbol.equals("CNY") || symbol.equals("KRW");
    }
    
    /**
     * Inner class to hold mapping result
     */
    public static class SymbolMapping {
        private String originalSymbol;
        private String standardSymbol;
        private String assetType;
        private boolean isStaked;
        private String stakingAPR;
        
        // Getters and Setters
        public String getOriginalSymbol() {
            return originalSymbol;
        }
        
        public void setOriginalSymbol(String originalSymbol) {
            this.originalSymbol = originalSymbol;
        }
        
        public String getStandardSymbol() {
            return standardSymbol;
        }
        
        public void setStandardSymbol(String standardSymbol) {
            this.standardSymbol = standardSymbol;
        }
        
        public String getAssetType() {
            return assetType;
        }
        
        public void setAssetType(String assetType) {
            this.assetType = assetType;
        }
        
        public boolean isStaked() {
            return isStaked;
        }
        
        public void setStaked(boolean staked) {
            isStaked = staked;
        }
        
        public String getStakingAPR() {
            return stakingAPR;
        }
        
        public void setStakingAPR(String stakingAPR) {
            this.stakingAPR = stakingAPR;
        }
    }
}