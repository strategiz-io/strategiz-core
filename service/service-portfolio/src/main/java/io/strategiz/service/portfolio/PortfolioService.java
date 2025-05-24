package io.strategiz.service.portfolio;

import io.strategiz.data.portfolio.PortfolioCredentialsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling portfolio operations across multiple brokerages
 */
@Slf4j
@Service
public class PortfolioService {

    private final PortfolioCredentialsRepository credentialsRepository;
    private final List<BrokerageService> brokerageServices;

    @Autowired
    public PortfolioService(
            PortfolioCredentialsRepository credentialsRepository,
            List<BrokerageService> brokerageServices) {
        this.credentialsRepository = credentialsRepository;
        this.brokerageServices = brokerageServices;
    }

    /**
     * Get portfolio data for a specific brokerage
     *
     * @param userId User ID (email)
     * @param provider Brokerage provider name (e.g., "robinhood", "coinbase")
     * @return Portfolio data or error information
     */
    public Map<String, Object> getBrokeragePortfolioData(String userId, String provider) {
        try {
            log.info("Getting {} portfolio data for user: {}", provider, userId);
            
            // Get the appropriate brokerage service
            BrokerageService brokerageService = getBrokerageServiceByName(provider);
            if (brokerageService == null) {
                log.warn("No brokerage service found for provider: {}", provider);
                return Map.of("error", "Unsupported brokerage provider: " + provider);
            }
            
            // Get credentials for the brokerage
            Map<String, String> credentials = credentialsRepository.getBrokerageCredentials(userId, provider);
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No valid {} credentials found for user: {}", provider, userId);
                return Map.of("error", "No valid " + provider + " credentials found");
            }
            
            // Get portfolio data from the brokerage service
            return brokerageService.getPortfolioData(credentials);
        } catch (Exception e) {
            log.error("Error getting {} portfolio data for user: {}", provider, userId, e);
            return Map.of(
                "error", "Error retrieving " + provider + " portfolio data: " + e.getMessage()
            );
        }
    }
    
    /**
     * Find the appropriate brokerage service implementation by provider name
     *
     * @param providerName Brokerage provider name
     * @return BrokerageService implementation or null if not found
     */
    private BrokerageService getBrokerageServiceByName(String providerName) {
        return brokerageServices.stream()
                .filter(service -> service.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElse(null);
    }
}
