package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateProviderIntegrationRepository using BaseRepository
 */
@Repository
public class CreateProviderIntegrationRepositoryImpl implements CreateProviderIntegrationRepository {

	private final ProviderIntegrationBaseRepository baseRepository;

	@Autowired
	public CreateProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public ProviderIntegrationEntity create(ProviderIntegrationEntity integration) {
		// DO NOT set ID here - let BaseRepository handle it as a create operation
		// The BaseRepository will generate the ID when it detects a null ID

		// Set default enabled state (it's a primitive boolean, so no null check needed)
		// The entity already defaults to true in its field declaration

		// For now, use a dummy userId - this should be handled differently
		// The entity doesn't store userId anymore since it's in the path
		return baseRepository.save(integration, "system");
	}

	@Override
	public ProviderIntegrationEntity createForUser(ProviderIntegrationEntity integration, String userId) {
		// Just save with the provided userId
		// The entity doesn't need to store userId since it's in the path
		return baseRepository.save(integration, userId);
	}

}