package io.strategiz.service.labs.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ExecuteStrategyResponse {
    
    @JsonProperty("strategyId")
    private String strategyId;
    
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("signals")
    private List<Signal> signals;
    
    @JsonProperty("indicators")
    private List<Indicator> indicators;
    
    @JsonProperty("performance")
    private Performance performance;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    @JsonProperty("executionTime")
    private long executionTime;
    
    @JsonProperty("logs")
    private List<String> logs;
    
    // Nested classes
    public static class Signal {
        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("type")
        private String type; // BUY, SELL, HOLD

        @JsonProperty("price")
        private double price;

        @JsonProperty("quantity")
        private double quantity;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("text")
        private String text; // Display text for marker

        @JsonProperty("shape")
        private String shape; // Marker shape: circle, arrow_up, arrow_down, triangle
        
        // Getters and Setters
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
        
        public double getQuantity() {
            return quantity;
        }
        
        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }
        
        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }
    }
    
    public static class Indicator {
        @JsonProperty("name")
        private String name;

        @JsonProperty("data")
        private List<DataPoint> data;

        @JsonProperty("color")
        private String color;

        @JsonProperty("type")
        private String type; // line, area, histogram

        @JsonProperty("linewidth")
        private int linewidth; // Line thickness in pixels

        @JsonProperty("overlay")
        private boolean overlay; // Whether to overlay on price chart or separate panel
        
        public static class DataPoint {
            @JsonProperty("time")
            private String time;
            
            @JsonProperty("value")
            private double value;
            
            // Getters and Setters
            public String getTime() {
                return time;
            }
            
            public void setTime(String time) {
                this.time = time;
            }
            
            public double getValue() {
                return value;
            }
            
            public void setValue(double value) {
                this.value = value;
            }
        }
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<DataPoint> getData() {
            return data;
        }
        
        public void setData(List<DataPoint> data) {
            this.data = data;
        }
        
        public String getColor() {
            return color;
        }
        
        public void setColor(String color) {
            this.color = color;
        }
        
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getLinewidth() {
            return linewidth;
        }

        public void setLinewidth(int linewidth) {
            this.linewidth = linewidth;
        }

        public boolean isOverlay() {
            return overlay;
        }

        public void setOverlay(boolean overlay) {
            this.overlay = overlay;
        }
    }
    
    public static class Performance {
        @JsonProperty("totalReturn")
        private double totalReturn;

        @JsonProperty("totalPnL")
        private double totalPnL;

        @JsonProperty("winRate")
        private double winRate;

        @JsonProperty("totalTrades")
        private int totalTrades;

        @JsonProperty("profitableTrades")
        private int profitableTrades;

        @JsonProperty("buyCount")
        private int buyCount;

        @JsonProperty("sellCount")
        private int sellCount;

        @JsonProperty("avgWin")
        private double avgWin;

        @JsonProperty("avgLoss")
        private double avgLoss;

        @JsonProperty("profitFactor")
        private double profitFactor;

        @JsonProperty("maxDrawdown")
        private double maxDrawdown;

        @JsonProperty("sharpeRatio")
        private double sharpeRatio;

        @JsonProperty("trades")
        private List<Trade> trades;

        @JsonProperty("lastTestedAt")
        private String lastTestedAt;

        // Equity curve (portfolio value over time)
        @JsonProperty("equityCurve")
        private List<EquityPoint> equityCurve;

        // Test period info
        @JsonProperty("startDate")
        private String startDate;

        @JsonProperty("endDate")
        private String endDate;

        @JsonProperty("testPeriod")
        private String testPeriod; // Formatted period (e.g., "2 years, 3 months")

        // Buy-and-hold comparison metrics
        @JsonProperty("buyAndHoldReturn")
        private Double buyAndHoldReturn;         // $ return if just buying and holding

        @JsonProperty("buyAndHoldReturnPercent")
        private Double buyAndHoldReturnPercent;  // % return if just buying and holding

        @JsonProperty("outperformance")
        private Double outperformance;           // Strategy return - Buy&Hold return (%)

        // Getters and Setters
        public double getTotalReturn() {
            return totalReturn;
        }

        public void setTotalReturn(double totalReturn) {
            this.totalReturn = totalReturn;
        }

        public double getTotalPnL() {
            return totalPnL;
        }

        public void setTotalPnL(double totalPnL) {
            this.totalPnL = totalPnL;
        }

        public double getWinRate() {
            return winRate;
        }

        public void setWinRate(double winRate) {
            this.winRate = winRate;
        }

        public int getTotalTrades() {
            return totalTrades;
        }

        public void setTotalTrades(int totalTrades) {
            this.totalTrades = totalTrades;
        }

        public int getProfitableTrades() {
            return profitableTrades;
        }

        public void setProfitableTrades(int profitableTrades) {
            this.profitableTrades = profitableTrades;
        }

        public int getBuyCount() {
            return buyCount;
        }

        public void setBuyCount(int buyCount) {
            this.buyCount = buyCount;
        }

        public int getSellCount() {
            return sellCount;
        }

        public void setSellCount(int sellCount) {
            this.sellCount = sellCount;
        }

        public double getAvgWin() {
            return avgWin;
        }

        public void setAvgWin(double avgWin) {
            this.avgWin = avgWin;
        }

        public double getAvgLoss() {
            return avgLoss;
        }

        public void setAvgLoss(double avgLoss) {
            this.avgLoss = avgLoss;
        }

        public double getProfitFactor() {
            return profitFactor;
        }

        public void setProfitFactor(double profitFactor) {
            this.profitFactor = profitFactor;
        }

        public double getMaxDrawdown() {
            return maxDrawdown;
        }

        public void setMaxDrawdown(double maxDrawdown) {
            this.maxDrawdown = maxDrawdown;
        }

        public double getSharpeRatio() {
            return sharpeRatio;
        }

        public void setSharpeRatio(double sharpeRatio) {
            this.sharpeRatio = sharpeRatio;
        }

        public List<Trade> getTrades() {
            return trades;
        }

        public void setTrades(List<Trade> trades) {
            this.trades = trades;
        }

        public String getLastTestedAt() {
            return lastTestedAt;
        }

        public void setLastTestedAt(String lastTestedAt) {
            this.lastTestedAt = lastTestedAt;
        }

        public String getTestPeriod() {
            return testPeriod;
        }

        public void setTestPeriod(String testPeriod) {
            this.testPeriod = testPeriod;
        }

        public Double getBuyAndHoldReturn() {
            return buyAndHoldReturn;
        }

        public void setBuyAndHoldReturn(Double buyAndHoldReturn) {
            this.buyAndHoldReturn = buyAndHoldReturn;
        }

        public Double getBuyAndHoldReturnPercent() {
            return buyAndHoldReturnPercent;
        }

        public void setBuyAndHoldReturnPercent(Double buyAndHoldReturnPercent) {
            this.buyAndHoldReturnPercent = buyAndHoldReturnPercent;
        }

        public Double getOutperformance() {
            return outperformance;
        }

        public void setOutperformance(Double outperformance) {
            this.outperformance = outperformance;
        }

        public List<EquityPoint> getEquityCurve() {
            return equityCurve;
        }

        public void setEquityCurve(List<EquityPoint> equityCurve) {
            this.equityCurve = equityCurve;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }

    public static class EquityPoint {
        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("portfolioValue")
        private double portfolioValue;

        @JsonProperty("type")
        private String type; // "initial", "buy", "sell", "final"

        // Getters and Setters
        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public double getPortfolioValue() {
            return portfolioValue;
        }

        public void setPortfolioValue(double portfolioValue) {
            this.portfolioValue = portfolioValue;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class Trade {
        @JsonProperty("buyTimestamp")
        private String buyTimestamp;

        @JsonProperty("sellTimestamp")
        private String sellTimestamp;

        @JsonProperty("buyPrice")
        private double buyPrice;

        @JsonProperty("sellPrice")
        private double sellPrice;

        @JsonProperty("pnl")
        private double pnl;

        @JsonProperty("pnlPercent")
        private double pnlPercent;

        @JsonProperty("isWin")
        private boolean isWin;

        @JsonProperty("buyReason")
        private String buyReason;

        @JsonProperty("sellReason")
        private String sellReason;

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
    
    // Main class getters and setters
    public String getStrategyId() {
        return strategyId;
    }
    
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }
    
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public List<Signal> getSignals() {
        return signals;
    }
    
    public void setSignals(List<Signal> signals) {
        this.signals = signals;
    }
    
    public List<Indicator> getIndicators() {
        return indicators;
    }
    
    public void setIndicators(List<Indicator> indicators) {
        this.indicators = indicators;
    }
    
    public Performance getPerformance() {
        return performance;
    }
    
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    
    public List<String> getLogs() {
        return logs;
    }
    
    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}