package io.strategiz.client.coingecko;

import io.strategiz.framework.exception.ApplicationClientException;
import io.strategiz.client.coingecko.model.CryptoCurrency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client for interacting with the CoinGecko API.
 * This class handles all low-level API communication with CoinGecko following Synapse patterns.
 */
@Component
public class CoinGeckoClient {
    
    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);
    private static final String MARKETS_ENDPOINT = "/coins/markets";
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String GLOBAL_ENDPOINT = "/global";
    
    @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}")
    private String baseUrl;
    
    @Value("${coingecko.api.key:}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    
    /**
     * Constructor with dependency injection for RestTemplate
     * 
     * @param coinGeckoRestTemplate RestTemplate configured for CoinGecko API
     */
    public CoinGeckoClient(@Qualifier("coinGeckoRestTemplate") RestTemplate coinGeckoRestTemplate) {
        this.restTemplate = coinGeckoRestTemplate;
        log.info("CoinGeckoClient initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Get cryptocurrency market data for multiple currencies
     * 
     * @param coinIds List of coin IDs to fetch (e.g. "bitcoin", "ethereum")
     * @param currency Currency to get prices in (e.g. "usd")
     * @return List of CryptoCurrency objects with market data
     */
    @Cacheable(value = "cryptoMarketData", key = "#coinIds.toString() + '-' + #currency")
    public List<CryptoCurrency> getCryptocurrencyMarketData(List<String> coinIds, String currency) {
        log.info("Fetching market data for coins: {} in currency: {}", coinIds, currency);
        
        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + MARKETS_ENDPOINT)
                    .queryParam("vs_currency", currency)
                    .queryParam("ids", String.join(",", coinIds))
                    .queryParam("order", "market_cap_desc")
                    .queryParam("per_page", 100)
                    .queryParam("page", 1)
                    .queryParam("sparkline", false)
                    .queryParam("price_change_percentage", "24h");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-cg-api-key", apiKey);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<CryptoCurrency>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CryptoCurrency>>() {}
            );
            
            if (response.getBody() == null) {
                log.error("Empty response body from CoinGecko API for coins: {}", coinIds);
                return Collections.emptyList();
            }
            
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error fetching market data from CoinGecko API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to retrieve cryptocurrency market data", e);
        }
    }
    
    /**
     * Search for cryptocurrencies by query
     * 
     * @param query Search query (e.g. "bitcoin")
     * @return List of search results
     */
    @Cacheable(value = "cryptoSearch", key = "#query")
    public List<Map<String, Object>> searchCryptocurrencies(String query) {
        log.info("Searching for cryptocurrencies with query: {}", query);
        
        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + SEARCH_ENDPOINT)
                    .queryParam("query", query);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-cg-api-key", apiKey);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, List<Map<String, Object>>>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, List<Map<String, Object>>>>() {}
            );
            
            Map<String, List<Map<String, Object>>> body = response.getBody();
            if (body == null || !body.containsKey("coins")) {
                log.error("Invalid response format from CoinGecko search API");
                return Collections.emptyList();
            }
            
            // Return the first 10 results
            List<Map<String, Object>> coins = body.get("coins");
            return coins.subList(0, Math.min(coins.size(), 10));
        } catch (RestClientException e) {
            log.error("Error searching cryptocurrencies from CoinGecko API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to search cryptocurrencies", e);
        }
    }
    
    /**
     * Get top cryptocurrencies by market cap
     * 
     * @param currency Currency to get prices in (e.g. "usd")
     * @param limit Number of cryptocurrencies to retrieve
     * @return List of CryptoCurrency objects with market data
     */
    @Cacheable(value = "topCryptos", key = "#currency + '-' + #limit")
    public List<CryptoCurrency> getTopCryptocurrencies(String currency, int limit) {
        log.info("Fetching top {} cryptocurrencies in {}", limit, currency);
        
        try {
            // Build URL with query parameters
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + MARKETS_ENDPOINT)
                    .queryParam("vs_currency", currency)
                    .queryParam("order", "market_cap_desc")
                    .queryParam("per_page", limit)
                    .queryParam("page", 1)
                    .queryParam("sparkline", false)
                    .queryParam("price_change_percentage", "24h");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-cg-api-key", apiKey);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<CryptoCurrency>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CryptoCurrency>>() {}
            );
            
            List<CryptoCurrency> body = response.getBody();
            if (body == null) {
                log.error("Empty response body from CoinGecko API for top cryptocurrencies");
                return Collections.emptyList();
            }
            
            return body;
        } catch (RestClientException e) {
            log.error("Error fetching top cryptocurrencies from CoinGecko API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to retrieve top cryptocurrencies", e);
        }
    }
    
    /**
     * Get global cryptocurrency market data
     * 
     * @return Map containing global market data like total market cap, trading volume, etc.
     */
    @Cacheable(value = "globalMarketData", key = "'global'")
    public Map<String, Object> getGlobalMarketData() {
        log.info("Fetching global cryptocurrency market data");
        
        try {
            // Build URL for global endpoint
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + GLOBAL_ENDPOINT);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Add API key if available
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-cg-api-key", apiKey);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                log.error("Invalid response format from CoinGecko global API");
                return Collections.emptyMap();
            }
            
            // Return the data object which contains all the global market stats
            Object dataObj = body.get("data");
            if (dataObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) dataObj;
                return result;
            } else {
                log.error("Unexpected data type in CoinGecko global API response");
                return Collections.emptyMap();
            }
        } catch (RestClientException e) {
            log.error("Error fetching global market data from CoinGecko API: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to retrieve global cryptocurrency market data", e);
        }
    }
}
