package io.strategiz.data.exchange;

import lombok.Data;
import java.util.Map;

/**
 * Model class for Kraken account data
 * This represents the completely unmodified raw data from the Kraken API
 */
@Data
public class KrakenAccount {
    private String[] error;
    private Map<String, Object> result; // Using Object to handle different value types
}
