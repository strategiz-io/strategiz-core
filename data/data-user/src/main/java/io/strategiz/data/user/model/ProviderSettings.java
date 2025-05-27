package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a document in the "providers" subcollection under a user.
 * Contains non-sensitive provider configuration and settings.
 */
@Data
@NoArgsConstructor
public class Provider {
    private String id;  // The provider ID (e.g., "kraken", "binanceus", "alpaca")
    private String providerType;  // "EXCHANGE" or "BROKER"
    private String accountType;  // "PAPER" or "REAL" (only relevant for brokers)
    private Map<String, Object> settings;  // Non-sensitive provider-specific settings
}
