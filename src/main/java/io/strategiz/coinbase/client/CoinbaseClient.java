package io.strategiz.coinbase.client;

import io.strategiz.coinbase.client.exception.CoinbaseApiException;
import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.model.CoinbaseResponse;
import io.strategiz.coinbase.model.TickerPrice;
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
// Removing unused import
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

    public CoinbaseClient() {
        this.restTemplate = new RestTemplate();
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
     * @param passphrase Coinbase API passphrase
     * @param responseType Expected response type
     * @return API response
     */
    public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              String apiKey, String privateKey, String passphrase, ParameterizedTypeReference<T> responseType) 
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
            headers.set("CB-ACCESS-PASSPHRASE", passphrase);
            headers.set("CB-VERSION", COINBASE_API_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Log request headers for debugging
            log.info("Coinbase API request headers:");
            log.info("  CB-ACCESS-KEY: {}...", apiKey.substring(0, Math.min(apiKey.length(), 8)));
            log.info("  CB-ACCESS-SIGN: {}...", signature.substring(0, Math.min(signature.length(), 8)));
            log.info("  CB-ACCESS-TIMESTAMP: {}", timestamp);
            log.info("  CB-ACCESS-PASSPHRASE: {}", passphrase != null ? "[provided]" : "[MISSING]");
            log.info("  CB-VERSION: {}", COINBASE_API_VERSION);
            
            // Log request details for debugging
            log.info("Making Coinbase API request to {} with key {}", uri, apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            log.debug("Full request details - Method: {}, Endpoint: {}, Timestamp: {}, API Key Length: {}, Private Key Length: {}", 
                method, endpoint, timestamp, apiKey.length(), privateKey.length());
            
            // Log a sample of the generated signature for debugging
            try {
                String signaturePreview = signature.substring(0, Math.min(signature.length(), 10)) + "...";
                log.debug("Generated signature (preview): {}", signaturePreview);
            } catch (Exception e) {
                log.debug("Unable to log signature preview: {}", e.getMessage());
            }
            
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
            
            // Extract more detailed error information if available
            String errorDetails = "Unknown error";
            if (e instanceof InvalidKeyException) {
                errorDetails = "Invalid key format: " + e.getMessage();
            } else if (e instanceof NoSuchAlgorithmException) {
                errorDetails = "Algorithm error: " + e.getMessage();
            } else if (e instanceof ResourceAccessException) {
                errorDetails = "Network error: " + e.getMessage();
            } else if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException httpError = (HttpStatusCodeException) e;
                errorDetails = String.format("HTTP %d error: %s | %s", 
                                           httpError.getStatusCode().value(),
                                           httpError.getStatusCode(),
                                           httpError.getResponseBodyAsString());
            }
            
            log.error("Detailed Coinbase API error: {}", errorDetails);
            throw new CoinbaseApiException("Error making signed request to Coinbase API: " + errorDetails, e, errorDetails);
        }
    }

    /**
     * Generate HMAC SHA256 signature for Coinbase API authentication
     * @param message Message to sign
     * @param privateKey Private key
     * @return Base64 encoded signature
     */
    /**
     * Format a private key for use with Coinbase API
     * This handles different encoding formats that users might enter
     * 
     * @param rawKey The raw private key as stored in the database
     * @return A properly formatted private key
     */
    private String formatPrivateKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            log.warn("Null or empty private key provided");
            return rawKey; // Will fail validation elsewhere
        }
        
        log.debug("Formatting private key: length={}, contains dashes={}, contains underscores={}", 
                 rawKey.length(), rawKey.contains("-"), rawKey.contains("_"));
        
        // Clean the key - convert from URL-safe Base64 to standard Base64 and remove whitespace
        String cleanedKey = rawKey
            .replace('-', '+')
            .replace('_', '/')
            .replaceAll("\\s", ""); // Remove any whitespace
        
        // Add padding if needed
        int padding = 4 - (cleanedKey.length() % 4);
        if (padding < 4) {
            for (int i = 0; i < padding; i++) {
                cleanedKey += '=';
            }
            log.debug("Added {} padding characters to private key", padding);
        }
        
        return cleanedKey;
    }
    
    /**
     * Directly decode a Base64 string, handling both standard and URL-safe variants
     * 
     * @param base64String The string to decode
     * @return Decoded bytes
     */
    private byte[] decodeBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            throw new IllegalArgumentException("Private key cannot be null or empty");
        }
        
        // Log key diagnostics (safely)
        log.info("Decoding private key: length={}, first few chars={}", 
                base64String.length(), 
                base64String.substring(0, Math.min(4, base64String.length())));
        
        // Try multiple cleaning strategies in sequence
        // Each item in this array is a different cleaning approach
        String[] cleaningStrategies = {
            // Strategy 1: Original string as-is
            base64String,
            
            // Strategy 2: Convert URL-safe to standard Base64
            base64String.replace('-', '+').replace('_', '/'),
            
            // Strategy 3: Strategy 2 + padding
            addBase64Padding(base64String.replace('-', '+').replace('_', '/')),
            
            // Strategy 4: Raw key with explicit padding removal
            base64String.replace("=", ""),
            
            // Strategy 5: Only keep valid Base64 characters
            base64String.replaceAll("[^A-Za-z0-9+/=]", ""),
            
            // Strategy 6: Only keep valid URL-safe Base64 characters
            base64String.replaceAll("[^A-Za-z0-9\\-_=]", "")
        };
        
        // Try each strategy in sequence
        for (int i = 0; i < cleaningStrategies.length; i++) {
            String cleaned = cleaningStrategies[i];
            
            // Skip empty cleaning results
            if (cleaned.isEmpty()) {
                continue;
            }
            
            // Add padding if needed
            if (cleaned.length() % 4 != 0) {
                cleaned = addBase64Padding(cleaned);
            }
            
            try {
                // First try standard decoder
                try {
                    return Base64.getDecoder().decode(cleaned);
                } catch (IllegalArgumentException e) {
                    // Then try URL-safe decoder
                    return Base64.getUrlDecoder().decode(cleaned);
                }
            } catch (IllegalArgumentException e) {
                log.debug("Strategy {} failed: {}", i+1, e.getMessage());
                // Continue to next strategy
            }
        }
        
        // If all strategies failed, try one desperate approach
        try {
            log.warn("All clean strategies failed. Using raw key bytes as last resort");
            return base64String.getBytes(); 
        } catch (Exception e) {
            log.error("Fatal private key error: {}", e.getMessage());
            throw new IllegalArgumentException("Cannot decode private key after all attempts: " + e.getMessage());
        }
    }
    
    /**
     * Add proper Base64 padding to a string
     */
    private String addBase64Padding(String input) {
        // Calculate how many padding chars are needed
        int padding = 0;
        if (input.length() % 4 != 0) {
            padding = 4 - (input.length() % 4);
        }
        
        StringBuilder sb = new StringBuilder(input);
        for (int i = 0; i < padding; i++) {
            sb.append('=');
        }
        return sb.toString();
    }
    
    private String generateSignature(String message, String privateKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        try {
            log.debug("Generating signature for message of length: {} with privateKey of length: {}", 
                    message.length(), privateKey.length());
            log.debug("Message to sign: '{}'", message);
            log.debug("Private key first 4 chars: '{}'", 
                    privateKey.length() > 4 ? privateKey.substring(0, 4) + "..." : privateKey);
            
            // Check for common key format issues
            if (privateKey.startsWith("-----BEGIN")) {
                log.warn("Private key appears to be in PEM format, which is not compatible with Coinbase API");
            }
            
            // Directly use our safe decoding method
            byte[] decodedKey = decodeBase64(privateKey);
            log.debug("Successfully decoded private key, decoded length: {} bytes", decodedKey.length);
            
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(decodedKey, HMAC_SHA256);
            hmacSha256.init(secretKeySpec);
            byte[] hash = hmacSha256.doFinal(message.getBytes());
            String signature = Base64.getEncoder().encodeToString(hash);
            
            log.debug("Generated signature: '{}'", signature);
            return signature;
        } catch (Exception e) {
            log.error("Error generating signature: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            e.printStackTrace();
            throw new InvalidKeyException("Failed to generate signature: " + e.getMessage());
        }
    }

    /**
     * Get all accounts from Coinbase API
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase API private key
     * @param passphrase Coinbase API passphrase
     * @return List of accounts
     */
    public List<Account> getAccounts(String apiKey, String privateKey, String passphrase) {
        try {
            // Validate parameters first
            if (apiKey == null || apiKey.isEmpty()) {
                throw new CoinbaseApiException("API key is null or empty", null, "API key validation failed");
            }
            if (privateKey == null || privateKey.isEmpty()) {
                throw new CoinbaseApiException("Private key is null or empty", null, "Private key validation failed");
            }
            
            log.info("Getting accounts from Coinbase API with key starting with: {}", 
                    apiKey.substring(0, Math.min(apiKey.length(), 4)) + "...");
            
            // Format the private key properly before using it
            privateKey = formatPrivateKey(privateKey);
            
            CoinbaseResponse<Account> response = signedRequest(
                HttpMethod.GET,
                "/accounts",
                null,
                apiKey,
                privateKey,
                passphrase,
                new ParameterizedTypeReference<CoinbaseResponse<Account>>() {}
            );
            
            return response.getData();
        } catch (Exception e) {
            String errorDetails = extractErrorDetails(e);
            log.error("Error getting accounts from Coinbase API: {}", errorDetails);
            
            // Enhanced error reporting for 401 errors
            if (e instanceof RestClientResponseException) {
                RestClientResponseException restError = (RestClientResponseException) e;
                if (restError.getRawStatusCode() == 401) {
                    log.error("Coinbase API returned 401 Unauthorized - this typically indicates invalid API credentials");
                    log.error("Please verify that your Coinbase API key and secret are correct and have appropriate permissions");
                }
                
                String responseBody = "No response body";
                try {
                    responseBody = restError.getResponseBodyAsString();
                } catch (Exception ex) {
                    log.error("Failed to extract response body: {}", ex.getMessage());
                }
                
                throw new CoinbaseApiException(
                    "Error getting accounts from Coinbase API: HTTP " + 
                    restError.getRawStatusCode() + ": " + restError.getStatusText(), 
                    e, 
                    responseBody
                );
            }
            
            throw new CoinbaseApiException("Error getting accounts from Coinbase API", e, errorDetails);
        }
    }
    
    /**
     * Get raw account data from Coinbase API
     * This method returns the completely unmodified raw data
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase API private key
     * @param passphrase Coinbase API passphrase
     * @return Raw account data object
     * @throws CoinbaseApiException If there's an error calling the Coinbase API
     */
    public Object getRawAccountData(String apiKey, String privateKey, String passphrase) throws CoinbaseApiException {
        try {
            // Validate the API key and private key before making the request
            if (apiKey == null || apiKey.isEmpty()) {
                throw new CoinbaseApiException("API key is null or empty", null, "API key validation failed");
            }
            if (privateKey == null || privateKey.isEmpty()) {
                throw new CoinbaseApiException("Private key is null or empty", null, "Private key validation failed");
            }
            
            // Format the private key properly
            privateKey = formatPrivateKey(privateKey);
            
            log.info("Getting raw account data from Coinbase API with key starting with: {}", 
                    apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
            log.debug("API key length: {}, Private key length: {}", apiKey.length(), privateKey.length());
            
            return signedRequest(
                HttpMethod.GET,
                "/accounts",
                null,
                apiKey,
                privateKey,
                passphrase,
                new ParameterizedTypeReference<Object>() {}
            );
        } catch (Exception e) {
            // Extract more detailed error information
            String errorDetails = extractErrorDetails(e);
            
            // Log both the original error and the detailed information
            log.error("Error getting raw account data from Coinbase API: {}", e.getMessage());
            log.error("Detailed Coinbase error information: {}", errorDetails);
            
            // If it's already a CoinbaseApiException, just re-throw it
            if (e instanceof CoinbaseApiException) {
                throw (CoinbaseApiException) e;
            }
            
            // Get the specific error message if possible
            String specificMessage = e.getMessage();
            if (e instanceof RestClientResponseException) {
                RestClientResponseException restError = (RestClientResponseException) e;
                specificMessage = "HTTP " + restError.getRawStatusCode() + ": " + restError.getResponseBodyAsString();
            }
            
            // Throw a custom exception with more details
            throw new CoinbaseApiException(specificMessage, e, errorDetails);
        }
    }

    /**
     * Get ticker price for a specific currency pair
     * @param baseCurrency Base currency (e.g., BTC)
     * @param quoteCurrency Quote currency (e.g., USD)
     * @return Ticker price
     */
    public TickerPrice getTickerPrice(String baseCurrency, String quoteCurrency) {
        try {
            log.info("Getting ticker price for {}-{} from Coinbase API", baseCurrency, quoteCurrency);
            Map<String, String> params = new HashMap<>();
            params.put("currency", quoteCurrency);
            
            CoinbaseResponse<TickerPrice> response = publicRequest(
                HttpMethod.GET,
                "/prices/" + baseCurrency + "/spot",
                params,
                new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
            );
            
            return response.getData().get(0);
        } catch (Exception e) {
            String errorDetails = extractErrorDetails(e);
            log.error("Error getting ticker price for {}-{}: {}", baseCurrency, quoteCurrency, errorDetails);
            throw new CoinbaseApiException("Error getting ticker price for " + baseCurrency + "-" + quoteCurrency, e, errorDetails);
        }
    }

    /**
     * Test connection to Coinbase API
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase API private key
     * @param passphrase Coinbase API passphrase
     * @return True if connection is successful
     */
    public boolean testConnection(String apiKey, String privateKey, String passphrase) {
        try {
            log.info("Testing connection to Coinbase API");
            Object response = signedRequest(
                HttpMethod.GET,
                "/user",
                null,
                apiKey,
                privateKey,
                passphrase,
                new ParameterizedTypeReference<Object>() {}
            );
            return response != null;
        } catch (Exception e) {
            String errorDetails = extractErrorDetails(e);
            log.error("Error testing connection to Coinbase API: {}", errorDetails);
            // Don't throw an exception, just return false to indicate failed connection
            return false;
        }
    }
    
    /**
     * Extract detailed error information from an exception
     * @param e Exception to extract details from
     * @return Detailed error message
     */
    private String extractErrorDetails(Exception e) {
        StringBuilder details = new StringBuilder();
        
        // Add exception type and message
        details.append(e.getClass().getSimpleName())
               .append(": ")
               .append(e.getMessage());
        
        // Check for RestClientException with response body
        if (e instanceof org.springframework.web.client.RestClientResponseException) {
            org.springframework.web.client.RestClientResponseException responseException = 
                (org.springframework.web.client.RestClientResponseException) e;
            
            details.append(" | Status code: ")
                   .append(responseException.getRawStatusCode())
                   .append(" | Response body: ")
                   .append(responseException.getResponseBodyAsString());
        }
        
        // Add first level of cause if available
        if (e.getCause() != null) {
            details.append(" | Cause: ")
                   .append(e.getCause().getClass().getSimpleName())
                   .append(": ")
                   .append(e.getCause().getMessage());
        }
        
        return details.toString();
    }
}
