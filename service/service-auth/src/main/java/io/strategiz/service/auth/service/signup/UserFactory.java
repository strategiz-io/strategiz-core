package io.strategiz.service.auth.service.signup;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory for creating UserEntity objects from OAuth signup requests
 */
@Component
public class UserFactory {

    /**
     * Creates a new UserEntity populated with data from OAuth signup
     * Uses UUID as the userId - immutable and privacy-friendly
     *
     * @param request OAuth signup request containing profile data
     * @return UserEntity ready for persistence
     */
    public UserEntity createUser(OAuthSignupRequest request) {
        // Generate unique UUID - immutable, doesn't change even if email changes
        String userId = UUID.randomUUID().toString();

        // Create user profile with OAuth data
        UserProfileEntity profile = new UserProfileEntity(
            request.getName(),
            request.getEmail(),
            request.getPhotoURL(),
            true, // OAuth providers verify email
            "trial", // Default to 30-day trial tier
            true // Default demo mode
        );

        // Create user entity
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setProfile(profile);

        return user;
    }
}