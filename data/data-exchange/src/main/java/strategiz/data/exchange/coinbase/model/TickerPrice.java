package strategiz.data.exchange.coinbase.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Model class representing a Coinbase ticker price
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerPrice {
    private String base;
    private String currency;
    private String amount;
    
    @JsonProperty("product_id")
    private String productId;
}
