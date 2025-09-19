package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;
import java.time.Instant;
import java.util.List;

/**
 * Repository interface for reading portfolio summary from Firestore
 */
public interface ReadPortfolioSummaryRepository {

    /**
     * Get current portfolio summary for a user
     * 
     * @param userId User ID
     * @return Portfolio summary entity or null if not found
     */
    PortfolioSummaryEntity getPortfolioSummary(String userId);

    /**
     * Get portfolio summary history
     * 
     * @param userId User ID
     * @param from Start date
     * @param to End date
     * @return List of historical portfolio summaries
     */
    List<PortfolioSummaryEntity> getPortfolioSummaryHistory(String userId, Instant from, Instant to);

    /**
     * Check if portfolio summary exists
     * 
     * @param userId User ID
     * @return true if exists, false otherwise
     */
    boolean portfolioSummaryExists(String userId);

    /**
     * Get last sync time for portfolio
     * 
     * @param userId User ID
     * @return Last sync timestamp or null if not found
     */
    Instant getLastSyncTime(String userId);
}