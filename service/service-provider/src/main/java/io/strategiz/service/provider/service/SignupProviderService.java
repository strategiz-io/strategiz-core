package io.strategiz.service.provider.service;

import io.strategiz.business.tokenauth.SessionAuthBusiness;
import io.strategiz.data.user.model.User;
import io.strategiz.data.user.model.UserProfile;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.provider.exception.ProviderErrors;
import io.strategiz.service.provider.model.SignupCompleteResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for Step 3 of the signup process: Provider Integration
 * 
 * Handles the final step of signup where users can integrate with providers
 * or complete signup in demo mode. This service finalizes the signup process
 * and prepares users for full platform access.
 */
@Service
public class SignupProviderService {

    private static final Logger logger = LoggerFactory.getLogger(SignupProviderService.class);
    
    private final UserRepository userRepository;
    private final SessionAuthBusiness sessionAuthBusiness;
    
    @Value("${application.frontend-url}")
    private String frontendUrl;
    
    public SignupProviderService(
        UserRepository userRepository,
        SessionAuthBusiness sessionAuthBusiness
    ) {
        this.userRepository = userRepository;
        this.sessionAuthBusiness = sessionAuthBusiness;
    }
    
    /**
     * Complete the signup process without provider integration
     * 
     * This method finalizes the signup process for users who choose to skip
     * provider integration and proceed with demo mode or basic access.
     * 
     * @param accessToken Access token from Step 2 (authentication setup)
     * @param mode The selected mode ("demo" or "basic")
     * @param skipProviders Whether providers were skipped
     * @return SignupCompleteResponse with completion details
     */
    @Transactional
    public SignupCompleteResponse completeSignup(String accessToken, String mode, Boolean skipProviders) {
        logger.info("Completing signup process - Mode: {}, Skip Providers: {}", mode, skipProviders);
        
        // Extract user ID from token
        String userId = extractUserIdFromToken(accessToken);
        
        // Get user from repository
        Optional<User> userOptional = userRepository.getUserById(userId);
        if (!userOptional.isPresent()) {
            throw new StrategizException(ProviderErrors.USER_NOT_FOUND, "User not found during signup completion: " + userId);
        }

        User user = userOptional.get();
        UserProfile profile = user.getProfile();
        
        // Update user preferences for signup completion
        updateUserPreferencesForSignupCompletion(user, mode, skipProviders);
        
        // Save updated user
        User updatedUser = userRepository.updateUser(user);
        if (updatedUser == null) {
            throw new StrategizException(ProviderErrors.VERIFICATION_FAILED, "Failed to update user during signup completion");
        }
        
        // Determine next step URL based on mode
        String nextStepUrl = determineNextStepUrl(mode);
        
        // Build response
        SignupCompleteResponse response = new SignupCompleteResponse(
            updatedUser.getUserId(),
            profile.getName(),
            profile.getEmail(),
            mode,
            0, // No providers connected in this flow
            nextStepUrl
        );
        
        // Set user preferences in response
        response.setUserPreferences(buildUserPreferences(updatedUser, mode));
        
        logger.info("Successfully completed signup for user: {} in mode: {}", userId, mode);
        
        return response;
    }
    
    /**
     * Extract user ID from access token
     * 
     * TODO: This should be implemented to properly validate and parse the token
     * 
     * @param accessToken The access token from Step 2
     * @return The user ID from the token
     */
    private String extractUserIdFromToken(String accessToken) {
        // TODO: Implement proper token validation and parsing
        // For now, return a mock user ID
        logger.warn("Token validation not implemented - using mock user ID");
        return "mock-user-id-" + System.currentTimeMillis();
    }
    
    /**
     * Update user preferences for signup completion
     * 
     * @param user The user to update
     * @param mode The selected mode
     * @param skipProviders Whether providers were skipped
     */
    private void updateUserPreferencesForSignupCompletion(User user, String mode, Boolean skipProviders) {
        UserProfile profile = user.getProfile();
        
        // Update trading mode based on user selection
        profile.setTradingMode(mode);
        
        // Mark signup as complete
        // TODO: Add a signupComplete field to UserProfile if needed
        
        // Update audit fields
        user.setModifiedBy("signup-provider-service");
        user.setModifiedAt(new java.util.Date());
        
        logger.info("Updated user preferences for signup completion - User: {}, Mode: {}", 
                user.getUserId(), mode);
    }
    
    /**
     * Determine the next step URL based on user mode
     * 
     * @param mode The user's selected mode
     * @return The appropriate next step URL
     */
    private String determineNextStepUrl(String mode) {
        switch (mode.toLowerCase()) {
            case "demo":
                return frontendUrl + "/dashboard?welcome=true&mode=demo";
            case "basic":
                return frontendUrl + "/dashboard?welcome=true&mode=basic";
            default:
                return frontendUrl + "/dashboard?welcome=true";
        }
    }
    
    /**
     * Build user preferences map for response
     * 
     * @param user The user
     * @param mode The selected mode
     * @return Map of user preferences
     */
    private Map<String, Object> buildUserPreferences(User user, String mode) {
        Map<String, Object> preferences = new HashMap<>();
        
        UserProfile profile = user.getProfile();
        
        preferences.put("tradingMode", mode);
        preferences.put("subscriptionTier", profile.getSubscriptionTier());
        preferences.put("providersConnected", 0);
        preferences.put("signupComplete", true);
        preferences.put("welcomeShown", false);
        
        return preferences;
    }
    
    /**
     * Initiate provider connection during signup
     * 
     * @param providerId The provider to connect
     * @param userId The user ID
     * @return Map with connection data
     */
    public Map<String, String> initiateProviderConnection(String providerId, String userId) {
        logger.info("Initiating provider connection for provider: {} and user: {}", providerId, userId);
        
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("providerId", providerId);
        connectionData.put("userId", userId);
        connectionData.put("status", "initiated");
        
        // TODO: Implement actual provider connection logic
        logger.warn("Provider connection logic not implemented - returning mock response");
        
        return connectionData;
    }
    
    /**
     * Complete signup (overloaded method for controller)
     * 
     * @param userId The user ID
     * @param request The signup completion request
     * @return SignupCompleteResponse with completion details
     */
    public SignupCompleteResponse completeSignup(String userId, Object request) {
        logger.info("Completing signup for user: {}", userId);
        
        // TODO: Extract mode and skipProviders from request object
        // For now, use defaults
        String mode = "demo";
        Boolean skipProviders = true;
        
        // Use mock access token since we have userId directly
        String mockAccessToken = "mock-token-" + userId;
        
        return completeSignup(mockAccessToken, mode, skipProviders);
    }
    
    /**
     * Get available providers for signup
     * 
     * @return Map of available providers
     */
    public Map<String, Object> getAvailableProviders() {
        logger.info("Getting available providers for signup");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> providers = new HashMap<>();
        
        // Add mock providers
        providers.put("kraken", createProviderInfo("Kraken", "exchange", true));
        providers.put("binanceus", createProviderInfo("Binance US", "exchange", true));
        providers.put("coinbase", createProviderInfo("Coinbase", "exchange", true));
        
        response.put("providers", providers);
        response.put("total", providers.size());
        
        logger.info("Returning {} available providers", providers.size());
        
        return response;
    }
    
    /**
     * Helper method to create provider info
     */
    private Map<String, Object> createProviderInfo(String name, String type, boolean available) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", name);
        info.put("type", type);
        info.put("available", available);
        return info;
    }
} 