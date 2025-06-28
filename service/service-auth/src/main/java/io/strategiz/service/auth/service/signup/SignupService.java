package io.strategiz.service.auth.service.signup;

import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import io.strategiz.business.tokenauth.SessionAuthBusiness.TokenPair;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.service.auth.model.signup.SignupRequest;
import io.strategiz.service.auth.model.signup.SignupResponse;
import io.strategiz.service.auth.service.common.AuthMethodStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for handling the complete signup process,
 * including both profile creation and authentication method setup
 * in a unified atomic operation.
 * 
 * Refactored to follow SOLID principles:
 * - Single Responsibility: Delegates user creation, auth method setup, and response building to specialized classes
 * - Open/Closed: New auth methods can be added without modifying this class
 * - Liskov Substitution: All auth methods follow the same interface
 * - Interface Segregation: Each component has focused interfaces
 * - Dependency Inversion: Depends on abstractions (AuthMethodStrategy) not implementations
 */
@Service
public class SignupService {

    private static final Logger logger = LoggerFactory.getLogger(SignupService.class);
    
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final SignupResponseBuilder responseBuilder;
    private final Map<String, AuthMethodStrategy> authStrategies;
    
    public SignupService(
        UserRepository userRepository,
        UserFactory userFactory,
        SignupResponseBuilder responseBuilder,
        List<AuthMethodStrategy> authStrategiesList
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.responseBuilder = responseBuilder;
        
        // Map authentication strategies by their name for easy lookup
        this.authStrategies = new HashMap<>();
        for (AuthMethodStrategy strategy : authStrategiesList) {
            authStrategies.put(strategy.getAuthMethodName().toLowerCase(), strategy);
        }
    }
    

    
    /**
     * Process a complete user signup, creating the profile and initiating the 
     * appropriate authentication method setup
     * 
     * @param request Signup request containing profile and auth method information
     * @return SignupResponse with user details, tokens, and any auth-specific data
     */
    @Transactional
    public SignupResponse processSignup(SignupRequest request) {
        String authMethod = request.getAuthMethod().toLowerCase();
        logger.info("Processing signup for email: {} with auth method: {}", request.getEmail(), authMethod);
        
        try {
            // Step 1: Create the user with profile information using the factory
            User user = userFactory.createUser(request);
            
            // Step 2: Persist the user in the repository
            User createdUser = userRepository.createUser(user);
            if (createdUser == null) {
                throw new RuntimeException("Failed to create user profile");
            }
            logger.info("Successfully created user with ID: {}", createdUser.getUserId());
            
            // Step 3: Generate authentication tokens
            TokenPair tokenPair = responseBuilder.createTokens(createdUser.getUserId());
            
            // Step 4: Set up the authentication method
            AuthMethodStrategy authStrategy = authStrategies.get(authMethod);
            if (authStrategy == null) {
                throw new IllegalArgumentException("Unsupported authentication method: " + authMethod);
            }
            
            Object authMethodData = authStrategy.setupAuthentication(createdUser);
            
            // Step 5: Build and return the response
            return responseBuilder.buildResponse(createdUser, tokenPair, authMethodData);
            
        } catch (RuntimeException e) {
            logger.error("Failed to process signup: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during signup: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to complete signup process: " + e.getMessage());
        }
    }

}
