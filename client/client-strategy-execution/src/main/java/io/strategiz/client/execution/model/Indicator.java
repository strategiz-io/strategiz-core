package io.strategiz.client.execution.model;

import java.util.List;

public class Indicator {
    private String name;
    private List<DataPoint> data;

    public Indicator() {}

    public Indicator(String name, List<DataPoint> data) {
        this.name = name;
        this.data = data;
    }

    public static IndicatorBuilder builder() {
        return new IndicatorBuilder();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<DataPoint> getData() { return data; }
    public void setData(List<DataPoint> data) { this.data = data; }

    public static class IndicatorBuilder {
        private String name;
        private List<DataPoint> data;

        public IndicatorBuilder name(String name) { this.name = name; return this; }
        public IndicatorBuilder data(List<DataPoint> data) { this.data = data; return this; }
        public Indicator build() {
            return new Indicator(name, data);
        }
    }
}
