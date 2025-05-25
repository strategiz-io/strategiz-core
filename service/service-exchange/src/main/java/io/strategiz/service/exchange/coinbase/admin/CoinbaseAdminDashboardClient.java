package io.strategiz.service.exchange.coinbase.admin;

import io.strategiz.client.coinbase.util.CoinbaseJwtUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for interacting with the Coinbase API for admin dashboard purposes
 * This class handles direct API communication with Coinbase for admin operations
 */
@Component
@Slf4j
public class CoinbaseAdminDashboardClient {
    private static final String BASE_URL = "https://api.coinbase.com/api/v3/brokerage/accounts";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get account information from Coinbase API
     * 
     * @param apiKey Coinbase API key
     * @param privateKey Coinbase private key
     * @return Response containing account information
     */
    public ResponseEntity<String> getAccounts(String apiKey, String privateKey) {
        HttpHeaders headers = new HttpHeaders();
        try {
            // 1. Log credential presence (not value)
            log.info("[Coinbase] Credential presence - API Key: {}, Private Key: {}", 
                    apiKey != null && !apiKey.isEmpty(), privateKey != null && !privateKey.isEmpty());

            // 2. Generate JWT and log header/payload/truncated token
            String jwt = CoinbaseJwtUtil.generateJwt(apiKey, privateKey);
            
            // Log JWT header and payload for debugging
            String[] jwtParts = jwt.split("\\.");
            if (jwtParts.length == 3) {
                String headerJson = new String(java.util.Base64.getUrlDecoder().decode(jwtParts[0]), java.nio.charset.StandardCharsets.UTF_8);
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(jwtParts[1]), java.nio.charset.StandardCharsets.UTF_8);
                log.info("[Coinbase] JWT Header: {}", headerJson);
                log.info("[Coinbase] JWT Payload: {}", payloadJson);
                log.info("[Coinbase] JWT (truncated): {}...{}", jwt.substring(0, 16), jwt.substring(jwt.length() - 16));
            } else {
                log.warn("[Coinbase] JWT does not have 3 parts, cannot decode header/payload.");
            }

            headers.set("CB-ACCESS-KEY", apiKey);
            headers.set("CB-ACCESS-SIGN", jwt);
            headers.set("CB-VERSION", "2023-01-01");
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            // 3. Log outgoing HTTP request
            log.info("[Coinbase] Outgoing request: GET {}", BASE_URL);
            log.info("[Coinbase] Request Headers: {}", headers);

            // Important: Always use real API data, never mock responses
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL, HttpMethod.GET, entity, String.class);

            // Log response status and body
            log.info("[Coinbase] Response Status: {}", response.getStatusCode());
            log.info("[Coinbase] Response Body: {}", response.getBody());

            return response;
        } catch (Exception e) {
            log.error("Failed to generate JWT for Coinbase request: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating JWT: " + e.getMessage());
        }
    }
    
    // Add more methods as needed for other endpoints
}
