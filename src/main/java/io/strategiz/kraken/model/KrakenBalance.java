package io.strategiz.kraken.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Model for Kraken balance
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KrakenBalance {
    private String asset;
    private String balance;
    
    // Additional fields for UI display
    private double balanceValue;
    private double usdValue;
}
