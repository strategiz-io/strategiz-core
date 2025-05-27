package io.strategiz.business.portfolio.model;

import io.americanexpress.synapse.framework.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Business model for portfolio metrics following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PortfolioMetrics extends BaseModel {
    
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
