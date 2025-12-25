package io.strategiz.client.execution.model;

public class Trade {
    private String buyTimestamp;
    private String sellTimestamp;
    private double buyPrice;
    private double sellPrice;
    private double pnl;
    private double pnlPercent;
    private boolean win;
    private String buyReason;
    private String sellReason;

    public Trade() {}

    public Trade(String buyTimestamp, String sellTimestamp, double buyPrice, double sellPrice,
                 double pnl, double pnlPercent, boolean win, String buyReason, String sellReason) {
        this.buyTimestamp = buyTimestamp;
        this.sellTimestamp = sellTimestamp;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.pnl = pnl;
        this.pnlPercent = pnlPercent;
        this.win = win;
        this.buyReason = buyReason;
        this.sellReason = sellReason;
    }

    public static TradeBuilder builder() {
        return new TradeBuilder();
    }

    public String getBuyTimestamp() { return buyTimestamp; }
    public void setBuyTimestamp(String buyTimestamp) { this.buyTimestamp = buyTimestamp; }
    public String getSellTimestamp() { return sellTimestamp; }
    public void setSellTimestamp(String sellTimestamp) { this.sellTimestamp = sellTimestamp; }
    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public double getPnl() { return pnl; }
    public void setPnl(double pnl) { this.pnl = pnl; }
    public double getPnlPercent() { return pnlPercent; }
    public void setPnlPercent(double pnlPercent) { this.pnlPercent = pnlPercent; }
    public boolean isWin() { return win; }
    public void setWin(boolean win) { this.win = win; }
    public String getBuyReason() { return buyReason; }
    public void setBuyReason(String buyReason) { this.buyReason = buyReason; }
    public String getSellReason() { return sellReason; }
    public void setSellReason(String sellReason) { this.sellReason = sellReason; }

    public static class TradeBuilder {
        private String buyTimestamp;
        private String sellTimestamp;
        private double buyPrice;
        private double sellPrice;
        private double pnl;
        private double pnlPercent;
        private boolean win;
        private String buyReason;
        private String sellReason;

        public TradeBuilder buyTimestamp(String buyTimestamp) { this.buyTimestamp = buyTimestamp; return this; }
        public TradeBuilder sellTimestamp(String sellTimestamp) { this.sellTimestamp = sellTimestamp; return this; }
        public TradeBuilder buyPrice(double buyPrice) { this.buyPrice = buyPrice; return this; }
        public TradeBuilder sellPrice(double sellPrice) { this.sellPrice = sellPrice; return this; }
        public TradeBuilder pnl(double pnl) { this.pnl = pnl; return this; }
        public TradeBuilder pnlPercent(double pnlPercent) { this.pnlPercent = pnlPercent; return this; }
        public TradeBuilder win(boolean win) { this.win = win; return this; }
        public TradeBuilder buyReason(String buyReason) { this.buyReason = buyReason; return this; }
        public TradeBuilder sellReason(String sellReason) { this.sellReason = sellReason; return this; }
        public Trade build() {
            return new Trade(buyTimestamp, sellTimestamp, buyPrice, sellPrice, pnl, pnlPercent, win, buyReason, sellReason);
        }
    }
}
