package io.strategiz.business.portfolio;

import io.strategiz.business.portfolio.exception.PortfolioErrorDetails;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
import io.strategiz.framework.exception.StrategizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business logic for portfolio operations.
 * Aggregates portfolio data from multiple providers stored in Firestore.
 * Implements clean architecture pattern - reads from data layer, performs business logic.
 */
@Component
public class PortfolioManager {

    private static final Logger log = LoggerFactory.getLogger(PortfolioManager.class);

    private final ReadProviderDataRepository providerDataRepository;

    @Autowired
    public PortfolioManager(ReadProviderDataRepository providerDataRepository) {
        this.providerDataRepository = providerDataRepository;
        log.info("PortfolioManager initialized with provider data repository");
    }

    /**
     * Aggregates portfolio data from ALL connected providers.
     * Reads provider data from Firestore (already synced by provider sync processes).
     *
     * @param userId The user ID to fetch portfolio data for
     * @return Aggregated portfolio data from all providers
     */
    public PortfolioData getAggregatedPortfolioData(String userId) {
        log.info("Getting aggregated portfolio data for user: {}", userId);

        try {
            // 1. Read ALL provider data from Firestore
            List<ProviderDataEntity> providers = providerDataRepository.getAllProviderData(userId);

            if (providers == null || providers.isEmpty()) {
                log.info("No provider data found for user: {}", userId);
                return createEmptyPortfolioData(userId);
            }

            log.info("Found {} provider(s) for user: {}", providers.size(), userId);

            // 2. Initialize aggregation variables
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalDayChange = BigDecimal.ZERO;
            BigDecimal totalProfitLoss = BigDecimal.ZERO;
            BigDecimal totalCashBalance = BigDecimal.ZERO;
            Map<String, PortfolioData.ExchangeData> exchanges = new HashMap<>();

            // 3. Aggregate data from each provider
            for (ProviderDataEntity provider : providers) {
                try {
                    log.debug("Processing provider: {} for user: {}", provider.getProviderId(), userId);

                    // Sum up totals
                    if (provider.getTotalValue() != null) {
                        totalValue = totalValue.add(provider.getTotalValue());
                    }

                    if (provider.getDayChange() != null) {
                        totalDayChange = totalDayChange.add(provider.getDayChange());
                    }

                    if (provider.getTotalProfitLoss() != null) {
                        totalProfitLoss = totalProfitLoss.add(provider.getTotalProfitLoss());
                    }

                    if (provider.getCashBalance() != null) {
                        totalCashBalance = totalCashBalance.add(provider.getCashBalance());
                    }

                    // Build exchange data for this provider
                    PortfolioData.ExchangeData exchangeData = buildExchangeData(provider);
                    exchanges.put(provider.getProviderId(), exchangeData);

                    log.debug("Processed provider {}: totalValue={}, dayChange={}",
                            provider.getProviderId(), provider.getTotalValue(), provider.getDayChange());

                } catch (Exception e) {
                    log.error("Error processing provider {} for user {}: {}",
                            provider.getProviderId(), userId, e.getMessage(), e);
                    // Continue processing other providers even if one fails
                }
            }

            // 4. Calculate aggregated percentages
            BigDecimal dayChangePercent = calculatePercentage(totalDayChange, totalValue);
            BigDecimal profitLossPercent = calculatePercentage(totalProfitLoss, totalValue);

            // 5. Build aggregated portfolio data
            PortfolioData portfolioData = new PortfolioData();
            portfolioData.setUserId(userId);
            portfolioData.setTotalValue(totalValue);
            portfolioData.setDailyChange(totalDayChange);
            portfolioData.setDailyChangePercent(dayChangePercent);
            portfolioData.setExchanges(exchanges);

            log.info("Aggregated portfolio for user {}: totalValue={}, dayChange={} ({}%), {} exchanges",
                    userId, totalValue, totalDayChange, dayChangePercent, exchanges.size());

            return portfolioData;

        } catch (Exception e) {
            log.error("Error getting aggregated portfolio data for user {}: {}", userId, e.getMessage(), e);
            throw new StrategizException(PortfolioErrorDetails.PORTFOLIO_DATA_RETRIEVAL_FAILED, "business-portfolio", e, userId);
        }
    }
    
    /**
     * Builds ExchangeData from ProviderDataEntity
     *
     * @param provider Provider data entity from Firestore
     * @return Exchange data for portfolio response
     */
    private PortfolioData.ExchangeData buildExchangeData(ProviderDataEntity provider) {
        PortfolioData.ExchangeData exchangeData = new PortfolioData.ExchangeData();
        exchangeData.setId(provider.getProviderId());
        exchangeData.setName(provider.getProviderName() != null ? provider.getProviderName() : provider.getProviderId());
        exchangeData.setValue(provider.getTotalValue() != null ? provider.getTotalValue() : BigDecimal.ZERO);

        // Convert holdings to assets map
        Map<String, PortfolioData.AssetData> assets = new HashMap<>();
        if (provider.getHoldings() != null) {
            for (ProviderDataEntity.Holding holding : provider.getHoldings()) {
                try {
                    PortfolioData.AssetData assetData = new PortfolioData.AssetData();
                    assetData.setSymbol(holding.getAsset());
                    assetData.setName(holding.getName() != null ? holding.getName() : holding.getAsset());
                    assetData.setQuantity(holding.getQuantity() != null ? holding.getQuantity() : BigDecimal.ZERO);
                    assetData.setPrice(holding.getCurrentPrice() != null ? holding.getCurrentPrice() : BigDecimal.ZERO);
                    assetData.setValue(holding.getCurrentValue() != null ? holding.getCurrentValue() : BigDecimal.ZERO);

                    // Calculate allocation percentage
                    if (provider.getTotalValue() != null && provider.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal allocation = assetData.getValue()
                                .divide(provider.getTotalValue(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        assetData.setAllocationPercent(allocation);
                    } else {
                        assetData.setAllocationPercent(BigDecimal.ZERO);
                    }

                    assets.put(holding.getAsset(), assetData);
                } catch (Exception e) {
                    log.warn("Error converting holding {} from provider {}: {}",
                            holding.getAsset(), provider.getProviderId(), e.getMessage());
                }
            }
        }
        exchangeData.setAssets(assets);

        return exchangeData;
    }

    /**
     * Creates empty portfolio data for users with no connected providers
     *
     * @param userId User ID
     * @return Empty portfolio data
     */
    private PortfolioData createEmptyPortfolioData(String userId) {
        PortfolioData portfolioData = new PortfolioData();
        portfolioData.setUserId(userId);
        portfolioData.setTotalValue(BigDecimal.ZERO);
        portfolioData.setDailyChange(BigDecimal.ZERO);
        portfolioData.setDailyChangePercent(BigDecimal.ZERO);
        portfolioData.setExchanges(new HashMap<>());
        return portfolioData;
    }

    /**
     * Calculates percentage safely (handles division by zero)
     *
     * @param change Change amount
     * @param total Total amount
     * @return Percentage change, or zero if total is zero
     */
    private BigDecimal calculatePercentage(BigDecimal change, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (change == null) {
            return BigDecimal.ZERO;
        }
        return change.divide(total, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }
    
    
    /**
     * Calculates portfolio statistics and metrics using Synapse patterns
     * 
     * @param portfolioData The structured portfolio data
     * @return Structured portfolio metrics
     */
    public PortfolioMetrics calculatePortfolioMetrics(PortfolioData portfolioData) {
        log.info("Calculating portfolio metrics for user: {}", portfolioData.getUserId());
        
        try {
            // Create portfolio metrics object
            PortfolioMetrics metrics = new PortfolioMetrics();
            metrics.setUserId(portfolioData.getUserId());
            metrics.setTotalValue(portfolioData.getTotalValue());
            
            // Set performance metrics
            Map<String, BigDecimal> performance = new HashMap<>();
            performance.put("daily", new BigDecimal("0.0"));
            performance.put("weekly", new BigDecimal("0.0"));
            performance.put("monthly", new BigDecimal("0.0"));
            performance.put("yearly", new BigDecimal("0.0"));
            metrics.setPerformance(performance);
            
            // Set allocation metrics
            Map<String, BigDecimal> allocation = new HashMap<>();
            metrics.setAllocation(allocation);
            
            // Set risk metrics
            Map<String, BigDecimal> risk = new HashMap<>();
            risk.put("volatility", new BigDecimal("0.0"));
            risk.put("sharpeRatio", new BigDecimal("0.0"));
            metrics.setRisk(risk);
            
            return metrics;
        } catch (Exception e) {
            log.error("Error calculating metrics for portfolio: {}", e.getMessage(), e);
            throw new StrategizException(PortfolioErrorDetails.METRICS_CALCULATION_FAILED, "business-portfolio", e);
        }
    }
    
    /**
     * Overloaded method to support the legacy Map<String, Object> interface
     * This is a temporary bridge method that will be removed once all services are migrated to Synapse
     * 
     * @param portfolioData Raw portfolio data as a Map
     * @return Portfolio metrics as a Map
     */
    public Map<String, Object> calculatePortfolioMetrics(Map<String, Object> portfolioData) {
        log.info("Using legacy interface for calculating portfolio metrics");
        
        // Convert the Map to a PortfolioData object
        PortfolioData data = new PortfolioData();
        data.setUserId((String) portfolioData.getOrDefault("userId", "unknown"));
        
        // For simplicity, just set the total value
        if (portfolioData.containsKey("totalValue")) {
            Object value = portfolioData.get("totalValue");
            if (value instanceof Number) {
                data.setTotalValue(new BigDecimal(value.toString()));
            } else {
                data.setTotalValue(BigDecimal.ZERO);
            }
        } else {
            data.setTotalValue(BigDecimal.ZERO);
        }
        
        // Calculate metrics using the structured object
        PortfolioMetrics metrics = calculatePortfolioMetrics(data);
        
        // Convert back to a Map for legacy support
        Map<String, Object> result = new HashMap<>();
        result.put("totalValue", metrics.getTotalValue());
        result.put("performance", metrics.getPerformance());
        result.put("allocation", metrics.getAllocation());
        result.put("risk", metrics.getRisk());
        
        return result;
    }
}
