package io.strategiz.client.binanceus.auth;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Binance US API Authentication Client
 * Handles API key authentication and request signing for Binance US API
 */
@Service
public class BinanceUSApiAuthClient {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSApiAuthClient.class);
    
    private static final String BINANCE_US_API_BASE_URL = "https://api.binance.us";
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    private final WebClient.Builder webClientBuilder;
    
    public BinanceUSApiAuthClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    /**
     * Create HMAC SHA256 signature for Binance US API
     * 
     * @param queryString The query string to sign
     * @param apiSecret The API secret key
     * @return Hex encoded signature
     */
    public String createSignature(String queryString, String apiSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(queryString.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
            
        } catch (Exception e) {
            log.error("Error creating Binance US signature", e);
            throw new RuntimeException("Failed to create signature", e);
        }
    }
    
    /**
     * Make an authenticated request to Binance US API
     * 
     * @param endpoint API endpoint (e.g., "/api/v3/account")
     * @param apiKey Public API key
     * @param apiSecret Private API key
     * @param params Additional parameters (timestamp will be added automatically)
     * @return Response as Map
     */
    public Mono<Map<String, Object>> makeAuthenticatedRequest(
            String endpoint, 
            String apiKey, 
            String apiSecret,
            Map<String, String> params) {
        
        // Add timestamp
        long timestamp = System.currentTimeMillis();
        
        // Build query string
        StringBuilder queryString = new StringBuilder("timestamp=" + timestamp);
        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> 
                queryString.append("&").append(key).append("=").append(value)
            );
        }
        
        // Create signature
        String signature = createSignature(queryString.toString(), apiSecret);
        queryString.append("&signature=").append(signature);
        
        // Build URL
        String url = BINANCE_US_API_BASE_URL + endpoint + "?" + queryString;
        
        // Make request
        WebClient webClient = webClientBuilder
                .baseUrl(BINANCE_US_API_BASE_URL)
                .build();
                
        return webClient.get()
                .uri(url)
                .header("X-MBX-APIKEY", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> log.debug("Binance US API response: {}", response))
                .doOnError(error -> log.error("Binance US API error", error));
    }
    
    /**
     * Test API connection by fetching account information
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @return Account information
     */
    public Mono<Map<String, Object>> getAccountInfo(String apiKey, String apiSecret) {
        return makeAuthenticatedRequest("/api/v3/account", apiKey, apiSecret, null);
    }
    
    /**
     * Test connection to Binance US API
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @return true if connection successful
     */
    public boolean testConnection(String apiKey, String apiSecret) {
        try {
            Map<String, Object> response = getAccountInfo(apiKey, apiSecret).block();
            
            if (response == null) {
                log.warn("Null response from Binance US API");
                return false;
            }
            
            // Check for API errors
            if (response.containsKey("code") && response.containsKey("msg")) {
                log.warn("Binance US API error: {} - {}", response.get("code"), response.get("msg"));
                return false;
            }
            
            // Check for account data
            return response.containsKey("accountType");
            
        } catch (Exception e) {
            log.error("Error testing Binance US connection", e);
            return false;
        }
    }
    
    /**
     * Get account balances
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @return Account balances
     */
    public Mono<Map<String, Object>> getAccountBalances(String apiKey, String apiSecret) {
        return makeAuthenticatedRequest("/api/v3/account", apiKey, apiSecret, null)
            .map(response -> {
                // Extract balances from account response
                if (response.containsKey("balances")) {
                    return Map.of("balances", response.get("balances"));
                }
                return response;
            });
    }
    
    /**
     * Get open orders
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param symbol Optional symbol filter
     * @return Open orders
     */
    public Mono<Map<String, Object>> getOpenOrders(String apiKey, String apiSecret, String symbol) {
        Map<String, String> params = null;
        if (symbol != null && !symbol.isEmpty()) {
            params = Map.of("symbol", symbol);
        }
        return makeAuthenticatedRequest("/api/v3/openOrders", apiKey, apiSecret, params);
    }
    
    /**
     * Get trade history
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param symbol Trading symbol (required)
     * @param limit Number of trades to return
     * @return Trade history
     */
    public Mono<Map<String, Object>> getTradeHistory(String apiKey, String apiSecret, String symbol, Integer limit) {
        Map<String, String> params = Map.of(
            "symbol", symbol,
            "limit", limit != null ? limit.toString() : "500"
        );
        return makeAuthenticatedRequest("/api/v3/myTrades", apiKey, apiSecret, params);
    }
}