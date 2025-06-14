package io.strategiz.service.portfolio;

import java.util.Map;

/**
 * Interface for all brokerage service implementations
 */
public interface BrokerageService {

    /**
     * Get portfolio data from the brokerage
     * 
     * @param credentials Map of credentials required by the brokerage
     * @return Portfolio data in a standardized format
     */
    Map<String, Object> getPortfolioData(Map<String, String> credentials);
    
    /**
     * Get raw account data from the brokerage
     * 
     * @param credentials Map of credentials required by the brokerage
     * @return Raw account data from the brokerage API
     */
    Object getRawAccountData(Map<String, String> credentials);
    
    /**
     * Get the name of the brokerage provider
     * 
     * @return Provider name (e.g., "robinhood", "coinbase")
     */
    String getProviderName();
}
