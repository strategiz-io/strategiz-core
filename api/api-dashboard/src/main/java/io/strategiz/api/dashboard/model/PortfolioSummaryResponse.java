package io.strategiz.api.dashboard.model;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response model for portfolio summary data following Synapse patterns.
 * Designed to support the Portfolio Summary UI component.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioSummaryResponse extends BaseServiceResponse {
    
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
     * Flag indicating if any exchange connections exist
     */
    private boolean hasExchangeConnections;
    
    /**
     * Message to display when no exchange connections are found
     */
    private String statusMessage;
    
    /**
     * Flag indicating if the user needs to configure API keys
     */
    private boolean needsApiKeyConfiguration;
    
    /**
     * Exchange data
     */
    @Data
    public static class ExchangeData {
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
    }
    
    /**
     * Asset data
     */
    @Data
    public static class AssetData {
        private String symbol;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private BigDecimal allocationPercent;
    }
}
