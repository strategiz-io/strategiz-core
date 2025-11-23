package io.strategiz.business.portfolio;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import io.strategiz.data.provider.repository.ReadProviderDataRepository;
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
 * Aggregates data from all provider_data and maintains portfolio_summary collection.
 *
 * @author Strategiz Team
 * @version 1.0
 */
@Component
public class PortfolioSummaryManager {

    private static final Logger log = LoggerFactory.getLogger(PortfolioSummaryManager.class);

    @Autowired(required = false)
    private ReadProviderDataRepository readProviderDataRepo;

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
            // Read all provider data for user
            List<ProviderDataEntity> providerData = readProviderDataRepo.getAllProviderData(userId);

            // Calculate summary
            PortfolioSummaryEntity summary = calculatePortfolioSummary(providerData);

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
     * Calculate portfolio summary from provider data.
     *
     * @param providerData List of provider data entities
     * @return Calculated portfolio summary
     */
    private PortfolioSummaryEntity calculatePortfolioSummary(List<ProviderDataEntity> providerData) {
        PortfolioSummaryEntity summary = new PortfolioSummaryEntity();

        if (providerData == null || providerData.isEmpty()) {
            // Return empty summary
            return createEmptySummary();
        }

        // Calculate total value across all providers
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal dayChange = BigDecimal.ZERO;
        Map<String, BigDecimal> accountPerformance = new HashMap<>();

        for (ProviderDataEntity data : providerData) {
            // Sum total values
            if (data.getTotalValue() != null) {
                totalValue = totalValue.add(data.getTotalValue());
                accountPerformance.put(data.getProviderId(), data.getTotalValue());
            }

            // Sum day changes
            if (data.getDayChange() != null) {
                dayChange = dayChange.add(data.getDayChange());
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
        summary.setProvidersCount(providerData.size());
        summary.setLastSyncedAt(Instant.now());

        // TODO: Calculate week/month changes, asset allocation, top performers
        // For now, set defaults
        summary.setWeekChange(BigDecimal.ZERO);
        summary.setWeekChangePercent(BigDecimal.ZERO);
        summary.setMonthChange(BigDecimal.ZERO);
        summary.setMonthChangePercent(BigDecimal.ZERO);
        summary.setTotalReturn(BigDecimal.ZERO);
        summary.setTotalReturnPercent(BigDecimal.ZERO);
        summary.setCashAvailable(BigDecimal.ZERO);

        // Create empty asset allocation
        PortfolioSummaryEntity.AssetAllocation allocation = new PortfolioSummaryEntity.AssetAllocation();
        allocation.setCrypto(BigDecimal.ZERO);
        allocation.setCryptoPercent(BigDecimal.ZERO);
        allocation.setStocks(BigDecimal.ZERO);
        allocation.setStocksPercent(BigDecimal.ZERO);
        allocation.setForex(BigDecimal.ZERO);
        allocation.setForexPercent(BigDecimal.ZERO);
        allocation.setCommodities(BigDecimal.ZERO);
        allocation.setCommoditiesPercent(BigDecimal.ZERO);
        summary.setAssetAllocation(allocation);

        return summary;
    }

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
        summary.setAccountPerformance(new HashMap<>());
        summary.setProvidersCount(0);
        summary.setLastSyncedAt(Instant.now());
        return summary;
    }
}
