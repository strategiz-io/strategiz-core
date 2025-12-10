package io.strategiz.business.strategy.execution.model;

/**
 * Model representing a trading signal (BUY/SELL/HOLD) with associated metadata.
 */
public class SignalData {

    private String timestamp;
    private String type; // BUY, SELL, HOLD
    private double price;
    private double quantity;
    private String reason;

    public SignalData() {
    }

    public SignalData(String timestamp, String type, double price) {
        this.timestamp = timestamp;
        this.type = type;
        this.price = price;
    }

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
