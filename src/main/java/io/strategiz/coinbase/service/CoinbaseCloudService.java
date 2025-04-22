package io.strategiz.coinbase.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.model.Balance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for interacting with the Coinbase Cloud API
 * This service handles the EC private key format used in Coinbase Cloud
 */
@Slf4j
@Service
public class CoinbaseCloudService {

    private static final String COINBASE_API_URL = "https://api.coinbase.com/api/v3";
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    public CoinbaseCloudService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Configure the Coinbase API credentials
     * @param apiKey API key (organizations/.../apiKeys/...)
     * @param privateKeyPem EC private key in PEM format
     * @return Configuration status
     */
    public Map<String, String> configure(String apiKey, String privateKeyPem) {
        Map<String, String> response = new HashMap<>();
        if (apiKey == null || apiKey.isEmpty() || privateKeyPem == null || privateKeyPem.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API Key and Private Key are required");
            return response;
        }

        try {
            // Verify we can load the private key
            getPrivateKeyFromPem(privateKeyPem);
            response.put("status", "success");
            response.put("message", "Coinbase Cloud API configured successfully");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Invalid private key format: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Extract private key from PEM format string
     * Handles both PKCS#8 and SEC1 formats for EC keys
     */
    private PrivateKey getPrivateKeyFromPem(String pemKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            log.info("Attempting to parse private key");
            // Clean the PEM format
            pemKey = pemKey.replace("-----BEGIN EC PRIVATE KEY-----", "")
                    .replace("-----END EC PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .replace("\n", "")
                    .replaceAll("\\s", "");
            
            // Try to decode the Base64
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(pemKey);
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode Base64 key: {}", e.getMessage());
                throw new InvalidKeySpecException("Invalid Base64 encoding in private key");
            }
            
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            
            // First try using PKCS#8 format
            try {
                EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
                return keyFactory.generatePrivate(privateKeySpec);
            } catch (InvalidKeySpecException e) {
                log.warn("Failed to parse key as PKCS#8: {}", e.getMessage());
                // If the standard parsing didn't work, we might need to convert from SEC1 to PKCS#8
                // This could be added later if needed
                throw new InvalidKeySpecException("Private key appears to be in SEC1 format which requires conversion. Please provide the key in PKCS#8 format.");
            }
        } catch (Exception e) {
            log.error("Error parsing private key: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Generate JWT token for Coinbase Cloud API authentication
     */
    private String generateJwtToken(String apiKey, String privateKeyPem) throws Exception {
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        
        // Header
        Map<String, String> header = new HashMap<>();
        header.put("alg", "ES256");
        header.put("typ", "JWT");
        
        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(gson.toJson(header).getBytes());
        
        // Payload
        long now = Instant.now().getEpochSecond();
        String requestId = UUID.randomUUID().toString();
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("iat", now);
        payload.put("exp", now + 60); // Token expires in 60 seconds
        payload.put("aud", "https://api.coinbase.com/");
        payload.put("sub", apiKey);
        payload.put("iss", apiKey);
        payload.put("nbf", now - 60);
        payload.put("jti", requestId);
        
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(gson.toJson(payload).getBytes());
        
        // Signature
        String content = encodedHeader + "." + encodedPayload;
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(content.getBytes());
        byte[] signatureBytes = signature.sign();
        
        String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);
        
        // Complete JWT
        return content + "." + encodedSignature;
    }
    
    /**
     * Make an authenticated request to the Coinbase Cloud API
     */
    private <T> T authenticatedRequest(HttpMethod method, String endpoint, Object body,
                               String apiKey, String privateKeyPem, Class<T> responseType) throws Exception {
        String jwt = generateJwtToken(apiKey, privateKeyPem);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwt);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<?> requestEntity;
        if (body != null) {
            requestEntity = new HttpEntity<>(gson.toJson(body), headers);
        } else {
            requestEntity = new HttpEntity<>(headers);
        }
        
        ResponseEntity<String> response = restTemplate.exchange(
            COINBASE_API_URL + endpoint,
            method,
            requestEntity,
            String.class
        );
        
        return gson.fromJson(response.getBody(), responseType);
    }
    
    /**
     * Get user accounts from Coinbase Cloud API
     * @param apiKey API key
     * @param privateKeyPem EC Private Key in PEM format
     * @return List of accounts
     */
    public List<Account> getAccounts(String apiKey, String privateKeyPem) {
        try {
            JsonObject response = authenticatedRequest(
                HttpMethod.GET, 
                "/brokerage/accounts",
                null,
                apiKey,
                privateKeyPem,
                JsonObject.class
            );
            
            List<Account> accounts = new ArrayList<>();
            JsonArray accountsArray = response.getAsJsonArray("accounts");
            
            for (int i = 0; i < accountsArray.size(); i++) {
                JsonObject accountJson = accountsArray.get(i).getAsJsonObject();
                
                Account account = new Account();
                account.setId(accountJson.get("uuid").getAsString());
                account.setName(accountJson.get("name").getAsString());
                account.setCurrency(accountJson.get("currency").getAsString());
                
                // Extract balance
                JsonObject balanceJson = accountJson.getAsJsonObject("available_balance");
                Balance balance = new Balance();
                balance.setCurrency(balanceJson.get("currency").getAsString());
                balance.setAmount(balanceJson.get("value").getAsString());
                account.setBalance(balance);
                
                accounts.add(account);
            }
            
            return accounts;
        } catch (Exception e) {
            log.error("Error getting accounts: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting accounts from Coinbase Cloud API", e);
        }
    }
    
    /**
     * Get account balances with USD values
     * @param apiKey API key
     * @param privateKeyPem EC Private Key in PEM format
     * @return List of accounts with balances
     */
    public List<Account> getAccountBalances(String apiKey, String privateKeyPem) {
        try {
            // Get all accounts
            List<Account> accounts = getAccounts(apiKey, privateKeyPem);
            
            // Filter accounts with non-zero balances
            List<Account> nonZeroAccounts = new ArrayList<>();
            for (Account account : accounts) {
                try {
                    if (account.getBalance() == null) continue;
                    double amount = Double.parseDouble(account.getBalance().getAmount());
                    if (amount <= 0) continue;
                    
                    // Get current price for USD value calculation
                    account.setUsdValue(amount); // Simplified, should fetch price
                    nonZeroAccounts.add(account);
                } catch (Exception e) {
                    log.warn("Error processing account {}: {}", account.getId(), e.getMessage());
                }
            }
            
            return nonZeroAccounts;
        } catch (Exception e) {
            log.error("Error getting account balances: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting account balances from Coinbase Cloud API", e);
        }
    }
    
    /**
     * Test connection to Coinbase Cloud API
     * @param apiKey API key
     * @param privateKeyPem EC Private Key in PEM format
     * @return True if connection is successful
     */
    public boolean testConnection(String apiKey, String privateKeyPem) {
        try {
            authenticatedRequest(
                HttpMethod.GET,
                "/brokerage/accounts",
                null,
                apiKey,
                privateKeyPem,
                JsonObject.class
            );
            return true;
        } catch (Exception e) {
            log.error("Error testing connection to Coinbase Cloud API: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get raw account data from Coinbase Cloud API
     * @param apiKey API key
     * @param privateKeyPem EC Private Key in PEM format
     * @return Raw account data
     */
    public Object getRawAccountData(String apiKey, String privateKeyPem) {
        try {
            return authenticatedRequest(
                HttpMethod.GET,
                "/brokerage/accounts",
                null,
                apiKey,
                privateKeyPem,
                Object.class
            );
        } catch (Exception e) {
            log.error("Error getting raw account data: {}", e.getMessage());
            throw new RuntimeException("Error getting raw account data from Coinbase Cloud API", e);
        }
    }
}
