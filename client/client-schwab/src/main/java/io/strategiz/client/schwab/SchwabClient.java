package io.strategiz.client.schwab;

import io.strategiz.client.schwab.auth.SchwabOAuthClient;
import io.strategiz.client.schwab.client.SchwabDataClient;
import io.strategiz.client.schwab.error.SchwabErrors;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Main client for interacting with the Charles Schwab API. This class coordinates OAuth
 * operations and data retrieval.
 *
 * API Documentation: https://developer.schwab.com/products/trader-api--individual
 *
 * Key features: - OAuth 2.0 authentication with Basic Auth for token requests - Access
 * tokens expire after 30 minutes - Refresh tokens expire after 7 days - Supports account,
 * position, and transaction data retrieval - Supports real-time market data quotes
 */
@Component
public class SchwabClient {

	private static final Logger log = LoggerFactory.getLogger(SchwabClient.class);

	private final RestTemplate restTemplate;

	private final SchwabOAuthClient oauthClient;

	private final SchwabDataClient dataClient;

	@Value("${oauth.providers.schwab.api-url:https://api.schwabapi.com}")
	private String apiUrl;

	public SchwabClient(@Qualifier("schwabRestTemplate") RestTemplate restTemplate, SchwabOAuthClient oauthClient,
			SchwabDataClient dataClient) {
		this.restTemplate = restTemplate;
		this.oauthClient = oauthClient;
		this.dataClient = dataClient;
		log.info("SchwabClient initialized");
	}

	/**
	 * Make an OAuth authenticated request to Schwab API.
	 * @param method HTTP method
	 * @param endpoint API endpoint (e.g., "/trader/v1/accounts")
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
				throw new StrategizException(SchwabErrors.SCHWAB_INVALID_TOKEN, "Access token is required");
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

			log.debug("Making OAuth request to Schwab API: {} {}", method, uri);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Schwab OAuth API request error - HTTP Status {}: {}", statusCode, responseBody);

			// Handle token expiration
			if (statusCode == 401) {
				throw new StrategizException(SchwabErrors.SCHWAB_TOKEN_EXPIRED,
						"Access token expired or invalid. Please reconnect your Schwab account.");
			}

			// Handle rate limiting
			if (statusCode == 429) {
				throw new StrategizException(SchwabErrors.SCHWAB_RATE_LIMIT,
						"Schwab API rate limit exceeded. Please try again later.");
			}

			SchwabErrors errorCode = determineErrorCode(statusCode, responseBody);
			String detailedError = buildErrorMessage(statusCode, responseBody);
			throw new StrategizException(errorCode, detailedError);

		}
		catch (Exception e) {
			String errorDetails = extractErrorDetails(e);
			log.error("Error making OAuth request to {}: {}", endpoint, errorDetails);
			throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR, errorDetails);
		}
	}

	// Delegated account operations

	/**
	 * Get all user accounts
	 */
	public List<Map<String, Object>> getAccounts(String accessToken) {
		return dataClient.getAccounts(accessToken);
	}

	/**
	 * Get all accounts with positions
	 */
	public List<Map<String, Object>> getAccountsWithPositions(String accessToken) {
		return dataClient.getAccountsWithPositions(accessToken);
	}

	/**
	 * Get specific account with positions
	 */
	public Map<String, Object> getAccountWithPositions(String accessToken, String accountHash) {
		return dataClient.getAccountWithPositions(accessToken, accountHash);
	}

	/**
	 * Get account balance information
	 */
	public Map<String, Object> getAccountBalance(String accessToken, String accountHash) {
		return dataClient.getAccountBalance(accessToken, accountHash);
	}

	// Delegated transaction operations

	/**
	 * Get transactions for an account
	 */
	public List<Map<String, Object>> getTransactions(String accessToken, String accountHash, LocalDate startDate,
			LocalDate endDate, String types) {
		return dataClient.getTransactions(accessToken, accountHash, startDate, endDate, types);
	}

	/**
	 * Get trade transactions (buy/sell) for an account
	 */
	public List<Map<String, Object>> getTradeTransactions(String accessToken, String accountHash) {
		return dataClient.getTradeTransactions(accessToken, accountHash);
	}

	/**
	 * Get all transactions for an account
	 */
	public List<Map<String, Object>> getAllTransactions(String accessToken, String accountHash) {
		return dataClient.getAllTransactions(accessToken, accountHash);
	}

	/**
	 * Get a specific transaction by ID
	 */
	public Map<String, Object> getTransaction(String accessToken, String accountHash, String transactionId) {
		return dataClient.getTransaction(accessToken, accountHash, transactionId);
	}

	// Delegated market data operations

	/**
	 * Get real-time quote for a symbol
	 */
	public Map<String, Object> getQuote(String accessToken, String symbol) {
		return dataClient.getQuote(accessToken, symbol);
	}

	/**
	 * Get real-time quotes for multiple symbols
	 */
	public Map<String, Object> getQuotes(String accessToken, List<String> symbols) {
		return dataClient.getQuotes(accessToken, symbols);
	}

	/**
	 * Get last price for a symbol
	 */
	public Double getLastPrice(String accessToken, String symbol) {
		return dataClient.getLastPrice(accessToken, symbol);
	}

	// Delegated OAuth operations

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
	 * Generate OAuth authorization URL
	 */
	public String generateAuthorizationUrl(String state) {
		return oauthClient.generateAuthorizationUrl(state);
	}

	/**
	 * Validate OAuth configuration
	 */
	public void validateConfiguration() {
		oauthClient.validateConfiguration();
	}

	/**
	 * Test connection to Schwab API
	 */
	public boolean testConnection(String accessToken) {
		return dataClient.testConnection(accessToken);
	}

	// Legacy methods for backward compatibility

	/**
	 * Get user account information (legacy method name)
	 * @deprecated Use getAccounts() instead
	 */
	@Deprecated
	public List<Map<String, Object>> getUserAccountInfo(String accessToken) {
		return getAccounts(accessToken);
	}

	/**
	 * Get account positions (legacy method name)
	 * @deprecated Use getAccountWithPositions() instead
	 */
	@Deprecated
	public Map<String, Object> getAccountPositions(String accessToken, String accountHash) {
		return getAccountWithPositions(accessToken, accountHash);
	}

	// Helper methods

	/**
	 * Determine appropriate error code based on HTTP status and response body
	 */
	private SchwabErrors determineErrorCode(int statusCode, String responseBody) {
		if (statusCode == 401) {
			return SchwabErrors.SCHWAB_TOKEN_EXPIRED;
		}
		else if (statusCode == 429) {
			return SchwabErrors.SCHWAB_RATE_LIMIT;
		}
		else if (statusCode >= 400 && statusCode < 500) {
			return SchwabErrors.SCHWAB_INVALID_RESPONSE;
		}
		else if (statusCode >= 500) {
			return SchwabErrors.SCHWAB_API_ERROR;
		}
		return SchwabErrors.SCHWAB_API_ERROR;
	}

	/**
	 * Build detailed error message from HTTP status and response
	 */
	private String buildErrorMessage(int statusCode, String responseBody) {
		StringBuilder errorMsg = new StringBuilder();
		errorMsg.append("Schwab API error (HTTP ").append(statusCode).append(")");

		if (responseBody != null && !responseBody.isEmpty()) {
			errorMsg.append(": ").append(responseBody);
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

		return "Failed to communicate with Schwab API: " + message;
	}

}
