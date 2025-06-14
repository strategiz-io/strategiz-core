package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response model for portfolio summary data.
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
    
    /**
     * Default constructor
     */
    public PortfolioSummaryResponse() {
    }
    
    /**
     * All-args constructor
     */
    public PortfolioSummaryResponse(String userId, BigDecimal totalValue, BigDecimal dailyChange,
                                   BigDecimal dailyChangePercent, BigDecimal weeklyChange,
                                   BigDecimal weeklyChangePercent, BigDecimal monthlyChange,
                                   BigDecimal monthlyChangePercent, BigDecimal yearlyChange,
                                   BigDecimal yearlyChangePercent, List<Asset> assets,
                                   LocalDateTime lastUpdated, boolean hasExchangeConnections,
                                   String statusMessage, boolean needsApiKeyConfiguration,
                                   Map<String, ExchangeData> exchanges, PortfolioMetrics portfolioMetrics) {
        this.userId = userId;
        this.totalValue = totalValue;
        this.dailyChange = dailyChange;
        this.dailyChangePercent = dailyChangePercent;
        this.weeklyChange = weeklyChange;
        this.weeklyChangePercent = weeklyChangePercent;
        this.monthlyChange = monthlyChange;
        this.monthlyChangePercent = monthlyChangePercent;
        this.yearlyChange = yearlyChange;
        this.yearlyChangePercent = yearlyChangePercent;
        this.assets = assets;
        this.lastUpdated = lastUpdated;
        this.hasExchangeConnections = hasExchangeConnections;
        this.statusMessage = statusMessage;
        this.needsApiKeyConfiguration = needsApiKeyConfiguration;
        this.exchanges = exchanges;
        this.portfolioMetrics = portfolioMetrics;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public BigDecimal getTotalValue() {
        return totalValue;
    }
    
    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }
    
    public BigDecimal getDailyChange() {
        return dailyChange;
    }
    
    public void setDailyChange(BigDecimal dailyChange) {
        this.dailyChange = dailyChange;
    }
    
    public BigDecimal getDailyChangePercent() {
        return dailyChangePercent;
    }
    
    public void setDailyChangePercent(BigDecimal dailyChangePercent) {
        this.dailyChangePercent = dailyChangePercent;
    }
    
    public BigDecimal getWeeklyChange() {
        return weeklyChange;
    }
    
    public void setWeeklyChange(BigDecimal weeklyChange) {
        this.weeklyChange = weeklyChange;
    }
    
    public BigDecimal getWeeklyChangePercent() {
        return weeklyChangePercent;
    }
    
    public void setWeeklyChangePercent(BigDecimal weeklyChangePercent) {
        this.weeklyChangePercent = weeklyChangePercent;
    }
    
    public BigDecimal getMonthlyChange() {
        return monthlyChange;
    }
    
    public void setMonthlyChange(BigDecimal monthlyChange) {
        this.monthlyChange = monthlyChange;
    }
    
    public BigDecimal getMonthlyChangePercent() {
        return monthlyChangePercent;
    }
    
    public void setMonthlyChangePercent(BigDecimal monthlyChangePercent) {
        this.monthlyChangePercent = monthlyChangePercent;
    }
    
    public BigDecimal getYearlyChange() {
        return yearlyChange;
    }
    
    public void setYearlyChange(BigDecimal yearlyChange) {
        this.yearlyChange = yearlyChange;
    }
    
    public BigDecimal getYearlyChangePercent() {
        return yearlyChangePercent;
    }
    
    public void setYearlyChangePercent(BigDecimal yearlyChangePercent) {
        this.yearlyChangePercent = yearlyChangePercent;
    }
    
    public List<Asset> getAssets() {
        return assets;
    }
    
    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public boolean isHasExchangeConnections() {
        return hasExchangeConnections;
    }
    
    public void setHasExchangeConnections(boolean hasExchangeConnections) {
        this.hasExchangeConnections = hasExchangeConnections;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public boolean isNeedsApiKeyConfiguration() {
        return needsApiKeyConfiguration;
    }
    
    public void setNeedsApiKeyConfiguration(boolean needsApiKeyConfiguration) {
        this.needsApiKeyConfiguration = needsApiKeyConfiguration;
    }
    
    public Map<String, ExchangeData> getExchanges() {
        return exchanges;
    }
    
    public void setExchanges(Map<String, ExchangeData> exchanges) {
        this.exchanges = exchanges;
    }
    
    public PortfolioMetrics getPortfolioMetrics() {
        return portfolioMetrics;
    }
    
    public void setPortfolioMetrics(PortfolioMetrics portfolioMetrics) {
        this.portfolioMetrics = portfolioMetrics;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortfolioSummaryResponse that = (PortfolioSummaryResponse) o;
        return hasExchangeConnections == that.hasExchangeConnections &&
                needsApiKeyConfiguration == that.needsApiKeyConfiguration &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(totalValue, that.totalValue) &&
                Objects.equals(dailyChange, that.dailyChange) &&
                Objects.equals(dailyChangePercent, that.dailyChangePercent) &&
                Objects.equals(weeklyChange, that.weeklyChange) &&
                Objects.equals(weeklyChangePercent, that.weeklyChangePercent) &&
                Objects.equals(monthlyChange, that.monthlyChange) &&
                Objects.equals(monthlyChangePercent, that.monthlyChangePercent) &&
                Objects.equals(yearlyChange, that.yearlyChange) &&
                Objects.equals(yearlyChangePercent, that.yearlyChangePercent) &&
                Objects.equals(assets, that.assets) &&
                Objects.equals(lastUpdated, that.lastUpdated) &&
                Objects.equals(statusMessage, that.statusMessage) &&
                Objects.equals(exchanges, that.exchanges) &&
                Objects.equals(portfolioMetrics, that.portfolioMetrics);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, totalValue, dailyChange, dailyChangePercent, weeklyChange, weeklyChangePercent, monthlyChange, monthlyChangePercent, yearlyChange, yearlyChangePercent, assets, lastUpdated, hasExchangeConnections, statusMessage, needsApiKeyConfiguration, exchanges, portfolioMetrics);
    }
    
    @Override
    public String toString() {
        return "PortfolioSummaryResponse{" +
                "userId='" + userId + '\'' +
                ", totalValue=" + totalValue +
                ", dailyChange=" + dailyChange +
                ", dailyChangePercent=" + dailyChangePercent +
                ", weeklyChange=" + weeklyChange +
                ", weeklyChangePercent=" + weeklyChangePercent +
                ", monthlyChange=" + monthlyChange +
                ", monthlyChangePercent=" + monthlyChangePercent +
                ", yearlyChange=" + yearlyChange +
                ", yearlyChangePercent=" + yearlyChangePercent +
                ", assets=" + assets +
                ", lastUpdated=" + lastUpdated +
                ", hasExchangeConnections=" + hasExchangeConnections +
                ", statusMessage='" + statusMessage + '\'' +
                ", needsApiKeyConfiguration=" + needsApiKeyConfiguration +
                ", exchanges=" + exchanges +
                ", portfolioMetrics=" + portfolioMetrics +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
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
        
        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder withTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }
        
        public Builder withDailyChange(BigDecimal dailyChange) {
            this.dailyChange = dailyChange;
            return this;
        }
        
        public Builder withDailyChangePercent(BigDecimal dailyChangePercent) {
            this.dailyChangePercent = dailyChangePercent;
            return this;
        }
        
        public Builder withWeeklyChange(BigDecimal weeklyChange) {
            this.weeklyChange = weeklyChange;
            return this;
        }
        
        public Builder withWeeklyChangePercent(BigDecimal weeklyChangePercent) {
            this.weeklyChangePercent = weeklyChangePercent;
            return this;
        }
        
        public Builder withMonthlyChange(BigDecimal monthlyChange) {
            this.monthlyChange = monthlyChange;
            return this;
        }
        
        public Builder withMonthlyChangePercent(BigDecimal monthlyChangePercent) {
            this.monthlyChangePercent = monthlyChangePercent;
            return this;
        }
        
        public Builder withYearlyChange(BigDecimal yearlyChange) {
            this.yearlyChange = yearlyChange;
            return this;
        }
        
        public Builder withYearlyChangePercent(BigDecimal yearlyChangePercent) {
            this.yearlyChangePercent = yearlyChangePercent;
            return this;
        }
        
        public Builder withAssets(List<Asset> assets) {
            this.assets = assets;
            return this;
        }
        
        public Builder withLastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        public Builder withHasExchangeConnections(boolean hasExchangeConnections) {
            this.hasExchangeConnections = hasExchangeConnections;
            return this;
        }
        
        public Builder withStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }
        
        public Builder withNeedsApiKeyConfiguration(boolean needsApiKeyConfiguration) {
            this.needsApiKeyConfiguration = needsApiKeyConfiguration;
            return this;
        }
        
        public Builder withExchanges(Map<String, ExchangeData> exchanges) {
            this.exchanges = exchanges;
            return this;
        }
        
        public Builder withPortfolioMetrics(PortfolioMetrics portfolioMetrics) {
            this.portfolioMetrics = portfolioMetrics;
            return this;
        }
        
        public PortfolioSummaryResponse build() {
            return new PortfolioSummaryResponse(userId, totalValue, dailyChange, dailyChangePercent, weeklyChange, weeklyChangePercent, monthlyChange, monthlyChangePercent, yearlyChange, yearlyChangePercent, assets, lastUpdated, hasExchangeConnections, statusMessage, needsApiKeyConfiguration, exchanges, portfolioMetrics);
        }
    }
}
