package io.strategiz.service.dashboard;

import io.americanexpress.synapse.service.rest.service.BaseService;
import io.strategiz.business.portfolio.PortfolioManager;
import io.strategiz.business.portfolio.model.PortfolioData;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocation;
import io.strategiz.service.dashboard.model.assetallocation.AssetAllocationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for asset allocation operations.
 * This service provides data for asset allocation pie charts and related visualizations.
 */
@Service
@Slf4j
public class AssetAllocationService extends BaseService {

    private final PortfolioManager portfolioManager;
    
    // Color palette for asset visualization
    private static final List<String> COLORS = Arrays.asList(
        "#3498db", "#2ecc71", "#e74c3c", "#f39c12", "#9b59b6", 
        "#1abc9c", "#d35400", "#34495e", "#16a085", "#c0392b",
        "#27ae60", "#8e44ad", "#f1c40f", "#e67e22", "#95a5a6"
    );

    @Autowired
    public AssetAllocationService(PortfolioManager portfolioManager) {
        this.portfolioManager = portfolioManager;
    }

    /**
     * Gets asset allocation data for the user's portfolio
     * 
     * @param userId The user ID to fetch allocation data for
     * @return Asset allocation data for pie chart visualization
     */
    public AssetAllocationData getAssetAllocation(String userId) {
        log.info("Getting asset allocation data for user: {}", userId);
        
        try {
            // Get portfolio data from the business layer
            PortfolioData portfolioData = portfolioManager.getAggregatedPortfolioData(userId);
            
            // Calculate asset allocations for pie chart
            return calculateAssetAllocation(portfolioData);
        } catch (Exception e) {
            log.error("Error getting asset allocation for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve asset allocation data", e);
        }
    }
    
    /**
     * Calculates asset allocation data from portfolio data
     * 
     * @param portfolioData Portfolio data
     * @return Asset allocation data for pie chart visualization
     */
    private AssetAllocationData calculateAssetAllocation(PortfolioData portfolioData) {
        AssetAllocationData response = new AssetAllocationData();
        List<AssetAllocation> allocations = new ArrayList<>();
        
        // Check if portfolio has any assets
        if (portfolioData == null || portfolioData.getExchanges() == null || 
            portfolioData.getExchanges().isEmpty() || 
            portfolioData.getTotalValue() == null || 
            portfolioData.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            
            response.setAllocations(allocations);
            return response;
        }
        
        // Total portfolio value for percentage calculations
        BigDecimal totalValue = portfolioData.getTotalValue();
        
        // Track assets that appear in multiple exchanges to combine them
        Map<String, AssetAllocation> assetMap = new HashMap<>();
        
        // Process each exchange
        int colorIndex = 0;
        for (Map.Entry<String, PortfolioData.ExchangeData> exchangeEntry : portfolioData.getExchanges().entrySet()) {
            PortfolioData.ExchangeData exchangeData = exchangeEntry.getValue();
            
            // Process each asset in the exchange
            if (exchangeData.getAssets() != null) {
                for (Map.Entry<String, PortfolioData.AssetData> assetEntry : exchangeData.getAssets().entrySet()) {
                    PortfolioData.AssetData assetData = assetEntry.getValue();
                    
                    // Skip assets with zero or null value
                    if (assetData.getValue() == null || assetData.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    // Create or update asset allocation
                    String assetKey = assetData.getSymbol();
                    AssetAllocation allocation;
                    
                    if (assetMap.containsKey(assetKey)) {
                        // Update existing asset allocation
                        allocation = assetMap.get(assetKey);
                        BigDecimal existingValue = allocation.getValue() != null ? allocation.getValue() : BigDecimal.ZERO;
                        BigDecimal newValue = assetData.getValue() != null ? assetData.getValue() : BigDecimal.ZERO;
                        allocation.setValue(existingValue.add(newValue));
                    } else {
                        // Create new asset allocation
                        allocation = new AssetAllocation();
                        allocation.setName(assetData.getName());
                        allocation.setSymbol(assetData.getSymbol());
                        allocation.setValue(assetData.getValue());
                        allocation.setExchange(exchangeData.getName());
                        allocation.setColor(COLORS.get(colorIndex % COLORS.size()));
                        colorIndex++;
                        
                        assetMap.put(assetKey, allocation);
                    }
                }
            }
        }
        
        // Calculate percentages and build final list
        for (AssetAllocation allocation : assetMap.values()) {
            // Calculate percentage of total portfolio
            BigDecimal percentage = allocation.getValue()
                .divide(totalValue, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
            
            allocation.setPercentage(percentage);
            allocations.add(allocation);
        }
        
        // Sort by value descending
        allocations = allocations.stream()
            .sorted((a1, a2) -> a2.getValue().compareTo(a1.getValue()))
            .collect(Collectors.toList());
        
        response.setAllocations(allocations);
        return response;
    }
}
