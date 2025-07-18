package io.strategiz.service.auth.service.signup;

import io.strategiz.data.user.entity.UserEntity;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.auth.exception.AuthErrors;
import io.strategiz.service.auth.model.signup.OAuthSignupRequest;
import io.strategiz.service.auth.model.signup.OAuthSignupResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for handling OAuth signup processes
 * Specifically designed for signup flows where profile data comes from OAuth providers
 */
@Service
public class SignupService {

    private static final Logger logger = LoggerFactory.getLogger(SignupService.class);
    
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final SignupResponseBuilder responseBuilder;
    
    public SignupService(
        UserRepository userRepository,
        UserFactory userFactory,
        SignupResponseBuilder responseBuilder
    ) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.responseBuilder = responseBuilder;
    }
    
    /**
     * Process OAuth signup with profile data from external provider
     * 
     * @param request OAuth signup request containing OAuth profile data
     * @param deviceId Device ID for token generation  
     * @param ipAddress IP address for token generation
     * @return OAuthSignupResponse with user details and authentication tokens
     */
    @Transactional
    public OAuthSignupResponse processSignup(OAuthSignupRequest request, String deviceId, String ipAddress) {
        String authMethod = request.getAuthMethod().toLowerCase();
        logger.info("Processing OAuth signup for email: {} with auth method: {}", request.getEmail(), authMethod);
        
        try {
            // Check if user already exists
            if (userRepository.getUserByEmail(request.getEmail()).isPresent()) {
                throw new StrategizException(AuthErrors.EMAIL_ALREADY_EXISTS, "User with email already exists");
            }
            
            // Create the user with profile information using the factory
            UserEntity user = userFactory.createUser(request);
            
            // Persist the user in the repository
            UserEntity createdUser = userRepository.createUser(user);
            
            logger.info("OAuth user created successfully: {}", createdUser.getUserId());
            
            // Build success response with tokens
            List<String> authMethods = List.of(authMethod);
            return responseBuilder.buildSuccessResponse(
                createdUser, 
                "OAuth signup completed successfully", 
                authMethods,
                deviceId,
                ipAddress
            );
            
        } catch (StrategizException e) {
            logger.warn("OAuth signup failed for {}: {}", request.getEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during OAuth signup for {}: {}", request.getEmail(), e.getMessage(), e);
            throw new StrategizException(AuthErrors.SIGNUP_FAILED, "OAuth signup failed due to internal error");
        }
    }
}