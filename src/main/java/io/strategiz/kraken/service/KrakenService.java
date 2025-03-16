package io.strategiz.kraken.service;

import io.strategiz.kraken.model.KrakenAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KrakenService {

    @Value("${kraken.api.url:https://api.kraken.com}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    public KrakenService() {
        log.info("KrakenService initialized");
    }

    /**
     * Configure the Kraken API credentials
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Configuration status
     */
    public Map<String, String> configure(String apiKey, String secretKey) {
        Map<String, String> response = new HashMap<>();
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API Key and Secret Key are required");
            return response;
        }

        try {
            // Test connection with the provided credentials
            KrakenAccount account = getAccount(apiKey, secretKey);
            if (account != null && account.getError() != null && account.getError().length > 0) {
                response.put("status", "error");
                response.put("message", String.join(", ", account.getError()));
                return response;
            }
            response.put("status", "success");
            response.put("message", "Kraken API credentials configured successfully");
            return response;
        } catch (Exception e) {
            log.error("Error configuring Kraken API credentials", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * Get account information
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Account information - completely unmodified raw data from Kraken API
     */
    public KrakenAccount getAccount(String apiKey, String secretKey) {
        log.info("Starting Kraken getAccount API call");
        
        try {
            // Validate API key and secret key
            if (apiKey == null || apiKey.isEmpty()) {
                log.error("API key is null or empty");
                throw new IllegalArgumentException("API key cannot be null or empty");
            }
            
            if (secretKey == null || secretKey.isEmpty()) {
                log.error("Secret key is null or empty");
                throw new IllegalArgumentException("Secret key cannot be null or empty");
            }
            
            log.info("API key format check - length: {}, contains slashes: {}, contains plus: {}", 
                apiKey.length(), 
                apiKey.contains("/"), 
                apiKey.contains("+"));
            
            log.info("Secret key format check - length: {}, contains slashes: {}, contains plus: {}", 
                secretKey.length(), 
                secretKey.contains("/"), 
                secretKey.contains("+"));
            
            // Create URI
            String endpoint = "/0/private/Balance";
            String url = baseUrl + endpoint;
            URI uri = URI.create(url);
            log.info("Kraken API URL: {}", url);
            
            // Create nonce
            String nonce = String.valueOf(System.currentTimeMillis());
            log.info("Generated nonce: {}", nonce);
            
            // Create data
            String data = "nonce=" + nonce;
            log.info("Request data: {}", data);
            
            // Create signature
            try {
                String signature = createSignature(endpoint, Long.parseLong(nonce), data, secretKey);
                log.info("Generated signature with length: {}", signature.length());
                
                // Create headers
                HttpHeaders headers = new HttpHeaders();
                headers.add("API-Key", apiKey);
                headers.add("API-Sign", signature);
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                log.info("Headers prepared with API-Key: {}", apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...");
                
                // Create request entity
                HttpEntity<String> entity = new HttpEntity<>(data, headers);
                log.info("Request entity created");
                
                // Log the full request details for debugging
                log.info("Making Kraken API request to: {} with method: {}", uri, HttpMethod.POST);
                log.info("Request headers: {}", headers);
                log.info("Request body: {}", data);
                
                // Execute request with timeout handling
                long startTime = System.currentTimeMillis();
                log.info("Sending request to Kraken API...");
                
                ResponseEntity<KrakenAccount> response = restTemplate.exchange(uri, HttpMethod.POST, entity, KrakenAccount.class);
                
                long endTime = System.currentTimeMillis();
                log.info("Kraken API response received in {}ms", endTime - startTime);
                log.info("Response status: {}", response.getStatusCode());
                
                KrakenAccount account = response.getBody();
                
                // Log response details
                if (account != null) {
                    if (account.getError() != null && account.getError().length > 0) {
                        log.error("Kraken API returned error: {}", String.join(", ", account.getError()));
                    } else {
                        log.info("Kraken API call successful");
                        if (account.getResult() != null) {
                            log.info("Account balance contains {} assets", account.getResult().size());
                        } else {
                            log.warn("Account balance result is null");
                        }
                    }
                } else {
                    log.warn("Kraken API returned null response body");
                }
                
                return account;
            } catch (Exception e) {
                log.error("Error creating signature: {}", e.getMessage(), e);
                throw new RuntimeException("Error creating signature: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error calling Kraken API: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling Kraken API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test the connection with the Kraken API
     * This method makes a simple API call to verify that the credentials are valid
     * 
     * @param apiKey Kraken API key
     * @param secretKey Kraken API secret key
     * @return true if the connection is successful, false otherwise
     */
    public boolean testConnection(String apiKey, String secretKey) {
        log.info("Testing connection to Kraken API");
        
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.error("API key or secret key is null or empty");
            return false;
        }
        
        try {
            // Use the GetAccountBalance endpoint to test the connection
            String endpoint = "/0/private/Balance";
            String url = baseUrl + endpoint;
            
            long nonce = System.currentTimeMillis();
            String postData = "nonce=" + nonce;
            
            // Create signature
            String signature = createSignature(endpoint, nonce, postData, secretKey);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("API-Key", apiKey);
            headers.set("API-Sign", signature);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Create request entity
            HttpEntity<String> entity = new HttpEntity<>(postData, headers);
            
            // Make the request
            log.info("Making test request to Kraken API: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            // Check if the request was successful
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Connection test successful: Status code {}", response.getStatusCode());
                return true;
            } else {
                log.error("Connection test failed: Status code {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error testing connection to Kraken API: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Create signature for Kraken API request
     * 
     * @param path API endpoint path
     * @param nonce Nonce value
     * @param postData POST data
     * @param secret Secret key
     * @return Signature
     */
    private String createSignature(String path, long nonce, String postData, String secret) {
        try {
            log.info("Creating signature for path: {}, nonce: {}", path, nonce);
            
            // Step 1: Create the SHA256 hash of (nonce + postData)
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] noncePostDataHash = md.digest((nonce + postData).getBytes());
            
            // Step 2: Concatenate path bytes with the hash from step 1
            byte[] pathBytes = path.getBytes();
            byte[] message = new byte[pathBytes.length + noncePostDataHash.length];
            System.arraycopy(pathBytes, 0, message, 0, pathBytes.length);
            System.arraycopy(noncePostDataHash, 0, message, pathBytes.length, noncePostDataHash.length);
            
            // Step 3: Apply HMAC-SHA512 using the base64-decoded secret key
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA512");
            mac.init(key);
            byte[] hmacDigest = mac.doFinal(message);
            
            // Step 4: Base64 encode the result
            String signature = Base64.getEncoder().encodeToString(hmacDigest);
            log.info("Generated signature with length: {}", signature.length());
            
            return signature;
        } catch (Exception e) {
            log.error("Error creating signature", e);
            throw new RuntimeException("Error creating signature", e);
        }
    }
}
