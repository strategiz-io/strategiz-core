package io.strategiz.service.dashboard.model.assetallocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Data model for asset allocation.
 */
public class AssetAllocationData {
    
    /**
     * List of asset allocations
     */
    private List<AssetAllocation> allocations;
    
    /**
     * Total value of all assets
     */
    private BigDecimal totalValue;

    // Constructors
    public AssetAllocationData() {}

    public AssetAllocationData(List<AssetAllocation> allocations, BigDecimal totalValue) {
        this.allocations = allocations;
        this.totalValue = totalValue;
    }

    // Getters and Setters
    public List<AssetAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<AssetAllocation> allocations) {
        this.allocations = allocations;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetAllocationData that = (AssetAllocationData) o;
        return Objects.equals(allocations, that.allocations) &&
               Objects.equals(totalValue, that.totalValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocations, totalValue);
    }

    @Override
    public String toString() {
        return "AssetAllocationData{" +
               "allocations=" + allocations +
               ", totalValue=" + totalValue +
               '}';
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<AssetAllocation> allocations;
        private BigDecimal totalValue;

        public Builder withAllocations(List<AssetAllocation> allocations) {
            this.allocations = allocations;
            return this;
        }

        public Builder withTotalValue(BigDecimal totalValue) {
            this.totalValue = totalValue;
            return this;
        }

        public AssetAllocationData build() {
            return new AssetAllocationData(allocations, totalValue);
        }
    }
}
