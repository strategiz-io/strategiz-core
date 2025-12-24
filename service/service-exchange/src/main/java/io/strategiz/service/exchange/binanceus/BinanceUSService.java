package io.strategiz.service.exchange.binanceus;

import io.strategiz.data.exchange.binanceus.model.Account;
import io.strategiz.data.exchange.binanceus.model.Balance;
import io.strategiz.data.exchange.binanceus.model.TickerPrice;
import io.strategiz.data.exchange.binanceus.model.response.BalanceResponse;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.service.exchange.exception.ExchangeErrorDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;

/**
 * Service for interacting with the Binance US API
 * This service handles all communication with the Binance US API and provides
 * methods for accessing account data, balances, and other information.
 */
@Service
public class BinanceUSService extends BaseService {

    private static final String BINANCEUS_API_URL = "https://api.binance.us";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public BinanceUSService(RestTemplateBuilder restTemplateBuilder) {
        // Use Spring's RestTemplateBuilder to configure the RestTemplate
        // This leverages Spring's built-in client capabilities instead of Apache HttpClient
        // Note: These methods are deprecated but still needed for the current Spring Boot version
        // They will be replaced in a future version with non-deprecated alternatives
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(READ_TIMEOUT))
                .setReadTimeout(Duration.ofMillis(READ_TIMEOUT))
                .build();
        
        // Configure proxy settings if needed
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            log.info("Proxy settings detected for Binance US API: {}:{}", proxyHost, proxyPort);
            log.info("Using system proxy settings through Spring's client implementation");
        }
        
        log.info("Initialized Binance US service with Spring RestTemplate");
    }
    
    @Override
    protected String getModuleName() {
        return "service-exchange";
    }
    
    /**
     * Validate that a real API connection is available before making requests.
     * Strategiz ONLY uses real API data - never mock data or simulated responses.
     * 
     * @return true if the connection is available, false otherwise
     */
    protected boolean validateRealApiConnection() {
        log.info("Validating real API connection for Binance US");
        try {
            // Make a simple ping request to validate connectivity
            String url = BINANCEUS_API_URL + "/api/v3/ping";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception ex) {
            log.error("Failed to validate real API connection to Binance US: {}", ex.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the Binance US API is available without authentication
     * Used for health monitoring
     * 
     * @return true if the API is available, false otherwise
     */
    public boolean isApiAvailable() {
        log.debug("Checking Binance US API availability");
        try {
            // We can reuse the validateRealApiConnection method which already does a ping check
            boolean available = validateRealApiConnection();
            log.debug("Binance US API available: {}", available);
            return available;
        } catch (Exception e) {
            log.debug("Binance US API unavailable: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Ensures we're working with real API data, not mocks or simulations.
     * This is a core principle of the Strategiz platform.
     * 
     * @throws IllegalStateException if mock data would be returned
     */
    protected void ensureRealApiData() {
        log.info("Ensuring real API data from Binance US");
        if (!validateRealApiConnection()) {
            throw new IllegalStateException("Cannot establish connection to Binance US API. Only real API data is allowed.");
        }
    }
    
    /**
     * Get raw account data from Binance US API
     * This returns the completely unmodified object from the API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US secret key
     * @return Raw account data as received from the API
     */
    public Object getRawAccountData(String apiKey, String secretKey) {
        log.info("Getting raw account data from Binance US API");
        
        // Ensure we're working with real API data - Synapse architecture principle
        ensureRealApiData();
        
        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp;
            String signature = generateSignature(queryString, secretKey);
            
            String url = BINANCEUS_API_URL + "/api/v3/account?" + queryString + "&signature=" + signature;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            return response.getBody();
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting raw account data from Binance US API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.BINANCE_ACCOUNT_FETCH_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Test connection to Binance US API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US secret key
     * @return Connection status
     */
    public Map<String, Object> testConnection(String apiKey, String secretKey) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp;
            String signature = generateSignature(queryString, secretKey);
            
            String url = BINANCEUS_API_URL + "/api/v3/account?" + queryString + "&signature=" + signature;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                result.put("status", "ok");
                result.put("message", "Connection successful");
                result.put("timestamp", System.currentTimeMillis());
            } else {
                result.put("status", "error");
                result.put("message", "Unexpected response: " + response.getStatusCode());
                result.put("timestamp", System.currentTimeMillis());
            }
            
            return result;
        } catch (HttpClientErrorException e) {
            log.error("Client error testing connection to Binance US API: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Client error: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (HttpServerErrorException e) {
            log.error("Server error testing connection to Binance US API: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Server error: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (ResourceAccessException e) {
            log.error("Resource access error testing connection to Binance US API: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Connection error: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("Error testing connection to Binance US API: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return result;
        }
    }
    
    /**
     * Generate HMAC SHA256 signature for Binance US API requests
     * 
     * @param data Data to sign
     * @param key Secret key
     * @return Signature
     */
    private String generateSignature(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), HMAC_SHA256);
            sha256_HMAC.init(secret_key);
            return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new StrategizException(ExchangeErrorDetails.BINANCE_SIGNATURE_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Get account information from Binance US API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US secret key
     * @return Account information
     */
    public Account getAccount(String apiKey, String secretKey) {
        try {
            long timestamp = System.currentTimeMillis();
            String queryString = "timestamp=" + timestamp;
            String signature = generateSignature(queryString, secretKey);
            
            String url = BINANCEUS_API_URL + "/api/v3/account?" + queryString + "&signature=" + signature;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Account> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Account.class
            );

            return response.getBody();
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting account from Binance US API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.BINANCE_ACCOUNT_FETCH_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Get ticker prices for all symbols from Binance US API
     * 
     * @return List of ticker prices
     */
    public List<TickerPrice> getTickerPrices() {
        try {
            String url = BINANCEUS_API_URL + "/api/v3/ticker/price";
            
            ResponseEntity<List<TickerPrice>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<TickerPrice>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting ticker prices from Binance US API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.BINANCE_TICKER_FETCH_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Get exchange information from Binance US API
     * 
     * @return Exchange information
     */
    public Object getExchangeInfo() {
        try {
            String url = BINANCEUS_API_URL + "/api/v3/exchangeInfo";
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                Object.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error getting exchange info from Binance US API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.BINANCE_EXCHANGE_INFO_FAILED, "service-exchange", e);
        }
    }
    
    /**
     * Get balances from Binance US API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US secret key
     * @return BalanceResponse containing balances and account data
     */
    public BalanceResponse getBalances(String apiKey, String secretKey) {
        try {
            Account account = getAccount(apiKey, secretKey);
            BalanceResponse response = new BalanceResponse();
            
            if (account != null && account.getBalances() != null) {
                List<Balance> nonZeroBalances = account.getBalances().stream()
                    .filter(balance -> {
                        double free = Double.parseDouble(balance.getFree());
                        double locked = Double.parseDouble(balance.getLocked());
                        return free > 0 || locked > 0;
                    })
                    .collect(Collectors.toList());
                
                response.setPositions(nonZeroBalances);
                response.setRawAccountData(account);
                
                // Calculate total USD value (simplified implementation)
                double totalUsdValue = 0.0;
                // In a real implementation, you would fetch prices and calculate actual USD values
                response.setTotalUsdValue(totalUsdValue);
            } else {
                response.setPositions(List.of());
                response.setTotalUsdValue(0.0);
            }

            return response;
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting balances from Binance US API: {}", e.getMessage(), e);
            throw new StrategizException(ExchangeErrorDetails.BINANCE_BALANCE_FETCH_FAILED, "service-exchange", e);
        }
    }
}
