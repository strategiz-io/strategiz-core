package io.strategiz.coinbase.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

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
    private PrivateKey getPrivateKeyFromPem(String pemKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Debug: Print key characteristics to help diagnose format issues
        log.info("Private key length: {} characters", pemKey.length());
        log.info("Key format analysis: " + 
                 "Contains BEGIN EC PRIVATE KEY: " + pemKey.contains("BEGIN EC PRIVATE KEY") + 
                 ", Contains BEGIN PRIVATE KEY: " + pemKey.contains("BEGIN PRIVATE KEY") + 
                 ", Contains dashes: " + pemKey.contains("-") + 
                 ", Contains equals: " + pemKey.contains("="));
        
        // Print first and last few characters to help identify format
        String startChars = pemKey.length() > 10 ? pemKey.substring(0, 10) : pemKey;
        String endChars = pemKey.length() > 10 ? pemKey.substring(pemKey.length() - 10) : pemKey;
        log.info("Key starts with: '{}', ends with: '{}'", startChars, endChars);
        try {
            log.info("Attempting to parse private key with Bouncy Castle support");
            
            // Register Bouncy Castle provider if not already registered
            if (java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                log.info("Added Bouncy Castle security provider");
            }
            
            // Determine the format based on content
            boolean isSec1Format = pemKey.contains("BEGIN EC PRIVATE KEY");
            boolean isPkcs8Format = pemKey.contains("BEGIN PRIVATE KEY");
            
            log.debug("Key format appears to be: {}. Contains BEGIN EC: {}, Contains BEGIN PRIVATE: {}", 
                    isSec1Format ? "SEC1" : (isPkcs8Format ? "PKCS#8" : "unknown"), 
                    pemKey.contains("BEGIN EC"), pemKey.contains("BEGIN PRIVATE"));
            
            // Clean the PEM format
            String cleanedKey = pemKey
                    .replace("-----BEGIN EC PRIVATE KEY-----", "")
                    .replace("-----END EC PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .replace("\n", "")
                    .replaceAll("\\s", "");
            
            // Decode the Base64 content
            byte[] decodedKey;
            try {
                decodedKey = Base64.getDecoder().decode(cleanedKey);
                log.debug("Successfully decoded Base64 key, length: {} bytes", decodedKey.length);
            } catch (IllegalArgumentException e) {
                log.error("Failed to decode Base64 key: {}", e.getMessage());
                throw new InvalidKeySpecException("Invalid Base64 encoding in private key");
            }
            
            // Method 1: Try direct parsing with Java's built-in providers
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodedKey);
                return keyFactory.generatePrivate(privateKeySpec);
            } catch (InvalidKeySpecException e) {
                log.warn("Standard Java PKCS#8 parsing failed: {}", e.getMessage());
                // Continue to other methods
            }
            
            // Method 2: Try with Bouncy Castle directly
            try {
                // First try to parse as PKCS#8
                try {
                    KeyFactory bcKeyFactory = KeyFactory.getInstance("EC", "BC");
                    return bcKeyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
                } catch (InvalidKeySpecException e) {
                    log.warn("Bouncy Castle PKCS#8 parsing failed, trying SEC1: {}", e.getMessage());
                }
                
                // Then try direct SEC1 parsing if available
                try {
                    org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki;
                    
                    // Check if this is already in SEC1 format
                    if (isSec1Format || decodedKey[0] == 0x30) { // ASN.1 SEQUENCE tag
                        log.debug("Key appears to be in SEC1/ASN.1 format, converting to PKCS#8");
                        
                        // Parse the SEC1 format private key
                        org.bouncycastle.asn1.sec.ECPrivateKey ecKey = 
                                org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(decodedKey);
                        
                        // Get the curve parameters - try a few standard curves if not specified
                        org.bouncycastle.jce.spec.ECNamedCurveParameterSpec curveSpec;
                        
                        // If curve is specified in the key, use it
                        if (ecKey.getParametersObject() != null) {
                            org.bouncycastle.asn1.ASN1ObjectIdentifier curveOid = 
                                    org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(
                                            ecKey.getParametersObject());
                            curveSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(
                                    curveOid.getId());
                        } else {
                            // Try standard curves used by Coinbase
                            String[] curves = {"secp256r1", "secp256k1", "prime256v1"};
                            curveSpec = null;
                            
                            for (String curve : curves) {
                                try {
                                    curveSpec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec(curve);
                                    if (curveSpec != null) {
                                        log.debug("Using curve: {}", curve);
                                        break;
                                    }
                                } catch (Exception ex) {
                                    log.debug("Curve {} not supported", curve);
                                }
                            }
                            
                            if (curveSpec == null) {
                                throw new InvalidKeySpecException("Could not determine EC curve parameters");
                            }
                        }
                        
                        // Convert EC private key to JCE format
                        org.bouncycastle.jce.spec.ECPrivateKeySpec ecPrivateKeySpec = 
                                new org.bouncycastle.jce.spec.ECPrivateKeySpec(
                                        ecKey.getKey(), curveSpec);
                        
                        // Generate the private key
                        KeyFactory bcKeyFactory = KeyFactory.getInstance("EC", "BC");
                        return bcKeyFactory.generatePrivate(ecPrivateKeySpec);
                    } else {
                        log.warn("Key is not in expected SEC1 format");
                        throw new InvalidKeySpecException("Key format could not be determined");
                    }
                } catch (Exception ex) {
                    log.error("SEC1 parsing with Bouncy Castle failed: {}", ex.getMessage());
                    throw new InvalidKeySpecException("Could not parse key with Bouncy Castle: " + ex.getMessage());
                }
            } catch (Exception bcEx) {
                log.error("All Bouncy Castle parsing attempts failed: {}", bcEx.getMessage(), bcEx);
                
                // Method 3: Simple fallback for raw EC key
                try {
                    log.debug("Attempting fallback method for raw EC key");
                    org.bouncycastle.jce.spec.ECNamedCurveParameterSpec ecSpec = 
                            org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256r1");
                    
                    // Assume the decoded key contains just the private key value
                    // This is a simplified approach that might work in some cases
                    java.math.BigInteger privateKeyValue = new java.math.BigInteger(1, decodedKey);
                    org.bouncycastle.jce.spec.ECPrivateKeySpec privateKeySpec = 
                            new org.bouncycastle.jce.spec.ECPrivateKeySpec(privateKeyValue, ecSpec);
                    
                    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
                    return keyFactory.generatePrivate(privateKeySpec);
                } catch (Exception fallbackEx) {
                    log.error("Fallback parsing method failed: {}", fallbackEx.getMessage());
                    throw new InvalidKeySpecException("All key parsing methods failed. The provided key format is not supported.");
                }
            }
        } catch (Exception e) {
            log.error("Error parsing private key: {}", e.getMessage(), e);
            throw e;
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
            String token = Jwts.builder()
                    .setHeaderParam("typ", "JWT")
                    .setId(requestId)                       // jti claim
                    .setIssuer(apiKey)                      // iss claim
                    .setSubject(apiKey)                     // sub claim
                    .setAudience("https://api.coinbase.com/") // aud claim
                    .setIssuedAt(Date.from(Instant.ofEpochSecond(now))) // iat claim
                    .setNotBefore(Date.from(Instant.ofEpochSecond(now - 60))) // nbf claim
                    .setExpiration(Date.from(Instant.ofEpochSecond(now + 60))) // exp claim
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
            
            // Create the payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("iat", now);
            payload.put("exp", now + 60); // Token expires in 60 seconds
            payload.put("aud", "https://api.coinbase.com/");
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
            
            // Create a standardized error response
            Map<String, Object> errorResponse = buildErrorResponse(
                500, // Error code
                "Coinbase API Error", // User-friendly message
                "Error getting raw account data from Coinbase Cloud API: " + e.getMessage(), // Developer message
                null, // Additional info will be added below
                e // Include the exception
            );
            
            // Extract more details if possible
            Map<String, Object> additionalInfo = new HashMap<>();
            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException httpError = (HttpStatusCodeException) e;
                additionalInfo.put("statusCode", httpError.getRawStatusCode());
                additionalInfo.put("responseBody", httpError.getResponseBodyAsString());
            }
            
            // Add the additional info to the error response
            if (!additionalInfo.isEmpty()) {
                errorResponse.put("moreInformation", additionalInfo);
            }
            
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
