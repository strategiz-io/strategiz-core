package io.strategiz.service.exchange.coinbase;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.exchange.exception.ExchangeErrorDetails;
import io.strategiz.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with the Coinbase API
 * This service handles all communication with the Coinbase API and provides
 * methods for accessing account data and other information.
 *
 * IMPORTANT: This service always uses real API data, never mock responses.
 */
@Service
public class CoinbaseService extends BaseService {

    @Override
    protected String getModuleName() {
        return "service-exchange";
    }

    private static final String COINBASE_API_URL = "https://api.coinbase.com";
    
    @Autowired
    private CoinbaseCloudService coinbaseCloudService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Get raw account data from Coinbase API
     * This returns the completely unmodified object from the API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key (PEM format)
     * @return Raw account data as received from the API
     */
    public Object getRawAccountData(String apiKey, String privateKey) {
        try {
            log.info("Getting raw account data from Coinbase API");
            
            // Generate JWT token for authentication
            String jwtToken = coinbaseCloudService.generateJwtToken(apiKey, privateKey);
            
            // Set up headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("CB-VERSION", "2023-04-01");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make API call to get accounts
            String url = COINBASE_API_URL + "/v2/accounts";
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            log.info("Successfully retrieved raw account data from Coinbase API");
            return response.getBody();
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting raw account data from Coinbase API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.COINBASE_ACCOUNT_FETCH_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Check if the Coinbase API is available (for health monitoring)
     * This method performs a lightweight check without requiring authentication
     * 
     * @return true if API is available, false otherwise
     */
    public boolean isApiAvailable() {
        try {
            log.debug("Checking Coinbase API availability");
            String url = COINBASE_API_URL + "/v2/currencies";
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Object.class
            );
            boolean available = response.getStatusCode().is2xxSuccessful();
            log.debug("Coinbase API available: {}", available);
            return available;
        } catch (Exception e) {
            log.debug("Coinbase API unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Test connection to Coinbase API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key (PEM format)
     * @return Connection status
     */
    public Map<String, Object> testConnection(String apiKey, String privateKey) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Testing connection to Coinbase API");
            
            // Generate JWT token for authentication
            String jwtToken = coinbaseCloudService.generateJwtToken(apiKey, privateKey);
            
            // Set up headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("CB-VERSION", "2023-04-01");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make a simple API call to test the connection
            String url = COINBASE_API_URL + "/v2/user";
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            // Check response status
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new StrategizException(ExchangeErrorDetails.COINBASE_CONNECTION_FAILED, "service-exchange");
            }
            
            log.info("Successfully tested connection to Coinbase API: {}", response.getStatusCode());
            result.put("status", "ok");
            result.put("message", "Connection successful");
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("Error testing connection to Coinbase API: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
}
