package io.strategiz.business.base.provider;

import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;

/**
 * Interface for provider integration handlers
 * Each provider (Kraken, BinanceUS, etc.) implements this interface
 */
public interface ProviderIntegrationHandler {

    /**
     * Test connection to the provider using provided credentials
     * 
     * @param request Provider integration request with credentials
     * @param userId User ID for context
     * @return true if connection successful, false otherwise
     */
    boolean testConnection(CreateProviderIntegrationRequest request, String userId);

    /**
     * Create the provider integration (store credentials + update user document)
     * 
     * @param request Provider integration request with credentials
     * @param userId User ID
     * @return Integration result with metadata
     */
    ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId);

    /**
     * Get the provider ID this handler supports
     * 
     * @return Provider identifier (e.g., "kraken", "binanceus")
     */
    String getProviderId();
}