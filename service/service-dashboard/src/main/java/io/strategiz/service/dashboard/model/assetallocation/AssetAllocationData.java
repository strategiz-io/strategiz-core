package io.strategiz.service.dashboard.model.assetallocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data model for asset allocation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocationData {
    
    /**
     * List of asset allocations
     */
    private List<AssetAllocation> allocations;
    
    /**
     * Total value of all assets
     */
    private BigDecimal totalValue;
}
