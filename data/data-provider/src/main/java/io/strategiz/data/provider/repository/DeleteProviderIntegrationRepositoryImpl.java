package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementation of DeleteProviderIntegrationRepository using BaseRepository
 */
@Repository
public class DeleteProviderIntegrationRepositoryImpl implements DeleteProviderIntegrationRepository {

	private final ProviderIntegrationBaseRepository baseRepository;

	private final ReadProviderIntegrationRepository readRepository;

	@Autowired
	public DeleteProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository,
			ReadProviderIntegrationRepository readRepository) {
		this.baseRepository = baseRepository;
		this.readRepository = readRepository;
	}

	@Override
	public boolean deleteById(String id) {
		// Use soft delete from BaseRepository (there's no hard delete)
		return baseRepository.delete(id, "system");
	}

	@Override
	public boolean deleteByUserIdAndProviderId(String userId, String providerId) {
		Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
		if (entity.isPresent()) {
			return baseRepository.delete(entity.get().getId(), userId);
		}
		return false;
	}

	@Override
	public boolean softDelete(String id, String userId) {
		return baseRepository.delete(id, userId);
	}

	@Override
	public boolean softDeleteByUserIdAndProviderId(String userId, String providerId, String deleteUserId) {
		Optional<ProviderIntegrationEntity> entity = readRepository.findByUserIdAndProviderId(userId, providerId);
		if (entity.isPresent()) {
			return baseRepository.delete(entity.get().getId(), deleteUserId);
		}
		return false;
	}

}