package io.strategiz.business.provider.etrade.model;

import java.time.Instant;
import java.util.Map;

/**
 * Result of E*TRADE OAuth 1.0a connection. Note: E*TRADE uses accessToken +
 * accessTokenSecret (OAuth 1.0a) instead of accessToken + refreshToken (OAuth 2.0).
 */
public class EtradeConnectionResult {

	private String userId;

	private String providerId;

	private String providerName;

	private String accessToken;

	private String accessTokenSecret;

	private Map<String, Object> accountInfo;

	private Instant connectedAt;

	private String status;

	// Getters and setters
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

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAccessTokenSecret() {
		return accessTokenSecret;
	}

	public void setAccessTokenSecret(String accessTokenSecret) {
		this.accessTokenSecret = accessTokenSecret;
	}

	public Map<String, Object> getAccountInfo() {
		return accountInfo;
	}

	public void setAccountInfo(Map<String, Object> accountInfo) {
		this.accountInfo = accountInfo;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public void setConnectedAt(Instant connectedAt) {
		this.connectedAt = connectedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
