package io.strategiz.data.user.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Represents a user document in the Firestore "users" collection.
 * Following the new schema design with profile, connectedProviders, and audit fields.
 */
public class User {
    private String id;
    private UserProfile profile;
    private List<ConnectedProvider> connectedProviders = new ArrayList<>();
    private List<AuthenticationMethod> authenticationMethods = new ArrayList<>();

    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;

    // Constructors
    public User() {
    }

    /**
     * Creates a new user with the minimum required fields
     */
    public User(String id, String name, String email, String createdBy) {
        this.id = id;
        this.profile = new UserProfile();
        this.profile.setName(name);
        this.profile.setEmail(email);
        this.profile.setVerifiedEmail(false);
        this.profile.setSubscriptionTier("free");
        this.profile.setTradingMode("demo");
        this.profile.setIsActive(true);

        Date now = new Date();
        this.createdBy = createdBy;
        this.createdAt = now;
        this.modifiedBy = createdBy;
        this.modifiedAt = now;
        // Default initializers for version and isActive will apply
        // connectedProviders is initialized by default as new ArrayList<>()
    }

    public User(String id, UserProfile profile, List<ConnectedProvider> connectedProviders, List<AuthenticationMethod> authenticationMethods, String createdBy, Date createdAt, String modifiedBy, Date modifiedAt, Integer version, Boolean isActive) {
        this.id = id;
        this.profile = profile;
        this.connectedProviders = connectedProviders;
        this.authenticationMethods = authenticationMethods;
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

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public List<ConnectedProvider> getConnectedProviders() {
        return connectedProviders;
    }

    public void setConnectedProviders(List<ConnectedProvider> connectedProviders) {
        this.connectedProviders = connectedProviders;
    }

    public List<AuthenticationMethod> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(List<AuthenticationMethod> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public void addAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        if (this.authenticationMethods == null) {
            this.authenticationMethods = new ArrayList<>();
        }
        this.authenticationMethods.add(authenticationMethod);
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(profile, user.profile) &&
               Objects.equals(connectedProviders, user.connectedProviders) &&
               Objects.equals(authenticationMethods, user.authenticationMethods) &&
               Objects.equals(createdBy, user.createdBy) &&
               Objects.equals(createdAt, user.createdAt) &&
               Objects.equals(modifiedBy, user.modifiedBy) &&
               Objects.equals(modifiedAt, user.modifiedAt) &&
               Objects.equals(version, user.version) &&
               Objects.equals(isActive, user.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, profile, connectedProviders, authenticationMethods, createdBy, createdAt, modifiedBy, modifiedAt, version, isActive);
    }

    @Override
    public String toString() {
        return "User{" +
               "id='" + id + '\'' +
               ", profile=" + profile +
               ", connectedProviders=" + connectedProviders +
               ", authenticationMethods=" + authenticationMethods +
               ", createdBy='" + createdBy + '\'' +
               ", createdAt=" + createdAt +
               ", modifiedBy='" + modifiedBy + '\'' +
               ", modifiedAt=" + modifiedAt +
               ", version=" + version +
               ", isActive=" + isActive +
               '}';
    }
}
