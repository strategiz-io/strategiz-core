package io.strategiz.client.alphavantage.model;

import io.americanexpress.synapse.service.rest.model.BaseServiceResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Base response model for AlphaVantage API responses following Synapse patterns.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlphaVantageResponse extends BaseServiceResponse {
    private boolean success;
    private String message;
}
