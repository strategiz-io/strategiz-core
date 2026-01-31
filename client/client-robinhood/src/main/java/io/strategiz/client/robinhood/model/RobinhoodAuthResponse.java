package io.strategiz.client.robinhood.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Robinhood OAuth token endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobinhoodAuthResponse {

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("refresh_token")
	private String refreshToken;

	@JsonProperty("expires_in")
	private Integer expiresIn;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("scope")
	private String scope;

	@JsonProperty("mfa_code")
	private String mfaCode;

	@JsonProperty("backup_code")
	private String backupCode;

	// Getters and setters
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

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getMfaCode() {
		return mfaCode;
	}

	public void setMfaCode(String mfaCode) {
		this.mfaCode = mfaCode;
	}

	public String getBackupCode() {
		return backupCode;
	}

	public void setBackupCode(String backupCode) {
		this.backupCode = backupCode;
	}

}
