package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import java.util.List;

/**
 * Repository interface for reading provider data from Firestore
 */
public interface ReadProviderDataRepository {

	/**
	 * Get provider data for a specific provider
	 * @param userId User ID
	 * @param providerId Provider ID
	 * @return Provider data entity or null if not found
	 */
	ProviderDataEntity getProviderData(String userId, String providerId);

	/**
	 * Get all provider data for a user
	 * @param userId User ID
	 * @return List of provider data entities
	 */
	List<ProviderDataEntity> getAllProviderData(String userId);

	/**
	 * Get provider data by account type
	 * @param userId User ID
	 * @param accountType Account type (crypto, stock, forex)
	 * @return List of provider data entities matching the account type
	 */
	List<ProviderDataEntity> getProviderDataByType(String userId, String accountType);

	/**
	 * Check if provider data exists
	 * @param userId User ID
	 * @param providerId Provider ID
	 * @return true if exists, false otherwise
	 */
	boolean providerDataExists(String userId, String providerId);

}