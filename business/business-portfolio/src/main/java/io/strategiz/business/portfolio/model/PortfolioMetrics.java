package io.strategiz.business.portfolio.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.io.Serializable;

/**
 * Business model for portfolio metrics.
 */
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

    // Default constructor
    public PortfolioMetrics() {}

    // All-args constructor
    public PortfolioMetrics(String userId, BigDecimal totalValue, Map<String, BigDecimal> performance,
                           Map<String, BigDecimal> allocation, Map<String, BigDecimal> risk) {
        this.userId = userId;
        this.totalValue = totalValue;
        this.performance = performance;
        this.allocation = allocation;
        this.risk = risk;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public Map<String, BigDecimal> getPerformance() {
        return performance;
    }

    public Map<String, BigDecimal> getAllocation() {
        return allocation;
    }

    public Map<String, BigDecimal> getRisk() {
        return risk;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public void setPerformance(Map<String, BigDecimal> performance) {
        this.performance = performance;
    }

    public void setAllocation(Map<String, BigDecimal> allocation) {
        this.allocation = allocation;
    }

    public void setRisk(Map<String, BigDecimal> risk) {
        this.risk = risk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioMetrics that = (PortfolioMetrics) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(totalValue, that.totalValue) &&
               Objects.equals(performance, that.performance) &&
               Objects.equals(allocation, that.allocation) &&
               Objects.equals(risk, that.risk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, totalValue, performance, allocation, risk);
    }

    @Override
    public String toString() {
        return "PortfolioMetrics{" +
               "userId='" + userId + '\'' +
               ", totalValue=" + totalValue +
               ", performance=" + performance +
               ", allocation=" + allocation +
               ", risk=" + risk +
               '}';
    }
}
