package io.strategiz.binanceus.model.response;

import io.strategiz.framework.rest.model.BaseServiceResponse;
import io.strategiz.binanceus.model.TickerPrice;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TickerPricesResponse extends BaseServiceResponse {
    private List<TickerPrice> tickerPrices;
}
