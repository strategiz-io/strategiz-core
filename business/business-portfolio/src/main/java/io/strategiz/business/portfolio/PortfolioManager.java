package io.strategiz.business.portfolio;

import io.americanexpress.synapse.framework.manager.BaseManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.business.portfolio.model.PortfolioMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Core business logic for portfolio operations.
 * This class contains domain logic that can be reused across different services.
 * Implements Synapse BaseManager pattern.
 */
@Slf4j
@Component
public class PortfolioManager extends BaseManager {

    /**
     * Aggregates portfolio data from multiple exchanges using Synapse patterns
     * 
     * @param userId The user ID to fetch portfolio data for
     * @return Structured portfolio data
     */
    public PortfolioData getAggregatedPortfolioData(String userId) {
        log.info("Getting aggregated portfolio data for user: {}", userId);
        
        try {
            // Create portfolio data object
            PortfolioData portfolioData = new PortfolioData();
            portfolioData.setUserId(userId);
            portfolioData.setTotalValue(BigDecimal.ZERO);
            portfolioData.setDailyChange(BigDecimal.ZERO);
            portfolioData.setDailyChangePercent(BigDecimal.ZERO);
            
            // Create example exchange data
            Map<String, PortfolioData.ExchangeData> exchanges = new HashMap<>();
            
            // Add sample exchange (in a real implementation, this would fetch from repositories)
            PortfolioData.ExchangeData exchange = new PortfolioData.ExchangeData();
            exchange.setId("coinbase");
            exchange.setName("Coinbase");
            exchange.setValue(BigDecimal.ZERO);
            exchange.setAssets(new HashMap<>());
            exchanges.put("coinbase", exchange);
            
            portfolioData.setExchanges(exchanges);
            
            return portfolioData;
        } catch (Exception e) {
            log.error("Error getting portfolio data for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve portfolio data", e);
        }
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
            throw new RuntimeException("Failed to calculate portfolio metrics", e);
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
