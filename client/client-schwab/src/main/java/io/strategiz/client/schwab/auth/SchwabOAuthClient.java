package io.strategiz.client.schwab.auth;

import io.strategiz.client.schwab.error.SchwabErrors;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for handling Charles Schwab OAuth operations.
 * This class handles OAuth token exchange, refresh, and revocation.
 *
 * Note: Schwab uses Basic Authentication for token requests with client credentials
 * in the Authorization header, not in the request body.
 *
 * Token expiration:
 * - Access tokens expire after 30 minutes
 * - Refresh tokens expire after 7 days
 */
@Component
public class SchwabOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(SchwabOAuthClient.class);

    private static final String AUTH_BASE_URL = "https://api.schwabapi.com/v1";

    private final RestTemplate restTemplate;

    @Value("${oauth.providers.schwab.client-id:}")
    private String clientId;

    @Value("${oauth.providers.schwab.client-secret:}")
    private String clientSecret;

    @Value("${oauth.providers.schwab.redirect-uri:}")
    private String redirectUri;

    @Value("${oauth.providers.schwab.token-url:https://api.schwabapi.com/v1/oauth/token}")
    private String tokenUrl;

    public SchwabOAuthClient(@Qualifier("schwabRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        log.info("SchwabOAuthClient initialized");
    }

    /**
     * Exchange authorization code for access token.
     * Schwab requires Basic Authentication with client credentials in header.
     *
     * @param authorizationCode The authorization code from OAuth callback
     * @return Token response containing access_token, refresh_token, expires_in, etc.
     */
    public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
        try {
            log.info("Exchanging authorization code for Schwab tokens");
            log.info("Token exchange config - client_id: {}, redirect_uri: {}, token_url: {}",
                clientId != null ? clientId.substring(0, 8) + "..." : "null",
                redirectUri,
                tokenUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // Schwab requires Basic Authentication
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", authorizationCode);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info("Making token exchange request to: {}", tokenUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully exchanged code for Schwab tokens");
                return response.getBody();
            } else {
                throw new StrategizException(SchwabErrors.SCHWAB_AUTH_FAILED,
                    "Failed to exchange authorization code");
            }

        } catch (RestClientResponseException e) {
            log.error("Schwab OAuth token exchange failed with status {}: {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new StrategizException(SchwabErrors.SCHWAB_AUTH_FAILED,
                "OAuth token exchange failed: " + e.getResponseBodyAsString());
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Schwab token exchange", e);
            throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR,
                "Failed to exchange authorization code: " + e.getMessage());
        }
    }

    /**
     * Refresh access token using refresh token.
     * Should be called every 29 minutes to keep the access token valid.
     * Note: Refresh tokens expire after 7 days and require re-authentication.
     *
     * @param refreshToken The refresh token
     * @return New token response
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            log.debug("Refreshing Schwab access token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            // Schwab requires Basic Authentication
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Successfully refreshed Schwab access token");
                return response.getBody();
            } else {
                throw new StrategizException(SchwabErrors.SCHWAB_TOKEN_EXPIRED,
                    "Failed to refresh access token");
            }

        } catch (RestClientResponseException e) {
            log.error("Schwab token refresh failed with status {}: {}",
                e.getStatusCode(), e.getResponseBodyAsString());

            // Check if refresh token is expired (7 day limit)
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401) {
                throw new StrategizException(SchwabErrors.SCHWAB_TOKEN_EXPIRED,
                    "Refresh token expired. Please reconnect your Schwab account.");
            }

            throw new StrategizException(SchwabErrors.SCHWAB_TOKEN_EXPIRED,
                "Token refresh failed: " + e.getResponseBodyAsString());
        } catch (StrategizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Schwab token refresh", e);
            throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR,
                "Failed to refresh access token: " + e.getMessage());
        }
    }

    /**
     * Generate OAuth authorization URL for Charles Schwab.
     *
     * @param state Security state parameter
     * @return Authorization URL for user to visit
     */
    public String generateAuthorizationUrl(String state) {
        validateConfiguration();

        String scope = "readonly"; // Schwab basic scope for account access

        return String.format(
            "%s/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code&state=%s&scope=%s",
            AUTH_BASE_URL,
            clientId,
            redirectUri,
            state,
            scope
        );
    }

    /**
     * Validate OAuth configuration
     */
    public void validateConfiguration() {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new StrategizException(SchwabErrors.SCHWAB_CONFIGURATION_ERROR,
                "Schwab OAuth client ID not configured");
        }
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new StrategizException(SchwabErrors.SCHWAB_CONFIGURATION_ERROR,
                "Schwab OAuth client secret not configured");
        }
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            throw new StrategizException(SchwabErrors.SCHWAB_CONFIGURATION_ERROR,
                "Schwab OAuth redirect URI not configured");
        }
    }

    // Getters for configuration (used by SchwabClient)

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
