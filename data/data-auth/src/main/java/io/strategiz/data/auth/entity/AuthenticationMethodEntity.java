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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity for security/authentication methods stored in users/{userId}/security
 * subcollection Inherits audit fields from BaseEntity (createdBy, modifiedBy,
 * createdDate, modifiedDate, isActive, version)
 */
@Entity
@Table(name = "security")
@Collection("security")
public class AuthenticationMethodEntity extends BaseEntity {

	@Id
	@DocumentId
	@PropertyName("id")
	@JsonProperty("id")
	@Column(name = "id")
	private String id;

	@PropertyName("authentication_method")
	@JsonProperty("authentication_method")
	@Enumerated(EnumType.STRING)
	@NotNull(message = "Authentication method type is required")
	private AuthenticationMethodType authenticationMethod;

	@PropertyName("name")
	@JsonProperty("name")
	private String name; // User-friendly name

	@PropertyName("lastUsedAt")
	@JsonProperty("lastUsedAt")
	private Instant lastUsedAt;

	@PropertyName("metadata")
	@JsonProperty("metadata")
	private Map<String, Object> metadata; // Type-specific metadata

	// Constructors
	public AuthenticationMethodEntity() {
		super();
		this.metadata = new HashMap<>();
	}

	public AuthenticationMethodEntity(AuthenticationMethodType authenticationMethod, String name) {
		super();
		this.authenticationMethod = authenticationMethod;
		this.name = name;
		this.metadata = new HashMap<>();
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public AuthenticationMethodType getAuthenticationMethod() {
		return authenticationMethod;
	}

	public void setAuthenticationMethod(AuthenticationMethodType authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	// Backward compatibility - alias for getAuthenticationMethod()
	public AuthenticationMethodType getType() {
		return authenticationMethod;
	}

	// Backward compatibility - alias for setAuthenticationMethod()
	public void setType(AuthenticationMethodType type) {
		this.authenticationMethod = type;
	}

	// New recommended alias - shorter and clearer
	public AuthenticationMethodType getAuthenticationType() {
		return authenticationMethod;
	}

	public void setAuthenticationType(AuthenticationMethodType type) {
		this.authenticationMethod = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public void setLastUsedAt(Instant lastUsedAt) {
		this.lastUsedAt = lastUsedAt;
	}

	public Map<String, Object> getMetadata() {
		if (metadata == null) {
			metadata = new HashMap<>();
		}
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	// Convenience methods
	public void markAsUsed() {
		this.lastUsedAt = Instant.now();
	}

	public boolean isRecentlyUsed(long withinSeconds) {
		if (lastUsedAt == null) {
			return false;
		}
		return lastUsedAt.isAfter(Instant.now().minusSeconds(withinSeconds));
	}

	// Metadata convenience methods
	public void putMetadata(String key, Object value) {
		getMetadata().put(key, value);
	}

	public Object getMetadata(String key) {
		return getMetadata().get(key);
	}

	public String getMetadataAsString(String key) {
		Object value = getMetadata(key);
		return value != null ? value.toString() : null;
	}

	// Required BaseEntity methods

	// Type-specific validation (called programmatically, not stored)
	public boolean isConfigured() {
		if (metadata == null || metadata.isEmpty()) {
			return false;
		}

		return switch (authenticationMethod) {
			case PASSKEY -> metadata.containsKey(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID)
					&& metadata.containsKey(AuthenticationMethodMetadata.PasskeyMetadata.PUBLIC_KEY_BASE64);
			case TOTP -> metadata.containsKey(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY)
					&& Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.TotpMetadata.VERIFIED));
			case SMS_OTP -> metadata.containsKey(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER)
					&& Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED));
			case EMAIL_OTP -> metadata.containsKey(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS)
					&& Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED));
			default -> AuthenticationMethodMetadata.validateMetadata(authenticationMethod, metadata);
		};
	}

	// Display helpers (called programmatically, not stored)
	public String getDisplayInfo() {
		if (metadata == null) {
			return name;
		}

		return switch (authenticationMethod) {
			case PASSKEY -> {
				String deviceName = getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.DEVICE_NAME);
				String authenticatorName = getMetadataAsString(
						AuthenticationMethodMetadata.PasskeyMetadata.AUTHENTICATOR_NAME);
				yield deviceName != null ? deviceName : (authenticatorName != null ? authenticatorName : "Passkey");
			}
			case TOTP -> {
				String issuer = getMetadataAsString(AuthenticationMethodMetadata.TotpMetadata.ISSUER);
				yield issuer != null ? issuer : "Authenticator App";
			}
			case SMS_OTP -> {
				String phoneNumber = getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
				yield phoneNumber != null ? formatPhoneNumber(phoneNumber) : "SMS Authentication";
			}
			case EMAIL_OTP -> {
				String email = getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS);
				yield email != null ? maskEmail(email) : "Email Authentication";
			}
			default -> name != null ? name : authenticationMethod.getDisplayName();
		};
	}

	// Helper methods
	private String formatPhoneNumber(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.length() < 4) {
			return phoneNumber;
		}
		String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
		return "+• (•••) •••-" + lastFour;
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return email;
		}
		String[] parts = email.split("@");
		String local = parts[0];
		String domain = parts[1];

		if (local.length() <= 2) {
			return "••@" + domain;
		}
		return local.substring(0, 2) + "••@" + domain;
	}

}