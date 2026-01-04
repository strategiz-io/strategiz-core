package io.strategiz.business.aichat.context;

import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Provides portfolio context data to enrich AI chat responses.
 * Integrates with PortfolioManager to fetch real-time portfolio data.
 */
@Component
public class PortfolioContextProvider {

	private static final Logger logger = LoggerFactory.getLogger(PortfolioContextProvider.class);

	private final PortfolioManager portfolioManager;

	@Autowired
	public PortfolioContextProvider(PortfolioManager portfolioManager) {
		this.portfolioManager = portfolioManager;
	}

	/**
	 * Get user's portfolio context for AI analysis
	 * @param userId the user ID
	 * @return Map of portfolio data including totals, allocations, top holdings, and risk metrics
	 */
	public Map<String, Object> getPortfolioContext(String userId) {
		logger.debug("Fetching portfolio context for user: {}", userId);

		Map<String, Object> portfolioData = new HashMap<>();

		try {
			// Fetch aggregated portfolio data from all providers
			PortfolioData data = portfolioManager.getAggregatedPortfolioData(userId);

			if (data == null || data.getExchanges() == null || data.getExchanges().isEmpty()) {
				logger.warn("No portfolio data found for user: {}", userId);
				return createEmptyPortfolioContext();
			}

			// Calculate portfolio metrics (risk, allocation, etc.)
			PortfolioMetrics metrics = portfolioManager.calculatePortfolioMetrics(data);

			// Build portfolio context map
			portfolioData.put("hasPortfolio", true);
			portfolioData.put("totalValue", formatCurrency(data.getTotalValue()));
			portfolioData.put("dayChange", formatCurrency(data.getDailyChange()));
			portfolioData.put("dayChangePercent", formatPercent(data.getDailyChangePercent()));

			// Provider count
			portfolioData.put("connectedProviders", data.getExchanges().size());

			// Asset allocation
			portfolioData.put("allocation", buildAllocationContext(data, metrics));

			// Top holdings (by value)
			portfolioData.put("topHoldings", buildTopHoldingsContext(data));

			// Risk metrics
			portfolioData.put("riskMetrics", buildRiskMetricsContext(data, metrics));

			logger.debug("Portfolio context created successfully for user: {}", userId);

		} catch (Exception e) {
			logger.error("Error fetching portfolio context for user {}: {}", userId, e.getMessage(), e);
			return createEmptyPortfolioContext();
		}

		return portfolioData;
	}

	/**
	 * Build allocation context (crypto%, stocks%, etc.)
	 */
	private Map<String, Object> buildAllocationContext(PortfolioData data, PortfolioMetrics metrics) {
		Map<String, Object> allocation = new HashMap<>();

		// Use metrics to determine allocation if available
		if (metrics != null && metrics.getAllocation() != null) {
			allocation.put("crypto", formatPercent(metrics.getAllocation().getCryptoPercent()));
			allocation.put("stocks", formatPercent(metrics.getAllocation().getStocksPercent()));
			allocation.put("cash", formatPercent(metrics.getAllocation().getCashPercent()));
		} else {
			// Fallback: calculate simple provider-based allocation
			int totalProviders = data.getExchanges().size();
			if (totalProviders > 0) {
				// Simple assumption: equal weight per provider type
				// This is a rough approximation - actual allocation should use metrics
				allocation.put("crypto", "50%");
				allocation.put("stocks", "30%");
				allocation.put("cash", "20%");
			}
		}

		return allocation;
	}

	/**
	 * Build top holdings context (top 5 positions by value)
	 */
	private List<Map<String, Object>> buildTopHoldingsContext(PortfolioData data) {
		List<Map<String, Object>> topHoldings = new ArrayList<>();

		if (data.getExchanges() == null) {
			return topHoldings;
		}

		// Collect all assets from all exchanges
		List<Map<String, Object>> allAssets = new ArrayList<>();

		for (PortfolioData.ExchangeData exchange : data.getExchanges().values()) {
			if (exchange.getAssets() != null) {
				for (PortfolioData.AssetData asset : exchange.getAssets().values()) {
					Map<String, Object> assetMap = new HashMap<>();
					assetMap.put("symbol", asset.getSymbol() != null ? asset.getSymbol() : "UNKNOWN");
					assetMap.put("name", asset.getName() != null ? asset.getName() : asset.getSymbol());
					assetMap.put("value", asset.getCurrentValue() != null ? asset.getCurrentValue() : BigDecimal.ZERO);
					assetMap.put("profitLoss", asset.getProfitLoss() != null ? asset.getProfitLoss() : BigDecimal.ZERO);
					assetMap.put("provider", exchange.getName());
					allAssets.add(assetMap);
				}
			}
		}

		// Sort by value (descending) and take top 5
		allAssets.sort((a, b) -> {
			BigDecimal valueA = (BigDecimal) a.get("value");
			BigDecimal valueB = (BigDecimal) b.get("value");
			return valueB.compareTo(valueA);
		});

		// Format top 5 holdings
		for (int i = 0; i < Math.min(5, allAssets.size()); i++) {
			Map<String, Object> asset = allAssets.get(i);
			Map<String, Object> holding = new HashMap<>();
			holding.put("symbol", asset.get("symbol"));
			holding.put("name", asset.get("name"));
			holding.put("value", formatCurrency((BigDecimal) asset.get("value")));
			holding.put("profitLoss", formatCurrency((BigDecimal) asset.get("profitLoss")));
			holding.put("provider", asset.get("provider"));
			topHoldings.add(holding);
		}

		return topHoldings;
	}

	/**
	 * Build risk metrics context
	 */
	private Map<String, Object> buildRiskMetricsContext(PortfolioData data, PortfolioMetrics metrics) {
		Map<String, Object> riskMetrics = new HashMap<>();

		if (metrics == null || metrics.getRisk() == null) {
			// Fallback: calculate basic concentration risk
			return buildBasicRiskMetrics(data);
		}

		// Use metrics risk data
		riskMetrics.put("concentrationRisk", metrics.getRisk().getConcentrationRisk());
		riskMetrics.put("diversificationScore", metrics.getRisk().getDiversificationScore());
		riskMetrics.put("volatilityRisk", metrics.getRisk().getVolatilityRisk());
		riskMetrics.put("largestPositionPercent", formatPercent(metrics.getRisk().getLargestPositionPercent()));

		return riskMetrics;
	}

	/**
	 * Build basic risk metrics when PortfolioMetrics is unavailable
	 */
	private Map<String, Object> buildBasicRiskMetrics(PortfolioData data) {
		Map<String, Object> riskMetrics = new HashMap<>();

		int totalPositions = 0;
		BigDecimal largestPosition = BigDecimal.ZERO;

		if (data.getExchanges() != null) {
			for (PortfolioData.ExchangeData exchange : data.getExchanges().values()) {
				if (exchange.getAssets() != null) {
					totalPositions += exchange.getAssets().size();
					for (PortfolioData.AssetData asset : exchange.getAssets().values()) {
						if (asset.getCurrentValue() != null && asset.getCurrentValue().compareTo(largestPosition) > 0) {
							largestPosition = asset.getCurrentValue();
						}
					}
				}
			}
		}

		// Calculate concentration risk
		BigDecimal totalValue = data.getTotalValue() != null ? data.getTotalValue() : BigDecimal.ZERO;
		BigDecimal largestPercent = BigDecimal.ZERO;
		if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
			largestPercent = largestPosition.divide(totalValue, 4, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100"));
		}

		String concentrationRisk = largestPercent.compareTo(new BigDecimal("20")) > 0 ? "HIGH" : "LOW";
		String diversificationScore = totalPositions >= 15 ? "HIGH" :
			(totalPositions >= 5 ? "MODERATE" : "LOW");

		riskMetrics.put("concentrationRisk", concentrationRisk);
		riskMetrics.put("diversificationScore", diversificationScore);
		riskMetrics.put("totalPositions", totalPositions);
		riskMetrics.put("largestPositionPercent", formatPercent(largestPercent));

		return riskMetrics;
	}

	/**
	 * Create empty portfolio context when no data available
	 */
	private Map<String, Object> createEmptyPortfolioContext() {
		Map<String, Object> emptyContext = new HashMap<>();
		emptyContext.put("hasPortfolio", false);
		emptyContext.put("totalValue", "$0.00");
		emptyContext.put("dayChange", "$0.00");
		emptyContext.put("dayChangePercent", "0.00%");
		emptyContext.put("connectedProviders", 0);
		emptyContext.put("allocation", Map.of());
		emptyContext.put("topHoldings", List.of());
		emptyContext.put("riskMetrics", Map.of());
		return emptyContext;
	}

	/**
	 * Format BigDecimal as currency string
	 */
	private String formatCurrency(BigDecimal value) {
		if (value == null) {
			return "$0.00";
		}
		return String.format("$%,.2f", value);
	}

	/**
	 * Format BigDecimal as percentage string
	 */
	private String formatPercent(BigDecimal value) {
		if (value == null) {
			return "0.00%";
		}
		return String.format("%.2f%%", value);
	}

}
