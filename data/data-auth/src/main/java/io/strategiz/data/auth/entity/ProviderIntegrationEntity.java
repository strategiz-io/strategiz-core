package io.strategiz.data.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity for provider integrations stored in users/{userId}/provider_integrations subcollection
 * Stores metadata about provider connections (credentials stored separately in Vault)
 */
@Entity
@Table(name = "provider_integrations")
public class ProviderIntegrationEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    @Column(name = "id")
    private String id;

    @PropertyName("providerId")
    @JsonProperty("providerId")
    @NotNull(message = "Provider ID is required")
    private String providerId; // e.g., "kraken", "binanceus"

    @PropertyName("providerName")
    @JsonProperty("providerName")
    @NotNull(message = "Provider name is required")
    private String providerName; // e.g., "Kraken", "Binance US"

    @PropertyName("providerType")
    @JsonProperty("providerType")
    @NotNull(message = "Provider type is required")
    private String providerType; // e.g., "exchange", "brokerage"

    @PropertyName("status")
    @JsonProperty("status")
    private String status = "connected"; // connected, disconnected, error

    @PropertyName("isEnabled")
    @JsonProperty("isEnabled")
    private boolean isEnabled = true;

    @PropertyName("supportsTrading")
    @JsonProperty("supportsTrading")
    private boolean supportsTrading = false;

    @PropertyName("permissions")
    @JsonProperty("permissions")
    private List<String> permissions; // e.g., ["read", "trade"]

    @PropertyName("connectedAt")
    @JsonProperty("connectedAt")
    private Instant connectedAt;

    @PropertyName("lastTestedAt")
    @JsonProperty("lastTestedAt")
    private Instant lastTestedAt;

    @PropertyName("lastUsedAt")
    @JsonProperty("lastUsedAt")
    private Instant lastUsedAt;

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Provider-specific metadata

    // Constructors
    public ProviderIntegrationEntity() {
        super();
        this.metadata = new HashMap<>();
        this.connectedAt = Instant.now();
    }

    public ProviderIntegrationEntity(String providerId, String providerName, String providerType) {
        super();
        this.providerId = providerId;
        this.providerName = providerName;
        this.providerType = providerType;
        this.metadata = new HashMap<>();
        this.connectedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isSupportsTrading() {
        return supportsTrading;
    }

    public void setSupportsTrading(boolean supportsTrading) {
        this.supportsTrading = supportsTrading;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }

    public Instant getLastTestedAt() {
        return lastTestedAt;
    }

    public void setLastTestedAt(Instant lastTestedAt) {
        this.lastTestedAt = lastTestedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    public void markAsTested() {
        this.lastTestedAt = Instant.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.status = "disconnected";
    }

    public void enable() {
        this.isEnabled = true;
        this.status = "connected";
    }

    public void markAsError(String errorMessage) {
        this.status = "error";
        putMetadata("lastError", errorMessage);
        putMetadata("lastErrorAt", Instant.now().toString());
    }

    public boolean isRecentlyUsed(long withinSeconds) {
        if (lastUsedAt == null) {
            return false;
        }
        return lastUsedAt.isAfter(Instant.now().minusSeconds(withinSeconds));
    }

    public boolean isRecentlyTested(long withinSeconds) {
        if (lastTestedAt == null) {
            return false;
        }
        return lastTestedAt.isAfter(Instant.now().minusSeconds(withinSeconds));
    }

    // Metadata convenience methods
    public void putMetadata(String key, Object value) {
        getMetadata().put(key, value);
    }

    public Object getMetadata(String key) {
        return getMetadata().get(key);
    }

    public String getMetadataAsString(String key) {
        Object value = getMetadata(key);
        return value != null ? value.toString() : null;
    }

    // Required BaseEntity methods
    @Override
    public String getCollectionName() {
        return "provider_integrations";
    }

    // Provider-specific behavior
    public boolean isConnected() {
        return "connected".equals(status) && isEnabled;
    }

    public boolean hasError() {
        return "error".equals(status);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean canTrade() {
        return isConnected() && supportsTrading && hasPermission("trade");
    }

    public boolean canRead() {
        return isConnected() && hasPermission("read");
    }
}