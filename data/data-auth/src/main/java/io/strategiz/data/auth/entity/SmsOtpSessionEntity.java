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
import java.time.temporal.ChronoUnit;

/**
 * Entity for SMS OTP sessions stored in Firestore.
 *
 * <p>Industry standard configuration:</p>
 * <ul>
 *   <li>Expiration: 5 minutes (standard for financial services)</li>
 *   <li>Max attempts: 5 verification attempts</li>
 *   <li>Rate limiting: 60 seconds between SMS requests</li>
 * </ul>
 *
 * <p>OTP flow:</p>
 * <ol>
 *   <li>User requests OTP → code generated and stored with hash</li>
 *   <li>User submits code → verified against stored hash</li>
 *   <li>On success, session marked verified and deleted</li>
 *   <li>On failure, attempts incremented</li>
 *   <li>Expired sessions cleaned up by scheduled job</li>
 * </ol>
 *
 * Inherits audit fields from BaseEntity (createdBy, modifiedBy, createdDate, modifiedDate, isActive, version)
 */
@Entity
@Table(name = "sms_otp_sessions")
@Collection("sms_otp_sessions")
public class SmsOtpSessionEntity extends BaseEntity {

	/** Industry standard OTP expiry time in minutes */
	public static final int DEFAULT_EXPIRY_MINUTES = 5;

	/** Industry standard max verification attempts */
	public static final int DEFAULT_MAX_ATTEMPTS = 5;

	/** Rate limit in seconds between SMS requests */
	public static final int RATE_LIMIT_SECONDS = 60;

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("phoneNumber")
	@JsonProperty("phoneNumber")
	@NotNull(message = "Phone number is required")
	private String phoneNumber;

	@PropertyName("countryCode")
	@JsonProperty("countryCode")
	private String countryCode;

	@PropertyName("userId")
	@JsonProperty("userId")
	private String userId;

	@PropertyName("purpose")
	@JsonProperty("purpose")
	@NotNull(message = "Purpose is required")
	private String purpose; // "registration" or "authentication"

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
	private Integer maxAttempts = DEFAULT_MAX_ATTEMPTS;

	@PropertyName("verified")
	@JsonProperty("verified")
	private Boolean verified = false;

	@PropertyName("ipAddress")
	@JsonProperty("ipAddress")
	private String ipAddress;

	@PropertyName("userAgent")
	@JsonProperty("userAgent")
	private String userAgent;

	// Constructors
	public SmsOtpSessionEntity() {
		super();
		this.attempts = 0;
		this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
		this.verified = false;
	}

	public SmsOtpSessionEntity(String phoneNumber, String countryCode, String codeHash, String purpose) {
		this();
		this.phoneNumber = phoneNumber;
		this.countryCode = countryCode;
		this.codeHash = codeHash;
		this.purpose = purpose;
		this.expiresAt = Instant.now().plus(DEFAULT_EXPIRY_MINUTES, ChronoUnit.MINUTES);
	}

	public SmsOtpSessionEntity(String phoneNumber, String countryCode, String codeHash, String purpose,
			int expiryMinutes) {
		this();
		this.phoneNumber = phoneNumber;
		this.countryCode = countryCode;
		this.codeHash = codeHash;
		this.purpose = purpose;
		this.expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
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

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public Boolean getVerified() {
		return verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
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

	// Convenience methods

	/**
	 * Check if this OTP session has expired.
	 */
	public boolean isExpired() {
		return expiresAt != null && Instant.now().isAfter(expiresAt);
	}

	/**
	 * Check if this OTP session is still valid (not expired and not verified).
	 */
	public boolean isValid() {
		return !isExpired() && !Boolean.TRUE.equals(verified);
	}

	/**
	 * Increment verification attempts.
	 */
	public void incrementAttempts() {
		this.attempts = (this.attempts == null ? 0 : this.attempts) + 1;
	}

	/**
	 * Check if maximum attempts have been exceeded.
	 */
	public boolean hasExceededMaxAttempts() {
		return this.attempts != null && this.maxAttempts != null && this.attempts >= this.maxAttempts;
	}

	/**
	 * Get remaining verification attempts.
	 */
	public int getRemainingAttempts() {
		if (maxAttempts == null || attempts == null) {
			return DEFAULT_MAX_ATTEMPTS;
		}
		return Math.max(0, maxAttempts - attempts);
	}

	/**
	 * Get time remaining until expiration in seconds.
	 */
	public long getSecondsUntilExpiry() {
		if (isExpired()) {
			return 0;
		}
		return ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
	}

	/**
	 * Mark this session as verified.
	 */
	public void markVerified() {
		this.verified = true;
	}

	/**
	 * Check if this is a registration session.
	 */
	public boolean isRegistration() {
		return "registration".equals(purpose);
	}

	/**
	 * Check if this is an authentication session.
	 */
	public boolean isAuthentication() {
		return "authentication".equals(purpose);
	}

	/**
	 * Get masked phone number for logging (e.g., +1***1234).
	 */
	public String getMaskedPhoneNumber() {
		if (phoneNumber == null || phoneNumber.length() < 8) {
			return "***-***-****";
		}

		String countryPrefix = phoneNumber.substring(0, phoneNumber.length() - 7);
		String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
		return countryPrefix + "***" + lastFour;
	}

	/**
	 * Purpose constants for type safety.
	 */
	public static final class Purpose {

		public static final String REGISTRATION = "registration";

		public static final String AUTHENTICATION = "authentication";

		private Purpose() {
		}

	}

}
