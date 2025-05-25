package strategiz.service.exchange.binanceus;

import strategiz.data.exchange.binanceus.model.Account;
import strategiz.data.exchange.binanceus.model.Balance;
import strategiz.data.exchange.binanceus.model.TickerPrice;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
@Slf4j
@Service
public class BinanceUSService {

    private static final String BINANCEUS_API_URL = "https://api.binance.us";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public BinanceUSService() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECTION_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        
        HttpClientBuilder builder = HttpClientBuilder.create();
        
        // Configure proxy if needed
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            builder.setRoutePlanner(routePlanner);
            log.info("Configured HTTP proxy for Binance US API: {}:{}", proxyHost, proxyPort);
        }
        
        HttpClient httpClient = builder.build();
        factory.setHttpClient(httpClient);
        
        this.restTemplate = new RestTemplate(factory);
        log.info("Initialized Binance US service with RestTemplate");
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
        } catch (Exception e) {
            log.error("Error getting raw account data from Binance US API: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting raw account data from Binance US API", e);
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
            throw new RuntimeException("Error generating signature", e);
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
        } catch (Exception e) {
            log.error("Error getting account from Binance US API: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting account from Binance US API", e);
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
            throw new RuntimeException("Error getting ticker prices from Binance US API", e);
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
            throw new RuntimeException("Error getting exchange info from Binance US API", e);
        }
    }
    
    /**
     * Get balances from Binance US API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US secret key
     * @return BalanceResponse containing balances and account data
     */
    public strategiz.data.exchange.binanceus.model.response.BalanceResponse getBalances(String apiKey, String secretKey) {
        try {
            Account account = getAccount(apiKey, secretKey);
            strategiz.data.exchange.binanceus.model.response.BalanceResponse response = new strategiz.data.exchange.binanceus.model.response.BalanceResponse();
            
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
        } catch (Exception e) {
            log.error("Error getting balances from Binance US API: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting balances from Binance US API", e);
        }
    }
}
