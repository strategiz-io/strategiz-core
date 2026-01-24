package io.strategiz.data.preferences.repository;

import io.strategiz.data.preferences.entity.ServiceAccountEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ServiceAccount entities.
 *
 * Service accounts are stored in Firestore collection: service_accounts
 *
 * This repository provides CRUD operations for managing service accounts
 * used in machine-to-machine authentication for CI/CD pipelines and
 * integration testing.
 */
public interface ServiceAccountRepository {

	/**
	 * Create a new service account.
	 * @param entity The service account entity to create
	 * @return The created entity with generated ID
	 */
	ServiceAccountEntity create(ServiceAccountEntity entity);

	/**
	 * Find a service account by its document ID.
	 * @param id The document ID
	 * @return Optional containing the entity if found
	 */
	Optional<ServiceAccountEntity> findById(String id);

	/**
	 * Find a service account by its client ID.
	 * @param clientId The client ID (public identifier)
	 * @return Optional containing the entity if found
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
	 * @param entity The entity with updated fields
	 * @return The updated entity
	 */
	ServiceAccountEntity update(ServiceAccountEntity entity);

	/**
	 * Delete a service account by ID.
	 * @param id The document ID
	 * @return true if deleted, false if not found
	 */
	boolean delete(String id);

	/**
	 * Record usage of a service account (updates lastUsedAt, lastUsedIp, usageCount).
	 * @param id The service account ID
	 * @param ip The client IP address
	 */
	void recordUsage(String id, String ip);

	/**
	 * Check if a service account exists with the given client ID.
	 * @param clientId The client ID to check
	 * @return true if exists
	 */
	boolean existsByClientId(String clientId);

}
