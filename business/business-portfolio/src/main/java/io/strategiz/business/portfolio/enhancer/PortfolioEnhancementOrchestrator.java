package io.strategiz.business.portfolio.enhancer;

import io.strategiz.business.portfolio.enhancer.model.EnhancedPortfolio;
import io.strategiz.business.portfolio.enhancer.provider.KrakenPortfolioEnhancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Orchestrates portfolio enhancement across multiple providers. Routes enhancement
 * requests to appropriate provider-specific enhancers and aggregates results when needed.
 */
@Component
public class PortfolioEnhancementOrchestrator {

	private static final Logger LOGGER = Logger.getLogger(PortfolioEnhancementOrchestrator.class.getName());

	private final KrakenPortfolioEnhancer krakenEnhancer;

	// Add more provider enhancers as they are implemented
	// private final CoinbasePortfolioEnhancer coinbaseEnhancer;
	// private final BinancePortfolioEnhancer binanceEnhancer;
	// private final AlpacaPortfolioEnhancer alpacaEnhancer;

	@Autowired
	public PortfolioEnhancementOrchestrator(KrakenPortfolioEnhancer krakenEnhancer) {
		this.krakenEnhancer = krakenEnhancer;
	}

	/**
	 * Enhance portfolio data from a specific provider
	 * @param userId User ID
	 * @param providerName Provider name (e.g., "kraken", "coinbase")
	 * @param rawData Raw portfolio data from provider
	 * @return Enhanced portfolio with metadata and prices
	 */
	public EnhancedPortfolio enhanceProviderPortfolio(String userId, String providerName, Map<String, Object> rawData) {
		LOGGER.info("Orchestrating enhancement for " + providerName + " portfolio");

		switch (providerName.toLowerCase()) {
			case "kraken":
				return enhanceKrakenPortfolio(userId, rawData);
			case "coinbase":
				return enhanceCoinbasePortfolio(userId, rawData);
			case "binance":
			case "binanceus":
				return enhanceBinancePortfolio(userId, rawData);
			case "alpaca":
				return enhanceAlpacaPortfolio(userId, rawData);
			default:
				LOGGER.warning("No enhancer available for provider: " + providerName);
				return createEmptyPortfolio(userId, providerName);
		}
	}

	/**
	 * Enhance portfolios from multiple providers and aggregate
	 * @param userId User ID
	 * @param providerData Map of provider name to raw data
	 * @return List of enhanced portfolios
	 */
	public List<EnhancedPortfolio> enhanceMultiplePortfolios(String userId,
			Map<String, Map<String, Object>> providerData) {
		List<EnhancedPortfolio> enhancedPortfolios = new ArrayList<>();

		for (Map.Entry<String, Map<String, Object>> entry : providerData.entrySet()) {
			String providerName = entry.getKey();
			Map<String, Object> rawData = entry.getValue();

			try {
				EnhancedPortfolio enhanced = enhanceProviderPortfolio(userId, providerName, rawData);
				enhancedPortfolios.add(enhanced);
			}
			catch (Exception e) {
				LOGGER.warning("Failed to enhance portfolio for " + providerName + ": " + e.getMessage());
			}
		}

		return enhancedPortfolios;
	}

	/**
	 * Aggregate multiple enhanced portfolios into a single view
	 * @param portfolios List of enhanced portfolios from different providers
	 * @return Aggregated portfolio
	 */
	public EnhancedPortfolio aggregatePortfolios(List<EnhancedPortfolio> portfolios) {
		if (portfolios == null || portfolios.isEmpty()) {
			return new EnhancedPortfolio();
		}

		if (portfolios.size() == 1) {
			return portfolios.get(0);
		}

		// Create aggregated portfolio
		EnhancedPortfolio aggregated = new EnhancedPortfolio();
		aggregated.setProviderId("aggregated");
		aggregated.setProviderName("All Providers");

		// Combine all assets from all portfolios
		for (EnhancedPortfolio portfolio : portfolios) {
			if (portfolio.getAssets() != null) {
				portfolio.getAssets().forEach(aggregated::addAsset);
			}
		}

		// Recalculate metrics for the aggregated portfolio
		aggregated.calculateMetrics();

		return aggregated;
	}

	/**
	 * Enhance Kraken portfolio
	 */
	@SuppressWarnings("unchecked")
	private EnhancedPortfolio enhanceKrakenPortfolio(String userId, Map<String, Object> rawData) {
		// Extract balances from raw data
		Map<String, BigDecimal> balances = extractBalances(rawData);
		return krakenEnhancer.enhance(userId, balances);
	}

	/**
	 * Enhance Coinbase portfolio (placeholder)
	 */
	private EnhancedPortfolio enhanceCoinbasePortfolio(String userId, Map<String, Object> rawData) {
		// TODO: Implement when CoinbasePortfolioEnhancer is created
		LOGGER.info("Coinbase enhancement not yet implemented");
		return createEmptyPortfolio(userId, "coinbase");
	}

	/**
	 * Enhance Binance portfolio (placeholder)
	 */
	private EnhancedPortfolio enhanceBinancePortfolio(String userId, Map<String, Object> rawData) {
		// TODO: Implement when BinancePortfolioEnhancer is created
		LOGGER.info("Binance enhancement not yet implemented");
		return createEmptyPortfolio(userId, "binance");
	}

	/**
	 * Enhance Alpaca portfolio (placeholder)
	 */
	private EnhancedPortfolio enhanceAlpacaPortfolio(String userId, Map<String, Object> rawData) {
		// TODO: Implement when AlpacaPortfolioEnhancer is created
		LOGGER.info("Alpaca enhancement not yet implemented");
		return createEmptyPortfolio(userId, "alpaca");
	}

	/**
	 * Extract balances from raw provider data
	 */
	@SuppressWarnings("unchecked")
	private Map<String, BigDecimal> extractBalances(Map<String, Object> rawData) {
		// Look for common balance field names
		if (rawData.containsKey("balances")) {
			Object balances = rawData.get("balances");
			if (balances instanceof Map) {
				return convertToBalanceMap((Map<String, Object>) balances);
			}
		}

		if (rawData.containsKey("assets")) {
			Object assets = rawData.get("assets");
			if (assets instanceof Map) {
				return convertToBalanceMap((Map<String, Object>) assets);
			}
		}

		// If the raw data itself is the balance map
		return convertToBalanceMap(rawData);
	}

	/**
	 * Convert raw balance data to BigDecimal map
	 */
	private Map<String, BigDecimal> convertToBalanceMap(Map<String, Object> raw) {
		Map<String, BigDecimal> balances = new java.util.HashMap<>();

		for (Map.Entry<String, Object> entry : raw.entrySet()) {
			String symbol = entry.getKey();
			Object value = entry.getValue();

			BigDecimal balance = null;
			if (value instanceof BigDecimal) {
				balance = (BigDecimal) value;
			}
			else if (value instanceof Number) {
				balance = new BigDecimal(value.toString());
			}
			else if (value instanceof String) {
				try {
					balance = new BigDecimal((String) value);
				}
				catch (NumberFormatException e) {
					LOGGER.warning("Invalid balance value for " + symbol + ": " + value);
				}
			}

			if (balance != null && balance.compareTo(BigDecimal.ZERO) > 0) {
				balances.put(symbol, balance);
			}
		}

		return balances;
	}

	/**
	 * Create an empty portfolio
	 */
	private EnhancedPortfolio createEmptyPortfolio(String userId, String providerName) {
		EnhancedPortfolio portfolio = new EnhancedPortfolio();
		portfolio.setUserId(userId);
		portfolio.setProviderId(providerName);
		portfolio.setProviderName(providerName);
		return portfolio;
	}

	/**
	 * Check if enhancement is available for a provider
	 */
	public boolean isEnhancementAvailable(String providerName) {
		switch (providerName.toLowerCase()) {
			case "kraken":
				return true;
			case "coinbase":
			case "binance":
			case "binanceus":
			case "alpaca":
				// Will return true when enhancers are implemented
				return false;
			default:
				return false;
		}
	}

}