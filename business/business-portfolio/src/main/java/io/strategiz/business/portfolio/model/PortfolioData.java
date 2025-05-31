package io.strategiz.business.portfolio.model;

import io.strategiz.framework.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Business model for portfolio data following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioData extends BaseModel {
    
    /**
     * User ID associated with this portfolio data
     */
    private String userId;
    
    /**
     * Total portfolio value
     */
    private BigDecimal totalValue;
    
    /**
     * Daily change in portfolio value
     */
    private BigDecimal dailyChange;
    
    /**
     * Daily change percentage
     */
    private BigDecimal dailyChangePercent;
    
    /**
     * Map of exchange ID to exchange data
     */
    private Map<String, ExchangeData> exchanges;
    
    /**
     * Exchange data
     */
    @Data
    public static class ExchangeData {
        private String id;
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
    }
    
    /**
     * Asset data
     */
    @Data
    public static class AssetData {
        private String id;
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal amount;  // Added for compatibility with existing code
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;
    }
}
