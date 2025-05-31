package io.strategiz.api.dashboard.model.portfoliosummary;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO model for portfolio summary data following Synapse patterns.
 * Includes validation annotations and serialization configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioSummaryDTO extends BaseServiceResponse {
    
    /**
     * User ID associated with this portfolio summary
     */
    @NotBlank(message = "User ID is required")
    private String userId;
    
    /**
     * Total portfolio value
     */
    @NotNull(message = "Total value is required")
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
     * Weekly change in portfolio value
     */
    private BigDecimal weeklyChange;
    
    /**
     * Weekly change percentage
     */
    private BigDecimal weeklyChangePercent;
    
    /**
     * Monthly change in portfolio value
     */
    private BigDecimal monthlyChange;
    
    /**
     * Monthly change percentage
     */
    private BigDecimal monthlyChangePercent;
    
    /**
     * Yearly change in portfolio value
     */
    private BigDecimal yearlyChange;
    
    /**
     * Yearly change percentage
     */
    private BigDecimal yearlyChangePercent;
    
    /**
     * List of assets in the portfolio
     */
    @Valid
    private List<AssetDTO> assets;
    
    /**
     * Map of exchange ID to exchange data
     */
    private Map<String, ExchangeDataDTO> exchanges;
    
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
     * Timestamp of last update
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;
    
    /**
     * DTO for individual asset in a portfolio.
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssetDTO {
        /**
         * Asset identifier
         */
        @NotBlank(message = "Asset ID is required")
        private String id;
        
        /**
         * Asset ticker symbol
         */
        @NotBlank(message = "Symbol is required")
        private String symbol;
        
        /**
         * Asset name
         */
        private String name;
        
        /**
         * Asset type (crypto, stock, etc.)
         */
        private String type;
        
        /**
         * Quantity owned
         */
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        
        /**
         * Current price
         */
        private BigDecimal price;
        
        /**
         * Total value (price * quantity)
         */
        private BigDecimal value;
        
        /**
         * Percentage allocation in portfolio
         */
        private BigDecimal allocation;
        
        /**
         * Daily change in value
         */
        private BigDecimal dailyChange;
        
        /**
         * Daily change percentage
         */
        private BigDecimal dailyChangePercent;
    }
    
    /**
     * DTO for exchange data
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExchangeDataDTO {
        /**
         * Exchange name
         */
        @NotBlank(message = "Exchange name is required")
        private String name;
        
        /**
         * Total value in this exchange
         */
        private BigDecimal value;
        
        /**
         * Exchange-specific metadata
         */
        private Map<String, String> metadata;
    }
}
