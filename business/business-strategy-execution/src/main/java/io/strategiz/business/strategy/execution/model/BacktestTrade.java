package io.strategiz.business.strategy.execution.model;

/**
 * Model representing a single trade (buy-sell pair) in a backtest.
 */
public class BacktestTrade {

    private String buyTimestamp;
    private String sellTimestamp;
    private double buyPrice;
    private double sellPrice;
    private double pnl;
    private double pnlPercent;
    private boolean isWin;
    private boolean unrealized;
    private String buyReason;
    private String sellReason;

    public BacktestTrade() {
    }

    // Getters and Setters
    public String getBuyTimestamp() {
        return buyTimestamp;
    }

    public void setBuyTimestamp(String buyTimestamp) {
        this.buyTimestamp = buyTimestamp;
    }

    public String getSellTimestamp() {
        return sellTimestamp;
    }

    public void setSellTimestamp(String sellTimestamp) {
        this.sellTimestamp = sellTimestamp;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }

    public double getPnl() {
        return pnl;
    }

    public void setPnl(double pnl) {
        this.pnl = pnl;
    }

    public double getPnlPercent() {
        return pnlPercent;
    }

    public void setPnlPercent(double pnlPercent) {
        this.pnlPercent = pnlPercent;
    }

    public boolean isWin() {
        return isWin;
    }

    public void setWin(boolean isWin) {
        this.isWin = isWin;
    }

    public boolean isUnrealized() {
        return unrealized;
    }

    public void setUnrealized(boolean unrealized) {
        this.unrealized = unrealized;
    }

    public String getBuyReason() {
        return buyReason;
    }

    public void setBuyReason(String buyReason) {
        this.buyReason = buyReason;
    }

    public String getSellReason() {
        return sellReason;
    }

    public void setSellReason(String sellReason) {
        this.sellReason = sellReason;
    }
}
