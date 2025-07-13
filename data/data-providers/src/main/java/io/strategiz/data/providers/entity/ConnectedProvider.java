package io.strategiz.data.providers.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Map;

/**
 * Connected provider for users/{userId}/providers subcollection
 * Represents external service connections (trading platforms, APIs, etc.)
 */
public class ConnectedProvider extends BaseEntity {

    @DocumentId
    @PropertyName("providerId")
    @JsonProperty("providerId")
    private String providerId;

    @PropertyName("providerType")
    @JsonProperty("providerType")
    @NotBlank(message = "Provider type is required")
    private String providerType; // TRADING, CRYPTO, BANK, OAUTH, etc.

    @PropertyName("providerName")
    @JsonProperty("providerName")
    @NotBlank(message = "Provider name is required")
    private String providerName; // alpaca, tdameritrade, coinbase, etc.

    @PropertyName("displayName")
    @JsonProperty("displayName")
    private String displayName; // User-friendly name

    @PropertyName("status")
    @JsonProperty("status")
    private String status = "CONNECTED"; // CONNECTED, DISCONNECTED, ERROR, EXPIRED

    @PropertyName("connectionType")
    @JsonProperty("connectionType")
    private String connectionType; // API_KEY, OAUTH, PLAID, etc.

    @PropertyName("lastSyncAt")
    @JsonProperty("lastSyncAt")
    private Instant lastSyncAt;

    @PropertyName("lastErrorAt")
    @JsonProperty("lastErrorAt")
    private Instant lastErrorAt;

    @PropertyName("errorMessage")
    @JsonProperty("errorMessage")
    private String errorMessage;

    @PropertyName("capabilities")
    @JsonProperty("capabilities")
    private String[] capabilities; // READ, TRADE, PORTFOLIO, etc.

    @PropertyName("metadata")
    @JsonProperty("metadata")
    private Map<String, Object> metadata; // Provider-specific data

    // Constructors
    public ConnectedProvider() {
        super();
    }

    public ConnectedProvider(String providerType, String providerName) {
        super();
        this.providerType = providerType;
        this.providerName = providerName;
        this.displayName = providerName;
        this.status = "CONNECTED";
    }

    public ConnectedProvider(String providerType, String providerName, String displayName, String connectionType) {
        super();
        this.providerType = providerType;
        this.providerName = providerName;
        this.displayName = displayName;
        this.connectionType = connectionType;
        this.status = "CONNECTED";
    }

    // Getters and Setters
    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }

    public void setLastErrorAt(Instant lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String[] getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String[] capabilities) {
        this.capabilities = capabilities;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Convenience methods
    public boolean isConnected() {
        return "CONNECTED".equals(status);
    }

    public boolean hasError() {
        return "ERROR".equals(status);
    }

    public boolean isExpired() {
        return "EXPIRED".equals(status);
    }

    public void markAsError(String errorMessage) {
        this.status = "ERROR";
        this.errorMessage = errorMessage;
        this.lastErrorAt = Instant.now();
    }

    public void markAsConnected() {
        this.status = "CONNECTED";
        this.errorMessage = null;
        this.lastErrorAt = null;
        this.lastSyncAt = Instant.now();
    }

    public void markAsDisconnected() {
        this.status = "DISCONNECTED";
    }

    // Required BaseEntity methods
    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public void setId(String id) {
        this.providerId = id;
    }

    @Override
    public String getCollectionName() {
        return "providers";
    }
}