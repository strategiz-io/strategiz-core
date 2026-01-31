package io.strategiz.client.facebook.helper;

import io.strategiz.client.facebook.model.FacebookTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * Helper class for Facebook OAuth token operations
 */
@Component
public class FacebookTokenHelper {

	private static final Logger logger = LoggerFactory.getLogger(FacebookTokenHelper.class);

	private static final String TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";

	private final RestTemplate restTemplate;

	public FacebookTokenHelper(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * Exchange authorization code for access token
	 */
	public Optional<FacebookTokenResponse> exchangeCodeForToken(String code, String clientId, String clientSecret,
			String redirectUri) {
		try {
			logger.info("Exchanging code for token with redirect_uri: {}", redirectUri);
			logger.info("Using client_id: {}...",
					clientId != null ? clientId.substring(0, Math.min(15, clientId.length())) : "null");

			String tokenUrl = buildTokenUrl(code, clientId, clientSecret, redirectUri);
			ResponseEntity<Map<String, Object>> response = makeTokenRequest(tokenUrl);

			if (isSuccessfulResponse(response)) {
				logger.info("Successfully exchanged code for token");
				return extractTokenFromResponse(response);
			}

			// Log the actual error response from Facebook
			logger.error("Failed to get access token from Facebook. Status: {}, Body: {}", response.getStatusCode(),
					response.getBody());
			return Optional.empty();

		}
		catch (Exception e) {
			logger.error("Error exchanging code for token: {}", e.getMessage(), e);
			return Optional.empty();
		}
	}

	private String buildTokenUrl(String code, String clientId, String clientSecret, String redirectUri) {
		return UriComponentsBuilder.fromUriString(TOKEN_URL)
			.queryParam("client_id", clientId)
			.queryParam("client_secret", clientSecret)
			.queryParam("code", code)
			.queryParam("redirect_uri", redirectUri)
			.toUriString();
	}

	private ResponseEntity<Map<String, Object>> makeTokenRequest(String tokenUrl) {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> entity = new HttpEntity<>(headers);

		return restTemplate.exchange(tokenUrl, HttpMethod.GET, entity,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	private boolean isSuccessfulResponse(ResponseEntity<Map<String, Object>> response) {
		return response.getStatusCode().is2xxSuccessful() && response.getBody() != null;
	}

	private Optional<FacebookTokenResponse> extractTokenFromResponse(ResponseEntity<Map<String, Object>> response) {
		Map<String, Object> body = response.getBody();
		String accessToken = (String) body.get("access_token");
		String tokenType = (String) body.get("token_type");
		Integer expiresIn = (Integer) body.get("expires_in");

		if (accessToken != null) {
			return Optional.of(new FacebookTokenResponse(accessToken, tokenType, expiresIn));
		}
		return Optional.empty();
	}

}