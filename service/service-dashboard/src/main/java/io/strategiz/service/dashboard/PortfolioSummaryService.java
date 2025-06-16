package io.strategiz.service.dashboard;

import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import io.strategiz.service.dashboard.model.portfoliosummary.PortfolioSummaryResponse;
import io.strategiz.service.dashboard.model.portfoliosummary.Asset;
import io.strategiz.service.dashboard.model.portfoliosummary.AssetData;
import io.strategiz.service.dashboard.model.portfoliosummary.ExchangeData;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for portfolio summary operations.
 * This service provides portfolio summary data for dashboard visualizations.
 */
@Service
public class PortfolioSummaryService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryService.class);

    private final PortfolioManager portfolioManager;

    public PortfolioSummaryService(PortfolioManager portfolioManager) {
        this.portfolioManager = portfolioManager;
    }

    /**
     * Gets portfolio summary data for a user
     * 
     * @param userId The user ID to get portfolio data for
     * @return PortfolioSummaryResponse containing portfolio data
     */
    public PortfolioSummaryResponse getPortfolioSummary(String userId) {
        if (userId == null || userId.isEmpty()) {
            userId = "default-user";
        }
        
        try {
            // Get portfolio data from portfolio manager
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            PortfolioMetrics portfolioMetrics = portfolioManager.calculatePortfolioMetrics(portfolioData);
            
            // Create service layer response object
            PortfolioSummaryResponse response = new PortfolioSummaryResponse();
            
            // Copy scalar fields from business model to service model
            response.setUserId(portfolioData.getUserId());
            response.setTotalValue(portfolioData.getTotalValue() != null ? portfolioData.getTotalValue() : BigDecimal.ZERO);
            response.setDailyChange(portfolioData.getDailyChange() != null ? portfolioData.getDailyChange() : BigDecimal.ZERO);
            response.setDailyChangePercent(portfolioData.getDailyChangePercent() != null ? portfolioData.getDailyChangePercent() : BigDecimal.ZERO);
            response.setWeeklyChange(portfolioMetrics.getPerformance().getOrDefault("weeklyChange", BigDecimal.ZERO));
            response.setWeeklyChangePercent(portfolioMetrics.getPerformance().getOrDefault("weeklyChangePercent", BigDecimal.ZERO));
            response.setMonthlyChange(portfolioMetrics.getPerformance().getOrDefault("monthlyChange", BigDecimal.ZERO));
            response.setMonthlyChangePercent(portfolioMetrics.getPerformance().getOrDefault("monthlyChangePercent", BigDecimal.ZERO));
            response.setYearlyChange(portfolioMetrics.getPerformance().getOrDefault("yearlyChange", BigDecimal.ZERO));
            response.setYearlyChangePercent(portfolioMetrics.getPerformance().getOrDefault("yearlyChangePercent", BigDecimal.ZERO));
            response.setHasExchangeConnections(portfolioData.getExchanges() != null && !portfolioData.getExchanges().isEmpty());
            
            // Convert assets list
            // Convert assets list from all exchanges
            List<Asset> aggregatedAssets = new ArrayList<>();
            if (portfolioData.getExchanges() != null) {
                for (PortfolioData.ExchangeData businessExchange : portfolioData.getExchanges().values()) {
                    if (businessExchange != null && businessExchange.getAssets() != null) {
                        for (PortfolioData.AssetData businessAsset : businessExchange.getAssets().values()) {
                            if (businessAsset == null) continue;
                            Asset serviceAsset = new Asset();
                            serviceAsset.setSymbol(businessAsset.getSymbol());
                            serviceAsset.setName(businessAsset.getName());
                            // serviceAsset.setType(businessAsset.getType()); // Type field might not exist on AssetData
                            serviceAsset.setQuantity(businessAsset.getQuantity());
                            serviceAsset.setValue(businessAsset.getValue());
                            serviceAsset.setAllocation(businessAsset.getAllocationPercent());
                            // serviceAsset.setCurrentPrice(businessAsset.getCurrentPrice()); // CurrentPrice might not exist directly
                            // serviceAsset.setPriceChangePercentage24h(businessAsset.getPriceChangePercentage24h()); // PriceChangePercentage24h might not exist directly
                            // serviceAsset.setExchange(businessExchange.getName()); // Exchange name is part of the parent ExchangeData
                            aggregatedAssets.add(serviceAsset);
                        }
                    }
                }
            }
            response.setAssets(aggregatedAssets);
            
            // Convert exchanges map
            if (portfolioData.getExchanges() != null) {
                Map<String, ExchangeData> serviceExchanges = new HashMap<>();
                portfolioData.getExchanges().forEach((key, businessExchange) -> {
                    ExchangeData serviceExchange = new ExchangeData();
                    serviceExchange.setName(businessExchange.getName());
                    // serviceExchange.setConnected(businessExchange.isConnected()); // Method not available
                    serviceExchange.setValue(businessExchange.getValue() != null ? businessExchange.getValue() : BigDecimal.ZERO);

                    Map<String, AssetData> exchangeAssets = new HashMap<>();
                    if (businessExchange.getAssets() != null) {
                        for (PortfolioData.AssetData bizAssetData : businessExchange.getAssets().values()) {
                            if (bizAssetData == null) continue;
                            AssetData serviceLayerAssetData = new AssetData();
                            serviceLayerAssetData.setSymbol(bizAssetData.getSymbol());
                            serviceLayerAssetData.setName(bizAssetData.getName());
                            serviceLayerAssetData.setQuantity(bizAssetData.getQuantity());
                            serviceLayerAssetData.setPrice(bizAssetData.getPrice());
                            serviceLayerAssetData.setValue(bizAssetData.getValue());
                            serviceLayerAssetData.setAllocationPercent(bizAssetData.getAllocationPercent());
                            // Note: dailyChange and dailyChangePercent might not be on bizAssetData, or need separate fetching
                            exchangeAssets.put(bizAssetData.getSymbol(), serviceLayerAssetData);
                        }
                    }
                    serviceExchange.setAssets(exchangeAssets);
                    serviceExchanges.put(key, serviceExchange);
                });
                response.setExchanges(serviceExchanges);
            } else {
                response.setExchanges(new HashMap<>());
            }
            
            // Convert portfolio metrics
            if (portfolioMetrics != null) {
                io.strategiz.service.dashboard.model.portfoliosummary.PortfolioMetrics serviceMetrics = new io.strategiz.service.dashboard.model.portfoliosummary.PortfolioMetrics();
                serviceMetrics.setSharpeRatio(portfolioMetrics.getRisk().getOrDefault("sharpeRatio", BigDecimal.ZERO));
                serviceMetrics.setBeta(portfolioMetrics.getRisk().getOrDefault("beta", BigDecimal.ZERO));
                serviceMetrics.setAlpha(portfolioMetrics.getPerformance().getOrDefault("alpha", BigDecimal.ZERO));
                serviceMetrics.setVolatility(portfolioMetrics.getRisk().getOrDefault("volatility", BigDecimal.ZERO));
                serviceMetrics.setMaxDrawdown(portfolioMetrics.getRisk().getOrDefault("maxDrawdown", BigDecimal.ZERO));
                serviceMetrics.setAnnualizedReturn(portfolioMetrics.getPerformance().getOrDefault("annualizedReturn", BigDecimal.ZERO));
                response.setPortfolioMetrics(serviceMetrics);
            }
            
            return response;
        } catch (Exception e) {
            log.error("Error getting portfolio summary for user: " + userId, e);
            throw new RuntimeException("Failed to get portfolio summary", e);
        }
    }
}
