package strategiz.client.kraken.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Model for Kraken balance
 * This represents balance information for a specific asset from the Kraken API
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
