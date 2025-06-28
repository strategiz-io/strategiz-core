package io.strategiz.service.provider;

import io.strategiz.data.user.model.Credentials;
import io.strategiz.data.user.model.Provider;
import io.strategiz.data.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling OAuth flows with various exchanges and brokerages.
 */
@Service
public class ProviderOAuthService {

    private static final Logger log = LoggerFactory.getLogger(ProviderOAuthService.class);

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    
    // OAuth credentials from configuration
    @Value("${provider.kraken.client-id}")
    private String krakenClientId;
    
    @Value("${provider.kraken.client-secret}")
    private String krakenClientSecret;
    
    @Value("${provider.binanceus.client-id}")
    private String binanceUsClientId;
    
    @Value("${provider.binanceus.client-secret}")
    private String binanceUsClientSecret;
    
    // Charles Schwab OAuth will be implemented separately
    
    // OAuth endpoints
    private static final String KRAKEN_AUTH_URL = "https://auth.kraken.com/oauth2/authorize";
    private static final String KRAKEN_TOKEN_URL = "https://auth.kraken.com/oauth2/token";
    
    private static final String BINANCEUS_AUTH_URL = "https://accounts.binance.us/oauth/authorize";
    private static final String BINANCEUS_TOKEN_URL = "https://accounts.binance.us/oauth/token";
    
    // Charles Schwab OAuth URLs removed - will be implemented separately
    
    // Redirect URI for OAuth callbacks
    @Value("${provider.oauth.redirect-uri}")
    private String redirectUri;

    @Autowired
    public ProviderOAuthService(RestTemplate restTemplate, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }

    /**
     * Generate the OAuth authorization URL for a specific provider.
     *
     * @param providerId The provider ID (kraken, binanceus)
     * @param userId The user ID
     * @param accountType The account type (paper or real)
     * @param state A state parameter for security
     * @return The authorization URL to redirect the user to
     */
    public String generateAuthorizationUrl(String providerId, String userId, String accountType, String state) {
        UriComponentsBuilder builder;
        
        switch (providerId) {
            case "kraken":
                builder = UriComponentsBuilder.fromHttpUrl(KRAKEN_AUTH_URL)
                        .queryParam("client_id", krakenClientId)
                        .queryParam("response_type", "code")
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("scope", "read trade") // Adjust scopes as needed
                        .queryParam("state", state);
                break;
            case "binanceus":
                builder = UriComponentsBuilder.fromHttpUrl(BINANCEUS_AUTH_URL)
                        .queryParam("client_id", binanceUsClientId)
                        .queryParam("response_type", "code")
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("scope", "user:read account:read trade:read trade:write")
                        .queryParam("state", state);
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + providerId);
        }
        
        // Add account type to state
        if ("paper".equals(accountType)) {
            builder.queryParam("account_type", "paper");
        }
        
        return builder.toUriString();
    }

    /**
     * Handle the OAuth callback and exchange the code for tokens.
     *
     * @param providerId The provider ID
     * @param userId The user ID
     * @param code The authorization code
     * @param accountType The account type (paper or real)
     * @return True if successful
     */
    public boolean handleOAuthCallback(String providerId, String userId, String code, String accountType) {
        try {
            String tokenUrl;
            String clientId;
            String clientSecret;
            
            // Determine which provider credentials to use
            switch (providerId) {
                case "kraken":
                    tokenUrl = KRAKEN_TOKEN_URL;
                    clientId = krakenClientId;
                    clientSecret = krakenClientSecret;
                    break;
                case "binanceus":
                    tokenUrl = BINANCEUS_TOKEN_URL;
                    clientId = binanceUsClientId;
                    clientSecret = binanceUsClientSecret;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported provider: " + providerId);
            }
            
            // Exchange code for tokens
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "authorization_code");
            requestBody.put("code", code);
            requestBody.put("redirect_uri", redirectUri);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to get access token from {}", providerId);
                return false;
            }
            
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            
            // Store provider configuration
            Provider provider = new Provider();
            provider.setId(providerId);
            provider.setProviderType("kraken".equals(providerId) || "binanceus".equals(providerId) ? "EXCHANGE" : "BROKER");
            provider.setAccountType(accountType.toUpperCase());
            
            Map<String, Object> settings = new HashMap<>();
            settings.put("connectedAt", System.currentTimeMillis());
            provider.setSettings(settings);
            
            userRepository.saveProvider(userId, providerId, provider);
            
            // Store credentials separately
            Credentials credentials = new Credentials();
            credentials.setApiKey(accessToken);
            credentials.setPrivateKey(refreshToken);
            
            Map<String, Object> encryptedData = new HashMap<>();
            encryptedData.put("token_type", tokenResponse.get("token_type"));
            encryptedData.put("expires_in", tokenResponse.get("expires_in"));
            encryptedData.put("scope", tokenResponse.get("scope"));
            credentials.setEncryptedData(encryptedData);
            
            userRepository.saveCredentials(userId, providerId, credentials);
            
            return true;
        } catch (Exception e) {
            log.error("Error handling OAuth callback for provider: {}", providerId, e);
            return false;
        }
    }
    
    /**
     * Refresh the OAuth tokens for a provider.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return True if successful
     */
    public boolean refreshTokens(String userId, String providerId) {
        try {
            // Get current credentials
            Optional<Credentials> credentialsOpt = userRepository.getCredentials(userId, providerId);
            if (!credentialsOpt.isPresent()) {
                log.error("No credentials found for user {} and provider {}", userId, providerId);
                return false;
            }
            
            Credentials credentials = credentialsOpt.get();
            String refreshToken = credentials.getPrivateKey(); // Refresh token is stored in privateKey field
            
            String tokenUrl;
            String clientId;
            String clientSecret;
            
            // Determine which provider credentials to use
            switch (providerId) {
                case "kraken":
                    tokenUrl = KRAKEN_TOKEN_URL;
                    clientId = krakenClientId;
                    clientSecret = krakenClientSecret;
                    break;
                case "binanceus":
                    tokenUrl = BINANCEUS_TOKEN_URL;
                    clientId = binanceUsClientId;
                    clientSecret = binanceUsClientSecret;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported provider: " + providerId);
            }
            
            // Refresh tokens
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(clientId, clientSecret);
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "refresh_token");
            requestBody.put("refresh_token", refreshToken);
            
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );
            
            Map<String, Object> tokenResponse = responseEntity.getBody();
            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                log.error("Failed to refresh token for {}", providerId);
                return false;
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
            
            userRepository.saveCredentials(userId, providerId, credentials);
            
            return true;
        } catch (Exception e) {
            log.error("Error refreshing tokens for provider: {}", providerId, e);
            return false;
        }
    }
    
    /**
     * Disconnect a provider by removing its configuration and credentials.
     *
     * @param userId The user ID
     * @param providerId The provider ID
     * @return True if successful
     */
    public boolean disconnectProvider(String userId, String providerId) {
        try {
            userRepository.deleteProvider(userId, providerId);
            return true;
        } catch (Exception e) {
            log.error("Error disconnecting provider: {}", providerId, e);
            return false;
        }
    }
}
