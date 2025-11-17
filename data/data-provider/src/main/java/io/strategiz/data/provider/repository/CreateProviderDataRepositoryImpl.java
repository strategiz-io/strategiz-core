package io.strategiz.data.provider.repository;

import io.strategiz.data.provider.entity.ProviderDataEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Implementation of CreateProviderDataRepository using BaseRepository
 */
@Repository
public class CreateProviderDataRepositoryImpl implements CreateProviderDataRepository {

    private static final Logger log = LoggerFactory.getLogger(CreateProviderDataRepositoryImpl.class);
    private final ProviderDataBaseRepository baseRepository;

    @Autowired
    public CreateProviderDataRepositoryImpl(ProviderDataBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }

    @Override
    public ProviderDataEntity createProviderData(String userId, String providerId, ProviderDataEntity data) {
        log.info("CreateProviderDataRepository: Creating provider data - userId={}, providerId={}", userId, providerId);

        // Set the provider ID in the entity
        data.setProviderId(providerId);

        // Use providerId as document ID for easy lookup
        ProviderDataEntity saved = baseRepository.saveWithProviderId(data, userId, providerId);

        log.info("CreateProviderDataRepository: Successfully created provider data at users/{}/provider_data/{}", userId, providerId);
        return saved;
    }

    @Override
    public ProviderDataEntity createOrReplaceProviderData(String userId, String providerId, ProviderDataEntity data) {
        log.info("CreateProviderDataRepository: Saving provider data - userId={}, providerId={}, holdings count={}",
            userId, providerId, data.getHoldings() != null ? data.getHoldings().size() : 0);

        // Set the provider ID in the entity
        data.setProviderId(providerId);

        // This will overwrite if exists or create if not
        ProviderDataEntity saved = baseRepository.saveWithProviderId(data, userId, providerId);

        log.info("CreateProviderDataRepository: Successfully saved provider data at users/{}/provider_data/{}", userId, providerId);
        return saved;
    }
}