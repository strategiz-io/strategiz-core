package io.strategiz.business.portfolio;

import io.strategiz.data.provider.entity.PortfolioProviderEntity;
import io.strategiz.data.provider.entity.ProviderHoldingsEntity;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.repository.PortfolioProviderRepository;
import io.strategiz.data.provider.repository.ProviderHoldingsRepository;
import io.strategiz.data.provider.repository.CreatePortfolioSummaryRepository;
import io.strategiz.data.provider.repository.ReadPortfolioSummaryRepository;
import io.strategiz.data.provider.repository.UpdatePortfolioSummaryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Business component for managing portfolio summary calculations and updates.
 * Aggregates data from all portfolio providers and maintains portfolio summary.
 *
 * Data structure:
 *   users/{userId}/portfolio/summary              ← Aggregated totals
 *   users/{userId}/portfolio/{providerId}         ← Provider status (lightweight)
 *   users/{userId}/portfolio/{providerId}/holdings/current  ← Holdings (heavy data)
 *
 * @author Strategiz Team
 * @version 1.0
 */
@Component
public class PortfolioSummaryManager {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryManager.class);

    @Autowired(required = false)
    private PortfolioProviderRepository portfolioProviderRepository;

    @Autowired(required = false)
    private ProviderHoldingsRepository providerHoldingsRepository;

    @Autowired(required = false)
    private CreatePortfolioSummaryRepository createPortfolioSummaryRepo;

    @Autowired(required = false)
    private ReadPortfolioSummaryRepository readPortfolioSummaryRepo;

    @Autowired(required = false)
    private UpdatePortfolioSummaryRepository updatePortfolioSummaryRepo;

    /**
     * Refresh portfolio summary for a user by recalculating from all provider data.
     * Creates summary if it doesn't exist, updates if it does.
     *
     * @param userId The user ID
     */
    public void refreshPortfolioSummary(String userId) {
        log.info("Refreshing portfolio summary for user: {}", userId);

        try {
            // Read all connected providers for user
            List<PortfolioProviderEntity> providers = portfolioProviderRepository.findAllByUserId(userId);

            // Load holdings for each provider
            List<ProviderHoldingsEntity> allHoldings = new ArrayList<>();
            for (PortfolioProviderEntity provider : providers) {
                providerHoldingsRepository.findByUserIdAndProviderId(userId, provider.getProviderId())
                        .ifPresent(allHoldings::add);
            }

            // Calculate summary from providers and holdings
            PortfolioSummaryEntity summary = calculatePortfolioSummary(providers, allHoldings);

            // Check if summary exists
            PortfolioSummaryEntity existing = readPortfolioSummaryRepo.getPortfolioSummary(userId);

            if (existing != null) {
                // Update existing
                updatePortfolioSummaryRepo.updatePortfolioSummary(userId, summary);
                log.debug("Updated portfolio summary for user: {}", userId);
            } else {
                // Create new
                createPortfolioSummaryRepo.createPortfolioSummary(userId, summary);
                log.debug("Created portfolio summary for user: {}", userId);
            }

            log.info("Successfully refreshed portfolio summary for user: {}", userId);

        } catch (Exception e) {
            log.error("Error refreshing portfolio summary for user: {}", userId, e);
            // Don't throw - portfolio summary update is not critical, provider operations should continue
        }
    }

    /**
     * Calculate portfolio summary from providers and their holdings.
     *
     * @param providers List of connected provider entities
     * @param holdings List of holdings entities (one per provider)
     * @return Calculated portfolio summary
     */
    private PortfolioSummaryEntity calculatePortfolioSummary(
            List<PortfolioProviderEntity> providers,
            List<ProviderHoldingsEntity> holdings) {

        PortfolioSummaryEntity summary = new PortfolioSummaryEntity();

        if (providers == null || providers.isEmpty()) {
            // Return empty summary
            return createEmptySummary();
        }

        // Create a map of holdings by providerId for quick lookup
        Map<String, ProviderHoldingsEntity> holdingsByProvider = new HashMap<>();
        for (ProviderHoldingsEntity h : holdings) {
            holdingsByProvider.put(h.getProviderId(), h);
        }

        // Calculate total value across all providers
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal dayChange = BigDecimal.ZERO;
        BigDecimal totalCash = BigDecimal.ZERO;
        Map<String, BigDecimal> accountPerformance = new HashMap<>();

        // Track values by provider type for asset allocation
        Map<String, BigDecimal> valueByType = new HashMap<>();
        Map<String, Integer> assetCountByType = new HashMap<>();

        for (PortfolioProviderEntity provider : providers) {
            // Use totalValue from provider entity (lightweight summary)
            if (provider.getTotalValue() != null) {
                totalValue = totalValue.add(provider.getTotalValue());
                accountPerformance.put(provider.getProviderId(), provider.getTotalValue());
            }

            // Get holdings for detailed calculations
            ProviderHoldingsEntity providerHoldings = holdingsByProvider.get(provider.getProviderId());
            if (providerHoldings != null) {
                // Sum day changes from holdings
                if (providerHoldings.getDayChange() != null) {
                    dayChange = dayChange.add(providerHoldings.getDayChange());
                }

                // Sum cash balances
                if (providerHoldings.getCashBalance() != null) {
                    totalCash = totalCash.add(providerHoldings.getCashBalance());
                }

                // Aggregate by provider type for asset allocation
                String providerType = provider.getProviderType();
                if (providerType != null && provider.getTotalValue() != null) {
                    valueByType.merge(providerType, provider.getTotalValue(), BigDecimal::add);

                    // Count assets (holdings) per type
                    int holdingCount = provider.getHoldingsCount() != null ? provider.getHoldingsCount() : 0;
                    assetCountByType.merge(providerType, holdingCount, Integer::sum);
                }
            }
        }

        // Calculate day change percentage
        BigDecimal dayChangePercent = BigDecimal.ZERO;
        if (totalValue.compareTo(BigDecimal.ZERO) > 0 && dayChange.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal previousValue = totalValue.subtract(dayChange);
            if (previousValue.compareTo(BigDecimal.ZERO) > 0) {
                dayChangePercent = dayChange.divide(previousValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
            }
        }

        // Set basic fields
        summary.setTotalValue(totalValue);
        summary.setDayChange(dayChange);
        summary.setDayChangePercent(dayChangePercent);
        summary.setAccountPerformance(accountPerformance);
        summary.setProvidersCount(providers.size());
        summary.setLastSyncedAt(Instant.now());
        summary.setCashAvailable(totalCash);

        // Set defaults for fields not yet calculated
        summary.setWeekChange(BigDecimal.ZERO);
        summary.setWeekChangePercent(BigDecimal.ZERO);
        summary.setMonthChange(BigDecimal.ZERO);
        summary.setMonthChangePercent(BigDecimal.ZERO);
        summary.setTotalReturn(BigDecimal.ZERO);
        summary.setTotalReturnPercent(BigDecimal.ZERO);

        // Calculate asset allocation by provider type
        PortfolioSummaryEntity.AssetAllocation allocation = calculateAssetAllocation(valueByType, totalValue);
        summary.setAssetAllocation(allocation);

        // Calculate category allocations for pie chart
        List<PortfolioSummaryEntity.CategoryAllocationData> categoryAllocations =
                calculateCategoryAllocations(valueByType, assetCountByType, totalValue, totalCash);
        summary.setCategoryAllocations(categoryAllocations);

        return summary;
    }

    /**
     * Calculate asset allocation breakdown.
     */
    private PortfolioSummaryEntity.AssetAllocation calculateAssetAllocation(
            Map<String, BigDecimal> valueByType, BigDecimal totalValue) {

        PortfolioSummaryEntity.AssetAllocation allocation = new PortfolioSummaryEntity.AssetAllocation();

        BigDecimal cryptoValue = valueByType.getOrDefault("crypto", BigDecimal.ZERO);
        BigDecimal stocksValue = valueByType.getOrDefault("equity", BigDecimal.ZERO);
        BigDecimal forexValue = valueByType.getOrDefault("forex", BigDecimal.ZERO);

        allocation.setCrypto(cryptoValue);
        allocation.setStocks(stocksValue);
        allocation.setForex(forexValue);
        allocation.setCommodities(BigDecimal.ZERO);

        // Calculate percentages
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            allocation.setCryptoPercent(cryptoValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
            allocation.setStocksPercent(stocksValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
            allocation.setForexPercent(forexValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")));
        } else {
            allocation.setCryptoPercent(BigDecimal.ZERO);
            allocation.setStocksPercent(BigDecimal.ZERO);
            allocation.setForexPercent(BigDecimal.ZERO);
        }
        allocation.setCommoditiesPercent(BigDecimal.ZERO);

        return allocation;
    }

    /**
     * Calculate category allocations for pie chart display.
     * Maps provider types to user-friendly categories with colors.
     */
    private List<PortfolioSummaryEntity.CategoryAllocationData> calculateCategoryAllocations(
            Map<String, BigDecimal> valueByType,
            Map<String, Integer> assetCountByType,
            BigDecimal totalValue,
            BigDecimal totalCash) {

        List<PortfolioSummaryEntity.CategoryAllocationData> allocations = new ArrayList<>();

        // Define category mappings
        Map<String, CategoryInfo> categoryMappings = new LinkedHashMap<>();
        categoryMappings.put("crypto", new CategoryInfo("CRYPTOCURRENCY", "Cryptocurrencies", "#39FF14"));
        categoryMappings.put("equity", new CategoryInfo("STOCKS", "Stocks & Equities", "#00BFFF"));
        categoryMappings.put("forex", new CategoryInfo("FOREX", "Foreign Exchange", "#9D00FF"));

        // Add categories that have value
        for (Map.Entry<String, CategoryInfo> entry : categoryMappings.entrySet()) {
            String providerType = entry.getKey();
            CategoryInfo info = entry.getValue();
            BigDecimal value = valueByType.getOrDefault(providerType, BigDecimal.ZERO);

            if (value.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentage = totalValue.compareTo(BigDecimal.ZERO) > 0
                        ? value.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

                Integer assetCount = assetCountByType.getOrDefault(providerType, 0);

                allocations.add(new PortfolioSummaryEntity.CategoryAllocationData(
                        info.category,
                        info.categoryName,
                        info.color,
                        value,
                        percentage,
                        assetCount
                ));
            }
        }

        // Add cash category if there's cash available
        if (totalCash.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cashPercentage = totalValue.compareTo(BigDecimal.ZERO) > 0
                    ? totalCash.divide(totalValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            allocations.add(new PortfolioSummaryEntity.CategoryAllocationData(
                    "CASH",
                    "Cash & Money Market",
                    "#FFFFFF",
                    totalCash,
                    cashPercentage,
                    1  // Cash is counted as 1 "asset"
            ));
        }

        // Sort by value descending for better pie chart display
        allocations.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        return allocations;
    }

    /**
     * Helper record for category information.
     */
    private record CategoryInfo(String category, String categoryName, String color) {}

    /**
     * Create an empty portfolio summary.
     *
     * @return Empty portfolio summary entity
     */
    private PortfolioSummaryEntity createEmptySummary() {
        PortfolioSummaryEntity summary = new PortfolioSummaryEntity();
        summary.setTotalValue(BigDecimal.ZERO);
        summary.setTotalReturn(BigDecimal.ZERO);
        summary.setTotalReturnPercent(BigDecimal.ZERO);
        summary.setCashAvailable(BigDecimal.ZERO);
        summary.setDayChange(BigDecimal.ZERO);
        summary.setDayChangePercent(BigDecimal.ZERO);
        summary.setWeekChange(BigDecimal.ZERO);
        summary.setWeekChangePercent(BigDecimal.ZERO);
        summary.setMonthChange(BigDecimal.ZERO);
        summary.setMonthChangePercent(BigDecimal.ZERO);
        summary.setAssetAllocation(new PortfolioSummaryEntity.AssetAllocation());
        summary.setCategoryAllocations(new ArrayList<>());
        summary.setAccountPerformance(new HashMap<>());
        summary.setProvidersCount(0);
        summary.setLastSyncedAt(Instant.now());
        return summary;
    }
}
