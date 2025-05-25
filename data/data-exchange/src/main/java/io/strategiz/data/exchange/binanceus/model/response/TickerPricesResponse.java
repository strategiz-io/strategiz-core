package io.strategiz.data.exchange.binanceus.model.response;

import io.strategiz.data.exchange.binanceus.model.TickerPrice;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Response model for ticker prices information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TickerPricesResponse {
    private List<TickerPrice> tickerPrices;
}
