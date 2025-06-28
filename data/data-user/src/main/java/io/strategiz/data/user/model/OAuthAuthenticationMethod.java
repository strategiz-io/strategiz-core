package io.strategiz.data.user.model;

import java.util.Date;
import java.util.Objects;

/**
 * OAuth authentication method for services like Google and Facebook.
 */
public class OAuthAuthenticationMethod extends AuthenticationMethod {
    private String provider; // "GOOGLE" or "FACEBOOK"
    private String uid; // Provider-specific user ID
    private String email; // Email from provider
    private String userId; // Reference to the User ID in the system

    // No-argument constructor
    public OAuthAuthenticationMethod() {
        super();
    }

    // Custom constructor (preserved and updated)
    public OAuthAuthenticationMethod(String provider, String uid, String email, String createdBy) {
        super();
        this.setType("OAUTH_" + (provider != null ? provider.toUpperCase() : ""));
        this.setName((provider != null ? provider : "") + " Account");
        this.setCreatedBy(createdBy);
        this.setModifiedBy(createdBy);
        this.provider = provider;
        this.uid = uid;
        this.email = email;
    }

    // All-arguments constructor (including inherited fields)
    public OAuthAuthenticationMethod(String id, String type, String name, Date lastVerifiedAt, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive, String provider, String uid, String email) {
        super(id, type, name, lastVerifiedAt, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
        this.provider = provider;
        this.uid = uid;
        this.email = email;
    }

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    /**
     * Sets the provider ID (alias for setUid for compatibility)
     * @param providerId The provider-specific ID
     */
    public void setProviderId(String providerId) {
        this.uid = providerId;
    }
    
    /**
     * Gets the provider ID (alias for getUid for compatibility)
     * @return The provider-specific ID
     */
    public String getProviderId() {
        return this.uid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OAuthAuthenticationMethod that = (OAuthAuthenticationMethod) o;
        return Objects.equals(provider, that.provider) &&
               Objects.equals(uid, that.uid) &&
               Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), provider, uid, email);
    }

    @Override
    public String toString() {
        String superString = super.toString();
        String content = superString.substring(0, superString.length() - 1);
        return content +
               ", provider='" + provider + '\'' +
               ", uid='" + uid + '\'' +
               ", email='" + email + '\'' +
               '}';
    }
}
