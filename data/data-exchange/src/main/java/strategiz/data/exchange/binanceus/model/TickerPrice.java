package strategiz.data.exchange.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Model class for Binance US ticker price data
 * This represents price information for a specific trading pair from the Binance US API
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TickerPrice {
    private String symbol;
    private String price;
}
