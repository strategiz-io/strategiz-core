package io.strategiz.service.provider.api;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.provider.exception.ProviderErrors;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory for getting the appropriate provider API implementation.
 */
@Component
public class ProviderApiFactory {

    private final Map<String, ProviderApiService> providerApis;
    public ProviderApiFactory(Set<ProviderApiService> providerApiServices) {
        this.providerApis = new HashMap<>();
        
        // Register all provider implementations
        for (ProviderApiService api : providerApiServices) {
            providerApis.put(api.getProviderId(), api);
        }
    }
    
    /**
     * Get the provider API implementation for the specified provider.
     *
     * @param providerId The provider identifier
     * @return The provider API service implementation
     * @throws IllegalArgumentException if the provider is not supported
     */
    public ProviderApiService getProviderApi(String providerId) {
        ProviderApiService api = providerApis.get(providerId.toLowerCase());
        if (api == null) {
            throw new StrategizException(ProviderErrors.PROVIDER_NOT_SUPPORTED, "Unsupported provider: " + providerId);
        }
        return api;
    }
    
    /**
     * Check if a provider is supported.
     *
     * @param providerId The provider identifier
     * @return True if supported, false otherwise
     */
    public boolean isProviderSupported(String providerId) {
        return providerApis.containsKey(providerId.toLowerCase());
    }
    
    /**
     * Get all supported provider IDs.
     *
     * @return Set of supported provider IDs
     */
    public Set<String> getSupportedProviders() {
        return providerApis.keySet();
    }
}
