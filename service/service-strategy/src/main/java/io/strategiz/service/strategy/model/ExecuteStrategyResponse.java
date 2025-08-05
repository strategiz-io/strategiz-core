package io.strategiz.service.strategy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ExecuteStrategyResponse {
    
    @JsonProperty("strategyId")
    private String strategyId;
    
    @JsonProperty("ticker")
    private String ticker;
    
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
    }
    
    public static class Performance {
        @JsonProperty("totalReturn")
        private double totalReturn;
        
        @JsonProperty("winRate")
        private double winRate;
        
        @JsonProperty("totalTrades")
        private int totalTrades;
        
        @JsonProperty("profitableTrades")
        private int profitableTrades;
        
        @JsonProperty("maxDrawdown")
        private double maxDrawdown;
        
        @JsonProperty("sharpeRatio")
        private double sharpeRatio;
        
        // Getters and Setters
        public double getTotalReturn() {
            return totalReturn;
        }
        
        public void setTotalReturn(double totalReturn) {
            this.totalReturn = totalReturn;
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
    }
    
    // Main class getters and setters
    public String getStrategyId() {
        return strategyId;
    }
    
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
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