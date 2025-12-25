package io.strategiz.client.execution.model;

public class Signal {
    private String timestamp;
    private String type;
    private double price;
    private int quantity;
    private String reason;

    public Signal() {}

    public Signal(String timestamp, String type, double price, int quantity, String reason) {
        this.timestamp = timestamp;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.reason = reason;
    }

    public static SignalBuilder builder() {
        return new SignalBuilder();
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static class SignalBuilder {
        private String timestamp;
        private String type;
        private double price;
        private int quantity;
        private String reason;

        public SignalBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public SignalBuilder type(String type) { this.type = type; return this; }
        public SignalBuilder price(double price) { this.price = price; return this; }
        public SignalBuilder quantity(int quantity) { this.quantity = quantity; return this; }
        public SignalBuilder reason(String reason) { this.reason = reason; return this; }
        public Signal build() {
            return new Signal(timestamp, type, price, quantity, reason);
        }
    }
}
