package io.strategiz.data.preferences.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
import io.strategiz.data.base.annotation.Collection;

import java.util.List;

/**
 * Service Account entity for machine-to-machine authentication.
 * Stored in Firestore collection: service_accounts
 *
 * Service accounts enable programmatic API access for:
 * - CI/CD pipelines
 * - Integration testing
 * - External service integrations
 * - Automated scripts
 *
 * Extends BaseEntity which provides audit fields (createdBy, modifiedBy, createdDate, modifiedDate).
 */
@Collection("service_accounts")
public class ServiceAccountEntity extends BaseEntity {

	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	private String id;

	@PropertyName("name")
	@JsonProperty("name")
	private String name; // Human-readable name (e.g., "CI Test Runner")

	@PropertyName("description")
	@JsonProperty("description")
	private String description; // Purpose/usage description

	@PropertyName("clientId")
	@JsonProperty("clientId")
	private String clientId; // Public identifier (UUID)

	@PropertyName("hashedClientSecret")
	@JsonProperty("hashedClientSecret")
	private String hashedClientSecret; // BCrypt hashed secret

	@PropertyName("scopes")
	@JsonProperty("scopes")
	private List<String> scopes; // Authorized scopes (e.g., ["read:strategies", "write:test-results"])

	@PropertyName("enabled")
	@JsonProperty("enabled")
	private boolean enabled = true;

	@PropertyName("lastUsedAt")
	@JsonProperty("lastUsedAt")
	private Timestamp lastUsedAt;

	@PropertyName("lastUsedIp")
	@JsonProperty("lastUsedIp")
	private String lastUsedIp;

	@PropertyName("usageCount")
	@JsonProperty("usageCount")
	private long usageCount = 0;

	// Note: createdBy is inherited from BaseEntity

	@PropertyName("expiresAt")
	@JsonProperty("expiresAt")
	private Timestamp expiresAt; // Optional expiration date

	@PropertyName("ipWhitelist")
	@JsonProperty("ipWhitelist")
	private List<String> ipWhitelist; // Optional IP restrictions

	@PropertyName("rateLimit")
	@JsonProperty("rateLimit")
	private int rateLimit = 100; // Requests per minute

	// Constructors

	public ServiceAccountEntity() {
		super();
	}

	public ServiceAccountEntity(String name, String clientId, String hashedClientSecret, List<String> scopes) {
		super();
		this.name = name;
		this.clientId = clientId;
		this.hashedClientSecret = hashedClientSecret;
		this.scopes = scopes;
		this.enabled = true;
	}

	// =====================================================
	// Getters and Setters
	// =====================================================

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getHashedClientSecret() {
		return hashedClientSecret;
	}

	public void setHashedClientSecret(String hashedClientSecret) {
		this.hashedClientSecret = hashedClientSecret;
	}

	public List<String> getScopes() {
		return scopes;
	}

	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Timestamp getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Timestamp lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public String getLastUsedIp() {
		return lastUsedIp;
	}

	public void setLastUsedIp(String lastUsedIp) {
		this.lastUsedIp = lastUsedIp;
	}

	public long getUsageCount() {
		return usageCount;
	}

	public void setUsageCount(long usageCount) {
		this.usageCount = usageCount;
	}

	// createdBy getter/setter from BaseEntity is used directly

	public Timestamp getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Timestamp expiresAt) {
		this.expiresAt = expiresAt;
	}

	public List<String> getIpWhitelist() {
		return ipWhitelist;
	}

	public void setIpWhitelist(List<String> ipWhitelist) {
		this.ipWhitelist = ipWhitelist;
	}

	public int getRateLimit() {
		return rateLimit;
	}

	public void setRateLimit(int rateLimit) {
		this.rateLimit = rateLimit;
	}

	// =====================================================
	// Convenience Alias Methods (for API compatibility)
	// =====================================================

	/**
	 * Alias for getCreatedDate() from BaseEntity.
	 */
	public Timestamp getCreatedAt() {
		return getCreatedDate();
	}

	/**
	 * Alias for getModifiedDate() from BaseEntity.
	 */
	public Timestamp getUpdatedAt() {
		return getModifiedDate();
	}

	// =====================================================
	// Business Logic Methods
	// =====================================================

	/**
	 * Check if service account is currently valid (enabled and not expired).
	 */
	public boolean isValid() {
		if (!enabled) {
			return false;
		}
		if (expiresAt != null && expiresAt.toDate().before(new java.util.Date())) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the given IP is allowed for this service account.
	 */
	public boolean isIpAllowed(String ip) {
		if (ipWhitelist == null || ipWhitelist.isEmpty()) {
			return true; // No restrictions
		}
		return ipWhitelist.contains(ip);
	}

}
