package io.strategiz.data.provider.repository;

/**
 * Repository interface for deleting portfolio summary from Firestore
 */
public interface DeletePortfolioSummaryRepository {

	/**
	 * Delete portfolio summary for a user
	 * @param userId User ID
	 * @return true if deleted successfully, false otherwise
	 */
	boolean deletePortfolioSummary(String userId);

	/**
	 * Delete portfolio summary history
	 * @param userId User ID
	 * @return Number of deleted documents
	 */
	int deletePortfolioSummaryHistory(String userId);

}