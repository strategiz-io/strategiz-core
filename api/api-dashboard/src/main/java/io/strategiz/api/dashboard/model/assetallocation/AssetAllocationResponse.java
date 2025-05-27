package io.strategiz.api.dashboard.model.assetallocation;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response model for asset allocation data following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetAllocationResponse extends BaseServiceResponse {
    

    /**
     * List of asset allocations for pie chart
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
