package io.strategiz.client.coinbase;

import io.strategiz.client.coinbase.exception.CoinbaseApiException;
import io.strategiz.client.coinbase.model.CoinbaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Client for interacting with the Coinbase API
 * This class handles all direct API communication with Coinbase
 */
@Slf4j
@Component
public class CoinbaseClient {

    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String COINBASE_API_VERSION = "2021-04-29";

    private final RestTemplate restTemplate;

    public CoinbaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("CoinbaseClient initialized with API URL: {}", COINBASE_API_URL);
    }

    /**
     * Make a public request to Coinbase API (no authentication required)
     * @param method HTTP method
     * @param endpoint API endpoint (e.g., "/currencies")
     * @param params Request parameters
     * @param responseType Expected response type
     * @return API response
     */
    public <T> T publicRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              ParameterizedTypeReference<T> responseType) {
        try {
            URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
            
            if (params != null) {
                params.forEach(uriBuilder::addParameter);
            }
            
            URI uri = uriBuilder.build();
            
            log.debug("Making public request to Coinbase API: {} {}", method, uri);
            
            ResponseEntity<T> response = restTemplate.exchange(
                uri,
                method,
                null,
                responseType
            );
            
            return response.getBody();
        } catch (Exception e) {
            String errorDetails = extractErrorDetails(e);
            log.error("Error making public request to {}: {}", endpoint, errorDetails);
            throw new CoinbaseApiException("Error making public request to Coinbase API", e, errorDetails);
        }
    }

    /**
     * Make a signed request to Coinbase API (requires authentication)
     * @param method HTTP method
     * @param endpoint API endpoint (e.g., "/accounts")
     * @param params Request parameters
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase API private key
     * @param responseType Expected response type
     * @return API response
     */
    public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              String apiKey, String privateKey, ParameterizedTypeReference<T> responseType) 
            throws CoinbaseApiException {
        try {
            // Validate inputs
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new CoinbaseApiException("API key cannot be null or empty", null, "API key validation error");
            }
            if (privateKey == null || privateKey.trim().isEmpty()) {
                throw new CoinbaseApiException("Private key cannot be null or empty", null, "Private key validation error");
            }
            
            // Format the private key properly before using it
            privateKey = formatPrivateKey(privateKey);
            
            log.info("Beginning Coinbase API request preparation with key starting with: {}",
                    apiKey.substring(0, Math.min(apiKey.length(), 4)) + "...");
            
            // Build the URL
            URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
            if (params != null) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    uriBuilder.addParameter(param.getKey(), param.getValue());
                }
            }
            
            URI uri = uriBuilder.build();
            
            // Get current timestamp for signature
            long timestamp = System.currentTimeMillis() / 1000;
            
            // Create message to sign
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(timestamp)
                         .append(method.name())
                         .append(endpoint);
            
            if (params != null && !params.isEmpty()) {
                String queryParams = uri.getQuery();
                if (queryParams != null && !queryParams.isEmpty()) {
                    messageBuilder.append("?").append(queryParams);
                }
            }
            
            String message = messageBuilder.toString();
            log.info("Preparing to sign message: '{}'", message);
            
            // Generate signature
            String signature = generateSignature(message, privateKey);
            log.info("Generated signature (first 10 chars): '{}'", 
                     signature.length() > 10 ? signature.substring(0, 10) + "..." : signature);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("CB-ACCESS-KEY", apiKey);
            headers.set("CB-ACCESS-SIGN", signature);
            headers.set("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp));
            headers.set("CB-VERSION", COINBASE_API_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Make request
            ResponseEntity<T> response = restTemplate.exchange(
                uri,
                method,
                new HttpEntity<>(headers),
                responseType
            );
            
            return response.getBody();
        } catch (RestClientResponseException e) {
            // Handle Spring's RestClientResponseException which contains HTTP status and response body
            int statusCode = e.getRawStatusCode();
            String responseBody = e.getResponseBodyAsString();
            
            log.error("Coinbase API error - HTTP Status {}: {}", statusCode, responseBody);
            
            // Build a detailed error message
            String detailedError = String.format("Coinbase API returned HTTP %d: %s", statusCode, responseBody);
            throw new CoinbaseApiException(detailedError, e, responseBody);
        } catch (Exception e) {
            log.error("Error making signed request to Coinbase API: {}", e.getMessage(), e);
            throw new CoinbaseApiException("Error making signed request to Coinbase API", e);
        }
    }
    
    /**
     * Generate HMAC-SHA256 signature for Coinbase API requests
     * 
     * @param message Message to sign
     * @param privateKey Private key to sign with
     * @return Base64-encoded signature
     * @throws NoSuchAlgorithmException if the algorithm is not available
     * @throws InvalidKeyException if the key is invalid
     */
    private String generateSignature(String message, String privateKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA256);
        hmacSha256.init(secretKeySpec);
        byte[] signature = hmacSha256.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }
    
    /**
     * Format private key for Coinbase API
     * 
     * @param privateKey Private key to format
     * @return Formatted private key
     */
    private String formatPrivateKey(String privateKey) {
        // Remove any whitespace, newlines, etc.
        privateKey = privateKey.trim();
        
        // Handle PEM format if needed
        if (privateKey.startsWith("-----BEGIN PRIVATE KEY-----")) {
            privateKey = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        }
        
        return privateKey;
    }
    
    /**
     * Extract error details from an exception
     * 
     * @param e Exception to extract details from
     * @return Error details as a string
     */
    private String extractErrorDetails(Exception e) {
        if (e instanceof HttpStatusCodeException) {
            return ((HttpStatusCodeException) e).getResponseBodyAsString();
        } else if (e instanceof ResourceAccessException) {
            return "Connection error: " + e.getMessage();
        } else {
            return e.getMessage();
        }
    }
    
    /**
     * Test connection to Coinbase API
     * 
     * @param apiKey API key
     * @param privateKey Private key
     * @return true if successful, false otherwise
     */
    public boolean testConnection(String apiKey, String privateKey) {
        try {
            // Make a simple request to test the connection
            Map<String, String> params = new HashMap<>();
            ParameterizedTypeReference<Map<String, Object>> responseType = 
                new ParameterizedTypeReference<Map<String, Object>>() {};
            
            Map<String, Object> response = signedRequest(
                HttpMethod.GET,
                "/user",
                params,
                apiKey,
                privateKey,
                responseType
            );
            
            return response != null && response.containsKey("data");
        } catch (Exception e) {
            log.error("Error testing Coinbase API connection", e);
            return false;
        }
    }
}
