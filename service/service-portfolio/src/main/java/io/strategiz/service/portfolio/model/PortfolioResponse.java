package io.strategiz.service.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service model for portfolio data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    
    private String userId;
    private String provider;
    private BigDecimal totalValue;
    private BigDecimal cashBalance;
    private List<AssetHolding> assets;
    private Map<String, String> metadata;
    private boolean success;
    private String errorMessage;
    
    /**
     * Represents a holding of a financial asset in a portfolio.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetHolding {
        private String symbol;
        private String name;
        private String type; // crypto, stock, etc.
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal value;
        private String currency;
        private BigDecimal costBasis;
        private Map<String, Object> additionalData;
    }
}
