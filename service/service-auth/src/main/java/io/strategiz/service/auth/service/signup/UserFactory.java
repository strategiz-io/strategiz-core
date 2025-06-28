package io.strategiz.service.auth.service.signup;

import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.UserProfile;
import io.strategiz.service.auth.model.signup.SignupRequest;

/**
 * Factory class responsible for creating User objects from signup requests.
 * This follows the Single Responsibility Principle by separating user creation
 * from other business logic.
 */
@Component
public class UserFactory {
    
    /**
     * Creates a new User entity populated with data from a SignupRequest
     * 
     * @param request The signup request containing user data
     * @return A fully populated User entity (not yet persisted)
     */
    public User createUser(SignupRequest request) {
        // Generate a unique user ID
        String userId = UUID.randomUUID().toString();
        
        // Create and populate the user
        User user = new User();
        user.setUserId(userId);
        
        // Create and populate the profile
        UserProfile profile = new UserProfile();
        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setPhotoURL(request.getPhotoURL());
        profile.setVerifiedEmail(false);
        profile.setSubscriptionTier("free"); // Default tier
        profile.setTradingMode("demo"); // Default mode
        profile.setIsActive(true);
        
        // Set the profile on the user
        user.setProfile(profile);
        
        // Set audit fields
        Date now = new Date();
        user.setCreatedBy("signup_service");
        user.setCreatedAt(now);
        user.setModifiedBy("signup_service");
        user.setModifiedAt(now);
        user.setIsActive(true);
        user.setVersion(1);
        
        return user;
    }
}
