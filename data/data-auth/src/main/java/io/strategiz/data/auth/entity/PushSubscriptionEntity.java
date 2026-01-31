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

/**
 * Entity for Web Push subscriptions stored in push_subscriptions collection. Stores the
 * Web Push API subscription data needed to send push notifications.
 *
 * <p>
 * Each subscription represents a browser/device that can receive push notifications.
 * Users can have multiple subscriptions (multiple browsers/devices).
 * </p>
 *
 * <p>
 * Subscription data from Web Push API:
 * </p>
 * <ul>
 * <li>endpoint - The push service URL</li>
 * <li>keys.p256dh - Public key for encryption</li>
 * <li>keys.auth - Auth secret for encryption</li>
 * </ul>
 */
@Entity
@Table(name = "push_subscriptions")
@Collection("push_subscriptions")
public class PushSubscriptionEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("userId")
	@JsonProperty("userId")
	@NotNull(message = "User ID is required")
	private String userId;

	/**
	 * The push service endpoint URL. This is where we send push messages.
	 */
	@PropertyName("endpoint")
	@JsonProperty("endpoint")
	@NotNull(message = "Endpoint is required")
	private String endpoint;

	/**
	 * The p256dh public key for message encryption. Base64 URL-safe encoded.
	 */
	@PropertyName("p256dh")
	@JsonProperty("p256dh")
	@NotNull(message = "P256DH key is required")
	private String p256dh;

	/**
	 * The auth secret for message encryption. Base64 URL-safe encoded.
	 */
	@PropertyName("auth")
	@JsonProperty("auth")
	@NotNull(message = "Auth key is required")
	private String auth;

	/**
	 * User-friendly name for this device/browser. e.g., "Chrome on MacBook", "Firefox on
	 * iPhone"
	 */
	@PropertyName("deviceName")
	@JsonProperty("deviceName")
	private String deviceName;

	/**
	 * Browser user agent string for identification.
	 */
	@PropertyName("userAgent")
	@JsonProperty("userAgent")
	private String userAgent;

	/**
	 * When this subscription was last used successfully.
	 */
	@PropertyName("lastUsedAt")
	@JsonProperty("lastUsedAt")
	private Instant lastUsedAt;

	/**
	 * When this subscription expires (if known from push service).
	 */
	@PropertyName("expiresAt")
	@JsonProperty("expiresAt")
	private Instant expiresAt;

	/**
	 * Whether push auth is enabled for this subscription. User can disable without
	 * removing the subscription.
	 */
	@PropertyName("pushAuthEnabled")
	@JsonProperty("pushAuthEnabled")
	private Boolean pushAuthEnabled = true;

	/**
	 * Count of failed push attempts. Used to detect stale subscriptions.
	 */
	@PropertyName("failedAttempts")
	@JsonProperty("failedAttempts")
	private Integer failedAttempts = 0;

	// Constructors
	public PushSubscriptionEntity() {
		super();
		this.pushAuthEnabled = true;
		this.failedAttempts = 0;
	}

	public PushSubscriptionEntity(String userId, String endpoint, String p256dh, String auth) {
		super();
		this.userId = userId;
		this.endpoint = endpoint;
		this.p256dh = p256dh;
		this.auth = auth;
		this.pushAuthEnabled = true;
		this.failedAttempts = 0;
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

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getP256dh() {
		return p256dh;
	}

	public void setP256dh(String p256dh) {
		this.p256dh = p256dh;
	}

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Boolean getPushAuthEnabled() {
		return pushAuthEnabled;
	}

	public void setPushAuthEnabled(Boolean pushAuthEnabled) {
		this.pushAuthEnabled = pushAuthEnabled;
	}

	public Integer getFailedAttempts() {
		return failedAttempts;
	}

	public void setFailedAttempts(Integer failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	// Convenience methods

	/**
	 * Check if this subscription is likely still valid.
	 */
	public boolean isValid() {
		if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
			return false;
		}
		// Consider invalid after 5 consecutive failures
		return failedAttempts == null || failedAttempts < 5;
	}

	/**
	 * Record a successful push.
	 */
	public void recordSuccess() {
		this.lastUsedAt = Instant.now();
		this.failedAttempts = 0;
	}

	/**
	 * Record a failed push attempt.
	 */
	public void recordFailure() {
		this.failedAttempts = (this.failedAttempts == null ? 0 : this.failedAttempts) + 1;
	}

	/**
	 * Get a hash of the endpoint for quick lookup. Useful for finding existing
	 * subscriptions.
	 */
	public String getEndpointHash() {
		if (endpoint == null) {
			return null;
		}
		return String.valueOf(endpoint.hashCode());
	}

}
