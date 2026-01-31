package io.strategiz.data.auth.model.session;

import java.time.Instant;
import java.util.Map;

/**
 * PASETO token model for business layer This is a business domain object that represents
 * a PASETO token
 */
public class PasetoToken {

	private String id;

	private String userId;

	private String tokenType;

	private String tokenValue;

	private long issuedAt;

	private long expiresAt;

	private String deviceId;

	private String issuedFrom;

	private boolean revoked;

	private long revokedAt;

	private String revocationReason;

	private Map<String, Object> claims;

	// === CONSTRUCTORS ===

	public PasetoToken() {
		this.revoked = false;
	}

	// === BUILDER PATTERN ===

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private PasetoToken token = new PasetoToken();

		public Builder id(String id) {
			token.id = id;
			return this;
		}

		public Builder userId(String userId) {
			token.userId = userId;
			return this;
		}

		public Builder tokenType(String tokenType) {
			token.tokenType = tokenType;
			return this;
		}

		public Builder tokenValue(String tokenValue) {
			token.tokenValue = tokenValue;
			return this;
		}

		public Builder issuedAt(long issuedAt) {
			token.issuedAt = issuedAt;
			return this;
		}

		public Builder expiresAt(long expiresAt) {
			token.expiresAt = expiresAt;
			return this;
		}

		public Builder deviceId(String deviceId) {
			token.deviceId = deviceId;
			return this;
		}

		public Builder issuedFrom(String issuedFrom) {
			token.issuedFrom = issuedFrom;
			return this;
		}

		public Builder revoked(boolean revoked) {
			token.revoked = revoked;
			return this;
		}

		public Builder claims(Map<String, Object> claims) {
			token.claims = claims;
			return this;
		}

		public PasetoToken build() {
			return token;
		}

	}

	// === CONVENIENCE METHODS ===

	public boolean isValid() {
		return !revoked && (expiresAt == 0 || Instant.ofEpochSecond(expiresAt).isAfter(Instant.now()));
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public void setRevokedAt(long revokedAt) {
		this.revokedAt = revokedAt;
	}

	public void setRevocationReason(String revocationReason) {
		this.revocationReason = revocationReason;
	}

	// === GETTERS AND SETTERS ===

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getTokenValue() {
		return tokenValue;
	}

	public void setTokenValue(String tokenValue) {
		this.tokenValue = tokenValue;
	}

	public long getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(long issuedAt) {
		this.issuedAt = issuedAt;
	}

	public long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getIssuedFrom() {
		return issuedFrom;
	}

	public void setIssuedFrom(String issuedFrom) {
		this.issuedFrom = issuedFrom;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public long getRevokedAt() {
		return revokedAt;
	}

	public String getRevocationReason() {
		return revocationReason;
	}

	public Map<String, Object> getClaims() {
		return claims;
	}

	public void setClaims(Map<String, Object> claims) {
		this.claims = claims;
	}

}