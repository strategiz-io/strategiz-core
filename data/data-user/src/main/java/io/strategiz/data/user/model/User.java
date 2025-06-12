package io.strategiz.data.user.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a user document in the Firestore "users" collection.
 * Following the new schema design with profile, connectedProviders, and audit fields.
 */
@Data
@NoArgsConstructor
public class User {
    private String id;
    private UserProfile profile;
    private List<ConnectedProvider> connectedProviders = new ArrayList<>();
    
    // Audit fields
    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
    private Integer version = 1;
    private Boolean isActive = true;
    
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
    }
}
