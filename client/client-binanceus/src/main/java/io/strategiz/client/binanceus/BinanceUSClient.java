package io.strategiz.client.binanceus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Client for interacting with the Binance US exchange API
 * This class handles all low-level API communication with Binance US
 */
@Component
public class BinanceUSClient {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSClient.class);
    private static final String ACCOUNT_INFO_PATH = "/api/v3/account";
    
    @Value("${binanceus.api.url:https://api.binance.us}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    /**
     * Constructor with dependency injection for RestTemplate
     * 
     * @param binanceRestTemplate RestTemplate configured for Binance US API
     */
    public BinanceUSClient(@Qualifier("binanceRestTemplate") RestTemplate binanceRestTemplate) {
        this.restTemplate = binanceRestTemplate;
        log.info("BinanceUSClient initialized with base URL: {}", baseUrl);
    }
    
    /**
     * Generate API signature for authenticated requests
     * 
     * @param data Data to sign
     * @param secret API secret key
     * @return HMAC SHA256 signature
     */
    protected String generateSignature(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating signature: {}", e.getMessage());
            throw new RuntimeException("Error generating Binance US API signature", e);
        }
    }
    
    /**
     * Get account information
     * 
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Account information as a string
     */
    public String getAccountInfo(String apiKey, String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString, secretKey);
        
        String url = baseUrl + ACCOUNT_INFO_PATH + "?" + queryString + "&signature=" + signature;
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        return response.getBody();
    }
    
    /**
     * Test connectivity to the Binance US API
     * 
     * @return Ping response
     */
    public String ping() {
        String url = baseUrl + "/api/v3/ping";
        return restTemplate.getForObject(url, String.class);
    }
}
