package io.strategiz.client.google.model;

import java.io.Serializable;

/**
 * Model for Google OAuth token response
 */
public class GoogleTokenResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String accessToken;

	private final String tokenType;

	private final Integer expiresIn;

	private final String refreshToken;

	private final String scope;

	public GoogleTokenResponse(String accessToken, String tokenType, Integer expiresIn) {
		this(accessToken, tokenType, expiresIn, null, null);
	}

	public GoogleTokenResponse(String accessToken, String tokenType, Integer expiresIn, String refreshToken,
			String scope) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
		this.refreshToken = refreshToken;
		this.scope = scope;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public String getScope() {
		return scope;
	}

	@Override
	public String toString() {
		return "GoogleTokenResponse{" + "tokenType='" + tokenType + '\'' + ", expiresIn=" + expiresIn + ", scope='"
				+ scope + '\'' + '}';
	}

}