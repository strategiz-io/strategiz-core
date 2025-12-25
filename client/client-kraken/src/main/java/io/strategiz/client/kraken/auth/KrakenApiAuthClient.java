package io.strategiz.client.kraken.auth;

import io.strategiz.client.base.exception.ClientErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kraken API Authentication Client
 * Handles API key authentication and request signing for Kraken API
 */
@Service
public class KrakenApiAuthClient {
    
    private static final Logger log = LoggerFactory.getLogger(KrakenApiAuthClient.class);
    
    private static final String KRAKEN_API_BASE_URL = "https://api.kraken.com";
    private static final String API_VERSION = "0";
    
    private final WebClient.Builder webClientBuilder;
    
    public KrakenApiAuthClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    /**
     * Creates a Kraken API signature for authentication
     * 
     * @param path API endpoint path
     * @param nonce Unique increasing integer
     * @param postData POST body data
     * @param apiSecret Private API key (base64 encoded)
     * @return Base64 encoded signature
     */
    public String createSignature(String path, String nonce, String postData, String apiSecret) {
        try {
            // Add validation and logging
            if (apiSecret == null || apiSecret.trim().isEmpty()) {
                log.error("API secret is null or empty");
                throw new StrategizException(ClientErrorDetails.MISSING_CREDENTIALS,
                    "kraken-api", "API secret is null or empty");
            }
            
            log.debug("Creating signature for path: {}, API secret length: {}", path, apiSecret.length());
            
            // Step 1: Calculate SHA256 of (nonce + postData)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String nonceAndData = nonce + postData;
            byte[] sha256Hash = sha256.digest(nonceAndData.getBytes(StandardCharsets.UTF_8));
            
            // Step 2: Decode the API secret from base64
            byte[] decodedSecret;
            try {
                decodedSecret = Base64.decodeBase64(apiSecret);
                log.debug("Successfully decoded API secret, decoded length: {}", decodedSecret.length);
            } catch (Exception e) {
                log.error("Failed to decode API secret from base64: {}", e.getMessage());
                throw new StrategizException(ClientErrorDetails.INVALID_CREDENTIALS, e,
                    "kraken-api", "Invalid base64 API secret");
            }
            
            // Step 3: Concatenate path and SHA256 hash
            byte[] pathBytes = path.getBytes(StandardCharsets.UTF_8);
            byte[] message = new byte[pathBytes.length + sha256Hash.length];
            System.arraycopy(pathBytes, 0, message, 0, pathBytes.length);
            System.arraycopy(sha256Hash, 0, message, pathBytes.length, sha256Hash.length);
            
            // Step 4: Calculate HMAC-SHA512
            Mac mac = Mac.getInstance(HmacAlgorithms.HMAC_SHA_512.toString());
            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedSecret, HmacAlgorithms.HMAC_SHA_512.toString());
            mac.init(secretKeySpec);
            byte[] hmacHash = mac.doFinal(message);
            
            // Step 5: Encode to base64
            return Base64.encodeBase64String(hmacHash);
            
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating Kraken signature", e);
            throw new StrategizException(ClientErrorDetails.SIGNATURE_GENERATION_FAILED, "client-kraken", e);
        }
    }
    
    /**
     * Makes an authenticated request to Kraken API
     * 
     * @param endpoint API endpoint (e.g., "/private/Balance")
     * @param apiKey Public API key
     * @param apiSecret Private API key
     * @param params Request parameters
     * @param otp Optional one-time password for 2FA
     * @return Response as Map
     */
    public Mono<Map<String, Object>> makeAuthenticatedRequest(
            String endpoint, 
            String apiKey, 
            String apiSecret,
            Map<String, String> params,
            String otp) {
        
        String path = "/" + API_VERSION + endpoint;
        String nonce = String.valueOf(Instant.now().toEpochMilli());
        
        // Build POST data
        StringBuilder postData = new StringBuilder("nonce=" + nonce);
        if (params != null) {
            params.forEach((key, value) -> 
                postData.append("&").append(key).append("=").append(value)
            );
        }
        if (otp != null && !otp.isEmpty()) {
            postData.append("&otp=").append(otp);
        }
        
        // Create signature
        String signature = createSignature(path, nonce, postData.toString(), apiSecret);
        
        // Make request
        WebClient webClient = webClientBuilder
                .baseUrl(KRAKEN_API_BASE_URL)
                .build();
                
        return webClient.post()
                .uri(path)
                .header("API-Key", apiKey)
                .header("API-Sign", signature)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(postData.toString())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (Map<String, Object>) response)
                .doOnSuccess(response -> log.debug("Kraken API response: {}", response))
                .doOnError(error -> log.error("Kraken API error", error));
    }
    
    /**
     * Test API connection and fetch account balance
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param otp Optional OTP
     * @return Account balance response
     */
    public Mono<Map<String, Object>> getAccountBalance(String apiKey, String apiSecret, String otp) {
        return makeAuthenticatedRequest("/private/Balance", apiKey, apiSecret, null, otp);
    }
    
    /**
     * Test connection to Kraken API
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param otp Optional OTP
     * @return true if connection successful
     */
    public boolean testConnection(String apiKey, String apiSecret, String otp) {
        try {
            Map<String, Object> response = getAccountBalance(apiKey, apiSecret, otp).block();
            
            if (response == null) {
                log.warn("Null response from Kraken API");
                return false;
            }
            
            // Check for Kraken API errors
            if (response.containsKey("error")) {
                Object errors = response.get("error");
                if (errors instanceof java.util.List && !((java.util.List<?>) errors).isEmpty()) {
                    log.warn("Kraken API errors: {}", errors);
                    return false;
                }
            }
            
            // Check for result data
            return response.containsKey("result");
            
        } catch (Exception e) {
            log.error("Error testing Kraken connection", e);
            return false;
        }
    }
    
    /**
     * Get portfolio information including positions
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param otp Optional OTP
     * @return Portfolio positions
     */
    public Mono<Map<String, Object>> getPortfolio(String apiKey, String apiSecret, String otp) {
        Map<String, String> params = new HashMap<>();
        // Can add additional parameters if needed
        return makeAuthenticatedRequest("/private/OpenPositions", apiKey, apiSecret, params, otp);
    }
    
    /**
     * Get trade history
     * 
     * @param apiKey API key
     * @param apiSecret API secret
     * @param otp Optional OTP
     * @return Trade history
     */
    public Mono<Map<String, Object>> getTradeHistory(String apiKey, String apiSecret, String otp) {
        return makeAuthenticatedRequest("/private/TradesHistory", apiKey, apiSecret, null, otp);
    }
}