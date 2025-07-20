package io.strategiz.service.auth.service.provider;

import io.strategiz.business.provider.kraken.KrakenProviderBusiness;
import io.strategiz.business.provider.binanceus.BinanceUSProviderBusiness;
import io.strategiz.business.provider.coinbase.CoinbaseProviderBusiness;
import io.strategiz.data.auth.model.provider.CreateProviderIntegrationRequest;
import io.strategiz.service.auth.model.provider.CreateProviderIntegrationResponse;
import io.strategiz.business.base.provider.ProviderIntegrationHandler;
import io.strategiz.data.auth.model.provider.ProviderIntegrationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for creating provider integrations during signup flow
 * Routes requests to appropriate business modules
 */
@Service("authCreateProviderService")
public class CreateProviderService {

    private static final Logger log = LoggerFactory.getLogger(CreateProviderService.class);

    private final Map<String, ProviderIntegrationHandler> providerHandlers;

    public CreateProviderService(
            KrakenProviderBusiness krakenProviderBusiness,
            BinanceUSProviderBusiness binanceUSProviderBusiness,
            CoinbaseProviderBusiness coinbaseProviderBusiness) {
        this.providerHandlers = new HashMap<>();
        
        // Register provider handlers
        registerProviderHandler(krakenProviderBusiness.getProviderId(), krakenProviderBusiness);
        registerProviderHandler(binanceUSProviderBusiness.getProviderId(), binanceUSProviderBusiness);
        registerProviderHandler(coinbaseProviderBusiness.getProviderId(), coinbaseProviderBusiness);
        
        log.info("CreateProviderService initialized with {} providers: {}", 
            providerHandlers.size(), providerHandlers.keySet());
    }

    /**
     * Create a provider integration
     */
    public CreateProviderIntegrationResponse createProviderIntegration(
            CreateProviderIntegrationRequest request, String userId) {
        
        log.info("Creating provider integration for user: {}, provider: {}", userId, request.getProviderId());
        
        ProviderIntegrationHandler handler = getProviderHandler(request.getProviderId());
        
        try {
            // Test connection first
            boolean connectionSuccess = handler.testConnection(request, userId);
            
            if (!connectionSuccess) {
                return CreateProviderIntegrationResponse.builder()
                    .success(false)
                    .providerId(request.getProviderId())
                    .status("connection_failed")
                    .message("Failed to connect to " + request.getProviderId() + ". Please verify your credentials.")
                    .build();
            }
            
            // Store credentials and update user document
            ProviderIntegrationResult result = handler.createIntegration(request, userId);
            
            return CreateProviderIntegrationResponse.builder()
                .success(true)
                .providerId(request.getProviderId())
                .status("connected")
                .message("Successfully connected to " + request.getProviderId())
                .connectedAt(Instant.now())
                .permissions(result.getPermissions())
                .metadata(CreateProviderIntegrationResponse.ProviderIntegrationMetadata.builder()
                    .providerName(result.getProviderName())
                    .providerType(result.getProviderType())
                    .supportsTrading(result.isSupportsTrading())
                    .lastTestedAt(Instant.now())
                    .build())
                .build();
                
        } catch (Exception e) {
            log.error("Error creating provider integration for user: {}, provider: {}", 
                userId, request.getProviderId(), e);
            throw new RuntimeException("Failed to create provider integration", e);
        }
    }

    /**
     * Test provider connection without storing credentials
     */
    public CreateProviderIntegrationResponse testProviderConnection(
            CreateProviderIntegrationRequest request, String userId) {
        
        log.info("Testing provider connection for user: {}, provider: {}", userId, request.getProviderId());
        
        ProviderIntegrationHandler handler = getProviderHandler(request.getProviderId());
        
        try {
            boolean connectionSuccess = handler.testConnection(request, userId);
            
            return CreateProviderIntegrationResponse.builder()
                .success(connectionSuccess)
                .providerId(request.getProviderId())
                .status(connectionSuccess ? "test_passed" : "test_failed")
                .message(connectionSuccess ? 
                    "Connection successful" : 
                    "Connection failed - please verify your credentials")
                .build();
                
        } catch (Exception e) {
            log.error("Error testing provider connection for user: {}, provider: {}", 
                userId, request.getProviderId(), e);
            
            return CreateProviderIntegrationResponse.builder()
                .success(false)
                .providerId(request.getProviderId())
                .status("test_error")
                .message("Connection test failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Get provider handler for the specified provider
     */
    private ProviderIntegrationHandler getProviderHandler(String providerId) {
        ProviderIntegrationHandler handler = providerHandlers.get(providerId.toLowerCase());
        
        if (handler == null) {
            throw new IllegalArgumentException("Unsupported provider: " + providerId);
        }
        
        return handler;
    }

    /**
     * Register a provider handler (used by business modules)
     */
    public void registerProviderHandler(String providerId, ProviderIntegrationHandler handler) {
        providerHandlers.put(providerId.toLowerCase(), handler);
        log.info("Registered provider handler for: {}", providerId);
    }
}