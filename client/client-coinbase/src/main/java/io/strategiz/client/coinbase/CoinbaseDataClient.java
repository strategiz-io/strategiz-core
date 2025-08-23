package io.strategiz.client.coinbase;

import io.strategiz.framework.exception.StrategizException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching data from Coinbase using OAuth access tokens
 * This class handles all account data retrieval operations
 */
@Component
public class CoinbaseDataClient {
    
    private static final Logger log = LoggerFactory.getLogger(CoinbaseDataClient.class);
    
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2";
    private static final String COINBASE_API_VERSION = "2021-04-29";
    
    private final RestTemplate restTemplate;
    
    public CoinbaseDataClient(@Qualifier("coinbaseRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("CoinbaseDataClient initialized with API URL: {}", COINBASE_API_URL);
    }
    
    /**
     * Get current user information
     * @param accessToken OAuth access token
     * @return User information
     */
    public Map<String, Object> getCurrentUser(String accessToken) {
        return makeAuthenticatedRequest(HttpMethod.GET, "/user", accessToken, null);
    }
    
    /**
     * Get all user accounts (wallets)
     * @param accessToken OAuth access token
     * @return List of accounts with balances
     */
    public List<Map<String, Object>> getAccounts(String accessToken) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/accounts", accessToken, null
        );
        return extractDataList(response);
    }
    
    /**
     * Get specific account details
     * @param accessToken OAuth access token
     * @param accountId Account ID
     * @return Account details
     */
    public Map<String, Object> getAccount(String accessToken, String accountId) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/accounts/" + accountId, accessToken, null
        );
        return extractData(response);
    }
    
    /**
     * Get account transactions
     * @param accessToken OAuth access token
     * @param accountId Account ID
     * @param limit Number of transactions to fetch
     * @return List of transactions
     */
    public List<Map<String, Object>> getTransactions(String accessToken, String accountId, Integer limit) {
        Map<String, String> params = null;
        if (limit != null) {
            params = Map.of("limit", limit.toString());
        }
        
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/accounts/" + accountId + "/transactions", accessToken, params
        );
        return extractDataList(response);
    }
    
    /**
     * Get all transactions across all accounts
     * @param accessToken OAuth access token
     * @param limit Number of transactions to fetch
     * @return List of transactions
     */
    public List<Map<String, Object>> getAllTransactions(String accessToken, Integer limit) {
        Map<String, String> params = null;
        if (limit != null) {
            params = Map.of("limit", limit.toString());
        }
        
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/transactions", accessToken, params
        );
        return extractDataList(response);
    }
    
    /**
     * Get current buy price for a currency pair
     * @param accessToken OAuth access token
     * @param currencyPair Currency pair (e.g., "BTC-USD")
     * @return Price information
     */
    public Map<String, Object> getBuyPrice(String accessToken, String currencyPair) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/prices/" + currencyPair + "/buy", accessToken, null
        );
        return extractData(response);
    }
    
    /**
     * Get current sell price for a currency pair
     * @param accessToken OAuth access token
     * @param currencyPair Currency pair (e.g., "BTC-USD")
     * @return Price information
     */
    public Map<String, Object> getSellPrice(String accessToken, String currencyPair) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/prices/" + currencyPair + "/sell", accessToken, null
        );
        return extractData(response);
    }
    
    /**
     * Get current spot price for a currency pair
     * @param accessToken OAuth access token
     * @param currencyPair Currency pair (e.g., "BTC-USD")
     * @return Price information
     */
    public Map<String, Object> getSpotPrice(String accessToken, String currencyPair) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/prices/" + currencyPair + "/spot", accessToken, null
        );
        return extractData(response);
    }
    
    /**
     * Get exchange rates
     * @param accessToken OAuth access token
     * @param currency Base currency
     * @return Exchange rates
     */
    public Map<String, Object> getExchangeRates(String accessToken, String currency) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/exchange-rates?currency=" + currency, accessToken, null
        );
        return extractData(response);
    }
    
    /**
     * Get payment methods
     * @param accessToken OAuth access token
     * @return List of payment methods
     */
    public List<Map<String, Object>> getPaymentMethods(String accessToken) {
        Map<String, Object> response = makeAuthenticatedRequest(
            HttpMethod.GET, "/payment-methods", accessToken, null
        );
        return extractDataList(response);
    }
    
    /**
     * Make an authenticated request to Coinbase API
     */
    private Map<String, Object> makeAuthenticatedRequest(HttpMethod method, String endpoint, 
                                                         String accessToken, Map<String, String> params) {
        try {
            // Validate access token
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR, 
                    "Access token is required");
            }
            
            // Build the URL
            URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
            if (params != null) {
                params.forEach(uriBuilder::addParameter);
            }
            
            URI uri = uriBuilder.build();
            
            // Create headers with OAuth Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("CB-VERSION", COINBASE_API_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            log.debug("Making authenticated request to Coinbase API: {} {}", method, uri);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                uri,
                method,
                entity,
                Map.class
            );
            
            if (response.getBody() == null) {
                throw new StrategizException(CoinbaseErrors.INVALID_RESPONSE, 
                    "Empty response from Coinbase API");
            }
            
            return response.getBody();
            
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            
            log.error("Coinbase API request error - HTTP Status {}: {}", statusCode, responseBody);
            
            // Handle token expiration
            if (statusCode == 401) {
                throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR, 
                    "Access token expired or invalid. Please reconnect your Coinbase account.");
            }
            
            throw new StrategizException(CoinbaseErrors.API_ERROR, 
                "Coinbase API error: " + responseBody);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error making authenticated request to {}: {}", endpoint, e.getMessage());
            throw new StrategizException(CoinbaseErrors.API_ERROR, 
                "Failed to communicate with Coinbase: " + e.getMessage());
        }
    }
    
    /**
     * Extract data object from Coinbase API response
     */
    private Map<String, Object> extractData(Map<String, Object> response) {
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }
        }
        return response;
    }
    
    /**
     * Extract data list from Coinbase API response
     */
    private List<Map<String, Object>> extractDataList(Map<String, Object> response) {
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        }
        return Collections.emptyList();
    }
}