package io.strategiz.client.fmp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.strategiz.client.fmp.config.FmpConfig;
import io.strategiz.client.fmp.dto.FmpSECFiling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Client for fetching SEC filings from Financial Modeling Prep API.
 *
 * <p>
 * Note: This class is NOT a @Component. It is instantiated as a @Bean by ClientFmpConfig to ensure
 * proper dependency ordering.
 * </p>
 *
 * <p>
 * FMP Endpoint: GET /api/v3/sec_filings/{symbol}?type={type}&limit={limit}
 * </p>
 */
public class FmpFilingsClient {

	private static final Logger log = LoggerFactory.getLogger(FmpFilingsClient.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private final FmpConfig config;

	private final RestTemplate restTemplate;

	private final Bucket rateLimiter;

	private final ObjectMapper objectMapper;

	public FmpFilingsClient(FmpConfig config, RestTemplate restTemplate, Bucket rateLimiter,
			ObjectMapper objectMapper) {
		this.config = config;
		this.restTemplate = restTemplate;
		this.rateLimiter = rateLimiter;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get SEC filings for a company within a date range.
	 * @param symbol Stock symbol
	 * @param from Start date
	 * @param to End date
	 * @return List of SEC filings
	 */
	public List<FmpSECFiling> getFilings(String symbol, LocalDate from, LocalDate to) {
		if (!config.isConfigured()) {
			log.warn("FMP API key not configured");
			return Collections.emptyList();
		}

		log.debug("Fetching SEC filings for {} from {} to {}", symbol, from, to);

		try {
			if (!rateLimiter.tryConsume(1)) {
				boolean acquired = rateLimiter.asBlocking().tryConsume(1, Duration.ofSeconds(10));
				if (!acquired) {
					log.warn("Rate limit exceeded for FMP SEC filings");
					return Collections.emptyList();
				}
			}

			String url = String.format("%s/sec_filings/%s?from=%s&to=%s&apikey=%s", config.getBaseUrl(),
					symbol.toUpperCase(), from.format(DATE_FORMATTER), to.format(DATE_FORMATTER), config.getApiKey());

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

			if (response.getBody() == null || !response.getStatusCode().is2xxSuccessful()) {
				log.warn("No SEC filings data returned for {}", symbol);
				return Collections.emptyList();
			}

			List<FmpSECFiling> filings = objectMapper.readValue(response.getBody(),
					new TypeReference<List<FmpSECFiling>>() {
					});

			log.debug("Fetched {} SEC filings for {}", filings != null ? filings.size() : 0, symbol);
			return filings != null ? filings : Collections.emptyList();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrupted while fetching SEC filings for {}", symbol, ex);
			return Collections.emptyList();
		}
		catch (HttpClientErrorException ex) {
			log.warn("FMP API error fetching SEC filings for {}: {} - {}", symbol, ex.getStatusCode(),
					ex.getMessage());
			return Collections.emptyList();
		}
		catch (Exception ex) {
			log.error("Failed to fetch SEC filings for {}: {}", symbol, ex.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Get recent SEC filings for a company (last 90 days).
	 */
	public List<FmpSECFiling> getRecentFilings(String symbol) {
		LocalDate today = LocalDate.now();
		return getFilings(symbol, today.minusDays(90), today);
	}

	/**
	 * Get major SEC filings only (10-K, 10-Q, 8-K, DEF 14A).
	 */
	public List<FmpSECFiling> getMajorFilings(String symbol, int days) {
		LocalDate today = LocalDate.now();
		return getFilings(symbol, today.minusDays(days), today).stream()
			.filter(FmpSECFiling::isMajorFiling)
			.sorted(Comparator.comparing(FmpSECFiling::getFiledDate, Comparator.nullsLast(Comparator.reverseOrder())))
			.toList();
	}

	/**
	 * Get filings for multiple symbols.
	 */
	public List<FmpSECFiling> getFilingsForSymbols(List<String> symbols, int days) {
		if (symbols == null || symbols.isEmpty()) {
			return Collections.emptyList();
		}

		LocalDate today = LocalDate.now();
		LocalDate fromDate = today.minusDays(days);

		return symbols.stream()
			.flatMap(symbol -> getFilings(symbol, fromDate, today).stream())
			.filter(FmpSECFiling::isMajorFiling)
			.sorted(
					Comparator.comparing(FmpSECFiling::getFiledDate, Comparator.nullsLast(Comparator.reverseOrder())))
			.limit(20)
			.toList();
	}

	/**
	 * Format SEC filings for AI context injection.
	 */
	public String formatFilingsForContext(List<FmpSECFiling> filings) {
		if (filings == null || filings.isEmpty()) {
			return "No recent SEC filings available.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("RECENT SEC FILINGS:\n");
		sb.append("| Symbol | Form | Description | Filed Date |\n");
		sb.append("|--------|------|-------------|------------|\n");

		filings.stream().limit(15).forEach(filing -> {
			sb.append(String.format("| %s | %s | %s | %s |\n", filing.getSymbol() != null ? filing.getSymbol() : "N/A",
					filing.getFormType() != null ? filing.getFormType() : "N/A", filing.getFormDescription(),
					filing.getFiledDate() != null ? filing.getFiledDate() : "N/A"));
		});

		return sb.toString();
	}

	/**
	 * Check if the client is properly configured.
	 */
	public boolean isConfigured() {
		return config.isConfigured();
	}

}
