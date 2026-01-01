package io.strategiz.data.strategy.repository;

import io.strategiz.data.strategy.entity.Strategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Implementation of CreateStrategyRepository using BaseRepository
 */
@Repository
public class CreateStrategyRepositoryImpl implements CreateStrategyRepository {
    
    private final StrategyBaseRepository baseRepository;
    
    @Autowired
    public CreateStrategyRepositoryImpl(StrategyBaseRepository baseRepository) {
        this.baseRepository = baseRepository;
    }
    
    @Override
    public Strategy create(Strategy strategy) {
        throw new UnsupportedOperationException("Use createWithUserId instead - strategies require userId for subcollection path");
    }

    @Override
    public Strategy createWithUserId(Strategy strategy, String userId) {
        // Generate ID if not provided
        if (strategy.getId() == null || strategy.getId().isEmpty()) {
            strategy.setId(UUID.randomUUID().toString());
        }

        // Set default publishStatus if not provided
        if (strategy.getPublishStatus() == null || strategy.getPublishStatus().isEmpty()) {
            strategy.setPublishStatus("DRAFT");
        }

        // Set default listedStatus if not provided
        if (strategy.getListedStatus() == null || strategy.getListedStatus().isEmpty()) {
            strategy.setListedStatus("NOT_LISTED");
        }

        // Use forceCreate since we pre-assign the ID (save() would route to update)
        return baseRepository.forceCreate(strategy, userId);
    }
}