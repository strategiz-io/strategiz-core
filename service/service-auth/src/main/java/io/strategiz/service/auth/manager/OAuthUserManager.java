package io.strategiz.service.auth.manager;

import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.model.signup.SignupRequest;
import io.strategiz.service.auth.model.signup.SignupResponse;
import io.strategiz.service.auth.service.signup.SignupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manager for OAuth user operations (creation, lookup)
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
    public Optional<User> findUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    /**
     * Check if this is a signup flow based on state
     */
    public boolean isSignupFlow(String state) {
        return state != null && state.startsWith("signup:");
    }

    /**
     * Create new user through signup process
     */
    public SignupResponse createOAuthUser(String email, String name, String pictureUrl, 
                                          String authMethod, String providerId) {
        try {
            SignupRequest signupRequest = buildSignupRequest(email, name, pictureUrl, authMethod, providerId);
            return signupService.processSignup(signupRequest);
        } catch (Exception e) {
            logger.error("Error during OAuth user creation", e);
            throw new RuntimeException("Failed to create OAuth user: " + e.getMessage(), e);
        }
    }

    private SignupRequest buildSignupRequest(String email, String name, String pictureUrl, 
                                             String authMethod, String providerId) {
        SignupRequest signupRequest = new SignupRequest();
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