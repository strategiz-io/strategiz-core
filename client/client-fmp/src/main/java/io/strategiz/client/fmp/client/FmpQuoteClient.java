package io.strategiz.client.fmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.fmp.config.FmpConfig;
import io.strategiz.client.fmp.dto.FmpQuote;
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
import java.util.stream.Collectors;

/**
 * Client for fetching real-time quotes from Financial Modeling Prep API.
 *
 * <p>
 * Note: This class is NOT a @Component. It is instantiated as a @Bean by ClientFmpConfig
 * to ensure proper dependency ordering (fmpRestTemplate and fmpRateLimiter must exist
 * first).
 * </p>
 *
 * <p>
 * FMP Quote Endpoints:
 * <ul>
 * <li>Single Quote: /api/v3/quote/{symbol}</li>
 * <li>Batch Quotes: /api/v3/quote/{symbol1,symbol2,...}</li>
 * </ul>
 * </p>
 *
 * <p>
 * FMP API Documentation: https://site.financialmodelingprep.com/developer/docs
 * </p>
 */
public class FmpQuoteClient {

	private static final Logger log = LoggerFactory.getLogger(FmpQuoteClient.class);

	// Key market indices and ETFs
	private static final List<String> INDEX_ETFS = List.of("SPY", // S&P 500
			"QQQ", // NASDAQ-100
			"IWM", // Russell 2000
			"DIA" // Dow Jones
	);

	// Sector ETFs for rotation analysis
	private static final List<String> SECTOR_ETFS = List.of("XLK", // Technology
			"XLF", // Financials
			"XLE", // Energy
			"XLV", // Healthcare
			"XLI", // Industrials
			"XLY", // Consumer Discretionary
			"XLP", // Consumer Staples
			"XLRE", // Real Estate
			"XLU", // Utilities
			"XLB", // Materials
			"XLC" // Communication Services
	);

	// VIX for volatility
	private static final String VIX_SYMBOL = "^VIX";

	private final FmpConfig config;

	private final RestTemplate restTemplate;

	private final Bucket rateLimiter;

	private final ObjectMapper objectMapper;

	public FmpQuoteClient(FmpConfig config, RestTemplate restTemplate, Bucket rateLimiter, ObjectMapper objectMapper) {
		this.config = config;
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get real-time quote for a single symbol.
	 * @param symbol Stock symbol (e.g., "AAPL", "SPY")
	 * @return FmpQuote with current price data
	 */
	public FmpQuote getQuote(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbol cannot be null or empty");
		}

		List<FmpQuote> quotes = getBatchQuotes(List.of(symbol));
		if (quotes.isEmpty()) {
			throw new StrategizException(FmpErrorDetails.NO_DATA_AVAILABLE,
					String.format("No quote data available for symbol: %s", symbol));
		}
		return quotes.get(0);
	}

	/**
	 * Get real-time quotes for multiple symbols.
	 * @param symbols List of stock symbols
	 * @return List of FmpQuote objects
	 */
	public List<FmpQuote> getBatchQuotes(List<String> symbols) {
		if (symbols == null || symbols.isEmpty()) {
			return new ArrayList<>();
		}

		ensureConfigured();

		// FMP API allows comma-separated symbols in a single request
		String symbolList = symbols.stream().map(String::toUpperCase).collect(Collectors.joining(","));

		log.debug("Fetching quotes for symbols: {}", symbolList);

		String url = String.format("%s/quote/%s?apikey=%s", config.getBaseUrl(), symbolList, config.getApiKey());

		return fetchQuotes(url, "quotes for " + symbolList);
	}

	/**
	 * Get real-time quotes for major market indices (SPY, QQQ, IWM, DIA).
	 * @return List of FmpQuote for index ETFs
	 */
	public List<FmpQuote> getIndexQuotes() {
		return getBatchQuotes(INDEX_ETFS);
	}

	/**
	 * Get real-time quotes for sector ETFs (XLK, XLF, XLE, etc.).
	 * @return List of FmpQuote for sector ETFs
	 */
	public List<FmpQuote> getSectorETFQuotes() {
		return getBatchQuotes(SECTOR_ETFS);
	}

	/**
	 * Get VIX (volatility index) quote.
	 * @return FmpQuote for VIX or null if not available
	 */
	public FmpQuote getVixQuote() {
		try {
			return getQuote(VIX_SYMBOL);
		}
		catch (StrategizException e) {
			log.warn("Failed to fetch VIX quote: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Get all market overview quotes (indices + VIX).
	 * @return List of quotes for market overview
	 */
	public List<FmpQuote> getMarketOverviewQuotes() {
		List<String> symbols = new ArrayList<>(INDEX_ETFS);
		symbols.add(VIX_SYMBOL);
		return getBatchQuotes(symbols);
	}

	/**
	 * Format index quotes for AI context display.
	 * @return Formatted string with index prices and changes
	 */
	public String formatIndexQuotesForContext() {
		List<FmpQuote> quotes = getIndexQuotes();
		if (quotes.isEmpty()) {
			return "Index data: [Unable to fetch - service temporarily unavailable]\n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Market Indices:\n");
		for (FmpQuote quote : quotes) {
			sb.append("  ").append(quote.toContextString()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Format sector ETF quotes for AI context display.
	 * @return Formatted string with sector performance
	 */
	public String formatSectorQuotesForContext() {
		List<FmpQuote> quotes = getSectorETFQuotes();
		if (quotes.isEmpty()) {
			return "Sector data: [Unable to fetch - service temporarily unavailable]\n";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Sector Performance (ETFs):\n");

		// Sort by change percent descending
		quotes.sort((a, b) -> {
			if (b.getChangePercent() == null) {
				return -1;
			}
			if (a.getChangePercent() == null) {
				return 1;
			}
			return b.getChangePercent().compareTo(a.getChangePercent());
		});

		for (FmpQuote quote : quotes) {
			String sector = getSectorName(quote.getSymbol());
			sb.append(String.format("  %s (%s): %s\n", quote.getSymbol(), sector, formatChangePercent(quote)));
		}
		return sb.toString();
	}

	/**
	 * Format VIX for context with interpretation.
	 * @return Formatted VIX string with market sentiment
	 */
	public String formatVixForContext() {
		FmpQuote vix = getVixQuote();
		if (vix == null || vix.getPrice() == null) {
			return "VIX: [Unable to fetch]\n";
		}

		double vixValue = vix.getPrice().doubleValue();
		String sentiment;
		if (vixValue < 15) {
			sentiment = "Low volatility - complacency";
		}
		else if (vixValue < 20) {
			sentiment = "Normal volatility";
		}
		else if (vixValue < 30) {
			sentiment = "Elevated volatility - caution";
		}
		else {
			sentiment = "High volatility - fear";
		}

		return String.format("VIX: %.2f (%s)\n", vixValue, sentiment);
	}

	/**
	 * Check if the client is properly configured with an API key.
	 * @return true if configured
	 */
	public boolean isConfigured() {
		return config.isConfigured();
	}

	/**
	 * Fetch quotes from the given URL.
	 */
	private List<FmpQuote> fetchQuotes(String url, String description) {
		try {
			waitForRateLimiter();

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				log.warn("No data returned for {}", description);
				return new ArrayList<>();
			}

			List<FmpQuote> quotes = objectMapper.readValue(response.getBody(), new TypeReference<List<FmpQuote>>() {
			});

			log.info("Successfully fetched {} quotes for {}", quotes != null ? quotes.size() : 0, description);
			return quotes != null ? quotes : new ArrayList<>();

		}
		catch (HttpClientErrorException ex) {
			handleHttpError(ex, description);
			return new ArrayList<>();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while waiting for rate limiter", ex);
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limiter interrupted", ex);
		}
		catch (StrategizException ex) {
			throw ex;
		}
		catch (Exception ex) {
			log.error("Failed to fetch {}: {}", description, ex.getMessage(), ex);
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("Failed to fetch %s: %s", description, ex.getMessage()), ex);
		}
	}

	/**
	 * Wait for rate limiter token.
	 */
	private void waitForRateLimiter() throws InterruptedException {
		if (!rateLimiter.tryConsume(1)) {
			log.debug("Rate limit reached, waiting for token...");
			boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
			if (!acquired) {
				throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API");
			}
		}
	}

	/**
	 * Ensure API is configured.
	 */
	private void ensureConfigured() {
		if (!config.isConfigured()) {
			throw new StrategizException(FmpErrorDetails.API_KEY_MISSING, "FMP API key is not configured");
		}
	}

	/**
	 * Handle HTTP errors from FMP API.
	 */
	private void handleHttpError(HttpClientErrorException ex, String description) {
		if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
			log.warn("Rate limit exceeded for FMP API (429)");
			throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API", ex);
		}
		else if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			log.error("Unauthorized access to FMP API - check API key");
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE, "Invalid FMP API key", ex);
		}
		else {
			log.error("FMP API error for {}: {} - {}", description, ex.getStatusCode(), ex.getMessage());
			throw new StrategizException(FmpErrorDetails.API_ERROR_RESPONSE,
					String.format("FMP API error: %s", ex.getMessage()), ex);
		}
	}

	/**
	 * Get sector name from ETF symbol.
	 */
	private String getSectorName(String symbol) {
		return switch (symbol) {
			case "XLK" -> "Technology";
			case "XLF" -> "Financials";
			case "XLE" -> "Energy";
			case "XLV" -> "Healthcare";
			case "XLI" -> "Industrials";
			case "XLY" -> "Consumer Disc.";
			case "XLP" -> "Consumer Staples";
			case "XLRE" -> "Real Estate";
			case "XLU" -> "Utilities";
			case "XLB" -> "Materials";
			case "XLC" -> "Communication";
			default -> symbol;
		};
	}

	/**
	 * Format change percent for display.
	 */
	private String formatChangePercent(FmpQuote quote) {
		if (quote.getChangePercent() == null) {
			return "N/A";
		}
		String sign = quote.getChangePercent().doubleValue() >= 0 ? "+" : "";
		return String.format("%s%.2f%%", sign, quote.getChangePercent());
	}

}
