package io.strategiz.binanceus.controller;

import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dedicated controller for raw Binance US data
 * This controller is specifically designed to provide raw data access for admin purposes
 */
@RestController
@RequestMapping("/raw-binanceus")
public class BinanceUSRawDataController {

    private static final Logger log = LoggerFactory.getLogger(BinanceUSRawDataController.class);
    
    @Autowired
    @Qualifier("binanceRestTemplate")
    private RestTemplate binanceRestTemplate;

    /**
     * Endpoint to get completely unmodified raw data directly from the Binance US API
     * This endpoint requires a userId to fetch the API credentials from Firebase
     * 
     * @param request Request containing user ID and useSampleData flag
     * @return Raw JSON response from Binance US API
     */
    @PostMapping("/data")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001", "https://strategiz.io"}, allowedHeaders = "*")
    public ResponseEntity<Object> getRawData(@RequestBody Map<String, String> request) {
        log.info("Received request for raw Binance US data");
        
        try {
            // Extract user ID from the request
            String userId = request.get("userId");
            
            // Check if sample data is requested as a fallback
            boolean useSampleData = "true".equalsIgnoreCase(request.getOrDefault("useSampleData", "false"));
            
            log.info("Request parameters - userId: {}, useSampleData: {}", userId, useSampleData);
            
            if (useSampleData) {
                log.info("Using sample data as requested");
                // Create a sample account data response that mimics the Binance US API
                Map<String, Object> sampleData = new HashMap<>();
                sampleData.put("makerCommission", 10);
                sampleData.put("takerCommission", 10);
                sampleData.put("buyerCommission", 0);
                sampleData.put("sellerCommission", 0);
                sampleData.put("canTrade", true);
                sampleData.put("canWithdraw", true);
                sampleData.put("canDeposit", true);
                sampleData.put("updateTime", System.currentTimeMillis());
                sampleData.put("accountType", "SPOT");
                
                // Create balances array with Solana (SOL) balance as mentioned in requirements
                List<Map<String, String>> balances = new ArrayList<>();
                
                // Add Solana with the specific balance mentioned in requirements
                Map<String, String> solBalance = new HashMap<>();
                solBalance.put("asset", "SOL");
                solBalance.put("free", "26.26435019");
                solBalance.put("locked", "0.00000000");
                balances.add(solBalance);
                
                // Add some other common cryptocurrencies
                Map<String, String> btcBalance = new HashMap<>();
                btcBalance.put("asset", "BTC");
                btcBalance.put("free", "0.12345678");
                btcBalance.put("locked", "0.00000000");
                balances.add(btcBalance);
                
                Map<String, String> ethBalance = new HashMap<>();
                ethBalance.put("asset", "ETH");
                ethBalance.put("free", "1.98765432");
                ethBalance.put("locked", "0.00000000");
                balances.add(ethBalance);
                
                Map<String, String> usdBalance = new HashMap<>();
                usdBalance.put("asset", "USD");
                usdBalance.put("free", "1250.42");
                usdBalance.put("locked", "0.00");
                balances.add(usdBalance);
                
                sampleData.put("balances", balances);
                
                Map<String, Object> result = new HashMap<>();
                result.put("rawJsonResponse", new ObjectMapper().writeValueAsString(sampleData));
                result.put("parsedResponse", sampleData);
                result.put("isSampleData", true);
                
                log.info("Returning sample account data");
                return ResponseEntity.ok(result);
            }
            
            if (userId == null) {
                log.warn("Missing user ID in request");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "User ID is required"
                ));
            }
            
            log.info("Fetching Binance US API keys from Firebase for user: {}", userId);
            
            // Get Firestore instance
            Firestore firestore = FirestoreClient.getFirestore();
            
            // Get user document from Firestore
            DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
            
            if (!userDoc.exists()) {
                log.warn("User document not found in Firebase for user: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User document not found"
                ));
            }
            
            // Extract Binance US API keys from user document
            Map<String, Object> userData = userDoc.getData();
            
            if (userData == null) {
                log.warn("User data is null for user: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "error",
                    "message", "User data not found"
                ));
            }
            
            // This cast is necessary but can't be checked at runtime due to type erasure
            @SuppressWarnings("unchecked")
            Map<String, String> binanceusConfig = (Map<String, String>) userData.get("binanceusConfig");
            
            if (binanceusConfig == null || binanceusConfig.get("apiKey") == null || binanceusConfig.get("secretKey") == null) {
                log.warn("Binance US API keys not configured for user: {}", userId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "error",
                    "message", "Binance US API keys not configured"
                ));
            }
            
            String apiKey = binanceusConfig.get("apiKey");
            String secretKey = binanceusConfig.get("secretKey");
            
            log.info("Successfully retrieved Binance US API keys from Firebase for user: {}", userId);
            log.info("Fetching raw account data from Binance US API with key: {}", apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
            
            try {
                // Create timestamp for the request
                long timestamp = System.currentTimeMillis();
                
                // Create parameter map with timestamp
                Map<String, String> params = new HashMap<>();
                params.put("timestamp", String.valueOf(timestamp));
                
                // Generate signature
                String queryString = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
                
                Mac hmacSha256 = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
                hmacSha256.init(secretKeySpec);
                String signature = Hex.encodeHexString(hmacSha256.doFinal(queryString.getBytes()));
                
                // Add signature to parameters
                params.put("signature", signature);
                
                // Build URL with parameters
                StringBuilder urlBuilder = new StringBuilder("https://api.binance.us/api/v3/account");
                urlBuilder.append("?");
                urlBuilder.append(queryString);
                urlBuilder.append("&signature=").append(signature);
                
                // Create headers with API key
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-MBX-APIKEY", apiKey);
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                headers.set("Accept", "application/json");
                headers.set("Content-Type", "application/json");
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                // Make direct request using binanceRestTemplate
                log.info("Making direct request to Binance US API: {}", urlBuilder.toString());
                ResponseEntity<String> response = binanceRestTemplate.exchange(
                    urlBuilder.toString(),
                    HttpMethod.GET,
                    entity,
                    String.class
                );
                
                // Log the raw response
                log.info("Raw response from Binance US API: {}", response.getBody());
                
                // Create a response object that includes both the raw JSON string and parsed object
                Map<String, Object> result = new HashMap<>();
                result.put("rawJsonResponse", response.getBody());
                
                // Also include the response as a parsed object for convenience
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    Object parsedResponse = objectMapper.readValue(response.getBody(), Object.class);
                    result.put("parsedResponse", parsedResponse);
                } catch (Exception e) {
                    log.warn("Could not parse JSON response: {}", e.getMessage());
                    result.put("parseError", e.getMessage());
                }
                
                result.put("statusCode", response.getStatusCodeValue());
                result.put("headers", response.getHeaders());
                
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                log.error("Error making direct request to Binance US API: {}", e.getMessage(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Error making direct request to Binance US API: " + e.getMessage());
                errorResponse.put("error", e.getClass().getSimpleName());
                
                if (e instanceof HttpClientErrorException) {
                    HttpClientErrorException clientEx = (HttpClientErrorException) e;
                    log.error("Client error from Binance US API: {} - {}", clientEx.getStatusCode(), clientEx.getResponseBodyAsString());
                    errorResponse.put("detail", "Client error: " + clientEx.getStatusCode() + " - " + clientEx.getResponseBodyAsString());
                    errorResponse.put("rawResponse", clientEx.getResponseBodyAsString());
                    return ResponseEntity.status(clientEx.getStatusCode()).body(errorResponse);
                }
                
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Unexpected error processing request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Unexpected error: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
