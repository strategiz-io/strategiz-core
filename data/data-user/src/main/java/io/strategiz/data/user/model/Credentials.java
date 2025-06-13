package io.strategiz.data.user.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a document in the "credentials" subcollection under a provider.
 * Contains sensitive API credentials and keys.
 */
public class Credentials {
    private String id;  // Usually "default"
    private String apiKey;
    private String privateKey;
    private Map<String, Object> encryptedData;  // Any additional encrypted credentials

    // Constructors
    public Credentials() {
    }

    public Credentials(String id, String apiKey, String privateKey, Map<String, Object> encryptedData) {
        this.id = id;
        this.apiKey = apiKey;
        this.privateKey = privateKey;
        this.encryptedData = encryptedData;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Map<String, Object> getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(Map<String, Object> encryptedData) {
        this.encryptedData = encryptedData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credentials that = (Credentials) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(apiKey, that.apiKey) &&
               Objects.equals(privateKey, that.privateKey) &&
               Objects.equals(encryptedData, that.encryptedData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, apiKey, privateKey, encryptedData);
    }

    @Override
    public String toString() {
        return "Credentials{" +
               "id='" + id + '\'' +
               ", apiKey='" + (apiKey != null ? "[PROTECTED]" : "null") + '\'' +
               ", privateKey='" + (privateKey != null ? "[PROTECTED]" : "null") + '\'' +
               ", encryptedData=" + (encryptedData != null ? "[PROTECTED]" : "null") +
               '}';
    }
}
