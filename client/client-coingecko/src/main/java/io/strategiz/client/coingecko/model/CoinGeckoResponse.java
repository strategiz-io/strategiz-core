package io.strategiz.client.coingecko.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base response model for CoinGecko API responses.
 * Simple POJO that follows the pattern used in other client modules.
 */
@Data
public class CoinGeckoResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();
}
