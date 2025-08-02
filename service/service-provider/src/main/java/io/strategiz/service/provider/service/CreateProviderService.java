package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.exception.ProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.kraken.KrakenProviderBusiness;
import io.strategiz.business.provider.binanceus.BinanceUSProviderBusiness;
import io.strategiz.business.provider.alpaca.AlpacaProviderBusiness;
import io.strategiz.data.auth.model.provider.CreateProviderIntegrationRequest;
import io.strategiz.data.auth.model.provider.ProviderIntegrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for creating provider connections and integrations.
 * Handles business logic for OAuth initiation and API key setup.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service("providerCreateProviderService")
public class CreateProviderService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateProviderService.class);
    
    private final CoinbaseProviderBusiness coinbaseProviderBusiness;
    private final KrakenProviderBusiness krakenProviderBusiness;
    private final BinanceUSProviderBusiness binanceUSProviderBusiness;
    private final AlpacaProviderBusiness alpacaProviderBusiness;
    
    @Autowired
    public CreateProviderService(CoinbaseProviderBusiness coinbaseProviderBusiness,
                                KrakenProviderBusiness krakenProviderBusiness,
                                BinanceUSProviderBusiness binanceUSProviderBusiness,
                                AlpacaProviderBusiness alpacaProviderBusiness) {
        this.coinbaseProviderBusiness = coinbaseProviderBusiness;
        this.krakenProviderBusiness = krakenProviderBusiness;
        this.binanceUSProviderBusiness = binanceUSProviderBusiness;
        this.alpacaProviderBusiness = alpacaProviderBusiness;
    }
    
    /**
     * Creates a new provider connection.
     * 
     * @param request The provider connection request
     * @return CreateProviderResponse with connection details or OAuth URL
     */
    public CreateProviderResponse createProvider(CreateProviderRequest request) {
        log.info("Processing provider creation request for user: {}, provider: {}", 
                request.getUserId(), request.getProviderId());
        
        // Validate request
        validateCreateRequest(request);
        
        CreateProviderResponse response = new CreateProviderResponse();
        response.setProviderId(request.getProviderId());
        response.setProviderName(getProviderName(request.getProviderId()));
        response.setConnectionType(request.getConnectionType());
        
        try {
            if ("oauth".equals(request.getConnectionType())) {
                // Handle OAuth flow initiation
                processOAuthCreation(request, response);
                
            } else if ("api_key".equals(request.getConnectionType())) {
                // Handle API key setup
                processApiKeyCreation(request, response);
            } else {
                throw new StrategizException(ProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                    request.getUserId(), request.getProviderId(), "connection_type", request.getConnectionType());
            }
            
            log.info("Provider creation processed successfully for user: {}, provider: {}", 
                   request.getUserId(), request.getProviderId());
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error processing provider creation for user: {}, provider: {}", 
                    request.getUserId(), request.getProviderId(), e);
            
            throw new StrategizException(ProviderErrorDetails.PROVIDER_CONNECTION_FAILED, "service-provider", 
                request.getUserId(), request.getProviderId(), e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Processes OAuth connection creation.
     * 
     * @param request The creation request
     * @param response The response to populate
     */
    private void processOAuthCreation(CreateProviderRequest request, CreateProviderResponse response) {
        log.info("Processing OAuth creation for user: {}, provider: {}", 
                request.getUserId(), request.getProviderId());
        
        try {
            String state = generateState(request.getUserId(), "create_provider");
            String oauthUrl = null;
            
            // Use provider-specific business logic for OAuth URL generation
            switch (request.getProviderId().toLowerCase()) {
                case "coinbase":
                    oauthUrl = coinbaseProviderBusiness.generateAuthorizationUrl(request.getUserId(), state);
                    break;
                case "alpaca":
                    oauthUrl = alpacaProviderBusiness.generateAuthorizationUrl(request.getUserId(), state);
                    break;
                default:
                    // For other providers, use the legacy buildOAuthUrl method
                    oauthUrl = buildOAuthUrl(request.getProviderId(), state);
                    break;
            }
            
            // Set response data
            response.setStatus("pending");
            response.setMessage("OAuth authorization URL generated. Please complete authorization.");
            response.setOperationSuccess(true);
            
            // Set OAuth specific data
            Map<String, Object> oauthData = new HashMap<>();
            oauthData.put("authorizationUrl", oauthUrl);
            oauthData.put("state", state);
            oauthData.put("expiresAt", Instant.now().plusSeconds(3600).toString()); // 1 hour expiry
            
            response.setConnectionData(oauthData);
            
        } catch (Exception e) {
            log.error("Error processing OAuth creation for user: {}, provider: {}", 
                    request.getUserId(), request.getProviderId(), e);
            
            throw new StrategizException(ProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                request.getUserId(), request.getProviderId(), e.getMessage());
        }
    }
    
    /**
     * Processes API key connection creation.
     * 
     * @param request The creation request  
     * @param response The response to populate
     */
    private void processApiKeyCreation(CreateProviderRequest request, CreateProviderResponse response) {
        log.info("Processing API key creation for user: {}, provider: {}", 
                request.getUserId(), request.getProviderId());
        
        try {
            // Create integration request
            CreateProviderIntegrationRequest integrationRequest = new CreateProviderIntegrationRequest();
            integrationRequest.setProviderId(request.getProviderId());
            
            // Build credentials object
            CreateProviderIntegrationRequest.ProviderCredentials credentials = new CreateProviderIntegrationRequest.ProviderCredentials();
            if (request.getCredentials() != null) {
                Object apiKeyObj = request.getCredentials().get("apiKey");
                Object apiSecretObj = request.getCredentials().get("apiSecret");
                Object otpObj = request.getCredentials().get("otp");
                
                credentials.setApiKey(apiKeyObj != null ? apiKeyObj.toString() : null);
                credentials.setApiSecret(apiSecretObj != null ? apiSecretObj.toString() : null);
                credentials.setOtp(otpObj != null ? otpObj.toString() : null);
            }
            integrationRequest.setCredentials(credentials);
            
            // Use provider-specific business logic
            ProviderIntegrationResult result = null;
            boolean testPassed = false;
            
            switch (request.getProviderId().toLowerCase()) {
                case "kraken":
                    testPassed = krakenProviderBusiness.testConnection(integrationRequest, request.getUserId());
                    if (testPassed) {
                        result = krakenProviderBusiness.createIntegration(integrationRequest, request.getUserId());
                    }
                    break;
                case "binance":
                case "binanceus":
                    testPassed = binanceUSProviderBusiness.testConnection(integrationRequest, request.getUserId());
                    if (testPassed) {
                        result = binanceUSProviderBusiness.createIntegration(integrationRequest, request.getUserId());
                    }
                    break;
                default:
                    throw new StrategizException(ProviderErrorDetails.INVALID_PROVIDER_TYPE, "service-provider", 
                        request.getProviderId());
            }
            
            if (!testPassed) {
                throw new StrategizException(ProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS, "service-provider", 
                    request.getUserId(), request.getProviderId());
            }
            
            // Set response data
            response.setStatus("connected");
            response.setMessage("API key connection established successfully.");
            response.setOperationSuccess(true);
            
            // Set connection metadata
            Map<String, Object> connectionData = new HashMap<>();
            connectionData.put("connectionId", UUID.randomUUID().toString());
            connectionData.put("connectedAt", Instant.now().toString());
            connectionData.put("verified", true);
            if (result != null) {
                connectionData.put("supportsTrading", result.isSupportsTrading());
                connectionData.put("permissions", result.getPermissions());
                connectionData.put("metadata", result.getMetadata());
            }
            response.setConnectionData(connectionData);
            
        } catch (StrategizException e) {
            // Re-throw business exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error processing API key creation for user: {}, provider: {}", 
                    request.getUserId(), request.getProviderId(), e);
            
            throw new StrategizException(ProviderErrorDetails.API_KEY_INVALID, "service-provider", 
                request.getUserId(), request.getProviderId(), e.getMessage());
        }
    }
    
    /**
     * Validates the create provider request.
     * 
     * @param request The request to validate
     * @throws StrategizException if validation fails
     */
    private void validateCreateRequest(CreateProviderRequest request) {
        if (request == null) {
            throw new StrategizException(ProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "request");
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new StrategizException(ProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "userId");
        }
        
        if (request.getProviderId() == null || request.getProviderId().trim().isEmpty()) {
            throw new StrategizException(ProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "providerId");
        }
        
        if (request.getConnectionType() == null || request.getConnectionType().trim().isEmpty()) {
            throw new StrategizException(ProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", "connectionType");
        }
        
        // Validate provider type
        if (!isValidProviderType(request.getProviderId())) {
            throw new StrategizException(ProviderErrorDetails.INVALID_PROVIDER_TYPE, "service-provider", 
                request.getUserId(), request.getProviderId());
        }
        
        // Validate connection type
        if (!"oauth".equals(request.getConnectionType()) && !"api_key".equals(request.getConnectionType())) {
            throw new StrategizException(ProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                request.getUserId(), request.getProviderId(), "connection_type", request.getConnectionType());
        }
    }
    
    /**
     * Builds OAuth URL for the specified provider.
     * 
     * @param providerId The provider ID
     * @param state The OAuth state parameter
     * @return The OAuth authorization URL
     */
    private String buildOAuthUrl(String providerId, String state) {
        try {
            // TODO: Replace with actual provider-specific OAuth URL building
            switch (providerId.toLowerCase()) {
                case "binance":
                    return "https://accounts.binance.com/oauth/authorize?client_id=your_binance_client_id&redirect_uri=https://app.strategiz.io/auth/callback&response_type=code&state=" + state;
                case "kraken":
                    return "https://api.kraken.com/oauth/authorize?client_id=your_kraken_client_id&redirect_uri=https://app.strategiz.io/auth/callback&response_type=code&state=" + state;
                default:
                    throw new StrategizException(ProviderErrorDetails.INVALID_PROVIDER_TYPE, "service-provider", providerId);
            }
        } catch (Exception e) {
            log.error("Error building OAuth URL for provider: {}", providerId, e);
            throw new StrategizException(ProviderErrorDetails.OAUTH_TOKEN_EXCHANGE_FAILED, "service-provider", 
                providerId, e.getMessage());
        }
    }
    
    /**
     * Validates API credentials for the specified provider.
     * 
     * @param request The creation request containing credentials
     * @return true if credentials are valid, false otherwise
     */
    private boolean validateApiCredentials(CreateProviderRequest request) {
        // TODO: Implement actual API credential validation with business module
        
        // Simulated validation logic
        log.info("Validating API credentials for provider: {}", request.getProviderId());
        
        // Check if required credential fields are present
        if (request.getCredentials() == null || request.getCredentials().isEmpty()) {
            throw new StrategizException(ProviderErrorDetails.MISSING_REQUIRED_FIELD, "service-provider", 
                request.getUserId(), request.getProviderId(), "credentials");
        }
        
        // Simulate credential validation
        return true; // TODO: Replace with actual validation
    }
    
    /**
     * Validates if the provider type is supported.
     * 
     * @param providerId The provider ID to validate
     * @return true if provider is supported, false otherwise
     */
    private boolean isValidProviderType(String providerId) {
        if (providerId == null) {
            return false;
        }
        
        String provider = providerId.toLowerCase();
        return "coinbase".equals(provider) || "binance".equals(provider) || "binanceus".equals(provider) || "kraken".equals(provider) || "alpaca".equals(provider);
    }
    
    /**
     * Generates a secure OAuth state parameter.
     * 
     * @param userId The user ID
     * @param action The action being performed
     * @return A secure state parameter
     */
    private String generateState(String userId, String action) {
        return UUID.randomUUID().toString() + "_" + userId + "_" + action + "_" + Instant.now().getEpochSecond();
    }
    
    /**
     * Gets the display name for a provider.
     * 
     * @param providerId The provider ID
     * @return The provider display name
     */
    private String getProviderName(String providerId) {
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
            default:
                return "Unknown Provider";
        }
    }
}