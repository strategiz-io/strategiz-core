package io.strategiz.data.exchange.binanceus.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request model for balance information
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BalanceRequest {
    private String apiKey;
    private String secretKey;
    private String userId;
}
