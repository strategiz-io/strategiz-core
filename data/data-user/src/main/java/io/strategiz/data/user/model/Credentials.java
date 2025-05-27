package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a document in the "credentials" subcollection under a provider.
 * Contains sensitive API credentials and keys.
 */
@Data
@NoArgsConstructor
public class Credentials {
    private String id;  // Usually "default"
    private String apiKey;
    private String privateKey;
    private Map<String, Object> encryptedData;  // Any additional encrypted credentials
}
