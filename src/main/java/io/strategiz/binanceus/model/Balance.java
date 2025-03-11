package io.strategiz.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Balance {
    private String asset;
    private String free;
    private String locked;
    
    // Added fields for calculated values (not part of raw API response)
    private double freeValue;
    private double lockedValue;
    private double totalValue;
    private double usdValue;
}
