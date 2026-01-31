package io.strategiz.client.coinbase;

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
 * Client for handling Coinbase OAuth operations This class handles OAuth token exchange,
 * refresh, and revocation
 */
@Component
public class CoinbaseOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(CoinbaseOAuthClient.class);

	private static final String COINBASE_OAUTH_URL = "https://api.coinbase.com/oauth";

	private static final String TOKEN_ENDPOINT = "/token";

	private static final String REVOKE_ENDPOINT = "/revoke";

	private final RestTemplate restTemplate;

	@Value("${oauth.providers.coinbase.client-id:}")
	private String clientId;

	@Value("${oauth.providers.coinbase.client-secret:}")
	private String clientSecret;

	@Value("${oauth.providers.coinbase.redirect-uri:}")
	private String redirectUri;

	public CoinbaseOAuthClient(@Qualifier("coinbaseRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("CoinbaseOAuthClient initialized");
	}

	/**
	 * Exchange authorization code for access token
	 * @param authorizationCode The authorization code from OAuth callback
	 * @return Token response containing access_token, refresh_token, etc.
	 */
	public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
		try {
			log.debug("Exchanging authorization code for tokens");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "authorization_code");
			params.add("code", authorizationCode);
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);
			params.add("redirect_uri", redirectUri);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(COINBASE_OAUTH_URL + TOKEN_ENDPOINT, request,
					Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("Successfully exchanged code for tokens");
				return response.getBody();
			}
			else {
				throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR,
						"Failed to exchange authorization code");
			}

		}
		catch (RestClientResponseException e) {
			log.error("OAuth token exchange failed: {}", e.getResponseBodyAsString());
			throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR,
					"OAuth token exchange failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during token exchange", e);
			throw new StrategizException(CoinbaseErrors.API_ERROR,
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

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "refresh_token");
			params.add("refresh_token", refreshToken);
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(COINBASE_OAUTH_URL + TOKEN_ENDPOINT, request,
					Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.info("Successfully refreshed access token");
				return response.getBody();
			}
			else {
				throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR, "Failed to refresh access token");
			}

		}
		catch (RestClientResponseException e) {
			log.error("Token refresh failed: {}", e.getResponseBodyAsString());
			throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR,
					"Token refresh failed: " + e.getMessage());
		}
		catch (Exception e) {
			log.error("Unexpected error during token refresh", e);
			throw new StrategizException(CoinbaseErrors.API_ERROR, "Failed to refresh access token: " + e.getMessage());
		}
	}

	/**
	 * Revoke access token
	 * @param accessToken The access token to revoke
	 * @return Success status
	 */
	public boolean revokeAccessToken(String accessToken) {
		try {
			log.debug("Revoking access token");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("token", accessToken);
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			ResponseEntity<Void> response = restTemplate.postForEntity(COINBASE_OAUTH_URL + REVOKE_ENDPOINT, request,
					Void.class);

			boolean success = response.getStatusCode() == HttpStatus.OK
					|| response.getStatusCode() == HttpStatus.NO_CONTENT;

			if (success) {
				log.info("Successfully revoked access token");
			}
			else {
				log.warn("Token revocation returned status: {}", response.getStatusCode());
			}

			return success;

		}
		catch (Exception e) {
			log.error("Error revoking access token", e);
			// Don't throw exception for revocation failures
			return false;
		}
	}

	/**
	 * Validate OAuth configuration
	 */
	public void validateConfiguration() {
		if (clientId == null || clientId.trim().isEmpty()) {
			throw new StrategizException(CoinbaseErrors.CONFIGURATION_ERROR, "Coinbase OAuth client ID not configured");
		}
		if (clientSecret == null || clientSecret.trim().isEmpty()) {
			throw new StrategizException(CoinbaseErrors.CONFIGURATION_ERROR,
					"Coinbase OAuth client secret not configured");
		}
		if (redirectUri == null || redirectUri.trim().isEmpty()) {
			throw new StrategizException(CoinbaseErrors.CONFIGURATION_ERROR,
					"Coinbase OAuth redirect URI not configured");
		}
	}

}