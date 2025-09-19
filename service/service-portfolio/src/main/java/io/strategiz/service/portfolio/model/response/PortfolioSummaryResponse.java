package io.strategiz.service.portfolio.model.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight response for dashboard summary widget.
 * Contains only essential data for quick loading.
 */
public class PortfolioSummaryResponse {
    
    private BigDecimal totalValue;
    private BigDecimal dayChange;
    private BigDecimal dayChangePercent;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private int connectedProviders;
    private int totalPositions;
    private List<PortfolioPositionResponse> topPositions; // Top 5-10 by value
    private long lastUpdated;
    
    // Constructors
    public PortfolioSummaryResponse() {}
    
    // Getters and Setters
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public BigDecimal getDayChange() {
        return dayChange;
    }
    
    public void setDayChange(BigDecimal dayChange) {
        this.dayChange = dayChange;
    }
    
    public BigDecimal getDayChangePercent() {
        return dayChangePercent;
    }
    
    public void setDayChangePercent(BigDecimal dayChangePercent) {
        this.dayChangePercent = dayChangePercent;
    }
    
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }
    
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }
    
    public BigDecimal getTotalProfitLossPercent() {
        return totalProfitLossPercent;
    }
    
    public void setTotalProfitLossPercent(BigDecimal totalProfitLossPercent) {
        this.totalProfitLossPercent = totalProfitLossPercent;
    }
    
    public int getConnectedProviders() {
        return connectedProviders;
    }
    
    public void setConnectedProviders(int connectedProviders) {
        this.connectedProviders = connectedProviders;
    }
    
    public int getTotalPositions() {
        return totalPositions;
    }
    
    public void setTotalPositions(int totalPositions) {
        this.totalPositions = totalPositions;
    }
    
    public List<PortfolioPositionResponse> getTopPositions() {
        return topPositions;
    }
    
    public void setTopPositions(List<PortfolioPositionResponse> topPositions) {
        this.topPositions = topPositions;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}