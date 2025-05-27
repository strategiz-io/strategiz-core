package io.strategiz.client.coingecko.model;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base response model for CoinGecko API responses following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CoinGeckoResponse extends BaseServiceResponse {
    private boolean success;
    private String message;
}
