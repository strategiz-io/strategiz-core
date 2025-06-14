package io.strategiz.service.dashboard.model.portfoliosummary;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Exchange-specific data containing assets grouped by exchange.
 */
public class ExchangeData {
    private String id;
    private String name;
    private BigDecimal value;
    private Map<String, AssetData> assets;
    
    /**
     * Default constructor
     */
    public ExchangeData() {
    }
    
    /**
     * All-args constructor
     */
    public ExchangeData(String id, String name, BigDecimal value, Map<String, AssetData> assets) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.assets = assets;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeData that = (ExchangeData) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(assets, that.assets);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, value, assets);
    }
    
    @Override
    public String toString() {
        return "ExchangeData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", value=" + value +
                ", assets=" + assets +
                '}';
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String name;
        private BigDecimal value;
        private Map<String, AssetData> assets;
        
        public Builder withId(String id) { this.id = id; return this; }
        public Builder withName(String name) { this.name = name; return this; }
        public Builder withValue(BigDecimal value) { this.value = value; return this; }
        public Builder withAssets(Map<String, AssetData> assets) { this.assets = assets; return this; }
        
        public ExchangeData build() {
            return new ExchangeData(id, name, value, assets);
        }
    }
}
