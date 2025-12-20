package io.strategiz.data.provider.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

/**
 * Provider integration entity - root-level collection
 * Collection: provider_integrations/{integrationId}
 *
 * Subcollection: provider_integrations/{integrationId}/provider_data/{dataId}
 *
 * This entity represents a user's connection to an external provider (exchange/brokerage).
 * Provider data (balances, transactions, etc.) is stored as a subcollection under this entity.
 */
@Collection("provider_integrations")
public class ProviderIntegrationEntity extends BaseEntity {

    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    private String id;

    @PropertyName("userId")
    @JsonProperty("userId")
    @NotBlank(message = "User ID is required")
    private String userId;

    @PropertyName("providerId")
    @JsonProperty("providerId")
    @NotBlank(message = "Provider ID is required")
    private String providerId; // e.g., "kraken", "coinbase", "alpaca"

    @PropertyName("status")
    @JsonProperty("status")
    private String status = "connected"; // Status of the integration - must be lowercase for consistency

    @PropertyName("connectionType")
    @JsonProperty("connectionType")
    @NotBlank(message = "Connection type is required")
    private String connectionType; // "oauth" or "api_key"

    @PropertyName("environment")
    @JsonProperty("environment")
    private String environment; // For providers with multiple environments (e.g., "paper", "live")

    @PropertyName("errorMessage")
    @JsonProperty("errorMessage")
    private String errorMessage; // Error message when status is "error"

    // Constructors
    public ProviderIntegrationEntity() {
        super();
    }

    public ProviderIntegrationEntity(String userId, String providerId, String connectionType) {
        super(userId);
        this.userId = userId;
        this.providerId = providerId;
        this.connectionType = connectionType;
        this.status = "connected";
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    @JsonProperty("status")
    @PropertyName("status")
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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Mark this integration as disconnected (soft delete alternative for explicit status)
     */
    public void disconnect() {
        this.status = "disconnected";
    }

    /**
     * Mark this integration as having an error
     */
    public void setError(String errorMessage) {
        this.status = "error";
        this.errorMessage = errorMessage;
    }

    /**
     * Check if this integration is connected and active
     */
    public boolean isConnected() {
        return "connected".equalsIgnoreCase(this.status) && Boolean.TRUE.equals(getIsActive());
    }
}
