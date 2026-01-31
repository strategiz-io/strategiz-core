package io.strategiz.data.provider.repository;

/**
 * Repository interface for deleting provider data from Firestore
 */
public interface DeleteProviderDataRepository {

	/**
	 * Delete provider data
	 * @param userId User ID
	 * @param providerId Provider ID
	 * @return true if deleted successfully, false otherwise
	 */
	boolean deleteProviderData(String userId, String providerId);

	/**
	 * Delete all provider data for a user
	 * @param userId User ID
	 * @return Number of deleted documents
	 */
	int deleteAllProviderData(String userId);

	/**
	 * Delete provider data by account type
	 * @param userId User ID
	 * @param accountType Account type (crypto, stock, forex)
	 * @return Number of deleted documents
	 */
	int deleteProviderDataByType(String userId, String accountType);

}