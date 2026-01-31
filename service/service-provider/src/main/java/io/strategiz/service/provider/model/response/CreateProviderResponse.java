package io.strategiz.service.provider.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Map;

/**
 * Response model for creating a new provider connection. Contains the result of
 * connection initiation (OAuth URL or API key setup confirmation).
 */
public class CreateProviderResponse {

	private String providerId;

	private String providerName;

	private String connectionType;

	private String status; // "initiated", "pending", "requires_oauth", "requires_api_key"

	private String message; // General status message

	private Boolean operationSuccess; // True if operation was successful

	// Error fields
	private String errorCode;

	private String errorMessage;

	// OAuth-specific fields
	private String authorizationUrl;

	private String oauthUrl; // Alias for authorizationUrl

	private String state;

	private String oauthInstructions;

	private Long expiresIn; // OAuth token expiration in seconds

	// Connection fields
	private String accountType; // "paper", "live"

	private Map<String, Object> connectionData;

	private Map<String, Object> instructions; // Step-by-step instructions

	// API Key-specific fields
	private String[] requiredFields;

	private String apiKeyInstructions;

	private boolean credentialsReceived;

	// General fields
	private String connectionId; // Unique identifier for this connection attempt

	private String flowType; // "oauth" or "api_key"

	private String nextStep; // Instructions for the next step

	private String redirectUrl; // Where to redirect after completion

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant createdAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant expiresAt; // When the connection attempt expires

	private Map<String, Object> metadata; // Additional provider-specific data

	// Constructors
	public CreateProviderResponse() {
		this.createdAt = Instant.now();
	}

	public CreateProviderResponse(String providerId, String providerName, String connectionType) {
		this.providerId = providerId;
		this.providerName = providerName;
		this.connectionType = connectionType;
		this.createdAt = Instant.now();
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

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
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

	public Boolean getOperationSuccess() {
		return operationSuccess;
	}

	public void setOperationSuccess(Boolean operationSuccess) {
		this.operationSuccess = operationSuccess;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getAuthorizationUrl() {
		return authorizationUrl;
	}

	public void setAuthorizationUrl(String authorizationUrl) {
		this.authorizationUrl = authorizationUrl;
	}

	public String getOauthUrl() {
		return oauthUrl;
	}

	public void setOauthUrl(String oauthUrl) {
		this.oauthUrl = oauthUrl;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getOauthInstructions() {
		return oauthInstructions;
	}

	public void setOauthInstructions(String oauthInstructions) {
		this.oauthInstructions = oauthInstructions;
	}

	public String[] getRequiredFields() {
		return requiredFields;
	}

	public void setRequiredFields(String[] requiredFields) {
		this.requiredFields = requiredFields;
	}

	public String getApiKeyInstructions() {
		return apiKeyInstructions;
	}

	public void setApiKeyInstructions(String apiKeyInstructions) {
		this.apiKeyInstructions = apiKeyInstructions;
	}

	public boolean isCredentialsReceived() {
		return credentialsReceived;
	}

	public void setCredentialsReceived(boolean credentialsReceived) {
		this.credentialsReceived = credentialsReceived;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getFlowType() {
		return flowType;
	}

	public void setFlowType(String flowType) {
		this.flowType = flowType;
	}

	public String getNextStep() {
		return nextStep;
	}

	public void setNextStep(String nextStep) {
		this.nextStep = nextStep;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public Map<String, Object> getConnectionData() {
		return connectionData;
	}

	public void setConnectionData(Map<String, Object> connectionData) {
		this.connectionData = connectionData;
	}

	public Map<String, Object> getInstructions() {
		return instructions;
	}

	public void setInstructions(Map<String, Object> instructions) {
		this.instructions = instructions;
	}

	// Helper methods
	public boolean isOAuthFlow() {
		return "oauth".equals(flowType);
	}

	public boolean isApiKeyFlow() {
		return "api_key".equals(flowType);
	}

	public boolean requiresUserAction() {
		return "requires_oauth".equals(status) || "requires_api_key".equals(status);
	}

	public boolean isExpired() {
		return expiresAt != null && Instant.now().isAfter(expiresAt);
	}

	@Override
	public String toString() {
		return "CreateProviderResponse{" + "providerId='" + providerId + '\'' + ", providerName='" + providerName + '\''
				+ ", connectionType='" + connectionType + '\'' + ", status='" + status + '\'' + ", flowType='"
				+ flowType + '\'' + ", connectionId='" + connectionId + '\'' + ", createdAt=" + createdAt
				+ ", expiresAt=" + expiresAt + '}';
	}

}