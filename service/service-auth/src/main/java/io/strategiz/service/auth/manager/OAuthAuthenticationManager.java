package io.strategiz.service.auth.manager;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodEntity;
import io.strategiz.data.auth.entity.AuthenticationMethodMetadata;
import io.strategiz.data.auth.entity.AuthenticationMethodType;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.ServiceAuthErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for managing OAuth authentication methods
 */
@Component
public class OAuthAuthenticationManager {

    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticationManager.class);

    private final AuthenticationMethodRepository authenticationMethodRepository;

    public OAuthAuthenticationManager(AuthenticationMethodRepository authenticationMethodRepository) {
        this.authenticationMethodRepository = authenticationMethodRepository;
    }

    /**
     * Ensure user has active OAuth authentication method
     */
    public void ensureOAuthMethod(UserEntity user, String provider, String providerId, String email) {
        AuthenticationMethodType oauthType = getOAuthTypeForProvider(provider);
        List<AuthenticationMethodEntity> existingMethods = findExistingOAuthMethods(user.getUserId(), oauthType, providerId);

        if (existingMethods.isEmpty()) {
            createNewOAuthMethod(user, provider, providerId, email, oauthType);
        } else {
            activateExistingMethodIfNeeded(existingMethods.get(0), user);
        }
    }

    /**
     * Get the AuthenticationMethodType for a given provider name
     */
    private AuthenticationMethodType getOAuthTypeForProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> AuthenticationMethodType.OAUTH_GOOGLE;
            case "facebook" -> AuthenticationMethodType.OAUTH_FACEBOOK;
            case "microsoft" -> AuthenticationMethodType.OAUTH_MICROSOFT;
            case "github" -> AuthenticationMethodType.OAUTH_GITHUB;
            case "linkedin" -> AuthenticationMethodType.OAUTH_LINKEDIN;
            case "twitter" -> AuthenticationMethodType.OAUTH_TWITTER;
            default -> throw new StrategizException(ServiceAuthErrorDetails.OAUTH_CONFIGURATION_ERROR,
                    "service-auth", "Unsupported OAuth provider: " + provider);
        };
    }

    private List<AuthenticationMethodEntity> findExistingOAuthMethods(String userId, AuthenticationMethodType oauthType, String providerId) {
        // Find existing OAuth methods of the same type for this user
        List<AuthenticationMethodEntity> methods = authenticationMethodRepository.findByUserIdAndType(userId, oauthType);

        // Filter by provider user ID in metadata
        return methods.stream()
            .filter(method -> {
                Object storedProviderId = method.getMetadata(AuthenticationMethodMetadata.OAuthMetadata.PROVIDER_USER_ID);
                return providerId.equals(storedProviderId);
            })
            .toList();
    }

    private void createNewOAuthMethod(UserEntity user, String provider, String providerId, String email, AuthenticationMethodType oauthType) {
        logger.info("Creating new {} OAuth method for user: {}", provider, user.getUserId());

        AuthenticationMethodEntity entity = buildOAuthMethodEntity(user, provider, providerId, email, oauthType);
        authenticationMethodRepository.saveForUser(user.getUserId(), entity);

        logger.info("Successfully created {} OAuth authentication method for user: {}", provider, user.getUserId());
    }

    private void activateExistingMethodIfNeeded(AuthenticationMethodEntity entity, UserEntity user) {
        boolean needsUpdate = false;

        // Reactivate if not active
        if (!entity.getIsActive()) {
            logger.info("Reactivating {} OAuth method for user: {}", entity.getAuthenticationMethod(), user.getUserId());
            entity.setIsActive(true);
            needsUpdate = true;
        }

        // Update last used timestamp
        entity.markAsUsed();
        needsUpdate = true;

        if (needsUpdate) {
            authenticationMethodRepository.saveForUser(user.getUserId(), entity);
        }
    }

    private AuthenticationMethodEntity buildOAuthMethodEntity(UserEntity user, String provider, String providerId, String email, AuthenticationMethodType oauthType) {
        AuthenticationMethodEntity entity = new AuthenticationMethodEntity();
        // Don't set ID - let the repository generate it so audit fields are initialized
        entity.setAuthenticationMethod(oauthType);
        entity.setName(getDisplayNameForProvider(provider));
        entity.setIsActive(true);
        entity.markAsUsed();

        // Set OAuth-specific metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.PROVIDER, provider.toLowerCase());
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.PROVIDER_USER_ID, providerId);
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.EMAIL, email);
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.PROFILE_VERIFIED, true);
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.CONNECTED_TIME, Instant.now().toString());
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.CONNECTION_STATUS, "active");
        metadata.put(AuthenticationMethodMetadata.OAuthMetadata.LOGIN_COUNT, 1);

        entity.setMetadata(metadata);

        return entity;
    }

    private String getDisplayNameForProvider(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> "Google Account";
            case "facebook" -> "Facebook Account";
            case "microsoft" -> "Microsoft Account";
            case "github" -> "GitHub Account";
            case "linkedin" -> "LinkedIn Account";
            case "twitter" -> "Twitter Account";
            default -> provider + " Account";
        };
    }
} 