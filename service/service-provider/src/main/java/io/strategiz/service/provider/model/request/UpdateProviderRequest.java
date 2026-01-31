package io.strategiz.service.provider.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * Request model for updating provider configurations. Used for completing OAuth flows,
 * refreshing tokens, updating settings, etc.
 */
public class UpdateProviderRequest {

	// Identity fields
	private String userId;

	private String providerId;

	@NotBlank(message = "Action is required")
	@Pattern(regexp = "^(complete_oauth|refresh_token|update_config|test_connection|reconnect)$",
			message = "Action must be one of: complete_oauth, refresh_token, update_config, test_connection, reconnect")
	private String action;

	// OAuth completion fields
	private String code; // Authorization code from OAuth callback

	private String state; // State parameter for OAuth validation

	// Token refresh fields
	private String refreshToken;

	private String accessToken; // For testing connections

	// Configuration update fields
	private String accountType; // paper, live

	private boolean enableNotifications;

	private boolean enableAutoRefresh;

	private String[] permissions; // Specific permissions to request

	// API key update fields (for API key providers)
	private String apiKey;

	private String apiSecret;

	private String passphrase;

	// Connection settings
	private String endpoint; // Custom API endpoint

	private Integer timeoutSeconds;

	private Integer rateLimitPerMinute;

	// Additional provider-specific configuration
	private Map<String, Object> additionalConfig;

	// Metadata
	private String reason; // Reason for the update (for audit logs)

	private boolean forceUpdate = false; // Override safety checks

	// Constructors
	public UpdateProviderRequest() {
	}

	public UpdateProviderRequest(String action) {
		this.action = action;
	}

	public UpdateProviderRequest(String action, String code, String state) {
		this.action = action;
		this.code = code;
		this.state = state;
	}

	// Getters and Setters
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public boolean isEnableNotifications() {
		return enableNotifications;
	}

	public void setEnableNotifications(boolean enableNotifications) {
		this.enableNotifications = enableNotifications;
	}

	public boolean isEnableAutoRefresh() {
		return enableAutoRefresh;
	}

	public void setEnableAutoRefresh(boolean enableAutoRefresh) {
		this.enableAutoRefresh = enableAutoRefresh;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String[] permissions) {
		this.permissions = permissions;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public Integer getRateLimitPerMinute() {
		return rateLimitPerMinute;
	}

	public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
		this.rateLimitPerMinute = rateLimitPerMinute;
	}

	public Map<String, Object> getAdditionalConfig() {
		return additionalConfig;
	}

	public void setAdditionalConfig(Map<String, Object> additionalConfig) {
		this.additionalConfig = additionalConfig;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isForceUpdate() {
		return forceUpdate;
	}

	public void setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
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

	// Convenience methods for OAuth
	public String getAuthorizationCode() {
		return code;
	}

	public void setAuthorizationCode(String code) {
		this.code = code;
	}

	public String[] getScopes() {
		return permissions;
	}

	public void setScopes(String[] scopes) {
		this.permissions = scopes;
	}

	public Map<String, Object> getConfig() {
		return additionalConfig;
	}

	public void setConfig(Map<String, Object> config) {
		this.additionalConfig = config;
	}

	// Helper methods
	public boolean isOAuthCompletion() {
		return "complete_oauth".equals(action);
	}

	public boolean isTokenRefresh() {
		return "refresh_token".equals(action);
	}

	public boolean isConfigUpdate() {
		return "update_config".equals(action);
	}

	public boolean isConnectionTest() {
		return "test_connection".equals(action);
	}

	public boolean isReconnect() {
		return "reconnect".equals(action);
	}

	public boolean hasOAuthData() {
		return code != null && !code.trim().isEmpty() && state != null && !state.trim().isEmpty();
	}

	public boolean hasApiCredentials() {
		return apiKey != null && !apiKey.trim().isEmpty() && apiSecret != null && !apiSecret.trim().isEmpty();
	}

	@Override
	public String toString() {
		return "UpdateProviderRequest{" + "action='" + action + '\'' + ", hasOAuthData=" + hasOAuthData()
				+ ", hasApiCredentials=" + hasApiCredentials() + ", accountType='" + accountType + '\''
				+ ", enableNotifications=" + enableNotifications + ", enableAutoRefresh=" + enableAutoRefresh
				+ ", forceUpdate=" + forceUpdate + ", reason='" + reason + '\'' + '}';
	}

}