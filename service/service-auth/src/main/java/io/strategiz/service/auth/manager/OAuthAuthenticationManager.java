package io.strategiz.service.auth.manager;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.auth.model.oauth.OAuthAuthenticationMethod;
import io.strategiz.data.auth.repository.AuthenticationMethodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

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
        List<OAuthAuthenticationMethod> existingMethods = findExistingOAuthMethods(user, provider, providerId);
        
        if (existingMethods.isEmpty()) {
            createNewOAuthMethod(user, provider, providerId, email);
        } else {
            activateExistingMethodIfNeeded(existingMethods.get(0), user);
        }
    }

    private List<OAuthAuthenticationMethod> findExistingOAuthMethods(UserEntity user, String provider, String providerId) {
        // Note: This method needs to be updated to work with the new entity architecture
        // For now, return empty list to avoid compilation errors
        return List.of();
    }

    private void createNewOAuthMethod(UserEntity user, String provider, String providerId, String email) {
        logger.info("Creating new {} OAuth method for user: {}", provider, user.getUserId());
        
        OAuthAuthenticationMethod oAuthMethod = buildOAuthMethod(user, provider, providerId, email);
        // TODO: Update to use entity architecture - convert to OAuthAuthenticationMethodEntity
        // authenticationMethodRepository.save(oAuthMethod);
    }

    private void activateExistingMethodIfNeeded(OAuthAuthenticationMethod oAuthMethod, UserEntity user) {
        if (!oAuthMethod.isActive()) {
            logger.info("Reactivating {} OAuth method for user: {}", oAuthMethod.getProvider(), user.getUserId());
            // TODO: Update to use entity architecture - convert to OAuthAuthenticationMethodEntity
            // authenticationMethodRepository.save(oAuthMethod);
        }
    }

    private OAuthAuthenticationMethod buildOAuthMethod(UserEntity user, String provider, String providerId, String email) {
        OAuthAuthenticationMethod oAuthMethod = new OAuthAuthenticationMethod();
        oAuthMethod.setUserId(user.getUserId());
        oAuthMethod.setProvider(provider);
        oAuthMethod.setProviderId(providerId);
        oAuthMethod.setProviderEmail(email);
        // Audit fields are handled automatically by BaseEntity
        return oAuthMethod;
    }
} 