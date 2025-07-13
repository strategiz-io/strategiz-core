package io.strategiz.data.user.model;

import io.strategiz.data.base.entity.BaseEntity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Represents a user document in the Firestore "users" collection.
 * Following the new schema design with profile, connectedProviders, and audit fields.
 */
public class User extends BaseEntity {
    private String userId;
    private UserProfile profile;
    private List<ConnectedProvider> connectedProviders = new ArrayList<>();
    private List<AuthenticationMethod> authenticationMethods = new ArrayList<>();


    // Constructors
    public User() {
    }

    /**
     * Creates a new user with the minimum required fields
     */
    public User(String userId, String name, String email, String createdBy) {
        super(createdBy);
        this.userId = userId;
        this.profile = new UserProfile();
        this.profile.setName(name);
        this.profile.setEmail(email);
        this.profile.setVerifiedEmail(false);
        this.profile.setSubscriptionTier("free");
        this.profile.setTradingMode("demo");
        this.profile.setIsActive(true);
    }

    public User(String userId, UserProfile profile, List<ConnectedProvider> connectedProviders, List<AuthenticationMethod> authenticationMethods, String createdBy) {
        super(createdBy);
        this.userId = userId;
        this.profile = profile;
        this.connectedProviders = connectedProviders;
        this.authenticationMethods = authenticationMethods;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    @Override
    public String getId() {
        return userId;
    }

    @Override
    public void setId(String id) {
        this.userId = id;
    }

    @Override
    public String getCollectionName() {
        return "users";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
               Objects.equals(profile, user.profile) &&
               Objects.equals(connectedProviders, user.connectedProviders) &&
               Objects.equals(authenticationMethods, user.authenticationMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, profile, connectedProviders, authenticationMethods);
    }

    @Override
    public String toString() {
        return "User{" +
               "userId='" + userId + '\'' +
               ", profile=" + profile +
               ", connectedProviders=" + connectedProviders +
               ", authenticationMethods=" + authenticationMethods +
               ", audit=" + getAuditFields() +
               '}';
    }
}
