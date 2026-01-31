package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;

/**
 * Repository interface for creating provider integration entities Following Single
 * Responsibility Principle - focused only on create operations
 */
public interface CreateProviderIntegrationRepository {

	/**
	 * Create a new provider integration
	 * @param integration The provider integration to create
	 * @return The created provider integration
	 */
	ProviderIntegrationEntity create(ProviderIntegrationEntity integration);

	/**
	 * Create a new provider integration for a specific user
	 * @param integration The provider integration to create
	 * @param userId The user ID
	 * @return The created provider integration
	 */
	ProviderIntegrationEntity createForUser(ProviderIntegrationEntity integration, String userId);

}