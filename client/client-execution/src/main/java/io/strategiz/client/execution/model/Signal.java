package io.strategiz.client.execution.model;

public class Signal {
    private String timestamp;
    private String type;
    private double price;
    private int quantity;
    private String reason;

    public static SignalBuilder builder() {
        return new SignalBuilder();
    }

    public static class SignalBuilder {
        private String timestamp;
        private String type;
        private double price;
        private int quantity;
        private String reason;

        public SignalBuilder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public SignalBuilder type(String type) {
            this.type = type;
            return this;
        }

        public SignalBuilder price(double price) {
            this.price = price;
            return this;
        }

        public SignalBuilder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public SignalBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Signal build() {
            Signal signal = new Signal();
            signal.timestamp = this.timestamp;
            signal.type = this.type;
            signal.price = this.price;
            signal.quantity = this.quantity;
            signal.reason = this.reason;
            return signal;
        }
    }

    public String getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getReason() { return reason; }
}
