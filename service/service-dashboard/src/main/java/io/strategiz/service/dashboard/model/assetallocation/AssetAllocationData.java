package io.strategiz.service.dashboard.model.assetallocation;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data model for asset allocation.
 */
@Data
public class AssetAllocationData {
    
    /**
     * List of asset allocations
     */
    private List<AssetAllocation> allocations;
    
    /**
     * Asset allocation data
     */
    @Data
    public static class AssetAllocation {
        /**
         * Asset name
         */
        private String name;
        
        /**
         * Asset symbol
         */
        private String symbol;
        
        /**
         * Value of asset holding in USD
         */
        private BigDecimal value;
        
        /**
         * Percentage of total portfolio value
         */
        private BigDecimal percentage;
        
        /**
         * Source exchange (Coinbase, Kraken, Binance, etc.)
         */
        private String exchange;
        
        /**
         * Color to use for this asset in charts
         */
        private String color;
    }
}
