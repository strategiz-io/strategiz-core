package io.strategiz.client.etrade.auth;

import io.strategiz.client.etrade.error.EtradeErrors;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for handling E*TRADE OAuth 1.0a operations.
 * E*TRADE uses OAuth 1.0a with HMAC-SHA1 signing.
 *
 * OAuth 1.0a Flow:
 * 1. Get Request Token (POST /oauth/request_token)
 * 2. User authorizes at E*TRADE website
 * 3. Exchange Request Token + Verifier for Access Token (POST /oauth/access_token)
 *
 * Note: E*TRADE access tokens expire after 2 hours of inactivity.
 * There are no refresh tokens - users must re-authenticate when tokens expire.
 *
 * API URLs:
 * - Sandbox: https://apisb.etrade.com
 * - Production: https://api.etrade.com
 * - Authorization: https://us.etrade.com/e/t/etws/authorize
 */
@Component
public class EtradeOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(EtradeOAuthClient.class);

	private final RestTemplate restTemplate;

	@Value("${oauth.providers.etrade.consumer-key:}")
	private String consumerKey;

	@Value("${oauth.providers.etrade.consumer-secret:}")
	private String consumerSecret;

	@Value("${oauth.providers.etrade.redirect-uri:}")
	private String redirectUri;

	@Value("${oauth.providers.etrade.api-url:https://api.etrade.com}")
	private String apiUrl;

	@Value("${oauth.providers.etrade.authorize-url:https://us.etrade.com/e/t/etws/authorize}")
	private String authorizeUrl;

	public EtradeOAuthClient(@Qualifier("etradeRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("EtradeOAuthClient initialized");
	}

	/**
	 * Step 1: Get a request token from E*TRADE.
	 * @return Map containing oauth_token and oauth_token_secret for the request token
	 */
	public Map<String, String> getRequestToken() {
		try {
			log.info("Requesting E*TRADE request token");
			validateConfiguration();

			String endpoint = apiUrl + "/oauth/request_token";

			// Build OAuth parameters
			Map<String, String> oauthParams = OAuth1aSignature.createBaseOAuthParams(consumerKey, null);
			oauthParams.put("oauth_callback", redirectUri);

			// Generate signature (no token secret for request token)
			String signature = OAuth1aSignature.sign("POST", endpoint, oauthParams, null, consumerSecret, "");

			oauthParams.put("oauth_signature", signature);

			// Build Authorization header
			String authHeader = OAuth1aSignature.buildAuthorizationHeader(oauthParams);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authHeader);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<String> request = new HttpEntity<>(headers);

			log.info("Making request token request to: {}", endpoint);
			ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, String> tokenData = parseOAuthResponse(response.getBody());
				log.info("Successfully obtained E*TRADE request token");
				return tokenData;
			}
			else {
				throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED, "Failed to obtain request token");
			}

		}
		catch (RestClientResponseException e) {
			log.error("E*TRADE request token failed with status {}: {}", e.getStatusCode(),
					e.getResponseBodyAsString());
			throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED,
					"Request token failed: " + e.getResponseBodyAsString());
		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error getting E*TRADE request token", e);
			throw new StrategizException(EtradeErrors.ETRADE_API_ERROR,
					"Failed to get request token: " + e.getMessage());
		}
	}

	/**
	 * Step 2: Generate the authorization URL for the user to authorize the app.
	 * @param requestToken The request token from step 1
	 * @return URL for user to visit to authorize
	 */
	public String generateAuthorizationUrl(String requestToken) {
		validateConfiguration();

		return String.format("%s?key=%s&token=%s", authorizeUrl, consumerKey, requestToken);
	}

	/**
	 * Step 3: Exchange request token + verifier for access token.
	 * @param requestToken The request token from step 1
	 * @param requestTokenSecret The request token secret from step 1
	 * @param verifier The oauth_verifier from callback
	 * @return Map containing access token and access token secret
	 */
	public Map<String, String> getAccessToken(String requestToken, String requestTokenSecret, String verifier) {
		try {
			log.info("Exchanging request token for E*TRADE access token");

			String endpoint = apiUrl + "/oauth/access_token";

			// Build OAuth parameters
			Map<String, String> oauthParams = OAuth1aSignature.createBaseOAuthParams(consumerKey, requestToken);
			oauthParams.put("oauth_verifier", verifier);

			// Generate signature with request token secret
			String signature = OAuth1aSignature.sign("POST", endpoint, oauthParams, null, consumerSecret,
					requestTokenSecret);

			oauthParams.put("oauth_signature", signature);

			// Build Authorization header
			String authHeader = OAuth1aSignature.buildAuthorizationHeader(oauthParams);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authHeader);
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<String> request = new HttpEntity<>(headers);

			log.info("Making access token request to: {}", endpoint);
			ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, String> tokenData = parseOAuthResponse(response.getBody());
				log.info("Successfully obtained E*TRADE access token");
				return tokenData;
			}
			else {
				throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED, "Failed to obtain access token");
			}

		}
		catch (RestClientResponseException e) {
			log.error("E*TRADE access token exchange failed with status {}: {}", e.getStatusCode(),
					e.getResponseBodyAsString());
			throw new StrategizException(EtradeErrors.ETRADE_AUTH_FAILED,
					"Access token exchange failed: " + e.getResponseBodyAsString());
		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error during E*TRADE access token exchange", e);
			throw new StrategizException(EtradeErrors.ETRADE_API_ERROR,
					"Failed to exchange for access token: " + e.getMessage());
		}
	}

	/**
	 * Renew access token to extend session. E*TRADE allows renewing access tokens to
	 * reset the 2-hour inactivity timeout. Call this periodically to keep the session
	 * alive.
	 * @param accessToken The current access token
	 * @param accessTokenSecret The access token secret
	 * @return Map containing renewed access token info
	 */
	public Map<String, String> renewAccessToken(String accessToken, String accessTokenSecret) {
		try {
			log.info("Renewing E*TRADE access token");

			String endpoint = apiUrl + "/oauth/renew_access_token";

			// Build OAuth parameters
			Map<String, String> oauthParams = OAuth1aSignature.createBaseOAuthParams(consumerKey, accessToken);

			// Generate signature
			String signature = OAuth1aSignature.sign("GET", endpoint, oauthParams, null, consumerSecret,
					accessTokenSecret);

			oauthParams.put("oauth_signature", signature);

			// Build Authorization header
			String authHeader = OAuth1aSignature.buildAuthorizationHeader(oauthParams);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authHeader);

			HttpEntity<String> request = new HttpEntity<>(headers);

			log.info("Making renew token request to: {}", endpoint);
			ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				log.info("Successfully renewed E*TRADE access token");
				// Return the same tokens - they're still valid, just renewed
				Map<String, String> result = new HashMap<>();
				result.put("oauth_token", accessToken);
				result.put("oauth_token_secret", accessTokenSecret);
				result.put("renewed", "true");
				return result;
			}
			else {
				throw new StrategizException(EtradeErrors.ETRADE_TOKEN_EXPIRED,
						"Failed to renew access token - re-authentication required");
			}

		}
		catch (RestClientResponseException e) {
			log.error("E*TRADE token renewal failed with status {}: {}", e.getStatusCode(),
					e.getResponseBodyAsString());
			throw new StrategizException(EtradeErrors.ETRADE_TOKEN_EXPIRED,
					"Token renewal failed - re-authentication required");
		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Unexpected error during E*TRADE token renewal", e);
			throw new StrategizException(EtradeErrors.ETRADE_API_ERROR,
					"Failed to renew access token: " + e.getMessage());
		}
	}

	/**
	 * Revoke access token (logout).
	 * @param accessToken The access token to revoke
	 * @param accessTokenSecret The access token secret
	 */
	public void revokeAccessToken(String accessToken, String accessTokenSecret) {
		try {
			log.info("Revoking E*TRADE access token");

			String endpoint = apiUrl + "/oauth/revoke_access_token";

			// Build OAuth parameters
			Map<String, String> oauthParams = OAuth1aSignature.createBaseOAuthParams(consumerKey, accessToken);

			// Generate signature
			String signature = OAuth1aSignature.sign("GET", endpoint, oauthParams, null, consumerSecret,
					accessTokenSecret);

			oauthParams.put("oauth_signature", signature);

			// Build Authorization header
			String authHeader = OAuth1aSignature.buildAuthorizationHeader(oauthParams);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authHeader);

			HttpEntity<String> request = new HttpEntity<>(headers);

			restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
			log.info("Successfully revoked E*TRADE access token");

		}
		catch (Exception e) {
			log.warn("Failed to revoke E*TRADE access token: {}", e.getMessage());
			// Don't throw - token revocation failure shouldn't break logout flow
		}
	}

	/**
	 * Parse OAuth response (form-urlencoded format).
	 * @param response Response body like "oauth_token=xxx&oauth_token_secret=yyy"
	 * @return Map of parsed values
	 */
	private Map<String, String> parseOAuthResponse(String response) {
		Map<String, String> result = new LinkedHashMap<>();

		if (response != null && !response.isEmpty()) {
			String[] pairs = response.split("&");
			for (String pair : pairs) {
				String[] keyValue = pair.split("=", 2);
				if (keyValue.length == 2) {
					result.put(keyValue[0], keyValue[1]);
				}
			}
		}

		return result;
	}

	/**
	 * Validate OAuth configuration.
	 */
	public void validateConfiguration() {
		if (consumerKey == null || consumerKey.trim().isEmpty()) {
			throw new StrategizException(EtradeErrors.ETRADE_CONFIGURATION_ERROR,
					"E*TRADE OAuth consumer key not configured");
		}
		if (consumerSecret == null || consumerSecret.trim().isEmpty()) {
			throw new StrategizException(EtradeErrors.ETRADE_CONFIGURATION_ERROR,
					"E*TRADE OAuth consumer secret not configured");
		}
		if (redirectUri == null || redirectUri.trim().isEmpty()) {
			throw new StrategizException(EtradeErrors.ETRADE_CONFIGURATION_ERROR,
					"E*TRADE OAuth redirect URI not configured");
		}
	}

	// Getters for configuration

	public String getConsumerKey() {
		return consumerKey;
	}

	public String getConsumerSecret() {
		return consumerSecret;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public String getApiUrl() {
		return apiUrl;
	}

}
