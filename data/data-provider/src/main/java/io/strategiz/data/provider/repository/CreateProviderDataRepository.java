package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;

/**
 * Repository interface for creating provider data in Firestore
 */
public interface CreateProviderDataRepository {

	/**
	 * Create new provider data for a user
	 * @param userId User ID
	 * @param providerId Provider ID (e.g., "kraken", "coinbase")
	 * @param data Provider data to save
	 * @return Saved provider data entity
	 */
	ProviderDataEntity createProviderData(String userId, String providerId, ProviderDataEntity data);

	/**
	 * Create or replace provider data for a user
	 * @param userId User ID
	 * @param providerId Provider ID
	 * @param data Provider data to save
	 * @return Saved provider data entity
	 */
	ProviderDataEntity createOrReplaceProviderData(String userId, String providerId, ProviderDataEntity data);

}