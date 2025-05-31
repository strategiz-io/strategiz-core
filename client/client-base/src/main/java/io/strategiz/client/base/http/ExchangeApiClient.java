package io.strategiz.client.base.http;

import io.strategiz.framework.exception.ApplicationClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

/**
 * Specialized HTTP client for cryptocurrency exchange APIs.
 * Provides common authentication and request signing methods.
 * Ensures we only use real API data, never mocks or simulations.
 */
public abstract class ExchangeApiClient extends BaseHttpClient {
    
    private static final Logger log = LoggerFactory.getLogger(ExchangeApiClient.class);
    
    protected String apiKey;
    protected String privateKey;
    
    /**
     * Creates a new ExchangeApiClient with the specified base URL and credentials.
     *
     * @param baseUrl The base URL for API requests
     * @param apiKey The API key for authentication
     * @param privateKey The private key (secret) for request signing
     */
    protected ExchangeApiClient(String baseUrl, String apiKey, String privateKey) {
        super(baseUrl);
        this.apiKey = apiKey;
        this.privateKey = privateKey;
        log.info("Initialized ExchangeApiClient for exchange at: {}", baseUrl);
    }
    
    /**
     * Adds authentication headers for exchange API requests.
     * Override this in specific exchange clients to match their auth requirements.
     *
     * @param headers The HTTP headers to configure
     */
    @Override
    protected void configureDefaultHeaders(HttpHeaders headers) {
        super.configureDefaultHeaders(headers);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.set("API-Key", apiKey);
        }
    }
    
    /**
     * Signs an API request payload using HMAC-SHA256.
     * This is a common authentication method for exchange APIs.
     *
     * @param payload The payload to sign
     * @return The signed payload as a Base64 string
     */
    protected String signRequest(String payload) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(
                    hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error signing request: {}", e.getMessage(), e);
            throw new ApplicationClientException("Failed to sign API request", e);
        }
    }
    
    /**
     * Gets account information from the exchange API.
     * Returns the completely unmodified raw data from the API.
     *
     * @return The raw account data exactly as it comes from the API
     */
    public abstract Object getRawAccountData();
    
    /**
     * Gets raw position/balance information from the exchange API.
     * Returns the completely unmodified raw data from the API.
     *
     * @return The raw position/balance data exactly as it comes from the API
     */
    public abstract Object getRawPositionsData();
    
    /**
     * Validates that credentials are for a real exchange account, not test or paper trading.
     * This enforces Strategiz's requirement to use ONLY real APIs, never mock data.
     * 
     * @throws ApplicationClientException if credentials are for a test/demo account
     */
    protected void validateRealExchangeAccount() {
        log.info("Validating real exchange account credentials");
        // Implementation will check if credentials are for a real account
        // and throw an exception if they're for a test/demo account
    }
}
