package io.strategiz.data.exchange.binanceus.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response model for exchange information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ExchangeInfoResponse {
    private Object exchangeInfo;
}
