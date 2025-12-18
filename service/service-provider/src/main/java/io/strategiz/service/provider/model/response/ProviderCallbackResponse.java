package io.strategiz.service.provider.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Response model for provider OAuth callback processing.
 * Contains the result of OAuth authorization and connection establishment.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderCallbackResponse {
    
    @JsonProperty("provider_id")
    private String providerId;
    
    @JsonProperty("provider_name")
    private String providerName;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("redirect_url")
    private String redirectUrl;
    
    @JsonProperty("connected_at")
    private Instant connectedAt;
    
    @JsonProperty("connection_data")
    private Map<String, Object> connectionData;
    
    @JsonProperty("operation_success")
    private boolean operationSuccess;
    
    @JsonProperty("error_code")
    private String errorCode;
    
    @JsonProperty("error_description")
    private String errorDescription;

    // Access token for setting HTTP-only cookie (not serialized to JSON)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private transient String accessToken;

    // Constructors
    public ProviderCallbackResponse() {
        this.operationSuccess = false;
    }
    
    // Getters and Setters
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    public Instant getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(Instant connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    public Map<String, Object> getConnectionData() {
        return connectionData;
    }
    
    public void setConnectionData(Map<String, Object> connectionData) {
        this.connectionData = connectionData;
    }
    
    public boolean isOperationSuccess() {
        return operationSuccess;
    }
    
    public void setOperationSuccess(boolean operationSuccess) {
        this.operationSuccess = operationSuccess;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorDescription() {
        return errorDescription;
    }
    
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "ProviderCallbackResponse{" +
                "providerId='" + providerId + '\'' +
                ", providerName='" + providerName + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", operationSuccess=" + operationSuccess +
                ", connectedAt=" + connectedAt +
                '}';
    }
}