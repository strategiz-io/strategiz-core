package io.strategiz.client.etrade.client;

import io.strategiz.client.etrade.auth.OAuth1aSignature;
import io.strategiz.client.etrade.error.EtradeErrors;
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
import java.util.*;

/**
 * Client for fetching data from E*TRADE using OAuth 1.0a access tokens.
 * This class handles all account, position, and market data retrieval operations.
 *
 * API Documentation: https://apisb.etrade.com/docs/api/account/api-account-v1.html
 *
 * Key endpoints:
 * - /v1/accounts/list - List all accounts
 * - /v1/accounts/{accountIdKey}/balance - Account balance
 * - /v1/accounts/{accountIdKey}/portfolio - Account holdings/positions
 * - /v1/accounts/{accountIdKey}/transactions - Transaction history
 * - /v1/market/quote/{symbols} - Stock quotes
 */
@Component
public class EtradeDataClient {

	private static final Logger log = LoggerFactory.getLogger(EtradeDataClient.class);

	private final RestTemplate restTemplate;

	@Value("${oauth.providers.etrade.api-url:https://api.etrade.com}")
	private String apiUrl;

	@Value("${oauth.providers.etrade.consumer-key:}")
	private String consumerKey;

	@Value("${oauth.providers.etrade.consumer-secret:}")
	private String consumerSecret;

	public EtradeDataClient(@Qualifier("etradeRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("EtradeDataClient initialized");
	}

	/**
	 * Get all user accounts.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @return List of accounts
	 */
	public List<Map<String, Object>> getAccounts(String accessToken, String accessTokenSecret) {
		Object response = makeAuthenticatedRequest(HttpMethod.GET, "/v1/accounts/list", accessToken, accessTokenSecret,
				null);

		return extractAccountsList(response);
	}

	/**
	 * Get account balance information.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey The account ID key
	 * @return Account balance information
	 */
	public Map<String, Object> getAccountBalance(String accessToken, String accessTokenSecret, String accountIdKey) {
		String endpoint = "/v1/accounts/" + accountIdKey + "/balance";
		Map<String, String> params = new HashMap<>();
		params.put("instType", "BROKERAGE");
		params.put("realTimeNAV", "true");

		Object response = makeAuthenticatedRequest(HttpMethod.GET, endpoint, accessToken, accessTokenSecret, params);

		return extractMapResponse(response);
	}

	/**
	 * Get portfolio (holdings/positions) for an account.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey The account ID key
	 * @return Portfolio data with positions
	 */
	public Map<String, Object> getPortfolio(String accessToken, String accessTokenSecret, String accountIdKey) {
		String endpoint = "/v1/accounts/" + accountIdKey + "/portfolio";

		Object response = makeAuthenticatedRequest(HttpMethod.GET, endpoint, accessToken, accessTokenSecret, null);

		return extractMapResponse(response);
	}

	/**
	 * Get transactions for an account.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param accountIdKey The account ID key
	 * @param startDate Start date (MMDDYYYY format)
	 * @param endDate End date (MMDDYYYY format)
	 * @return List of transactions
	 */
	public List<Map<String, Object>> getTransactions(String accessToken, String accessTokenSecret, String accountIdKey,
			String startDate, String endDate) {
		String endpoint = "/v1/accounts/" + accountIdKey + "/transactions";

		Map<String, String> params = new HashMap<>();
		if (startDate != null) {
			params.put("startDate", startDate);
		}
		if (endDate != null) {
			params.put("endDate", endDate);
		}

		Object response = makeAuthenticatedRequest(HttpMethod.GET, endpoint, accessToken, accessTokenSecret, params);

		return extractTransactionsList(response);
	}

	/**
	 * Get real-time quotes for symbols.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbols List of stock symbols
	 * @return Quote data
	 */
	public Map<String, Object> getQuotes(String accessToken, String accessTokenSecret, List<String> symbols) {
		String symbolsParam = String.join(",", symbols);
		String endpoint = "/v1/market/quote/" + symbolsParam;

		Object response = makeAuthenticatedRequest(HttpMethod.GET, endpoint, accessToken, accessTokenSecret, null);

		return extractMapResponse(response);
	}

	/**
	 * Get quote for a single symbol.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbol The stock symbol
	 * @return Quote data for the symbol
	 */
	public Map<String, Object> getQuote(String accessToken, String accessTokenSecret, String symbol) {
		Map<String, Object> quotes = getQuotes(accessToken, accessTokenSecret, Collections.singletonList(symbol));

		// Extract quote from response
		if (quotes != null && quotes.containsKey("QuoteResponse")) {
			Map<String, Object> quoteResponse = (Map<String, Object>) quotes.get("QuoteResponse");
			if (quoteResponse.containsKey("QuoteData")) {
				Object quoteData = quoteResponse.get("QuoteData");
				if (quoteData instanceof List) {
					List<Map<String, Object>> quoteList = (List<Map<String, Object>>) quoteData;
					if (!quoteList.isEmpty()) {
						return quoteList.get(0);
					}
				}
				else if (quoteData instanceof Map) {
					return (Map<String, Object>) quoteData;
				}
			}
		}

		return Collections.emptyMap();
	}

	/**
	 * Get the last price for a symbol.
	 * @param accessToken OAuth access token
	 * @param accessTokenSecret OAuth access token secret
	 * @param symbol The stock symbol
	 * @return Last price or null if not available
	 */
	public Double getLastPrice(String accessToken, String accessTokenSecret, String symbol) {
		try {
			Map<String, Object> quote = getQuote(accessToken, accessTokenSecret, symbol);

			if (quote != null && quote.containsKey("All")) {
				Map<String, Object> allData = (Map<String, Object>) quote.get("All");
				if (allData.containsKey("lastTrade")) {
					Object lastTrade = allData.get("lastTrade");
					if (lastTrade instanceof Number) {
						return ((Number) lastTrade).doubleValue();
					}
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to get last price for {}: {}", symbol, e.getMessage());
		}
		return null;
	}

	/**
	 * Test connection to E*TRADE API using access token.
	 * @param accessToken The access token
	 * @param accessTokenSecret The access token secret
	 * @return true if connection is valid
	 */
	public boolean testConnection(String accessToken, String accessTokenSecret) {
		try {
			List<Map<String, Object>> accounts = getAccounts(accessToken, accessTokenSecret);
			return accounts != null && !accounts.isEmpty();
		}
		catch (Exception e) {
			log.debug("E*TRADE connection test failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Make an authenticated request to E*TRADE API with OAuth 1.0a signing.
	 */
	private Object makeAuthenticatedRequest(HttpMethod method, String endpoint, String accessToken,
			String accessTokenSecret, Map<String, String> queryParams) {
		try {
			// Validate tokens
			if (accessToken == null || accessToken.trim().isEmpty()) {
				throw new StrategizException(EtradeErrors.ETRADE_INVALID_TOKEN, "Access token is required");
			}
			if (accessTokenSecret == null || accessTokenSecret.trim().isEmpty()) {
				throw new StrategizException(EtradeErrors.ETRADE_INVALID_TOKEN, "Access token secret is required");
			}

			// Build the URL
			String baseUrl = apiUrl + endpoint;
			URIBuilder uriBuilder = new URIBuilder(baseUrl);
			if (queryParams != null) {
				queryParams.forEach(uriBuilder::addParameter);
			}
			URI uri = uriBuilder.build();

			// Build OAuth parameters
			Map<String, String> oauthParams = OAuth1aSignature.createBaseOAuthParams(consumerKey, accessToken);

			// Generate signature - include query params in signature calculation
			String signature = OAuth1aSignature.sign(method.name(), baseUrl, oauthParams, queryParams, consumerSecret,
					accessTokenSecret);

			oauthParams.put("oauth_signature", signature);

			// Build Authorization header
			String authHeader = OAuth1aSignature.buildAuthorizationHeader(oauthParams);

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authHeader);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			log.info("Making authenticated request to E*TRADE API: {} {}", method, uri);

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Object> response = restTemplate.exchange(uri, method, entity, Object.class);

			if (response.getBody() == null) {
				throw new StrategizException(EtradeErrors.ETRADE_INVALID_RESPONSE, "Empty response from E*TRADE API");
			}

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			String responseBody = e.getResponseBodyAsString();

			log.error("E*TRADE API request error - HTTP Status {}", statusCode);
			log.error("Response body: {}", responseBody != null && !responseBody.isEmpty() ? responseBody : "(empty)");

			// Handle token expiration (2 hour inactivity)
			if (statusCode == 401) {
				throw new StrategizException(EtradeErrors.ETRADE_TOKEN_EXPIRED,
						"Access token expired. Please reconnect your E*TRADE account.");
			}

			// Handle rate limiting
			if (statusCode == 429) {
				throw new StrategizException(EtradeErrors.ETRADE_RATE_LIMIT,
						"E*TRADE API rate limit exceeded. Please try again later.");
			}

			throw new StrategizException(EtradeErrors.ETRADE_API_ERROR, "E*TRADE API error: "
					+ (responseBody != null && !responseBody.isEmpty() ? responseBody : "HTTP " + statusCode));

		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error making authenticated request to {}: {}", endpoint, e.getMessage());
			throw new StrategizException(EtradeErrors.ETRADE_NETWORK_ERROR,
					"Failed to communicate with E*TRADE: " + e.getMessage());
		}
	}

	/**
	 * Extract accounts list from API response.
	 */
	private List<Map<String, Object>> extractAccountsList(Object response) {
		if (response instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) response;

			// E*TRADE returns: { "AccountListResponse": { "Accounts": { "Account": [...] }
			// } }
			if (map.containsKey("AccountListResponse")) {
				Map<String, Object> accountListResponse = (Map<String, Object>) map.get("AccountListResponse");
				if (accountListResponse.containsKey("Accounts")) {
					Map<String, Object> accounts = (Map<String, Object>) accountListResponse.get("Accounts");
					if (accounts.containsKey("Account")) {
						Object accountList = accounts.get("Account");
						if (accountList instanceof List) {
							return (List<Map<String, Object>>) accountList;
						}
						else if (accountList instanceof Map) {
							return Collections.singletonList((Map<String, Object>) accountList);
						}
					}
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Extract transactions list from API response.
	 */
	private List<Map<String, Object>> extractTransactionsList(Object response) {
		if (response instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) response;

			// E*TRADE returns: { "TransactionListResponse": { "Transaction": [...] } }
			if (map.containsKey("TransactionListResponse")) {
				Map<String, Object> txResponse = (Map<String, Object>) map.get("TransactionListResponse");
				if (txResponse.containsKey("Transaction")) {
					Object txList = txResponse.get("Transaction");
					if (txList instanceof List) {
						return (List<Map<String, Object>>) txList;
					}
					else if (txList instanceof Map) {
						return Collections.singletonList((Map<String, Object>) txList);
					}
				}
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Extract Map response.
	 */
	private Map<String, Object> extractMapResponse(Object response) {
		if (response instanceof Map) {
			return (Map<String, Object>) response;
		}
		return Collections.emptyMap();
	}

}
