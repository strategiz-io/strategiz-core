package io.strategiz.data.provider.entity;

import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import com.google.cloud.firestore.annotation.DocumentId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a point-in-time snapshot of a user's portfolio.
 * Stored daily for performance tracking and historical analysis.
 *
 * Firestore path: users/{userId}/portfolio_history/{snapshotId}
 */
@Collection("portfolio_history")
public class PortfolioHistoryEntity extends BaseEntity {

    @DocumentId
    private String id;  // Format: YYYY-MM-DD (e.g., "2024-01-15")

    private String userId;
    private LocalDate snapshotDate;
    private Long snapshotTimestamp;  // Unix timestamp in milliseconds

    // Portfolio totals
    private BigDecimal totalValue;
    private BigDecimal totalCostBasis;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;
    private BigDecimal cashBalance;

    // Performance metrics
    private BigDecimal dailyChange;
    private BigDecimal dailyChangePercent;
    private BigDecimal weeklyChange;
    private BigDecimal weeklyChangePercent;
    private BigDecimal monthlyChange;
    private BigDecimal monthlyChangePercent;
    private BigDecimal yearlyChange;
    private BigDecimal yearlyChangePercent;

    // Category breakdown
    private List<CategorySnapshot> categoryAllocations;

    // Provider breakdown
    private List<ProviderSnapshot> providerBreakdown;

    // Top holdings at this point in time
    private List<HoldingSnapshot> topHoldings;

    // Metadata
    private Integer totalAssetCount;
    private Integer connectedProvidersCount;

    // Nested class for category allocation snapshots
    public static class CategorySnapshot {
        private String category;
        private String categoryName;
        private BigDecimal value;
        private BigDecimal percentage;
        private BigDecimal costBasis;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;
        private Integer assetCount;

        // Getters and Setters
        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }

        public BigDecimal getCostBasis() {
            return costBasis;
        }

        public void setCostBasis(BigDecimal costBasis) {
            this.costBasis = costBasis;
        }

        public BigDecimal getProfitLoss() {
            return profitLoss;
        }

        public void setProfitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss;
        }

        public BigDecimal getProfitLossPercent() {
            return profitLossPercent;
        }

        public void setProfitLossPercent(BigDecimal profitLossPercent) {
            this.profitLossPercent = profitLossPercent;
        }

        public Integer getAssetCount() {
            return assetCount;
        }

        public void setAssetCount(Integer assetCount) {
            this.assetCount = assetCount;
        }
    }

    // Nested class for provider breakdown
    public static class ProviderSnapshot {
        private String providerId;
        private String providerName;
        private BigDecimal value;
        private BigDecimal percentage;
        private Integer positionCount;

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

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }

        public Integer getPositionCount() {
            return positionCount;
        }

        public void setPositionCount(Integer positionCount) {
            this.positionCount = positionCount;
        }
    }

    // Nested class for holding snapshots
    public static class HoldingSnapshot {
        private String symbol;
        private String name;
        private String assetType;
        private BigDecimal quantity;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal costBasis;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercent;

        // Getters and Setters
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAssetType() {
            return assetType;
        }

        public void setAssetType(String assetType) {
            this.assetType = assetType;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
        }

        public BigDecimal getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(BigDecimal currentValue) {
            this.currentValue = currentValue;
        }

        public BigDecimal getCostBasis() {
            return costBasis;
        }

        public void setCostBasis(BigDecimal costBasis) {
            this.costBasis = costBasis;
        }

        public BigDecimal getProfitLoss() {
            return profitLoss;
        }

        public void setProfitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss;
        }

        public BigDecimal getProfitLossPercent() {
            return profitLossPercent;
        }

        public void setProfitLossPercent(BigDecimal profitLossPercent) {
            this.profitLossPercent = profitLossPercent;
        }
    }

    // Main entity getters and setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public Long getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    public void setSnapshotTimestamp(Long snapshotTimestamp) {
        this.snapshotTimestamp = snapshotTimestamp;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getTotalCostBasis() {
        return totalCostBasis;
    }

    public void setTotalCostBasis(BigDecimal totalCostBasis) {
        this.totalCostBasis = totalCostBasis;
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

    public List<CategorySnapshot> getCategoryAllocations() {
        if (categoryAllocations == null) {
            categoryAllocations = new ArrayList<>();
        }
        return categoryAllocations;
    }

    public void setCategoryAllocations(List<CategorySnapshot> categoryAllocations) {
        this.categoryAllocations = categoryAllocations;
    }

    public List<ProviderSnapshot> getProviderBreakdown() {
        if (providerBreakdown == null) {
            providerBreakdown = new ArrayList<>();
        }
        return providerBreakdown;
    }

    public void setProviderBreakdown(List<ProviderSnapshot> providerBreakdown) {
        this.providerBreakdown = providerBreakdown;
    }

    public List<HoldingSnapshot> getTopHoldings() {
        if (topHoldings == null) {
            topHoldings = new ArrayList<>();
        }
        return topHoldings;
    }

    public void setTopHoldings(List<HoldingSnapshot> topHoldings) {
        this.topHoldings = topHoldings;
    }

    public Integer getTotalAssetCount() {
        return totalAssetCount;
    }

    public void setTotalAssetCount(Integer totalAssetCount) {
        this.totalAssetCount = totalAssetCount;
    }

    public Integer getConnectedProvidersCount() {
        return connectedProvidersCount;
    }

    public void setConnectedProvidersCount(Integer connectedProvidersCount) {
        this.connectedProvidersCount = connectedProvidersCount;
    }

    @Override
    public String toString() {
        return String.format("PortfolioHistory[date=%s, userId=%s, totalValue=%s, assets=%d]",
                snapshotDate, userId, totalValue, totalAssetCount);
    }
}
