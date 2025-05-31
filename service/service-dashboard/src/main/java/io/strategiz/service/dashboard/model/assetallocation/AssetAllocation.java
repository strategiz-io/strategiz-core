package io.strategiz.service.dashboard.model.assetallocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Model for individual asset allocation data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAllocation {
    
    private String id;
    private String name;
    private String symbol;
    private String exchange;
    private BigDecimal value;
    private BigDecimal percentage;
    private String color;
}
