package io.strategiz.client.coinbase;

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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Client for interacting with the Coinbase API This class handles all direct API
 * communication with Coinbase
 */
@Component
public class CoinbaseClient {

	private static final Logger log = LoggerFactory.getLogger(CoinbaseClient.class);

	private static final String COINBASE_API_URL = "https://api.coinbase.com/v2";

	private static final String HMAC_SHA256 = "HmacSHA256";

	private static final String COINBASE_API_VERSION = "2021-04-29";

	private static final int MAX_RETRIES = 3;

	private static final long INITIAL_RETRY_DELAY_MS = 1000; // 1 second

	private final RestTemplate restTemplate;

	public CoinbaseClient(@Qualifier("coinbaseRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("CoinbaseClient initialized with API URL: {}", COINBASE_API_URL);
	}

	/**
	 * Make an OAuth authenticated request to Coinbase API
	 * @param method HTTP method
	 * @param endpoint API endpoint (e.g., "/accounts")
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
				throw new StrategizException(CoinbaseErrors.INVALID_RESPONSE, "Access token is required");
			}

			// Build the URL
			URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
			if (params != null) {
				params.forEach(uriBuilder::addParameter);
			}

			URI uri = uriBuilder.build();

			// Create headers with OAuth Bearer token
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + accessToken);
			headers.set("CB-VERSION", COINBASE_API_VERSION);
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			log.debug("Making OAuth request to Coinbase API: {} {}", method, uri);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<T> response = restTemplate.exchange(uri, method, entity, responseType);

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Coinbase OAuth API request error - HTTP Status {}: {}", statusCode, responseBody);

			// Handle token expiration
			if (statusCode == 401) {
				throw new StrategizException(CoinbaseErrors.AUTHENTICATION_ERROR,
						"Access token expired or invalid. Please reconnect your Coinbase account.");
			}

			CoinbaseErrors errorCode = determineErrorCode(statusCode, responseBody);
			String detailedError = buildErrorMessage(statusCode, responseBody);
			throw new StrategizException(errorCode, detailedError);

		}
		catch (Exception e) {
			String errorDetails = extractErrorDetails(e);
			log.error("Error making OAuth request to {}: {}", endpoint, errorDetails);
			throw new StrategizException(CoinbaseErrors.API_ERROR, errorDetails);
		}
	}

	/**
	 * Get user information using OAuth token
	 */
	public Map<String, Object> getCurrentUser(String accessToken) {
		return oauthRequest(HttpMethod.GET, "/user", accessToken, null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	/**
	 * Get all accounts using OAuth token with pagination support Fetches all accounts by
	 * following pagination links
	 */
	public Map<String, Object> getAccounts(String accessToken) {
		Map<String, String> params = new HashMap<>();
		params.put("limit", "100"); // Request maximum allowed per page

		Map<String, Object> allAccountsResponse = new HashMap<>();
		java.util.List<Map<String, Object>> allAccounts = new java.util.ArrayList<>();

		String nextUri = null;
		int pageCount = 0;
		final int MAX_PAGES = 10; // Safety limit to prevent infinite loops

		do {
			// If we have a next_uri from pagination, use starting_after parameter
			if (nextUri != null && nextUri.contains("starting_after=")) {
				String startingAfter = nextUri.substring(nextUri.indexOf("starting_after=") + 15);
				if (startingAfter.contains("&")) {
					startingAfter = startingAfter.substring(0, startingAfter.indexOf("&"));
				}
				params.put("starting_after", startingAfter);
			}

			Map<String, Object> response = oauthRequest(HttpMethod.GET, "/accounts", accessToken, params,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			// Extract accounts from this page
			if (response != null && response.containsKey("data")) {
				java.util.List<Map<String, Object>> pageAccounts = (java.util.List<Map<String, Object>>) response
					.get("data");
				if (pageAccounts != null) {
					allAccounts.addAll(pageAccounts);
					log.debug("Fetched page {} with {} accounts (total: {})", pageCount + 1, pageAccounts.size(),
							allAccounts.size());
				}
			}

			// Check for next page
			nextUri = null;
			if (response != null && response.containsKey("pagination")) {
				Map<String, Object> pagination = (Map<String, Object>) response.get("pagination");
				if (pagination != null && pagination.containsKey("next_uri")) {
					nextUri = (String) pagination.get("next_uri");
				}
			}

			pageCount++;

		}
		while (nextUri != null && pageCount < MAX_PAGES);

		log.info("Fetched total of {} Coinbase accounts across {} pages", allAccounts.size(), pageCount);

		// Build final response
		allAccountsResponse.put("data", allAccounts);
		allAccountsResponse.put("pagination", Collections.singletonMap("total_count", allAccounts.size()));

		return allAccountsResponse;
	}

	/**
	 * Get specific account using OAuth token
	 */
	public Map<String, Object> getAccount(String accessToken, String accountId) {
		return oauthRequest(HttpMethod.GET, "/accounts/" + accountId, accessToken, null,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	/**
	 * Get account transactions using OAuth token
	 */
	public Map<String, Object> getTransactions(String accessToken, String accountId, Map<String, String> params) {
		return oauthRequest(HttpMethod.GET, "/accounts/" + accountId + "/transactions", accessToken, params,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	/**
	 * Make a public request to Coinbase API (no authentication required)
	 * @param method HTTP method
	 * @param endpoint API endpoint (e.g., "/currencies")
	 * @param params Request parameters
	 * @param responseType Expected response type
	 * @return API response
	 */
	public <T> T publicRequest(HttpMethod method, String endpoint, Map<String, String> params,
			ParameterizedTypeReference<T> responseType) {
		try {
			URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);

			if (params != null) {
				params.forEach(uriBuilder::addParameter);
			}

			URI uri = uriBuilder.build();

			log.debug("Making public request to Coinbase API: {} {}", method, uri);

			ResponseEntity<T> response = restTemplate.exchange(uri, method, null, responseType);

			return response.getBody();
		}
		catch (RestClientResponseException e) {
			// Handle Spring's RestClientResponseException which contains HTTP status and
			// response body
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Coinbase API public request error - HTTP Status {}: {}", statusCode, responseBody);

			// Determine appropriate error code based on HTTP status
			CoinbaseErrors errorCode = determineErrorCode(statusCode, responseBody);

			// Build a detailed error message with user-friendly text
			String detailedError = buildErrorMessage(statusCode, responseBody);
			throw new StrategizException(errorCode, detailedError);
		}
		catch (Exception e) {
			String errorDetails = extractErrorDetails(e);
			log.error("Error making public request to {}: {}", endpoint, errorDetails);
			throw new StrategizException(CoinbaseErrors.API_ERROR, errorDetails);
		}
	}

	/**
	 * Make a signed request to Coinbase API (requires authentication)
	 * @param method HTTP method
	 * @param endpoint API endpoint (e.g., "/accounts")
	 * @param params Request parameters
	 * @param apiKey Coinbase API key
	 * @param privateKey Coinbase API private key
	 * @param responseType Expected response type
	 * @return API response
	 */
	public <T> T signedRequest(HttpMethod method, String endpoint, Map<String, String> params, String apiKey,
			String privateKey, ParameterizedTypeReference<T> responseType) {
		try {
			// Validate inputs
			if (apiKey == null || apiKey.trim().isEmpty()) {
				throw new StrategizException(CoinbaseErrors.INVALID_RESPONSE, "API key validation error");
			}
			if (privateKey == null || privateKey.trim().isEmpty()) {
				throw new StrategizException(CoinbaseErrors.INVALID_RESPONSE, "Private key validation error");
			}

			// Format the private key properly before using it
			privateKey = formatPrivateKey(privateKey);

			log.info("Beginning Coinbase API request preparation with key starting with: {}",
					apiKey.substring(0, Math.min(apiKey.length(), 4)) + "...");

			// Build the URL
			URIBuilder uriBuilder = new URIBuilder(COINBASE_API_URL + endpoint);
			if (params != null) {
				for (Map.Entry<String, String> param : params.entrySet()) {
					uriBuilder.addParameter(param.getKey(), param.getValue());
				}
			}

			URI uri = uriBuilder.build();

			// Get current timestamp for signature
			long timestamp = System.currentTimeMillis() / 1000;

			// Create message to sign
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append(timestamp).append(method.name()).append(endpoint);

			if (params != null && !params.isEmpty()) {
				String queryParams = uri.getQuery();
				if (queryParams != null && !queryParams.isEmpty()) {
					messageBuilder.append("?").append(queryParams);
				}
			}

			String message = messageBuilder.toString();
			log.info("Preparing to sign message: '{}'", message);

			// Generate signature
			String signature = generateSignature(message, privateKey);
			log.info("Generated signature (first 10 chars): '{}'",
					signature.length() > 10 ? signature.substring(0, 10) + "..." : signature);

			// Set up headers
			HttpHeaders headers = new HttpHeaders();
			headers.set("CB-ACCESS-KEY", apiKey);
			headers.set("CB-ACCESS-SIGN", signature);
			headers.set("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp));
			headers.set("CB-VERSION", COINBASE_API_VERSION);
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			// Make request
			ResponseEntity<T> response = restTemplate.exchange(uri, method, new HttpEntity<>(headers), responseType);

			return response.getBody();
		}
		catch (RestClientResponseException e) {
			// Handle Spring's RestClientResponseException which contains HTTP status and
			// response body
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("Coinbase API error - HTTP Status {}: {}", statusCode, responseBody);

			// Determine appropriate error code based on HTTP status
			CoinbaseErrors errorCode = determineErrorCode(statusCode, responseBody);

			// Build a detailed error message with user-friendly text
			String detailedError = buildErrorMessage(statusCode, responseBody);
			throw new StrategizException(errorCode, detailedError);
		}
		catch (Exception e) {
			log.error("Error making signed request to Coinbase API: {}", e.getMessage(), e);
			throw new StrategizException(CoinbaseErrors.API_ERROR, e.getMessage());
		}
	}

	/**
	 * Generate HMAC-SHA256 signature for Coinbase API requests
	 * @param message Message to sign
	 * @param privateKey Private key to sign with
	 * @return Base64-encoded signature
	 * @throws NoSuchAlgorithmException if the algorithm is not available
	 * @throws InvalidKeyException if the key is invalid
	 */
	private String generateSignature(String message, String privateKey)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
		SecretKeySpec secretKeySpec = new SecretKeySpec(privateKey.getBytes(), HMAC_SHA256);
		hmacSha256.init(secretKeySpec);
		byte[] signature = hmacSha256.doFinal(message.getBytes());
		return Base64.getEncoder().encodeToString(signature);
	}

	/**
	 * Format private key for Coinbase API
	 * @param privateKey Private key to format
	 * @return Formatted private key
	 */
	private String formatPrivateKey(String privateKey) {
		// Remove any whitespace, newlines, etc.
		privateKey = privateKey.trim();

		// Handle PEM format if needed
		if (privateKey.startsWith("-----BEGIN PRIVATE KEY-----")) {
			privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s+", "");
		}

		return privateKey;
	}

	/**
	 * Extract error details from an exception
	 * @param e Exception to extract details from
	 * @return Error details as a string
	 */
	private String extractErrorDetails(Exception e) {
		if (e instanceof HttpStatusCodeException) {
			return ((HttpStatusCodeException) e).getResponseBodyAsString();
		}
		else if (e instanceof ResourceAccessException) {
			return "Connection error: " + e.getMessage();
		}
		else {
			return e.getMessage();
		}
	}

	/**
	 * Determine the appropriate CoinbaseErrors enum based on HTTP status and response
	 * @param statusCode HTTP status code
	 * @param responseBody Response body from API
	 * @return Appropriate CoinbaseErrors enum
	 */
	private CoinbaseErrors determineErrorCode(int statusCode, String responseBody) {
		switch (statusCode) {
			case 400:
				if (responseBody != null && responseBody.toLowerCase().contains("invalid_request")) {
					return CoinbaseErrors.INVALID_CREDENTIALS;
				}
				return CoinbaseErrors.INVALID_RESPONSE;
			case 401:
			case 403:
				return CoinbaseErrors.API_AUTHENTICATION_FAILED;
			case 429:
				return CoinbaseErrors.API_RATE_LIMITED;
			case 500:
			case 502:
			case 503:
			case 504:
				return CoinbaseErrors.SERVICE_UNAVAILABLE;
			case 404:
				return CoinbaseErrors.ACCOUNT_NOT_FOUND;
			default:
				return CoinbaseErrors.API_ERROR;
		}
	}

	/**
	 * Build a user-friendly error message based on HTTP status and response
	 * @param statusCode HTTP status code
	 * @param responseBody Response body from API
	 * @return User-friendly error message
	 */
	private String buildErrorMessage(int statusCode, String responseBody) {
		switch (statusCode) {
			case 400:
				return "Bad request to Coinbase API. Please check your request parameters.";
			case 401:
				return "Coinbase API authentication failed. Please check your credentials.";
			case 403:
				return "Access forbidden. Your Coinbase API key may not have sufficient permissions.";
			case 404:
				return "Requested Coinbase resource not found.";
			case 429:
				return "Coinbase API rate limit exceeded. Please try again later.";
			case 500:
				if (responseBody != null && responseBody.contains("Coinbase is temporarily unavailable")) {
					return "Coinbase is temporarily unavailable. Their servers are busy and they expect normal service to return soon. Your funds are safe.";
				}
				return "Coinbase API server error. Please try again in a few minutes.";
			case 502:
			case 503:
			case 504:
				return "Coinbase API is temporarily unavailable. Please try again in a few minutes.";
			default:
				return String.format("Coinbase API returned HTTP %d: %s", statusCode,
						responseBody != null ? responseBody : "Unknown error");
		}
	}

	/**
	 * Execute a request with retry logic for temporary failures
	 * @param operation Operation to execute
	 * @param operationName Name of the operation for logging
	 * @return Result from operation
	 */
	private <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName) {
		int attempt = 0;
		Exception lastException = null;

		while (attempt < MAX_RETRIES) {
			try {
				return operation.get();
			}
			catch (StrategizException e) {
				lastException = e;

				// Only retry for service unavailable errors
				if (CoinbaseErrors.SERVICE_UNAVAILABLE.name().equals(e.getErrorCode()) && attempt < MAX_RETRIES - 1) {
					long delayMs = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
					log.warn("Coinbase API temporarily unavailable, retrying {} in {} ms (attempt {}/{})",
							operationName, delayMs, attempt + 1, MAX_RETRIES);

					try {
						Thread.sleep(delayMs);
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new StrategizException(CoinbaseErrors.API_ERROR, "Request interrupted during retry");
					}

					attempt++;
				}
				else {
					// Not retryable or max retries reached
					throw e;
				}
			}
			catch (Exception e) {
				lastException = e;
				throw e;
			}
		}

		// This should not be reached, but just in case
		if (lastException instanceof StrategizException) {
			throw (StrategizException) lastException;
		}
		else {
			throw new StrategizException(CoinbaseErrors.API_ERROR,
					"Max retries exceeded: " + lastException.getMessage());
		}
	}

	/**
	 * Test connection to Coinbase API
	 * @param apiKey API key
	 * @param privateKey Private key
	 * @return true if successful, false otherwise
	 */
	public boolean testConnection(String apiKey, String privateKey) {
		try {
			return executeWithRetry(() -> {
				// Make a simple request to test the connection
				Map<String, String> params = new HashMap<>();
				ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<Map<String, Object>>() {
				};

				Map<String, Object> response = signedRequest(HttpMethod.GET, "/user", params, apiKey, privateKey,
						responseType);

				return response != null && response.containsKey("data");
			}, "connection test");
		}
		catch (Exception e) {
			log.error("Error testing Coinbase API connection", e);
			return false;
		}
	}

}
