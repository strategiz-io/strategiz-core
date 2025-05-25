package io.strategiz.client.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Model class representing a Coinbase account balance
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Balance {
    private String amount;
    private String currency;
    
    // Additional fields for calculated values (not part of raw API response)
    private double amountValue;
    private double usdValue;
}
