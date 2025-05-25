package io.strategiz.service.exchange.coinbase;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for Coinbase Cloud API operations
 * This service handles JWT token generation and authentication with the Coinbase API
 */
@Service
@Slf4j
public class CoinbaseCloudService {

    private static final String COINBASE_API_URL = "https://api.coinbase.com";
    private static final long TOKEN_EXPIRATION_TIME = 60 * 1000; // 60 seconds
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Generate JWT token for Coinbase API authentication
     * 
     * @param apiKey Coinbase API key
     * @param privateKeyPem Coinbase private key in PEM format
     * @return JWT token
     */
    public String generateJwtToken(String apiKey, String privateKeyPem) {
        try {
            log.info("Generating JWT token for Coinbase API authentication");
            
            // Parse the private key from PEM format
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            
            // Current time
            long now = System.currentTimeMillis();
            
            // Generate JWT token
            String jwtToken = Jwts.builder()
                .setSubject(apiKey)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TOKEN_EXPIRATION_TIME))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
            
            log.info("Successfully generated JWT token for Coinbase API authentication");
            return jwtToken;
        } catch (Exception e) {
            log.error("Error generating JWT token for Coinbase API authentication: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating JWT token for Coinbase API authentication", e);
        }
    }
    
    /**
     * Generate JWT token for diagnostic purposes
     * 
     * @param apiKey Coinbase API key
     * @param privateKeyPem Coinbase private key in PEM format
     * @return JWT token
     */
    public String generateJwtTokenForDiagnostic(String apiKey, String privateKeyPem) {
        try {
            log.info("Generating JWT token for diagnostic purposes");
            
            // Parse the private key from PEM format
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            
            // Current time
            long now = System.currentTimeMillis();
            
            // Generate JWT token
            String jwtToken = Jwts.builder()
                .setSubject(apiKey)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TOKEN_EXPIRATION_TIME))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
            
            log.info("Successfully generated JWT token for diagnostic purposes");
            return jwtToken;
        } catch (Exception e) {
            log.error("Error generating JWT token for diagnostic purposes: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating JWT token for diagnostic purposes", e);
        }
    }
    
    /**
     * Test API authentication with a generated JWT token
     * 
     * @param apiKey Coinbase API key
     * @param jwtToken JWT token
     * @return API response
     */
    public Map<String, Object> testApiAuthentication(String apiKey, String jwtToken) {
        try {
            log.info("Testing API authentication with JWT token");
            
            // Set up headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("CB-VERSION", "2023-04-01");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make a simple API call to test the token
            String url = COINBASE_API_URL + "/v2/user";
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            log.info("Successfully tested API authentication with JWT token");
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("statusCode", response.getStatusCodeValue());
            result.put("response", response.getBody());
            
            return result;
        } catch (Exception e) {
            log.error("Error testing API authentication with JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Error testing API authentication with JWT token", e);
        }
    }
    
    /**
     * Parse private key from PEM format
     * 
     * @param privateKeyPem Private key in PEM format
     * @return PrivateKey object
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        // Remove PEM headers and newlines
        String privateKeyContent = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        
        // Decode the Base64 encoded key
        byte[] encodedKey = Base64.getDecoder().decode(privateKeyContent);
        
        // Create a PKCS8 key spec and generate the private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * Configure the Coinbase API client with the provided credentials
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return Configuration status
     */
    public Map<String, Object> configure(String apiKey, String privateKey) {
        try {
            log.info("Configuring Coinbase API client with provided credentials");
            
            // Validate credentials by generating a JWT token
            String jwtToken = generateJwtToken(apiKey, privateKey);
            
            // Test the token with a simple API call
            Map<String, Object> testResult = testApiAuthentication(apiKey, jwtToken);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Coinbase API client configured successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            return result;
        } catch (Exception e) {
            log.error("Error configuring Coinbase API client: {}", e.getMessage(), e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "Error configuring Coinbase API client: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return result;
        }
    }
    
    /**
     * Get account balances from Coinbase API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return Map containing account data and status
     */
    public Map<String, Object> getAccountBalances(String apiKey, String privateKey) {
        try {
            log.info("Getting account balances from Coinbase API");
            
            // Generate JWT token for authentication
            String jwtToken = generateJwtToken(apiKey, privateKey);
            
            // Set up headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("CB-VERSION", "2023-04-01");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make API call to get accounts
            String url = COINBASE_API_URL + "/v2/accounts";
            
            // Use a parameterized type for the response
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Successfully retrieved account balances from Coinbase API");
            
            // Create a response map
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("statusCode", response.getStatusCodeValue());
            result.put("data", response.getBody());
            
            return result;
        } catch (Exception e) {
            log.error("Error getting account balances from Coinbase API: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting account balances from Coinbase API: " + e.getMessage(), e);
        }
    }
}
