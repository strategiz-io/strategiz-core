package io.strategiz.service.dashboard.service;

import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.service.dashboard.model.riskanalysis.RiskAnalysisData;
import io.strategiz.service.dashboard.model.riskanalysis.VolatilityMetric;
import io.strategiz.service.dashboard.model.riskanalysis.DiversificationMetric;
import io.strategiz.service.dashboard.model.riskanalysis.CorrelationMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing portfolio risk within the Dashboard module. This service provides
 * risk calculations, volatility analysis, and diversification metrics.
 */
@Service
public class RiskAnalysisService extends BaseService {

	private static final Logger log = LoggerFactory.getLogger(RiskAnalysisService.class);

	private final PortfolioManager portfolioManager;

	@Autowired
	public RiskAnalysisService(PortfolioManager portfolioManager) {
		this.portfolioManager = portfolioManager;
	}

	@Override
	protected String getModuleName() {
		return "service-dashboard";
	}

	/**
	 * Gets risk analysis data for the user's portfolio
	 * @param userId The user ID to fetch risk data for
	 * @return Risk analysis data
	 */
	public RiskAnalysisData getRiskAnalysis(String userId) {
		log.info("Getting risk analysis for user: {}", userId);

		try {
			// Get portfolio data from the business layer
			PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);

			// Get historical data for volatility calculations - in a real implementation
			// this would come from a database
			List<PortfolioData> historicalData = getHistoricalPortfolioData(userId);

			// Calculate risk metrics
			return calculateRiskMetrics(portfolioData, historicalData);
		}
		catch (Exception e) {
			log.error("Error getting risk analysis for user {}: {}", userId, e.getMessage(), e);
			throw new StrategizException(ServiceDashboardErrorDetails.RISK_CALCULATION_FAILED, "service-dashboard", e,
					userId);
		}
	}

	/**
	 * Gets historical portfolio data for a user. In a real implementation, this would
	 * come from a database of historical snapshots. This is a placeholder implementation
	 * that generates mock historical data.
	 * @param userId The user ID to fetch historical data for
	 * @return List of historical portfolio data points
	 */
	private List<PortfolioData> getHistoricalPortfolioData(String userId) {
		// In a real implementation, this would fetch historical data from a database
		// For now, we'll return a simple mock implementation

		List<PortfolioData> historicalData = new ArrayList<>();
		// Current data is already being used, no need to add it here

		return historicalData;
	}

	/**
	 * Calculates risk metrics from portfolio data
	 * @param portfolioData Current portfolio data
	 * @param historicalData Historical portfolio data
	 * @return Risk analysis data
	 */
	private RiskAnalysisData calculateRiskMetrics(PortfolioData portfolioData, List<PortfolioData> historicalData) {

		RiskAnalysisData response = new RiskAnalysisData();

		// Calculate volatility metrics
		VolatilityMetric volatilityMetric = calculateVolatilityMetrics(historicalData);
		response.setVolatility(volatilityMetric);

		// Calculate diversification metrics
		DiversificationMetric diversificationMetric = calculateDiversificationMetrics(portfolioData);
		response.setDiversification(diversificationMetric);

		// Calculate correlation metrics
		CorrelationMetric correlationMetric = calculateCorrelationMetrics(portfolioData, historicalData);
		response.setCorrelation(correlationMetric);

		return response;
	}

	/**
	 * Calculates volatility metrics from historical portfolio data
	 * @param historicalData Historical portfolio data
	 * @return Volatility metrics
	 */
	private VolatilityMetric calculateVolatilityMetrics(List<PortfolioData> historicalData) {
		VolatilityMetric volatilityMetric = new VolatilityMetric();

		// Default values for empty or insufficient data
		volatilityMetric.setScore(new BigDecimal("50"));
		volatilityMetric.setCategory("Medium");
		volatilityMetric.setStandardDeviation(new BigDecimal("0"));
		volatilityMetric.setMaxDrawdown(new BigDecimal("0"));

		// In a real implementation, we would calculate these metrics from historical data
		// For now, since we're using simulated data, we'll set some reasonable values

		// Simulate medium volatility with some randomization
		double baseVolatility = 0.08; // 8% standard deviation is moderate for crypto
		double randomFactor = 0.02; // Add some randomness

		double randomizedVolatility = baseVolatility + (Math.random() * randomFactor - randomFactor / 2);
		BigDecimal standardDeviation = new BigDecimal(String.valueOf(randomizedVolatility)).setScale(4,
				RoundingMode.HALF_UP);

		volatilityMetric.setStandardDeviation(standardDeviation);

		// Set a reasonable max drawdown value (common in crypto markets)
		double drawdown = 0.15 + (Math.random() * 0.10); // 15-25% max drawdown
		BigDecimal maxDrawdown = new BigDecimal(String.valueOf(drawdown)).multiply(new BigDecimal("100")) // Convert
																											// to
																											// percentage
			.setScale(2, RoundingMode.HALF_UP);

		volatilityMetric.setMaxDrawdown(maxDrawdown);

		// Set volatility score based on standard deviation
		BigDecimal score;
		String category;

		if (standardDeviation.compareTo(new BigDecimal("0.05")) < 0) {
			score = new BigDecimal("25");
			category = "Low";
		}
		else if (standardDeviation.compareTo(new BigDecimal("0.10")) < 0) {
			score = new BigDecimal("50");
			category = "Medium";
		}
		else if (standardDeviation.compareTo(new BigDecimal("0.20")) < 0) {
			score = new BigDecimal("75");
			category = "High";
		}
		else {
			score = new BigDecimal("100");
			category = "Very High";
		}

		volatilityMetric.setScore(score);
		volatilityMetric.setCategory(category);

		return volatilityMetric;
	}

	/**
	 * Calculates diversification metrics from portfolio data
	 * @param portfolioData Current portfolio data
	 * @return Diversification metrics
	 */
	private DiversificationMetric calculateDiversificationMetrics(PortfolioData portfolioData) {
		DiversificationMetric diversificationMetric = new DiversificationMetric();

		// Default values for empty or insufficient data
		diversificationMetric.setScore(new BigDecimal("50"));
		diversificationMetric.setCategory("Medium");
		diversificationMetric.setAssetCount(0);
		diversificationMetric.setConcentrationIndex(new BigDecimal("0"));
		diversificationMetric.setLargestAllocation(new BigDecimal("0"));

		if (portfolioData == null || portfolioData.getExchanges() == null || portfolioData.getExchanges().isEmpty()) {
			return diversificationMetric;
		}

		try {
			// Calculate values for each asset across all exchanges
			BigDecimal totalValue = portfolioData.getTotalValue();
			Map<String, BigDecimal> assetValues = new HashMap<>();
			int assetCount = 0;

			// Calculate values for each asset across all exchanges
			for (PortfolioData.ExchangeData exchangeData : portfolioData.getExchanges().values()) {
				if (exchangeData.getAssets() != null) {
					for (PortfolioData.AssetData assetData : exchangeData.getAssets().values()) {
						if (assetData.getValue() != null && assetData.getValue().compareTo(BigDecimal.ZERO) > 0) {
							String assetKey = assetData.getSymbol();

							if (assetValues.containsKey(assetKey)) {
								assetValues.put(assetKey, assetValues.get(assetKey).add(assetData.getValue()));
							}
							else {
								assetValues.put(assetKey, assetData.getValue());
								assetCount++;
							}
						}
					}
				}
			}

			diversificationMetric.setAssetCount(assetCount);

			// Find largest allocation
			BigDecimal largestValue = BigDecimal.ZERO;
			for (BigDecimal value : assetValues.values()) {
				if (value.compareTo(largestValue) > 0) {
					largestValue = value;
				}
			}

			// Calculate largest allocation percentage
			if (largestValue.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal largestPercentage = largestValue.divide(totalValue, 6, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100"))
					.setScale(2, RoundingMode.HALF_UP);

				diversificationMetric.setLargestAllocation(largestPercentage);
			}

			// Calculate Herfindahl-Hirschman Index (HHI) - a measure of concentration
			BigDecimal hhi = BigDecimal.ZERO;
			for (BigDecimal value : assetValues.values()) {
				BigDecimal percentage = value.divide(totalValue, 6, RoundingMode.HALF_UP);
				hhi = hhi.add(percentage.multiply(percentage));
			}

			diversificationMetric.setConcentrationIndex(hhi.setScale(4, RoundingMode.HALF_UP));

			// Set diversification score based on number of assets and concentration
			BigDecimal score;
			String category;

			if (assetCount < 3) {
				score = new BigDecimal("25");
				category = "Low";
			}
			else if (assetCount < 5) {
				score = new BigDecimal("50");
				category = "Medium";
			}
			else if (assetCount < 10) {
				score = new BigDecimal("75");
				category = "High";
			}
			else {
				score = new BigDecimal("100");
				category = "Very High";
			}

			// Adjust the score based on concentration
			// Lower HHI means better diversification
			if (hhi.compareTo(new BigDecimal("0.5")) > 0) {
				score = score.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP);
			}
			else if (hhi.compareTo(new BigDecimal("0.3")) > 0) {
				score = score.multiply(new BigDecimal("0.7")).setScale(0, RoundingMode.HALF_UP);
			}
			else if (hhi.compareTo(new BigDecimal("0.2")) > 0) {
				score = score.multiply(new BigDecimal("0.9")).setScale(0, RoundingMode.HALF_UP);
			}

			// Recategorize based on the adjusted score
			if (score.compareTo(new BigDecimal("25")) <= 0) {
				category = "Low";
			}
			else if (score.compareTo(new BigDecimal("50")) <= 0) {
				category = "Medium";
			}
			else if (score.compareTo(new BigDecimal("75")) <= 0) {
				category = "High";
			}
			else {
				category = "Very High";
			}

			diversificationMetric.setScore(score);
			diversificationMetric.setCategory(category);
		}
		catch (Exception e) {
			log.error("Error calculating diversification metrics: {}", e.getMessage(), e);
		}

		return diversificationMetric;
	}

	/**
	 * Calculates correlation metrics from portfolio and historical data
	 * @param portfolioData Current portfolio data
	 * @param historicalData Historical portfolio data
	 * @return Correlation metrics
	 */
	private CorrelationMetric calculateCorrelationMetrics(PortfolioData portfolioData,
			List<PortfolioData> historicalData) {

		CorrelationMetric correlationMetric = new CorrelationMetric();

		// Default values
		correlationMetric.setAverageCorrelation(new BigDecimal("0.5"));
		correlationMetric.setCategory("Moderate");

		// For a proper correlation calculation, we'd need historical data for each asset
		// This is a simplified implementation

		// Set default or mock values based on asset types
		if (portfolioData != null && portfolioData.getExchanges() != null && !portfolioData.getExchanges().isEmpty()) {
			boolean hasCrypto = false;
			boolean hasStocks = false;

			for (PortfolioData.ExchangeData exchangeData : portfolioData.getExchanges().values()) {
				String exchangeName = exchangeData.getName().toLowerCase();

				// Check if this is a crypto exchange
				if (exchangeName.contains("coinbase") || exchangeName.contains("binance")
						|| exchangeName.contains("kraken")) {
					hasCrypto = true;
				}

				// Check if this is a stock exchange
				if (exchangeName.contains("robinhood") || exchangeName.contains("etrade")
						|| exchangeName.contains("schwab") || exchangeName.contains("fidelity")) {
					hasStocks = true;
				}
			}

			// If portfolio has both crypto and stocks, assume lower correlation
			if (hasCrypto && hasStocks) {
				correlationMetric.setAverageCorrelation(new BigDecimal("0.3"));
				correlationMetric.setCategory("Low");
			}
			// If portfolio has only crypto, assume higher correlation
			else if (hasCrypto) {
				correlationMetric.setAverageCorrelation(new BigDecimal("0.7"));
				correlationMetric.setCategory("High");
			}
		}

		return correlationMetric;
	}

	/**
	 * Calculates the mean of a list of decimal values
	 * @param values List of decimal values
	 * @return Mean value
	 */
	private BigDecimal calculateMean(List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return BigDecimal.ZERO;
		}

		BigDecimal sum = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			sum = sum.add(value);
		}

		return sum.divide(new BigDecimal(values.size()), 6, RoundingMode.HALF_UP);
	}

	/**
	 * Calculates the variance of a list of decimal values
	 * @param values List of decimal values
	 * @param mean Mean value
	 * @return Variance
	 */
	private BigDecimal calculateVariance(List<BigDecimal> values, BigDecimal mean) {
		if (values == null || values.isEmpty() || mean == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal sumSquaredDifferences = BigDecimal.ZERO;
		for (BigDecimal value : values) {
			BigDecimal difference = value.subtract(mean);
			sumSquaredDifferences = sumSquaredDifferences.add(difference.multiply(difference));
		}

		return sumSquaredDifferences.divide(new BigDecimal(values.size()), 6, RoundingMode.HALF_UP);
	}

	/**
	 * Calculates the maximum drawdown from historical portfolio data
	 * @param historicalData Historical portfolio data
	 * @return Maximum drawdown as a percentage
	 */
	private BigDecimal calculateMaxDrawdown(List<PortfolioData> historicalData) {
		if (historicalData == null || historicalData.size() < 2) {
			return BigDecimal.ZERO;
		}

		BigDecimal maxValue = BigDecimal.ZERO;
		BigDecimal maxDrawdown = BigDecimal.ZERO;

		for (PortfolioData data : historicalData) {
			if (data.getTotalValue() != null && data.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
				if (data.getTotalValue().compareTo(maxValue) > 0) {
					maxValue = data.getTotalValue();
				}
				else if (maxValue.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal drawdown = maxValue.subtract(data.getTotalValue())
						.divide(maxValue, 6, RoundingMode.HALF_UP)
						.multiply(new BigDecimal("100"))
						.setScale(2, RoundingMode.HALF_UP);

					if (drawdown.compareTo(maxDrawdown) > 0) {
						maxDrawdown = drawdown;
					}
				}
			}
		}

		return maxDrawdown;
	}

}
