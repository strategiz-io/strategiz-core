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

    public static TradeBuilder builder() { return new TradeBuilder(); }

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

        public TradeBuilder buyTimestamp(String buyTimestamp) {
            this.buyTimestamp = buyTimestamp;
            return this;
        }

        public TradeBuilder sellTimestamp(String sellTimestamp) {
            this.sellTimestamp = sellTimestamp;
            return this;
        }

        public TradeBuilder buyPrice(double buyPrice) {
            this.buyPrice = buyPrice;
            return this;
        }

        public TradeBuilder sellPrice(double sellPrice) {
            this.sellPrice = sellPrice;
            return this;
        }

        public TradeBuilder pnl(double pnl) {
            this.pnl = pnl;
            return this;
        }

        public TradeBuilder pnlPercent(double pnlPercent) {
            this.pnlPercent = pnlPercent;
            return this;
        }

        public TradeBuilder win(boolean win) {
            this.win = win;
            return this;
        }

        public TradeBuilder buyReason(String buyReason) {
            this.buyReason = buyReason;
            return this;
        }

        public TradeBuilder sellReason(String sellReason) {
            this.sellReason = sellReason;
            return this;
        }

        public Trade build() {
            Trade trade = new Trade();
            trade.buyTimestamp = this.buyTimestamp;
            trade.sellTimestamp = this.sellTimestamp;
            trade.buyPrice = this.buyPrice;
            trade.sellPrice = this.sellPrice;
            trade.pnl = this.pnl;
            trade.pnlPercent = this.pnlPercent;
            trade.win = this.win;
            trade.buyReason = this.buyReason;
            trade.sellReason = this.sellReason;
            return trade;
        }
    }

    public String getBuyTimestamp() { return buyTimestamp; }
    public String getSellTimestamp() { return sellTimestamp; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public double getPnl() { return pnl; }
    public double getPnlPercent() { return pnlPercent; }
    public boolean isWin() { return win; }
    public String getBuyReason() { return buyReason; }
    public String getSellReason() { return sellReason; }
}
