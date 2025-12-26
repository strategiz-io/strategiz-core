package io.strategiz.client.yahoo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Yahoo Finance Financial Data DTO.
 *
 * Contains financial ratios, margins, and profitability metrics.
 * Corresponds to the financialData section of Yahoo's quote summary.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YahooFinancialData {

    @JsonProperty("currentPrice")
    private BigDecimal currentPrice;

    @JsonProperty("targetHighPrice")
    private BigDecimal targetHighPrice;

    @JsonProperty("targetLowPrice")
    private BigDecimal targetLowPrice;

    @JsonProperty("targetMeanPrice")
    private BigDecimal targetMeanPrice;

    @JsonProperty("recommendationMean")
    private BigDecimal recommendationMean;

    @JsonProperty("numberOfAnalystOpinions")
    private Integer numberOfAnalystOpinions;

    @JsonProperty("totalCash")
    private BigDecimal totalCash;

    @JsonProperty("totalCashPerShare")
    private BigDecimal totalCashPerShare;

    @JsonProperty("ebitda")
    private BigDecimal ebitda;

    @JsonProperty("totalDebt")
    private BigDecimal totalDebt;

    @JsonProperty("quickRatio")
    private BigDecimal quickRatio;

    @JsonProperty("currentRatio")
    private BigDecimal currentRatio;

    @JsonProperty("totalRevenue")
    private BigDecimal totalRevenue;

    @JsonProperty("debtToEquity")
    private BigDecimal debtToEquity;

    @JsonProperty("revenuePerShare")
    private BigDecimal revenuePerShare;

    @JsonProperty("returnOnAssets")
    private BigDecimal returnOnAssets;

    @JsonProperty("returnOnEquity")
    private BigDecimal returnOnEquity;

    @JsonProperty("grossProfits")
    private BigDecimal grossProfits;

    @JsonProperty("freeCashflow")
    private BigDecimal freeCashflow;

    @JsonProperty("operatingCashflow")
    private BigDecimal operatingCashflow;

    @JsonProperty("earningsGrowth")
    private BigDecimal earningsGrowth;

    @JsonProperty("revenueGrowth")
    private BigDecimal revenueGrowth;

    @JsonProperty("grossMargins")
    private BigDecimal grossMargins;

    @JsonProperty("ebitdaMargins")
    private BigDecimal ebitdaMargins;

    @JsonProperty("operatingMargins")
    private BigDecimal operatingMargins;

    @JsonProperty("profitMargins")
    private BigDecimal profitMargins;

    // Constructors
    public YahooFinancialData() {
    }

    // Getters and Setters
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getTargetHighPrice() {
        return targetHighPrice;
    }

    public void setTargetHighPrice(BigDecimal targetHighPrice) {
        this.targetHighPrice = targetHighPrice;
    }

    public BigDecimal getTargetLowPrice() {
        return targetLowPrice;
    }

    public void setTargetLowPrice(BigDecimal targetLowPrice) {
        this.targetLowPrice = targetLowPrice;
    }

    public BigDecimal getTargetMeanPrice() {
        return targetMeanPrice;
    }

    public void setTargetMeanPrice(BigDecimal targetMeanPrice) {
        this.targetMeanPrice = targetMeanPrice;
    }

    public BigDecimal getRecommendationMean() {
        return recommendationMean;
    }

    public void setRecommendationMean(BigDecimal recommendationMean) {
        this.recommendationMean = recommendationMean;
    }

    public Integer getNumberOfAnalystOpinions() {
        return numberOfAnalystOpinions;
    }

    public void setNumberOfAnalystOpinions(Integer numberOfAnalystOpinions) {
        this.numberOfAnalystOpinions = numberOfAnalystOpinions;
    }

    public BigDecimal getTotalCash() {
        return totalCash;
    }

    public void setTotalCash(BigDecimal totalCash) {
        this.totalCash = totalCash;
    }

    public BigDecimal getTotalCashPerShare() {
        return totalCashPerShare;
    }

    public void setTotalCashPerShare(BigDecimal totalCashPerShare) {
        this.totalCashPerShare = totalCashPerShare;
    }

    public BigDecimal getEbitda() {
        return ebitda;
    }

    public void setEbitda(BigDecimal ebitda) {
        this.ebitda = ebitda;
    }

    public BigDecimal getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(BigDecimal totalDebt) {
        this.totalDebt = totalDebt;
    }

    public BigDecimal getQuickRatio() {
        return quickRatio;
    }

    public void setQuickRatio(BigDecimal quickRatio) {
        this.quickRatio = quickRatio;
    }

    public BigDecimal getCurrentRatio() {
        return currentRatio;
    }

    public void setCurrentRatio(BigDecimal currentRatio) {
        this.currentRatio = currentRatio;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getDebtToEquity() {
        return debtToEquity;
    }

    public void setDebtToEquity(BigDecimal debtToEquity) {
        this.debtToEquity = debtToEquity;
    }

    public BigDecimal getRevenuePerShare() {
        return revenuePerShare;
    }

    public void setRevenuePerShare(BigDecimal revenuePerShare) {
        this.revenuePerShare = revenuePerShare;
    }

    public BigDecimal getReturnOnAssets() {
        return returnOnAssets;
    }

    public void setReturnOnAssets(BigDecimal returnOnAssets) {
        this.returnOnAssets = returnOnAssets;
    }

    public BigDecimal getReturnOnEquity() {
        return returnOnEquity;
    }

    public void setReturnOnEquity(BigDecimal returnOnEquity) {
        this.returnOnEquity = returnOnEquity;
    }

    public BigDecimal getGrossProfits() {
        return grossProfits;
    }

    public void setGrossProfits(BigDecimal grossProfits) {
        this.grossProfits = grossProfits;
    }

    public BigDecimal getFreeCashflow() {
        return freeCashflow;
    }

    public void setFreeCashflow(BigDecimal freeCashflow) {
        this.freeCashflow = freeCashflow;
    }

    public BigDecimal getOperatingCashflow() {
        return operatingCashflow;
    }

    public void setOperatingCashflow(BigDecimal operatingCashflow) {
        this.operatingCashflow = operatingCashflow;
    }

    public BigDecimal getEarningsGrowth() {
        return earningsGrowth;
    }

    public void setEarningsGrowth(BigDecimal earningsGrowth) {
        this.earningsGrowth = earningsGrowth;
    }

    public BigDecimal getRevenueGrowth() {
        return revenueGrowth;
    }

    public void setRevenueGrowth(BigDecimal revenueGrowth) {
        this.revenueGrowth = revenueGrowth;
    }

    public BigDecimal getGrossMargins() {
        return grossMargins;
    }

    public void setGrossMargins(BigDecimal grossMargins) {
        this.grossMargins = grossMargins;
    }

    public BigDecimal getEbitdaMargins() {
        return ebitdaMargins;
    }

    public void setEbitdaMargins(BigDecimal ebitdaMargins) {
        this.ebitdaMargins = ebitdaMargins;
    }

    public BigDecimal getOperatingMargins() {
        return operatingMargins;
    }

    public void setOperatingMargins(BigDecimal operatingMargins) {
        this.operatingMargins = operatingMargins;
    }

    public BigDecimal getProfitMargins() {
        return profitMargins;
    }

    public void setProfitMargins(BigDecimal profitMargins) {
        this.profitMargins = profitMargins;
    }
}
