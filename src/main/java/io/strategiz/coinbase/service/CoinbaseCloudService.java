package io.strategiz.coinbase.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.springframework.web.client.HttpClientErrorException;
import com.google.gson.JsonSyntaxException;

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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Bouncy Castle PEM parsing
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

// Import JJWT libraries
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

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
    private PrivateKey getPrivateKeyFromPem(String pemKey) throws Exception {
        log.info("[PEM] Raw key length: {} characters", pemKey == null ? 0 : pemKey.length());
        if (pemKey == null || pemKey.trim().isEmpty()) {
            throw new IllegalArgumentException("PEM key is empty or null");
        }
        // Print the first and last 20 chars for diagnostics
        log.info("[PEM] Starts with: '{}', ends with: '{}'", 
            pemKey.substring(0, Math.min(20, pemKey.length())),
            pemKey.substring(Math.max(0, pemKey.length() - 20)));
        
        try (org.bouncycastle.openssl.PEMParser pemParser = new org.bouncycastle.openssl.PEMParser(new java.io.StringReader(pemKey))) {
            Object object = pemParser.readObject();
            org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter converter = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                log.info("[PEM] Detected PEMKeyPair (SEC1 format)");
                return converter.getKeyPair((org.bouncycastle.openssl.PEMKeyPair) object).getPrivate();
            } else if (object instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo) {
                log.info("[PEM] Detected PrivateKeyInfo (PKCS#8 format)");
                return converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
            } else if (object instanceof org.bouncycastle.openssl.PEMEncryptedKeyPair) {
                log.error("[PEM] Encrypted key pairs are not supported. Please provide an unencrypted EC private key.");
                throw new IllegalArgumentException("Encrypted EC keys are not supported");
            } else {
                log.error("[PEM] Unsupported PEM object type: {}", object == null ? "null" : object.getClass().getName());
                throw new IllegalArgumentException("Unsupported PEM object: " + (object == null ? "null" : object.getClass()));
            }
        } catch (Exception e) {
            log.error("[PEM] Failed to parse PEM key: {}", e.getMessage(), e);
            throw new InvalidKeySpecException("Could not parse EC private key from PEM. Check for a mismatched, truncated, or corrupted key. Original error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate JWT token for Coinbase Cloud API authentication
     * Uses JJWT library for proper JWT token generation with ES256 signature
     */
    private String generateJwtToken(String apiKey, String privateKeyPem) throws Exception {
        // Parse the private key using our robust method
        PrivateKey privateKey = getPrivateKeyFromPem(privateKeyPem);
        
        log.info("Generating JWT token for Coinbase API using JJWT library");
        
        // Register Bouncy Castle provider if not already registered
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
        
        // Get the current time
        long now = Instant.now().getEpochSecond();
        String requestId = UUID.randomUUID().toString();
        
        try {
            // Use JJWT library to build and sign the JWT token
            // Important: The audience must be exactly "https://api.coinbase.com" without trailing slash
            // This is a common cause of 401 Unauthorized errors
            String token = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setId(requestId)                       // jti claim
                    .setIssuer(apiKey)                      // iss claim
                    .setSubject(apiKey)                     // sub claim
                    .setAudience("https://api.coinbase.com") // aud claim - NO trailing slash
                    .setIssuedAt(Date.from(Instant.ofEpochSecond(now))) // iat claim
                    .setNotBefore(Date.from(Instant.ofEpochSecond(now - 60))) // nbf claim
                    .setExpiration(Date.from(Instant.ofEpochSecond(now + 300))) // exp claim - 5 minutes
                    .signWith(privateKey, SignatureAlgorithm.ES256) // ES256 signature
                    .compact();
            
            log.debug("Successfully generated JWT token with JJWT, length: {} chars", token.length());
            return token;
            
        } catch (Exception e) {
            log.error("Error generating JWT token with JJWT: {}", e.getMessage(), e);
            
            // Fallback to manual JWT generation if JJWT fails
            log.info("Falling back to manual JWT generation");
            
            // Create the header
            Map<String, String> header = new HashMap<>();
            header.put("alg", "ES256");
            header.put("typ", "JWT");
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(gson.toJson(header).getBytes("UTF-8"));
            
            // Create the payload - fix the audience to match Coinbase requirements
            Map<String, Object> payload = new HashMap<>();
            payload.put("iat", now);
            payload.put("exp", now + 300); // Token expires in 5 minutes (300 seconds)
            payload.put("aud", "https://api.coinbase.com"); // NO trailing slash
            payload.put("sub", apiKey);
            payload.put("iss", apiKey);
            payload.put("nbf", now - 60);
            payload.put("jti", requestId);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(gson.toJson(payload).getBytes("UTF-8"));
            
            // Create the signature
            String content = encodedHeader + "." + encodedPayload;
            
            try {
                // Use Bouncy Castle for signing
                org.bouncycastle.crypto.signers.ECDSASigner signer = new org.bouncycastle.crypto.signers.ECDSASigner();
                
                // Extract the private key parameters
                org.bouncycastle.crypto.params.ECPrivateKeyParameters privateKeyParams = 
                        (org.bouncycastle.crypto.params.ECPrivateKeyParameters) 
                        org.bouncycastle.crypto.util.PrivateKeyFactory.createKey(privateKey.getEncoded());
                
                signer.init(true, privateKeyParams);
                
                // Hash the content
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(content.getBytes("UTF-8"));
                
                // Sign the hash
                java.math.BigInteger[] components = signer.generateSignature(hash);
                
                // Convert to the proper format (R and S values, fixed length)
                byte[] rBytes = bigIntegerToBytes(components[0], 32);
                byte[] sBytes = bigIntegerToBytes(components[1], 32);
                
                // Concatenate R and S values
                byte[] rawSignature = new byte[rBytes.length + sBytes.length];
                System.arraycopy(rBytes, 0, rawSignature, 0, rBytes.length);
                System.arraycopy(sBytes, 0, rawSignature, rBytes.length, sBytes.length);
                
                String encodedSignature = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(rawSignature);
                
                log.debug("Generated JWT signature using fallback method, length: {} chars", encodedSignature.length());
                
                return content + "." + encodedSignature;
            } catch (Exception e2) {
                log.error("Both JWT generation methods failed: {}", e2.getMessage(), e2);
                throw new RuntimeException("Failed to generate JWT token for Coinbase API: " + e2.getMessage(), e2);
            }
        }
    }
    
    /**
     * Convert a BigInteger to a byte array of the specified length
     * This ensures the signature components (r,s) are the correct length
     * 
     * @param value The BigInteger to convert
     * @param length The desired length of the resulting byte array
     * @return A byte array of the specified length
     */
    private byte[] bigIntegerToBytes(java.math.BigInteger value, int length) {
        byte[] bytes = value.toByteArray();
        
        if (bytes.length == length) {
            return bytes;
        }
        
        // Handle the case where the array is too long (due to sign bit)
        if (bytes.length > length) {
            // If we have an extra byte due to the sign bit that we don't need,
            // we can safely remove it
            if (bytes.length == length + 1 && bytes[0] == 0) {
                byte[] tmp = new byte[length];
                System.arraycopy(bytes, 1, tmp, 0, length);
                return tmp;
            } else {
                // This shouldn't happen with properly generated signature components
                throw new IllegalArgumentException("BigInteger is too large for the specified length");
            }
        }
        
        // Handle the case where the array is too short
        byte[] tmp = new byte[length];
        // Pad with zeros at the beginning
        System.arraycopy(bytes, 0, tmp, length - bytes.length, bytes.length);
        return tmp;
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
        
        // Add CB-VERSION header which is required by Coinbase API v3
        headers.set("CB-VERSION", "2023-05-12");
        
        HttpEntity<?> requestEntity;
        if (body != null) {
            requestEntity = new HttpEntity<>(gson.toJson(body), headers);
        } else {
            requestEntity = new HttpEntity<>(headers);
        }
        
        try {
            log.info("Making Coinbase API request to: {} {}", method, COINBASE_API_URL + endpoint);
            
            ResponseEntity<String> response = restTemplate.exchange(
                COINBASE_API_URL + endpoint,
                method,
                requestEntity,
                String.class
            );
            
            log.info("Coinbase API request successful with status: {}", response.getStatusCode());
            return gson.fromJson(response.getBody(), responseType);
            
        } catch (HttpClientErrorException e) {
            // Log the detailed error information
            log.error("Coinbase API request failed with status: {} and response: {}", 
                      e.getStatusCode(), e.getResponseBodyAsString());
            
            // Try to parse the error response for more details
            try {
                JsonObject errorJson = gson.fromJson(e.getResponseBodyAsString(), JsonObject.class);
                String errorMessage = "Unknown error";
                
                if (errorJson.has("error")) {
                    JsonObject error = errorJson.getAsJsonObject("error");
                    if (error.has("message")) {
                        errorMessage = error.get("message").getAsString();
                    }
                } else if (errorJson.has("message")) {
                    errorMessage = errorJson.get("message").getAsString();
                }
                
                log.error("Coinbase API error message: {}", errorMessage);
                throw new RuntimeException("API request failed with status: " + e.getStatusCode() + 
                                         " - " + errorMessage, e);
            } catch (JsonSyntaxException jsonEx) {
                // If we can't parse the JSON, just use the raw response
                throw new RuntimeException("API request failed with status: " + e.getStatusCode() + 
                                         " - " + e.getResponseBodyAsString(), e);
            }
        } catch (Exception e) {
            log.error("Error making Coinbase API request: {}", e.getMessage(), e);
            throw new RuntimeException("Error making Coinbase API request: " + e.getMessage(), e);
        }
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
            String endpoint = "/brokerage/accounts";
            Map<String, Object> result = authenticatedRequest(HttpMethod.GET, endpoint, null, apiKey, privateKeyPem, Map.class);
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            log.error("Error testing connection to Coinbase Cloud API: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate a JWT token for diagnostic purposes
     * This method exposes the normally private JWT token generation for testing
     * 
     * @param apiKey API key for Coinbase
     * @param privateKeyPem EC Private Key in PEM format
     * @return The generated JWT token
     * @throws Exception If token generation fails
     */
    public String generateJwtTokenForDiagnostic(String apiKey, String privateKeyPem) throws Exception {
        return generateJwtToken(apiKey, privateKeyPem);
    }
    
    /**
     * Test API authentication with a previously generated JWT token
     * Makes a simple request to the Coinbase API to check if the token works
     * 
     * @param apiKey API key for Coinbase
     * @param jwtToken Pre-generated JWT token to test
     * @return API response containing basic account information
     * @throws Exception If the API call fails
     */
    public Map<String, Object> testApiAuthentication(String apiKey, String jwtToken) throws Exception {
        // Endpoint for a simple API call
        String endpoint = "/brokerage/accounts";
        String url = COINBASE_API_URL + endpoint;
        
        log.info("Testing API authentication with endpoint: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.set("CB-ACCESS-KEY", apiKey);
        
        // Add CB-VERSION header which is required by Coinbase API v3
        headers.set("CB-VERSION", "2023-05-12");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // Make the request
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);
            
            String responseBody = response.getBody();
            log.info("Received API response with status: {}", response.getStatusCode());
            
            // Parse the response body to return as a Map
            Map<String, Object> result = new HashMap<>();
            result.put("status", response.getStatusCodeValue());
            result.put("success", true);
            
            // Parse the JSON response if present
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    result.put("data", gson.fromJson(responseBody, Map.class));
                } catch (Exception e) {
                    // If we can't parse as JSON, just include the raw response
                    result.put("rawResponse", responseBody);
                }
            }
            
            return result;
        } catch (RestClientException e) {
            log.error("Error testing API authentication: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("errorMessage", e.getMessage());
            
            // Extract more details if possible
            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException httpError = (HttpStatusCodeException) e;
                errorResult.put("statusCode", httpError.getRawStatusCode());
                errorResult.put("responseBody", httpError.getResponseBodyAsString());
            }
            
            throw new Exception("API authentication test failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get raw account data from Coinbase Cloud API
     * @param apiKey API key
     * @param privateKeyPem EC Private Key in PEM format
     * @return Raw account data
     */
    public Object getRawAccountData(String apiKey, String privateKeyPem) {
        log.info("Fetching raw account data from Coinbase Cloud API using JWT auth");
        try {
            // Generate a JWT token - this helps diagnose JWT generation issues
            String jwtToken = generateJwtToken(apiKey, privateKeyPem);
            log.info("Successfully generated JWT token for API auth, length: {} characters", jwtToken.length());
            
            // Manually construct the API call for detailed logging and error handling
            String endpoint = "/brokerage/accounts";
            String url = COINBASE_API_URL + endpoint;
            
            // Set up headers with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("CB-ACCESS-KEY", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.info("Making authenticated request to Coinbase Cloud API: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class);
            
            log.info("Received response from Coinbase Cloud API with status: {}", response.getStatusCode());
            String responseBody = response.getBody();
            
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    // Parse as JSON to return a proper Object
                    return gson.fromJson(responseBody, Object.class);
                } catch (Exception e) {
                    log.warn("Error parsing API response as JSON, returning raw response: {}", e.getMessage());
                    // If parsing fails, return the raw response string
                    return responseBody;
                }
            } else {
                log.warn("Received empty response body from Coinbase Cloud API");
                Map<String, Object> emptyResponse = new HashMap<>();
                emptyResponse.put("message", "No data returned from Coinbase API");
                emptyResponse.put("statusCode", response.getStatusCodeValue());
                return emptyResponse;
            }
        } catch (Exception e) {
            log.error("Error getting raw account data from Coinbase Cloud API: {}", e.getMessage(), e);
            
            // Create a standardized error response with appropriate status code
            int statusCode = 500;
            String userMessage = "Coinbase API Error";
            String developerMessage = "Error getting raw account data from Coinbase Cloud API: " + e.getMessage();
            Map<String, Object> additionalInfo = new HashMap<>();
            
            // Extract more specific error information for authentication issues
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException httpError = (HttpClientErrorException) e;
                statusCode = httpError.getRawStatusCode();
                
                if (statusCode == 401) {
                    userMessage = "Authentication Failed";
                    developerMessage = "Coinbase API authentication failed. Please check your API credentials.";
                    additionalInfo.put("suggestion", "Verify that your Coinbase API credentials are correct and have the necessary permissions.");
                }
                
                additionalInfo.put("responseBody", httpError.getResponseBodyAsString());
                additionalInfo.put("statusCode", statusCode);
            }
            
            // Create the error response with the additionalInfo we've already populated
            Map<String, Object> errorResponse = buildErrorResponse(
                statusCode,
                userMessage,
                developerMessage,
                additionalInfo,
                e // Include the exception
            );
            
            // No need to create another additionalInfo map or extract more details
            // as we've already done that above in the HttpClientErrorException handling
            
            // Return error information instead of throwing exception
            // This allows the controller to see the actual error details
            return errorResponse;
        }
    }
    
    /**
     * Helper method to build standardized error responses
     * 
     * @param code Error code (HTTP status code-like)
     * @param message User-friendly error message
     * @param developerMessage Technical message for developers
     * @param moreInformation Additional details about the error
     * @param exception The exception that occurred, if any
     * @return A standardized error response map
     */
    private Map<String, Object> buildErrorResponse(int code, String message, String developerMessage, 
                                              Map<String, Object> moreInformation, Exception exception) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        // Standard error fields
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("developerMessage", developerMessage);
        errorResponse.put("timestamp", new Date().toString());
        
        // Add stack trace if exception is provided
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            errorResponse.put("stackTrace", sw.toString());
            
            // Add exception details to developerMessage
            String fullDevMessage = developerMessage + "\nException: " + exception.getClass().getName() + ": " + exception.getMessage();
            errorResponse.put("developerMessage", fullDevMessage);
        }
        
        // Add additional information if provided
        if (moreInformation != null) {
            errorResponse.put("moreInformation", moreInformation);
        }
        
        return errorResponse;
    }
}
