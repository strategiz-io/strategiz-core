package io.strategiz.service.auth.service.signup;

import io.strategiz.data.base.constants.SubscriptionTierConstants;
import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.entity.UserProfileEntity;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Factory for creating UserEntity objects from OAuth signup requests
 */
@Component
public class UserFactory {

    private static final Logger log = LoggerFactory.getLogger(UserFactory.class);

    @Value("${email.signup.admin.emails:}")
    private String adminEmails;

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
            SubscriptionTierConstants.DEFAULT, // Explorer (free) tier
            true // Default demo mode
        );

        // Set ADMIN role for admin emails
        if (isAdminEmail(request.getEmail())) {
            profile.setRole("ADMIN");
            log.info("Setting ADMIN role for admin email: {}", request.getEmail());
        }

        // Create user entity
        UserEntity user = new UserEntity();
        user.setUserId(userId);
        user.setProfile(profile);

        return user;
    }

    /**
     * Check if email is in the admin list.
     */
    private boolean isAdminEmail(String email) {
        if (adminEmails == null || adminEmails.isBlank() || email == null) {
            return false;
        }
        String normalizedEmail = email.toLowerCase();
        String[] emails = adminEmails.split(",");
        for (String adminEmail : emails) {
            if (adminEmail.trim().equalsIgnoreCase(normalizedEmail)) {
                return true;
            }
        }
        return false;
    }
}