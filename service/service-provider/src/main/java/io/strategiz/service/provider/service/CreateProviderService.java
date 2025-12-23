package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.kraken.business.KrakenProviderBusiness;
import io.strategiz.business.provider.binanceus.BinanceUSProviderBusiness;
import io.strategiz.business.provider.alpaca.AlpacaProviderBusiness;
import io.strategiz.business.provider.schwab.SchwabProviderBusiness;
import io.strategiz.business.provider.robinhood.RobinhoodProviderBusiness;
import io.strategiz.business.provider.webull.business.WebullProviderBusiness;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import io.strategiz.data.provider.repository.ReadProviderIntegrationRepository;
import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;import io.strategiz.service.profile.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that orchestrates provider creation by delegating to appropriate business modules.
 * This is a thin orchestration layer following the delegation pattern:
 * 
 * Flow for API Key Providers (Kraken, Binance US):
 * 1. Service receives request and validates
 * 2. Service delegates to business module's createIntegration() - which stores credentials in Vault
 * 3. Service delegates to business module's testConnection() - which uses stored credentials
 * 4. Service returns response or handles errors
 * 
 * Flow for OAuth Providers (Coinbase, Alpaca, Schwab):
 * 1. Service receives request and validates
 * 2. Service delegates to business module's createIntegration() - which generates OAuth URL
 * 3. Service returns OAuth URL to frontend (no connection test until OAuth callback)
 * 
 * Note: This service does NOT store credentials - it only orchestrates.
 * All actual storage happens in the business modules.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service("providerCreateProviderService")
public class CreateProviderService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateProviderService.class);
    
    private final Map<String, ProviderIntegrationHandler> providerHandlers;
    private final ProfileService profileService;
    private final ReadProviderIntegrationRepository readProviderIntegrationRepository;
    
    @Autowired
    public CreateProviderService(
            CoinbaseProviderBusiness coinbaseProviderBusiness,
            KrakenProviderBusiness krakenProviderBusiness,
            BinanceUSProviderBusiness binanceUSProviderBusiness,
            AlpacaProviderBusiness alpacaProviderBusiness,
            SchwabProviderBusiness schwabProviderBusiness,
            RobinhoodProviderBusiness robinhoodProviderBusiness,
            WebullProviderBusiness webullProviderBusiness,
            ProfileService profileService,
            ReadProviderIntegrationRepository readProviderIntegrationRepository) {

        // Initialize provider handler map
        this.providerHandlers = new HashMap<>();
        this.providerHandlers.put("coinbase", coinbaseProviderBusiness);
        this.providerHandlers.put("kraken", krakenProviderBusiness);
        this.providerHandlers.put("binance", binanceUSProviderBusiness);
        this.providerHandlers.put("binanceus", binanceUSProviderBusiness);
        this.providerHandlers.put("alpaca", alpacaProviderBusiness);
        this.providerHandlers.put("schwab", schwabProviderBusiness);
        this.providerHandlers.put("robinhood", robinhoodProviderBusiness);
        this.providerHandlers.put("webull", webullProviderBusiness);

        this.profileService = profileService;
        this.readProviderIntegrationRepository = readProviderIntegrationRepository;
    }
    
    /**
     * Creates a new provider connection by delegating to the appropriate business module.
     * 
     * @param request The provider connection request
     * @return CreateProviderResponse with connection details
     */
    public CreateProviderResponse createProvider(CreateProviderRequest request) {
        log.info("Delegating provider creation for user: {}, provider: {}", 
                request.getUserId(), request.getProviderId());
        
        // Debug logging
        log.info("=== CreateProviderService.createProvider ===");
        log.info("Provider ID: {}", request.getProviderId());
        log.info("Connection Type: {}", request.getConnectionType());
        log.info("User: {}", request.getUserId());
        log.info("Has Direct API Key: {}", request.getApiKey() != null);
        log.info("Has Direct API Secret: {}", request.getApiSecret() != null);
        log.info("Has Credentials Map: {}", request.getCredentials() != null);
        if (request.getCredentials() != null) {
            log.info("Credentials Map Keys: {}", request.getCredentials().keySet());
        }
        
        // Basic validation
        validateRequest(request);
        
        // Get the appropriate handler
        ProviderIntegrationHandler handler = getProviderHandler(request.getProviderId());
        
        try {
            // Convert service request to business request
            CreateProviderIntegrationRequest integrationRequest = convertToIntegrationRequest(request);
            
            // For OAuth providers, skip connection test and generate OAuth URL instead
            // OAuth providers need user authorization first before we can test the connection
            if ("oauth".equalsIgnoreCase(request.getConnectionType())) {
                log.info("OAuth flow detected for provider: {}, skipping connection test", request.getProviderId());
                // Skip connection test for OAuth - go directly to creating the integration
                // The provider will generate the OAuth URL
            } else {
                // For API key providers:
                // 1. Delegate to provider business module to store credentials in Vault
                // 2. Test connection using the stored credentials
                // 3. If test fails, clean up stored credentials (TODO)
                
                // Step 1: Delegate credential storage to the provider's business module
                log.info("Delegating credential storage to {} provider business module", request.getProviderId());
                ProviderIntegrationResult storeResult = handler.createIntegration(integrationRequest, request.getUserId());
                
                if (!storeResult.isSuccess()) {
                    throw new StrategizException(
                        ServiceProviderErrorDetails.PROVIDER_CONNECTION_FAILED, 
                        "service-provider",
                        request.getProviderId(),
                        "Failed to store credentials"
                    );
                }
                
                // Step 2: Delegate connection testing to provider (uses stored credentials from Vault)
                boolean connectionValid = handler.testConnection(integrationRequest, request.getUserId());
                
                if (!connectionValid) {
                    // Step 3: Clean up on failure
                    // TODO: Add cleanup method to handler interface
                    throw new StrategizException(
                        ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS, 
                        "service-provider",  // {0} - module name
                        request.getProviderId()  // {1} - provider
                        // Note: user ID is masked in the message template
                    );
                }
                
                // Connection test passed, return the store result
                return convertToServiceResponse(request, storeResult);
            }
            
            // For OAuth providers, create the integration (generates OAuth URL)
            ProviderIntegrationResult result = handler.createIntegration(integrationRequest, request.getUserId());
            
            // For API key providers, set trading mode to "live" when successfully connected
            if (!"oauth".equalsIgnoreCase(request.getConnectionType()) && result.isSuccess()) {
                try {
                    profileService.updateDemoMode(request.getUserId(), false); // false = live mode
                    log.info("Set demo mode to 'false' (live mode) for user {} after connecting {}", 
                            request.getUserId(), request.getProviderId());
                } catch (Exception e) {
                    log.warn("Failed to update demo mode for user {}: {}", request.getUserId(), e.getMessage());
                }
            }
            
            // Convert business result to service response
            return convertToServiceResponse(request, result);
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error creating provider for user: {}, provider: {}", 
                    request.getUserId(), request.getProviderId(), e);
            
            throw new StrategizException(
                ServiceProviderErrorDetails.PROVIDER_CONNECTION_FAILED, 
                "service-provider", 
                request.getUserId(), 
                request.getProviderId(), 
                e.getMessage()
            );
        }
    }
    
    /**
     * Validates the incoming request.
     * 
     * @param request The request to validate
     * @throws StrategizException if validation fails
     */
    private void validateRequest(CreateProviderRequest request) {
        if (request == null) {
            throw new StrategizException(
                ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                "service-provider", 
                "request"
            );
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new StrategizException(
                ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                "service-provider", 
                "userId"
            );
        }
        
        if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
            throw new StrategizException(
                ServiceProviderErrorDetails.MISSING_REQUIRED_FIELD, 
                "service-provider", 
                "providerId"
            );
        }
    }
    
    /**
     * Gets the appropriate provider handler.
     * 
     * @param providerId The provider ID
     * @return The provider handler
     * @throws StrategizException if provider is not supported
     */
    private ProviderIntegrationHandler getProviderHandler(String providerId) {
        String normalizedId = providerId.toLowerCase();
        ProviderIntegrationHandler handler = providerHandlers.get(normalizedId);
        
        if (handler == null) {
            throw new StrategizException(
                ServiceProviderErrorDetails.INVALID_PROVIDER_TYPE, 
                "service-provider", 
                providerId
            );
        }
        
        return handler;
    }
    
    /**
     * Converts service request to business integration request.
     * 
     * @param request The service request
     * @return The business integration request
     */
    private CreateProviderIntegrationRequest convertToIntegrationRequest(CreateProviderRequest request) {
        CreateProviderIntegrationRequest integrationRequest = new CreateProviderIntegrationRequest();
        integrationRequest.setProviderId(request.getProviderId());
        
        // Check for direct API key fields (frontend sends these at root level)
        if (request.getApiKey() != null && request.getApiSecret() != null) {
            log.info("Using direct API key fields for provider: {}", request.getProviderId());
            integrationRequest.setApiKey(request.getApiKey());
            integrationRequest.setApiSecret(request.getApiSecret());
            integrationRequest.setAccountType(request.getAccountType());
        }
        // Fallback to credentials map if direct fields not found
        else if (request.getCredentials() != null && !request.getCredentials().isEmpty()) {
            log.info("Converting credentials from map for provider: {}", request.getProviderId());
            Object apiKeyObj = request.getCredentials().get("apiKey");
            Object apiSecretObj = request.getCredentials().get("apiSecret");
            
            if (apiKeyObj != null) {
                integrationRequest.setApiKey(apiKeyObj.toString());
            }
            if (apiSecretObj != null) {
                integrationRequest.setApiSecret(apiSecretObj.toString());
            }
            
            // Pass any additional provider-specific data
            Object accountTypeObj = request.getCredentials().get("accountType");
            if (accountTypeObj != null) {
                integrationRequest.setAccountType(accountTypeObj.toString());
            }
        } else {
            log.warn("No credentials provided in request for provider: {}", request.getProviderId());
        }
        
        return integrationRequest;
    }
    
    /**
     * Converts business result to service response.
     * 
     * @param request The original request
     * @param result The business result
     * @return The service response
     */
    private CreateProviderResponse convertToServiceResponse(
            CreateProviderRequest request, 
            ProviderIntegrationResult result) {
        
        CreateProviderResponse response = new CreateProviderResponse();
        response.setProviderId(request.getProviderId());
        response.setProviderName(getProviderDisplayName(request.getProviderId()));
        response.setConnectionType(request.getConnectionType());
        response.setOperationSuccess(result.isSuccess());
        response.setMessage(result.getMessage());
        
        // Pass through all metadata from the business layer
        Map<String, Object> connectionData = result.getMetadata() != null
            ? new HashMap<>(result.getMetadata())
            : new HashMap<>();

        // Check if this is an OAuth flow by looking for OAuth URL in metadata
        if (result.getMetadata() != null && result.getMetadata().containsKey("oauthUrl")) {
            // OAuth flow - set status to pending and extract OAuth URL
            response.setStatus("pending");
            String oauthUrl = result.getMetadata().get("oauthUrl").toString();
            response.setAuthorizationUrl(oauthUrl);
            response.setFlowType("oauth");

            // Also add authorizationUrl to connectionData for frontend compatibility
            connectionData.put("authorizationUrl", oauthUrl);

            // Also set state if available
            if (result.getMetadata().containsKey("state")) {
                response.setState(result.getMetadata().get("state").toString());
            }

            log.info("OAuth flow initiated for provider: {}, URL: {}",
                    request.getProviderId(), response.getAuthorizationUrl());
        } else {
            // Non-OAuth flow or completed flow
            if (result.isSuccess()) {
                response.setStatus("connected");
            } else {
                response.setStatus("failed");
            }
        }

        // Set connectionData on response
        response.setConnectionData(connectionData);

        return response;
    }
    
    /**
     * Gets the display name for a provider.
     * 
     * @param providerId The provider ID
     * @return The provider display name
     */
    private String getProviderDisplayName(String providerId) {
        if (providerId == null) {
            return "Unknown Provider";
        }
        
        switch (providerId.toLowerCase()) {
            case "coinbase":
                return "Coinbase";
            case "binance":
            case "binanceus":
                return "Binance.US";
            case "kraken":
                return "Kraken";
            case "alpaca":
                return "Alpaca";
            case "schwab":
                return "Charles Schwab";
            case "robinhood":
                return "Robinhood";
            case "webull":
                return "Webull";
            default:
                return providerId;
        }
    }
    
    /**
     * Gets the list of connected providers for a user.
     * 
     * @param userId The user ID
     * @return Map containing the list of connected providers
     */
    public Map<String, Object> getConnectedProviders(String userId) {
        log.info("Fetching connected providers for user: {}", userId);
        
        try {
            // Fetch provider integrations from Firestore
            List<ProviderIntegrationEntity> integrations = readProviderIntegrationRepository.findByUserId(userId);
            
            log.info("Found {} provider integrations for user {}", integrations.size(), userId);
            
            // Convert entities to response format
            List<Map<String, Object>> providers = integrations.stream()
                .map(entity -> {
                    Map<String, Object> provider = new HashMap<>();
                    provider.put("providerId", entity.getProviderId());
                    provider.put("providerName", entity.getProviderId()); // Use providerId as name for now
                    provider.put("status", ProviderStatus.CONNECTED.getValue().equals(entity.getStatus()) ? "active" : "inactive");
                    provider.put("connectedAt", entity.getCreatedDate());
                    provider.put("lastSyncedAt", entity.getModifiedDate()); // Use modified date as last sync
                    return provider;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("providers", providers);
            response.put("count", providers.size());
            
            return response;
        } catch (Exception e) {
            log.error("Error fetching connected providers for user {}: {}", userId, e.getMessage(), e);
            // Return empty list on error
            Map<String, Object> response = new HashMap<>();
            response.put("providers", List.of());
            response.put("count", 0);
            response.put("error", e.getMessage());
            return response;
        }
    }
}