package io.strategiz.client.binanceus;

import io.strategiz.client.base.http.ExchangeApiClient;
import io.strategiz.framework.exception.ApplicationClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for interacting with the Binance US exchange API.
 * Implements the ExchangeApiClient interface for standardized exchange interactions.
 * Ensures we ONLY work with real API data, never mocks or simulations.
 */
@Component
public class BinanceUSClient extends ExchangeApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(BinanceUSClient.class);
    private static final String ACCOUNT_INFO_PATH = "/api/v3/account";
    private static final String TICKER_PATH = "/api/v3/ticker/price";
    private static final String TEST_ORDER_PATH = "/api/v3/order/test";
    
    @Value("${binanceus.api.url:https://api.binance.us}")
    private String apiUrl;
    
    @Value("${binanceus.api.key:}")
    private String apiKeyValue;
    
    @Value("${binanceus.api.secret:}")
    private String apiSecretValue;
    
    // RestTemplate for API requests
    private RestTemplate restTemplate;
    
    /**
     * Default constructor with Spring property injection
     */
    public BinanceUSClient() {
        // Default initialization, will be updated in init()
        super("https://api.binance.us", "", "");
    }
    
    /**
     * Constructor with dependency injection for RestTemplate
     * 
     * @param binanceRestTemplate RestTemplate configured for Binance US API
     */
    public BinanceUSClient(@Qualifier("binanceRestTemplate") RestTemplate binanceRestTemplate) {
        super("https://api.binance.us", "", "");
        this.restTemplate = binanceRestTemplate;
        log.info("BinanceUSClient initialized with RestTemplate and base URL: {}", baseUrl);
    }
    
    /**
     * Initialize after Spring has set the properties
     */
    @PostConstruct
    public void init() {
        log.info("Initializing BinanceUSClient with base URL: {}", apiUrl);
        // Update fields from parent class
        this.baseUrl = apiUrl;
        this.apiKey = apiKeyValue;
        this.privateKey = apiSecretValue;
        // Create the RestClient
        this.restClient = createRestClient();
    }
    
    /**
     * Gets raw account data from Binance US API.
     * This returns exactly what the API sends, ensuring we work with real data.
     * 
     * @return The raw, unmodified account data from the API
     */
    @Override
    public Object getRawAccountData() {
        log.info("Getting raw account data from Binance US API");
        validateRealExchangeAccount();
        
        Map<String, String> params = new HashMap<>();
        params.put("timestamp", String.valueOf(Instant.now().toEpochMilli()));
        
        String queryString = "timestamp=" + Instant.now().toEpochMilli();
        String signature = generateSignature(queryString, privateKey);
        String url = baseUrl + ACCOUNT_INFO_PATH + "?" + queryString + "&signature=" + signature;
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        if (restTemplate != null) {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody();
        } else {
            return getRawApiResponse(ACCOUNT_INFO_PATH + "?" + queryString + "&signature=" + signature, Map.class);
        }
    }

    /**
     * Gets raw position/balance data from Binance US API.
     * This returns exactly what the API sends, ensuring we work with real data.
     * 
     * @return The raw, unmodified position data from the API
     */
    @Override
    public Object getRawPositionsData() {
        log.info("Getting raw position data from Binance US API");
        validateRealExchangeAccount();
        
        // For Binance US, positions are in the same endpoint as account data
        return getRawAccountData();
    }
    
    /**
     * Gets raw ticker price data from Binance US API.
     * 
     * @param symbol The trading pair symbol (e.g., "BTCUSD")
     * @return The raw ticker data from the API
     */
    public Object getRawTickerData(String symbol) {
        log.info("Getting raw ticker data for symbol: {}", symbol);
        
        String url = baseUrl + TICKER_PATH + "?symbol=" + symbol;
        
        if (restTemplate != null) {
            return restTemplate.getForObject(url, Map.class);
        } else {
            return getRawApiResponse(TICKER_PATH + "?symbol=" + symbol, Map.class);
        }
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
            throw new ApplicationClientException("Error generating Binance US API signature", e);
        }
    }
    
    /**
     * Get account information using provided API credentials
     * 
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Account information as a string
     */
    public String getAccountInfo(String apiKey, String secretKey) {
        if (restTemplate == null) {
            throw new ApplicationClientException("RestTemplate not initialized");
        }
        
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
        if (restTemplate == null) {
            throw new ApplicationClientException("RestTemplate not initialized");
        }
        
        String url = baseUrl + "/api/v3/ping";
        return restTemplate.getForObject(url, String.class);
    }
}
