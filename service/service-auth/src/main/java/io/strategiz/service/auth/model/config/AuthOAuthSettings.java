package io.strategiz.service.auth.model.config;

/**
 * Authentication OAuth provider settings
 */
public class AuthOAuthSettings {

	private String clientId;

	private String clientSecret;

	private String redirectUri;

	private String signupRedirectUri;

	private String authUrl;

	private String tokenUrl;

	private String userInfoUrl;

	private String scope;

	// Default constructor
	public AuthOAuthSettings() {
	}

	// Copy constructor
	public AuthOAuthSettings(AuthOAuthSettings other) {
		this.clientId = other.clientId;
		this.clientSecret = other.clientSecret;
		this.redirectUri = other.redirectUri;
		this.signupRedirectUri = other.signupRedirectUri;
		this.authUrl = other.authUrl;
		this.tokenUrl = other.tokenUrl;
		this.userInfoUrl = other.userInfoUrl;
		this.scope = other.scope;
	}

	// Getters and setters
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	public String getSignupRedirectUri() {
		return signupRedirectUri;
	}

	public void setSignupRedirectUri(String signupRedirectUri) {
		this.signupRedirectUri = signupRedirectUri;
	}

	public String getAuthUrl() {
		return authUrl;
	}

	public void setAuthUrl(String authUrl) {
		this.authUrl = authUrl;
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public String getUserInfoUrl() {
		return userInfoUrl;
	}

	public void setUserInfoUrl(String userInfoUrl) {
		this.userInfoUrl = userInfoUrl;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

}