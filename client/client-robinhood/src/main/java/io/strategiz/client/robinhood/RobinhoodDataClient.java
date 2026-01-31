package io.strategiz.client.robinhood;

import io.strategiz.client.robinhood.error.RobinhoodErrors;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching data from Robinhood API. Handles portfolio, positions, accounts,
 * and quote data.
 *
 * Note: This is an unofficial API integration.
 */
@Component
public class RobinhoodDataClient {

	private static final Logger log = LoggerFactory.getLogger(RobinhoodDataClient.class);

	private static final String BASE_URL = "https://api.robinhood.com";

	private static final String ACCOUNTS_URL = BASE_URL + "/accounts/";

	private static final String POSITIONS_URL = BASE_URL + "/positions/";

	private static final String PORTFOLIOS_URL = BASE_URL + "/portfolios/";

	private static final String INSTRUMENTS_URL = BASE_URL + "/instruments/";

	private static final String QUOTES_URL = BASE_URL + "/quotes/";

	private static final String USER_URL = BASE_URL + "/user/";

	// Nummus (Crypto) endpoints
	private static final String CRYPTO_HOLDINGS_URL = "https://nummus.robinhood.com/holdings/";

	private final RestTemplate restTemplate;

	public RobinhoodDataClient(@Qualifier("robinhoodRestTemplate") RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		log.info("RobinhoodDataClient initialized");
	}

	/**
	 * Get user information.
	 * @param accessToken OAuth access token
	 * @return User info map
	 */
	public Map<String, Object> getUser(String accessToken) {
		return makeAuthenticatedRequest(USER_URL, accessToken, new ParameterizedTypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Get all accounts for the authenticated user.
	 * @param accessToken OAuth access token
	 * @return List of account maps
	 */
	public List<Map<String, Object>> getAccounts(String accessToken) {
		Map<String, Object> response = makeAuthenticatedRequest(ACCOUNTS_URL, accessToken,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});

		if (response != null && response.containsKey("results")) {
			return (List<Map<String, Object>>) response.get("results");
		}
		return new ArrayList<>();
	}

	/**
	 * Get portfolio summary for an account.
	 * @param accessToken OAuth access token
	 * @param accountNumber Account number
	 * @return Portfolio summary map
	 */
	public Map<String, Object> getPortfolio(String accessToken, String accountNumber) {
		String url = PORTFOLIOS_URL + accountNumber + "/";
		return makeAuthenticatedRequest(url, accessToken, new ParameterizedTypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Get all portfolios for the authenticated user.
	 * @param accessToken OAuth access token
	 * @return List of portfolio maps
	 */
	public List<Map<String, Object>> getPortfolios(String accessToken) {
		Map<String, Object> response = makeAuthenticatedRequest(PORTFOLIOS_URL, accessToken,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});

		if (response != null && response.containsKey("results")) {
			return (List<Map<String, Object>>) response.get("results");
		}
		return new ArrayList<>();
	}

	/**
	 * Get all stock positions for the authenticated user.
	 * @param accessToken OAuth access token
	 * @return List of position maps with instrument details
	 */
	public List<Map<String, Object>> getPositions(String accessToken) {
		return getPositions(accessToken, false);
	}

	/**
	 * Get stock positions for the authenticated user.
	 * @param accessToken OAuth access token
	 * @param nonzeroOnly Only return positions with quantity > 0
	 * @return List of position maps with instrument details
	 */
	public List<Map<String, Object>> getPositions(String accessToken, boolean nonzeroOnly) {
		String url = POSITIONS_URL;
		if (nonzeroOnly) {
			url += "?nonzero=true";
		}

		List<Map<String, Object>> allPositions = new ArrayList<>();
		String nextUrl = url;

		// Handle pagination
		while (nextUrl != null) {
			Map<String, Object> response = makeAuthenticatedRequest(nextUrl, accessToken,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			if (response != null && response.containsKey("results")) {
				List<Map<String, Object>> positions = (List<Map<String, Object>>) response.get("results");

				// Enrich positions with instrument details
				for (Map<String, Object> position : positions) {
					enrichPositionWithInstrument(position, accessToken);
				}

				allPositions.addAll(positions);
				nextUrl = (String) response.get("next");
			}
			else {
				break;
			}
		}

		return allPositions;
	}

	/**
	 * Get crypto holdings from Nummus.
	 * @param accessToken OAuth access token
	 * @return List of crypto holding maps
	 */
	public List<Map<String, Object>> getCryptoHoldings(String accessToken) {
		try {
			Map<String, Object> response = makeAuthenticatedRequest(CRYPTO_HOLDINGS_URL, accessToken,
					new ParameterizedTypeReference<Map<String, Object>>() {
					});

			if (response != null && response.containsKey("results")) {
				return (List<Map<String, Object>>) response.get("results");
			}
		}
		catch (Exception e) {
			log.warn("Could not fetch crypto holdings: {}", e.getMessage());
		}
		return new ArrayList<>();
	}

	/**
	 * Get quote for a single symbol.
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol
	 * @return Quote map
	 */
	public Map<String, Object> getQuote(String accessToken, String symbol) {
		String url = QUOTES_URL + symbol.toUpperCase() + "/";
		return makeAuthenticatedRequest(url, accessToken, new ParameterizedTypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Get quotes for multiple symbols.
	 * @param accessToken OAuth access token
	 * @param symbols List of stock symbols
	 * @return Map of symbol -> quote data
	 */
	public Map<String, Object> getQuotes(String accessToken, List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			return new HashMap<>();
		}

		String symbolList = String.join(",", symbols).toUpperCase();
		String url = QUOTES_URL + "?symbols=" + symbolList;

		Map<String, Object> response = makeAuthenticatedRequest(url, accessToken,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});

		Map<String, Object> quotesMap = new HashMap<>();

		if (response != null && response.containsKey("results")) {
			List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
			for (Map<String, Object> quote : results) {
				if (quote != null && quote.containsKey("symbol")) {
					String symbol = (String) quote.get("symbol");
					quotesMap.put(symbol, quote);
				}
			}
		}

		return quotesMap;
	}

	/**
	 * Get instrument details by URL.
	 * @param accessToken OAuth access token
	 * @param instrumentUrl Instrument URL
	 * @return Instrument details map
	 */
	public Map<String, Object> getInstrument(String accessToken, String instrumentUrl) {
		return makeAuthenticatedRequest(instrumentUrl, accessToken,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});
	}

	/**
	 * Get instrument details by symbol.
	 * @param accessToken OAuth access token
	 * @param symbol Stock symbol
	 * @return Instrument details map
	 */
	public Map<String, Object> getInstrumentBySymbol(String accessToken, String symbol) {
		String url = INSTRUMENTS_URL + "?symbol=" + symbol.toUpperCase();

		Map<String, Object> response = makeAuthenticatedRequest(url, accessToken,
				new ParameterizedTypeReference<Map<String, Object>>() {
				});

		if (response != null && response.containsKey("results")) {
			List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
			if (!results.isEmpty()) {
				return results.get(0);
			}
		}

		return null;
	}

	/**
	 * Test connection to Robinhood API.
	 * @param accessToken OAuth access token
	 * @return true if connection is valid
	 */
	public boolean testConnection(String accessToken) {
		try {
			Map<String, Object> user = getUser(accessToken);
			return user != null && !user.isEmpty();
		}
		catch (Exception e) {
			log.warn("Connection test failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Enrich a position with instrument details (symbol, name, etc.)
	 */
	private void enrichPositionWithInstrument(Map<String, Object> position, String accessToken) {
		try {
			String instrumentUrl = (String) position.get("instrument");
			if (instrumentUrl != null && !instrumentUrl.isEmpty()) {
				Map<String, Object> instrument = getInstrument(accessToken, instrumentUrl);
				if (instrument != null) {
					position.put("symbol", instrument.get("symbol"));
					position.put("name", instrument.get("simple_name"));
					position.put("instrument_details", instrument);
				}
			}
		}
		catch (Exception e) {
			log.warn("Could not enrich position with instrument details: {}", e.getMessage());
		}
	}

	/**
	 * Make an authenticated request to Robinhood API.
	 */
	private <T> T makeAuthenticatedRequest(String url, String accessToken, ParameterizedTypeReference<T> responseType) {
		try {
			if (accessToken == null || accessToken.trim().isEmpty()) {
				throw new StrategizException(RobinhoodErrors.ROBINHOOD_INVALID_TOKEN, "Access token is required");
			}

			HttpHeaders headers = buildHeaders(accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

			return response.getBody();

		}
		catch (RestClientResponseException e) {
			handleApiError(e);
			return null; // Won't reach here, handleApiError throws
		}
		catch (StrategizException e) {
			throw e;
		}
		catch (Exception e) {
			log.error("Error making request to {}: {}", url, e.getMessage());
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_API_ERROR,
					"Failed to communicate with Robinhood API: " + e.getMessage());
		}
	}

	/**
	 * Build headers for authenticated requests.
	 */
	private HttpHeaders buildHeaders(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("X-Robinhood-API-Version", "1.431.4");
		return headers;
	}

	/**
	 * Handle API error responses.
	 */
	private void handleApiError(RestClientResponseException e) {
		int statusCode = e.getStatusCode().value();
		String responseBody = e.getResponseBodyAsString();

		log.error("Robinhood API error - HTTP {}: {}", statusCode, responseBody);

		if (statusCode == 401) {
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_TOKEN_EXPIRED,
					"Access token expired or invalid. Please reconnect your Robinhood account.");
		}

		if (statusCode == 429) {
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_RATE_LIMIT,
					"Robinhood API rate limit exceeded. Please try again later.");
		}

		if (statusCode == 403) {
			throw new StrategizException(RobinhoodErrors.ROBINHOOD_AUTH_FAILED,
					"Access denied. Please reconnect your Robinhood account.");
		}

		throw new StrategizException(RobinhoodErrors.ROBINHOOD_API_ERROR,
				"Robinhood API error (HTTP " + statusCode + "): " + responseBody);
	}

}
