package io.strategiz.binanceus.service;

import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import io.strategiz.binanceus.model.TickerPrice;
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

@Slf4j
@Service
public class BinanceUSService {

    private static final String BINANCEUS_API_URL = "https://api.binance.us";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;
    
    @Value("${proxy.host:}")
    private String proxyHost;
    
    @Value("${proxy.port:0}")
    private int proxyPort;

    private final RestTemplate restTemplate;
    
    @Autowired
    @Qualifier("binanceRestTemplate")
    private RestTemplate binanceRestTemplate;

    public BinanceUSService() {
        // Configure timeouts for the RestTemplate
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT);
        
        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfigBuilder.build());
        
        // Check system properties for proxy settings
        String systemProxyHost = System.getProperty("http.proxyHost");
        String systemProxyPort = System.getProperty("http.proxyPort");
        
        if (systemProxyHost != null && !systemProxyHost.isEmpty() && systemProxyPort != null && !systemProxyPort.isEmpty()) {
            log.info("Using system proxy settings: {}:{}", systemProxyHost, systemProxyPort);
            HttpHost proxy = new HttpHost(systemProxyHost, Integer.parseInt(systemProxyPort));
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);
        } else if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            log.info("Using configured proxy settings: {}:{}", proxyHost, proxyPort);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);
        }
        
        HttpClient httpClient = clientBuilder.build();
        
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);
        
        log.info("BinanceUSService initialized with connection timeout: {}ms, read timeout: {}ms", 
                CONNECTION_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * Configure the BinanceUS API credentials
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Configuration status
     */
    public Map<String, String> configure(String apiKey, String secretKey) {
        Map<String, String> response = new HashMap<>();
        if (apiKey == null || apiKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            response.put("status", "error");
            response.put("message", "API Key and Secret Key are required");
            return response;
        }

        response.put("status", "success");
        response.put("message", "Binance US API configured successfully");
        return response;
    }

    /**
     * Make a public request to BinanceUS API with retry logic
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @return API response
     */
    public <T> T publicRequest(HttpMethod method, String endpoint, Map<String, String> params, Class<T> responseType) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                log.debug("Making public request to {} (attempt {})", endpoint, attempts + 1);
                URIBuilder uriBuilder = new URIBuilder(BINANCEUS_API_URL + endpoint);
                
                if (params != null) {
                    params.forEach(uriBuilder::addParameter);
                }
                
                URI uri = uriBuilder.build();
                log.debug("Request URI: {}", uri);
                
                // Add user agent header to avoid potential blocking
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                
                ResponseEntity<T> response = restTemplate.exchange(
                    uri,
                    method,
                    new HttpEntity<>(headers),
                    responseType
                );
                
                log.debug("Received response status: {}", response.getStatusCode());
                return response.getBody();
            } catch (HttpClientErrorException e) {
                log.error("Client error making public request to {}: {} - {}", 
                    endpoint, e.getStatusCode(), e.getResponseBodyAsString());
                // Don't retry client errors (4xx)
                throw e;
            } catch (HttpServerErrorException e) {
                log.error("Server error making public request to {}: {} - {} (attempt {})", 
                    endpoint, e.getStatusCode(), e.getResponseBodyAsString(), attempts + 1);
                lastException = e;
                // Retry server errors (5xx)
            } catch (ResourceAccessException e) {
                log.error("Network error making public request to {}: {} (attempt {})", 
                    endpoint, e.getMessage(), attempts + 1);
                lastException = e;
                // Retry network errors
            } catch (Exception e) {
                log.error("Error making public request to {}: {} (attempt {})", 
                    endpoint, e.getMessage(), attempts + 1);
                lastException = e;
            }
            
            attempts++;
            if (attempts < MAX_RETRY_ATTEMPTS) {
                try {
                    // Exponential backoff: delay increases with each retry attempt
                    long delay = RETRY_DELAY_MS * (long)Math.pow(2, attempts - 1);
                    log.debug("Retrying in {} ms...", delay);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        // If we've exhausted all retries, throw the last exception
        throw new RuntimeException("Error making public request to " + endpoint + " after " + 
            MAX_RETRY_ATTEMPTS + " attempts: " + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    /**
     * Make a signed request to the Binance US API
     *
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @param apiKey Binance US API key
     * @param secretKey Binance US API secret key
     * @param responseType Response type class
     * @param <T> Response type
     * @return Response object
     */
    public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              String apiKey, String secretKey, Class<T> responseType) {
        log.info("Making signed request to Binance US API: {} {}", method, endpoint);
        
        if (params == null) {
            params = new HashMap<>();
        }
        
        // Add timestamp parameter for signed requests
        // Add timestamp for the request
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        // Add a larger recvWindow to prevent timestamp sync issues (default is 5000ms)
        if (!params.containsKey("recvWindow")) {
            params.put("recvWindow", "60000"); // Use 60 seconds window to avoid timestamp sync issues
        }
        
        // Create signature
        String queryString = buildQueryString(params);
        String signature = createSignature(queryString, secretKey);
        
        // Add signature to parameters
        params.put("signature", signature);
        
        // Build the full URL with query parameters
        String fullUrl = BINANCEUS_API_URL + endpoint;
        if (!params.isEmpty()) {
            fullUrl += "?" + buildQueryString(params);
        }
        
        log.debug("Full request URL: {}", fullUrl);
        
        // Create HTTP request with API key header
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        headers.set("User-Agent", "Strategiz/1.0");
        
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        
        // Make the request with retry logic
        int maxRetries = 3;
        int attempt = 0;
        long retryDelay = 1000; // Start with 1 second delay
        
        while (attempt < maxRetries) {
            attempt++;
            try {
                log.debug("Making signed request attempt {} of {}", attempt, maxRetries);
                
                ResponseEntity<T> response = restTemplate.exchange(
                    fullUrl, method, entity, responseType);
                
                log.info("Signed request successful: {} {}", method, endpoint);
                return response.getBody();
                
            } catch (HttpClientErrorException e) {
                log.error("Client error making signed request to {}: {} - {} (attempt {})", 
                    endpoint, e.getStatusCode(), e.getResponseBodyAsString(), attempt);
                
                // Don't retry client errors (4xx) except for 429 (rate limit)
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    if (attempt < maxRetries) {
                        long waitTime = retryDelay * (long)Math.pow(2, attempt - 1);
                        log.warn("Rate limited by Binance US API, retrying in {} ms", waitTime);
                        try {
                            Thread.sleep(waitTime);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    } else {
                        throw e;
                    }
                } else {
                    // For other client errors, don't retry
                    throw e;
                }
            } catch (HttpServerErrorException e) {
                log.error("Server error making signed request to {}: {} - {} (attempt {})", 
                    endpoint, e.getStatusCode(), e.getResponseBodyAsString(), attempt);
                
                // Retry server errors (5xx)
                if (attempt < maxRetries) {
                    long waitTime = retryDelay * (long)Math.pow(2, attempt - 1);
                    log.warn("Server error from Binance US API, retrying in {} ms", waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                log.error("Network error making signed request to {}: {} (attempt {})", 
                    endpoint, e.getMessage(), attempt);
                
                // Retry network errors
                if (attempt < maxRetries) {
                    long waitTime = retryDelay * (long)Math.pow(2, attempt - 1);
                    log.warn("Network error connecting to Binance US API, retrying in {} ms", waitTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            } catch (Exception e) {
                log.error("Unexpected error making signed request to {}: {} (attempt {})", 
                    endpoint, e.getMessage(), attempt);
                throw e;
            }
        }
        
        // This should never be reached due to the throw in the last catch block
        throw new RuntimeException("Failed to make signed request after " + maxRetries + 
            " attempts: Unknown error");
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return queryString.toString();
    }

    private String createSignature(String queryString, String secretKey) {
        try {
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA256);
            hmacSha256.init(secretKeySpec);
            byte[] hash = hmacSha256.doFinal(queryString.getBytes());
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error creating signature: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating signature for Binance US API request", e);
        }
    }

    /**
     * Get exchange information
     * @return Exchange information
     */
    public Object getExchangeInfo() {
        try {
            log.debug("Fetching exchange information from Binance US API");
            return publicRequest(HttpMethod.GET, "/api/v3/exchangeInfo", null, Object.class);
        } catch (Exception e) {
            log.error("Error fetching exchange information: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch exchange information from Binance US API", e);
        }
    }

    /**
     * Get ticker prices for all symbols
     * @return Array of ticker prices
     */
    public List<TickerPrice> getTickerPrices() {
        try {
            log.debug("Fetching ticker prices from Binance US API");
            
            // Use manual retry for this method as well
            int attempts = 0;
            Exception lastException = null;
            
            while (attempts < MAX_RETRY_ATTEMPTS) {
                try {
                    log.debug("Fetching ticker prices (attempt {})", attempts + 1);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    
                    return restTemplate.exchange(
                        BINANCEUS_API_URL + "/api/v3/ticker/price",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<List<TickerPrice>>() {}
                    ).getBody();
                } catch (HttpServerErrorException | ResourceAccessException e) {
                    log.error("Error fetching ticker prices (attempt {}): {}", attempts + 1, e.getMessage());
                    lastException = e;
                    
                    attempts++;
                    if (attempts < MAX_RETRY_ATTEMPTS) {
                        try {
                            long delay = RETRY_DELAY_MS * (long)Math.pow(2, attempts - 1);
                            log.debug("Retrying in {} ms...", delay);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry interrupted", ie);
                        }
                    }
                }
            }
            
            throw new RuntimeException("Failed to fetch ticker prices after " + MAX_RETRY_ATTEMPTS + 
                " attempts: " + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
        } catch (Exception e) {
            log.error("Error fetching ticker prices: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch ticker prices from Binance US API", e);
        }
    }

    /**
     * Get account information from Binance US API
     * 
     * @param apiKey Binance US API key
     * @param secretKey Binance US API secret key
     * @return Account information
     */
    public Account getAccount(String apiKey, String secretKey) {
        log.info("Getting account information from Binance US API");
        try {
            Map<String, String> params = new HashMap<>();
            Account account = signedRequest(HttpMethod.GET, "/api/v3/account", params, apiKey, secretKey, Account.class);
            log.info("Successfully retrieved account with {} balances", 
                account.getBalances() != null ? account.getBalances().size() : 0);
            return account;
        } catch (Exception e) {
            log.error("Error getting account information: {}", e.getMessage(), e);
            // Add more detailed logging for specific error types
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException clientEx = (HttpClientErrorException) e;
                log.error("Client error from Binance US API: {} - {}", clientEx.getStatusCode(), clientEx.getResponseBodyAsString());
            } else if (e instanceof HttpServerErrorException) {
                HttpServerErrorException serverEx = (HttpServerErrorException) e;
                log.error("Server error from Binance US API: {} - {}", serverEx.getStatusCode(), serverEx.getResponseBodyAsString());
            } else if (e instanceof ResourceAccessException) {
                log.error("Network error connecting to Binance US API: {}", e.getMessage());
                log.error("Connection details: baseUrl={}, timeout={}", BINANCEUS_API_URL, CONNECTION_TIMEOUT);
            }
            throw e;
        }
    }

    /**
     * Get account balance with USD values
     * @param apiKey API key
     * @param secretKey Secret key
     * @return List of balances with USD values
     */
    public List<Balance> getAccountBalance(String apiKey, String secretKey) {
        try {
            log.debug("Fetching account balance from Binance US API");
            // Get account information
            Account account = getAccount(apiKey, secretKey);
            
            // Get ticker prices
            List<TickerPrice> tickerPrices = getTickerPrices();
            Map<String, Double> priceMap = new HashMap<>();
            for (TickerPrice ticker : tickerPrices) {
                priceMap.put(ticker.getSymbol(), Double.parseDouble(ticker.getPrice()));
            }
            
            // Filter balances with non-zero amounts and calculate USD values
            return account.getBalances().stream()
                    .filter(b -> Double.parseDouble(b.getFree()) > 0 || Double.parseDouble(b.getLocked()) > 0)
                    .map(b -> {
                        double free = Double.parseDouble(b.getFree());
                        double locked = Double.parseDouble(b.getLocked());
                        double total = free + locked;
                        
                        // Calculate USD value
                        double usdValue = 0.0;
                        String asset = b.getAsset();
                        
                        if ("USD".equals(asset) || "USDT".equals(asset) || "USDC".equals(asset) || "BUSD".equals(asset)) {
                            // Stablecoins have 1:1 value with USD
                            usdValue = total;
                        } else {
                            // For other assets, look for a USD or USDT pair
                            String usdPair = asset + "USD";
                            String usdtPair = asset + "USDT";
                            
                            if (priceMap.containsKey(usdPair)) {
                                usdValue = total * priceMap.get(usdPair);
                            } else if (priceMap.containsKey(usdtPair)) {
                                usdValue = total * priceMap.get(usdtPair);
                            }
                        }
                        
                        b.setUsdValue(usdValue);
                        return b;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error calculating account balance: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate account balance", e);
        }
    }

    /**
     * Calculate total USD value of balances
     * @param balances List of balances
     * @return Total USD value
     */
    public double calculateTotalUsdValue(List<Balance> balances) {
        return balances.stream()
                .mapToDouble(Balance::getUsdValue)
                .sum();
    }

    /**
     * Test the API connection
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Test results
     */
    public Map<String, Object> testConnection(String apiKey, String secretKey) {
        try {
            log.debug("Testing connection to Binance US API");
            
            // First test public API
            try {
                log.debug("Testing public API with ticker/price endpoint");
                List<TickerPrice> prices = getTickerPrices();
                log.info("Successfully connected to Binance US public API, received {} ticker prices", 
                    prices != null ? prices.size() : 0);
            } catch (Exception e) {
                log.error("Failed to connect to Binance US public API: {}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("status", "error");
                result.put("message", "Failed to connect to Binance US public API: " + e.getMessage());
                return result;
            }
            
            // Then test private API
            try {
                // Try to get account information
                Account account = getAccount(apiKey, secretKey);
                
                if (account != null && account.getAccountType() != null) {
                    log.info("Successfully connected to Binance US API with account type: {}", account.getAccountType());
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "ok");
                    result.put("accountType", account.getAccountType());
                    return result;
                } else {
                    log.warn("Connection test failed: received null or invalid account information");
                    Map<String, Object> result = new HashMap<>();
                    result.put("status", "error");
                    result.put("message", "Invalid account information received");
                    return result;
                }
            } catch (Exception e) {
                log.error("Failed to connect to Binance US private API: {}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("status", "error");
                result.put("message", "Failed to connect to Binance US private API: " + e.getMessage());
                return result;
            }
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Test connectivity to Binance US API with detailed debugging
     * 
     * @return Map containing connection details and status
     */
    public Map<String, Object> testConnectivityWithDebug() {
        log.info("Testing connectivity to Binance US API with detailed debugging");
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        result.put("baseUrl", BINANCEUS_API_URL);
        result.put("connectionTimeout", CONNECTION_TIMEOUT);
        result.put("readTimeout", READ_TIMEOUT);
        
        try {
            // Check if we can resolve the Binance US API domain
            String host = new URL(BINANCEUS_API_URL).getHost();
            InetAddress address = InetAddress.getByName(host);
            result.put("dnsResolution", Map.of(
                "host", host,
                "resolved", true,
                "address", address.getHostAddress()
            ));
            
            // Check if proxy is enabled
            result.put("proxyEnabled", proxyEnabled);
            if (proxyEnabled) {
                result.put("proxySettings", Map.of(
                    "host", proxyHost,
                    "port", proxyPort
                ));
            }
            
            // Try to ping the Binance US API
            long startTime = System.currentTimeMillis();
            Object pingResponse = publicRequest(HttpMethod.GET, "/api/v3/ping", null, Object.class);
            long endTime = System.currentTimeMillis();
            
            result.put("pingSuccess", true);
            result.put("pingResponseTime", endTime - startTime);
            result.put("pingResponse", pingResponse);
            
            // Try to get the server time
            startTime = System.currentTimeMillis();
            Object timeResponse = publicRequest(HttpMethod.GET, "/api/v3/time", null, Object.class);
            endTime = System.currentTimeMillis();
            
            result.put("timeSuccess", true);
            result.put("timeResponseTime", endTime - startTime);
            result.put("timeResponse", timeResponse);
            
            result.put("status", "success");
            result.put("message", "Successfully connected to Binance US API");
        } catch (Exception e) {
            log.error("Error testing connectivity to Binance US API: {}", e.getMessage(), e);
            
            result.put("status", "error");
            result.put("message", "Error connecting to Binance US API: " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            result.put("errorStackTrace", java.util.Arrays.stream(e.getStackTrace())
                .limit(10)
                .map(java.lang.StackTraceElement::toString)
                .collect(Collectors.toList()));
            
            if (e instanceof ResourceAccessException) {
                result.put("errorCategory", "network");
                result.put("errorDetail", "Network error: Unable to connect to Binance US API");
            } else if (e instanceof HttpClientErrorException) {
                HttpClientErrorException clientEx = (HttpClientErrorException) e;
                result.put("errorCategory", "client");
                result.put("statusCode", clientEx.getStatusCode().value());
                result.put("responseBody", clientEx.getResponseBodyAsString());
            } else if (e instanceof HttpServerErrorException) {
                HttpServerErrorException serverEx = (HttpServerErrorException) e;
                result.put("errorCategory", "server");
                result.put("statusCode", serverEx.getStatusCode().value());
                result.put("responseBody", serverEx.getResponseBodyAsString());
            }
        }
        
        return result;
    }

    /**
     * Test connectivity to Binance US API with detailed debugging using direct request
     * 
     * @return Map containing connection details and status
     */
    public Map<String, Object> testDirectConnectivity() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // Test ping endpoint
            String pingResponse = directRequest("/api/v3/ping", null);
            result.put("pingSuccess", true);
            result.put("pingResponse", pingResponse);
            
            // Test time endpoint
            String timeResponse = directRequest("/api/v3/time", null);
            result.put("timeSuccess", true);
            result.put("timeResponse", timeResponse);
            
            // Test exchange info endpoint
            String exchangeInfoResponse = directRequest("/api/v3/exchangeInfo", null);
            result.put("exchangeInfoSuccess", true);
            result.put("exchangeInfoResponseLength", exchangeInfoResponse != null ? exchangeInfoResponse.length() : 0);
            result.put("exchangeInfoResponsePreview", 
                exchangeInfoResponse != null && exchangeInfoResponse.length() > 500 ? 
                exchangeInfoResponse.substring(0, 500) + "..." : 
                exchangeInfoResponse);
            
            result.put("status", "success");
            result.put("message", "Successfully connected to Binance US API using direct request");
        } catch (Exception e) {
            log.error("Error testing direct connectivity: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "Error testing direct connectivity: " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }
        
        return result;
    }

    /**
     * Get completely unmodified raw account data from Binance US API
     * This method returns the exact JSON response from the API without any transformations
     * 
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Raw JSON response from Binance US API as a String
     */
    public String getRawAccountData(String apiKey, String secretKey) throws Exception {
        log.info("Getting raw account data from Binance US API");
        
        // Create timestamp for the request
        long timestamp = System.currentTimeMillis();
        String queryString = "timestamp=" + timestamp;
        
        // Create HMAC SHA256 signature
        String signature = createSignature(queryString, secretKey);
        
        // Build the URL with query parameters and signature
        String endpoint = "/api/v3/account";
        String url = BINANCEUS_API_URL + endpoint + "?" + queryString + "&signature=" + signature;
        
        // Set up headers with API key
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        log.info("Sending request to Binance US API: {}", url);
        
        // Make the request with retries
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Successfully retrieved raw account data from Binance US API");
                    // Return the completely unmodified raw response body
                    return response.getBody();
                } else {
                    log.warn("Unexpected status code from Binance US API: {}", response.getStatusCode());
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        log.info("Retrying request (attempt {}/{})", attempt, MAX_RETRY_ATTEMPTS);
                        Thread.sleep(RETRY_DELAY_MS);
                    }
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                log.error("HTTP error from Binance US API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("Retrying request (attempt {}/{})", attempt, MAX_RETRY_ATTEMPTS);
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    throw new Exception("Failed to get raw account data from Binance US API: " + e.getMessage());
                }
            } catch (ResourceAccessException e) {
                log.error("Network error connecting to Binance US API", e);
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.info("Retrying request (attempt {}/{})", attempt, MAX_RETRY_ATTEMPTS);
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    throw new Exception("Network error connecting to Binance US API: " + e.getMessage());
                }
            }
        }
        
        throw new Exception("Failed to get raw account data from Binance US API after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    /**
     * Make a direct request to the Binance US API using the specialized binanceRestTemplate
     * This is an alternative method that might bypass proxy issues
     * 
     * @param endpoint API endpoint
     * @param params Request parameters
     * @return Raw API response as String
     */
    public String directRequest(String endpoint, Map<String, String> params) {
        try {
            StringBuilder urlBuilder = new StringBuilder(BINANCEUS_API_URL + endpoint);
            
            if (params != null && !params.isEmpty()) {
                urlBuilder.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!first) {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            
            String url = urlBuilder.toString();
            log.info("Making direct request to: {}", url);
            
            // Add custom headers to avoid browser detection
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Use the specialized binanceRestTemplate for this request
            ResponseEntity<String> response = binanceRestTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                log.info("Received direct response: {}", responseBody);
                return responseBody;
            } else {
                log.error("Direct request failed with status code: {}", response.getStatusCode());
                throw new RuntimeException("Direct request failed with status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error making direct request to {}: {}", endpoint, e.getMessage(), e);
            throw new RuntimeException("Error making direct request to " + endpoint + ": " + e.getMessage(), e);
        }
    }
}
