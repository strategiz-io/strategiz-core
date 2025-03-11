package io.strategiz.binanceus.service;

import io.strategiz.binanceus.model.Account;
import io.strategiz.binanceus.model.Balance;
import io.strategiz.binanceus.model.TickerPrice;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BinanceUSService {

    private static final String BINANCEUS_API_URL = "https://api.binance.us";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate;

    public BinanceUSService() {
        this.restTemplate = new RestTemplate();
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
     * Make a public request to BinanceUS API
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @return API response
     */
    public <T> T publicRequest(HttpMethod method, String endpoint, Map<String, String> params, Class<T> responseType) {
        try {
            URIBuilder uriBuilder = new URIBuilder(BINANCEUS_API_URL + endpoint);
            
            if (params != null) {
                params.forEach(uriBuilder::addParameter);
            }
            
            URI uri = uriBuilder.build();
            
            ResponseEntity<T> response = restTemplate.exchange(
                uri,
                method,
                null,
                responseType
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error making public request to {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("Error making public request", e);
        }
    }

    /**
     * Make a signed request to BinanceUS API
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @param apiKey API key
     * @param secretKey Secret key
     * @return API response
     */
    public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              String apiKey, String secretKey, Class<T> responseType) {
        try {
            // Add timestamp
            if (params == null) {
                params = new HashMap<>();
            }
            
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Create query string for signature
            URIBuilder uriBuilder = new URIBuilder();
            params.forEach(uriBuilder::addParameter);
            String queryString = uriBuilder.build().getQuery();
            
            // Create signature
            String signature = generateSignature(queryString, secretKey);
            
            // Add signature to params
            params.put("signature", signature);
            
            // Build final URI
            URIBuilder finalUriBuilder = new URIBuilder(BINANCEUS_API_URL + endpoint);
            params.forEach(finalUriBuilder::addParameter);
            URI uri = finalUriBuilder.build();
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);
            
            // Make request
            ResponseEntity<T> response = restTemplate.exchange(
                uri,
                method,
                new HttpEntity<>(headers),
                responseType
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error making signed request to {}: {}", endpoint, e.getMessage());
            throw new RuntimeException("Error making signed request", e);
        }
    }

    /**
     * Generate HMAC SHA256 signature
     * @param queryString Query string to sign
     * @param secretKey Secret key
     * @return Signature
     */
    private String generateSignature(String queryString, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA256);
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(queryString.getBytes());
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Get exchange information
     * @return Exchange information
     */
    public Object getExchangeInfo() {
        return publicRequest(HttpMethod.GET, "/api/v3/exchangeInfo", null, Object.class);
    }

    /**
     * Get ticker prices for all symbols
     * @return Array of ticker prices
     */
    public List<TickerPrice> getTickerPrices() {
        return restTemplate.exchange(
            BINANCEUS_API_URL + "/api/v3/ticker/price",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TickerPrice>>() {}
        ).getBody();
    }

    /**
     * Get account information
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Account information
     */
    public Account getAccount(String apiKey, String secretKey) {
        return signedRequest(
            HttpMethod.GET, 
            "/api/v3/account", 
            null, 
            apiKey, 
            secretKey, 
            Account.class
        );
    }

    /**
     * Get account balance with USD values
     * @param apiKey API key
     * @param secretKey Secret key
     * @return List of balances with USD values
     */
    public List<Balance> getAccountBalance(String apiKey, String secretKey) {
        try {
            // Get account information
            Account account = getAccount(apiKey, secretKey);
            
            // Get ticker prices
            List<TickerPrice> tickers = getTickerPrices();
            
            // Create price mapping
            Map<String, Double> priceMap = new HashMap<>();
            tickers.forEach(ticker -> priceMap.put(ticker.getSymbol(), Double.parseDouble(ticker.getPrice())));
            
            // Filter non-zero balances
            List<Balance> balances = account.getBalances().stream()
                .filter(balance -> 
                    Double.parseDouble(balance.getFree()) > 0 || Double.parseDouble(balance.getLocked()) > 0)
                .collect(Collectors.toList());
            
            // Calculate USD values
            List<Balance> balancesWithUSD = balances.stream().map(balance -> {
                String asset = balance.getAsset();
                double free = Double.parseDouble(balance.getFree());
                double locked = Double.parseDouble(balance.getLocked());
                double total = free + locked;
                
                balance.setFreeValue(free);
                balance.setLockedValue(locked);
                balance.setTotalValue(total);
                
                double usdValue = 0;
                
                // Handle USD directly
                if (asset.equals("USD") || asset.equals("USDT") || asset.equals("USDC") || 
                    asset.equals("BUSD") || asset.equals("DAI")) {
                    usdValue = total;
                } else {
                    // Try to find direct USD pair
                    String usdPair = asset + "USD";
                    String usdtPair = asset + "USDT";
                    
                    if (priceMap.containsKey(usdPair)) {
                        usdValue = total * priceMap.get(usdPair);
                    } else if (priceMap.containsKey(usdtPair)) {
                        usdValue = total * priceMap.get(usdtPair);
                    } else {
                        // Try to find BTC pair and convert through BTC
                        String btcPair = asset + "BTC";
                        if (priceMap.containsKey(btcPair) && priceMap.containsKey("BTCUSD")) {
                            usdValue = total * priceMap.get(btcPair) * priceMap.get("BTCUSD");
                        }
                    }
                }
                
                balance.setUsdValue(usdValue);
                return balance;
            }).collect(Collectors.toList());
            
            // Sort by USD value
            balancesWithUSD.sort((a, b) -> Double.compare(b.getUsdValue(), a.getUsdValue()));
            
            return balancesWithUSD;
        } catch (Exception e) {
            log.error("Error getting account balance: {}", e.getMessage());
            throw new RuntimeException("Error getting account balance", e);
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
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test public API
            publicRequest(HttpMethod.GET, "/api/v3/ping", null, Object.class);
            result.put("publicApiWorking", true);
            
            // Test private API
            Account accountResult = null;
            String error = null;
            
            try {
                accountResult = getAccount(apiKey, secretKey);
            } catch (Exception e) {
                error = e.getMessage();
            }
            
            result.put("privateApiWorking", accountResult != null);
            result.put("error", error);
            result.put("accountResult", accountResult);
            
            return result;
        } catch (Exception e) {
            log.error("Error testing connection: {}", e.getMessage());
            result.put("publicApiWorking", false);
            result.put("privateApiWorking", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
}
