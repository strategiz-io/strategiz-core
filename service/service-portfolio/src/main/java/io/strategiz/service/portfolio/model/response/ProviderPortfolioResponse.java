package io.strategiz.service.portfolio.model.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response for a single provider's portfolio data.
 * Used when querying /providers/{providerId}
 */
public class ProviderPortfolioResponse {
    
    private String providerId;
    private String providerName;
    private String providerType;  // crypto, equity, forex
    private String providerCategory;  // exchange, brokerage
    private boolean connected;
    private BigDecimal totalValue;
    private BigDecimal dayChange;
    private BigDecimal dayChangePercent;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private BigDecimal cashBalance;
    private BigDecimal buyingPower;
    private BigDecimal marginBalance;
    private BigDecimal liquidationValue;
    private List<PortfolioPositionResponse> positions;
    private Map<String, BigDecimal> balances; // Raw balance data from provider
    private String syncStatus;
    private String errorMessage;
    private long lastSynced;
    
    // Constructors
    public ProviderPortfolioResponse() {}
    
    // Getters and Setters
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderCategory() {
        return providerCategory;
    }

    public void setProviderCategory(String providerCategory) {
        this.providerCategory = providerCategory;
    }

    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
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
    
    public BigDecimal getCashBalance() {
        return cashBalance;
    }
    
    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getBuyingPower() {
        return buyingPower;
    }

    public void setBuyingPower(BigDecimal buyingPower) {
        this.buyingPower = buyingPower;
    }

    public BigDecimal getMarginBalance() {
        return marginBalance;
    }

    public void setMarginBalance(BigDecimal marginBalance) {
        this.marginBalance = marginBalance;
    }

    public BigDecimal getLiquidationValue() {
        return liquidationValue;
    }

    public void setLiquidationValue(BigDecimal liquidationValue) {
        this.liquidationValue = liquidationValue;
    }

    public List<PortfolioPositionResponse> getPositions() {
        return positions;
    }
    
    public void setPositions(List<PortfolioPositionResponse> positions) {
        this.positions = positions;
    }
    
    public Map<String, BigDecimal> getBalances() {
        return balances;
    }
    
    public void setBalances(Map<String, BigDecimal> balances) {
        this.balances = balances;
    }
    
    public String getSyncStatus() {
        return syncStatus;
    }
    
    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public long getLastSynced() {
        return lastSynced;
    }
    
    public void setLastSynced(long lastSynced) {
        this.lastSynced = lastSynced;
    }
}