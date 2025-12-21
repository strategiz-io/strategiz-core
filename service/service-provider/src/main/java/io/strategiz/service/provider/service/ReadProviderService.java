package io.strategiz.service.provider.service;

import io.strategiz.service.provider.model.response.ReadProviderResponse;
import io.strategiz.service.provider.model.response.ProvidersListResponse;
import io.strategiz.service.provider.exception.ServiceProviderErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.time.Instant;

/**
 * Service for reading provider connections and data.
 * Handles business logic for retrieving provider status, balances, and transaction data.
 * 
 * @author Strategiz Team
 * @version 1.0
 */
@Service
public class ReadProviderService {
    
    private static final Logger log = LoggerFactory.getLogger(ReadProviderService.class);
    
    @Autowired(required = false)
    private io.strategiz.framework.secrets.service.VaultSecretService vaultSecretService;

    @Autowired(required = false)
    private PortfolioProviderRepository portfolioProviderRepository;
    
    /**
     * Gets all provider connections for a user as a list.
     * 
     * @param userId The user ID
     * @return ProvidersListResponse containing array of provider connections
     */
    public ProvidersListResponse getProvidersList(String userId) {
        log.info("Getting providers list for user: {}", userId);

        ProvidersListResponse listResponse = new ProvidersListResponse();

        if (portfolioProviderRepository != null) {
            try {
                // Get all active providers for the user from new portfolio structure
                // Path: users/{userId}/portfolio/data/providers/
                List<PortfolioProviderEntity> providers = portfolioProviderRepository.findAllByUserId(userId);

                log.info("Found {} active providers for user: {}", providers != null ? providers.size() : "null", userId);

                for (PortfolioProviderEntity provider : providers) {
                    ReadProviderResponse providerResponse = new ReadProviderResponse();
                    providerResponse.setProviderId(provider.getProviderId());

                    // Set proper provider name based on ID (or use stored name if available)
                    String providerName = provider.getProviderName() != null ?
                        provider.getProviderName() : getProviderDisplayName(provider.getProviderId());
                    providerResponse.setProviderName(providerName);

                    providerResponse.setConnectionType(provider.getConnectionType());
                    providerResponse.setStatus(provider.getStatus());
                    providerResponse.setConnectedAt(provider.getCreatedDate() != null ?
                        provider.getCreatedDate().toDate().toInstant() : Instant.now());

                    // Build account info
                    Map<String, Object> providerAccountInfo = new HashMap<>();
                    providerAccountInfo.put("provider", provider.getProviderId());
                    providerAccountInfo.put("connectionType", provider.getConnectionType());
                    providerAccountInfo.put("providerType", provider.getProviderType());
                    providerAccountInfo.put("environment", provider.getEnvironment());

                    // Add summary data if available
                    if (provider.getTotalValue() != null) {
                        providerAccountInfo.put("totalValue", provider.getTotalValue());
                    }
                    if (provider.getHoldingsCount() != null) {
                        providerAccountInfo.put("holdingsCount", provider.getHoldingsCount());
                    }
                    if (provider.getLastSyncedAt() != null) {
                        providerAccountInfo.put("lastSyncedAt", provider.getLastSyncedAt().toString());
                    }

                    providerResponse.setAccountInfo(providerAccountInfo);

                    listResponse.addProvider(providerResponse);
                    log.info("Added provider {} (status={}, providerName={}) to list for user: {}",
                        provider.getProviderId(), providerResponse.getStatus(), providerResponse.getProviderName(), userId);
                }
            } catch (Exception e) {
                log.error("Error reading providers from Firestore: {}", e.getMessage(), e);
                // Return empty list on error
            }
        } else {
            log.warn("PortfolioProviderRepository not available - returning empty list");
        }

        return listResponse;
    }
    
    /**
     * Gets the display name for a provider ID
     */
    private String getProviderDisplayName(String providerId) {
        if (providerId == null) {
            return "Unknown Provider";
        }
        
        switch (providerId.toLowerCase()) {
            case "kraken":
                return "Kraken";
            case "coinbase":
                return "Coinbase";
            case "binanceus":
                return "Binance US";
            case "alpaca":
                return "Alpaca";
            case "schwab":
                return "Charles Schwab";
            case "robinhood":
                return "Robinhood";
            default:
                // Capitalize first letter as fallback
                return providerId.substring(0, 1).toUpperCase() + providerId.substring(1);
        }
    }
    
    /**
     * Gets all provider connections for a user.
     * 
     * @param userId The user ID
     * @return ReadProviderResponse containing list of provider connections
     */
    public ReadProviderResponse getProviders(String userId) {
        log.info("Getting providers for user: {}", userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "userId", userId, "User ID cannot be null or empty");
        }
        
        try {
            ReadProviderResponse response = new ReadProviderResponse();
            
            // Check if user is test-user-123 and has Kraken connected
            if ("test-user-123".equals(userId)) {
                // Return Kraken as connected for our test user
                response.setProviderId("kraken");
                response.setProviderName("Kraken");
                response.setConnectionType("api_key");
                response.setStatus("connected");
                response.setTotalCount(1);
                response.setPage(1);
                response.setLimit(10);
                response.setHasMore(false);
                response.setConnectedAt(Instant.now());
                response.setAccountInfo(Map.of(
                    "provider", "Kraken",
                    "type", "Exchange",
                    "features", List.of("trading", "portfolio", "market_data")
                ));
                log.info("Returning Kraken as connected for test user");
            } else {
                // For other users, return no providers
                response.setTotalCount(0);
                response.setPage(1);
                response.setLimit(10);
                response.setHasMore(false);
                response.setStatus("disconnected");
                response.setErrorMessage("No providers found");
            }
            
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting providers for user: {}", userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_SERVICE_UNAVAILABLE, "service-provider", 
                userId, e.getMessage());
        }
    }
    
    /**
     * Gets a specific provider by ID.
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @return ReadProviderResponse containing provider details
     */
    public ReadProviderResponse getProvider(String userId, String providerId) {
        log.info("Getting provider {} for user: {}", providerId, userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "userId", userId, "User ID cannot be null or empty");
        }
        
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "providerId", providerId, "Provider ID cannot be null or empty");
        }
        
        try {
            ReadProviderResponse response = new ReadProviderResponse();
            
            // TODO: Replace with actual provider lookup
            // For now, simulate not found
            response.setProviderId(providerId);
            response.setStatus("error");
            response.setErrorCode("PROVIDER_NOT_FOUND");
            response.setErrorMessage("Provider not found");
            
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting provider {} for user: {}", providerId, userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
                userId, providerId);
        }
    }
    
    /**
     * Gets provider status (lightweight check).
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @return ReadProviderResponse containing provider status
     */
    public ReadProviderResponse getProviderStatus(String userId, String providerId) {
        log.info("Getting status for provider {} for user: {}", providerId, userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "userId", userId, "User ID cannot be null or empty");
        }
        
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "providerId", providerId, "Provider ID cannot be null or empty");
        }
        
        try {
            ReadProviderResponse response = new ReadProviderResponse();
            
            // TODO: Replace with actual status check
            // For now, simulate disconnected status
            response.setProviderId(providerId);
            response.setStatus("disconnected");
            response.setErrorMessage("Provider status: disconnected");
            
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting status for provider {} for user: {}", providerId, userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_NOT_FOUND, "service-provider", 
                userId, providerId);
        }
    }
    
    /**
     * Gets provider data (balances, transactions, etc.).
     * 
     * @param userId The user ID
     * @param providerId The provider ID
     * @param dataType The type of data to retrieve
     * @return ReadProviderResponse containing provider data
     */
    public ReadProviderResponse getProviderData(String userId, String providerId, String dataType) {
        log.info("Getting {} data for provider {} for user: {}", dataType, providerId, userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "userId", userId, "User ID cannot be null or empty");
        }
        
        if (providerId == null || providerId.trim().isEmpty()) {
            throw new StrategizException(ServiceProviderErrorDetails.INVALID_PROVIDER_CONFIG, "service-provider", 
                "providerId", providerId, "Provider ID cannot be null or empty");
        }
        
        try {
            ReadProviderResponse response = new ReadProviderResponse();
            
            // TODO: Replace with actual data retrieval based on dataType
            // For now, return empty data
            response.setProviderId(providerId);
            response.setStatus("disconnected");
            response.setErrorMessage("No " + dataType + " data available");
            
            // Set empty data structures based on dataType
            if ("balances".equals(dataType)) {
                response.setBalanceData(new HashMap<>());
            } else if ("transactions".equals(dataType)) {
                response.setTransactions(new ArrayList<>());
            } else if ("orders".equals(dataType)) {
                response.setOrders(new ArrayList<>());
            } else if ("positions".equals(dataType)) {
                response.setPositions(new ArrayList<>());
            }
            
            return response;
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting {} data for provider {} for user: {}", dataType, providerId, userId, e);
            throw new StrategizException(ServiceProviderErrorDetails.PROVIDER_DATA_UNAVAILABLE, "service-provider", 
                userId, providerId, dataType);
        }
    }
}