package io.strategiz.business.provider.kraken.enrichment.enricher;

import io.strategiz.business.provider.kraken.enrichment.constants.KrakenEnrichmentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Enricher responsible for normalizing Kraken-specific symbols to standard symbols.
 * Handles various Kraken naming conventions including X/Z prefixes, .F futures, and .S
 * staking suffixes.
 *
 * @author Strategiz Platform
 * @since 1.0
 */
@Component
public class KrakenSymbolEnricher {

	private static final Logger log = LoggerFactory.getLogger(KrakenSymbolEnricher.class);

	/**
	 * Normalize all symbols in the balance map from Kraken format to standard format.
	 * @param rawBalances Map of Kraken symbols to quantities
	 * @return Map with normalized symbols as keys
	 */
	public Map<String, Object> normalizeSymbols(Map<String, Object> rawBalances) {
		Map<String, Object> normalizedBalances = new HashMap<>();

		for (Map.Entry<String, Object> entry : rawBalances.entrySet()) {
			String krakenSymbol = entry.getKey();
			Object quantity = entry.getValue();

			String normalizedSymbol = normalizeSymbol(krakenSymbol);

			if (!krakenSymbol.equals(normalizedSymbol)) {
				log.debug("Normalized symbol: {} -> {}", krakenSymbol, normalizedSymbol);
			}

			// If we already have this normalized symbol (e.g., staked and unstaked
			// versions),
			// we need to handle it appropriately
			if (normalizedBalances.containsKey(normalizedSymbol)) {
				// This might happen with staked assets - keep them separate with suffix
				if (isStakedAsset(krakenSymbol)) {
					normalizedSymbol = normalizedSymbol + "_STAKED";
				}
			}

			normalizedBalances.put(normalizedSymbol, quantity);
		}

		return normalizedBalances;
	}

	/**
	 * Normalize a single Kraken symbol to standard format.
	 * @param krakenSymbol The Kraken-specific symbol
	 * @return The normalized symbol
	 */
	public String normalizeSymbol(String krakenSymbol) {
		if (krakenSymbol == null || krakenSymbol.isEmpty()) {
			return krakenSymbol;
		}

		// First check if we have an explicit mapping
		// Create inline mapping for common Kraken symbols
		Map<String, String> assetMapping = Map.ofEntries(Map.entry("XXBT", "BTC"), Map.entry("XBT", "BTC"),
				Map.entry("XETH", "ETH"), Map.entry("ETH2", "ETH"), // ETH2 is staked ETH
				Map.entry("ETH2.S", "ETH"), // ETH2.S is also staked ETH
				Map.entry("XXRP", "XRP"), Map.entry("XXLM", "XLM"), Map.entry("XLTC", "LTC"),
				Map.entry("XXDG", "DOGE"));
		String mapped = assetMapping.get(krakenSymbol);
		if (mapped != null) {
			return mapped;
		}

		String symbol = krakenSymbol;

		// Handle staking suffix (.S)
		boolean isStaked = false;
		if (symbol.endsWith(".S")) {
			symbol = symbol.substring(0, symbol.length() - 2);
			isStaked = true;
		}

		// Handle futures suffix (.F)
		if (symbol.endsWith(".F")) {
			symbol = symbol.substring(0, symbol.length() - 2);
		}

		// Handle X prefix for crypto (XXBT -> XBT, XETH -> ETH)
		if (symbol.startsWith("X") && symbol.length() == 4) {
			symbol = symbol.substring(1);

			// Check if the result needs further mapping (XBT -> BTC)
			// Use existing mapping variable
			mapped = assetMapping.get(symbol);
			if (mapped != null) {
				symbol = mapped;
			}
		}

		// Handle Z prefix for fiat (ZUSD -> USD, ZEUR -> EUR)
		if (symbol.startsWith("Z") && symbol.length() == 4) {
			symbol = symbol.substring(1);
		}

		return symbol;
	}

	/**
	 * Check if an asset is a staked version based on Kraken naming.
	 * @param krakenSymbol The Kraken symbol
	 * @return true if it's a staked asset
	 */
	public boolean isStakedAsset(String krakenSymbol) {
		return krakenSymbol != null && (krakenSymbol.endsWith(".S") || // Liquid staking
				krakenSymbol.endsWith(".F") // Futures/staking rewards
		);
	}

	/**
	 * Get the staking type from a Kraken symbol.
	 * @param krakenSymbol The Kraken symbol
	 * @return The staking type or null if not staked
	 */
	public String getStakingType(String krakenSymbol) {
		if (krakenSymbol == null) {
			return null;
		}

		if (krakenSymbol.endsWith(".S")) {
			return "liquid_staking";
		}
		else if (krakenSymbol.endsWith(".F")) {
			return "futures_staking";
		}

		return null;
	}

	/**
	 * Check if a symbol represents a cash/fiat currency.
	 * @param symbol The symbol to check (can be Kraken or normalized)
	 * @return true if it's a cash asset
	 */
	public boolean isCashAsset(String symbol) {
		return KrakenEnrichmentConstants.CASH_ASSETS.contains(symbol)
				|| KrakenEnrichmentConstants.CASH_ASSETS.contains("Z" + symbol); // Check
																					// with
																					// Z
																					// prefix
	}

}