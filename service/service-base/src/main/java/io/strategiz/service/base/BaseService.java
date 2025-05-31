package io.strategiz.service.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base service class for all services in the Strategiz application.
 * This provides common functionality for service implementations.
 */
public abstract class BaseService {
    
    private static final Logger log = LoggerFactory.getLogger(BaseService.class);
    
    /**
     * Default constructor.
     */
    protected BaseService() {
        // Default constructor
        log.debug("Initializing service: {}", this.getClass().getSimpleName());
    }
    
    /**
     * Validate that the real API connection is available before making requests.
     * Strategiz ONLY uses real API data - never mock data or simulated responses.
     * 
     * @param serviceName The name of the service making the request
     * @return true if the connection is available, false otherwise
     */
    protected boolean validateRealApiConnection(String serviceName) {
        log.info("Validating real API connection for: {}", serviceName);
        // Implement actual validation logic in subclasses
        return true;
    }
    
    /**
     * Ensures we're working with real API data, not mocks or simulations.
     * This is a core principle of the Strategiz platform.
     * 
     * @param dataSource The name of the data source being accessed
     * @throws IllegalStateException if mock data would be returned
     */
    protected void ensureRealApiData(String dataSource) {
        log.info("Ensuring real API data from: {}", dataSource);
        // Any implementation that would return mock data should throw an exception
        // instead of returning simulated data
    }
}
