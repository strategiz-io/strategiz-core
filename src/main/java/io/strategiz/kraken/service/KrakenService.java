package io.strategiz.kraken.service;

import io.strategiz.kraken.model.KrakenAccount;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KrakenService {

    private static final String KRAKEN_API_URL = "https://api.kraken.com";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private final RestTemplate restTemplate;

    public KrakenService() {
        this.restTemplate = new RestTemplate();
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
        try {
            long nonce = System.currentTimeMillis();
            String path = "/0/private/Balance";
            
            // Create post data
            String postData = "nonce=" + nonce;
            
            // Create signature
            String signature = createSignature(path, postData, secretKey);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("API-Key", apiKey);
            headers.set("API-Sign", signature);
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Create request entity
            HttpEntity<String> entity = new HttpEntity<>(postData, headers);
            
            // Make request
            URI uri = new URIBuilder(KRAKEN_API_URL + path).build();
            ResponseEntity<KrakenAccount> response = restTemplate.exchange(
                uri,
                HttpMethod.POST,
                entity,
                KrakenAccount.class
            );
            
            // Return completely unmodified raw data
            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting Kraken account information", e);
            throw new RuntimeException("Error getting Kraken account information: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test the API connection
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Test results
     */
    public Map<String, Object> testConnection(String apiKey, String secretKey) {
        Map<String, Object> result = new HashMap<>();
        try {
            KrakenAccount account = getAccount(apiKey, secretKey);
            if (account != null && account.getError() != null && account.getError().length > 0) {
                result.put("status", "error");
                result.put("message", String.join(", ", account.getError()));
                return result;
            }
            result.put("status", "success");
            result.put("message", "Connection successful");
            result.put("data", account);
            return result;
        } catch (Exception e) {
            log.error("Error testing Kraken API connection", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }
    
    /**
     * Create signature for Kraken API
     * @param path API path
     * @param postData Post data
     * @param secretKey Secret key
     * @return Signature
     */
    private String createSignature(String path, String postData, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        // Decode base64 secret key
        byte[] decodedSecretKey = Base64.getDecoder().decode(secretKey);
        
        // Create message
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update((postData).getBytes());
        byte[] messageHash = md.digest();
        
        byte[] pathBytes = path.getBytes();
        byte[] message = new byte[messageHash.length + pathBytes.length];
        System.arraycopy(pathBytes, 0, message, 0, pathBytes.length);
        System.arraycopy(messageHash, 0, message, pathBytes.length, messageHash.length);
        
        // Create HMAC
        Mac mac = Mac.getInstance(HMAC_SHA512);
        SecretKeySpec secretKeySpec = new SecretKeySpec(decodedSecretKey, HMAC_SHA512);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(message);
        
        // Encode as base64
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
