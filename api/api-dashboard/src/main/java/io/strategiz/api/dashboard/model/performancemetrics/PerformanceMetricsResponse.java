package io.strategiz.api.dashboard.model.performancemetrics;

import io.strategiz.service.base.model.BaseServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Response model for portfolio performance metrics following Synapse patterns.
 */
public class PerformanceMetricsResponse extends BaseServiceResponse {
    
    /**
     * Historical portfolio value data points
     */
    private List<PortfolioValueDataPoint> historicalValues;
    
    /**
     * Overall portfolio performance summary
     */
    private PerformanceSummary summary;
    
    /**
     * Gets historical portfolio value data points
     *
     * @return List of historical portfolio value data points
     */
    public List<PortfolioValueDataPoint> getHistoricalValues() {
        return historicalValues;
    }

    /**
     * Sets historical portfolio value data points
     *
     * @param historicalValues List of historical portfolio value data points
     */
    public void setHistoricalValues(List<PortfolioValueDataPoint> historicalValues) {
        this.historicalValues = historicalValues;
    }

    /**
     * Gets overall portfolio performance summary
     *
     * @return Portfolio performance summary
     */
    public PerformanceSummary getSummary() {
        return summary;
    }

    /**
     * Sets overall portfolio performance summary
     *
     * @param summary Portfolio performance summary
     */
    public void setSummary(PerformanceSummary summary) {
        this.summary = summary;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PerformanceMetricsResponse that = (PerformanceMetricsResponse) o;
        return Objects.equals(historicalValues, that.historicalValues) && 
               Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), historicalValues, summary);
    }

    @Override
    public String toString() {
        return "PerformanceMetricsResponse{" +
                "historicalValues=" + historicalValues +
                ", summary=" + summary +
                "}";
    }
    
    /**
     * Data point for historical portfolio value
     */
    public static class PortfolioValueDataPoint {
        /**
         * Timestamp of the data point
         */
        private LocalDateTime timestamp;
        
        /**
         * Portfolio total value at this timestamp
         */
        private BigDecimal value;
        
        /**
         * Default constructor
         */
        public PortfolioValueDataPoint() {
        }
        
        /**
         * Constructor with all fields
         * 
         * @param timestamp Timestamp of the data point
         * @param value Portfolio total value at this timestamp
         */
        public PortfolioValueDataPoint(LocalDateTime timestamp, BigDecimal value) {
            this.timestamp = timestamp;
            this.value = value;
        }
        
        /**
         * Gets timestamp of the data point
         * 
         * @return Timestamp of the data point
         */
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        /**
         * Sets timestamp of the data point
         * 
         * @param timestamp Timestamp of the data point
         */
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        /**
         * Gets portfolio total value at this timestamp
         * 
         * @return Portfolio total value at this timestamp
         */
        public BigDecimal getValue() {
            return value;
        }
        
        /**
         * Sets portfolio total value at this timestamp
         * 
         * @param value Portfolio total value at this timestamp
         */
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PortfolioValueDataPoint that = (PortfolioValueDataPoint) o;
            return Objects.equals(timestamp, that.timestamp) && 
                   Objects.equals(value, that.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(timestamp, value);
        }
        
        @Override
        public String toString() {
            return "PortfolioValueDataPoint{" +
                    "timestamp=" + timestamp +
                    ", value=" + value +
                    "}";
        }
    }
    
    /**
     * Performance summary metrics
     */
    public static class PerformanceSummary {
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
        
        /**
         * Default constructor
         */
        public PerformanceSummary() {
        }
        
        /**
         * Gets total unrealized profit/loss in USD
         * 
         * @return Total unrealized profit/loss in USD
         */
        public BigDecimal getTotalProfitLoss() {
            return totalProfitLoss;
        }
        
        /**
         * Sets total unrealized profit/loss in USD
         * 
         * @param totalProfitLoss Total unrealized profit/loss in USD
         */
        public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
            this.totalProfitLoss = totalProfitLoss;
        }
        
        /**
         * Gets total profit/loss percentage
         * 
         * @return Total profit/loss percentage
         */
        public BigDecimal getTotalProfitLossPercentage() {
            return totalProfitLossPercentage;
        }
        
        /**
         * Sets total profit/loss percentage
         * 
         * @param totalProfitLossPercentage Total profit/loss percentage
         */
        public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) {
            this.totalProfitLossPercentage = totalProfitLossPercentage;
        }
        
        /**
         * Gets 24-hour change in USD
         * 
         * @return 24-hour change in USD
         */
        public BigDecimal getDailyChange() {
            return dailyChange;
        }
        
        /**
         * Sets 24-hour change in USD
         * 
         * @param dailyChange 24-hour change in USD
         */
        public void setDailyChange(BigDecimal dailyChange) {
            this.dailyChange = dailyChange;
        }
        
        /**
         * Gets 24-hour change percentage
         * 
         * @return 24-hour change percentage
         */
        public BigDecimal getDailyChangePercentage() {
            return dailyChangePercentage;
        }
        
        /**
         * Sets 24-hour change percentage
         * 
         * @param dailyChangePercentage 24-hour change percentage
         */
        public void setDailyChangePercentage(BigDecimal dailyChangePercentage) {
            this.dailyChangePercentage = dailyChangePercentage;
        }
        
        /**
         * Gets 7-day change in USD
         * 
         * @return 7-day change in USD
         */
        public BigDecimal getWeeklyChange() {
            return weeklyChange;
        }
        
        /**
         * Sets 7-day change in USD
         * 
         * @param weeklyChange 7-day change in USD
         */
        public void setWeeklyChange(BigDecimal weeklyChange) {
            this.weeklyChange = weeklyChange;
        }
        
        /**
         * Gets 7-day change percentage
         * 
         * @return 7-day change percentage
         */
        public BigDecimal getWeeklyChangePercentage() {
            return weeklyChangePercentage;
        }
        
        /**
         * Sets 7-day change percentage
         * 
         * @param weeklyChangePercentage 7-day change percentage
         */
        public void setWeeklyChangePercentage(BigDecimal weeklyChangePercentage) {
            this.weeklyChangePercentage = weeklyChangePercentage;
        }
        
        /**
         * Gets 30-day change in USD
         * 
         * @return 30-day change in USD
         */
        public BigDecimal getMonthlyChange() {
            return monthlyChange;
        }
        
        /**
         * Sets 30-day change in USD
         * 
         * @param monthlyChange 30-day change in USD
         */
        public void setMonthlyChange(BigDecimal monthlyChange) {
            this.monthlyChange = monthlyChange;
        }
        
        /**
         * Gets 30-day change percentage
         * 
         * @return 30-day change percentage
         */
        public BigDecimal getMonthlyChangePercentage() {
            return monthlyChangePercentage;
        }
        
        /**
         * Sets 30-day change percentage
         * 
         * @param monthlyChangePercentage 30-day change percentage
         */
        public void setMonthlyChangePercentage(BigDecimal monthlyChangePercentage) {
            this.monthlyChangePercentage = monthlyChangePercentage;
        }
        
        /**
         * Gets year-to-date change in USD
         * 
         * @return Year-to-date change in USD
         */
        public BigDecimal getYtdChange() {
            return ytdChange;
        }
        
        /**
         * Sets year-to-date change in USD
         * 
         * @param ytdChange Year-to-date change in USD
         */
        public void setYtdChange(BigDecimal ytdChange) {
            this.ytdChange = ytdChange;
        }
        
        /**
         * Gets year-to-date change percentage
         * 
         * @return Year-to-date change percentage
         */
        public BigDecimal getYtdChangePercentage() {
            return ytdChangePercentage;
        }
        
        /**
         * Sets year-to-date change percentage
         * 
         * @param ytdChangePercentage Year-to-date change percentage
         */
        public void setYtdChangePercentage(BigDecimal ytdChangePercentage) {
            this.ytdChangePercentage = ytdChangePercentage;
        }
        
        /**
         * Gets whether the portfolio is currently profitable
         * 
         * @return Whether the portfolio is currently profitable
         */
        public boolean isProfitable() {
            return profitable;
        }
        
        /**
         * Sets whether the portfolio is currently profitable
         * 
         * @param profitable Whether the portfolio is currently profitable
         */
        public void setProfitable(boolean profitable) {
            this.profitable = profitable;
        }
        
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
            return Objects.hash(
                totalProfitLoss,
                totalProfitLossPercentage,
                dailyChange,
                dailyChangePercentage,
                weeklyChange,
                weeklyChangePercentage,
                monthlyChange,
                monthlyChangePercentage,
                ytdChange,
                ytdChangePercentage,
                profitable
            );
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
                    "}";
        }
    }
}
