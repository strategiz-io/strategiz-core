package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.PortfolioSummaryEntity;

/**
 * Repository interface for creating portfolio summary in Firestore
 */
public interface CreatePortfolioSummaryRepository {

    /**
     * Create new portfolio summary for a user
     * 
     * @param userId User ID
     * @param summary Portfolio summary to save
     * @return Saved portfolio summary entity
     */
    PortfolioSummaryEntity createPortfolioSummary(String userId, PortfolioSummaryEntity summary);

    /**
     * Create or replace portfolio summary for a user
     * 
     * @param userId User ID
     * @param summary Portfolio summary to save
     * @return Saved portfolio summary entity
     */
    PortfolioSummaryEntity createOrReplacePortfolioSummary(String userId, PortfolioSummaryEntity summary);
}