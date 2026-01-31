package io.strategiz.client.alpaca.client;

import io.strategiz.client.alpaca.auth.AlpacaOAuthClient;
import io.strategiz.client.alpaca.error.AlpacaErrors;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Main client for interacting with the Alpaca API. This class handles all direct API
 * communication with Alpaca.
 */
@Component
public class AlpacaClient {

	private static final Logger log = LoggerFactory.getLogger(AlpacaClient.class);

	private final RestTemplate restTemplate;

	private final AlpacaOAuthClient oauthClient;

	private final AlpacaDataClient dataClient;

	@Value("${oauth.providers.alpaca.api-url:https://api.alpaca.markets}")
	private String apiUrl;

	public AlpacaClient(@Qualifier("alpacaRestTemplate") RestTemplate restTemplate, AlpacaOAuthClient oauthClient,
			AlpacaDataClient dataClient) {
		this.restTemplate = restTemplate;
		this.oauthClient = oauthClient;
		this.dataClient = dataClient;
		log.info("AlpacaClient initialized");
	}

	/**
	 * Make an OAuth authenticated request to Alpaca API
	 * @param method HTTP method
	 * @param endpoint API endpoint (e.g., "/v2/account")
	 * @param accessToken OAuth access token
	 * @param params Request parameters
	 * @param responseType Expected response type
	 * @return API response
	 */
	public <T> T oauthRequest(HttpMethod method, String endpoint, String accessToken, Map<String, String> params,
			ParameterizedTypeReference<T> responseType) {
		try {
			// Validate access token
			if (accessToken == null || accessToken.trim().isEmpty()) {
				throw new StrategizException(AlpacaErrors.ALPACA_INVALID_TOKEN, "Access token is required");
			}

			// Build the URL
			URIBuilder uriBuilder = new URIBuilder(apiUrl + endpoint);
			if (params != null) {
				params.forEach(uriBuilder::addParameter);
			}

			URI uri = uriBuilder.build();

			// Create headers with OAuth Bearer token
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + accessToken);
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			log.debug("Making OAuth request to Alpaca API: {} {}", method, uri);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Alpaca OAuth API request error - HTTP Status {}: {}", statusCode, responseBody);

			// Handle token expiration
			if (statusCode == 401) {
				throw new StrategizException(AlpacaErrors.ALPACA_TOKEN_EXPIRED,
						"Access token expired or invalid. Please reconnect your Alpaca account.");
			}

			// Handle rate limiting
			if (statusCode == 429) {
				throw new StrategizException(AlpacaErrors.ALPACA_RATE_LIMIT,
						"Alpaca API rate limit exceeded. Please try again later.");
			}

			AlpacaErrors errorCode = determineErrorCode(statusCode, responseBody);
			String detailedError = buildErrorMessage(statusCode, responseBody);
			throw new StrategizException(errorCode, detailedError);

		}
		catch (Exception e) {
			String errorDetails = extractErrorDetails(e);
			log.error("Error making OAuth request to {}: {}", endpoint, errorDetails);
			throw new StrategizException(AlpacaErrors.ALPACA_API_ERROR, errorDetails);
		}
	}

	/**
	 * Get account information using OAuth token
	 */
	public Map<String, Object> getAccount(String accessToken) {
		return dataClient.getAccount(accessToken);
	}

	/**
	 * Get all positions using OAuth token
	 */
	public List<Map<String, Object>> getPositions(String accessToken) {
		return dataClient.getPositions(accessToken);
	}

	/**
	 * Get specific position by symbol
	 */
	public Map<String, Object> getPosition(String accessToken, String symbol) {
		return dataClient.getPosition(accessToken, symbol);
	}

	/**
	 * Get portfolio history
	 */
	public Map<String, Object> getPortfolioHistory(String accessToken, String period, String timeframe) {
		return dataClient.getPortfolioHistory(accessToken, period, timeframe);
	}

	/**
	 * Get account activities
	 */
	public List<Map<String, Object>> getAccountActivities(String accessToken, String activityType) {
		return dataClient.getAccountActivities(accessToken, activityType);
	}

	/**
	 * Get orders
	 */
	public List<Map<String, Object>> getOrders(String accessToken, String status, Integer limit) {
		return dataClient.getOrders(accessToken, status, limit);
	}

	/**
	 * Get assets
	 */
	public List<Map<String, Object>> getAssets(String accessToken, String status) {
		return dataClient.getAssets(accessToken, status);
	}

	/**
	 * Get specific asset
	 */
	public Map<String, Object> getAsset(String accessToken, String symbol) {
		return dataClient.getAsset(accessToken, symbol);
	}

	/**
	 * Get market clock
	 */
	public Map<String, Object> getClock(String accessToken) {
		return dataClient.getClock(accessToken);
	}

	/**
	 * Get trading calendar
	 */
	public List<Map<String, Object>> getCalendar(String accessToken, String start, String end) {
		return dataClient.getCalendar(accessToken, start, end);
	}

	// OAuth operations delegated to AlpacaOAuthClient

	/**
	 * Exchange authorization code for tokens
	 */
	public Map<String, Object> exchangeCodeForTokens(String authorizationCode) {
		return oauthClient.exchangeCodeForTokens(authorizationCode);
	}

	/**
	 * Refresh access token
	 */
	public Map<String, Object> refreshAccessToken(String refreshToken) {
		return oauthClient.refreshAccessToken(refreshToken);
	}

	/**
	 * Revoke access token
	 */
	public boolean revokeAccessToken(String accessToken) {
		return oauthClient.revokeAccessToken(accessToken);
	}

	/**
	 * Validate OAuth configuration
	 */
	public void validateConfiguration() {
		oauthClient.validateConfiguration();
	}

	// Helper methods

	/**
	 * Determine appropriate error code based on HTTP status and response body
	 */
	private AlpacaErrors determineErrorCode(int statusCode, String responseBody) {
		if (statusCode == 401) {
			return AlpacaErrors.ALPACA_TOKEN_EXPIRED;
		}
		else if (statusCode == 429) {
			return AlpacaErrors.ALPACA_RATE_LIMIT;
		}
		else if (statusCode >= 400 && statusCode < 500) {
			return AlpacaErrors.ALPACA_INVALID_RESPONSE;
		}
		else if (statusCode >= 500) {
			return AlpacaErrors.ALPACA_API_ERROR;
		}
		return AlpacaErrors.ALPACA_API_ERROR;
	}

	/**
	 * Build detailed error message from HTTP status and response
	 */
	private String buildErrorMessage(int statusCode, String responseBody) {
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append("Alpaca API error (HTTP ").append(statusCode).append(")");

		if (responseBody != null && !responseBody.isEmpty()) {
			try {
				// Try to extract error message from JSON response
				if (responseBody.contains("message")) {
					errorMsg.append(": ").append(responseBody);
				}
				else {
					errorMsg.append(": ").append(responseBody);
				}
			}
			catch (Exception e) {
				errorMsg.append(": ").append(responseBody);
			}
		}

		return errorMsg.toString();
	}

	/**
	 * Extract error details from exception
	 */
	private String extractErrorDetails(Exception e) {
		if (e instanceof StrategizException) {
			return e.getMessage();
		}

		String message = e.getMessage();
		if (message == null || message.isEmpty()) {
			message = e.getClass().getSimpleName();
		}

		return "Failed to communicate with Alpaca API: " + message;
	}

}
