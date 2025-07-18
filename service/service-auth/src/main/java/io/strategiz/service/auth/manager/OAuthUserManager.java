package io.strategiz.service.auth.manager;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import io.strategiz.service.auth.service.signup.SignupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manager for OAuth user operations (creation, lookup)
 * Specifically handles OAuth signup flows
 */
@Component
public class OAuthUserManager {

    private static final Logger logger = LoggerFactory.getLogger(OAuthUserManager.class);

    private final UserRepository userRepository;
    private final SignupService signupService;

    public OAuthUserManager(UserRepository userRepository, SignupService signupService) {
        this.userRepository = userRepository;
        this.signupService = signupService;
    }

    /**
     * Find existing user by email
     */
    public Optional<UserEntity> findUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    /**
     * Check if this is a signup flow based on state
     */
    public boolean isSignupFlow(String state) {
        return state != null && state.startsWith("signup:");
    }

    /**
     * Create new user through OAuth signup process
     */
    public OAuthSignupResponse createOAuthUser(String email, String name, String pictureUrl, 
                                          String authMethod, String providerId,
                                          String deviceId, String ipAddress) {
        try {
            OAuthSignupRequest signupRequest = buildSignupRequest(email, name, pictureUrl, authMethod, providerId);
            return signupService.processSignup(signupRequest, deviceId, ipAddress);
        } catch (Exception e) {
            logger.error("Error during OAuth user creation", e);
            throw e;
        }
    }

    private OAuthSignupRequest buildSignupRequest(String email, String name, String pictureUrl, 
                                             String authMethod, String providerId) {
        OAuthSignupRequest signupRequest = new OAuthSignupRequest();
        signupRequest.setEmail(email);
        signupRequest.setName(name);
        signupRequest.setPhotoURL(pictureUrl);
        signupRequest.setAuthMethod(authMethod);
        
        Map<String, String> authData = new HashMap<>();
        authData.put("providerId", providerId);
        authData.put("email", email);
        signupRequest.setAuthData(authData);
        
        return signupRequest;
    }
}