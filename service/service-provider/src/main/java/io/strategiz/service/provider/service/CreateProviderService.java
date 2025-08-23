package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.request.CreateProviderRequest;
import io.strategiz.service.provider.model.response.CreateProviderResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.business.provider.kraken.KrakenProviderBusiness;
import io.strategiz.business.provider.binanceus.BinanceUSProviderBusiness;
import io.strategiz.business.provider.alpaca.AlpacaProviderBusiness;
import io.strategiz.business.provider.schwab.SchwabProviderBusiness;
import io.strategiz.business.base.provider.model.CreateProviderIntegrationRequest;
import io.strategiz.business.base.provider.model.ProviderIntegrationResult;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that delegates provider creation to appropriate business modules.
 * This is a thin orchestration layer - all provider-specific logic is in the business modules.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service("providerCreateProviderService")
public class CreateProviderService {
    
    private static final Logger log = LoggerFactory.getLogger(CreateProviderService.class);
    
    private final Map<String, ProviderIntegrationHandler> providerHandlers;
    
    @Autowired
    public CreateProviderService(
            CoinbaseProviderBusiness coinbaseProviderBusiness,
            KrakenProviderBusiness krakenProviderBusiness,
            BinanceUSProviderBusiness binanceUSProviderBusiness,
            AlpacaProviderBusiness alpacaProviderBusiness,
            SchwabProviderBusiness schwabProviderBusiness) {
        
        // Initialize provider handler map
        this.providerHandlers = new HashMap<>();
        this.providerHandlers.put("coinbase", coinbaseProviderBusiness);
        this.providerHandlers.put("kraken", krakenProviderBusiness);
        this.providerHandlers.put("binance", binanceUSProviderBusiness);
        this.providerHandlers.put("binanceus", binanceUSProviderBusiness);
        this.providerHandlers.put("alpaca", alpacaProviderBusiness);
        this.providerHandlers.put("schwab", schwabProviderBusiness);
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
        
        // Basic validation
        validateRequest(request);
        
        // Get the appropriate handler
        ProviderIntegrationHandler handler = getProviderHandler(request.getProviderId());
        
        try {
            // Convert service request to business request
            CreateProviderIntegrationRequest integrationRequest = convertToIntegrationRequest(request);
            
            // Test connection first (each provider implements their own test logic)
            boolean connectionValid = handler.testConnection(integrationRequest, request.getUserId());
            
            if (!connectionValid) {
                throw new StrategizException(
                    ServiceProviderErrorDetails.PROVIDER_INVALID_CREDENTIALS, 
                    "service-provider", 
                    request.getUserId(), 
                    request.getProviderId()
                );
            }
            
            // Create the integration (each provider handles their own creation logic)
            ProviderIntegrationResult result = handler.createIntegration(integrationRequest, request.getUserId());
            
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
        
        // Pass credentials if provided (for API key providers)
        if (request.getCredentials() != null) {
            Object apiKeyObj = request.getCredentials().get("apiKey");
            Object apiSecretObj = request.getCredentials().get("apiSecret");
            
            if (apiKeyObj != null) {
                integrationRequest.setApiKey(apiKeyObj.toString());
            }
            if (apiSecretObj != null) {
                integrationRequest.setApiSecret(apiSecretObj.toString());
            }
            
            // Pass any additional provider-specific data
            integrationRequest.setAccountType(
                request.getCredentials().get("accountType") != null ? 
                request.getCredentials().get("accountType").toString() : null
            );
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
        response.setConnectionData(result.getMetadata());
        
        // Check if this is an OAuth flow by looking for OAuth URL in metadata
        if (result.getMetadata() != null && result.getMetadata().containsKey("oauthUrl")) {
            // OAuth flow - set status to pending and extract OAuth URL
            response.setStatus("pending");
            response.setAuthorizationUrl(result.getMetadata().get("oauthUrl").toString());
            response.setFlowType("oauth");
            
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
            default:
                return providerId;
        }
    }
}