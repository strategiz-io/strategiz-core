package io.strategiz.business.provider.kraken.enrichment.enricher;

import io.strategiz.business.provider.kraken.enrichment.model.EnrichedKrakenData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Enricher responsible for detecting and enriching staking information.
 * Identifies staked assets and adds APR information.
 * 
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenStakingEnricher {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenStakingEnricher.class);
    
    private final KrakenSymbolEnricher symbolEnricher;
    
    @Autowired
    public KrakenStakingEnricher(KrakenSymbolEnricher symbolEnricher) {
        this.symbolEnricher = symbolEnricher;
    }
    
    /**
     * Enrich data with staking information.
     * 
     * @param enrichedData The data to enrich with staking info
     * @param rawBalances Original raw balances to check for staking suffixes
     */
    public void enrichStakingData(EnrichedKrakenData enrichedData, Map<String, Object> rawBalances) {
        // Process each original balance to detect staking
        for (Map.Entry<String, Object> entry : rawBalances.entrySet()) {
            String originalSymbol = entry.getKey();
            
            if (symbolEnricher.isStakedAsset(originalSymbol)) {
                String normalizedSymbol = symbolEnricher.normalizeSymbol(originalSymbol);
                String stakingType = symbolEnricher.getStakingType(originalSymbol);
                
                // Find the corresponding asset info
                EnrichedKrakenData.AssetInfo assetInfo = findAssetInfo(enrichedData, normalizedSymbol);
                if (assetInfo != null) {
                    assetInfo.setStaked(true);
                    assetInfo.setOriginalSymbol(originalSymbol);
                    
                    // Set staking APR based on asset and type
                    BigDecimal apr = getStakingAPR(normalizedSymbol, stakingType);
                    assetInfo.setStakingAPR(apr);
                    
                    // Set staking period
                    String period = getStakingPeriod(normalizedSymbol, stakingType);
                    assetInfo.setStakingPeriod(period);
                    
                    log.debug("Enriched staking info for {}: APR {}%, Period: {}", 
                             normalizedSymbol, apr, period);
                }
            }
        }
    }
    
    /**
     * Find asset info in enriched data by normalized symbol.
     */
    private EnrichedKrakenData.AssetInfo findAssetInfo(EnrichedKrakenData data, String normalizedSymbol) {
        // First try exact match
        EnrichedKrakenData.AssetInfo info = data.getAssetInfo().get(normalizedSymbol);
        if (info != null) {
            return info;
        }
        
        // Try with _STAKED suffix
        info = data.getAssetInfo().get(normalizedSymbol + "_STAKED");
        if (info != null) {
            return info;
        }
        
        // Search by normalized symbol in values
        for (EnrichedKrakenData.AssetInfo assetInfo : data.getAssetInfo().values()) {
            if (normalizedSymbol.equals(assetInfo.getNormalizedSymbol())) {
                return assetInfo;
            }
        }
        
        return null;
    }
    
    /**
     * Get staking APR for an asset.
     * These are approximate current rates as of 2025.
     */
    private BigDecimal getStakingAPR(String symbol, String stakingType) {
        // Liquid staking typically has slightly lower APR
        boolean isLiquid = "liquid_staking".equals(stakingType);
        
        Map<String, BigDecimal> stakingAPRs = Map.ofEntries(
            // Ethereum and related
            Map.entry("ETH", new BigDecimal(isLiquid ? "3.5" : "4.0")),
            Map.entry("STETH", new BigDecimal("3.5")),
            Map.entry("RETH", new BigDecimal("3.3")),
            Map.entry("CBETH", new BigDecimal("3.2")),
            
            // Proof of Stake chains
            Map.entry("DOT", new BigDecimal(isLiquid ? "12.0" : "14.0")),
            Map.entry("KSM", new BigDecimal(isLiquid ? "15.0" : "18.0")),
            Map.entry("ADA", new BigDecimal(isLiquid ? "3.5" : "4.0")),
            Map.entry("ATOM", new BigDecimal(isLiquid ? "16.0" : "19.0")),
            Map.entry("SOL", new BigDecimal(isLiquid ? "6.0" : "7.0")),
            Map.entry("AVAX", new BigDecimal(isLiquid ? "7.0" : "8.5")),
            Map.entry("NEAR", new BigDecimal(isLiquid ? "9.0" : "11.0")),
            Map.entry("FTM", new BigDecimal(isLiquid ? "3.0" : "4.0")),
            Map.entry("ALGO", new BigDecimal(isLiquid ? "5.0" : "6.0")),
            Map.entry("XTZ", new BigDecimal(isLiquid ? "5.5" : "6.5")),
            Map.entry("MATIC", new BigDecimal(isLiquid ? "4.0" : "5.0")),
            Map.entry("TRX", new BigDecimal(isLiquid ? "4.5" : "5.5")),
            
            // Other stakeable assets
            Map.entry("FLOW", new BigDecimal(isLiquid ? "7.0" : "8.0")),
            Map.entry("KAVA", new BigDecimal(isLiquid ? "18.0" : "20.0")),
            Map.entry("ROSE", new BigDecimal(isLiquid ? "11.0" : "13.0")),
            Map.entry("MINA", new BigDecimal(isLiquid ? "14.0" : "16.0")),
            Map.entry("GRT", new BigDecimal(isLiquid ? "8.0" : "10.0")),
            Map.entry("LDO", new BigDecimal("4.0")),
            Map.entry("RPL", new BigDecimal("4.5")),
            
            // Cosmos ecosystem
            Map.entry("OSMO", new BigDecimal(isLiquid ? "14.0" : "16.0")),
            Map.entry("JUNO", new BigDecimal(isLiquid ? "28.0" : "30.0")),
            Map.entry("SCRT", new BigDecimal(isLiquid ? "23.0" : "25.0")),
            Map.entry("AKT", new BigDecimal(isLiquid ? "16.0" : "18.0")),
            Map.entry("INJ", new BigDecimal(isLiquid ? "14.0" : "16.0")),
            Map.entry("TIA", new BigDecimal(isLiquid ? "14.0" : "16.0")),
            Map.entry("DYM", new BigDecimal(isLiquid ? "17.0" : "19.0")),
            Map.entry("SEI", new BigDecimal(isLiquid ? "4.0" : "5.0")),
            Map.entry("SUI", new BigDecimal(isLiquid ? "3.0" : "4.0")),
            
            // Default for unknown assets
            Map.entry("DEFAULT", new BigDecimal(isLiquid ? "5.0" : "6.0"))
        );
        
        BigDecimal apr = stakingAPRs.get(symbol);
        if (apr == null) {
            apr = stakingAPRs.get("DEFAULT");
            log.debug("Using default APR for {}: {}%", symbol, apr);
        }
        
        return apr;
    }
    
    /**
     * Get staking period/lockup information.
     */
    private String getStakingPeriod(String symbol, String stakingType) {
        if ("liquid_staking".equals(stakingType)) {
            return "Flexible (Liquid)";
        }
        
        Map<String, String> stakingPeriods = Map.ofEntries(
            // Ethereum
            Map.entry("ETH", "Flexible"),
            
            // Fixed lockup periods
            Map.entry("DOT", "28 days unbonding"),
            Map.entry("KSM", "7 days unbonding"),
            Map.entry("ATOM", "21 days unbonding"),
            Map.entry("ADA", "Flexible"),
            Map.entry("SOL", "2-3 days warmup/cooldown"),
            Map.entry("AVAX", "14 days minimum"),
            Map.entry("NEAR", "2-3 days unbonding"),
            Map.entry("FTM", "7 days unbonding"),
            Map.entry("ALGO", "Flexible"),
            Map.entry("XTZ", "Flexible"),
            Map.entry("MATIC", "3-4 days unbonding"),
            Map.entry("TRX", "3 days unbonding"),
            
            // Cosmos ecosystem typically 21 days
            Map.entry("OSMO", "14 days unbonding"),
            Map.entry("JUNO", "28 days unbonding"),
            Map.entry("SCRT", "21 days unbonding"),
            Map.entry("AKT", "21 days unbonding"),
            Map.entry("INJ", "21 days unbonding"),
            Map.entry("TIA", "21 days unbonding"),
            Map.entry("DYM", "21 days unbonding"),
            Map.entry("SEI", "21 days unbonding"),
            Map.entry("SUI", "Flexible"),
            
            // Default
            Map.entry("DEFAULT", "Variable")
        );
        
        String period = stakingPeriods.get(symbol);
        if (period == null) {
            period = stakingPeriods.get("DEFAULT");
        }
        
        return period;
    }
}