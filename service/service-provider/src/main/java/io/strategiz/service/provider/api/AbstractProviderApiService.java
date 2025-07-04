package io.strategiz.service.provider.api;

import io.strategiz.data.user.model.Credentials;
import io.strategiz.data.user.model.Provider;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.provider.exception.ProviderErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for provider API implementations.
 */
public abstract class AbstractProviderApiService implements ProviderApiService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final RestTemplate restTemplate;
    protected final UserRepository userRepository;
    
    public AbstractProviderApiService(
            RestTemplate restTemplate,
            UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }
    
    /**
     * Check if a user has connected this provider.
     *
     * @param userId The user ID
     * @return True if connected, false otherwise
     */
    @Override
    public boolean isProviderConnected(String userId) {
        Optional<Provider> provider = userRepository.getProvider(userId, getProviderId());
        return provider.isPresent();
    }
    
    /**
     * Get provider metadata for a user.
     *
     * @param userId The user ID
     * @return Map containing provider metadata
     */
    @Override
    public Map<String, Object> getProviderMetadata(String userId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("connected", false);
        metadata.put("accountType", "paper");
        
        Optional<Provider> providerOpt = userRepository.getProvider(userId, getProviderId());
        if (providerOpt.isPresent()) {
            Provider provider = providerOpt.get();
            metadata.put("connected", true);
            metadata.put("accountType", provider.getAccountType().toLowerCase());
            
            if (provider.getSettings() != null) {
                if (provider.getSettings().containsKey("connectedAt")) {
                    metadata.put("connectedAt", provider.getSettings().get("connectedAt"));
                }
                if (provider.getSettings().containsKey("name")) {
                    metadata.put("name", provider.getSettings().get("name"));
                }
            }
        }
        
        return metadata;
    }
    
    /**
     * Disconnect the provider by removing its configuration and credentials.
     *
     * @param userId The user ID
     * @return True if successful
     */
    @Override
    public boolean disconnectProvider(String userId) {
        try {
            userRepository.deleteProvider(userId, getProviderId());
            userRepository.deleteCredentials(userId, getProviderId());
            return true;
        } catch (Exception e) {
            log.error("Error disconnecting provider: {}", getProviderId(), e);
            throw new StrategizException(ProviderErrors.API_ERROR, "Error disconnecting provider: " + getProviderId(), e);
        }
    }
    
    /**
     * Helper method to get credentials.
     *
     * @param userId The user ID
     * @return The credentials
     */
    protected Credentials getCredentials(String userId) {
        Optional<Credentials> credentialsOpt = userRepository.getCredentials(userId, getProviderId());
        if (!credentialsOpt.isPresent()) {
            log.error("No credentials found for user {} and provider {}", userId, getProviderId());
            throw new StrategizException(ProviderErrors.INVALID_CREDENTIALS, "No credentials found for user " + userId + " and provider " + getProviderId());
        }
        
        return credentialsOpt.get();
    }
    
    /**
     * Helper method to save provider configuration.
     *
     * @param userId The user ID
     * @param provider The provider configuration
     */
    protected void saveProvider(String userId, Provider provider) {
        userRepository.saveProvider(userId, getProviderId(), provider);
    }
    
    /**
     * Helper method to save credentials.
     *
     * @param userId The user ID
     * @param credentials The credentials
     */
    protected void saveCredentials(String userId, Credentials credentials) {
        userRepository.saveCredentials(userId, getProviderId(), credentials);
    }
}
