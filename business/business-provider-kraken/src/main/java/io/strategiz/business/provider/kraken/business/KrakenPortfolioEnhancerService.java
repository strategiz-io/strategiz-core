package io.strategiz.business.provider.kraken.business;

import io.strategiz.business.provider.kraken.constants.KrakenConstants;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for enhancing Kraken portfolio data retrieved from storage. Applies
 * symbol normalization and data transformations to ensure consistent display.
 *
 * @author Strategiz Platform
 * @since 1.0
 */
@Service
public class KrakenPortfolioEnhancerService {

	private static final Logger log = LoggerFactory.getLogger(KrakenPortfolioEnhancerService.class);

	/**
	 * Enhance portfolio data retrieved from Firebase storage. This method ensures that
	 * raw Kraken symbols are normalized to standard crypto symbols.
	 * @param data The raw portfolio data from storage
	 * @return Enhanced portfolio data with normalized symbols
	 */
	public ProviderDataEntity enhancePortfolioData(ProviderDataEntity data) {
		if (data == null) {
			return null;
		}

		log.debug("Enhancing Kraken portfolio data for provider: {}", data.getProviderId());

		// Only process if this is Kraken data
		if (!KrakenConstants.PROVIDER_ID.equals(data.getProviderId())) {
			return data;
		}

		// Enhance holdings with normalized symbols
		if (data.getHoldings() != null && !data.getHoldings().isEmpty()) {
			List<ProviderDataEntity.Holding> enhancedHoldings = data.getHoldings()
				.stream()
				.map(this::enhanceHolding)
				.collect(Collectors.toList());

			data.setHoldings(enhancedHoldings);

			log.debug("Enhanced {} holdings for Kraken portfolio", enhancedHoldings.size());
		}

		// Data has been enhanced

		return data;
	}

	/**
	 * Enhance individual holding with symbol normalization
	 */
	private ProviderDataEntity.Holding enhanceHolding(ProviderDataEntity.Holding holding) {
		if (holding == null || holding.getAsset() == null) {
			return holding;
		}

		String originalAsset = holding.getAsset();
		String normalizedAsset = normalizeAssetSymbol(originalAsset);

		if (!originalAsset.equals(normalizedAsset)) {
			log.debug("Normalized asset symbol: {} -> {}", originalAsset, normalizedAsset);
			holding.setAsset(normalizedAsset);

			// Update name if it matches the asset code
			if (originalAsset.equals(holding.getName())) {
				holding.setName(getAssetFullName(normalizedAsset));
			}
		}

		return holding;
	}

	/**
	 * Normalize Kraken asset symbol to standard crypto symbol
	 */
	private String normalizeAssetSymbol(String krakenAsset) {
		// First check explicit mappings
		String mapped = KrakenConstants.ASSET_MAPPING.get(krakenAsset);
		if (mapped != null) {
			return mapped;
		}

		// Apply general normalization rules
		if (krakenAsset.startsWith("X") && krakenAsset.length() == 4) {
			// XETH -> ETH, XXBT -> XBT, etc.
			String normalized = krakenAsset.substring(1);
			// Check if the normalized version has a mapping too
			String furtherMapped = KrakenConstants.ASSET_MAPPING.get(normalized);
			return furtherMapped != null ? furtherMapped : normalized;
		}

		if (krakenAsset.startsWith("Z") && krakenAsset.length() == 4) {
			// ZUSD -> USD, ZEUR -> EUR, etc.
			return krakenAsset.substring(1);
		}

		// Handle futures notation (e.g., TRX.F -> TRX)
		if (krakenAsset.endsWith(".F")) {
			return krakenAsset.substring(0, krakenAsset.length() - 2);
		}

		return krakenAsset;
	}

	/**
	 * Get full display name for asset
	 */
	private String getAssetFullName(String asset) {
		// Common crypto asset names
		switch (asset) {
			case "BTC":
				return "Bitcoin";
			case "ETH":
				return "Ethereum";
			case "XRP":
				return "Ripple";
			case "LTC":
				return "Litecoin";
			case "ADA":
				return "Cardano";
			case "DOT":
				return "Polkadot";
			case "LINK":
				return "Chainlink";
			case "DOGE":
				return "Dogecoin";
			case "TRX":
				return "Tron";
			case "PEPE":
				return "Pepe";
			case "USD":
				return "US Dollar";
			case "EUR":
				return "Euro";
			case "GBP":
				return "British Pound";
			default:
				return asset;
		}
	}

}