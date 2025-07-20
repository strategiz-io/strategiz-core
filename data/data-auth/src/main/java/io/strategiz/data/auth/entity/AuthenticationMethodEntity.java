package io.strategiz.data.auth.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.strategiz.data.base.entity.BaseEntity;
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
 * Unified entity for authentication methods stored in users/{userId}/authentication_methods subcollection
 * Supports all authentication method types (TOTP, OAuth, Passkey, SMS OTP, etc.)
 */
@Entity
@Table(name = "authentication_methods")
public class AuthenticationMethodEntity extends BaseEntity {

    @Id
    @DocumentId
    @PropertyName("id")
    @JsonProperty("id")
    @Column(name = "id")
    private String id;

    @PropertyName("type")
    @JsonProperty("type")
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Authentication method type is required")
    private AuthenticationMethodType type;

    @PropertyName("name")
    @JsonProperty("name")
    private String name; // User-friendly name

    @PropertyName("isEnabled")
    @JsonProperty("isEnabled")
    private boolean isEnabled = true;

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

    public AuthenticationMethodEntity(AuthenticationMethodType type, String name) {
        super();
        this.type = type;
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

    public AuthenticationMethodType getType() {
        return type;
    }

    public void setType(AuthenticationMethodType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
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

    public void disable() {
        this.isEnabled = false;
    }

    public void enable() {
        this.isEnabled = true;
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
    @Override
    public String getCollectionName() {
        return "authentication_methods";
    }

    // Type-specific behavior
    public boolean isConfigured() {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        
        return switch (type) {
            case PASSKEY -> metadata.containsKey(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID) && 
                           metadata.containsKey(AuthenticationMethodMetadata.PasskeyMetadata.PUBLIC_KEY_BASE64);
            case TOTP -> metadata.containsKey(AuthenticationMethodMetadata.TotpMetadata.SECRET_KEY) &&
                        Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.TotpMetadata.VERIFIED));
            case SMS_OTP -> metadata.containsKey(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER) && 
                           Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.SmsOtpMetadata.IS_VERIFIED));
            case EMAIL_OTP -> metadata.containsKey(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS) && 
                             Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.EmailOtpMetadata.IS_VERIFIED));
            default -> AuthenticationMethodMetadata.validateMetadata(type, metadata);
        };
    }

    /**
     * Validate that metadata is properly structured for this authentication method type
     */
    public boolean isMetadataValid() {
        return AuthenticationMethodMetadata.validateMetadata(type, metadata);
    }

    /**
     * Get type-specific display information for this authentication method
     */
    public String getDisplayInfo() {
        if (metadata == null) {
            return name;
        }
        
        return switch (type) {
            case PASSKEY -> {
                String deviceName = getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.DEVICE_NAME);
                String authenticatorName = getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.AUTHENTICATOR_NAME);
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
            default -> name != null ? name : type.getDisplayName();
        };
    }

    /**
     * Get unique identifier for this authentication method (type-specific)
     */
    public String getUniqueIdentifier() {
        if (metadata == null) {
            return id;
        }
        
        return switch (type) {
            case PASSKEY -> getMetadataAsString(AuthenticationMethodMetadata.PasskeyMetadata.CREDENTIAL_ID);
            case TOTP -> id; // TOTP uses entity ID as unique identifier
            case SMS_OTP -> getMetadataAsString(AuthenticationMethodMetadata.SmsOtpMetadata.PHONE_NUMBER);
            case EMAIL_OTP -> getMetadataAsString(AuthenticationMethodMetadata.EmailOtpMetadata.EMAIL_ADDRESS);
            default -> {
                String providerId = getMetadataAsString(AuthenticationMethodMetadata.OAuthMetadata.PROVIDER_USER_ID);
                String provider = getMetadataAsString(AuthenticationMethodMetadata.OAuthMetadata.PROVIDER);
                yield providerId != null && provider != null ? provider + ":" + providerId : id;
            }
        };
    }

    /**
     * Check if this authentication method supports backup/recovery
     */
    public boolean supportsBackup() {
        return switch (type) {
            case PASSKEY -> Boolean.TRUE.equals(metadata.get(AuthenticationMethodMetadata.PasskeyMetadata.BACKUP_ELIGIBLE));
            case TOTP -> metadata.containsKey(AuthenticationMethodMetadata.TotpMetadata.BACKUP_CODES);
            case SMS_OTP, EMAIL_OTP -> false; // OTP methods don't have backup
            default -> false; // OAuth doesn't need backup
        };
    }

    /**
     * Get security level for this authentication method
     */
    public String getSecurityLevel() {
        return switch (type) {
            case PASSKEY -> "HIGH"; // Strong cryptographic authentication
            case TOTP -> "MEDIUM"; // Time-based, but can be phished
            case SMS_OTP -> "LOW"; // Vulnerable to SIM swapping
            case EMAIL_OTP -> "LOW"; // Vulnerable to email compromise
            default -> "MEDIUM"; // OAuth providers vary
        };
    }

    // Helper methods
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        // Show last 4 digits: +1 (***) ***-1234
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