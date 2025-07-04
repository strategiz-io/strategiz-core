package io.strategiz.service.auth.manager;

import io.strategiz.data.user.model.OAuthAuthenticationMethod;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.AuthenticationMethodRepository;
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
    public void ensureOAuthMethod(User user, String provider, String providerId, String email) {
        List<OAuthAuthenticationMethod> existingMethods = findExistingOAuthMethods(user, provider, providerId);
        
        if (existingMethods.isEmpty()) {
            createNewOAuthMethod(user, provider, providerId, email);
        } else {
            activateExistingMethodIfNeeded(existingMethods.get(0), user);
        }
    }

    private List<OAuthAuthenticationMethod> findExistingOAuthMethods(User user, String provider, String providerId) {
        return authenticationMethodRepository
                .findByUserIdAndProviderAndProviderId(user.getUserId(), provider, providerId);
    }

    private void createNewOAuthMethod(User user, String provider, String providerId, String email) {
        logger.info("Creating new {} OAuth method for user: {}", provider, user.getUserId());
        
        OAuthAuthenticationMethod oAuthMethod = buildOAuthMethod(user, provider, providerId, email);
        authenticationMethodRepository.save(oAuthMethod);
    }

    private void activateExistingMethodIfNeeded(OAuthAuthenticationMethod oAuthMethod, User user) {
        if (!oAuthMethod.isActive()) {
            logger.info("Reactivating {} OAuth method for user: {}", oAuthMethod.getProvider(), user.getUserId());
            oAuthMethod.setIsActive(true);
            oAuthMethod.setModifiedBy(user.getUserId());
            oAuthMethod.setModifiedAt(new Date());
            authenticationMethodRepository.save(oAuthMethod);
        }
    }

    private OAuthAuthenticationMethod buildOAuthMethod(User user, String provider, String providerId, String email) {
        OAuthAuthenticationMethod oAuthMethod = new OAuthAuthenticationMethod();
        oAuthMethod.setUserId(user.getUserId());
        oAuthMethod.setProvider(provider);
        oAuthMethod.setProviderId(providerId);
        oAuthMethod.setEmail(email);
        oAuthMethod.setCreatedAt(new Date());
        oAuthMethod.setCreatedBy(user.getUserId());
        oAuthMethod.setModifiedAt(new Date());
        oAuthMethod.setModifiedBy(user.getUserId());
        oAuthMethod.setIsActive(true);
        return oAuthMethod;
    }
} 