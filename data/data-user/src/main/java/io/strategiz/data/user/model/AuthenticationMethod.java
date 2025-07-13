package io.strategiz.data.user.model;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.Date;
import java.util.Objects;

/**
 * Base class for authentication methods in the authentication_methods subcollection.
 */
public class AuthenticationMethod extends BaseEntity {
    private String id;
    private String type; // TOTP, SMS_OTP, PASSKEY, OAUTH_GOOGLE, OAUTH_FACEBOOK
    private String name; // Friendly name for the auth method
    private Date lastVerifiedAt;


    // Constructors
    public AuthenticationMethod() {
    }

    public AuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy) {
        super(createdBy);
        this.id = id;
        this.type = type;
        this.name = name;
        this.lastVerifiedAt = lastVerifiedAt;
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

    @Override
    public String getCollectionName() {
        return "authentication_methods";
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AuthenticationMethod that = (AuthenticationMethod) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(type, that.type) &&
               Objects.equals(name, that.name) &&
               Objects.equals(lastVerifiedAt, that.lastVerifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, type, name, lastVerifiedAt);
    }

    @Override
    public String toString() {
        return "AuthenticationMethod{" +
               "id='" + id + '\'' +
               ", type='" + type + '\'' +
               ", name='" + name + '\'' +
               ", lastVerifiedAt=" + lastVerifiedAt +
               ", audit=" + getAuditFields() +
               '}';
    }
}
