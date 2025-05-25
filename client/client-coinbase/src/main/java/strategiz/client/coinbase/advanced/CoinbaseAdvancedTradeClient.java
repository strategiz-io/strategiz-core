package strategiz.client.coinbase.advanced;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import strategiz.client.coinbase.exception.CoinbaseApiException;
import strategiz.client.coinbase.util.CoinbaseJwtUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Client for interacting with the Coinbase Advanced Trade API
 * This class handles direct API communication with Coinbase Advanced Trade
 */
@Component
@Slf4j
public class CoinbaseAdvancedTradeClient {
    private static final String BASE_URL = "https://api.coinbase.com/api/v3/brokerage/accounts";
    
    @Autowired
    @Qualifier("coinbaseRestTemplate")
    private RestTemplate restTemplate;

    /**
     * Get account information from Coinbase Advanced Trade API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return Response containing account information
     */
    public ResponseEntity<String> getAccounts(String apiKey, String privateKey) {
        log.info("Getting accounts from Coinbase Advanced Trade API");
        
        try {
            // Generate JWT for authentication
            String jwt = CoinbaseJwtUtil.generateJwt(apiKey, privateKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("CB-ACCESS-KEY", apiKey);
            headers.set("CB-ACCESS-SIGN", jwt);
            headers.set("CB-VERSION", "2023-01-01");
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            
            log.info("Making request to Coinbase Advanced Trade API: GET {}", BASE_URL);
            
            // Important: Always use real API data, never mock responses
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL, HttpMethod.GET, entity, String.class);
            
            log.info("Received response from Coinbase Advanced Trade API: {}", response.getStatusCode());
            
            return response;
        } catch (Exception e) {
            log.error("Error getting accounts from Coinbase Advanced Trade API: {}", e.getMessage(), e);
            throw new CoinbaseApiException("Error accessing Coinbase Advanced Trade API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get product information from Coinbase Advanced Trade API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @param productId Product ID (e.g., "BTC-USD")
     * @return Response containing product information
     */
    public ResponseEntity<String> getProduct(String apiKey, String privateKey, String productId) {
        log.info("Getting product information from Coinbase Advanced Trade API for product: {}", productId);
        
        try {
            String productUrl = "https://api.coinbase.com/api/v3/brokerage/products/" + productId;
            
            // Generate JWT for authentication
            String jwt = CoinbaseJwtUtil.generateJwt(apiKey, privateKey);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("CB-ACCESS-KEY", apiKey);
            headers.set("CB-ACCESS-SIGN", jwt);
            headers.set("CB-VERSION", "2023-01-01");
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            
            log.info("Making request to Coinbase Advanced Trade API: GET {}", productUrl);
            
            // Important: Always use real API data, never mock responses
            ResponseEntity<String> response = restTemplate.exchange(productUrl, HttpMethod.GET, entity, String.class);
            
            log.info("Received response from Coinbase Advanced Trade API: {}", response.getStatusCode());
            
            return response;
        } catch (Exception e) {
            log.error("Error getting product information from Coinbase Advanced Trade API: {}", e.getMessage(), e);
            throw new CoinbaseApiException("Error accessing Coinbase Advanced Trade API: " + e.getMessage(), e);
        }
    }
}
