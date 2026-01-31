package io.strategiz.business.provider.robinhood.model;

import io.strategiz.client.robinhood.model.RobinhoodChallenge;

import java.time.Instant;
import java.util.Map;

/**
 * Result of Robinhood OAuth connection. Handles the multi-step authentication flow
 * including MFA.
 */
public class RobinhoodConnectionResult {

	public enum ConnectionStatus {

		SUCCESS, // Fully connected with tokens
		MFA_REQUIRED, // Need MFA code from user
		DEVICE_APPROVAL, // Need device approval via Robinhood app
		PENDING_MFA, // MFA submitted, awaiting completion
		ERROR // Connection failed

	}

	private String userId;

	private String providerId;

	private String providerName;

	private ConnectionStatus connectionStatus;

	// Token data (only set on SUCCESS)
	private String accessToken;

	private String refreshToken;

	private Instant expiresAt;

	// MFA data (only set when MFA_REQUIRED)
	private RobinhoodChallenge challenge;

	private String challengeType;

	private String deviceToken;

	// Account info (only set on SUCCESS)
	private Map<String, Object> accountInfo;

	// Timestamps
	private Instant connectedAt;

	// Error info
	private String errorMessage;

	private String errorCode;

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

	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	public void setConnectionStatus(ConnectionStatus connectionStatus) {
		this.connectionStatus = connectionStatus;
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

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public RobinhoodChallenge getChallenge() {
		return challenge;
	}

	public void setChallenge(RobinhoodChallenge challenge) {
		this.challenge = challenge;
	}

	public String getChallengeType() {
		return challengeType;
	}

	public void setChallengeType(String challengeType) {
		this.challengeType = challengeType;
	}

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	// Convenience methods
	public boolean isSuccess() {
		return connectionStatus == ConnectionStatus.SUCCESS;
	}

	public boolean isMfaRequired() {
		return connectionStatus == ConnectionStatus.MFA_REQUIRED;
	}

	public boolean isDeviceApprovalRequired() {
		return connectionStatus == ConnectionStatus.DEVICE_APPROVAL;
	}

	public boolean isError() {
		return connectionStatus == ConnectionStatus.ERROR;
	}

}
