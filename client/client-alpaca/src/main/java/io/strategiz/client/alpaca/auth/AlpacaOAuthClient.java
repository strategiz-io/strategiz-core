package io.strategiz.client.alpaca.auth;

import io.strategiz.client.alpaca.error.AlpacaErrors;
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
 * Client for handling Alpaca OAuth operations. This class handles OAuth token exchange,
 * refresh, and revocation.
 */
@Component
public class AlpacaOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(AlpacaOAuthClient.class);

	private static final String TOKEN_ENDPOINT = "/oauth/token";

	private final RestTemplate restTemplate;

	@Value("${oauth.providers.alpaca.client-id:}")
	private String clientId;

	@Value("${oauth.providers.alpaca.client-secret:}")
	private String clientSecret;

	@Value("${oauth.providers.alpaca.redirect-uri:}")
	private String redirectUri;

	@Value("${oauth.providers.alpaca.token-url:https://api.alpaca.markets/oauth/token}")
	private String tokenUrl;

	public AlpacaOAuthClient(@Qualifier("alpacaRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("AlpacaOAuthClient initialized");
	}

	/**
	 * Exchange authorization code for access token
	 * @param authorizationCode The authorization code from OAuth callback
	 * @return Token response containing access_token, refresh_token, etc.
	 */
	public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
		try {
			log.debug("Exchanging authorization code for tokens");
			log.debug("Using client_id: {}", clientId);
			log.debug("Using redirect_uri: {}", redirectUri);
			log.debug("Using token_url: {}", tokenUrl);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			// Add Basic Authentication as some OAuth providers require it
			headers.setBasicAuth(clientId, clientSecret);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "authorization_code");
			params.add("code", authorizationCode);
			params.add("redirect_uri", redirectUri);
			// Also include in body for maximum compatibility
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			log.debug("Making token exchange request to: {}", tokenUrl);
			ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("Successfully exchanged code for tokens");
				return response.getBody();
			}
			else {
				throw new StrategizException(AlpacaErrors.ALPACA_AUTH_FAILED, "Failed to exchange authorization code");
			}

		}
		catch (RestClientResponseException e) {
			log.error("OAuth token exchange failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new StrategizException(AlpacaErrors.ALPACA_AUTH_FAILED,
					"OAuth token exchange failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during token exchange", e);
			throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR,
					"Failed to exchange authorization code: " + e.getMessage());
		}
	}

	/**
	 * Refresh access token using refresh token
	 * @param refreshToken The refresh token
	 * @return New token response
	 */
	public Map<String, Object> refreshAccessToken(String refreshToken) {
		try {
			log.debug("Refreshing access token");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			// Add Basic Authentication
			headers.setBasicAuth(clientId, clientSecret);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "refresh_token");
			params.add("refresh_token", refreshToken);
			// Also include in body for maximum compatibility
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("Successfully refreshed access token");
				return response.getBody();
			}
			else {
				throw new StrategizException(AlpacaErrors.ALPACA_TOKEN_EXPIRED, "Failed to refresh access token");
			}

		}
		catch (RestClientResponseException e) {
			log.error("Token refresh failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new StrategizException(AlpacaErrors.ALPACA_TOKEN_EXPIRED, "Token refresh failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during token refresh", e);
			throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR,
					"Failed to refresh access token: " + e.getMessage());
		}
	}

	/**
	 * Revoke access token Note: Alpaca doesn't have a specific token revocation endpoint
	 * in their OAuth spec. This method is a placeholder for consistency with other
	 * providers.
	 * @param accessToken The access token to revoke
	 * @return Success status (always returns true as Alpaca handles this server-side)
	 */
	public boolean revokeAccessToken(String accessToken) {
		try {
			log.debug("Revoking access token (no-op for Alpaca)");
			// Alpaca doesn't have a revoke endpoint
			// Tokens are invalidated when user disconnects via Alpaca dashboard
			log.info("Token revocation for Alpaca is handled server-side");
			return true;

		}
		catch (Exception e) {
			log.error("Error during token revocation", e);
			// Don't throw exception for revocation failures
			return false;
		}
	}

	/**
	 * Validate OAuth configuration
	 */
	public void validateConfiguration() {
		if (clientId == null || clientId.trim().isEmpty()) {
			throw new StrategizException(AlpacaErrors.ALPACA_CONFIGURATION_ERROR,
					"Alpaca OAuth client ID not configured");
		}
		if (clientSecret == null || clientSecret.trim().isEmpty()) {
			throw new StrategizException(AlpacaErrors.ALPACA_CONFIGURATION_ERROR,
					"Alpaca OAuth client secret not configured");
		}
		if (redirectUri == null || redirectUri.trim().isEmpty()) {
			throw new StrategizException(AlpacaErrors.ALPACA_CONFIGURATION_ERROR,
					"Alpaca OAuth redirect URI not configured");
		}
	}

}
