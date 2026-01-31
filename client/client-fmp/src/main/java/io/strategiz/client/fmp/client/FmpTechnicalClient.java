package io.strategiz.client.fmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.fmp.config.FmpConfig;
import io.strategiz.client.fmp.dto.FmpTechnicalIndicator;
import io.strategiz.client.fmp.error.FmpErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for fetching technical indicators from Financial Modeling Prep API.
 *
 * <p>
 * Note: This class is NOT a @Component. It is instantiated as a @Bean by ClientFmpConfig
 * to ensure proper dependency ordering (fmpRestTemplate and fmpRateLimiter must exist
 * first).
 * </p>
 *
 * <p>
 * FMP Technical Indicator Endpoints:
 * <ul>
 * <li>RSI: /api/v3/technical_indicator/daily/{symbol}?type=rsi&period=14</li>
 * <li>SMA: /api/v3/technical_indicator/daily/{symbol}?type=sma&period=20</li>
 * <li>EMA: /api/v3/technical_indicator/daily/{symbol}?type=ema&period=20</li>
 * <li>MACD: /api/v3/technical_indicator/daily/{symbol}?type=macd</li>
 * </ul>
 * </p>
 *
 * <p>
 * FMP API Documentation: https://site.financialmodelingprep.com/developer/docs
 * </p>
 */
public class FmpTechnicalClient {

	private static final Logger log = LoggerFactory.getLogger(FmpTechnicalClient.class);

	private static final int DEFAULT_RSI_PERIOD = 14;

	private static final int DEFAULT_SMA_PERIOD = 20;

	private static final int DEFAULT_EMA_PERIOD = 20;

	private final FmpConfig config;

	private final RestTemplate restTemplate;

	private final Bucket rateLimiter;

	private final ObjectMapper objectMapper;

	public FmpTechnicalClient(FmpConfig config, RestTemplate restTemplate, Bucket rateLimiter,
			ObjectMapper objectMapper) {
		this.config = config;
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get RSI (Relative Strength Index) for a symbol.
	 * @param symbol Stock symbol
	 * @param period RSI period (default: 14)
	 * @return Latest FmpTechnicalIndicator with RSI value
	 */
	public FmpTechnicalIndicator getRSI(String symbol, int period) {
		validateSymbol(symbol);
		ensureConfigured();

		int effectivePeriod = period > 0 ? period : DEFAULT_RSI_PERIOD;
		log.debug("Fetching RSI({}) for symbol: {}", effectivePeriod, symbol);

		String url = String.format("%s/technical_indicator/daily/%s?type=rsi&period=%d&apikey=%s", config.getBaseUrl(),
				symbol.toUpperCase(), effectivePeriod, config.getApiKey());

		List<FmpTechnicalIndicator> indicators = fetchIndicators(url, "RSI for " + symbol);
		return indicators.isEmpty() ? null : indicators.get(0);
	}

	/**
	 * Get RSI with default period (14).
	 * @param symbol Stock symbol
	 * @return Latest FmpTechnicalIndicator with RSI value
	 */
	public FmpTechnicalIndicator getRSI(String symbol) {
		return getRSI(symbol, DEFAULT_RSI_PERIOD);
	}

	/**
	 * Get SMA (Simple Moving Average) for a symbol.
	 * @param symbol Stock symbol
	 * @param period SMA period (e.g., 20, 50, 200)
	 * @return Latest FmpTechnicalIndicator with SMA value
	 */
	public FmpTechnicalIndicator getSMA(String symbol, int period) {
		validateSymbol(symbol);
		ensureConfigured();

		int effectivePeriod = period > 0 ? period : DEFAULT_SMA_PERIOD;
		log.debug("Fetching SMA({}) for symbol: {}", effectivePeriod, symbol);

		String url = String.format("%s/technical_indicator/daily/%s?type=sma&period=%d&apikey=%s", config.getBaseUrl(),
				symbol.toUpperCase(), effectivePeriod, config.getApiKey());

		List<FmpTechnicalIndicator> indicators = fetchIndicators(url, "SMA for " + symbol);
		return indicators.isEmpty() ? null : indicators.get(0);
	}

	/**
	 * Get EMA (Exponential Moving Average) for a symbol.
	 * @param symbol Stock symbol
	 * @param period EMA period (e.g., 9, 21, 50)
	 * @return Latest FmpTechnicalIndicator with EMA value
	 */
	public FmpTechnicalIndicator getEMA(String symbol, int period) {
		validateSymbol(symbol);
		ensureConfigured();

		int effectivePeriod = period > 0 ? period : DEFAULT_EMA_PERIOD;
		log.debug("Fetching EMA({}) for symbol: {}", effectivePeriod, symbol);

		String url = String.format("%s/technical_indicator/daily/%s?type=ema&period=%d&apikey=%s", config.getBaseUrl(),
				symbol.toUpperCase(), effectivePeriod, config.getApiKey());

		List<FmpTechnicalIndicator> indicators = fetchIndicators(url, "EMA for " + symbol);
		return indicators.isEmpty() ? null : indicators.get(0);
	}

	/**
	 * Get MACD (Moving Average Convergence Divergence) for a symbol.
	 * @param symbol Stock symbol
	 * @return Latest FmpTechnicalIndicator with MACD values
	 */
	public FmpTechnicalIndicator getMACD(String symbol) {
		validateSymbol(symbol);
		ensureConfigured();

		log.debug("Fetching MACD for symbol: {}", symbol);

		String url = String.format("%s/technical_indicator/daily/%s?type=macd&apikey=%s", config.getBaseUrl(),
				symbol.toUpperCase(), config.getApiKey());

		List<FmpTechnicalIndicator> indicators = fetchIndicators(url, "MACD for " + symbol);
		return indicators.isEmpty() ? null : indicators.get(0);
	}

	/**
	 * Get RSI for multiple symbols.
	 * @param symbols List of stock symbols
	 * @return List of FmpTechnicalIndicator with RSI values
	 */
	public List<FmpTechnicalIndicator> getRSIBatch(List<String> symbols) {
		List<FmpTechnicalIndicator> results = new ArrayList<>();
		for (String symbol : symbols) {
			try {
				FmpTechnicalIndicator indicator = getRSI(symbol);
				if (indicator != null) {
					// Store symbol on the indicator for context
					results.add(createWithSymbol(indicator, symbol));
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch RSI for {}: {}", symbol, e.getMessage());
			}
		}
		return results;
	}

	/**
	 * Get technical summary for a symbol (RSI, MACD, SMA 20/50/200).
	 * @param symbol Stock symbol
	 * @return Formatted technical summary string
	 */
	public String getTechnicalSummary(String symbol) {
		StringBuilder sb = new StringBuilder();
		sb.append("Technical Analysis for ").append(symbol).append(":\n");

		try {
			// RSI
			FmpTechnicalIndicator rsi = getRSI(symbol);
			if (rsi != null && rsi.getRsi() != null) {
				sb.append("  ").append(rsi.toRsiContextString()).append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch RSI for {}: {}", symbol, e.getMessage());
		}

		try {
			// MACD
			FmpTechnicalIndicator macd = getMACD(symbol);
			if (macd != null && macd.getMacd() != null) {
				sb.append("  ").append(macd.toMacdContextString()).append("\n");
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch MACD for {}: {}", symbol, e.getMessage());
		}

		try {
			// SMA 20, 50, 200
			FmpTechnicalIndicator sma20 = getSMA(symbol, 20);
			FmpTechnicalIndicator sma50 = getSMA(symbol, 50);
			FmpTechnicalIndicator sma200 = getSMA(symbol, 200);

			if (sma20 != null || sma50 != null || sma200 != null) {
				sb.append("  Moving Averages:\n");
				if (sma20 != null && sma20.getSma() != null) {
					String trend = sma20.isPriceAboveSma() ? "above" : "below";
					sb.append(String.format("    SMA(20): $%.2f (price %s)\n", sma20.getSma(), trend));
				}
				if (sma50 != null && sma50.getSma() != null) {
					String trend = sma50.isPriceAboveSma() ? "above" : "below";
					sb.append(String.format("    SMA(50): $%.2f (price %s)\n", sma50.getSma(), trend));
				}
				if (sma200 != null && sma200.getSma() != null) {
					String trend = sma200.isPriceAboveSma() ? "above" : "below";
					sb.append(String.format("    SMA(200): $%.2f (price %s)\n", sma200.getSma(), trend));
				}
			}
		}
		catch (Exception e) {
			log.warn("Failed to fetch SMAs for {}: {}", symbol, e.getMessage());
		}

		return sb.toString();
	}

	/**
	 * Format RSI batch for context display.
	 * @param symbols List of symbols
	 * @return Formatted RSI summary string
	 */
	public String formatRSIForContext(List<String> symbols) {
		StringBuilder sb = new StringBuilder();
		sb.append("RSI Signals:\n");

		for (String symbol : symbols) {
			try {
				FmpTechnicalIndicator rsi = getRSI(symbol);
				if (rsi != null && rsi.getRsi() != null) {
					sb.append(String.format("  %s: %.1f (%s)\n", symbol, rsi.getRsi(), rsi.interpretRsi()));
				}
			}
			catch (Exception e) {
				log.warn("Failed to fetch RSI for {}: {}", symbol, e.getMessage());
			}
		}

		return sb.toString();
	}

	/**
	 * Check if the client is properly configured with an API key.
	 * @return true if configured
	 */
	public boolean isConfigured() {
		return config.isConfigured();
	}

	/**
	 * Fetch technical indicators from the given URL.
	 */
	private List<FmpTechnicalIndicator> fetchIndicators(String url, String description) {
		try {
			waitForRateLimiter();

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				log.warn("No data returned for {}", description);
				return new ArrayList<>();
			}

			// Parse response - FMP returns array of indicator data points
			List<Map<String, Object>> data = objectMapper.readValue(response.getBody(),
					new TypeReference<List<Map<String, Object>>>() {
					});

			if (data == null || data.isEmpty()) {
				log.debug("No indicator data available for {}", description);
				return new ArrayList<>();
			}

			// Map the first (most recent) data point
			List<FmpTechnicalIndicator> indicators = new ArrayList<>();
			FmpTechnicalIndicator indicator = mapToIndicator(data.get(0));
			indicators.add(indicator);

			log.debug("Successfully fetched indicator for {}", description);
			return indicators;

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
	 * Map API response to FmpTechnicalIndicator.
	 */
	private FmpTechnicalIndicator mapToIndicator(Map<String, Object> data) {
		FmpTechnicalIndicator indicator = new FmpTechnicalIndicator();

		indicator.setDate(getString(data, "date"));
		indicator.setOpen(getBigDecimal(data, "open"));
		indicator.setHigh(getBigDecimal(data, "high"));
		indicator.setLow(getBigDecimal(data, "low"));
		indicator.setClose(getBigDecimal(data, "close"));
		indicator.setVolume(getLong(data, "volume"));

		// Technical indicator values
		indicator.setRsi(getBigDecimal(data, "rsi"));
		indicator.setSma(getBigDecimal(data, "sma"));
		indicator.setEma(getBigDecimal(data, "ema"));
		indicator.setMacd(getBigDecimal(data, "macd"));
		indicator.setMacdSignal(getBigDecimal(data, "macdSignal"));
		indicator.setMacdHist(getBigDecimal(data, "macdHist"));

		return indicator;
	}

	/**
	 * Create indicator copy with symbol for context.
	 */
	private FmpTechnicalIndicator createWithSymbol(FmpTechnicalIndicator original, String symbol) {
		// Just return original - symbol tracking can be done at higher level
		return original;
	}

	private void validateSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new StrategizException(FmpErrorDetails.INVALID_SYMBOL, "Symbol cannot be null or empty");
		}
	}

	private void ensureConfigured() {
		if (!config.isConfigured()) {
			throw new StrategizException(FmpErrorDetails.API_KEY_MISSING, "FMP API key is not configured");
		}
	}

	private void waitForRateLimiter() throws InterruptedException {
		if (!rateLimiter.tryConsume(1)) {
			log.debug("Rate limit reached, waiting for token...");
			boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
			if (!acquired) {
				throw new StrategizException(FmpErrorDetails.RATE_LIMIT_EXCEEDED, "Rate limit exceeded for FMP API");
			}
		}
	}

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

	private String getString(Map<String, Object> data, String key) {
		Object value = data.get(key);
		return value != null ? value.toString() : null;
	}

	private BigDecimal getBigDecimal(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof Number) {
				return new BigDecimal(value.toString());
			}
			return new BigDecimal(value.toString());
		}
		catch (Exception e) {
			log.debug("Failed to parse {} as BigDecimal: {}", key, value);
			return null;
		}
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

}
