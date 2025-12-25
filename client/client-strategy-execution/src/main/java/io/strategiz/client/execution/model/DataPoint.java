package io.strategiz.client.execution.model;

public class DataPoint {
    private String timestamp;
    private double value;

    public DataPoint() {}

    public DataPoint(String timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public static DataPointBuilder builder() {
        return new DataPointBuilder();
    }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public static class DataPointBuilder {
        private String timestamp;
        private double value;

        public DataPointBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public DataPointBuilder value(double value) { this.value = value; return this; }
        public DataPoint build() {
            return new DataPoint(timestamp, value);
        }
    }
}
