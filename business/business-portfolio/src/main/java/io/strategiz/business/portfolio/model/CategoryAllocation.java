package io.strategiz.business.portfolio.model;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.portfolio.exception.PortfolioErrorDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents the allocation of portfolio assets within a specific category.
 * Used for aggregated portfolio views and visualization (e.g., pie charts).
 */
public class CategoryAllocation {

    private final AssetCategory category;
    private final BigDecimal value;
    private final BigDecimal percentage;
    private final BigDecimal costBasis;
    private final BigDecimal profitLoss;
    private final BigDecimal profitLossPercent;
    private final int assetCount;

    private CategoryAllocation(Builder builder) {
        this.category = builder.category;
        this.value = builder.value;
        this.percentage = builder.percentage;
        this.costBasis = builder.costBasis;
        this.profitLoss = builder.profitLoss;
        this.profitLossPercent = builder.profitLossPercent;
        this.assetCount = builder.assetCount;
    }

    // Getters
    public AssetCategory getCategory() {
        return category;
    }

    public String getCategoryName() {
        return category.getDisplayName();
    }

    public String getColor() {
        return category.getColor();
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public BigDecimal getCostBasis() {
        return costBasis;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public BigDecimal getProfitLossPercent() {
        return profitLossPercent;
    }

    public int getAssetCount() {
        return assetCount;
    }

    public boolean isPositive() {
        return profitLoss != null && profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return profitLoss != null && profitLoss.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Builder pattern for CategoryAllocation.
     */
    public static class Builder {
        private AssetCategory category;
        private BigDecimal value = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
        private BigDecimal costBasis = BigDecimal.ZERO;
        private BigDecimal profitLoss = BigDecimal.ZERO;
        private BigDecimal profitLossPercent = BigDecimal.ZERO;
        private int assetCount = 0;

        public Builder category(AssetCategory category) {
            this.category = category;
            return this;
        }

        public Builder value(BigDecimal value) {
            this.value = value != null ? value : BigDecimal.ZERO;
            return this;
        }

        public Builder percentage(BigDecimal percentage) {
            this.percentage = percentage != null ? percentage : BigDecimal.ZERO;
            return this;
        }

        public Builder costBasis(BigDecimal costBasis) {
            this.costBasis = costBasis != null ? costBasis : BigDecimal.ZERO;
            return this;
        }

        public Builder profitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss != null ? profitLoss : BigDecimal.ZERO;
            return this;
        }

        public Builder profitLossPercent(BigDecimal profitLossPercent) {
            this.profitLossPercent = profitLossPercent != null ? profitLossPercent : BigDecimal.ZERO;
            return this;
        }

        public Builder assetCount(int assetCount) {
            this.assetCount = assetCount;
            return this;
        }

        /**
         * Calculates percentage based on total portfolio value.
         */
        public Builder calculatePercentage(BigDecimal totalPortfolioValue) {
            if (totalPortfolioValue != null && totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0) {
                this.percentage = this.value
                        .divide(totalPortfolioValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            return this;
        }

        /**
         * Calculates profit/loss percentage based on cost basis.
         */
        public Builder calculateProfitLossPercent() {
            if (this.costBasis != null && this.costBasis.compareTo(BigDecimal.ZERO) > 0) {
                this.profitLossPercent = this.profitLoss
                        .divide(this.costBasis, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            return this;
        }

        public CategoryAllocation build() {
            if (category == null) {
                throw new StrategizException(PortfolioErrorDetails.INVALID_ARGUMENT,
                    "CategoryAllocation", "Category is required");
            }
            return new CategoryAllocation(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f (%.2f%%) - %d assets",
                category.getDisplayName(),
                value,
                percentage,
                assetCount);
    }
}
