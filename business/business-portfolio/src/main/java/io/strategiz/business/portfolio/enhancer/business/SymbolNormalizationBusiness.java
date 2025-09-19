package io.strategiz.business.portfolio.enhancer.business;

import io.strategiz.business.portfolio.enhancer.mapper.KrakenSymbolMapper;
import io.strategiz.business.portfolio.enhancer.mapper.KrakenSymbolMapper.SymbolMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Business component for normalizing provider-specific symbols to standard symbols.
 * Handles different exchange naming conventions and symbol formats.
 */
@Component
public class SymbolNormalizationBusiness {
    
    private final KrakenSymbolMapper krakenMapper;
    
    // Add more provider mappers as needed
    // private final CoinbaseSymbolMapper coinbaseMapper;
    // private final BinanceSymbolMapper binanceMapper;
    
    @Autowired
    public SymbolNormalizationBusiness(KrakenSymbolMapper krakenMapper) {
        this.krakenMapper = krakenMapper;
    }
    
    /**
     * Normalize a symbol from a specific provider to standard format
     * @param providerSymbol The provider-specific symbol
     * @param providerName The name of the provider (e.g., "kraken", "coinbase")
     * @return Normalized symbol information
     */
    public NormalizedSymbol normalize(String providerSymbol, String providerName) {
        if (providerSymbol == null || providerSymbol.isEmpty()) {
            return createUnknownSymbol(providerSymbol);
        }
        
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(providerSymbol);
        result.setProvider(providerName);
        
        switch (providerName.toLowerCase()) {
            case "kraken":
                return normalizeKrakenSymbol(providerSymbol);
            case "coinbase":
                return normalizeCoinbaseSymbol(providerSymbol);
            case "binance":
            case "binanceus":
                return normalizeBinanceSymbol(providerSymbol);
            case "alpaca":
                return normalizeAlpacaSymbol(providerSymbol);
            default:
                // For unknown providers, assume symbol is already standard
                result.setStandardSymbol(providerSymbol);
                result.setAssetType(guessAssetType(providerSymbol));
                return result;
        }
    }
    
    /**
     * Normalize Kraken symbol using the Kraken mapper
     */
    private NormalizedSymbol normalizeKrakenSymbol(String krakenSymbol) {
        SymbolMapping mapping = krakenMapper.mapSymbol(krakenSymbol);
        
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(krakenSymbol);
        result.setProvider("kraken");
        result.setStandardSymbol(mapping.getStandardSymbol());
        result.setAssetType(mapping.getAssetType());
        result.setStaked(mapping.isStaked());
        result.setStakingInfo(mapping.getStakingAPR());
        
        return result;
    }
    
    /**
     * Normalize Coinbase symbol (Coinbase uses standard symbols mostly)
     */
    private NormalizedSymbol normalizeCoinbaseSymbol(String symbol) {
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(symbol);
        result.setProvider("coinbase");
        
        // Coinbase generally uses standard symbols
        // Special cases can be added here
        Map<String, String> coinbaseMapping = Map.of(
            "CGLD", "CELO",  // Celo Gold
            "WLUNA", "LUNC"  // Wrapped Luna Classic
        );
        
        String standardSymbol = coinbaseMapping.getOrDefault(symbol, symbol);
        result.setStandardSymbol(standardSymbol);
        result.setAssetType(guessAssetType(standardSymbol));
        
        return result;
    }
    
    /**
     * Normalize Binance symbol
     */
    private NormalizedSymbol normalizeBinanceSymbol(String symbol) {
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(symbol);
        result.setProvider("binance");
        
        // Binance uses standard symbols but may have trading pairs
        // Remove trading pair suffix if present (e.g., "BTCUSDT" -> "BTC")
        String standardSymbol = symbol;
        if (symbol.endsWith("USDT") || symbol.endsWith("BUSD") || symbol.endsWith("USD")) {
            standardSymbol = symbol.replaceAll("(USDT|BUSD|USD)$", "");
        }
        
        result.setStandardSymbol(standardSymbol);
        result.setAssetType(guessAssetType(standardSymbol));
        
        return result;
    }
    
    /**
     * Normalize Alpaca symbol (for stocks)
     */
    private NormalizedSymbol normalizeAlpacaSymbol(String symbol) {
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(symbol);
        result.setProvider("alpaca");
        result.setStandardSymbol(symbol); // Alpaca uses standard stock tickers
        result.setAssetType("stock");
        
        return result;
    }
    
    /**
     * Guess asset type based on symbol characteristics
     */
    private String guessAssetType(String symbol) {
        // Common fiat currencies
        if (symbol.matches("^(USD|EUR|GBP|JPY|CAD|AUD|CHF|CNY|KRW)$")) {
            return "fiat";
        }
        
        // Stock symbols are typically 1-5 uppercase letters
        if (symbol.matches("^[A-Z]{1,5}$") && !isCommonCrypto(symbol)) {
            return "stock";
        }
        
        // Default to crypto for everything else
        return "crypto";
    }
    
    /**
     * Check if symbol is a common crypto
     */
    private boolean isCommonCrypto(String symbol) {
        return symbol.matches("^(BTC|ETH|XRP|ADA|SOL|DOT|DOGE|AVAX|MATIC|LINK|UNI|ATOM|LTC|XLM|XMR)$");
    }
    
    /**
     * Create unknown symbol result
     */
    private NormalizedSymbol createUnknownSymbol(String symbol) {
        NormalizedSymbol result = new NormalizedSymbol();
        result.setOriginalSymbol(symbol);
        result.setStandardSymbol(symbol);
        result.setAssetType("unknown");
        return result;
    }
    
    /**
     * Inner class to hold normalization result
     */
    public static class NormalizedSymbol {
        private String originalSymbol;
        private String standardSymbol;
        private String provider;
        private String assetType;
        private boolean isStaked;
        private String stakingInfo;
        
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
        
        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
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
        
        public String getStakingInfo() {
            return stakingInfo;
        }
        
        public void setStakingInfo(String stakingInfo) {
            this.stakingInfo = stakingInfo;
        }
    }
}