package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderIntegrationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ReadProviderIntegrationRepository using BaseRepository
 */
@Repository
public class ReadProviderIntegrationRepositoryImpl implements ReadProviderIntegrationRepository {

	private final ProviderIntegrationBaseRepository baseRepository;

	@Autowired
	public ReadProviderIntegrationRepositoryImpl(ProviderIntegrationBaseRepository baseRepository) {
		this.baseRepository = baseRepository;
	}

	@Override
	public Optional<ProviderIntegrationEntity> findById(String id) {
		// For finding by ID without userId, we would need to search across all users
		// This is not ideal, so we should update the interface to require userId
		// For now, return empty as we can't search without userId
		return Optional.empty();
	}

	public Optional<ProviderIntegrationEntity> findById(String id, String userId) {
		return baseRepository.findById(id, userId);
	}

	@Override
	public List<ProviderIntegrationEntity> findByUserId(String userId) {
		return baseRepository.findAllByUserId(userId);
	}

	@Override
	public Optional<ProviderIntegrationEntity> findByUserIdAndProviderId(String userId, String providerId) {
		return baseRepository.findByUserIdAndProviderId(userId, providerId);
	}

	@Override
	public List<ProviderIntegrationEntity> findByUserIdAndEnabledTrue(String userId) {
		// Since findAllByUserId already filters by isEnabled=true, just return it
		return baseRepository.findAllByUserId(userId);
	}

	@Override
	public List<ProviderIntegrationEntity> findByUserIdAndStatus(String userId, String status) {
		// Status is now replaced by isEnabled boolean
		boolean enabled = "CONNECTED".equalsIgnoreCase(status);
		if (enabled) {
			return baseRepository.findAllByUserId(userId);
		}
		else {
			// Return empty list for disconnected status
			return new ArrayList<>();
		}
	}

	@Override
	public List<ProviderIntegrationEntity> findByUserIdAndProviderType(String userId, String providerType) {
		// Provider type is no longer stored, would need to be determined from providerId
		return baseRepository.findAllByUserId(userId);
	}

	@Override
	public boolean existsByUserIdAndProviderId(String userId, String providerId) {
		return findByUserIdAndProviderId(userId, providerId).isPresent();
	}

}