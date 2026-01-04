package io.strategiz.business.aichat.context;

import io.strategiz.service.portfolio.model.response.PortfolioOverviewResponse;
import io.strategiz.service.portfolio.model.response.PortfolioPositionResponse;
import io.strategiz.service.portfolio.service.PortfolioAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides portfolio context data to enrich AI chat responses.
 * Integrates with PortfolioAggregatorService to fetch real-time portfolio data.
 */
@Component
public class PortfolioContextProvider {

	private static final Logger logger = LoggerFactory.getLogger(PortfolioContextProvider.class);

	private final PortfolioAggregatorService portfolioAggregatorService;

	@Autowired
	public PortfolioContextProvider(PortfolioAggregatorService portfolioAggregatorService) {
		this.portfolioAggregatorService = portfolioAggregatorService;
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
			// Fetch complete portfolio overview
			PortfolioOverviewResponse overview = portfolioAggregatorService.getPortfolioOverview(userId);

			if (overview == null) {
				logger.warn("No portfolio overview found for user: {}", userId);
				return createEmptyPortfolioContext();
			}

			// Check if user has portfolio data
			boolean hasPortfolio = overview.getTotalValue() != null
					&& overview.getTotalValue().compareTo(BigDecimal.ZERO) > 0;

			portfolioData.put("hasPortfolio", hasPortfolio);

			if (!hasPortfolio) {
				portfolioData.put("connectedProviders", overview.getProviders() != null ? overview.getProviders().size() : 0);
				return portfolioData;
			}

			// Total portfolio metrics
			portfolioData.put("totalValue", overview.getTotalValue());
			portfolioData.put("dayChange", overview.getDayChange());
			portfolioData.put("dayChangePercent", overview.getDayChangePercent());
			portfolioData.put("totalProfitLoss", overview.getTotalProfitLoss());
			portfolioData.put("totalProfitLossPercent", overview.getTotalProfitLossPercent());
			portfolioData.put("cashBalance", overview.getTotalCashBalance());

			// Asset allocation breakdown
			Map<String, Object> allocation = new HashMap<>();
			if (overview.getAssetAllocation() != null) {
				allocation.put("cryptoPercent", overview.getAssetAllocation().getCryptoPercent());
				allocation.put("stockPercent", overview.getAssetAllocation().getStockPercent());
				allocation.put("forexPercent", overview.getAssetAllocation().getForexPercent());
				allocation.put("cashPercent", overview.getAssetAllocation().getCashPercent());
				allocation.put("otherPercent", overview.getAssetAllocation().getOtherPercent());
			}
			portfolioData.put("allocation", allocation);

			// Top 5 holdings
			List<Map<String, Object>> topHoldings = new ArrayList<>();
			if (overview.getAllPositions() != null && !overview.getAllPositions().isEmpty()) {
				topHoldings = overview.getAllPositions().stream()
						.limit(5)
						.map(this::mapPositionToContext)
						.collect(Collectors.toList());
			}
			portfolioData.put("topHoldings", topHoldings);
			portfolioData.put("totalPositions", overview.getAllPositions() != null ? overview.getAllPositions().size() : 0);

			// Connected providers
			portfolioData.put("connectedProviders", overview.getProviders() != null ? overview.getProviders().size() : 0);

			// Risk metrics (concentration analysis)
			Map<String, Object> riskMetrics = calculateRiskMetrics(overview);
			portfolioData.put("riskMetrics", riskMetrics);

			logger.info("Portfolio context loaded for user {}: totalValue={}, providers={}",
					userId, overview.getTotalValue(), portfolioData.get("connectedProviders"));

		}
		catch (Exception e) {
			logger.error("Error fetching portfolio context for user {}: {}", userId, e.getMessage(), e);
			return createEmptyPortfolioContext();
		}

		return portfolioData;
	}

	/**
	 * Get user's connected providers context
	 * @param userId the user ID
	 * @return Map of provider connection data
	 */
	public Map<String, Object> getProviderContext(String userId) {
		logger.debug("Fetching provider context for user: {}", userId);

		Map<String, Object> providerData = new HashMap<>();

		try {
			PortfolioOverviewResponse overview = portfolioAggregatorService.getPortfolioOverview(userId);

			if (overview == null || overview.getProviders() == null) {
				providerData.put("connectedCount", 0);
				providerData.put("providers", new ArrayList<>());
				return providerData;
			}

			providerData.put("connectedCount", overview.getProviders().size());

			// Map provider summaries
			List<Map<String, Object>> providers = overview.getProviders().stream()
					.map(provider -> {
						Map<String, Object> providerMap = new HashMap<>();
						providerMap.put("id", provider.getProviderId());
						providerMap.put("name", provider.getProviderName());
						providerMap.put("type", provider.getProviderType());
						providerMap.put("category", provider.getProviderCategory());
						providerMap.put("connected", provider.isConnected());
						providerMap.put("totalValue", provider.getTotalValue());
						providerMap.put("positionCount", provider.getPositionCount());
						providerMap.put("syncStatus", provider.getSyncStatus());
						return providerMap;
					})
					.collect(Collectors.toList());

			providerData.put("providers", providers);

		}
		catch (Exception e) {
			logger.error("Error fetching provider context for user {}: {}", userId, e.getMessage(), e);
			providerData.put("connectedCount", 0);
			providerData.put("providers", new ArrayList<>());
		}

		return providerData;
	}

	/**
	 * Map portfolio position to context map
	 */
	private Map<String, Object> mapPositionToContext(PortfolioPositionResponse position) {
		Map<String, Object> positionMap = new HashMap<>();
		positionMap.put("symbol", position.getSymbol());
		positionMap.put("name", position.getName());
		positionMap.put("quantity", position.getQuantity());
		positionMap.put("currentPrice", position.getCurrentPrice());
		positionMap.put("currentValue", position.getCurrentValue());
		positionMap.put("costBasis", position.getCostBasis());
		positionMap.put("profitLoss", position.getProfitLoss());
		positionMap.put("profitLossPercent", position.getProfitLossPercent());
		positionMap.put("assetType", position.getAssetType());
		return positionMap;
	}

	/**
	 * Calculate risk metrics for portfolio context
	 */
	private Map<String, Object> calculateRiskMetrics(PortfolioOverviewResponse overview) {
		Map<String, Object> riskMetrics = new HashMap<>();

		// Concentration risk: Check if any single position exceeds threshold
		BigDecimal concentrationThreshold = new BigDecimal("20.0"); // 20% threshold
		boolean highConcentration = false;
		String largestPosition = null;
		BigDecimal largestPositionPercent = BigDecimal.ZERO;

		if (overview.getAllPositions() != null && overview.getTotalValue() != null
				&& overview.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {

			for (PortfolioPositionResponse position : overview.getAllPositions()) {
				if (position.getCurrentValue() != null) {
					BigDecimal positionPercent = position.getCurrentValue()
							.divide(overview.getTotalValue(), 4, java.math.RoundingMode.HALF_UP)
							.multiply(new BigDecimal("100"));

					if (positionPercent.compareTo(concentrationThreshold) > 0) {
						highConcentration = true;
					}

					if (positionPercent.compareTo(largestPositionPercent) > 0) {
						largestPositionPercent = positionPercent;
						largestPosition = position.getSymbol();
					}
				}
			}
		}

		riskMetrics.put("concentrationRisk", highConcentration ? "HIGH" : "MODERATE");
		riskMetrics.put("largestPosition", largestPosition);
		riskMetrics.put("largestPositionPercent", largestPositionPercent);

		// Diversification score based on number of positions and allocation balance
		int positionCount = overview.getAllPositions() != null ? overview.getAllPositions().size() : 0;
		String diversificationScore;
		if (positionCount >= 15) {
			diversificationScore = "HIGH";
		}
		else if (positionCount >= 5) {
			diversificationScore = "MODERATE";
		}
		else {
			diversificationScore = "LOW";
		}
		riskMetrics.put("diversificationScore", diversificationScore);
		riskMetrics.put("positionCount", positionCount);

		return riskMetrics;
	}

	/**
	 * Create empty portfolio context when no data is available
	 */
	private Map<String, Object> createEmptyPortfolioContext() {
		Map<String, Object> emptyContext = new HashMap<>();
		emptyContext.put("hasPortfolio", false);
		emptyContext.put("connectedProviders", 0);
		emptyContext.put("totalValue", BigDecimal.ZERO);
		emptyContext.put("totalPositions", 0);
		return emptyContext;
	}

}
