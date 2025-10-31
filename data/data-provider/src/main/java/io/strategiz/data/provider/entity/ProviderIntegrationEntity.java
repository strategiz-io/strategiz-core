package io.strategiz.data.provider.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.validation.constraints.NotBlank;

/**
 * Provider integration for users/{userId}/provider_integrations subcollection
 * Simplified entity with only essential fields - no redundancy
 */
@Collection("provider_integrations")
public class ProviderIntegrationEntity extends BaseEntity {

    @DocumentId
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

    // Constructors
    public ProviderIntegrationEntity() {
        super();
    }

    public ProviderIntegrationEntity(String providerId, String connectionType) {
        super();
        this.providerId = providerId;
        this.connectionType = connectionType;
        this.status = "connected";
    }

    public ProviderIntegrationEntity(String providerId, String connectionType, String userId) {
        super(userId);
        this.providerId = providerId;
        this.connectionType = connectionType;
        this.status = "connected";
    }

    // Getters and Setters
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

    // Required BaseEntity methods
    @Override
    public String getId() {
        return providerId;
    }

    @Override
    public void setId(String id) {
        this.providerId = id;
    }
}