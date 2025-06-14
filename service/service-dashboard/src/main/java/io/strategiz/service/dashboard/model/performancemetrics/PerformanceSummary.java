package io.strategiz.service.dashboard.model.performancemetrics;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Performance summary metrics
 */
public class PerformanceSummary {
    /**
     * Total unrealized profit/loss in USD
     */
    private BigDecimal totalProfitLoss;
    
    /**
     * Total profit/loss percentage
     */
    private BigDecimal totalProfitLossPercentage;
    
    /**
     * 24-hour change in USD
     */
    private BigDecimal dailyChange;
    
    /**
     * 24-hour change percentage
     */
    private BigDecimal dailyChangePercentage;
    
    /**
     * 7-day change in USD
     */
    private BigDecimal weeklyChange;
    
    /**
     * 7-day change percentage
     */
    private BigDecimal weeklyChangePercentage;
    
    /**
     * 30-day change in USD
     */
    private BigDecimal monthlyChange;
    
    /**
     * 30-day change percentage
     */
    private BigDecimal monthlyChangePercentage;
    
    /**
     * Year-to-date change in USD
     */
    private BigDecimal ytdChange;
    
    /**
     * Year-to-date change percentage
     */
    private BigDecimal ytdChangePercentage;
    
    /**
     * Whether the portfolio is currently profitable
     */
    private boolean profitable;

    // Constructors
    public PerformanceSummary() {}

    public PerformanceSummary(BigDecimal totalProfitLoss, BigDecimal totalProfitLossPercentage,
                            BigDecimal dailyChange, BigDecimal dailyChangePercentage,
                            BigDecimal weeklyChange, BigDecimal weeklyChangePercentage,
                            BigDecimal monthlyChange, BigDecimal monthlyChangePercentage,
                            BigDecimal ytdChange, BigDecimal ytdChangePercentage, boolean profitable) {
        this.totalProfitLoss = totalProfitLoss;
        this.totalProfitLossPercentage = totalProfitLossPercentage;
        this.dailyChange = dailyChange;
        this.dailyChangePercentage = dailyChangePercentage;
        this.weeklyChange = weeklyChange;
        this.weeklyChangePercentage = weeklyChangePercentage;
        this.monthlyChange = monthlyChange;
        this.monthlyChangePercentage = monthlyChangePercentage;
        this.ytdChange = ytdChange;
        this.ytdChangePercentage = ytdChangePercentage;
        this.profitable = profitable;
    }

    // Getters and Setters
    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }

    public BigDecimal getTotalProfitLossPercentage() {
        return totalProfitLossPercentage;
    }

    public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) {
        this.totalProfitLossPercentage = totalProfitLossPercentage;
    }

    public BigDecimal getDailyChange() {
        return dailyChange;
    }

    public void setDailyChange(BigDecimal dailyChange) {
        this.dailyChange = dailyChange;
    }

    public BigDecimal getDailyChangePercentage() {
        return dailyChangePercentage;
    }

    public void setDailyChangePercentage(BigDecimal dailyChangePercentage) {
        this.dailyChangePercentage = dailyChangePercentage;
    }

    public BigDecimal getWeeklyChange() {
        return weeklyChange;
    }

    public void setWeeklyChange(BigDecimal weeklyChange) {
        this.weeklyChange = weeklyChange;
    }

    public BigDecimal getWeeklyChangePercentage() {
        return weeklyChangePercentage;
    }

    public void setWeeklyChangePercentage(BigDecimal weeklyChangePercentage) {
        this.weeklyChangePercentage = weeklyChangePercentage;
    }

    public BigDecimal getMonthlyChange() {
        return monthlyChange;
    }

    public void setMonthlyChange(BigDecimal monthlyChange) {
        this.monthlyChange = monthlyChange;
    }

    public BigDecimal getMonthlyChangePercentage() {
        return monthlyChangePercentage;
    }

    public void setMonthlyChangePercentage(BigDecimal monthlyChangePercentage) {
        this.monthlyChangePercentage = monthlyChangePercentage;
    }

    public BigDecimal getYtdChange() {
        return ytdChange;
    }

    public void setYtdChange(BigDecimal ytdChange) {
        this.ytdChange = ytdChange;
    }

    public BigDecimal getYtdChangePercentage() {
        return ytdChangePercentage;
    }

    public void setYtdChangePercentage(BigDecimal ytdChangePercentage) {
        this.ytdChangePercentage = ytdChangePercentage;
    }

    public boolean isProfitable() {
        return profitable;
    }

    public void setProfitable(boolean profitable) {
        this.profitable = profitable;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerformanceSummary that = (PerformanceSummary) o;
        return profitable == that.profitable &&
               Objects.equals(totalProfitLoss, that.totalProfitLoss) &&
               Objects.equals(totalProfitLossPercentage, that.totalProfitLossPercentage) &&
               Objects.equals(dailyChange, that.dailyChange) &&
               Objects.equals(dailyChangePercentage, that.dailyChangePercentage) &&
               Objects.equals(weeklyChange, that.weeklyChange) &&
               Objects.equals(weeklyChangePercentage, that.weeklyChangePercentage) &&
               Objects.equals(monthlyChange, that.monthlyChange) &&
               Objects.equals(monthlyChangePercentage, that.monthlyChangePercentage) &&
               Objects.equals(ytdChange, that.ytdChange) &&
               Objects.equals(ytdChangePercentage, that.ytdChangePercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalProfitLoss, totalProfitLossPercentage, dailyChange, dailyChangePercentage,
                          weeklyChange, weeklyChangePercentage, monthlyChange, monthlyChangePercentage,
                          ytdChange, ytdChangePercentage, profitable);
    }

    @Override
    public String toString() {
        return "PerformanceSummary{" +
               "totalProfitLoss=" + totalProfitLoss +
               ", totalProfitLossPercentage=" + totalProfitLossPercentage +
               ", dailyChange=" + dailyChange +
               ", dailyChangePercentage=" + dailyChangePercentage +
               ", weeklyChange=" + weeklyChange +
               ", weeklyChangePercentage=" + weeklyChangePercentage +
               ", monthlyChange=" + monthlyChange +
               ", monthlyChangePercentage=" + monthlyChangePercentage +
               ", ytdChange=" + ytdChange +
               ", ytdChangePercentage=" + ytdChangePercentage +
               ", profitable=" + profitable +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal totalProfitLoss;
        private BigDecimal totalProfitLossPercentage;
        private BigDecimal dailyChange;
        private BigDecimal dailyChangePercentage;
        private BigDecimal weeklyChange;
        private BigDecimal weeklyChangePercentage;
        private BigDecimal monthlyChange;
        private BigDecimal monthlyChangePercentage;
        private BigDecimal ytdChange;
        private BigDecimal ytdChangePercentage;
        private boolean profitable;

        public Builder withTotalProfitLoss(BigDecimal totalProfitLoss) {
            this.totalProfitLoss = totalProfitLoss;
            return this;
        }

        public Builder withTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) {
            this.totalProfitLossPercentage = totalProfitLossPercentage;
            return this;
        }

        public Builder withDailyChange(BigDecimal dailyChange) {
            this.dailyChange = dailyChange;
            return this;
        }

        public Builder withDailyChangePercentage(BigDecimal dailyChangePercentage) {
            this.dailyChangePercentage = dailyChangePercentage;
            return this;
        }

        public Builder withWeeklyChange(BigDecimal weeklyChange) {
            this.weeklyChange = weeklyChange;
            return this;
        }

        public Builder withWeeklyChangePercentage(BigDecimal weeklyChangePercentage) {
            this.weeklyChangePercentage = weeklyChangePercentage;
            return this;
        }

        public Builder withMonthlyChange(BigDecimal monthlyChange) {
            this.monthlyChange = monthlyChange;
            return this;
        }

        public Builder withMonthlyChangePercentage(BigDecimal monthlyChangePercentage) {
            this.monthlyChangePercentage = monthlyChangePercentage;
            return this;
        }

        public Builder withYtdChange(BigDecimal ytdChange) {
            this.ytdChange = ytdChange;
            return this;
        }

        public Builder withYtdChangePercentage(BigDecimal ytdChangePercentage) {
            this.ytdChangePercentage = ytdChangePercentage;
            return this;
        }

        public Builder withProfitable(boolean profitable) {
            this.profitable = profitable;
            return this;
        }

        public PerformanceSummary build() {
            return new PerformanceSummary(totalProfitLoss, totalProfitLossPercentage, dailyChange, dailyChangePercentage,
                                        weeklyChange, weeklyChangePercentage, monthlyChange, monthlyChangePercentage,
                                        ytdChange, ytdChangePercentage, profitable);
        }
    }
}
