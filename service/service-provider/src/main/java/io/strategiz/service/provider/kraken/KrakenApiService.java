package io.strategiz.service.provider.kraken;

import io.strategiz.data.user.model.Credentials;
import io.strategiz.data.user.model.Provider;
import io.strategiz.data.user.repository.UserRepository;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.provider.exception.ProviderErrors;
import io.strategiz.service.provider.api.AbstractProviderApiService;
import io.strategiz.service.provider.config.ProviderOAuthConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Service for interacting with the Kraken API.
 * Handles OAuth authentication and API calls.
 */
@Service
public class KrakenApiService extends AbstractProviderApiService {

    // Provider ID is static and immutable
    
    private static final String PROVIDER_ID = "kraken";
    
    // API endpoints
    private static final String KRAKEN_BASE_URL = "https://api.kraken.com";
    private static final String KRAKEN_AUTH_URL = "https://auth.kraken.com/oauth2/authorize";
    private static final String KRAKEN_TOKEN_URL = "https://auth.kraken.com/oauth2/token";
    
    // API version
    private static final String API_VERSION = "/0"; // Kraken uses /0 for their current REST API version
    
    // Endpoints
    private static final String GET_ACCOUNT_BALANCE_ENDPOINT = API_VERSION + "/private/Balance";
    private static final String GET_TRADE_BALANCE_ENDPOINT = API_VERSION + "/private/TradeBalance";
    private static final String GET_OPEN_ORDERS_ENDPOINT = API_VERSION + "/private/OpenOrders";
    private static final String GET_CLOSED_ORDERS_ENDPOINT = API_VERSION + "/private/ClosedOrders";
    private static final String CREATE_ORDER_ENDPOINT = API_VERSION + "/private/AddOrder";
    
    private final ProviderOAuthConfig providerOAuthConfig;

    @Autowired
    public KrakenApiService(
            RestTemplate restTemplate,
            UserRepository userRepository,
            ProviderOAuthConfig providerOAuthConfig) {
        super(restTemplate, userRepository);
        this.providerOAuthConfig = providerOAuthConfig;
    }
    
    /**
     * Get the provider ID.
     *
     * @return The provider ID
     */
    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    /**
     * Generate the OAuth authorization URL for Kraken.
     *
     * @param userId The user ID
     * @param accountType The account type (paper or real)
     * @param state A state parameter for security
     * @return The authorization URL to redirect the user to
     */
    public String generateAuthorizationUrl(String userId, String accountType, String state) {
        ProviderOAuthConfig.ProviderOAuthSettings krakenConfig = providerOAuthConfig.getKraken();
        if (krakenConfig == null) {
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken OAuth configuration not found for provider " + PROVIDER_ID);
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(KRAKEN_AUTH_URL)
                .queryParam("client_id", krakenConfig.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", krakenConfig.getRedirectUri())
                .queryParam("scope", "read trade") // Basic scopes for reading account info and trading
                .queryParam("state", state);
        
        // Add account type to state
        if ("paper".equals(accountType)) {
            builder.queryParam("account_type", "paper");
        }
        
        return builder.toUriString();
    }

    /**
     * Exchange an authorization code for access and refresh tokens.
     *
     * @param userId The user ID
     * @param code The authorization code
     * @param accountType The account type
     * @return True if successful
     */
    public boolean handleOAuthCallback(String userId, String code, String accountType) {
        try {
            ProviderOAuthConfig.ProviderOAuthSettings krakenConfig = providerOAuthConfig.getKraken();
            if (krakenConfig == null) {
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken OAuth configuration not found for provider " + PROVIDER_ID);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(krakenConfig.getClientId(), krakenConfig.getClientSecret());
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "authorization_code");
            requestBody.put("code", code);
            requestBody.put("redirect_uri", krakenConfig.getRedirectUri());
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    KRAKEN_TOKEN_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to get access token from Kraken");
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Failed to get access token from Kraken provider " + PROVIDER_ID);
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            
            // Store provider configuration
            Provider provider = new Provider();
            provider.setId(PROVIDER_ID);
            provider.setProviderType("EXCHANGE");
            provider.setAccountType(accountType.toUpperCase());
            
            Map<String, Object> settings = new HashMap<>();
            settings.put("connectedAt", System.currentTimeMillis());
            settings.put("name", "Kraken");
            provider.setSettings(settings);
            
            userRepository.saveProvider(userId, PROVIDER_ID, provider);
            
            // Store credentials separately
            Credentials credentials = new Credentials();
            credentials.setApiKey(accessToken);
            credentials.setPrivateKey(refreshToken);
            
            Map<String, Object> encryptedData = new HashMap<>();
            encryptedData.put("token_type", tokenResponse.get("token_type"));
            encryptedData.put("expires_in", tokenResponse.get("expires_in"));
            encryptedData.put("scope", tokenResponse.get("scope"));
            encryptedData.put("created_at", System.currentTimeMillis());
            credentials.setEncryptedData(encryptedData);
            
            userRepository.saveCredentials(userId, PROVIDER_ID, credentials);
            
            return true;
        } catch (RestClientException e) {
            log.error("REST client error handling OAuth callback for Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error connecting to Kraken API for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error handling OAuth callback for Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Unexpected error handling Kraken OAuth for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Refresh the OAuth tokens for Kraken.
     *
     * @param userId The user ID
     * @return True if successful
     */
    public boolean refreshTokens(String userId) {
        try {
            // Get current credentials
            Optional<Credentials> credentialsOpt = userRepository.getCredentials(userId, PROVIDER_ID);
            if (!credentialsOpt.isPresent()) {
                log.error("No credentials found for user {} and provider {}", userId, PROVIDER_ID);
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "No credentials found for Kraken provider " + PROVIDER_ID + " and user " + userId);
            }
            
            Credentials credentials = credentialsOpt.get();
            String refreshToken = credentials.getPrivateKey(); // Refresh token is stored in privateKey field
            
            // Refresh tokens
            ProviderOAuthConfig.ProviderOAuthSettings krakenConfig = providerOAuthConfig.getKraken();
            if (krakenConfig == null) {
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken OAuth configuration not found for provider " + PROVIDER_ID);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(krakenConfig.getClientId(), krakenConfig.getClientSecret());
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "refresh_token");
            requestBody.put("refresh_token", refreshToken);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    KRAKEN_TOKEN_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to refresh token for Kraken");
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Failed to verify access with Kraken API for provider " + PROVIDER_ID);
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            String newRefreshToken = (String) tokenResponse.get("refresh_token");
            
            // Update credentials
            credentials.setApiKey(accessToken);
            if (newRefreshToken != null) {
                credentials.setPrivateKey(newRefreshToken);
            }
            
            Map<String, Object> encryptedData = credentials.getEncryptedData();
            encryptedData.put("token_type", tokenResponse.get("token_type"));
            encryptedData.put("expires_in", tokenResponse.get("expires_in"));
            encryptedData.put("scope", tokenResponse.get("scope"));
            encryptedData.put("last_refreshed", System.currentTimeMillis());
            credentials.setEncryptedData(encryptedData);
            
            userRepository.saveCredentials(userId, PROVIDER_ID, credentials);
            
            return true;
        } catch (RestClientException e) {
            log.error("REST client error refreshing tokens for Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error connecting to Kraken API for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error refreshing tokens for Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Unexpected error refreshing Kraken tokens for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get account balance from Kraken.
     *
     * @param userId The user ID
     * @return Map containing the account balance data
     */
    public Map<String, Object> getAccountBalance(String userId) {
        Credentials credentials = getCredentials(userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + credentials.getApiKey());
        
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KRAKEN_BASE_URL + GET_ACCOUNT_BALANCE_ENDPOINT,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("error") && ((List<?>) responseBody.get("error")).size() > 0) {
                List<?> errors = (List<?>) responseBody.get("error");
                String errorMsg = errors.get(0).toString();
                log.error("Kraken API error: {}", errorMsg);
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken API error for provider " + PROVIDER_ID + ": " + errorMsg);
            }
            
            return responseBody != null ? responseBody : Collections.emptyMap();
        } catch (RestClientException e) {
            log.error("Error fetching account balance from Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error connecting to Kraken API for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        }
    }

    /**
     * Get open orders from Kraken.
     *
     * @param userId The user ID
     * @return Map containing the open orders data
     */
    public Map<String, Object> getOpenOrders(String userId) {
        Credentials credentials = getCredentials(userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + credentials.getApiKey());
        
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KRAKEN_BASE_URL + GET_OPEN_ORDERS_ENDPOINT,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("error") && ((List<?>) responseBody.get("error")).size() > 0) {
                List<?> errors = (List<?>) responseBody.get("error");
                String errorMsg = errors.get(0).toString();
                log.error("Kraken API error: {}", errorMsg);
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken API error for provider " + PROVIDER_ID + ": " + errorMsg);
            }
            
            return responseBody != null ? responseBody : Collections.emptyMap();
        } catch (RestClientException e) {
            log.error("Error fetching open orders from Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error connecting to Kraken API for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        }
    }

    /**
     * Place a new order on Kraken.
     *
     * @param userId The user ID
     * @param orderParams Map containing order parameters
     * @return Map containing the order result
     */
    @Override
    public Map<String, Object> placeOrder(String userId, Map<String, String> orderParams) {
        Credentials credentials = getCredentials(userId);
        
        // Add API authentication headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + credentials.getApiKey());
        
        // Convert String params to Object for the request body
        Map<String, Object> requestBody = new HashMap<>(orderParams);
        
        // Prepare request body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    KRAKEN_BASE_URL + CREATE_ORDER_ENDPOINT,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> responseBody = responseEntity.getBody();
            
            // Check for API errors
            if (responseBody != null && responseBody.containsKey("error") && !((List<?>) responseBody.get("error")).isEmpty()) {
                List<?> errors = (List<?>) responseBody.get("error");
                String errorMsg = errors.get(0).toString();
                log.error("Kraken API error: {}", errorMsg);
                throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Kraken API error for provider " + PROVIDER_ID + ": " + errorMsg);
            }
            
            return responseBody;
        } catch (RestClientException e) {
            log.error("Error placing order on Kraken", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error connecting to Kraken API for provider " + PROVIDER_ID + ": " + e.getMessage(), e);
        }
    }

    /**
     * Disconnect the Kraken provider by removing its configuration and credentials.
     *
     * @param userId The user ID
     * @return True if successful
     */
    public boolean disconnectProvider(String userId) {
        try {
            userRepository.deleteProvider(userId, PROVIDER_ID);
            userRepository.deleteCredentials(userId, PROVIDER_ID);
            return true;
        } catch (Exception e) {
            log.error("Error disconnecting Kraken provider", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Error disconnecting Kraken provider " + PROVIDER_ID, e);
        }
    }

    /**
     * Verify access to the Kraken API.
     *
     * @param userId The user ID
     * @return True if access is verified
     */
    public boolean verifyAccess(String userId) {
        try {
            // Test connection by making a simple API call
            Map<String, Object> accountInfo = getAccountBalance(userId);
            return accountInfo != null;
        } catch (Exception e) {
            log.error("Failed to verify access with Kraken API", e);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Failed to verify access with Kraken API for provider " + PROVIDER_ID, e);
        }
    }

    /**
     * Helper method to get credentials and validate token expiration
     */
    protected Credentials getCredentials(String userId) {
        Optional<Credentials> credentialsOpt = userRepository.getCredentials(userId, PROVIDER_ID);
        if (!credentialsOpt.isPresent()) {
            log.error("No credentials found for user {} and provider {}", userId, PROVIDER_ID);
            throw new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "No credentials found for Kraken provider " + PROVIDER_ID + " and user " + userId);
        }
        
        Credentials credentials = credentialsOpt.get();
        
        // Check if we need to refresh the token
        Map<String, Object> encryptedData = credentials.getEncryptedData();
        if (encryptedData != null && encryptedData.containsKey("created_at") && encryptedData.containsKey("expires_in")) {
            long createdAt = (long) encryptedData.get("created_at");
            int expiresIn = (int) encryptedData.get("expires_in");
            long expiresAt = createdAt + (expiresIn * 1000L);
            
            if (System.currentTimeMillis() + 60000 > expiresAt) { // Refresh if token expires in less than a minute
                log.info("Refreshing Kraken API token for user {}", userId);
                refreshTokens(userId);
                // Get updated credentials
                credentials = userRepository.getCredentials(userId, PROVIDER_ID)
                        .orElseThrow(() -> new StrategizException(ProviderErrors.KRAKEN_API_ERROR, "Failed to refresh Kraken token for provider " + PROVIDER_ID));
            }
        }
        
        return credentials;
    }
}
