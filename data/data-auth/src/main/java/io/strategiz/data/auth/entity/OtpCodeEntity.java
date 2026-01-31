package io.strategiz.data.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity for OTP codes stored in otp_codes collection. Used for email and SMS one-time
 * password verification.
 *
 * <p>
 * OTP flow:
 * </p>
 * <ol>
 * <li>User requests OTP → code generated and stored</li>
 * <li>User submits code → verified against stored hash</li>
 * <li>On success, OTP record deleted</li>
 * <li>On failure, attempts incremented</li>
 * <li>Expired OTPs are cleaned up by scheduled job</li>
 * </ol>
 *
 * Inherits audit fields from BaseEntity (createdBy, modifiedBy, createdDate,
 * modifiedDate, isActive, version)
 */
@Entity
@Table(name = "email_otp_codes")
@Collection("email_otp_codes")
public class OtpCodeEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("email")
	@JsonProperty("email")
	@NotNull(message = "Email is required")
	private String email;

	@PropertyName("purpose")
	@JsonProperty("purpose")
	@NotNull(message = "Purpose is required")
	private String purpose;

	@PropertyName("codeHash")
	@JsonProperty("codeHash")
	@NotNull(message = "Code hash is required")
	private String codeHash;

	@PropertyName("expiresAt")
	@JsonProperty("expiresAt")
	@NotNull(message = "Expiration time is required")
	private Instant expiresAt;

	@PropertyName("attempts")
	@JsonProperty("attempts")
	private Integer attempts = 0;

	@PropertyName("maxAttempts")
	@JsonProperty("maxAttempts")
	private Integer maxAttempts = 3;

	@PropertyName("ipAddress")
	@JsonProperty("ipAddress")
	private String ipAddress;

	@PropertyName("userAgent")
	@JsonProperty("userAgent")
	private String userAgent;

	@PropertyName("sessionId")
	@JsonProperty("sessionId")
	private String sessionId;

	@PropertyName("metadata")
	@JsonProperty("metadata")
	private Map<String, String> metadata;

	// Constructors
	public OtpCodeEntity() {
		super();
		this.attempts = 0;
		this.maxAttempts = 3;
	}

	public OtpCodeEntity(String email, String purpose, String codeHash, Instant expiresAt) {
		super();
		this.email = email;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.attempts = 0;
		this.maxAttempts = 3;
	}

	// Getters and Setters
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPurpose() {
		return purpose;
	}

	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public void setCodeHash(String codeHash) {
		this.codeHash = codeHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public Integer getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(Integer maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	// Convenience methods
	public boolean isExpired() {
		return expiresAt != null && Instant.now().isAfter(expiresAt);
	}

	public void incrementAttempts() {
		this.attempts = (this.attempts == null ? 0 : this.attempts) + 1;
	}

	public boolean hasExceededMaxAttempts() {
		return this.attempts != null && this.maxAttempts != null && this.attempts >= this.maxAttempts;
	}

	/**
	 * Generate the composite key for this OTP (email:purpose).
	 */
	public String getCompositeKey() {
		return email + ":" + purpose;
	}

	/**
	 * Create a composite key from email and purpose.
	 */
	public static String createCompositeKey(String email, String purpose) {
		return email + ":" + purpose;
	}

}
