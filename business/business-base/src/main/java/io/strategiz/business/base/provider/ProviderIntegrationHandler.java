package io.strategiz.business.base.provider;

import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;

/**
 * Interface for provider integration handlers Each provider (Kraken, BinanceUS, etc.)
 * implements this interface
 */
public interface ProviderIntegrationHandler {

	/**
	 * Test connection to the provider using stored credentials. For API key providers:
	 * Retrieves credentials from Vault and tests the connection. For OAuth providers:
	 * Validates the OAuth token stored in Vault.
	 * @param request Provider integration request (credentials may be ignored for API
	 * providers as they use stored credentials from Vault)
	 * @param userId User ID for context
	 * @return true if connection successful, false otherwise
	 */
	boolean testConnection(CreateProviderIntegrationRequest request, String userId);

	/**
	 * Create the provider integration by storing credentials and setting up the provider.
	 * For API key providers: Stores API keys in Vault and saves metadata to Firestore.
	 * For OAuth providers: Generates OAuth URL and prepares for OAuth flow.
	 *
	 * Note: This method handles the actual storage of credentials, not the service layer.
	 * The service layer only orchestrates by calling this method.
	 * @param request Provider integration request with credentials or OAuth setup
	 * @param userId User ID
	 * @return Integration result with metadata (e.g., success status, OAuth URL for OAuth
	 * providers)
	 */
	ProviderIntegrationResult createIntegration(CreateProviderIntegrationRequest request, String userId);

	/**
	 * Get the provider ID this handler supports
	 * @return Provider identifier (e.g., "kraken", "binanceus")
	 */
	String getProviderId();

}