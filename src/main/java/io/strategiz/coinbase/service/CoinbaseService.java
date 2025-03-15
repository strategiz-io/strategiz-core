package io.strategiz.coinbase.service;

import io.strategiz.coinbase.model.Account;
import io.strategiz.coinbase.model.CoinbaseResponse;
import io.strategiz.coinbase.model.TickerPrice;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
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
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for interacting with the Coinbase API
 */
@Slf4j
@Service
public class CoinbaseService {

    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RestTemplate restTemplate;

    public CoinbaseService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Configure the Coinbase API credentials
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
        response.put("message", "Coinbase API configured successfully");
        return response;
    }

    /**
     * Make a public request to Coinbase API
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @return API response
     */
    public <T> T publicRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              ParameterizedTypeReference<T> responseType) {
        try {
            URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
            
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
     * Make a signed request to Coinbase API
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param params Request parameters
     * @param apiKey API key
     * @param secretKey Secret key
     * @return API response
     */
    public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, 
                              String apiKey, String secretKey, ParameterizedTypeReference<T> responseType) {
        try {
            // Build the URL
            URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
            if (params != null) {
                params.forEach(uriBuilder::addParameter);
            }
            URI uri = uriBuilder.build();
            
            // Get timestamp
            long timestamp = Instant.now().getEpochSecond();
            
            // Create the message to sign
            String message = timestamp + method.name() + endpoint;
            if (params != null && !params.isEmpty()) {
                // Build query string manually instead of using getQuery()
                StringBuilder queryString = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(entry.getKey()).append("=").append(entry.getValue());
                }
                message += "?" + queryString.toString();
            }
            
            // Create signature
            String signature = generateSignature(message, secretKey);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("CB-ACCESS-KEY", apiKey);
            headers.set("CB-ACCESS-SIGN", signature);
            headers.set("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp));
            headers.set("CB-VERSION", "2021-04-29");
            
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
     * Generate HMAC SHA256 signature for Coinbase API
     * @param message Message to sign
     * @param secretKey Secret key
     * @return Signature
     */
    private String generateSignature(String message, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), HMAC_SHA256);
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Get user accounts from Coinbase
     * @param apiKey API key
     * @param secretKey Secret key
     * @return List of accounts
     */
    public List<Account> getAccounts(String apiKey, String secretKey) {
        try {
            CoinbaseResponse<Account> response = signedRequest(
                HttpMethod.GET,
                "/accounts",
                null,
                apiKey,
                secretKey,
                new ParameterizedTypeReference<CoinbaseResponse<Account>>() {}
            );
            
            return response.getData();
        } catch (Exception e) {
            log.error("Error getting accounts: {}", e.getMessage());
            throw new RuntimeException("Error getting accounts", e);
        }
    }

    /**
     * Get ticker price for a specific currency pair
     * @param baseCurrency Base currency (e.g., BTC)
     * @param quoteCurrency Quote currency (e.g., USD)
     * @return Ticker price
     */
    public TickerPrice getTickerPrice(String baseCurrency, String quoteCurrency) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("currency", quoteCurrency);
            
            CoinbaseResponse<TickerPrice> response = publicRequest(
                HttpMethod.GET,
                "/prices/" + baseCurrency + "/spot",
                params,
                new ParameterizedTypeReference<CoinbaseResponse<TickerPrice>>() {}
            );
            
            return response.getData().get(0);
        } catch (Exception e) {
            log.error("Error getting ticker price for {}-{}: {}", baseCurrency, quoteCurrency, e.getMessage());
            return null;
        }
    }

    /**
     * Get account balances with USD values
     * @param apiKey API key
     * @param secretKey Secret key
     * @return List of accounts with balances and USD values
     */
    public List<Account> getAccountBalances(String apiKey, String secretKey) {
        try {
            // Get all accounts
            List<Account> accounts = getAccounts(apiKey, secretKey);
            
            // Filter accounts with non-zero balances
            List<Account> nonZeroAccounts = accounts.stream()
                .filter(account -> {
                    if (account.getBalance() == null) return false;
                    double amount = Double.parseDouble(account.getBalance().getAmount());
                    return amount > 0;
                })
                .collect(Collectors.toList());
            
            // Calculate USD values for each account
            for (Account account : nonZeroAccounts) {
                String currency = account.getCurrency();
                double amount = Double.parseDouble(account.getBalance().getAmount());
                account.getBalance().setAmountValue(amount);
                
                double usdValue = 0;
                
                // Handle USD directly
                if (currency.equals("USD") || currency.equals("USDT") || currency.equals("USDC") || 
                    currency.equals("BUSD") || currency.equals("DAI")) {
                    usdValue = amount;
                } else {
                    // Get price in USD
                    TickerPrice ticker = getTickerPrice(currency, "USD");
                    if (ticker != null) {
                        usdValue = amount * Double.parseDouble(ticker.getAmount());
                    }
                }
                
                account.getBalance().setUsdValue(usdValue);
                account.setUsdValue(usdValue);
            }
            
            // Sort by USD value
            nonZeroAccounts.sort((a, b) -> Double.compare(b.getUsdValue(), a.getUsdValue()));
            
            return nonZeroAccounts;
        } catch (Exception e) {
            log.error("Error getting account balances: {}", e.getMessage());
            throw new RuntimeException("Error getting account balances", e);
        }
    }

    /**
     * Calculate total USD value of accounts
     * @param accounts List of accounts
     * @return Total USD value
     */
    public double calculateTotalUsdValue(List<Account> accounts) {
        return accounts.stream()
            .mapToDouble(Account::getUsdValue)
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
            TickerPrice btcPrice = getTickerPrice("BTC", "USD");
            result.put("publicApiWorking", btcPrice != null);
            
            // Test private API
            List<Account> accounts = null;
            String error = null;
            
            try {
                accounts = getAccounts(apiKey, secretKey);
            } catch (Exception e) {
                error = e.getMessage();
            }
            
            result.put("privateApiWorking", accounts != null);
            result.put("error", error);
            result.put("accountsCount", accounts != null ? accounts.size() : 0);
            
            return result;
        } catch (Exception e) {
            log.error("Error testing connection: {}", e.getMessage());
            result.put("publicApiWorking", false);
            result.put("privateApiWorking", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Get raw account data for admin viewing
     * @param apiKey API key
     * @param secretKey Secret key
     * @return Raw account data
     */
    public Object getRawAccountData(String apiKey, String secretKey) {
        try {
            return signedRequest(
                HttpMethod.GET,
                "/accounts",
                null,
                apiKey,
                secretKey,
                new ParameterizedTypeReference<Object>() {}
            );
        } catch (Exception e) {
            log.error("Error getting raw account data: {}", e.getMessage());
            throw new RuntimeException("Error getting raw account data", e);
        }
    }
}
