package io.strategiz.client.schwab.client;

import io.strategiz.client.schwab.error.SchwabErrors;
import io.strategiz.framework.exception.StrategizException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Client for fetching data from Charles Schwab using OAuth access tokens. This class
 * handles all account, position, and transaction data retrieval operations.
 *
 * API Documentation: https://developer.schwab.com/products/trader-api--individual
 *
 * Key endpoints: - /trader/v1/accounts - List all accounts -
 * /trader/v1/accounts?fields=positions - List accounts with positions -
 * /trader/v1/accounts/{accountHash}/transactions - Get transaction history -
 * /marketdata/v1/quotes - Get real-time quotes
 */
@Component
public class SchwabDataClient {

	private static final Logger log = LoggerFactory.getLogger(SchwabDataClient.class);

	private static final String TRADER_API_BASE = "/trader/v1";

	private static final String MARKET_DATA_BASE = "/marketdata/v1";

	private final RestTemplate restTemplate;

	@Value("${oauth.providers.schwab.api-url:https://api.schwabapi.com}")
	private String apiUrl;

	public SchwabDataClient(@Qualifier("schwabRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("SchwabDataClient initialized");
	}

	/**
	 * Get all user accounts with basic information.
	 * @param accessToken OAuth access token
	 * @return List of accounts
	 */
	public List<Map<String, Object>> getAccounts(String accessToken) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET, TRADER_API_BASE + "/accounts", accessToken, null);
		return extractAccountsList(response);
	}

	/**
	 * Get all accounts with positions included. This is the primary method for fetching
	 * portfolio data.
	 * @param accessToken OAuth access token
	 * @return List of accounts with positions
	 */
	public List<Map<String, Object>> getAccountsWithPositions(String accessToken) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET, TRADER_API_BASE + "/accounts?fields=positions",
				accessToken, null);
		return extractAccountsList(response);
	}

	/**
	 * Get specific account with positions.
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash (not account number)
	 * @return Account details with positions
	 */
	public Map<String, Object> getAccountWithPositions(String accessToken, String accountHash) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET,
				TRADER_API_BASE + "/accounts/" + accountHash + "?fields=positions", accessToken, null);
		return extractSingleAccount(response);
	}

	/**
	 * Get account balance information.
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash/ID
	 * @return Account balance information
	 */
	public Map<String, Object> getAccountBalance(String accessToken, String accountHash) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET, TRADER_API_BASE + "/accounts/" + accountHash,
				accessToken, null);
		return extractSingleAccount(response);
	}

	/**
	 * Get transactions for an account. Note: Schwab only allows fetching up to 1 year of
	 * transaction history.
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash/ID
	 * @param startDate Start date for filtering (optional, defaults to 1 year ago)
	 * @param endDate End date for filtering (optional, defaults to today)
	 * @param types Transaction types to filter (optional, e.g., "TRADE",
	 * "DIVIDEND_OR_INTEREST")
	 * @return List of transactions
	 */
	public List<Map<String, Object>> getTransactions(String accessToken, String accountHash, LocalDate startDate,
			LocalDate endDate, String types) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

			// Default to 1 year of history if not specified
			if (startDate == null) {
				startDate = LocalDate.now().minusYears(1);
			}
			if (endDate == null) {
				endDate = LocalDate.now();
			}

			Map<String, String> params = new LinkedHashMap<>();
			params.put("startDate", startDate.format(formatter));
			params.put("endDate", endDate.format(formatter));

			if (types != null && !types.isEmpty()) {
				params.put("types", types);
			}

			Object response = makeAuthenticatedRequest(HttpMethod.GET,
					TRADER_API_BASE + "/accounts/" + accountHash + "/transactions", accessToken, params);

			if (response instanceof List) {
				return (List<Map<String, Object>>) response;
			}
			return Collections.emptyList();

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error fetching Schwab transactions: {}", e.getMessage());
			throw new StrategizException(SchwabErrors.SCHWAB_TRANSACTION_ERROR,
					"Failed to fetch transactions: " + e.getMessage());
		}
	}

	/**
	 * Get trade transactions (buy/sell) for an account.
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash/ID
	 * @return List of trade transactions
	 */
	public List<Map<String, Object>> getTradeTransactions(String accessToken, String accountHash) {
		return getTransactions(accessToken, accountHash, null, null, "TRADE");
	}

	/**
	 * Get all transactions for an account (no type filter).
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash/ID
	 * @return List of all transactions
	 */
	public List<Map<String, Object>> getAllTransactions(String accessToken, String accountHash) {
		return getTransactions(accessToken, accountHash, null, null, null);
	}

	/**
	 * Get a specific transaction by ID.
	 * @param accessToken OAuth access token
	 * @param accountHash The account hash/ID
	 * @param transactionId The transaction ID
	 * @return Transaction details
	 */
	public Map<String, Object> getTransaction(String accessToken, String accountHash, String transactionId) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET,
				TRADER_API_BASE + "/accounts/" + accountHash + "/transactions/" + transactionId, accessToken, null);
		return extractSingleAccount(response);
	}

	/**
	 * Get real-time quote for a symbol.
	 * @param accessToken OAuth access token
	 * @param symbol The stock symbol (e.g., "AAPL")
	 * @return Quote data including lastPrice, openPrice, highPrice, lowPrice
	 */
	public Map<String, Object> getQuote(String accessToken, String symbol) {
		Map<String, String> params = Map.of("symbols", symbol);
		Object response = makeAuthenticatedRequest(HttpMethod.GET, MARKET_DATA_BASE + "/quotes", accessToken, params);

		// Response structure: { "AAPL": { quote data } }
		if (response instanceof Map) {
			Map<String, Object> quotes = (Map<String, Object>) response;
			if (quotes.containsKey(symbol)) {
				Object quote = quotes.get(symbol);
				if (quote instanceof Map) {
					return (Map<String, Object>) quote;
				}
			}
		}
		return Collections.emptyMap();
	}

	/**
	 * Get real-time quotes for multiple symbols.
	 * @param accessToken OAuth access token
	 * @param symbols List of stock symbols
	 * @return Map of symbol to quote data
	 */
	public Map<String, Object> getQuotes(String accessToken, List<String> symbols) {
		String symbolsParam = String.join(",", symbols);
		Map<String, String> params = Map.of("symbols", symbolsParam);

		Object response = makeAuthenticatedRequest(HttpMethod.GET, MARKET_DATA_BASE + "/quotes", accessToken, params);

		if (response instanceof Map) {
			return (Map<String, Object>) response;
		}
		return Collections.emptyMap();
	}

	/**
	 * Get the last price for a symbol. Convenience method that extracts just the
	 * lastPrice from quote data.
	 * @param accessToken OAuth access token
	 * @param symbol The stock symbol
	 * @return Last price as Double, or null if not available
	 */
	public Double getLastPrice(String accessToken, String symbol) {
		try {
			Map<String, Object> quote = getQuote(accessToken, symbol);
			if (quote != null && quote.containsKey("quote")) {
				Map<String, Object> quoteData = (Map<String, Object>) quote.get("quote");
				if (quoteData.containsKey("lastPrice")) {
					Object lastPrice = quoteData.get("lastPrice");
					if (lastPrice instanceof Number) {
						return ((Number) lastPrice).doubleValue();
					}
				}
			}
			// Try direct access if nested structure differs
			if (quote != null && quote.containsKey("lastPrice")) {
				Object lastPrice = quote.get("lastPrice");
				if (lastPrice instanceof Number) {
					return ((Number) lastPrice).doubleValue();
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to get last price for {}: {}", symbol, e.getMessage());
		}
		return null;
	}

	/**
	 * Make an authenticated request to Schwab API.
	 */
	private Object makeAuthenticatedRequest(HttpMethod method, String endpoint, String accessToken,
			Map<String, String> params) {
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
			// Only set Content-Type for requests with body (POST, PUT, PATCH)
			if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
				headers.setContentType(MediaType.APPLICATION_JSON);
			}
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			log.info("Making authenticated request to Schwab API: {} {}", method, uri);
			log.debug("Authorization header: Bearer {}...",
					accessToken.substring(0, Math.min(20, accessToken.length())));

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Object> response = restTemplate.exchange(uri, method, entity, Object.class);

			if (response.getBody() == null) {
				throw new StrategizException(SchwabErrors.SCHWAB_INVALID_RESPONSE, "Empty response from Schwab API");
			}

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();
			HttpHeaders responseHeaders = e.getResponseHeaders();

			log.error("Schwab API request error - HTTP Status {}", statusCode);
			log.error("Response body: {}", responseBody != null && !responseBody.isEmpty() ? responseBody : "(empty)");
			log.error("Response headers: {}", responseHeaders);

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

			// For 400 errors, provide more context
			if (statusCode == 400) {
				String errorMsg = responseBody != null && !responseBody.isEmpty() ? responseBody
						: "Bad request - check API parameters and authentication";
				throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR, "Schwab API error (400): " + errorMsg);
			}

			throw new StrategizException(SchwabErrors.SCHWAB_API_ERROR, "Schwab API error: "
					+ (responseBody != null && !responseBody.isEmpty() ? responseBody : "HTTP " + statusCode));

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error making authenticated request to {}: {}", endpoint, e.getMessage());
			throw new StrategizException(SchwabErrors.SCHWAB_NETWORK_ERROR,
					"Failed to communicate with Schwab: " + e.getMessage());
		}
	}

	/**
	 * Extract accounts list from API response. Schwab returns accounts as a list directly
	 * or wrapped in an object.
	 */
	private List<Map<String, Object>> extractAccountsList(Object response) {
		if (response instanceof List) {
			return (List<Map<String, Object>>) response;
		}
		else if (response instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) response;
			// Check if accounts are nested under a key
			if (map.containsKey("accounts")) {
				Object accounts = map.get("accounts");
				if (accounts instanceof List) {
					return (List<Map<String, Object>>) accounts;
				}
			}
			// Single account, wrap in list
			return Collections.singletonList(map);
		}
		return Collections.emptyList();
	}

	/**
	 * Extract single account/object from API response.
	 */
	private Map<String, Object> extractSingleAccount(Object response) {
		if (response instanceof Map) {
			return (Map<String, Object>) response;
		}
		return Collections.emptyMap();
	}

	/**
	 * Test connection to Schwab API using access token.
	 * @param accessToken The access token to test
	 * @return true if connection is valid
	 */
	public boolean testConnection(String accessToken) {
		try {
			List<Map<String, Object>> accounts = getAccounts(accessToken);
			return accounts != null && !accounts.isEmpty();
		}
		catch (Exception e) {
			log.debug("Schwab connection test failed: {}", e.getMessage());
			return false;
		}
	}

}
