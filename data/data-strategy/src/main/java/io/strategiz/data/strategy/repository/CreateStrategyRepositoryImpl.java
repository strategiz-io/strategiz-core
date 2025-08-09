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
        // Generate ID if not provided
        if (strategy.getId() == null || strategy.getId().isEmpty()) {
            strategy.setId(UUID.randomUUID().toString());
        }
        
        // Set default status if not provided
        if (strategy.getStatus() == null || strategy.getStatus().isEmpty()) {
            strategy.setStatus("draft");
        }
        
        // Use BaseRepository's save method (requires userId)
        return baseRepository.save(strategy, strategy.getUserId());
    }
    
    @Override
    public Strategy createWithUserId(Strategy strategy, String userId) {
        strategy.setUserId(userId);
        return create(strategy);
    }
}