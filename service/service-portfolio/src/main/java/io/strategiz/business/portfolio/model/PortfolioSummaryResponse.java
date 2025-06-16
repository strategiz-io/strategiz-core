package io.strategiz.business.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Business model for portfolio summary data.
 */
public class PortfolioSummaryResponse {
    private String userId;
    private BigDecimal totalValue;
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    private BigDecimal weeklyChange;
    private BigDecimal weeklyChangePercent;
    private BigDecimal monthlyChange;
    private BigDecimal monthlyChangePercent;
    private BigDecimal yearlyChange;
    private BigDecimal yearlyChangePercent;
    private List<Asset> assets;
    private LocalDateTime lastUpdated;
    private boolean hasExchangeConnections;
    private String statusMessage;
    private boolean needsApiKeyConfiguration;
    private Map<String, ExchangeData> exchanges;
    private PortfolioMetrics portfolioMetrics;
    
    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    
    public BigDecimal getDailyChange() { return dailyChange; }
    public void setDailyChange(BigDecimal dailyChange) { this.dailyChange = dailyChange; }
    
    public BigDecimal getDailyChangePercent() { return dailyChangePercent; }
    public void setDailyChangePercent(BigDecimal dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; }
    
    public BigDecimal getWeeklyChange() { return weeklyChange; }
    public void setWeeklyChange(BigDecimal weeklyChange) { this.weeklyChange = weeklyChange; }
    
    public BigDecimal getWeeklyChangePercent() { return weeklyChangePercent; }
    public void setWeeklyChangePercent(BigDecimal weeklyChangePercent) { this.weeklyChangePercent = weeklyChangePercent; }
    
    public BigDecimal getMonthlyChange() { return monthlyChange; }
    public void setMonthlyChange(BigDecimal monthlyChange) { this.monthlyChange = monthlyChange; }
    
    public BigDecimal getMonthlyChangePercent() { return monthlyChangePercent; }
    public void setMonthlyChangePercent(BigDecimal monthlyChangePercent) { this.monthlyChangePercent = monthlyChangePercent; }
    
    public BigDecimal getYearlyChange() { return yearlyChange; }
    public void setYearlyChange(BigDecimal yearlyChange) { this.yearlyChange = yearlyChange; }
    
    public BigDecimal getYearlyChangePercent() { return yearlyChangePercent; }
    public void setYearlyChangePercent(BigDecimal yearlyChangePercent) { this.yearlyChangePercent = yearlyChangePercent; }
    
    public List<Asset> getAssets() { return assets; }
    public void setAssets(List<Asset> assets) { this.assets = assets; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public boolean isHasExchangeConnections() { return hasExchangeConnections; }
    public void setHasExchangeConnections(boolean hasExchangeConnections) { this.hasExchangeConnections = hasExchangeConnections; }
    
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    
    public boolean isNeedsApiKeyConfiguration() { return needsApiKeyConfiguration; }
    public void setNeedsApiKeyConfiguration(boolean needsApiKeyConfiguration) { this.needsApiKeyConfiguration = needsApiKeyConfiguration; }
    
    public Map<String, ExchangeData> getExchanges() { return exchanges; }
    public void setExchanges(Map<String, ExchangeData> exchanges) { this.exchanges = exchanges; }
    
    public PortfolioMetrics getPortfolioMetrics() { return portfolioMetrics; }
    public void setPortfolioMetrics(PortfolioMetrics portfolioMetrics) { this.portfolioMetrics = portfolioMetrics; }
}
