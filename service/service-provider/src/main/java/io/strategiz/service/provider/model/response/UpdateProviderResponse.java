package io.strategiz.service.provider.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response model for updating provider connections. Contains results of OAuth completion,
 * token refresh, or configuration updates.
 */
public class UpdateProviderResponse {

	// Provider identification
	private String providerId;

	private String providerName;

	private String action; // "oauth_complete", "refresh_token", "update_config",
							// "sync_data"

	// Update result
	private String status; // "success", "failed", "pending", "partial"

	private String message;

	private Boolean operationSuccess;

	// OAuth completion details
	private String accessToken; // Only returned during OAuth completion

	private String refreshToken; // Only returned during OAuth completion

	private String tokenType; // "Bearer", etc.

	private Long expiresIn; // Token expiration in seconds

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant tokenExpiresAt;

	// Account information after update
	private String accountType; // "paper", "live"

	private String accountId;

	private Map<String, Object> accountInfo;

	// Updated connection metadata
	private String connectionType; // "oauth", "api_key"

	private String connectionStatus; // "connected", "disconnected", "pending", "error"

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant connectedAt;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant lastSyncAt;

	// Synchronization results (if action was "sync_data")
	private Map<String, Object> syncResults;

	private Integer recordsUpdated;

	private Integer recordsSkipped;

	private Integer recordsError;

	// Configuration updates (if action was "update_config")
	private Map<String, Object> updatedConfig;

	private List<String> changedFields;

	// Provider capabilities (may change after update)
	private List<String> supportedFeatures;

	private Map<String, Object> rateLimits;

	// Error information (if status is "failed" or "partial")
	private String errorCode;

	private String errorMessage;

	private String errorDetails;

	private List<String> warnings;

	// Response metadata
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Instant responseTimestamp;

	private Long responseTimeMs;

	private Map<String, Object> metadata;

	// Constructors
	public UpdateProviderResponse() {
		this.responseTimestamp = Instant.now();
	}

	public UpdateProviderResponse(String providerId, String providerName, String action) {
		this.providerId = providerId;
		this.providerName = providerName;
		this.action = action;
		this.responseTimestamp = Instant.now();
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

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
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

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(Instant tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public Map<String, Object> getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(Map<String, Object> accountInfo) {
		this.accountInfo = accountInfo;
	}

	public String getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(String connectionType) {
		this.connectionType = connectionType;
	}

	public String getConnectionStatus() {
		return connectionStatus;
	}

	public void setConnectionStatus(String connectionStatus) {
		this.connectionStatus = connectionStatus;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public void setConnectedAt(Instant connectedAt) {
		this.connectedAt = connectedAt;
	}

	public Instant getLastSyncAt() {
		return lastSyncAt;
	}

	public void setLastSyncAt(Instant lastSyncAt) {
		this.lastSyncAt = lastSyncAt;
	}

	public Map<String, Object> getSyncResults() {
		return syncResults;
	}

	public void setSyncResults(Map<String, Object> syncResults) {
		this.syncResults = syncResults;
	}

	public Integer getRecordsUpdated() {
		return recordsUpdated;
	}

	public void setRecordsUpdated(Integer recordsUpdated) {
		this.recordsUpdated = recordsUpdated;
	}

	public Integer getRecordsSkipped() {
		return recordsSkipped;
	}

	public void setRecordsSkipped(Integer recordsSkipped) {
		this.recordsSkipped = recordsSkipped;
	}

	public Integer getRecordsError() {
		return recordsError;
	}

	public void setRecordsError(Integer recordsError) {
		this.recordsError = recordsError;
	}

	public Map<String, Object> getUpdatedConfig() {
		return updatedConfig;
	}

	public void setUpdatedConfig(Map<String, Object> updatedConfig) {
		this.updatedConfig = updatedConfig;
	}

	public List<String> getChangedFields() {
		return changedFields;
	}

	public void setChangedFields(List<String> changedFields) {
		this.changedFields = changedFields;
	}

	public List<String> getSupportedFeatures() {
		return supportedFeatures;
	}

	public void setSupportedFeatures(List<String> supportedFeatures) {
		this.supportedFeatures = supportedFeatures;
	}

	public Map<String, Object> getRateLimits() {
		return rateLimits;
	}

	public void setRateLimits(Map<String, Object> rateLimits) {
		this.rateLimits = rateLimits;
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

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public Instant getResponseTimestamp() {
		return responseTimestamp;
	}

	public void setResponseTimestamp(Instant responseTimestamp) {
		this.responseTimestamp = responseTimestamp;
	}

	public Long getResponseTimeMs() {
		return responseTimeMs;
	}

	public void setResponseTimeMs(Long responseTimeMs) {
		this.responseTimeMs = responseTimeMs;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	// Helper methods
	public boolean isSuccess() {
		return "success".equals(status);
	}

	public boolean isFailed() {
		return "failed".equals(status);
	}

	public boolean isPending() {
		return "pending".equals(status);
	}

	public boolean isPartial() {
		return "partial".equals(status);
	}

	public boolean isOAuthComplete() {
		return "oauth_complete".equals(action);
	}

	public boolean isTokenRefresh() {
		return "refresh_token".equals(action);
	}

	public boolean isConfigUpdate() {
		return "update_config".equals(action);
	}

	public boolean isDataSync() {
		return "sync_data".equals(action);
	}

	public boolean hasTokens() {
		return accessToken != null && refreshToken != null;
	}

	public boolean hasWarnings() {
		return warnings != null && !warnings.isEmpty();
	}

	public boolean hasErrors() {
		return errorCode != null || errorMessage != null;
	}

	public boolean isTokenExpired() {
		return tokenExpiresAt != null && tokenExpiresAt.isBefore(Instant.now());
	}

	// Convenience methods for compatibility
	public void setSuccess(boolean success) {
		this.operationSuccess = success;
		this.status = success ? "success" : "failed";
	}

	public boolean getSuccess() {
		return Boolean.TRUE.equals(operationSuccess);
	}

	public void setData(Map<String, Object> data) {
		this.metadata = data;
	}

	public Map<String, Object> getData() {
		return metadata;
	}

	@Override
	public String toString() {
		return "UpdateProviderResponse{" + "providerId='" + providerId + '\'' + ", providerName='" + providerName + '\''
				+ ", action='" + action + '\'' + ", status='" + status + '\'' + ", operationSuccess=" + operationSuccess
				+ ", hasTokens=" + hasTokens() + ", hasWarnings=" + hasWarnings() + ", hasErrors=" + hasErrors()
				+ ", responseTimestamp=" + responseTimestamp + '}';
	}

}