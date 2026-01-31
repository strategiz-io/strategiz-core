package io.strategiz.service.dashboard.service;

import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.repository.ReadPortfolioSummaryRepository;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.service.dashboard.model.portfoliosummary.Asset;
import io.strategiz.service.dashboard.model.portfoliosummary.AssetData;
import io.strategiz.service.dashboard.model.portfoliosummary.ExchangeData;
import io.strategiz.service.dashboard.exception.ServiceDashboardErrorDetails;
import io.strategiz.framework.exception.StrategizException;
import io.strategiz.service.base.BaseService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for portfolio summary operations. This service provides portfolio summary data
 * for dashboard visualizations.
 *
 * Data structure: users/{userId}/portfolio/summary ← Aggregated totals (pre-computed)
 * users/{userId}/portfolio/{providerId} ← Provider status (lightweight)
 * users/{userId}/portfolio/{providerId}/holdings/current ← Holdings (heavy data)
 */
@Service
public class PortfolioSummaryService extends BaseService {

	@Override
	protected String getModuleName() {
		return "service-dashboard";
	}

	private final PortfolioManager portfolioManager;

	@Autowired(required = false)
	private ReadPortfolioSummaryRepository readPortfolioSummaryRepository;

	@Autowired(required = false)
	private PortfolioProviderRepository portfolioProviderRepository;

	@Autowired(required = false)
	private ProviderHoldingsRepository providerHoldingsRepository;

	public PortfolioSummaryService(PortfolioManager portfolioManager) {
		this.portfolioManager = portfolioManager;
	}

	/**
	 * Gets portfolio summary data for a user. Uses pre-computed portfolio_summary for
	 * aggregated stats and provider_data for detailed holdings.
	 * @param userId The user ID to get portfolio data for
	 * @return PortfolioSummaryResponse containing portfolio data
	 */
	public PortfolioSummaryResponse getPortfolioSummary(String userId) {
		if (userId == null || userId.isEmpty()) {
			userId = "default-user";
		}

		try {
			log.debug("Getting portfolio summary for user: {}", userId);

			// Create service layer response object
			PortfolioSummaryResponse response = new PortfolioSummaryResponse();
			response.setUserId(userId);

			// 1. Try to read pre-computed portfolio summary
			PortfolioSummaryEntity summary = null;
			if (readPortfolioSummaryRepository != null) {
				try {
					summary = readPortfolioSummaryRepository.getPortfolioSummary(userId);
					log.debug("Found pre-computed portfolio summary for user: {}", userId);
				}
				catch (Exception e) {
					log.warn(
							"Failed to read pre-computed summary for user {}, will fall back to real-time computation: {}",
							userId, e.getMessage());
				}
			}

			// 2. If pre-computed summary exists, use it for aggregated stats
			if (summary != null) {
				// Use pre-computed summary stats
				response.setTotalValue(summary.getTotalValue() != null ? summary.getTotalValue() : BigDecimal.ZERO);
				response.setDailyChange(summary.getDayChange() != null ? summary.getDayChange() : BigDecimal.ZERO);
				response.setDailyChangePercent(
						summary.getDayChangePercent() != null ? summary.getDayChangePercent() : BigDecimal.ZERO);
				response.setWeeklyChange(summary.getWeekChange() != null ? summary.getWeekChange() : BigDecimal.ZERO);
				response.setWeeklyChangePercent(
						summary.getWeekChangePercent() != null ? summary.getWeekChangePercent() : BigDecimal.ZERO);
				response
					.setMonthlyChange(summary.getMonthChange() != null ? summary.getMonthChange() : BigDecimal.ZERO);
				response.setMonthlyChangePercent(
						summary.getMonthChangePercent() != null ? summary.getMonthChangePercent() : BigDecimal.ZERO);
				response.setYearlyChange(BigDecimal.ZERO); // TODO: Add to summary entity
				response.setYearlyChangePercent(BigDecimal.ZERO); // TODO: Add to summary
																	// entity
				response
					.setHasExchangeConnections(summary.getProvidersCount() != null && summary.getProvidersCount() > 0);

				log.debug("Using pre-computed summary stats for user: {}", userId);
			}
			else {
				// Fall back to real-time computation if no pre-computed summary
				log.info("No pre-computed summary found for user {}, computing in real-time", userId);
				PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
				PortfolioMetrics portfolioMetrics = portfolioManager.calculatePortfolioMetrics(portfolioData);

				response.setTotalValue(
						portfolioData.getTotalValue() != null ? portfolioData.getTotalValue() : BigDecimal.ZERO);
				response.setDailyChange(
						portfolioData.getDailyChange() != null ? portfolioData.getDailyChange() : BigDecimal.ZERO);
				response.setDailyChangePercent(portfolioData.getDailyChangePercent() != null
						? portfolioData.getDailyChangePercent() : BigDecimal.ZERO);
				response
					.setWeeklyChange(portfolioMetrics.getPerformance().getOrDefault("weeklyChange", BigDecimal.ZERO));
				response.setWeeklyChangePercent(
						portfolioMetrics.getPerformance().getOrDefault("weeklyChangePercent", BigDecimal.ZERO));
				response
					.setMonthlyChange(portfolioMetrics.getPerformance().getOrDefault("monthlyChange", BigDecimal.ZERO));
				response.setMonthlyChangePercent(
						portfolioMetrics.getPerformance().getOrDefault("monthlyChangePercent", BigDecimal.ZERO));
				response
					.setYearlyChange(portfolioMetrics.getPerformance().getOrDefault("yearlyChange", BigDecimal.ZERO));
				response.setYearlyChangePercent(
						portfolioMetrics.getPerformance().getOrDefault("yearlyChangePercent", BigDecimal.ZERO));
				response.setHasExchangeConnections(
						portfolioData.getExchanges() != null && !portfolioData.getExchanges().isEmpty());
			}

			// 3. Read providers and their holdings for asset/exchange breakdown
			List<PortfolioProviderEntity> providers = null;
			if (portfolioProviderRepository != null) {
				try {
					providers = portfolioProviderRepository.findAllByUserId(userId);
					log.debug("Found {} connected providers for user: {}", providers != null ? providers.size() : 0,
							userId);
				}
				catch (Exception e) {
					log.warn("Failed to read providers for user {}: {}", userId, e.getMessage());
				}
			}

			// 4. Build detailed asset and exchange breakdown
			if (providers != null && !providers.isEmpty()) {
				List<Asset> aggregatedAssets = new ArrayList<>();
				Map<String, ExchangeData> serviceExchanges = new HashMap<>();

				for (PortfolioProviderEntity provider : providers) {
					if (provider == null)
						continue;

					// Create exchange data from provider
					ExchangeData exchangeData = new ExchangeData();
					exchangeData.setName(provider.getProviderId());
					exchangeData
						.setValue(provider.getTotalValue() != null ? provider.getTotalValue() : BigDecimal.ZERO);

					Map<String, AssetData> exchangeAssets = new HashMap<>();

					// Load holdings for this provider (only when needed for detailed
					// view)
					if (providerHoldingsRepository != null) {
						try {
							providerHoldingsRepository.findByUserIdAndProviderId(userId, provider.getProviderId())
								.ifPresent(holdingsEntity -> {
									if (holdingsEntity.getHoldings() != null) {
										for (ProviderHoldingsEntity.Holding holding : holdingsEntity.getHoldings()) {
											if (holding == null)
												continue;

											// Calculate allocation percentage
											BigDecimal allocationPercent = BigDecimal.ZERO;
											if (provider.getTotalValue() != null
													&& provider.getTotalValue().compareTo(BigDecimal.ZERO) > 0
													&& holding.getCurrentValue() != null) {
												allocationPercent = holding.getCurrentValue()
													.divide(provider.getTotalValue(), 4, java.math.RoundingMode.HALF_UP)
													.multiply(new BigDecimal("100"));
											}

											// Add to aggregated assets list
											Asset asset = new Asset();
											asset.setSymbol(holding.getAsset());
											asset.setName(holding.getName());
											asset.setQuantity(holding.getQuantity());
											asset.setValue(holding.getCurrentValue());
											asset.setAllocation(allocationPercent);
											aggregatedAssets.add(asset);

											// Add to exchange assets
											AssetData assetData = new AssetData();
											assetData.setSymbol(holding.getAsset());
											assetData.setName(holding.getName());
											assetData.setQuantity(holding.getQuantity());
											assetData.setPrice(holding.getCurrentPrice());
											assetData.setValue(holding.getCurrentValue());
											assetData.setAllocationPercent(allocationPercent);
											exchangeAssets.put(holding.getAsset(), assetData);
										}
									}
								});
						}
						catch (Exception e) {
							log.warn("Failed to read holdings for provider {} user {}: {}", provider.getProviderId(),
									userId, e.getMessage());
						}
					}

					exchangeData.setAssets(exchangeAssets);
					serviceExchanges.put(provider.getProviderId(), exchangeData);
				}

				response.setAssets(aggregatedAssets);
				response.setExchanges(serviceExchanges);
			}
			else {
				// No providers, set empty collections
				response.setAssets(new ArrayList<>());
				response.setExchanges(new HashMap<>());
			}

			// 5. Portfolio metrics (currently zeros, TODO: implement in
			// PortfolioSummaryManager)
			io.strategiz.service.dashboard.model.portfoliosummary.PortfolioMetrics serviceMetrics = new io.strategiz.service.dashboard.model.portfoliosummary.PortfolioMetrics();
			serviceMetrics.setSharpeRatio(BigDecimal.ZERO);
			serviceMetrics.setBeta(BigDecimal.ZERO);
			serviceMetrics.setAlpha(BigDecimal.ZERO);
			serviceMetrics.setVolatility(BigDecimal.ZERO);
			serviceMetrics.setMaxDrawdown(BigDecimal.ZERO);
			serviceMetrics.setAnnualizedReturn(BigDecimal.ZERO);
			response.setPortfolioMetrics(serviceMetrics);

			log.debug("Successfully built portfolio summary response for user: {}", userId);
			return response;

		}
		catch (StrategizException e) {
			// Re-throw business exceptions
			throw e;
		}
		catch (Exception e) {
			log.error("Error getting portfolio summary for user: {}", userId, e);
			throw new StrategizException(ServiceDashboardErrorDetails.PORTFOLIO_CALCULATION_FAILED, "service-dashboard",
					e, userId, "summary", e.getMessage());
		}
	}

}
