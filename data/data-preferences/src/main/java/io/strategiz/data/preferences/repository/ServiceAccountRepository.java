package io.strategiz.data.preferences.repository;

import io.strategiz.data.preferences.entity.ServiceAccountEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Service Account operations.
 * Service accounts enable machine-to-machine authentication for:
 * - CI/CD pipelines
 * - Integration testing
 * - External service integrations
 */
public interface ServiceAccountRepository {

	/**
	 * Create a new service account.
	 * @param entity The service account to create
	 * @return The created service account with generated ID
	 */
	ServiceAccountEntity create(ServiceAccountEntity entity);

	/**
	 * Find a service account by its ID.
	 * @param id The service account ID
	 * @return Optional containing the service account if found
	 */
	Optional<ServiceAccountEntity> findById(String id);

	/**
	 * Find a service account by client ID.
	 * Used during token generation to validate credentials.
	 * @param clientId The client ID
	 * @return Optional containing the service account if found
	 */
	Optional<ServiceAccountEntity> findByClientId(String clientId);

	/**
	 * Find all service accounts.
	 * @return List of all service accounts
	 */
	List<ServiceAccountEntity> findAll();

	/**
	 * Find all enabled service accounts.
	 * @return List of enabled service accounts
	 */
	List<ServiceAccountEntity> findAllEnabled();

	/**
	 * Update an existing service account.
	 * @param entity The service account to update
	 * @return The updated service account
	 */
	ServiceAccountEntity update(ServiceAccountEntity entity);

	/**
	 * Delete a service account by ID.
	 * @param id The service account ID
	 * @return true if deleted, false if not found
	 */
	boolean delete(String id);

	/**
	 * Update last used timestamp and usage count.
	 * Called when a service account is used to generate a token.
	 * @param id The service account ID
	 * @param ip The IP address used
	 */
	void recordUsage(String id, String ip);

	/**
	 * Check if a client ID already exists.
	 * @param clientId The client ID to check
	 * @return true if exists
	 */
	boolean existsByClientId(String clientId);

}
