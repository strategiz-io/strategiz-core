package io.strategiz.client.binanceus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Base response class for Binance US API responses
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceUSResponse {
    private boolean success;
    private String message;
}
