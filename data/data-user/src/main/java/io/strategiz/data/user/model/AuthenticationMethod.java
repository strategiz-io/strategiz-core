package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * Base class for authentication methods in the authentication_methods subcollection.
 */
public class AuthenticationMethod {
    private String id;
    private String type; // TOTP, SMS_OTP, PASSKEY, OAUTH_GOOGLE, OAUTH_FACEBOOK
    private String name; // Friendly name for the auth method
    private Date lastVerifiedAt;

    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;

    // Constructors
    public AuthenticationMethod() {
    }

    public AuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.lastVerifiedAt = lastVerifiedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.modifiedBy = modifiedBy;
        this.modifiedAt = modifiedAt;
        this.version = version;
        this.isActive = isActive;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Date lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationMethod that = (AuthenticationMethod) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(type, that.type) &&
               Objects.equals(name, that.name) &&
               Objects.equals(lastVerifiedAt, that.lastVerifiedAt) &&
               Objects.equals(createdBy, that.createdBy) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(modifiedBy, that.modifiedBy) &&
               Objects.equals(modifiedAt, that.modifiedAt) &&
               Objects.equals(version, that.version) &&
               Objects.equals(isActive, that.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name, lastVerifiedAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
    }

    @Override
    public String toString() {
        return "AuthenticationMethod{" +
               "id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", name='" + name + '\'' +
               ", lastVerifiedAt=" + lastVerifiedAt +
               ", createdBy='" + createdBy + '\'' +
               ", createdAt=" + createdAt +
               ", modifiedBy='" + modifiedBy + '\'' +
               ", modifiedAt=" + modifiedAt +
               ", version=" + version +
               ", isActive=" + isActive +
               '}';
    }
}
