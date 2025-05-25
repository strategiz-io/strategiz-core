package strategiz.data.exchange.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Model class representing a Coinbase account
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
    private String id;
    private String name;
    private String currency;
    private Balance balance;
    private String type;
    private boolean primary;
    private boolean active;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    @JsonProperty("updated_at")
    private String updatedAt;
    
    // Additional fields for calculated values (not part of raw API response)
    private double usdValue;
}
