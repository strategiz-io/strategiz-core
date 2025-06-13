package io.strategiz.data.user.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a document in the "providers" subcollection under a user.
 * Contains non-sensitive provider configuration and settings.
 */
public class Provider {
    private String id;  // The provider ID (e.g., "kraken", "binanceus", "alpaca")
    private String providerType;  // "EXCHANGE" or "BROKER"
    private String accountType;  // "PAPER" or "REAL" (only relevant for brokers)
    private Map<String, Object> settings;  // Non-sensitive provider-specific settings

    // Constructors
    public Provider() {
    }

    public Provider(String id, String providerType, String accountType, Map<String, Object> settings) {
        this.id = id;
        this.providerType = providerType;
        this.accountType = accountType;
        this.settings = settings;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Provider that = (Provider) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(providerType, that.providerType) &&
               Objects.equals(accountType, that.accountType) &&
               Objects.equals(settings, that.settings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerType, accountType, settings);
    }

    @Override
    public String toString() {
        return "Provider{" +
               "id='" + id + '\'' +
               ", providerType='" + providerType + '\'' +
               ", accountType='" + accountType + '\'' +
               ", settings=" + settings +
               '}';
    }
}
