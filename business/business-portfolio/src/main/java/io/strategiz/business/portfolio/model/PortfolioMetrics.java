package io.strategiz.business.portfolio.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.io.Serializable;

/**
 * Business model for portfolio metrics.
 */
@Data
public class PortfolioMetrics implements Serializable {
    
    /**
     * User ID associated with these metrics
     */
    private String userId;
    
    /**
     * Total portfolio value
     */
    private BigDecimal totalValue;
    
    /**
     * Performance metrics over different time periods
     */
    private Map<String, BigDecimal> performance;
    
    /**
     * Asset allocation by category
     */
    private Map<String, BigDecimal> allocation;
    
    /**
     * Risk metrics
     */
    private Map<String, BigDecimal> risk;
}
