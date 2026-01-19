package io.strategiz.client.fmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.fmp.config.FmpConfig;
import io.strategiz.client.fmp.dto.FmpFundamentals;
import io.strategiz.client.fmp.error.FmpErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching fundamentals data from Financial Modeling Prep API.
 *
 * <p>
 * Note: This class is NOT a @Component. It is instantiated as a @Bean by ClientFmpConfig
 * to ensure proper dependency ordering (fmpRestTemplate and fmpRateLimiter must exist first).
 * </p>
 *
 * <p>
 * FMP API Documentation: https://site.financialmodelingprep.com/developer/docs
 * </p>
 */
public class FmpFundamentalsClient {

	private static final Logger log = LoggerFactory.getLogger(FmpFundamentalsClient.class);

	private final FmpConfig config;

	private final RestTemplate restTemplate;

	private final Bucket rateLimiter;

	private final ObjectMapper objectMapper;

	public FmpFundamentalsClient(FmpConfig config, RestTemplate restTemplate,
			Bucket rateLimiter, ObjectMapper objectMapper) {
		this.config = config;
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get fundamentals data for a symbol. Fetches income statement, balance sheet, cash flow,
	 * and key metrics.
	 * @param symbol Stock symbol
	 * @return FmpFundamentals object with combined data
	 */
	public FmpFundamentals getFundamentals(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbol cannot be null or empty");
		}

		if (!config.isConfigured()) {
			throw new StrategizException(FmpErrorDetails.API_KEY_MISSING, "FMP API key is not configured");
		}

		log.debug("Fetching fundamentals for symbol: {}", symbol);

		try {
			// Wait for rate limiter
			if (!rateLimiter.tryConsume(1)) {
				log.debug("Rate limit reached, waiting for token...");
				boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
				if (!acquired) {
					throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED,
							"Rate limit exceeded for FMP API");
				}
			}

			// Build URL with API key - using stable API endpoint
			// Note: stable API uses query param ?symbol= instead of path param
			String baseUrl = config.getBaseUrl().replace("/api/v3", "");
			String url = String.format("%s/stable/key-metrics?symbol=%s&apikey=%s", baseUrl, symbol,
					config.getApiKey());

			// Execute request
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				throw new StrategizException(FmpErrorDetails.NO_DATA_AVAILABLE,
						String.format("No data returned for symbol: %s", symbol));
			}

			// Parse response
			List<Map<String, Object>> data = objectMapper.readValue(response.getBody(),
					new TypeReference<List<Map<String, Object>>>() {
					});

			if (data == null || data.isEmpty()) {
				throw new StrategizException(FmpErrorDetails.NO_DATA_AVAILABLE,
						String.format("No fundamentals data available for symbol: %s", symbol));
			}

			// Convert first result to FmpFundamentals
			FmpFundamentals fundamentals = mapToFundamentals(data.get(0), symbol);

			log.info("Successfully fetched fundamentals for symbol: {}", symbol);
			return fundamentals;

		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
				log.warn("Rate limit exceeded for FMP API (429)");
				throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API",
						ex);
			}
			else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				log.error("Unauthorized access to FMP API - check API key");
				throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE, "Invalid FMP API key", ex);
			}
			else {
				log.error("FMP API error: {} - {}", ex.getStatusCode(), ex.getMessage());
				throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
						String.format("FMP API error: %s", ex.getMessage()), ex);
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while waiting for rate limiter", ex);
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limiter interrupted", ex);
		}
		catch (StrategizException ex) {
			throw ex; // Re-throw StrategizException
		}
		catch (Exception ex) {
			log.error("Failed to fetch fundamentals for symbol: {}", symbol, ex);
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("Failed to fetch fundamentals for %s: %s", symbol, ex.getMessage()), ex);
		}
	}

	/**
	 * Get fundamentals for multiple symbols in batch.
	 * @param symbols List of stock symbols
	 * @return List of FmpFundamentals
	 */
	public List<FmpFundamentals> getFundamentalsBatch(List<String> symbols) {
		log.info("Fetching fundamentals for {} symbols", symbols.size());

		List<FmpFundamentals> results = new ArrayList<>();
		int successCount = 0;
		int errorCount = 0;

		for (String symbol : symbols) {
			try {
				FmpFundamentals fundamentals = getFundamentals(symbol);
				results.add(fundamentals);
				successCount++;
			}
			catch (Exception ex) {
				log.warn("Failed to fetch fundamentals for {}: {}", symbol, ex.getMessage());
				errorCount++;
			}
		}

		log.info("Batch complete: {} successful, {} errors", successCount, errorCount);
		return results;
	}

	/**
	 * Get real-time quote for multiple symbols.
	 * Fetches each symbol individually due to FMP subscription limitations.
	 * @param symbols List of stock symbols (e.g., ["AAPL", "MSFT", "GOOG"])
	 * @return List of FmpQuote objects with current prices
	 */
	public List<FmpQuote> getQuotes(List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			return new ArrayList<>();
		}

		if (!config.isConfigured()) {
			throw new StrategizException(FmpErrorDetails.API_KEY_MISSING, "FMP API key is not configured");
		}

		log.debug("Fetching quotes for {} symbols (individual requests)", symbols.size());

		List<FmpQuote> quotes = new ArrayList<>();
		String baseUrl = config.getBaseUrl().replace("/api/v3", "");

		for (String symbol : symbols) {
			try {
				// Wait for rate limiter for each request
				if (!rateLimiter.tryConsume(1)) {
					log.debug("Rate limit reached, waiting for token...");
					boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
					if (!acquired) {
						log.warn("Rate limit exceeded, skipping remaining symbols");
						break;
					}
				}

				// Build URL for single symbol
				String url = String.format("%s/stable/quote?symbol=%s&apikey=%s", baseUrl, symbol,
						config.getApiKey());

				// Execute request
				ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

				if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
					log.warn("No data returned for symbol: {}", symbol);
					continue;
				}

				// Parse response - FMP returns array with single quote
				List<Map<String, Object>> data = objectMapper.readValue(response.getBody(),
						new TypeReference<List<Map<String, Object>>>() {
						});

				if (data != null && !data.isEmpty()) {
					quotes.add(mapToQuote(data.get(0)));
				}

			}
			catch (HttpClientErrorException ex) {
				if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
					log.warn("Rate limit exceeded for FMP API (429), stopping requests");
					break;
				}
				log.warn("Failed to fetch quote for {}: {}", symbol, ex.getMessage());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while fetching quotes");
				break;
			}
			catch (Exception ex) {
				log.warn("Failed to fetch quote for {}: {}", symbol, ex.getMessage());
			}
		}

		log.info("Successfully fetched {} quotes out of {} symbols", quotes.size(), symbols.size());
		return quotes;
	}

	/**
	 * Map FMP quote response to FmpQuote DTO.
	 */
	private FmpQuote mapToQuote(Map<String, Object> data) {
		FmpQuote quote = new FmpQuote();
		quote.setSymbol(getString(data, "symbol"));
		quote.setName(getString(data, "name"));
		quote.setPrice(getBigDecimal(data, "price"));
		quote.setChange(getBigDecimal(data, "change"));
		quote.setChangePercent(getBigDecimal(data, "changesPercentage"));
		quote.setPreviousClose(getBigDecimal(data, "previousClose"));
		quote.setOpen(getBigDecimal(data, "open"));
		quote.setDayHigh(getBigDecimal(data, "dayHigh"));
		quote.setDayLow(getBigDecimal(data, "dayLow"));
		quote.setVolume(getLong(data, "volume"));
		return quote;
	}

	private Long getLong(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof Number) {
				return ((Number) value).longValue();
			}
			return Long.parseLong(value.toString());
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Simple DTO for FMP quote data.
	 */
	public static class FmpQuote {
		private String symbol;
		private String name;
		private java.math.BigDecimal price;
		private java.math.BigDecimal change;
		private java.math.BigDecimal changePercent;
		private java.math.BigDecimal previousClose;
		private java.math.BigDecimal open;
		private java.math.BigDecimal dayHigh;
		private java.math.BigDecimal dayLow;
		private Long volume;

		public String getSymbol() { return symbol; }
		public void setSymbol(String symbol) { this.symbol = symbol; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public java.math.BigDecimal getPrice() { return price; }
		public void setPrice(java.math.BigDecimal price) { this.price = price; }
		public java.math.BigDecimal getChange() { return change; }
		public void setChange(java.math.BigDecimal change) { this.change = change; }
		public java.math.BigDecimal getChangePercent() { return changePercent; }
		public void setChangePercent(java.math.BigDecimal changePercent) { this.changePercent = changePercent; }
		public java.math.BigDecimal getPreviousClose() { return previousClose; }
		public void setPreviousClose(java.math.BigDecimal previousClose) { this.previousClose = previousClose; }
		public java.math.BigDecimal getOpen() { return open; }
		public void setOpen(java.math.BigDecimal open) { this.open = open; }
		public java.math.BigDecimal getDayHigh() { return dayHigh; }
		public void setDayHigh(java.math.BigDecimal dayHigh) { this.dayHigh = dayHigh; }
		public java.math.BigDecimal getDayLow() { return dayLow; }
		public void setDayLow(java.math.BigDecimal dayLow) { this.dayLow = dayLow; }
		public Long getVolume() { return volume; }
		public void setVolume(Long volume) { this.volume = volume; }

		public boolean isPositive() {
			return change != null && change.compareTo(java.math.BigDecimal.ZERO) >= 0;
		}
	}

	/**
	 * Map FMP API response to FmpFundamentals DTO.
	 */
	private FmpFundamentals mapToFundamentals(Map<String, Object> data, String symbol) {
		FmpFundamentals fundamentals = new FmpFundamentals();
		fundamentals.setSymbol(symbol);

		// Extract fields safely
		fundamentals.setDate(getString(data, "date"));
		fundamentals.setPeriod(getString(data, "period"));
		fundamentals.setRevenue(getBigDecimal(data, "revenue"));
		fundamentals.setNetIncome(getBigDecimal(data, "netIncome"));
		fundamentals.setEps(getBigDecimal(data, "eps"));
		fundamentals.setTotalAssets(getBigDecimal(data, "totalAssets"));
		fundamentals.setTotalDebt(getBigDecimal(data, "totalDebt"));
		fundamentals.setPeRatio(getBigDecimal(data, "peRatio"));
		fundamentals.setPriceToBook(getBigDecimal(data, "priceToBookRatio"));
		fundamentals.setDebtToEquity(getBigDecimal(data, "debtToEquity"));
		fundamentals.setRoe(getBigDecimal(data, "returnOnEquity"));
		fundamentals.setRoa(getBigDecimal(data, "returnOnAssets"));
		fundamentals.setCurrentRatio(getBigDecimal(data, "currentRatio"));
		fundamentals.setQuickRatio(getBigDecimal(data, "quickRatio"));
		fundamentals.setGrossMargin(getBigDecimal(data, "grossProfitMargin"));
		fundamentals.setOperatingMargin(getBigDecimal(data, "operatingProfitMargin"));
		fundamentals.setNetMargin(getBigDecimal(data, "netProfitMargin"));
		fundamentals.setDividendYield(getBigDecimal(data, "dividendYield"));
		fundamentals.setPayoutRatio(getBigDecimal(data, "payoutRatio"));
		fundamentals.setFreeCashFlow(getBigDecimal(data, "freeCashFlow"));
		fundamentals.setOperatingCashFlow(getBigDecimal(data, "operatingCashFlow"));

		return fundamentals;
	}

	private String getString(Map<String, Object> data, String key) {
		Object value = data.get(key);
		return value != null ? value.toString() : null;
	}

	private java.math.BigDecimal getBigDecimal(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof Number) {
				return new java.math.BigDecimal(value.toString());
			}
			return new java.math.BigDecimal(value.toString());
		}
		catch (Exception e) {
			log.debug("Failed to parse {} as BigDecimal: {}", key, value);
			return null;
		}
	}

}
