package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import io.strategiz.data.provider.entity.ProviderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of UpdateProviderIntegrationRepository using BaseRepository
 */
@Repository
public class UpdateProviderIntegrationRepositoryImpl implements UpdateProviderIntegrationRepository {

	private final ProviderIntegrationBaseRepository baseRepository;

	private final ReadProviderIntegrationRepository readRepository;

	@Autowired
	public UpdateProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository,
			ReadProviderIntegrationRepository readRepository) {
		this.baseRepository = baseRepository;
		this.readRepository = readRepository;
	}

	@Override
	public ProviderIntegrationEntity update(ProviderIntegrationEntity integration) {
		// Since entity doesn't store userId, we need it from somewhere else
		// This method signature should probably be changed to include userId
		return baseRepository.save(integration, "system");
	}

	@Override
	public ProviderIntegrationEntity updateWithUserId(ProviderIntegrationEntity integration, String userId) {
		return baseRepository.save(integration, userId);
	}

	@Override
	public boolean updateStatus(String userId, String providerId, String status) {
		Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
		if (entity.isPresent()) {
			ProviderIntegrationEntity integration = entity.get();
			integration.setStatus(status);
			baseRepository.save(integration, userId);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateEnabled(String userId, String providerId, boolean enabled) {
		Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
		if (entity.isPresent()) {
			ProviderIntegrationEntity integration = entity.get();
			integration.setStatus(enabled ? "connected" : "disconnected");
			baseRepository.save(integration, userId);
			return true;
		}
		return false;
	}

}