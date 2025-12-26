package io.strategiz.client.yahoo;

import io.strategiz.client.yahoo.error.YahooFinanceErrorDetails;
import io.strategiz.client.yahoo.model.YahooFundamentals;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching company fundamental data from Yahoo Finance API.
 *
 * <p>
 * This client uses Yahoo Finance's unofficial API to fetch fundamental financial data including:
 * - Financial ratios (P/E, P/B, margins, ROE, etc.)
 * - Balance sheet data (assets, liabilities, equity)
 * - Income statement data (revenue, EBITDA, net income)
 * - Key statistics (market cap, EPS, shares outstanding)
 * </p>
 *
 * <p>
 * Rate Limiting: Implements configurable delays between requests (default 150ms) to avoid
 * overwhelming Yahoo's servers. Supports exponential backoff on failures.
 * </p>
 *
 * <p>
 * Error Handling: Uses structured error codes from {@link YahooFinanceErrorDetails} and
 * throws {@link StrategizException} for all failures.
 * </p>
 */
@Component
public class YahooFundamentalsClient {

	private static final Logger log = LoggerFactory.getLogger(YahooFundamentalsClient.class);

	private final RestTemplate restTemplate;

	@Value("${yahoo.finance.base-url:https://query2.finance.yahoo.com}")
	private String baseUrl;

	@Value("${yahoo.finance.delay-ms:150}")
	private long delayMs;

	@Value("${yahoo.finance.max-retries:5}")
	private int maxRetries;

	@Value("${yahoo.finance.retry-delay-ms:30000}")
	private long retryDelayMs;

	@Value("${yahoo.finance.timeout-ms:10000}")
	private long timeoutMs;

	public YahooFundamentalsClient(RestTemplate yahooFinanceRestTemplate) {
		this.restTemplate = yahooFinanceRestTemplate;
	}

	/**
	 * Fetch all fundamental data for a single symbol.
	 *
	 * @param symbol Stock symbol (e.g., "AAPL", "MSFT")
	 * @return YahooFundamentals with all available data
	 * @throws StrategizException if the request fails
	 */
	public YahooFundamentals getFundamentals(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(YahooFinanceErrorDetails.INVALID_SYMBOL,
					"Symbol cannot be null or empty");
		}

		log.debug("Fetching fundamentals for symbol: {}", symbol);

		try {
			// Build Yahoo Finance quote summary URL
			String url = String.format("%s/v10/finance/quoteSummary/%s?modules="
					+ "financialData,defaultKeyStatistics,balanceSheetHistory,incomeStatementHistory", baseUrl,
					symbol);

			// Execute request with retries
			ResponseEntity<Map> response = executeWithRetry(url, Map.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				throw new StrategizException(YahooFinanceErrorDetails.NO_DATA_AVAILABLE,
						String.format("No data returned for symbol: %s", symbol));
			}

			// Parse response into YahooFundamentals
			YahooFundamentals fundamentals = parseResponse(symbol, response.getBody());

			log.debug("Successfully fetched fundamentals for {}", symbol);
			return fundamentals;

		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
				throw new StrategizException(YahooFinanceErrorDetails.INVALID_SYMBOL,
						String.format("Symbol not found: %s", symbol), ex);
			}
			else if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				throw new StrategizException(YahooFinanceErrorDetails.RATE_LIMIT_EXCEEDED,
						"Rate limit exceeded for Yahoo Finance API", ex);
			}
			else {
				throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
						String.format("API error for symbol %s: %s", symbol, ex.getMessage()), ex);
			}
		}
		catch (HttpServerErrorException ex) {
			throw new StrategizException(YahooFinanceErrorDetails.API_ERROR_RESPONSE,
					String.format("Yahoo Finance server error for symbol %s", symbol), ex);
		}
		catch (StrategizException ex) {
			throw ex; // Re-throw StrategizException
		}
		catch (Exception ex) {
			throw new StrategizException(YahooFinanceErrorDetails.NETWORK_ERROR,
					String.format("Failed to fetch fundamentals for %s", symbol), ex);
		}
	}

	/**
	 * Fetch fundamentals for multiple symbols with rate limiting.
	 *
	 * @param symbols List of stock symbols
	 * @return Map of symbol to YahooFundamentals (excludes symbols with errors)
	 */
	public Map<String, YahooFundamentals> batchGetFundamentals(List<String> symbols) {
		log.info("Fetching fundamentals for {} symbols", symbols.size());

		Map<String, YahooFundamentals> results = new HashMap<>();
		int successCount = 0;
		int errorCount = 0;

		for (int i = 0; i < symbols.size(); i++) {
			String symbol = symbols.get(i);

			try {
				YahooFundamentals fundamentals = getFundamentals(symbol);
				results.put(symbol, fundamentals);
				successCount++;

				log.debug("Progress: {}/{} symbols processed ({})", i + 1, symbols.size(), symbol);

				// Rate limiting: delay between requests
				if (i < symbols.size() - 1) {
					Thread.sleep(delayMs);
				}
			}
			catch (StrategizException ex) {
				log.warn("Failed to fetch fundamentals for {}: {}", symbol, ex.getMessage());
				errorCount++;
				// Continue with next symbol
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.error("Batch processing interrupted", ex);
				break;
			}
		}

		log.info("Batch complete: {} successful, {} errors", successCount, errorCount);
		return results;
	}

	/**
	 * Execute HTTP request with exponential backoff retry logic.
	 */
	private <T> ResponseEntity<T> executeWithRetry(String url, Class<T> responseType) {
		int attempt = 0;
		long currentDelay = retryDelayMs;

		while (attempt < maxRetries) {
			try {
				return restTemplate.getForEntity(url, responseType);
			}
			catch (HttpServerErrorException ex) {
				attempt++;
				if (attempt >= maxRetries) {
					throw ex;
				}

				log.warn("Request failed (attempt {}/{}), retrying in {}ms", attempt, maxRetries, currentDelay);

				try {
					Thread.sleep(currentDelay);
					currentDelay *= 2; // Exponential backoff
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new StrategizException(YahooFinanceErrorDetails.NETWORK_ERROR, "Request interrupted", ie);
				}
			}
		}

		throw new StrategizException(YahooFinanceErrorDetails.TIMEOUT_ERROR,
				String.format("Max retries (%d) exceeded", maxRetries));
	}

	/**
	 * Parse Yahoo Finance API response into YahooFundamentals DTO.
	 *
	 * @param symbol Stock symbol
	 * @param responseBody Raw API response
	 * @return Parsed YahooFundamentals
	 */
	@SuppressWarnings("unchecked")
	private YahooFundamentals parseResponse(String symbol, Map<String, Object> responseBody) {
		try {
			Map<String, Object> quoteSummary = (Map<String, Object>) responseBody.get("quoteSummary");
			if (quoteSummary == null) {
				throw new StrategizException(YahooFinanceErrorDetails.PARSE_ERROR, "Missing quoteSummary in response");
			}

			List<Map<String, Object>> results = (List<Map<String, Object>>) quoteSummary.get("result");
			if (results == null || results.isEmpty()) {
				throw new StrategizException(YahooFinanceErrorDetails.NO_DATA_AVAILABLE,
						String.format("No data in quoteSummary.result for %s", symbol));
			}

			Map<String, Object> data = results.get(0);

			YahooFundamentals fundamentals = new YahooFundamentals(symbol);

			// Parse each module if present
			if (data.containsKey("financialData")) {
				// TODO: Parse YahooFinancialData from financialData map
			}
			if (data.containsKey("defaultKeyStatistics")) {
				// TODO: Parse YahooKeyStatistics from defaultKeyStatistics map
			}
			if (data.containsKey("balanceSheetHistory")) {
				// TODO: Parse YahooBalanceSheet from balanceSheetHistory map
			}
			if (data.containsKey("incomeStatementHistory")) {
				// TODO: Parse YahooIncomeStatement from incomeStatementHistory map
			}

			return fundamentals;
		}
		catch (ClassCastException | NullPointerException ex) {
			throw new StrategizException(YahooFinanceErrorDetails.PARSE_ERROR,
					String.format("Failed to parse response for %s", symbol), ex);
		}
	}

}
