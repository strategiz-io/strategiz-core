package io.strategiz.business.provider.kraken.enrichment.enricher;

import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Enricher responsible for adding metadata to assets including full names,
 * asset types, categories, and market cap rankings.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenAssetMetadataEnricher {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenAssetMetadataEnricher.class);
    
    private final KrakenSymbolEnricher symbolEnricher;
    
    @Autowired
    public KrakenAssetMetadataEnricher(KrakenSymbolEnricher symbolEnricher) {
        this.symbolEnricher = symbolEnricher;
    }
    
    /**
     * Enrich normalized balances with asset metadata.
     * 
     * @param normalizedBalances Map of normalized symbols to quantities
     * @return Map of asset information with metadata
     */
    public Map<String, EnrichedKrakenData.AssetInfo> enrichWithMetadata(Map<String, Object> normalizedBalances) {
        Map<String, EnrichedKrakenData.AssetInfo> assetInfoMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : normalizedBalances.entrySet()) {
            String symbol = entry.getKey();
            BigDecimal quantity = new BigDecimal(entry.getValue().toString());
            
            // Skip zero balances
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            EnrichedKrakenData.AssetInfo info = new EnrichedKrakenData.AssetInfo();
            info.setNormalizedSymbol(symbol);
            info.setQuantity(quantity);
            
            // Set full name
            info.setFullName(getAssetFullName(symbol));
            
            // Set asset type
            String assetType = getAssetType(symbol);
            info.setAssetType(assetType);
            info.setCash("fiat".equals(assetType));
            
            // Set category
            info.setCategory(getAssetCategory(symbol));
            
            // Set market cap rank
            info.setMarketCapRank(getMarketCapRank(symbol));
            
            assetInfoMap.put(symbol, info);
            
            log.debug("Enriched metadata for {}: {} ({}), Category: {}, Rank: {}", 
                     symbol, info.getFullName(), info.getAssetType(), 
                     info.getCategory(), info.getMarketCapRank());
        }
        
        return assetInfoMap;
    }
    
    /**
     * Get the full display name for an asset symbol.
     */
    private String getAssetFullName(String symbol) {
        // Remove any suffixes for lookup
        String baseSymbol = symbol.replace("_STAKED", "");
        
        Map<String, String> assetNames = Map.ofEntries(
            // Major cryptocurrencies
            Map.entry("BTC", "Bitcoin"),
            Map.entry("ETH", "Ethereum"),
            Map.entry("XRP", "Ripple"),
            Map.entry("LTC", "Litecoin"),
            Map.entry("BCH", "Bitcoin Cash"),
            Map.entry("ADA", "Cardano"),
            Map.entry("DOT", "Polkadot"),
            Map.entry("LINK", "Chainlink"),
            Map.entry("UNI", "Uniswap"),
            Map.entry("DOGE", "Dogecoin"),
            Map.entry("SHIB", "Shiba Inu"),
            Map.entry("AVAX", "Avalanche"),
            Map.entry("MATIC", "Polygon"),
            Map.entry("SOL", "Solana"),
            Map.entry("ATOM", "Cosmos"),
            Map.entry("FIL", "Filecoin"),
            Map.entry("TRX", "Tron"),
            Map.entry("VET", "VeChain"),
            Map.entry("XLM", "Stellar"),
            Map.entry("XMR", "Monero"),
            Map.entry("EOS", "EOS"),
            Map.entry("ALGO", "Algorand"),
            Map.entry("XTZ", "Tezos"),
            Map.entry("THETA", "Theta"),
            Map.entry("AXS", "Axie Infinity"),
            Map.entry("SAND", "The Sandbox"),
            Map.entry("MANA", "Decentraland"),
            Map.entry("GALA", "Gala"),
            Map.entry("ENJ", "Enjin Coin"),
            Map.entry("CHZ", "Chiliz"),
            Map.entry("CRV", "Curve"),
            Map.entry("AAVE", "Aave"),
            Map.entry("COMP", "Compound"),
            Map.entry("MKR", "Maker"),
            Map.entry("SNX", "Synthetix"),
            Map.entry("SUSHI", "SushiSwap"),
            Map.entry("YFI", "Yearn.finance"),
            Map.entry("1INCH", "1inch"),
            Map.entry("BAT", "Basic Attention Token"),
            Map.entry("ZEC", "Zcash"),
            Map.entry("DASH", "Dash"),
            Map.entry("WAVES", "Waves"),
            Map.entry("KSM", "Kusama"),
            Map.entry("NEAR", "NEAR Protocol"),
            Map.entry("GRT", "The Graph"),
            Map.entry("FTM", "Fantom"),
            Map.entry("OP", "Optimism"),
            Map.entry("APT", "Aptos"),
            Map.entry("ARB", "Arbitrum"),
            Map.entry("IMX", "Immutable X"),
            Map.entry("INJ", "Injective"),
            Map.entry("SUI", "Sui"),
            Map.entry("SEI", "Sei"),
            Map.entry("TIA", "Celestia"),
            Map.entry("DYM", "Dymension"),
            Map.entry("PEPE", "Pepe"),
            Map.entry("BONK", "Bonk"),
            Map.entry("WIF", "dogwifhat"),
            Map.entry("FLOKI", "Floki"),
            
            // Stablecoins
            Map.entry("USDT", "Tether"),
            Map.entry("USDC", "USD Coin"),
            Map.entry("BUSD", "Binance USD"),
            Map.entry("DAI", "Dai"),
            Map.entry("TUSD", "TrueUSD"),
            Map.entry("USDP", "Pax Dollar"),
            Map.entry("GUSD", "Gemini Dollar"),
            
            // Fiat currencies
            Map.entry("USD", "US Dollar"),
            Map.entry("EUR", "Euro"),
            Map.entry("GBP", "British Pound"),
            Map.entry("CAD", "Canadian Dollar"),
            Map.entry("AUD", "Australian Dollar"),
            Map.entry("JPY", "Japanese Yen"),
            Map.entry("CHF", "Swiss Franc"),
            Map.entry("NZD", "New Zealand Dollar"),
            
            // Additional assets
            Map.entry("STETH", "Lido Staked ETH"),
            Map.entry("WBTC", "Wrapped Bitcoin"),
            Map.entry("WETH", "Wrapped Ethereum"),
            Map.entry("RETH", "Rocket Pool ETH"),
            Map.entry("CBETH", "Coinbase Wrapped ETH"),
            Map.entry("LDO", "Lido DAO"),
            Map.entry("RPL", "Rocket Pool"),
            Map.entry("FXS", "Frax Share"),
            Map.entry("CVX", "Convex Finance"),
            Map.entry("STX", "Stacks"),
            Map.entry("BLUR", "Blur"),
            Map.entry("CFX", "Conflux"),
            Map.entry("DYDX", "dYdX"),
            Map.entry("ENS", "Ethereum Name Service"),
            Map.entry("FLR", "Flare"),
            Map.entry("FLOW", "Flow"),
            Map.entry("ICP", "Internet Computer"),
            Map.entry("KAVA", "Kava"),
            Map.entry("OCEAN", "Ocean Protocol"),
            Map.entry("ROSE", "Oasis Network"),
            Map.entry("STORJ", "Storj"),
            Map.entry("ANKR", "Ankr"),
            Map.entry("API3", "API3"),
            Map.entry("BAND", "Band Protocol"),
            Map.entry("BAL", "Balancer"),
            Map.entry("BNT", "Bancor"),
            Map.entry("C98", "Coin98"),
            Map.entry("CELR", "Celer Network"),
            Map.entry("CTK", "CertiK"),
            Map.entry("CVC", "Civic"),
            Map.entry("FORTH", "Ampleforth Governance"),
            Map.entry("GNO", "Gnosis"),
            Map.entry("JASMY", "JasmyCoin"),
            Map.entry("KEEP", "Keep Network"),
            Map.entry("KNC", "Kyber Network"),
            Map.entry("LPT", "Livepeer"),
            Map.entry("MLN", "Enzyme"),
            Map.entry("NMR", "Numeraire"),
            Map.entry("NU", "NuCypher"),
            Map.entry("OGN", "Origin Protocol"),
            Map.entry("OMG", "OMG Network"),
            Map.entry("OXT", "Orchid"),
            Map.entry("PERP", "Perpetual Protocol"),
            Map.entry("PLA", "PlayDapp"),
            Map.entry("POLS", "Polkastarter"),
            Map.entry("POND", "Marlin"),
            Map.entry("QNT", "Quant"),
            Map.entry("RAD", "Radicle"),
            Map.entry("RARI", "Rarible"),
            Map.entry("REN", "Ren"),
            Map.entry("REP", "Augur"),
            Map.entry("REQ", "Request"),
            Map.entry("RLC", "iExec RLC"),
            Map.entry("SKL", "SKALE Network"),
            Map.entry("SPELL", "Spell Token"),
            Map.entry("SUPER", "SuperFarm"),
            Map.entry("SYN", "Synapse"),
            Map.entry("TRIBE", "Tribe"),
            Map.entry("UMA", "UMA"),
            Map.entry("UNFI", "Unifi Protocol"),
            Map.entry("WAXL", "Axelar"),
            Map.entry("XCN", "Chain"),
            Map.entry("ZRX", "0x Protocol")
        );
        
        return assetNames.getOrDefault(baseSymbol, baseSymbol);
    }
    
    /**
     * Determine the asset type for a symbol.
     */
    private String getAssetType(String symbol) {
        // Check if it's fiat
        if (symbolEnricher.isCashAsset(symbol)) {
            return "fiat";
        }
        
        // Check if it's a stablecoin
        if (isStablecoin(symbol)) {
            return "stablecoin";
        }
        
        // Default to crypto
        return "crypto";
    }
    
    /**
     * Check if a symbol is a stablecoin.
     */
    private boolean isStablecoin(String symbol) {
        return symbol.matches("USD[CT]|BUSD|DAI|TUSD|USDP|GUSD|FRAX|LUSD|USDD|USTC");
    }
    
    /**
     * Get the category for an asset.
     */
    private String getAssetCategory(String symbol) {
        Map<String, String> categories = Map.ofEntries(
            // Layer 1 blockchains
            Map.entry("BTC", "Layer1"),
            Map.entry("ETH", "Layer1"),
            Map.entry("SOL", "Layer1"),
            Map.entry("ADA", "Layer1"),
            Map.entry("AVAX", "Layer1"),
            Map.entry("DOT", "Layer1"),
            Map.entry("ATOM", "Layer1"),
            Map.entry("NEAR", "Layer1"),
            Map.entry("FTM", "Layer1"),
            Map.entry("ALGO", "Layer1"),
            Map.entry("XTZ", "Layer1"),
            Map.entry("EOS", "Layer1"),
            Map.entry("TRX", "Layer1"),
            Map.entry("XLM", "Layer1"),
            
            // Layer 2 solutions
            Map.entry("MATIC", "Layer2"),
            Map.entry("OP", "Optimism"),
            Map.entry("ARB", "Layer2"),
            Map.entry("IMX", "Layer2"),
            
            // DeFi
            Map.entry("UNI", "DeFi"),
            Map.entry("AAVE", "DeFi"),
            Map.entry("COMP", "DeFi"),
            Map.entry("MKR", "DeFi"),
            Map.entry("SNX", "DeFi"),
            Map.entry("SUSHI", "DeFi"),
            Map.entry("YFI", "DeFi"),
            Map.entry("CRV", "DeFi"),
            Map.entry("1INCH", "DeFi"),
            Map.entry("BAL", "DeFi"),
            Map.entry("BNT", "DeFi"),
            Map.entry("LDO", "DeFi"),
            Map.entry("CVX", "DeFi"),
            Map.entry("FXS", "DeFi"),
            
            // Gaming & Metaverse
            Map.entry("AXS", "Gaming"),
            Map.entry("SAND", "Metaverse"),
            Map.entry("MANA", "Metaverse"),
            Map.entry("GALA", "Gaming"),
            Map.entry("ENJ", "Gaming"),
            Map.entry("CHZ", "Gaming"),
            
            // Meme coins
            Map.entry("DOGE", "Meme"),
            Map.entry("SHIB", "Meme"),
            Map.entry("PEPE", "Meme"),
            Map.entry("BONK", "Meme"),
            Map.entry("WIF", "Meme"),
            Map.entry("FLOKI", "Meme"),
            
            // Infrastructure
            Map.entry("LINK", "Oracle"),
            Map.entry("GRT", "Infrastructure"),
            Map.entry("FIL", "Storage"),
            Map.entry("STORJ", "Storage"),
            Map.entry("ANKR", "Infrastructure"),
            Map.entry("BAND", "Oracle"),
            Map.entry("API3", "Oracle"),
            
            // Privacy
            Map.entry("XMR", "Privacy"),
            Map.entry("ZEC", "Privacy"),
            Map.entry("DASH", "Privacy")
        );
        
        return categories.getOrDefault(symbol.replace("_STAKED", ""), "Other");
    }
    
    /**
     * Get the market cap rank for major assets.
     */
    private Integer getMarketCapRank(String symbol) {
        Map<String, Integer> ranks = Map.ofEntries(
            Map.entry("BTC", 1),
            Map.entry("ETH", 2),
            Map.entry("USDT", 3),
            Map.entry("SOL", 4),
            Map.entry("USDC", 5),
            Map.entry("XRP", 6),
            Map.entry("DOGE", 7),
            Map.entry("TRX", 8),
            Map.entry("ADA", 9),
            Map.entry("AVAX", 10),
            Map.entry("LINK", 11),
            Map.entry("DOT", 12),
            Map.entry("MATIC", 13),
            Map.entry("SHIB", 14),
            Map.entry("UNI", 15),
            Map.entry("LTC", 16),
            Map.entry("BCH", 17),
            Map.entry("ATOM", 18),
            Map.entry("XLM", 19),
            Map.entry("FIL", 20)
        );
        
        return ranks.get(symbol.replace("_STAKED", ""));
    }
}