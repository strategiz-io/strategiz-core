package io.strategiz.business.provider.kraken.helper;

import io.strategiz.client.fmp.client.FmpQuoteClient;
import io.strategiz.client.fmp.dto.FmpQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for fetching and processing prices from FMP (Financial Modeling Prep). Contains
 * the business logic for transforming raw API data into usable price information.
 */
@Service
public class FmpPriceService {

	private static final Logger log = LoggerFactory.getLogger(FmpPriceService.class);

	private final FmpQuoteClient fmpQuoteClient;

	@Autowired
	public FmpPriceService(@Autowired(required = false) FmpQuoteClient fmpQuoteClient) {
		this.fmpQuoteClient = fmpQuoteClient;
	}

	/**
	 * Get bulk prices for multiple symbols. Handles symbol conversion and price
	 * extraction from FMP API responses.
	 * @param symbols List of symbols (can be in various formats)
	 * @return Map of original symbol to price in USD
	 */
	public Map<String, Double> getBulkPrices(List<String> symbols) {
		if (fmpQuoteClient == null || symbols == null || symbols.isEmpty()) {
			return new HashMap<>();
		}

		// Convert symbols to FMP format (e.g., BTC -> BTCUSD for crypto)
		List<String> fmpSymbols = symbols.stream().map(this::toFmpSymbol).collect(Collectors.toList());

		log.debug("Fetching bulk prices for symbols: {}", fmpSymbols);

		try {
			List<FmpQuote> quotes = fmpQuoteClient.getBatchQuotes(fmpSymbols);

			Map<String, Double> prices = new HashMap<>();
			for (FmpQuote quote : quotes) {
				if (quote != null && quote.getPrice() != null) {
					// Map back to original symbol
					String originalSymbol = findOriginalSymbol(symbols, quote.getSymbol());
					prices.put(originalSymbol, quote.getPrice().doubleValue());
				}
			}

			log.info("Successfully fetched {} prices out of {} requested", prices.size(), symbols.size());
			return prices;

		}
		catch (Exception e) {
			log.error("Error fetching prices from FMP: {}", e.getMessage());
			return new HashMap<>();
		}
	}

	/**
	 * Convert a standard symbol to FMP format. For crypto symbols, append USD (e.g., BTC
	 * -> BTCUSD). Strips dashes (BTC-USD -> BTCUSD).
	 */
	private String toFmpSymbol(String symbol) {
		if (symbol == null) {
			return "";
		}
		// Strip dashes (handles BTC-USD format)
		String clean = symbol.replace("-", "");
		// If it doesn't already end with USD, append it (for crypto)
		if (!clean.toUpperCase().endsWith("USD") && !clean.contains(".")) {
			return clean.toUpperCase() + "USD";
		}
		return clean.toUpperCase();
	}

	/**
	 * Find the original symbol from the list that corresponds to the FMP symbol.
	 */
	private String findOriginalSymbol(List<String> originalSymbols, String fmpSymbol) {
		for (String original : originalSymbols) {
			String converted = toFmpSymbol(original);
			if (converted.equalsIgnoreCase(fmpSymbol)) {
				return original;
			}
		}
		// If no match found, strip USD suffix if present
		if (fmpSymbol != null && fmpSymbol.endsWith("USD")) {
			String base = fmpSymbol.substring(0, fmpSymbol.length() - 3);
			for (String original : originalSymbols) {
				if (original.equalsIgnoreCase(base)) {
					return original;
				}
			}
		}
		return fmpSymbol;
	}

}
