package io.strategiz.service.dashboard.model.dashboard;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Exchange data
 */
public class ExchangeData {
    private String name;
    private BigDecimal value;
    private Map<String, AssetData> assets;

    // Constructors
    public ExchangeData() {}

    public ExchangeData(String name, BigDecimal value, Map<String, AssetData> assets) {
        this.name = name;
        this.value = value;
        this.assets = assets;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public Map<String, AssetData> getAssets() {
        return assets;
    }

    public void setAssets(Map<String, AssetData> assets) {
        this.assets = assets;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeData that = (ExchangeData) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(value, that.value) &&
               Objects.equals(assets, that.assets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, assets);
    }

    @Override
    public String toString() {
        return "ExchangeData{" +
               "name='" + name + '\'' +
               ", value=" + value +
               ", assets=" + assets +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public Builder withAssets(Map<String, AssetData> assets) {
            this.assets = assets;
            return this;
        }

        public ExchangeData build() {
            return new ExchangeData(name, value, assets);
        }
    }
}
